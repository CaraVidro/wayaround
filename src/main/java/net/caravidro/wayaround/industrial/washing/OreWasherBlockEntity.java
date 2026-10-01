package net.caravidro.wayaround.industrial.washing;

import java.util.Collection;
import java.util.List;

import net.caravidro.wayaround.industrial.assembly.AssemblyConnection;
import net.caravidro.wayaround.industrial.assembly.AssemblyItemData;
import net.caravidro.wayaround.industrial.assembly.AssemblyMachine;
import net.caravidro.wayaround.industrial.assembly.AssemblyPartNode;
import net.caravidro.wayaround.industrial.assembly.AssemblyPartProfile;
import net.caravidro.wayaround.industrial.assembly.LegacyMachineAssembly;
import net.caravidro.wayaround.industrial.crushing.CrusherContent;
import net.caravidro.wayaround.industrial.mechanical.IRotationalPower;
import net.caravidro.wayaround.industrial.mechanical.MechanicalLoad;
import net.caravidro.wayaround.industrial.mechanical.MechanicalTransmission;
import net.caravidro.wayaround.industrial.pipework.PipeFlow;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Powered wet concentrator / trommel.
 *
 * The frame is the placed block. Drum and screen are the only removable
 * assembly pieces; crushed ore is fed by hand for now and water arrives through
 * the normal NeoForge fluid capability, meaning the Pipework network can feed
 * this machine directly.
 */
