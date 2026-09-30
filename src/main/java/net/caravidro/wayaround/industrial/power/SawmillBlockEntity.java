package net.caravidro.wayaround.industrial.power;

import javax.annotation.Nullable;

import net.caravidro.wayaround.industrial.assembly.AssemblyItemData;
import net.caravidro.wayaround.industrial.assembly.AssemblyPartProfile;
import net.caravidro.wayaround.industrial.mechanical.IRotationalPower;
import net.caravidro.wayaround.industrial.mechanical.MechanicalTransmission;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * V1 sawmill: a physical consumer rather than a binary recipe box.
 *
 * Installed components remain real ItemStacks with AssemblyPartProfile data.
 * Power quality, alignment, wear, fatigue and over-speed change cutting speed,
 * vibration, output yield and the quality stamped into the produced boards.
 */
public final class SawmillBlockEntity extends BlockEntity implements MenuProvider {

    private static final float OPTIMAL_RPM =
            30.0F;

    private static final float SAFE_RPM =
            48.0F;

    private ItemStack body =
            ItemStack.EMPTY;

    private ItemStack blade =
            ItemStack.EMPTY;

    private ItemStack driveShaft =
            ItemStack.EMPTY;

    private ItemStack input =
            ItemStack.EMPTY;

    private float progress;
    private float rpm;
    private float bladeAngle;
    private float vibration;
    private float heat;
    private float lastPowerRatio;
    private float lastRequestedPower;
    private float lastGrantedPower;

    private int completedCuts;
    private boolean jammed;
    private int manualCrankTicks;
    private SawmillRecipe selectedRecipe =
            SawmillRecipe.WOOD;

    private final ContainerData menuData =
            new ContainerData() {
                @Override
                public int get(int index) {
                    return switch (index) {
                        case 0 -> selectedRecipe.ordinal();
                        case 1 -> Math.round(progress * 1000.0F);
                        case 2 -> manualCrankTicks;
                        case 3 -> longTableMode() ? 1 : 0;
                        default -> 0;
                    };
                }

                @Override
                public void set(int index, int value) {
                    if (index == 0) {
                        selectRecipe(value);
                    }
                }

                @Override
                public int getCount() {
                    return 4;
                }
            };

