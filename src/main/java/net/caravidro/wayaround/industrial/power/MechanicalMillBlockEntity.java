package net.caravidro.wayaround.industrial.power;
import net.caravidro.wayaround.performance.PerformanceProfiler;

import java.util.Collection;
import java.util.ArrayList;
import net.caravidro.wayaround.industrial.crushing.*;
import java.util.List;

import javax.annotation.Nullable;

import net.caravidro.wayaround.industrial.assembly.AssemblyConnection;
import net.caravidro.wayaround.industrial.assembly.AssemblyMachine;
import net.caravidro.wayaround.industrial.assembly.AssemblyPartNode;
import net.caravidro.wayaround.industrial.assembly.AssemblyPartProfile;
import net.caravidro.wayaround.industrial.assembly.LegacyMachineAssembly;
import net.caravidro.wayaround.industrial.mechanical.IRotationalPower;
import net.caravidro.wayaround.industrial.mechanical.MechanicalLoad;
import net.caravidro.wayaround.industrial.mechanical.MechanicalTransmission;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class MechanicalMillBlockEntity
        extends BlockEntity
        implements AssemblyMachine {

    private static final float POWER_DRAW =
            1.75F;

    private static final float MIN_WORK_RPM =
            7.5F;

    private static final float SAFE_RPM =
            72.0F;

    private static final int MAX_INPUT =
            16;

    private static final int MAX_OUTPUT =
            64;

    private final MachineParts parts = new MachineParts("mill");

    public MachineParts parts() { return parts; }
    public int inputCapacity() { return parts.has(MachinePartSpec.Role.FEED) ? 8 * parts.feedMultiplier() : 0; }
    public void installPart(Player player, ItemStack stack) {
        if (level == null || level.isClientSide || !WorldFeatureRuntime.enabled(level, WorldFeature.ASSEMBLY)) return;
        if (Math.abs(rpm) > .5F || !parts.install(player, stack)) {
            player.displayClientMessage(Component.translatable("message.wayaround.machine.stop_or_slot"), true); return;
        }
        sync();
    }
    public void removePart(Player player) {
        if (Math.abs(rpm) > .5F) { player.displayClientMessage(Component.translatable("message.wayaround.machine.stop_first"),true); return; }
        ItemStack stack = parts.removeLast();
        if (!stack.isEmpty()) { if (!player.getInventory().add(stack)) player.drop(stack,false); sync(); }
    }

    private int wheatInput;
    private int flourOutput;

    private float progress;
    private float rpm;
    private float rotationDegrees;
    private float lastPower;
    private float assemblyWear;

    private boolean connected;

    public MechanicalMillBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                PowerContent.MECHANICAL_MILL_ENTITY.get(),
                pos,
                state
        );
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            MechanicalMillBlockEntity mill
    ) {
        long wayperfStartedAt =
                PerformanceProfiler.begin(
                        PerformanceProfiler.Section.MACHINE_SIM
                );

        try {
        if (!(level instanceof ServerLevel server)
                || !WorldFeatureRuntime.enabled(
                level,
                WorldFeature.POWER_NETWORKS
        )
                || !WorldFeatureRuntime.enabled(
                level,
                WorldFeature.INDUSTRIAL_MACHINES
        )
                || !WorldFeatureRuntime.enabled(level, WorldFeature.ASSEMBLY)) {
            if (mill.rpm != 0) { mill.rpm = 0; mill.lastPower = 0; mill.connected = false; mill.sync(); }
            return;
        }

        float oldRpm =
                mill.rpm;

        boolean oldConnected =
                mill.connected;

        IRotationalPower source =
                mill.parts.operable() ? mill.findBestSource() : null;

        mill.connected =
                source != null;

        float loadMultiplier =
                mill.wheatInput > 0
                        ? 1.0F
                        : 0.35F;

        float requested =
                POWER_DRAW
                        * loadMultiplier
                        * mill.parts.driveCost();

        float requiredTorque =
                (mill.wheatInput > 0
                        ? 0.95F
                        : 0.14F)
                        * mill.parts.driveCost();

        float condition =
                Mth.clamp(
                        mill.parts.condition(MachinePartSpec.Role.DRIVE) * 0.55F
                                + mill.parts.condition(MachinePartSpec.Role.BEARING) * 0.45F
                                - mill.assemblyWear * 0.20F,
                        0.0F,
                        1.0F
                );

        MechanicalLoad.OperatingPoint operating =
                MechanicalLoad.operate(
                        source,
                        requested,
                        requiredTorque,
                        SAFE_RPM,
                        0.0F,
                        0.0F,
                        condition
                );

        mill.lastPower =
                operating.grantedPower();

        float targetRpm =
                operating.targetRpm();

        mill.rpm +=
                (
                        targetRpm
                                - mill.rpm
                )
                        * 0.17F;

        if (Math.abs(
                mill.rpm
        ) < 0.005F
                && Math.abs(
                targetRpm
        ) < 0.005F) {
            mill.rpm =
                    0.0F;
        }

        mill.rotationDegrees =
                wrap(
                        mill.rotationDegrees
                                + mill.rpm
                                        * 0.30F
                );

        if (mill.parts.operable() && mill.wheatInput > 0
                && mill.flourOutput <= MAX_OUTPUT - 2
                && Math.abs(
                mill.rpm
        ) >= MIN_WORK_RPM) {

            float speed =
                    Mth.clamp(
                            Math.abs(
                                    mill.rpm
                            )
                                    / 42.0F,
                            0.18F,
                            1.45F
                    );

            speed *= mill.parts.speed();

            float wearPenalty =
                    1.0F
                            - mill.assemblyWear
                                    * 0.38F;

            mill.progress +=
                    0.0068F
                            * speed
                            * wearPenalty;

            if (Math.floorMod(
                    level.getGameTime()
                            + pos.asLong(),
                    10
            ) == 0) {

                server.sendParticles(
                        ParticleTypes.POOF,
                        pos.getX() + 0.5,
                        pos.getY() + 0.68,
                        pos.getZ() + 0.5,
                        2,
                        0.18,
                        0.04,
                        0.18,
                        0.004
                );
            }

            if (Math.floorMod(
                    level.getGameTime()
                            + pos.asLong(),
                    28
            ) == 0) {

                server.playSound(
                        null,
                        pos,
                        SoundEvents.GRINDSTONE_USE,
                        SoundSource.BLOCKS,
                        0.19F,
                        Mth.clamp(
                                0.72F
                                        + Math.abs(
                                        mill.rpm
                                )
                                        / 150.0F,
                                0.72F,
                                1.05F
                        )
                );
            }

            if (mill.progress >= 1.0F) {
                mill.progress -=
                        1.0F;

                mill.wheatInput--;
                mill.parts.wear(.0009F, Math.max(1, mill.lastPower / POWER_DRAW));
                mill.parts.observeMaterialUse(
                        level.getGameTime(),
                        Math.max(
                                0.25F,
                                mill.lastPower / POWER_DRAW
                        ),
                        0.0F,
                        Math.min(
                                1.0F,
                                Math.abs(
                                        mill.rpm
                                ) / 90.0F
                        )
                );

                mill.flourOutput =
                        Math.min(
                                MAX_OUTPUT,
                                mill.flourOutput + 2
                        );

                mill.assemblyWear =
                        Mth.clamp(
                                mill.assemblyWear
                                        + 0.0008F
                                                * (
                                                0.65F
                                                        + speed
                                        ),
                                0.0F,
                                1.0F
                        );

                server.playSound(
                        null,
                        pos,
                        SoundEvents.SAND_BREAK,
                        SoundSource.BLOCKS,
                        0.16F,
                        0.9F
                );

                mill.sync();
            }

        } else if (mill.wheatInput <= 0) {
            mill.progress =
                    0.0F;
        }

        if ((Math.abs(
                oldRpm
                        - mill.rpm
        ) > 0.03F
                || oldConnected
                        != mill.connected)
                && Math.floorMod(
                level.getGameTime()
                        + pos.asLong(),
                3
        ) == 0) {

            mill.sync();

        } else {
            mill.setChanged();
        }
    
        } finally {
            PerformanceProfiler.end(
                    PerformanceProfiler.Section.MACHINE_SIM,
                    wayperfStartedAt
            );
        }
    }

    public void insertWheat(
            Player player,
            ItemStack stack
    ) {
        if (level == null
                || level.isClientSide
                || !parts.complete() || wheatInput >= inputCapacity()) {

            if (!parts.complete() || wheatInput >= inputCapacity()) {
                player.displayClientMessage(
                        Component.translatable(
                                "message.wayaround.mechanical_mill.full"
                        ),
                        true
                );
            }

            return;
        }

        wheatInput++;

        if (!player.getAbilities()
                .instabuild) {
            stack.consume(
                    1,
                    player
            );
        }

        player.displayClientMessage(
                Component.translatable(
                        "message.wayaround.mechanical_mill.inserted",
                        wheatInput
                ),
                true
        );

        sync();
    }

    public void collectOutput(
            Player player
    ) {
        if (flourOutput <= 0) {
            return;
        }

        ItemStack flour =
                new ItemStack(
                        PowerContent.FLOUR.get(),
                        flourOutput
                );

        flourOutput =
                0;

        if (!player.getInventory()
                .add(
                        flour
                )) {
            player.drop(
                    flour,
                    false
            );
        }

        level.playSound(
                null,
                worldPosition,
                SoundEvents.ITEM_PICKUP,
                SoundSource.BLOCKS,
                0.24F,
                1.05F
        );

        sync();
    }

    public void dropContents() {
        if (level != null && !level.isClientSide) parts.drop(level, worldPosition);
        if (level == null
                || level.isClientSide) {
            return;
        }

        if (wheatInput > 0) {
            Block.popResource(
                    level,
                    worldPosition,
                    new ItemStack(
                            net.minecraft.world.item.Items.WHEAT,
                            wheatInput
                    )
            );
        }

        if (flourOutput > 0) {
            Block.popResource(
                    level,
                    worldPosition,
                    new ItemStack(
                            PowerContent.FLOUR.get(),
                            flourOutput
                    )
            );
        }

        wheatInput =
                0;

        flourOutput =
                0;

        progress =
                0.0F;
    }

    public void describe(
            Player player
    ) {
        if (!parts.complete()) {
            player.displayClientMessage(Component.translatable("message.wayaround.machine.assembly", parts.nodes(true).size(), 4), true);
            return;
        }
        player.displayClientMessage(
                Component.translatable(
                        "message.wayaround.mechanical_mill.status",
                        wheatInput,
                        flourOutput,
                        Math.round(
                                progress * 100.0F
                        ),
                        Math.round(
                                rpm
                        ),
                        connected
                                ? Component.translatable(
                                "message.wayaround.mechanical_mill.connected"
                        )
                                : Component.translatable(
                                "message.wayaround.mechanical_mill.disconnected"
                        )
                ),
                true
        );
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

    public float rpm() {
        return rpm;
    }

    public float rotationDegrees() {
        return rotationDegrees;
    }

    public float progress() {
        return progress;
    }

    public boolean hasInput() {
        return wheatInput > 0;
    }

    public boolean hasOutput() {
        return flourOutput > 0;
    }

    public int flourOutput() {
        return flourOutput;
    }

    public boolean connected() {
        return connected;
    }

    @Override
    public ResourceLocation assemblyType() {
        return ResourceLocation.fromNamespaceAndPath(
                "wayaround",
                "mechanical_mill"
        );
    }

    @Override
    public BlockPos assemblyAnchor() {
        return worldPosition;
    }

    @Override
    public Collection<AssemblyPartNode> assemblyParts() {
        var nodes = new ArrayList<>(parts.nodes(true));
        nodes.add(LegacyMachineAssembly.part("frame", "mill frame", AssemblyPartProfile.Kind.FRAME,
            AssemblyPartProfile.Material.WOOD, assemblyType(), assemblyWear, true, 1));
        return nodes;
    }

    @Override
    public Collection<AssemblyConnection> assemblyConnections() { return parts.connections(); }

    @Override
    public float currentAssemblyLoad() {
        return Mth.clamp(
                lastPower
                        / POWER_DRAW,
                0.0F,
                1.4F
        );
    }

    @Override
    public void applyAssemblyWear(
            float fraction
    ) {
        parts.wear(fraction, Math.max(1, lastPower / POWER_DRAW));
        assemblyWear =
                LegacyMachineAssembly.addWear(
                        assemblyWear,
                        fraction,
                        0.74F
                );

        sync();
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

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(
                tag,
                registries
        );

        parts.save(tag, registries);

        tag.putInt(
                "WheatInput",
                wheatInput
        );

        tag.putInt(
                "FlourOutput",
                flourOutput
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
                "Rotation",
                rotationDegrees
        );

        tag.putFloat(
                "LastPower",
                lastPower
        );

        tag.putFloat(
                "AssemblyWear",
                assemblyWear
        );

        tag.putBoolean(
                "Connected",
                connected
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

        if (tag.contains("InstalledMachineParts")) {
            parts.load(tag, registries);
        } else if (tag.contains("WheatInput")) {
            // Preserve pre-assembly saved mills. New placements start with an empty frame.
            parts.setLegacy(MachinePartSpec.Role.DRIVE, new ItemStack(CrusherContent.part("light_machine_shaft")));
            parts.setLegacy(MachinePartSpec.Role.BEARING, new ItemStack(CrusherContent.part("plain_machine_bearing")));
            parts.setLegacy(MachinePartSpec.Role.TOOL, new ItemStack(CrusherContent.part("stone_millstones")));
            parts.setLegacy(MachinePartSpec.Role.FEED, new ItemStack(CrusherContent.part("wide_machine_hopper")));
        }

        wheatInput =
                Mth.clamp(
                        tag.getInt(
                                "WheatInput"
                        ),
                        0,
                        MAX_INPUT
                );

        flourOutput =
                Mth.clamp(
                        tag.getInt(
                                "FlourOutput"
                        ),
                        0,
                        MAX_OUTPUT
                );

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

        rotationDegrees =
                wrap(
                        tag.getFloat(
                                "Rotation"
                        )
                );

        lastPower =
                Math.max(
                        0.0F,
                        tag.getFloat(
                                "LastPower"
                        )
                );

        assemblyWear =
                Mth.clamp(
                        tag.getFloat(
                                "AssemblyWear"
                        ),
                        0.0F,
                        1.0F
                );

        connected =
                tag.getBoolean(
                        "Connected"
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
}