public final class OreWasherBlockEntity
        extends BlockEntity
        implements AssemblyMachine {

    private static final int WATER_CAPACITY =
            4000;

    private static final int WATER_PER_BATCH =
            250;

    private static final int INPUT_CAPACITY =
            16;

    private static final int OUTPUT_CAPACITY =
            64;

    private static final float POWER_DRAW =
            3.2F;

    private static final float MIN_RPM =
            10.0F;

    private static final int CYCLE_TICKS =
            76;

    private ItemStack drum =
            ItemStack.EMPTY;

    private ItemStack screen =
            ItemStack.EMPTY;

    private ItemStack input =
            ItemStack.EMPTY;

    private ItemStack concentrate =
            ItemStack.EMPTY;

    private int tailings;
    private int water;

    private float rpm;
    private float angle;
    private float progress;
    private float load;
    private float vibration;
    private float assemblyWear;
    private boolean stalled;

    private final IFluidHandler waterInput =
            new IFluidHandler() {

                @Override
                public int getTanks() {
                    return 1;
                }

                @Override
                public FluidStack getFluidInTank(
                        int tank
                ) {
                    return tank == 0
                            && water > 0
                            ? new FluidStack(
                            Fluids.WATER,
                            water
                    )
                            : FluidStack.EMPTY;
                }

                @Override
                public int getTankCapacity(
                        int tank
                ) {
                    return tank == 0
                            ? WATER_CAPACITY
                            : 0;
                }

                @Override
                public boolean isFluidValid(
                        int tank,
                        FluidStack stack
                ) {
                    return tank == 0
                            && !stack.isEmpty()
                            && stack.getFluid()
                            == Fluids.WATER;
                }

                @Override
                public int fill(
                        FluidStack resource,
                        FluidAction action
                ) {
                    if (!isFluidValid(
                            0,
                            resource
                    )) {
                        return 0;
                    }

                    int accepted =
                            Math.min(
                                    resource.getAmount(),
                                    WATER_CAPACITY
                                            - water
                            );

                    if (accepted > 0
                            && action.execute()) {
                        water +=
                                accepted;

                        sync();
                    }

                    return accepted;
                }

                @Override
                public FluidStack drain(
                        FluidStack resource,
                        FluidAction action
                ) {
                    return FluidStack.EMPTY;
                }

                @Override
                public FluidStack drain(
                        int maxDrain,
                        FluidAction action
                ) {
                    return FluidStack.EMPTY;
                }
            };

    public OreWasherBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                OreWashingContent.ORE_WASHER_ENTITY.get(),
                pos,
                state
        );
    }

    public IFluidHandler waterInput() {
        return waterInput;
    }

    public boolean hasDrum() {
        return !drum.isEmpty();
    }

    public boolean hasScreen() {
        return !screen.isEmpty();
    }

    public boolean hasInput() {
        return !input.isEmpty();
    }

    public boolean hasOutput() {
        return !concentrate.isEmpty()
                || tailings > 0;
    }

    public int waterAmount() {
        return water;
    }

    public float rpm() {
        return rpm;
    }

    public float angle() {
        return angle;
    }

    public float progress() {
        return progress;
    }

    public float vibration() {
        return vibration;
    }

    public boolean working() {
        return !stalled
                && hasDrum()
                && hasScreen()
                && hasInput()
                && water >= WATER_PER_BATCH
                && Math.abs(
                rpm
        ) >= MIN_RPM;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            OreWasherBlockEntity washer
    ) {
        if (level instanceof ServerLevel server) {
            washer.tick(
                    server
            );
        }
    }

    private void tick(
            ServerLevel level
    ) {
        if (!WorldFeatureRuntime.enabled(
                level,
                WorldFeature.INDUSTRIAL_MACHINES
        )
                || !WorldFeatureRuntime.enabled(
                level,
                WorldFeature.POWER_NETWORKS
        )
                || !WorldFeatureRuntime.enabled(
                level,
                WorldFeature.ASSEMBLY
        )) {

            rpm +=
                    (
                            0.0F
                                    - rpm
                    )
                            * 0.16F;

            return;
        }

        float oldRpm =
                rpm;

        boolean oldStalled =
                stalled;

        WashRecipe recipe =
                WashRecipe.find(
                        input
                );

        IRotationalPower source =
                findBestSource();

        float requiredTorque =
                recipe == null
                        ? 0.0F
                        : 1.05F
                                + (
                                1.0F
                                        - screenCondition()
                        )
                                * 0.55F;

        MechanicalLoad.Demand demand =
                MechanicalLoad.sample(
                        source,
                        POWER_DRAW,
                        requiredTorque,
                        88.0F
                );

        stalled =
                !hasDrum()
                        || !hasScreen()
                        || drumCondition() < 0.12F
                        || screenCondition() < 0.12F
                        || (
                        source != null
                                && demand.torqueStarved()
                );

        float granted =
                0.0F;

        float targetRpm =
                0.0F;

        if (source != null
                && source.active()
                && hasDrum()
                && hasScreen()) {

            granted =
                    source.consumePower(
                            recipe == null
                                    ? POWER_DRAW * 0.18F
                                    : POWER_DRAW
                    );

            load =
                    MechanicalLoad.fulfillment(
                            recipe == null
                                    ? POWER_DRAW * 0.18F
                                    : POWER_DRAW,
                            granted
                    );

            if (!stalled
                    && granted > 0.01F) {
                targetRpm =
                        source.rpm()
                                * Mth.clamp(
                                granted
                                        / (
                                        recipe == null
                                                ? POWER_DRAW * 0.18F
                                                : POWER_DRAW
                                ),
                                0.0F,
                                1.0F
                        );
            }

        } else {
            load =
                    0.0F;
        }

        rpm +=
                (
                        targetRpm
                                - rpm
                )
                        * 0.13F;

        if (Math.abs(
                rpm
        ) < 0.01F) {
            rpm =
                    0.0F;
        }

        angle =
                wrap(
                        angle
                                + rpm
                                * 0.30F
                );

        vibration =
                Mth.clamp(
                        (
                                1.0F
                                        - drumCondition()
                        )
                                * load
                                * 0.42F
                                + (
                                1.0F
                                        - screenCondition()
                        )
                                * load
                                * 0.32F
                                + demand.overspeed()
                                * 0.18F,
                        0.0F,
                        1.0F
                );

        if (working()
                && recipe != null
                && canOutput(
                recipe
        )) {

            float speed =
                    Mth.clamp(
                            Math.abs(
                                    rpm
                            )
                                    / 36.0F,
                            0.25F,
                            1.65F
                    );

            progress +=
                    speed
                            / CYCLE_TICKS;

            if (Math.floorMod(
                    level.getGameTime()
                            + worldPosition.asLong(),
                    12
            ) == 0) {

                level.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.SPLASH,
                        worldPosition.getX()
                                + 0.5,
                        worldPosition.getY()
                                + 0.68,
                        worldPosition.getZ()
                                + 0.5,
                        3,
                        0.28,
                        0.12,
                        0.28,
                        0.03
                );
            }

            if (progress >= 1.0F) {
                progress -=
                        1.0F;

                finishBatch(
                        level,
                        recipe
                );
            }

        } else if (recipe == null
                || water < WATER_PER_BATCH
                || !canOutput(
                recipe
        )) {

            progress =
                    Math.max(
                            0.0F,
                            progress
                                    - 0.01F
                    );
        }

        if (Math.floorMod(
                level.getGameTime()
                        + worldPosition.asLong(),
                5
        ) == 0
                && (
                Math.abs(
                        oldRpm - rpm
                ) > 0.02F
                        || oldStalled != stalled
        )) {

            sync();

        } else {
            setChanged();
        }
    }

    private void finishBatch(
            ServerLevel level,
            WashRecipe recipe
    ) {
        if (input.isEmpty()
                || water < WATER_PER_BATCH
                || !canOutput(
                recipe
        )) {
            return;
        }

        input.shrink(
                1
        );

        if (input.isEmpty()) {
            input =
                    ItemStack.EMPTY;
        }

        water -=
                WATER_PER_BATCH;

        ItemStack produced =
                new ItemStack(
                        recipe.output(),
                        1
                );

        if (concentrate.isEmpty()) {
            concentrate =
                    produced;

        } else {
            concentrate.grow(
                    1
            );
        }

        tailings =
                Math.min(
                        OUTPUT_CAPACITY,
                        tailings + 1
                );

        applyPartWear(
                0.00055F
                        * Math.max(
                        0.55F,
                        load
                )
        );

        level.playSound(
                null,
                worldPosition,
                SoundEvents.BUCKET_EMPTY,
                SoundSource.BLOCKS,
                0.30F,
                1.18F
        );

        level.playSound(
                null,
                worldPosition,
                SoundEvents.GRAVEL_BREAK,
                SoundSource.BLOCKS,
                0.28F,
                0.76F
        );

        sync();
    }

    private boolean canOutput(
            WashRecipe recipe
    ) {
        if (tailings >= OUTPUT_CAPACITY) {
            return false;
        }

        if (concentrate.isEmpty()) {
            return true;
        }

        return concentrate.is(
                recipe.output()
        )
                && concentrate.getCount()
                < concentrate.getMaxStackSize();
    }

    private IRotationalPower findBestSource() {
        if (level == null) {
            return null;
        }

        Direction facing =
                getBlockState()
                        .getValue(
                                OreWasherBlock.FACING
                        );

        IRotationalPower best =
                null;

        float bestScore =
                -1.0F;

        for (Direction direction :
                new Direction[] {
                        facing.getOpposite(),
                        facing.getClockWise(),
                        facing.getCounterClockWise(),
                        Direction.DOWN
                }) {

            IRotationalPower candidate =
                    MechanicalTransmission.findSource(
                            level,
                            worldPosition,
                            direction
                    );

            float score =
                    MechanicalTransmission.sourceScore(
                            candidate
                    );

            if (score > bestScore) {
                bestScore =
                        score;

                best =
                        candidate;
            }
        }

        return best;
    }

    private float drumCondition() {
        return partCondition(
                drum
        );
    }

    private float screenCondition() {
        return partCondition(
                screen
        );
    }

    private float partCondition(
            ItemStack stack
    ) {
        if (stack.isEmpty()) {
            return 0.0F;
        }

        AssemblyPartProfile profile =
                AssemblyItemData.readPart(
                        stack
                );

        return profile == null
                ? AssemblyItemData.materialCondition(
                        stack
                )
                : profile.durabilityScore()
                        * profile.performanceFactor()
                        * AssemblyItemData.materialCondition(
                        stack
                );
    }

    private void applyPartWear(
            float amount
    ) {
        assemblyWear =
                LegacyMachineAssembly.addWear(
                        assemblyWear,
                        amount,
                        0.76F
                );

        wear(
                drum,
                amount,
                AssemblyPartProfile.Kind.GENERAL,
                AssemblyPartProfile.Material.IRON
        );

        wear(
                screen,
                amount * 1.20F,
                AssemblyPartProfile.Kind.GENERAL,
                AssemblyPartProfile.Material.IRON
        );
    }

    private void wear(
            ItemStack stack,
            float amount,
            AssemblyPartProfile.Kind kind,
            AssemblyPartProfile.Material material
    ) {
        if (stack.isEmpty()) {
            return;
        }

        AssemblyPartProfile profile =
                AssemblyItemData.readPart(
                        stack
                );

        if (profile == null) {
            profile =
                    AssemblyItemData.profileOrCreate(
                            stack,
                            kind,
                            material,
                            0,
                            level.random
                    );
        }

        profile.applyWear(
                amount
        );

        AssemblyItemData.writePart(
                stack,
                profile
        );
    }

    public boolean installPart(
            Player player,
            ItemStack held
    ) {
        if (level == null
                || level.isClientSide
                || Math.abs(
                rpm
        ) > 0.5F) {
            return false;
        }

        if (held.is(
                OreWashingContent.WASHER_DRUM.get()
        )
                && drum.isEmpty()) {

            drum =
                    held.copyWithCount(
                            1
                    );

            AssemblyPartProfile drumProfile =
                    AssemblyItemData.profileOrCreate(
                            drum,
                            AssemblyPartProfile.Kind.GENERAL,
                            AssemblyPartProfile.Material.IRON,
                            0,
                            player.getRandom()
                    );

            AssemblyItemData.writePart(
                    drum,
                    drumProfile
            );

        } else if (held.is(
                OreWashingContent.WASHER_SCREEN.get()
        )
                && screen.isEmpty()) {

            screen =
                    held.copyWithCount(
                            1
                    );

            AssemblyPartProfile screenProfile =
                    AssemblyItemData.profileOrCreate(
                            screen,
                            AssemblyPartProfile.Kind.GENERAL,
                            AssemblyPartProfile.Material.IRON,
                            0,
                            player.getRandom()
                    );

            AssemblyItemData.writePart(
                    screen,
                    screenProfile
            );

        } else {
            describe(
                    player
            );

            return false;
        }

        if (!player.getAbilities()
                .instabuild) {
            held.shrink(
                    1
            );
        }

        level.playSound(
                null,
                worldPosition,
                SoundEvents.ANVIL_PLACE,
                SoundSource.BLOCKS,
                0.38F,
                1.15F
        );

        sync();

        return true;
    }

    public boolean insertOre(
            Player player,
            ItemStack held
    ) {
        WashRecipe recipe =
                WashRecipe.find(
                        held
                );

        if (recipe == null
                || level == null
                || level.isClientSide) {
            return false;
        }

        if (!input.isEmpty()
                && !ItemStack.isSameItemSameComponents(
                input,
                held
        )) {
            describe(
                    player
            );

            return false;
        }

        int room =
                INPUT_CAPACITY
                        - (
                        input.isEmpty()
                                ? 0
                                : input.getCount()
                );

        if (room <= 0) {
            return false;
        }

        int accepted =
                Math.min(
                        room,
                        held.getCount()
                );

        if (input.isEmpty()) {
            input =
                    held.copyWithCount(
                            accepted
                    );

        } else {
            input.grow(
                    accepted
            );
        }

        if (!player.getAbilities()
                .instabuild) {
            held.shrink(
                    accepted
            );
        }

        sync();

        return accepted > 0;
    }

    public boolean collectOutput(
            Player player
    ) {
        if (!hasOutput()) {
            return false;
        }

        if (!concentrate.isEmpty()) {
            ItemStack moving =
                    concentrate.copy();

            if (!player.getInventory()
                    .add(
                            moving
                    )
                    && !moving.isEmpty()) {
                player.drop(
                        moving,
                        false
                );
            }

            concentrate =
                    ItemStack.EMPTY;
        }

        if (tailings > 0) {
            ItemStack waste =
                    new ItemStack(
                            OreWashingContent.MINERAL_TAILINGS.get(),
                            tailings
                    );

            if (!player.getInventory()
                    .add(
                            waste
                    )
                    && !waste.isEmpty()) {
                player.drop(
                        waste,
                        false
                );
            }

            tailings =
                    0;
        }

        sync();

        return true;
    }

    public void removePart(
            Player player
    ) {
        if (Math.abs(
                rpm
        ) > 0.5F) {

            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.machine.stop_first"
                    ),
                    true
            );

            return;
        }

        ItemStack removed;

        if (!screen.isEmpty()) {
            removed =
                    screen;

            screen =
                    ItemStack.EMPTY;

        } else if (!drum.isEmpty()) {
            removed =
                    drum;

            drum =
                    ItemStack.EMPTY;

        } else {
            return;
        }

        if (!player.getInventory()
                .add(
                        removed
                )) {

            player.drop(
                    removed,
                    false
            );
        }

        sync();
    }

    public void describe(
            Player player
    ) {
        player.displayClientMessage(
                Component.translatable(
                        "message.wayaround.ore_washer.status",
                        hasDrum()
                                ? Component.translatable(
                                "message.wayaround.ore_washer.drum_ready"
                        )
                                : Component.translatable(
                                "message.wayaround.ore_washer.drum_missing"
                        ),
                        hasScreen()
                                ? Component.translatable(
                                "message.wayaround.ore_washer.screen_ready"
                        )
                                : Component.translatable(
                                "message.wayaround.ore_washer.screen_missing"
                        ),
                        water,
                        input.isEmpty()
                                ? 0
                                : input.getCount(),
                        Math.round(
                                rpm
                        )
                ),
                true
        );
    }

    public void dropContents() {
        if (level == null
                || level.isClientSide) {
            return;
        }

        for (ItemStack stack :
                List.of(
                        drum,
                        screen,
                        input,
                        concentrate
                )) {

            if (!stack.isEmpty()) {
                Block.popResource(
                        level,
                        worldPosition,
                        stack
                );
            }
        }

        if (tailings > 0) {
            Block.popResource(
                    level,
                    worldPosition,
                    new ItemStack(
                            OreWashingContent.MINERAL_TAILINGS.get(),
                            tailings
                    )
            );
        }

        if (level instanceof ServerLevel server
                && water >= 1000) {

            FluidStack storedWater =
                    new FluidStack(
                            Fluids.WATER,
                            water
                    );

            for (Direction direction :
                    Direction.Plane.HORIZONTAL) {

                if (storedWater.getAmount() < 1000) {
                    break;
                }

                int used =
                        PipeFlow.spill(
                                server,
                                worldPosition.relative(
                                        direction
                                ),
                                storedWater,
                                false
                        );

                if (used > 0) {
                    storedWater.shrink(
                            used
                    );
                }
            }
        }

        drum =
                ItemStack.EMPTY;

        screen =
                ItemStack.EMPTY;

        input =
                ItemStack.EMPTY;

        concentrate =
                ItemStack.EMPTY;

        tailings =
                0;

        water =
                0;
    }

    private void sync() {
        setChanged();

        if (level != null
                && !level.isClientSide) {

            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
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

        if (!drum.isEmpty()) {
            tag.put(
                    "Drum",
                    drum.save(
                            registries
                    )
            );
        }

        if (!screen.isEmpty()) {
            tag.put(
                    "Screen",
                    screen.save(
                            registries
                    )
            );
        }

        if (!input.isEmpty()) {
            tag.put(
                    "Input",
                    input.save(
                            registries
                    )
            );
        }

        if (!concentrate.isEmpty()) {
            tag.put(
                    "Concentrate",
                    concentrate.save(
                            registries
                    )
            );
        }

        tag.putInt(
                "Tailings",
                tailings
        );

        tag.putInt(
                "Water",
                water
        );

        tag.putFloat(
                "Rpm",
                rpm
        );

        tag.putFloat(
                "Angle",
                angle
        );

        tag.putFloat(
                "Progress",
                progress
        );

        tag.putFloat(
                "Load",
                load
        );

        tag.putFloat(
                "Vibration",
                vibration
        );

        tag.putFloat(
                "AssemblyWear",
                assemblyWear
        );

        tag.putBoolean(
                "Stalled",
                stalled
        );
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

        drum =
                ItemStack.parseOptional(
                        registries,
                        tag.getCompound(
                                "Drum"
                        )
                );

        screen =
                ItemStack.parseOptional(
                        registries,
                        tag.getCompound(
                                "Screen"
                        )
                );

        input =
                ItemStack.parseOptional(
                        registries,
                        tag.getCompound(
                                "Input"
                        )
                );

        concentrate =
                ItemStack.parseOptional(
                        registries,
                        tag.getCompound(
                                "Concentrate"
                        )
                );

        tailings =
                Mth.clamp(
                        tag.getInt(
                                "Tailings"
                        ),
                        0,
                        OUTPUT_CAPACITY
                );

        water =
                Mth.clamp(
                        tag.getInt(
                                "Water"
                        ),
                        0,
                        WATER_CAPACITY
                );

        rpm =
                finite(
                        tag.getFloat(
                                "Rpm"
                        ),
                        -240.0F,
                        240.0F
                );

        angle =
                finite(
                        tag.getFloat(
                                "Angle"
                        ),
                        -360.0F,
                        360.0F
                );

        progress =
                finite(
                        tag.getFloat(
                                "Progress"
                        ),
                        0.0F,
                        1.0F
                );

        load =
                finite(
                        tag.getFloat(
                                "Load"
                        ),
                        0.0F,
                        2.0F
                );

        vibration =
                finite(
                        tag.getFloat(
                                "Vibration"
                        ),
                        0.0F,
                        1.0F
                );

        assemblyWear =
                finite(
                        tag.getFloat(
                                "AssemblyWear"
                        ),
                        0.0F,
                        1.0F
                );

        stalled =
                tag.getBoolean(
                        "Stalled"
                );
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

    @Override
    public ResourceLocation assemblyType() {
        return ResourceLocation.fromNamespaceAndPath(
                "wayaround",
                "ore_washer"
        );
    }

    @Override
    public BlockPos assemblyAnchor() {
        return worldPosition;
    }

    @Override
    public Collection<AssemblyPartNode> assemblyParts() {
        ResourceLocation source =
                assemblyType();

        return List.of(
                LegacyMachineAssembly.part(
                        "frame",
                        "washer frame",
                        AssemblyPartProfile.Kind.FRAME,
                        AssemblyPartProfile.Material.IRON,
                        source,
                        assemblyWear * 0.55F,
                        true,
                        1.8F
                ),
                LegacyMachineAssembly.part(
                        "drum",
                        "rotating drum",
                        AssemblyPartProfile.Kind.GENERAL,
                        AssemblyPartProfile.Material.IRON,
                        source,
                        assemblyWear,
                        hasDrum(),
                        1.25F
                ),
                LegacyMachineAssembly.part(
                        "screen",
                        "wash screen",
                        AssemblyPartProfile.Kind.GENERAL,
                        AssemblyPartProfile.Material.IRON,
                        source,
                        assemblyWear * 1.15F,
                        hasScreen(),
                        0.82F
                )
        );
    }

    @Override
    public Collection<AssemblyConnection> assemblyConnections() {
        return List.of(
                new AssemblyConnection(
                        "frame",
                        "drum",
                        AssemblyConnection.Type.BEARING,
                        0.92F,
                        Mth.clamp(
                                assemblyWear,
                                0.0F,
                                1.0F
                        )
                ),
                new AssemblyConnection(
                        "drum",
                        "screen",
                        AssemblyConnection.Type.FASTENED,
                        0.90F,
                        Mth.clamp(
                                assemblyWear * 1.12F,
                                0.0F,
                                1.0F
                        )
                )
        );
    }

    @Override
    public float currentAssemblyLoad() {
        return working()
                ? Math.max(
                0.15F,
                load
        )
                : 0.05F;
    }

    @Override
    public void applyAssemblyWear(
            float fraction
    ) {
        applyPartWear(
                fraction
        );

        sync();
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

    private static float finite(
            float value,
            float min,
            float max
    ) {
        return Float.isFinite(
                value
        )
                ? Mth.clamp(
                value,
                min,
                max
        )
                : 0.0F;
    }

    private record WashRecipe(
            Item output
    ) {

        private static WashRecipe find(
                ItemStack input
        ) {
            if (input.is(
                    CrusherContent.CRUSHED_IRON.get()
            )) {
                return new WashRecipe(
                        OreWashingContent.WASHED_IRON_CONCENTRATE.get()
                );
            }

            if (input.is(
                    CrusherContent.CRUSHED_COPPER.get()
            )) {
                return new WashRecipe(
                        OreWashingContent.WASHED_COPPER_CONCENTRATE.get()
                );
            }

            if (input.is(
                    CrusherContent.CRUSHED_GOLD.get()
            )) {
                return new WashRecipe(
                        OreWashingContent.WASHED_GOLD_CONCENTRATE.get()
                );
            }

            return null;
        }
    }
}
