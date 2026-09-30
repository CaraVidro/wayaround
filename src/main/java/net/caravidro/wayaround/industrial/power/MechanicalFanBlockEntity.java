package net.caravidro.wayaround.industrial.power;

import java.util.Collection;
import java.util.List;

import net.caravidro.wayaround.industrial.assembly.AssemblyConnection;
import net.caravidro.wayaround.industrial.assembly.AssemblyEngine;
import net.caravidro.wayaround.industrial.assembly.AssemblyFailureEvent;
import net.caravidro.wayaround.industrial.assembly.AssemblyFailureMode;
import net.caravidro.wayaround.industrial.assembly.AssemblyLoadCase;
import net.caravidro.wayaround.industrial.assembly.AssemblyMachine;
import net.caravidro.wayaround.industrial.assembly.AssemblyPartNode;
import net.caravidro.wayaround.industrial.assembly.AssemblyPartProfile;
import net.caravidro.wayaround.industrial.mechanical.IRotationalPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class MechanicalFanBlockEntity
        extends BlockEntity
        implements AssemblyMachine {

    private float rpm;
    private float airflow;
    private float pressure;
    private float load;
    private float frameWear;
    private float bearingWear;
    private float bladeWear;
    private boolean blocked;

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
        if (!(level instanceof ServerLevel server)) {
            return;
        }

        IRotationalPower source =
                MechanicalMachineUtil.findBestSource(
                        level,
                        pos
                );

        Direction facing =
                fan.facing();

        BlockPos front =
                pos.relative(
                        facing
                );

        fan.blocked =
                !level.getBlockState(
                        front
                ).isAir();

        float sourceRpm =
                source == null
                        ? 0.0F
                        : Math.abs(
                        source.rpm()
                );

        float requested =
                source == null
                        ? 0.0F
                        : 0.22F
                                + sourceRpm
                                        * 0.018F
                                + (
                                fan.blocked
                                        ? 1.10F
                                        : 0.0F
                        );

        float granted =
                source == null
                        ? 0.0F
                        : source.consumePower(
                                requested
                        );

        float ratio =
                requested <= 0.001F
                        ? 0.0F
                        : Mth.clamp(
                        granted
                                / requested,
                        0.0F,
                        1.0F
                );

        float targetRpm =
                sourceRpm
                        * ratio;

        fan.rpm +=
                (
                        targetRpm
                                - fan.rpm
                )
                        * 0.24F;

        float normalized =
                Mth.clamp(
                        fan.rpm
                                / 52.0F,
                        0.0F,
                        1.75F
                );

        fan.airflow =
                normalized
                        * (
                        fan.blocked
                                ? 0.16F
                                : 1.0F
                )
                        * (
                        1.0F
                                - fan.bladeWear
                                        * 0.55F
                );

        fan.pressure =
                normalized
                        * (
                        fan.blocked
                                ? 1.55F
                                : 0.46F
                )
                        * (
                        1.0F
                                - fan.bearingWear
                                        * 0.30F
                );

        fan.load =
                requested
                        * (
                        0.55F
                                + fan.pressure
                                        * 0.45F
                );

        if (Math.floorMod(
                server.getGameTime()
                        + pos.asLong(),
                20L
        ) == 0
                && fan.rpm > 2.0F) {

            float wear =
                    0.00020F
                            + normalized
                                    * 0.00022F
                            + (
                            fan.blocked
                                    ? 0.00065F
                                    : 0.0F
                    );

            fan.bladeWear =
                    Mth.clamp(
                            fan.bladeWear
                                    + wear,
                            0.0F,
                            1.0F
                    );

            fan.bearingWear =
                    Mth.clamp(
                            fan.bearingWear
                                    + wear
                                            * (
                                            fan.blocked
                                                    ? 1.45F
                                                    : 0.72F
                                    ),
                            0.0F,
                            1.0F
                    );

            fan.frameWear =
                    Mth.clamp(
                            fan.frameWear
                                    + wear
                                            * 0.22F,
                            0.0F,
                            1.0F
                    );

            AssemblyEngine.dispatchFailure(
                    fan,
                    new AssemblyLoadCase(
                            AssemblyLoadCase.Kind.MECHANICAL,
                            fan.currentAssemblyLoad(),
                            Vec3.atLowerCornerOf(
                                    facing.getNormal()
                            ),
                            Mth.clamp(
                                    normalized
                                            * 0.70F
                                            + (
                                            fan.blocked
                                                    ? 0.30F
                                                    : 0.0F
                                    ),
                                    0.0F,
                                    1.0F
                            )
                    )
            );

            fan.setChanged();
        }
    }

    public Direction facing() {
        return getBlockState().getValue(
                MechanicalFanBlock.FACING
        );
    }

    public float rpm() {
        return rpm;
    }

    public float airflow() {
        return airflow;
    }

    public float pressure() {
        return pressure;
    }

    public boolean blocked() {
        return blocked;
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
        return List.of(
                new AssemblyPartNode(
                        "frame",
                        "frame",
                        AssemblyPartProfile.legacy(
                                AssemblyPartProfile.Kind.FRAME,
                                AssemblyPartProfile.Material.IRON,
                                ResourceLocation.fromNamespaceAndPath(
                                        "wayaround",
                                        "mechanical_fan"
                                ),
                                0,
                                frameWear
                        ),
                        true,
                        0.70F
                ),
                new AssemblyPartNode(
                        "shaft",
                        "shaft",
                        AssemblyPartProfile.legacy(
                                AssemblyPartProfile.Kind.SHAFT,
                                AssemblyPartProfile.Material.IRON,
                                ResourceLocation.fromNamespaceAndPath(
                                        "wayaround",
                                        "mechanical_fan"
                                ),
                                0,
                                bearingWear
                        ),
                        false,
                        1.15F
                ),
                new AssemblyPartNode(
                        "blades",
                        "fan_blades",
                        AssemblyPartProfile.legacy(
                                AssemblyPartProfile.Kind.BLADE,
                                AssemblyPartProfile.Material.IRON,
                                ResourceLocation.fromNamespaceAndPath(
                                        "wayaround",
                                        "mechanical_fan"
                                ),
                                0,
                                bladeWear
                        ),
                        false,
                        1.55F
                )
        );
    }

    @Override
    public Collection<AssemblyConnection> assemblyConnections() {
        return List.of(
                new AssemblyConnection(
                        "frame",
                        "shaft",
                        AssemblyConnection.Type.BEARING,
                        0.92F,
                        bearingWear
                ),
                new AssemblyConnection(
                        "shaft",
                        "blades",
                        AssemblyConnection.Type.SHAFT,
                        0.94F,
                        bladeWear
                                * 0.55F
                )
        );
    }

    @Override
    public float currentAssemblyLoad() {
        return load
                + pressure
                        * 0.45F;
    }

    @Override
    public void applyAssemblyWear(
            float fraction
    ) {
        float value =
                Mth.clamp(
                        fraction,
                        0.0F,
                        0.35F
                );

        bladeWear =
                Mth.clamp(
                        bladeWear
                                + value
                                        * 0.65F,
                        0.0F,
                        1.0F
                );

        bearingWear =
                Mth.clamp(
                        bearingWear
                                + value
                                        * 0.80F,
                        0.0F,
                        1.0F
                );

        frameWear =
                Mth.clamp(
                        frameWear
                                + value
                                        * 0.22F,
                        0.0F,
                        1.0F
                );

        setChanged();
    }

    @Override
    public void applyAssemblyFailure(
            AssemblyFailureEvent failure
    ) {
        if (failure == null) {
            return;
        }

        float severity =
                Mth.clamp(
                        failure.severity(),
                        0.0F,
                        4.0F
                );

        if (failure.targetId().contains(
                "blade"
        )) {
            bladeWear =
                    Mth.clamp(
                            bladeWear
                                    + 0.10F
                                    + severity
                                            * 0.16F,
                            0.0F,
                            1.0F
                    );

        } else if (failure.targetId().contains(
                "shaft"
        )
                || failure.mode()
                        == AssemblyFailureMode.SEIZE) {

            bearingWear =
                    Mth.clamp(
                            bearingWear
                                    + 0.08F
                                    + severity
                                            * 0.18F,
                            0.0F,
                            1.0F
                    );

            if (failure.mode()
                    == AssemblyFailureMode.SEIZE) {
                rpm =
                        0.0F;
            }

        } else {
            frameWear =
                    Mth.clamp(
                            frameWear
                                    + 0.05F
                                    + severity
                                            * 0.12F,
                            0.0F,
                            1.0F
                    );
        }

        setChanged();
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
                "FanRpm",
                rpm
        );

        tag.putFloat(
                "FrameWear",
                frameWear
        );

        tag.putFloat(
                "BearingWear",
                bearingWear
        );

        tag.putFloat(
                "BladeWear",
                bladeWear
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
                Math.max(
                        0.0F,
                        tag.getFloat(
                                "FanRpm"
                        )
                );

        frameWear =
                Mth.clamp(
                        tag.getFloat(
                                "FrameWear"
                        ),
                        0.0F,
                        1.0F
                );

        bearingWear =
                Mth.clamp(
                        tag.getFloat(
                                "BearingWear"
                        ),
                        0.0F,
                        1.0F
                );

        bladeWear =
                Mth.clamp(
                        tag.getFloat(
                                "BladeWear"
                        ),
                        0.0F,
                        1.0F
                );
    }
}