    public SawmillBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                PowerContent.SAWMILL_ENTITY.get(),
                pos,
                state
        );
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            SawmillBlockEntity sawmill
    ) {
        if (!WorldFeatureRuntime.enabled(
                level,
                WorldFeature.INDUSTRIAL_MACHINES
        )) {
            return;
        }

        sawmill.tickMachine();
    }

    private void tickMachine() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }

        ensureProfiles(
                server
        );

        IRotationalPower source =
                findBestSource();

        boolean longLine =
                longTableMode();

        boolean longBatchReady =
                !longLine
                        || selectedRecipe
                                != SawmillRecipe.WOOD
                        || input.getCount()
                                >= 4;

        boolean manual =
                manualCrankTicks > 0;

        if (manualCrankTicks > 0) {
            manualCrankTicks--;
        }

        float targetRpm =
                source == null
                        ? (
                        manual
                                ? 18.0F
                                : 0.0F
                )
                        : source.rpm();

        AssemblyPartProfile bladeProfile =
                bladeProfile();

        AssemblyPartProfile shaftProfile =
                shaftProfile();

        AssemblyPartProfile bodyProfile =
                bodyProfile();

        boolean mechanicallyComplete =
                bladeProfile != null
                        && (
                        shaftProfile != null
                                || manual
                );

        float bladePerformance =
                bladeProfile == null
                        ? 0.0F
                        : bladeProfile.performanceFactor();

        float shaftPerformance =
                shaftProfile == null
                        ? 0.0F
                        : shaftProfile.performanceFactor();

        float bodyPerformance =
                bodyProfile == null
                        ? 0.82F
                        : bodyProfile.performanceFactor();

        float speedAbs =
                Math.abs(
                        targetRpm
                );

        float dullness =
                bladeProfile == null
                        ? 1.0F
                        : 1.0F
                                - bladeProfile.durabilityScore();

        float requestedPower =
                0.0F;

        if ((source != null
                || manual)
                && mechanicallyComplete) {

            if (jammed) {
                /*
                 * A jam is a locked shaft, not a magical off switch. The source
                 * still feels a heavy load until the player clears it.
                 */
                requestedPower =
                        longLine
                                ? 8.8F
                                : 3.8F;

            } else if (input.isEmpty()) {
                requestedPower =
                        (longLine
                                ? 0.18F
                                : 0.08F)
                                + speedAbs
                                        * (longLine
                                        ? 0.0045F
                                        : 0.002F);

            } else {
                requestedPower =
                        longLine
                                ? 6.40F
                                        + speedAbs
                                                * 0.060F
                                        + dullness
                                                * 2.40F
                                : 1.35F
                                        + speedAbs
                                                * 0.021F
                                        + dullness
                                                * 1.35F;
            }
        }

        float granted =
                requestedPower <= 0.0F
                        ? 0.0F
                        : manual
                                && source == null
                                ? Math.min(
                                        requestedPower,
                                        1.85F
                                )
                                : source == null
                                        ? 0.0F
                                        : source.consumePower(
                                                requestedPower
                                        );

        lastRequestedPower =
                requestedPower;

        lastGrantedPower =
                granted;

        lastPowerRatio =
                requestedPower <= 0.001F
                        ? 0.0F
                        : Mth.clamp(
                                granted
                                        / requestedPower,
                                0.0F,
                                1.0F
                        );

        boolean spinning =
                !jammed
                        && (source != null
                        || manual)
                        && mechanicallyComplete
                        && granted > 0.035F
                        && speedAbs > 0.5F;

        float transmissionFactor =
                Mth.clamp(
                        bladePerformance
                                * 0.58F
                                + (
                                manual
                                        && shaftProfile == null
                                        ? 0.19F
                                        : shaftPerformance
                                                * 0.27F
                        )
                                + bodyPerformance
                                        * 0.15F,
                        0.25F,
                        1.0F
                );

        float effectiveTarget =
                spinning
                        ? targetRpm
                                * (
                                0.70F
                                        + transmissionFactor
                                                * 0.30F
                        )
                        : 0.0F;

        rpm +=
                (
                        effectiveTarget
                                - rpm
                )
                        * (
                        jammed
                                ? 0.62F
                                : 0.30F
                );

        if (Math.abs(
                rpm
        ) < 0.01F
                && Math.abs(
                effectiveTarget
        ) < 0.01F) {

            rpm =
                    0.0F;
        }

        bladeAngle =
                wrap(
                        bladeAngle
                                + rpm
                                        * 0.30F
                );

        float overSpeed =
                Math.max(
                        0.0F,
                        (
                                Math.abs(
                                        rpm
                                )
                                        - SAFE_RPM
                        )
                                / SAFE_RPM
                );

        float shaftAlignment =
                shaftProfile == null
                        ? (
                        manual
                                ? 0.72F
                                : 0.0F
                )
                        : shaftProfile.alignment();

        float alignmentError =
                mechanicallyComplete
                        ? 1.0F
                                - (
                                bladeProfile.alignment()
                                        * 0.55F
                                        + shaftAlignment
                                                * 0.30F
                                        + bodyPerformance
                                                * 0.15F
                        )
                        : 1.0F;

        float starvation =
                input.isEmpty()
                        ? 0.0F
                        : 1.0F
                                - lastPowerRatio;

        float targetVibration =
                Mth.clamp(
                        alignmentError
                                * 0.68F
                                + starvation
                                        * 0.24F
                                + overSpeed
                                        * 0.72F
                                + (
                                bladeProfile == null
                                        ? 0.0F
                                        : bladeProfile.fatigue()
                                                * 0.32F
                        ),
                        0.0F,
                        1.0F
                );

        vibration +=
                (
                        targetVibration
                                - vibration
                )
                        * 0.12F;

        float targetHeat =
                spinning
                        ? Mth.clamp(
                        Math.abs(
                                rpm
                        )
                                / 70.0F
                                * (
                                0.34F
                                        + (
                                        input.isEmpty()
                                                ? 0.08F
                                                : 0.48F
                                )
                        )
                                + dullness
                                        * 0.28F,
                        0.0F,
                        1.0F
                )
                        : 0.0F;

        heat +=
                (
                        targetHeat
                                - heat
                )
                        * (
                        spinning
                                ? 0.035F
                                : 0.018F
                );

        if (spinning) {
            emitOperatingFeedback(
                    server
            );
        }

        if (spinning
                && !input.isEmpty()
                && longBatchReady
                && (
                !longLine
                        || source != null
        )) {

            float speedQuality =
                    Mth.clamp(
                            1.0F
                                    - Math.abs(
                                    Math.abs(
                                            rpm
                                    )
                                            - OPTIMAL_RPM
                            )
                                    / 48.0F,
                            0.25F,
                            1.0F
                    );

            float drivePerformance =
                    shaftProfile == null
                            && manual
                            ? 0.68F
                            : shaftPerformance;

            float cuttingFactor =
                    bladePerformance
                            * 0.52F
                            + drivePerformance
                                    * 0.18F
                            + lastPowerRatio
                                    * 0.20F
                            + speedQuality
                                    * 0.10F;

            progress +=
                    (
                            0.0034F
                                    + Math.min(
                                    60.0F,
                                    Math.abs(
                                            rpm
                                    )
                            )
                                    * 0.00036F
                    )
                            * Mth.clamp(
                            cuttingFactor,
                            0.18F,
                            1.05F
                    )
                            * (
                            longLine
                                    ? 0.74F
                                    : 1.0F
                    );

            if (Math.floorMod(
                    server.getGameTime()
                            + worldPosition.asLong(),
                    20
            ) == 0) {

                applyMachineWear(
                        server,
                        overSpeed
                );
            }

            float jamThreshold =
                    longLine
                            ? 0.54F
                            : 0.64F;

            float jamChance =
                    longLine
                            ? 0.028F
                            : 0.015F;

            if (vibration > jamThreshold
                    && server.random.nextFloat()
                    < (
                    vibration
                            - (
                            jamThreshold
                                    - 0.04F
                    )
            )
                    * jamChance) {

                jam(
                        server,
                        "vibration"
                );
            }

            if (progress >= 1.0F) {
                finishCut(
                        server
                );
            }
        }

        if (Math.floorMod(
                server.getGameTime()
                        + worldPosition.asLong(),
                4
        ) == 0
                && (
                Math.abs(
                        rpm
                ) > 0.01F
                        || progress > 0.0F
                        || vibration > 0.04F
                        || heat > 0.04F
                        || jammed
        )) {

            sync();
        }
    }

    private void emitOperatingFeedback(
            ServerLevel server
    ) {
        long time =
                server.getGameTime();

        float speed =
                Math.min(
                        70.0F,
                        Math.abs(
                                rpm
                        )
                );

        if (input.isEmpty()) {
            if (Math.floorMod(
                    time
                            + worldPosition.asLong(),
                    24
            ) == 0) {

                server.playSound(
                        null,
                        worldPosition,
                        SoundEvents.GRINDSTONE_USE,
                        SoundSource.BLOCKS,
                        0.13F
                                + Math.min(
                                0.08F,
                                speed
                                        / 500.0F
                        ),
                        0.58F
                                + Math.min(
                                0.34F,
                                speed
                                        / 150.0F
                        )
                );
            }

            return;
        }

        /*
         * Real cutting has two layers: the metallic blade tone and a softer
         * wood impact. Their cadence speeds up slightly with RPM.
         */
        int bladeInterval =
                Mth.clamp(
                        11
                                - Math.round(
                                speed
                                        / 14.0F
                        ),
                        5,
                        11
                );

        if (Math.floorMod(
                time
                        + worldPosition.asLong(),
                bladeInterval
        ) == 0) {

            server.playSound(
                    null,
                    worldPosition,
                    SoundEvents.GRINDSTONE_USE,
                    SoundSource.BLOCKS,
                    0.24F
                            + vibration
                                    * 0.15F,
                    0.84F
                            + Math.min(
                            0.34F,
                            speed
                                    / 170.0F
                    )
            );
        }

        if (Math.floorMod(
                time
                        + worldPosition.asLong(),
                12
        ) == 0) {

            server.playSound(
                    null,
                    worldPosition,
                    SoundEvents.WOOD_HIT,
                    SoundSource.BLOCKS,
                    0.18F
                            + lastPowerRatio
                                    * 0.10F,
                    0.90F
                            + server.random.nextFloat()
                                    * 0.14F
            );
        }

        if (Math.floorMod(
                time
                        + worldPosition.asLong(),
                4
        ) == 0) {

            Block dustBlock =
                    Block.byItem(
                            input.getItem()
                    );

            BlockState dustState =
                    dustBlock == Blocks.AIR
                            ? Blocks.OAK_LOG
                            .defaultBlockState()
                            : dustBlock
                            .defaultBlockState();

            Direction facing =
                    getBlockState()
                            .getValue(
                                    SawmillBlock.FACING
                            );

            double sideX =
                    facing.getStepZ()
                            * 0.18;

            double sideZ =
                    -facing.getStepX()
                            * 0.18;

            server.sendParticles(
                    new BlockParticleOption(
                            ParticleTypes.BLOCK,
                            dustState
                    ),
                    worldPosition.getX()
                            + 0.5
                            + sideX,
                    worldPosition.getY()
                            + 0.55,
                    worldPosition.getZ()
                            + 0.5
                            + sideZ,
                    4
                            + Math.round(
                            speed
                                    / 20.0F
                    ),
                    0.16,
                    0.08,
                    0.16,
                    0.025
                            + speed
                                    * 0.00025
            );
        }

        if (heat > 0.72F
                && Math.floorMod(
                time
                        + worldPosition.asLong(),
                10
        ) == 0) {

            server.sendParticles(
                    ParticleTypes.SMOKE,
                    worldPosition.getX()
                            + 0.5,
                    worldPosition.getY()
                            + 0.66,
                    worldPosition.getZ()
                            + 0.5,
                    1,
                    0.06,
                    0.03,
                    0.06,
                    0.006
            );
        }
    }

    private void applyMachineWear(
            ServerLevel server,
            float overSpeed
    ) {
        AssemblyPartProfile bladeProfile =
                bladeProfile();

        AssemblyPartProfile shaftProfile =
                shaftProfile();

        AssemblyPartProfile bodyProfile =
                bodyProfile();

        float load =
                Mth.clamp(
                        lastRequestedPower
                                / 4.0F,
                        0.0F,
                        1.5F
                );

        if (bladeProfile != null) {
            bladeProfile.applyWear(
                    0.0011F
                            * (
                            0.35F
                                    + load
                                            * 0.65F
                    )
                            * (
                            1.0F
                                    + overSpeed
                                            * 2.2F
                                    + heat
                                            * 0.7F
                    )
            );

            AssemblyItemData.writePart(
                    blade,
                    bladeProfile
            );
        }

        if (shaftProfile != null) {
            shaftProfile.applyWear(
                    0.00034F
                            * (
                            0.4F
                                    + load
                    )
                            * (
                            1.0F
                                    + vibration
                                            * 1.4F
                    )
            );

            AssemblyItemData.writePart(
                    driveShaft,
                    shaftProfile
            );
        }

        if (bodyProfile != null) {
            bodyProfile.applyWear(
                    0.000055F
                            * (
                            0.35F
                                    + vibration
                                            * 1.65F
                    )
            );

            AssemblyItemData.writePart(
                    body,
                    bodyProfile
            );
        }

        if (bladeProfile != null
                && bladeProfile.durabilityScore()
                < 0.055F
                && server.random.nextFloat()
                < 0.035F) {

            breakBlade(
                    server
            );

        } else if (shaftProfile != null
                && shaftProfile.durabilityScore()
                < 0.045F
                && server.random.nextFloat()
                < 0.018F) {

            breakShaft(
                    server
            );
        }
    }

    private void breakBlade(
            ServerLevel server
    ) {
        blade =
                ItemStack.EMPTY;

        jammed =
                true;

        progress =
                Math.max(
                        0.0F,
                        progress
                                - 0.16F
                );

        Block.popResource(
                server,
                worldPosition.above(),
                new ItemStack(
                        Items.IRON_NUGGET,
                        2
                )
        );

        server.playSound(
                null,
                worldPosition,
                SoundEvents.ANVIL_BREAK,
                SoundSource.BLOCKS,
                0.9F,
                1.45F
        );

        sync();
    }

    private void breakShaft(
            ServerLevel server
    ) {
        driveShaft =
                ItemStack.EMPTY;

        jammed =
                true;

        Block.popResource(
                server,
                worldPosition.above(),
                new ItemStack(
                        Items.IRON_NUGGET,
                        1
                )
        );

        server.playSound(
                null,
                worldPosition,
                SoundEvents.ANVIL_BREAK,
                SoundSource.BLOCKS,
                0.7F,
                0.78F
        );

        sync();
    }

    private void jam(
            ServerLevel server,
            String reason
    ) {
        if (jammed) {
            return;
        }

        jammed =
                true;

        server.playSound(
                null,
                worldPosition,
                SoundEvents.IRON_DOOR_CLOSE,
                SoundSource.BLOCKS,
                0.85F,
                0.54F
        );

        sync();
    }

    @Nullable
    private IRotationalPower findBestSource() {
        if (level == null) {
            return null;
        }

        IRotationalPower best =
                null;

        float bestScore =
                -1.0F;

        for (Direction direction :
                Direction.values()) {

            IRotationalPower source =
                    MechanicalTransmission.findSource(
                            level,
                            worldPosition,
                            direction
                    );

            float score =
                    MechanicalTransmission.sourceScore(
                            source
                    );

            if (score > bestScore) {
                bestScore =
                        score;

                best =
                        source;
            }
        }

        return best;
    }

    public void restoreBodyFromItem(
            ItemStack stack
    ) {
        body =
                stack.copyWithCount(
                        1
                );

        if (level instanceof ServerLevel server) {
            AssemblyItemData.ensurePart(
                    body,
                    AssemblyPartProfile.Kind.FRAME,
                    0,
                    server.random
            );
        }

        sync();
    }

    public void installBlade(
            Player player,
            ItemStack stack
    ) {
        if (bladeInstalled()) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.sawmill.blade_present"
                    ),
                    true
            );
            return;
        }

        blade =
                stack.copyWithCount(
                        1
                );

        if (level instanceof ServerLevel server) {
            AssemblyItemData.ensurePart(
                    blade,
                    AssemblyPartProfile.Kind.BLADE,
                    0,
                    server.random
            );
        }

        if (!player.getAbilities()
                .instabuild) {

            stack.consume(
                    1,
                    player
            );
        }

        jammed =
                false;

        playAssemblySound(
                SoundEvents.ANVIL_PLACE
        );

        sync();

        player.displayClientMessage(
                Component.translatable(
                        "message.wayaround.sawmill.blade_installed"
                ),
                true
        );
    }

    public void installShaft(
            Player player,
            ItemStack stack
    ) {
        if (!bladeInstalled()) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.sawmill.need_blade"
                    ),
                    true
            );
            return;
        }

        if (shaftInstalled()) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.sawmill.shaft_present"
                    ),
                    true
            );
            return;
        }

        driveShaft =
                stack.copyWithCount(
                        1
                );

        if (level instanceof ServerLevel server) {
            AssemblyItemData.ensurePart(
                    driveShaft,
                    AssemblyPartProfile.Kind.SHAFT,
                    0,
                    server.random
            );
        }

        if (!player.getAbilities()
                .instabuild) {

            stack.consume(
                    1,
                    player
            );
        }

        jammed =
                false;

        playAssemblySound(
                SoundEvents.ANVIL_PLACE
        );

        sync();

        player.displayClientMessage(
                Component.translatable(
                        "message.wayaround.sawmill.shaft_installed"
                ),
                true
        );
    }

    public void service(
            Player player
    ) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }

        ensureProfiles(
                server
        );

        boolean wasJammed =
                jammed;

        jammed =
                false;

        AssemblyPartProfile bladeProfile =
                bladeProfile();

        AssemblyPartProfile shaftProfile =
                shaftProfile();

        AssemblyPartProfile bodyProfile =
                bodyProfile();

        if (bladeProfile != null) {
            bladeProfile.service(
                    server.random,
                    0.56F
            );

            AssemblyItemData.writePart(
                    blade,
                    bladeProfile
            );
        }

        if (shaftProfile != null) {
            shaftProfile.service(
                    server.random,
                    0.42F
            );

            AssemblyItemData.writePart(
                    driveShaft,
                    shaftProfile
            );
        }

        if (bodyProfile != null) {
            bodyProfile.service(
                    server.random,
                    0.28F
            );

            AssemblyItemData.writePart(
                    body,
                    bodyProfile
            );
        }

        vibration *=
                0.35F;

        heat *=
                0.72F;

        server.playSound(
                null,
                worldPosition,
                SoundEvents.ANVIL_USE,
                SoundSource.BLOCKS,
                0.55F,
                wasJammed
                        ? 0.72F
                        : 1.08F
        );

        player.displayClientMessage(
                Component.translatable(
                        wasJammed
                                ? "message.wayaround.sawmill.unjammed"
                                : "message.wayaround.sawmill.serviced"
                ),
                true
        );

        sync();
    }

    public void insertLog(
            Player player,
            ItemStack stack
    ) {
        if (!bladeInstalled()) {

            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.sawmill.incomplete"
                    ),
                    true
            );

            return;
        }

        if (jammed) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.sawmill.jammed"
                    ),
                    true
            );

            return;
        }

        if (!stack.is(
                ItemTags.LOGS
        )) {
            return;
        }

        boolean longLine =
                longTableMode()
                        && selectedRecipe
                                == SawmillRecipe.WOOD;

        if (!longLine) {
            if (!input.isEmpty()) {
                player.displayClientMessage(
                        Component.translatable(
                                "message.wayaround.sawmill.busy"
                        ),
                        true
                );

                return;
            }

            input =
                    stack.copyWithCount(
                            1
                    );

            progress =
                    0.0F;

            if (!player.getAbilities()
                    .instabuild) {

                stack.consume(
                        1,
                        player
                );
            }

            sync();

            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.sawmill.log_inserted"
                    ),
                    true
            );

            return;
        }

        if (!input.isEmpty()
                && input.getItem()
                        != stack.getItem()) {

            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.sawmill.long_mixed_logs"
                    ),
                    true
            );

            return;
        }

        int existing =
                input.isEmpty()
                        ? 0
                        : input.getCount();

        int needed =
                Math.max(
                        0,
                        4 - existing
                );

        if (needed <= 0) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.sawmill.long_batch_full"
                    ),
                    true
            );

            return;
        }

        int added =
                Math.min(
                        needed,
                        stack.getCount()
                );

        if (added <= 0) {
            return;
        }

        if (input.isEmpty()) {
            input =
                    stack.copyWithCount(
                            added
                    );
        } else {
            input.grow(
                    added
            );
        }

        progress =
                0.0F;

        if (!player.getAbilities()
                .instabuild) {
            stack.consume(
                    added,
                    player
            );
        }

        sync();

        player.displayClientMessage(
                Component.translatable(
                        "message.wayaround.sawmill.long_batch",
                        input.getCount(),
                        4
                ),
                true
        );
    }

    public void removeLast(
            Player player
    ) {
        if (!input.isEmpty()) {
            giveOrDrop(
                    player,
                    input.copy()
            );

            input =
                    ItemStack.EMPTY;

            progress =
                    0.0F;

            sync();
            return;
        }

        if (shaftInstalled()) {
            giveOrDrop(
                    player,
                    driveShaft.copy()
            );

            driveShaft =
                    ItemStack.EMPTY;

            jammed =
                    false;

            sync();
            return;
        }

        if (bladeInstalled()) {
            giveOrDrop(
                    player,
                    blade.copy()
            );

            blade =
                    ItemStack.EMPTY;

            jammed =
                    false;

            sync();
            return;
        }

        describe(
                player
        );
    }

    public void describe(
            Player player
    ) {
        String stage;

        if (!bladeInstalled()) {
            stage =
                    Component.translatable(
                            "message.wayaround.sawmill.stage_body"
                    ).getString();

        } else if (!shaftInstalled()) {
            stage =
                    Component.translatable(
                            "message.wayaround.sawmill.stage_blade"
                    ).getString();

        } else if (jammed) {
            stage =
                    Component.translatable(
                            "message.wayaround.sawmill.stage_jammed"
                    ).getString();

        } else {
            stage =
                    Component.translatable(
                            longTableMode()
                                    ? "message.wayaround.sawmill.stage_long"
                                    : "message.wayaround.sawmill.stage_ready"
                    ).getString();
        }

        player.displayClientMessage(
                Component.translatable(
                        "message.wayaround.sawmill.status_v1",
                        stage,
                        String.format(
                                java.util.Locale.ROOT,
                                "%.1f",
                                rpm
                        ),
                        Math.round(
                                progress
                                        * 100.0F
                        ),
                        Math.round(
                                partCondition(
                                        bladeProfile()
                                )
                                        * 100.0F
                        ),
                        Math.round(
                                partCondition(
                                        shaftProfile()
                                )
                                        * 100.0F
                        ),
                        Math.round(
                                vibration
                                        * 100.0F
                        ),
                        Math.round(
                                heat
                                        * 100.0F
                        ),
                        Math.round(
                                lastPowerRatio
                                        * 100.0F
                        )
                ),
                true
        );
    }

    private void finishCut(
            ServerLevel server
    ) {
        if (input.isEmpty()) {
            return;
        }

        Item outputItem =
                switch (selectedRecipe) {
                    case WOOD ->
                            matchingPlanks(
                                    input
                            );

                    case WATER_WHEEL_BOARD ->
                            PowerContent.WATER_WHEEL_BLADE_ITEM.get();

                    case SHAFT ->
                            PowerContent.MECHANICAL_SHAFT_ITEM.get();

                    case WATER_WHEEL_BODY ->
                            PowerContent.WATER_WHEEL_HUB_ITEM.get();
                };

        float quality =
                currentCutQuality();

        int count =
                switch (selectedRecipe) {
                    case WOOD ->
                            longTableMode()
                                    ? 64
                                    : 10;
                    case WATER_WHEEL_BOARD -> 4;
                    case SHAFT -> 2;
                    case WATER_WHEEL_BODY -> 1;
                };

        ItemStack output =
                new ItemStack(
                        outputItem,
                        count
                );

        ResourceLocation outputId =
                BuiltInRegistries.ITEM.getKey(
                        outputItem
                );

        AssemblyPartProfile boardProfile =
                AssemblyPartProfile.manufactured(
                        AssemblyPartProfile.Kind.BOARD,
                        AssemblyPartProfile.Material.WOOD,
                        outputId,
                        0,
                        quality,
                        Mth.clamp(
                                0.44F
                                        + quality
                                                * 0.54F
                                        - vibration
                                                * 0.16F,
                                0.0F,
                                1.0F
                        ),
                        Mth.clamp(
                                0.52F
                                        + quality
                                                * 0.44F,
                                0.0F,
                                1.0F
                        ),
                        0.42F,
                        0.0F,
                        Mth.clamp(
                                vibration
                                        * 0.04F
                                        + heat
                                                * 0.025F,
                                0.0F,
                                0.12F
                        )
                );

        if (selectedRecipe
                == SawmillRecipe.WATER_WHEEL_BOARD) {
            AssemblyItemData.writePart(
                    output,
                    boardProfile
            );

        } else if (selectedRecipe
                == SawmillRecipe.SHAFT) {
            AssemblyPartProfile shaftProfile =
                    AssemblyPartProfile.manufactured(
                            AssemblyPartProfile.Kind.SHAFT,
                            AssemblyPartProfile.Material.WOOD,
                            outputId,
                            0,
                            quality,
                            Mth.clamp(
                                    0.50F
                                            + quality
                                                    * 0.45F
                                            - vibration
                                                    * 0.12F,
                                    0.0F,
                                    1.0F
                            ),
                            Mth.clamp(
                                    0.56F
                                            + quality
                                                    * 0.40F,
                                    0.0F,
                                    1.0F
                            ),
                            0.52F,
                            0.0F,
                            Mth.clamp(
                                    vibration
                                            * 0.035F,
                                    0.0F,
                                    0.10F
                            )
                    );

            AssemblyItemData.writePart(
                    output,
                    shaftProfile
            );

        } else if (selectedRecipe
                == SawmillRecipe.WATER_WHEEL_BODY) {
            AssemblyPartProfile frameProfile =
                    AssemblyPartProfile.manufactured(
                            AssemblyPartProfile.Kind.FRAME,
                            AssemblyPartProfile.Material.WOOD,
                            outputId,
                            0,
                            quality,
                            Mth.clamp(
                                    0.52F
                                            + quality
                                                    * 0.44F,
                                    0.0F,
                                    1.0F
                            ),
                            Mth.clamp(
                                    0.58F
                                            + quality
                                                    * 0.37F,
                                    0.0F,
                                    1.0F
                            ),
                            0.72F,
                            0.0F,
                            Mth.clamp(
                                    vibration
                                            * 0.025F,
                                    0.0F,
                                    0.08F
                            )
                    );

            AssemblyItemData.writePart(
                    output,
                    frameProfile
            );
        }

        if (selectedRecipe
                != SawmillRecipe.WOOD) {
            AssemblyItemData.writeProcessStamp(
                    output,
                    "sawmill",
                    quality,
                    machineCondition(),
                    server.getGameTime()
            );
        }

        Block.popResource(
                server,
                worldPosition.above(),
                output
        );

        server.playSound(
                null,
                worldPosition,
                SoundEvents.WOOD_BREAK,
                SoundSource.BLOCKS,
                0.65F,
                1.02F
                        + quality
                                * 0.22F
        );

        input =
                ItemStack.EMPTY;

        progress =
                0.0F;

        completedCuts++;

        sync();
    }

    private float currentCutQuality() {
        AssemblyPartProfile bladeProfile =
                bladeProfile();

        AssemblyPartProfile shaftProfile =
                shaftProfile();

        float bladeQuality =
                bladeProfile == null
                        ? 0.20F
                        : bladeProfile.assemblyScore();

        float shaftQuality =
                shaftProfile == null
                        ? 0.20F
                        : shaftProfile.assemblyScore();

        float speedQuality =
                Mth.clamp(
                        1.0F
                                - Math.abs(
                                Math.abs(
                                        rpm
                                )
                                        - OPTIMAL_RPM
                        )
                                / 42.0F,
                        0.20F,
                        1.0F
                );

        return Mth.clamp(
                bladeQuality
                        * 0.42F
                        + shaftQuality
                                * 0.17F
                        + lastPowerRatio
                                * 0.16F
                        + speedQuality
                                * 0.13F
                        + (
                        1.0F
                                - vibration
                )
                                * 0.08F
                        + (
                        1.0F
                                - heat
                )
                                * 0.04F,
                0.12F,
                1.0F
        );
    }

    private float machineCondition() {
        return Mth.clamp(
                partCondition(
                        bladeProfile()
                )
                        * 0.46F
                        + partCondition(
                        shaftProfile()
                )
                                * 0.26F
                        + partCondition(
                        bodyProfile()
                )
                                * 0.18F
                        + (
                        1.0F
                                - vibration
                )
                                * 0.10F,
                0.0F,
                1.0F
        );
    }

    private static float partCondition(
            @Nullable AssemblyPartProfile profile
    ) {
        return profile == null
                ? 0.0F
                : profile.durabilityScore();
    }

    private void ensureProfiles(
            ServerLevel server
    ) {
        if (body.isEmpty()) {
            body =
                    new ItemStack(
                            PowerContent.SAWMILL_ITEM.get()
                    );
        }

        AssemblyItemData.ensurePart(
                body,
                AssemblyPartProfile.Kind.FRAME,
                0,
                server.random
        );

        if (!blade.isEmpty()) {
            AssemblyItemData.ensurePart(
                    blade,
                    AssemblyPartProfile.Kind.BLADE,
                    0,
                    server.random
            );
        }

        if (!driveShaft.isEmpty()) {
            AssemblyItemData.ensurePart(
                    driveShaft,
                    AssemblyPartProfile.Kind.SHAFT,
                    0,
                    server.random
            );
        }
    }

    @Nullable
    private AssemblyPartProfile bladeProfile() {
        return blade.isEmpty()
                ? null
                : AssemblyItemData.readPart(
                        blade
                );
    }

    @Nullable
    private AssemblyPartProfile shaftProfile() {
        return driveShaft.isEmpty()
                ? null
                : AssemblyItemData.readPart(
                        driveShaft
                );
    }

    @Nullable
    private AssemblyPartProfile bodyProfile() {
        return body.isEmpty()
                ? null
                : AssemblyItemData.readPart(
                        body
                );
    }

    private Item matchingPlanks(
            ItemStack stack
    ) {
        String path =
                matchingPath(
                        stack
                );

        if (path == null) {
            return Items.OAK_PLANKS;
        }

        ResourceLocation source =
                BuiltInRegistries.ITEM.getKey(
                        stack.getItem()
                );

        ResourceLocation id =
                ResourceLocation.fromNamespaceAndPath(
                        source.getNamespace(),
                        path
                );

        Item item =
                BuiltInRegistries.ITEM.get(
                        id
                );

        return item == Items.AIR
                ? Items.OAK_PLANKS
                : item;
    }

    @Nullable
    private String matchingPath(
            ItemStack stack
    ) {
        ResourceLocation id =
                BuiltInRegistries.ITEM.getKey(
                        stack.getItem()
                );

        if (id == null) {
            return null;
        }

        String path =
                id.getPath();

        if (path.endsWith(
                "_log"
        )) {
            return path.substring(
                    0,
                    path.length()
                            - 4
            )
                    + "_planks";
        }

        if (path.endsWith(
                "_wood"
        )) {
            return path.substring(
                    0,
                    path.length()
                            - 5
            )
                    + "_planks";
        }

        if (path.endsWith(
                "_stem"
        )) {
            return path.substring(
                    0,
                    path.length()
                            - 5
            )
                    + "_planks";
        }

        if (path.endsWith(
                "_hyphae"
        )) {
            return path.substring(
                    0,
                    path.length()
                            - 7
            )
                    + "_planks";
        }

        return null;
    }

    private void giveOrDrop(
            Player player,
            ItemStack stack
    ) {
        if (!player.addItem(
                stack
        )
                && level != null) {

            Block.popResource(
                    level,
                    worldPosition,
                    stack
            );
        }
    }

    private void playAssemblySound(
            net.minecraft.sounds.SoundEvent sound
    ) {
        if (level != null) {
            level.playSound(
                    null,
                    worldPosition,
                    sound,
                    SoundSource.BLOCKS,
                    0.7F,
                    1.0F
            );
        }
    }

    public void crank(
            Player player
    ) {
        if (!bladeInstalled()) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.sawmill.need_blade"
                    ),
                    true
            );
            return;
        }

        if (jammed) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.sawmill.jammed"
                    ),
                    true
            );
            return;
        }

        manualCrankTicks =
                Math.max(
                        manualCrankTicks,
                        40
                );

        if (level != null) {
            level.playSound(
                    null,
                    worldPosition,
                    SoundEvents.LEVER_CLICK,
                    SoundSource.BLOCKS,
                    0.55F,
                    0.82F
            );
        }

        sync();
    }

    public boolean manualCranking() {
        return manualCrankTicks > 0;
    }

    public void selectRecipe(
            int id
    ) {
        selectedRecipe =
                SawmillRecipe.byId(
                        id
                );

        progress =
                0.0F;

        sync();
    }

    public int selectedRecipeId() {
        return selectedRecipe.ordinal();
    }

    public ContainerData menuData() {
        return menuData;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(
                "container.wayaround.sawmill"
        );
    }

    @Override
    public AbstractContainerMenu createMenu(
            int id,
            Inventory inventory,
            Player player
    ) {
        return new SawmillMenu(
                id,
                inventory,
                this
        );
    }

    public void dropAssembly() {
        if (level == null) {
            return;
        }

        if (!input.isEmpty()) {
            Block.popResource(
                    level,
                    worldPosition,
                    input.copy()
            );
        }

        if (!driveShaft.isEmpty()) {
            Block.popResource(
                    level,
                    worldPosition,
                    driveShaft.copy()
            );
        }

        if (!blade.isEmpty()) {
            Block.popResource(
                    level,
                    worldPosition,
                    blade.copy()
            );
        }

        ItemStack bodyDrop =
                body.isEmpty()
                        ? new ItemStack(
                        PowerContent.SAWMILL_ITEM.get()
                )
                        : body.copy();

        Block.popResource(
                level,
                worldPosition,
                bodyDrop
        );

        input =
                ItemStack.EMPTY;

        driveShaft =
                ItemStack.EMPTY;

        blade =
                ItemStack.EMPTY;

        body =
                ItemStack.EMPTY;
    }

    public boolean longTableMode() {
        if (level == null
                || !getBlockState().hasProperty(
                SawmillBlock.FACING
        )) {
            return false;
        }

        Direction facing =
                getBlockState().getValue(
                        SawmillBlock.FACING
                );

        Direction backward =
                facing.getOpposite();

        return isTableExtension(
                worldPosition.relative(
                        facing,
                        1
                )
        )
                && isTableExtension(
                worldPosition.relative(
                        facing,
                        2
                )
        )
                && isTableExtension(
                worldPosition.relative(
                        backward,
                        1
                )
        )
                && isTableExtension(
                worldPosition.relative(
                        backward,
                        2
                )
        );
    }

    private boolean isTableExtension(
            BlockPos pos
    ) {
        return level != null
                && level.getBlockState(
                pos
        ).is(
                PowerContent.SAWMILL_TABLE_EXTENSION.get()
        );
    }

    public int inputCount() {
        return input.getCount();
    }

    public boolean bladeInstalled() {
        return !blade.isEmpty();
    }

    public boolean shaftInstalled() {
        return !driveShaft.isEmpty();
    }

    public boolean hasInput() {
        return !input.isEmpty();
    }

    public Item inputItem() {
        return input.isEmpty()
                ? Items.AIR
                : input.getItem();
    }

    public float progress() {
        return progress;
    }

    public float rpm() {
        return rpm;
    }

    public float bladeAngle() {
        return bladeAngle;
    }

    public float vibration() {
        return vibration;
    }

    public float heat() {
        return heat;
    }

    public boolean jammed() {
        return jammed;
    }

    public enum SawmillRecipe {
        WOOD,
        WATER_WHEEL_BOARD,
        SHAFT,
        WATER_WHEEL_BODY;

        public static SawmillRecipe byId(
                int id
        ) {
            SawmillRecipe[] values =
                    values();

            return values[
                    Mth.clamp(
                            id,
                            0,
                            values.length - 1
                    )
                    ];
        }
    }

    private static float wrap(
            float value
    ) {
        value %=
                360.0F;

        if (value < 0.0F) {
            value +=
                    360.0F;
        }

        return value;
    }

    private void sync() {
        setChanged();

        if (level != null) {
            BlockState state =
                    getBlockState();

            level.sendBlockUpdated(
                    worldPosition,
                    state,
                    state,
                    3
            );
        }
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(
                tag,
                registries
        );

        tag.putFloat(
                "Progress",
                progress
        );

        tag.putFloat(
                "Rpm",
                rpm
        );

        tag.putFloat(
                "BladeAngle",
                bladeAngle
        );

        tag.putFloat(
                "Vibration",
                vibration
        );

        tag.putFloat(
                "Heat",
                heat
        );

        tag.putFloat(
                "PowerRatio",
                lastPowerRatio
        );

        tag.putFloat(
                "RequestedPower",
                lastRequestedPower
        );

        tag.putFloat(
                "GrantedPower",
                lastGrantedPower
        );

        tag.putInt(
                "CompletedCuts",
                completedCuts
        );

        tag.putBoolean(
                "Jammed",
                jammed
        );

        tag.putInt(
                "SawmillRecipe",
                selectedRecipe.ordinal()
        );

        tag.putInt(
                "ManualCrankTicks",
                manualCrankTicks
        );

        if (!body.isEmpty()) {
            tag.put(
                    "Body",
                    body.saveOptional(
                            registries
                    )
            );
        }

        if (!blade.isEmpty()) {
            tag.put(
                    "Blade",
                    blade.saveOptional(
                            registries
                    )
            );
        }

        if (!driveShaft.isEmpty()) {
            tag.put(
                    "DriveShaft",
                    driveShaft.saveOptional(
                            registries
                    )
            );
        }

        if (!input.isEmpty()) {
            tag.put(
                    "Input",
                    input.saveOptional(
                            registries
                    )
            );
        }
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(
                tag,
                registries
        );

        /*
         * Legacy migration: old V1 saves only stored booleans.
         */
        body =
                tag.contains(
                        "Body",
                        Tag.TAG_COMPOUND
                )
                        ? ItemStack.parseOptional(
                        registries,
                        tag.getCompound(
                                "Body"
                        )
                )
                        : ItemStack.EMPTY;

        blade =
                tag.contains(
                        "Blade",
                        Tag.TAG_COMPOUND
                )
                        ? ItemStack.parseOptional(
                        registries,
                        tag.getCompound(
                                "Blade"
                        )
                )
                        : tag.getBoolean(
                        "BladeInstalled"
                )
                        ? new ItemStack(
                        PowerContent.SAW_BLADE.get()
                )
                        : ItemStack.EMPTY;

        driveShaft =
                tag.contains(
                        "DriveShaft",
                        Tag.TAG_COMPOUND
                )
                        ? ItemStack.parseOptional(
                        registries,
                        tag.getCompound(
                                "DriveShaft"
                        )
                )
                        : tag.getBoolean(
                        "ShaftInstalled"
                )
                        ? new ItemStack(
                        PowerContent.MECHANICAL_SHAFT_ITEM.get()
                )
                        : ItemStack.EMPTY;

        progress =
                Mth.clamp(
                        tag.getFloat(
                                "Progress"
                        ),
                        0.0F,
                        1.0F
                );

        rpm =
                tag.getFloat(
                        "Rpm"
                );

        bladeAngle =
                tag.getFloat(
                        "BladeAngle"
                );

        vibration =
                Mth.clamp(
                        tag.getFloat(
                                "Vibration"
                        ),
                        0.0F,
                        1.0F
                );

        heat =
                Mth.clamp(
                        tag.getFloat(
                                "Heat"
                        ),
                        0.0F,
                        1.0F
                );

        selectedRecipe =
                SawmillRecipe.byId(
                        tag.contains(
                                "SawmillRecipe"
                        )
                                ? tag.getInt(
                                "SawmillRecipe"
                        )
                                : 0
                );

        lastPowerRatio =
                Mth.clamp(
                        tag.getFloat(
                                "PowerRatio"
                        ),
                        0.0F,
                        1.0F
                );

        lastRequestedPower =
                tag.getFloat(
                        "RequestedPower"
                );

        lastGrantedPower =
                tag.getFloat(
                        "GrantedPower"
                );

        completedCuts =
                Math.max(
                        0,
                        tag.getInt(
                                "CompletedCuts"
                        )
                );

        jammed =
                tag.getBoolean(
                        "Jammed"
                );

        input =
                tag.contains(
                        "Input",
                        Tag.TAG_COMPOUND
                )
                        ? ItemStack.parseOptional(
                        registries,
                        tag.getCompound(
                                "Input"
                        )
                )
                        : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries
    ) {
        CompoundTag tag =
                new CompoundTag();

        saveAdditional(
                tag,
                registries
        );

        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(
                this
        );
    }
}
