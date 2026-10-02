package net.caravidro.wayaround.industrial.power;

import java.util.Collection;
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
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class MechanicalFanBlockEntity
        extends BlockEntity
        implements AssemblyMachine {

    private static final float POWER_DRAW =
            1.25F;

    private static final float SAFE_RPM =
            96.0F;

    private float rpm;
    private float rotationDegrees;
    private float lastPower;
    private float assemblyWear;
    private boolean connected;

    public MechanicalFanBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                PowerContent.MECHANICAL_FAN_ENTITY.get(),
                pos,
                state
        );
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            MechanicalFanBlockEntity fan
    ) {
        if (!(level instanceof ServerLevel server)
                || !WorldFeatureRuntime.enabled(
                level,
                WorldFeature.POWER_NETWORKS
        )
                || !WorldFeatureRuntime.enabled(
                level,
                WorldFeature.INDUSTRIAL_MACHINES
        )) {
            return;
        }

        float oldRpm =
                fan.rpm;

        boolean oldConnected =
                fan.connected;

        IRotationalPower source =
                fan.findBestSource();

        fan.connected =
                source != null;

        float sourceRpm =
                source == null
                        ? 0.0F
                        : Math.abs(source.rpm());

        float requested =
                POWER_DRAW
                        + Math.min(
                        0.85F,
                        sourceRpm / 90.0F
                );

        /*
         * Air drag is a torque load, not just another power tax. A very fast
         * but weak drive may therefore fail to spin the fan under load.
         */
        float requiredTorque =
                0.16F
                        + Math.min(
                        0.62F,
                        sourceRpm / 150.0F
                );

        MechanicalLoad.OperatingPoint operating =
                MechanicalLoad.operate(
                        source,
                        requested,
                        requiredTorque,
                        SAFE_RPM,
                        0.0F,
                        0.0F,
                        Mth.clamp(
                                1.0F - fan.assemblyWear,
                                0.0F,
                                1.0F
                        )
                );

        fan.lastPower =
                operating.grantedPower();

        float targetRpm =
                operating.targetRpm();

        fan.rpm +=
                (
                        targetRpm
                                - fan.rpm
                )
                        * 0.18F;

        if (Math.abs(
                fan.rpm
        ) < 0.005F
                && Math.abs(
                targetRpm
        ) < 0.005F) {
            fan.rpm =
                    0.0F;
        }

        fan.rotationDegrees =
                wrap(
                        fan.rotationDegrees
                                + fan.rpm
                                        * 0.30F
                );

        fan.applyAirflow(
                server,
                state
        );

        if (Math.abs(
                fan.rpm
        ) > 2.5F
                && Math.floorMod(
                level.getGameTime()
                        + pos.asLong(),
                32
        ) == 0) {

            server.playSound(
                    null,
                    pos,
                    SoundEvents.MINECART_RIDING,
                    SoundSource.BLOCKS,
                    0.075F,
                    Mth.clamp(
                            0.62F
                                    + Math.abs(
                                    fan.rpm
                            )
                                    / 160.0F,
                            0.62F,
                            1.04F
                    )
            );
        }

        if ((Math.abs(
                oldRpm
                        - fan.rpm
        ) > 0.03F
                || oldConnected
                        != fan.connected)
                && Math.floorMod(
                level.getGameTime()
                        + pos.asLong(),
                3
        ) == 0) {

            fan.sync();
        } else {
            fan.setChanged();
        }
    }

    private void applyAirflow(
            ServerLevel server,
            BlockState state
    ) {
        float strength =
                airflowStrength();

        if (strength <= 0.01F) {
            return;
        }

        Direction direction =
                state.getValue(
                        MechanicalFanBlock.FACING
                );

        Vec3 origin =
                Vec3.atCenterOf(
                        worldPosition
                );

        Vec3 end =
                origin.add(
                        direction.getStepX()
                                * 4.5,
                        direction.getStepY()
                                * 4.5,
                        direction.getStepZ()
                                * 4.5
                );

        AABB corridor =
                new AABB(
                        Math.min(
                                origin.x,
                                end.x
                        ) - 0.68,
                        Math.min(
                                origin.y,
                                end.y
                        ) - 0.68,
                        Math.min(
                                origin.z,
                                end.z
                        ) - 0.68,
                        Math.max(
                                origin.x,
                                end.x
                        ) + 0.68,
                        Math.max(
                                origin.y,
                                end.y
                        ) + 0.68,
                        Math.max(
                                origin.z,
                                end.z
                        ) + 0.68
                );

        for (Entity entity :
                server.getEntities(
                        (Entity) null,
                        corridor,
                        candidate -> candidate.isAlive()
                                && !candidate.isSpectator()
                )) {

            Vec3 delta =
                    entity.position()
                            .subtract(
                                    origin
                            );

            double along =
                    delta.x
                            * direction.getStepX()
                            + delta.y
                            * direction.getStepY()
                            + delta.z
                            * direction.getStepZ();

            if (along < 0.35
                    || along > 5.0) {
                continue;
            }

            float falloff =
                    Mth.clamp(
                            1.0F
                                    - (float) along
                                            / 6.0F,
                            0.18F,
                            1.0F
                    );

            double push =
                    strength
                            * falloff
                            * 0.085;

            entity.push(
                    direction.getStepX()
                            * push,
                    direction.getStepY()
                            * push
                            + 0.006
                                    * strength,
                    direction.getStepZ()
                            * push
            );
        }

        if (Math.floorMod(
                server.getGameTime()
                        + worldPosition.asLong(),
                5
        ) == 0) {

            double distance =
                    0.9
                            + server.random.nextDouble()
                                    * 2.8;

            server.sendParticles(
                    ParticleTypes.CLOUD,
                    origin.x
                            + direction.getStepX()
                                    * distance,
                    origin.y
                            + (
                            server.random.nextDouble()
                                    - 0.5
                    )
                                    * 0.55,
                    origin.z
                            + direction.getStepZ()
                                    * distance,
                    1,
                    0.16,
                    0.12,
                    0.16,
                    0.01
            );
        }
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

    public boolean connected() {
        return connected;
    }

    public float airflowStrength() {
        return Mth.clamp(
                Math.abs(
                        rpm
                )
                        / 60.0F,
                0.0F,
                1.0F
        );
    }

    @Override
    public ResourceLocation assemblyType() {
        return ResourceLocation.fromNamespaceAndPath(
                "wayaround",
                "mechanical_fan"
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
                        "fan frame",
                        AssemblyPartProfile.Kind.FRAME,
                        AssemblyPartProfile.Material.IRON,
                        source,
                        assemblyWear * 0.68F,
                        true,
                        1.20F
                ),
                LegacyMachineAssembly.part(
                        "rotor",
                        "fan rotor",
                        AssemblyPartProfile.Kind.SHAFT,
                        AssemblyPartProfile.Material.IRON,
                        source,
                        assemblyWear,
                        true,
                        1.10F
                ),
                LegacyMachineAssembly.part(
                        "guard",
                        "fan guard",
                        AssemblyPartProfile.Kind.GENERAL,
                        AssemblyPartProfile.Material.IRON,
                        source,
                        assemblyWear * 0.55F,
                        true,
                        0.60F
                )
        );
    }

    @Override
    public Collection<AssemblyConnection> assemblyConnections() {
        return List.of(
                new AssemblyConnection(
                        "frame",
                        "rotor",
                        AssemblyConnection.Type.BEARING,
                        0.92F,
                        Mth.clamp(
                                assemblyWear,
                                0.0F,
                                1.0F
                        )
                ),
                new AssemblyConnection(
                        "frame",
                        "guard",
                        AssemblyConnection.Type.FASTENED,
                        0.96F,
                        Mth.clamp(
                                assemblyWear * 0.55F,
                                0.0F,
                                1.0F
                        )
                )
        );
    }

    @Override
    public float currentAssemblyLoad() {
        return Mth.clamp(
                lastPower
                        / 2.0F,
                0.0F,
                1.25F
        );
    }

    @Override
    public void applyAssemblyWear(
            float fraction
    ) {
        assemblyWear =
                LegacyMachineAssembly.addWear(
                        assemblyWear,
                        fraction,
                        0.78F
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
