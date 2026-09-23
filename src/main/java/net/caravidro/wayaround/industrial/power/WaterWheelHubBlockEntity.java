package net.caravidro.wayaround.industrial.power;

import java.util.Locale;

import net.caravidro.wayaround.worldgen.water.WaterDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class WaterWheelHubBlockEntity
        extends BlockEntity {

    private float rpm;
    private float efficiency;
    private float mechanicalPower;
    private int bladeCount;
    private int wetBladeCount;

    public WaterWheelHubBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                PowerContent.WATER_WHEEL_HUB_ENTITY.get(),
                pos,
                state
        );
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            WaterWheelHubBlockEntity hub
    ) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }

        if (Math.floorMod(
                level.getGameTime()
                + pos.asLong(),
                5
        ) != 0) {
            return;
        }

        float oldRpm =
                hub.rpm;

        float oldEfficiency =
                hub.efficiency;

        int oldBlades =
                hub.bladeCount;

        hub.sampleWheel(
                server,
                state
        );

        if (Math.abs(
                oldRpm - hub.rpm
        ) > 0.05F
                || Math.abs(
                        oldEfficiency - hub.efficiency
                ) > 0.01F
                || oldBlades != hub.bladeCount) {

            hub.setChanged();

            server.sendBlockUpdated(
                    pos,
                    state,
                    state,
                    net.minecraft.world.level.block.Block.UPDATE_CLIENTS
            );
        }
    }

    private void sampleWheel(
            ServerLevel level,
            BlockState hubState
    ) {
        Direction.Axis axle =
                hubState.getValue(
                        WaterWheelHubBlock.FACING
                ).getAxis();

        int found =
                0;

        int wet =
                0;

        double signedTorque =
                0.0;

        double usefulAlignment =
                0.0;

        for (int a = -4;
                a <= 4;
                a++) {

            for (int b = -4;
                    b <= 4;
                    b++) {

                if (a == 0
                        && b == 0) {
                    continue;
                }

                double radius =
                        Math.sqrt(
                                a * a
                                + b * b
                        );

                if (radius < 1.45
                        || radius > 4.25) {
                    continue;
                }

                BlockPos bladePos;

                if (axle == Direction.Axis.X) {
                    bladePos =
                            worldPosition.offset(
                                    0,
                                    a,
                                    b
                            );
                } else {
                    bladePos =
                            worldPosition.offset(
                                    b,
                                    a,
                                    0
                            );
                }

                BlockState bladeState =
                        level.getBlockState(
                                bladePos
                        );

                if (!(bladeState.getBlock()
                        instanceof WaterWheelBladeBlock)) {
                    continue;
                }

                found++;

                Vec3 current =
                        WaterDynamics.currentAround(
                                level,
                                bladePos
                        );

                double speed =
                        WaterDynamics.speed(
                                current
                        );

                if (speed < 0.008) {
                    continue;
                }

                wet++;

                Direction bladeFacing =
                        bladeState.getValue(
                                WaterWheelBladeBlock.FACING
                        );

                double alignment =
                        Math.abs(
                                current.x
                                        * bladeFacing.getStepX()
                                + current.z
                                        * bladeFacing.getStepZ()
                        ) / Math.max(
                                speed,
                                0.0001
                        );

                alignment =
                        Mth.clamp(
                                alignment,
                                0.0,
                                1.0
                        );

                double radiusFactor =
                        Mth.clamp(
                                radius / 3.2,
                                0.35,
                                1.25
                        );

                /*
                 * r x F projected onto the axle. With horizontal current,
                 * the vertical lever arm is what creates most of the torque.
                 */
                double torqueContribution;

                if (axle == Direction.Axis.X) {
                    torqueContribution =
                            a
                            * current.z;
                } else {
                    torqueContribution =
                            -a
                            * current.x;
                }

                signedTorque +=
                        torqueContribution
                        * alignment
                        * radiusFactor;

                usefulAlignment +=
                        alignment
                        * radiusFactor;
            }
        }

        bladeCount =
                found;

        wetBladeCount =
                wet;

        if (found == 0
                || wet == 0) {

            rpm =
                    Mth.lerp(
                            0.32F,
                            rpm,
                            0.0F
                    );

            efficiency =
                    0.0F;

            mechanicalPower =
                    0.0F;

            return;
        }

        float coverage =
                Mth.clamp(
                        found / 8.0F,
                        0.15F,
                        1.0F
                );

        float waterContact =
                Mth.clamp(
                        wet / (float) Math.max(
                                1,
                                found
                        ),
                        0.0F,
                        1.0F
                );

        float alignmentEfficiency =
                (float) Mth.clamp(
                        usefulAlignment
                        / Math.max(
                                1,
                                wet
                        ),
                        0.0,
                        1.0
                );

        float newEfficiency =
                Mth.clamp(
                        coverage
                        * waterContact
                        * alignmentEfficiency,
                        0.0F,
                        1.0F
                );

        float targetRpm =
                (float) Mth.clamp(
                        Math.abs(
                                signedTorque
                        ) * 42.0,
                        0.0,
                        36.0
                );

        rpm =
                Mth.lerp(
                        0.28F,
                        rpm,
                        targetRpm
                );

        efficiency =
                Mth.lerp(
                        0.24F,
                        efficiency,
                        newEfficiency
                );

        mechanicalPower =
                rpm
                * efficiency
                * 4.0F;
    }

    public float rpm() {
        return rpm;
    }

    public float efficiency() {
        return efficiency;
    }

    public float mechanicalPower() {
        return mechanicalPower;
    }

    public int bladeCount() {
        return bladeCount;
    }

    public int wetBladeCount() {
        return wetBladeCount;
    }

    public boolean axleMatches(
            Direction.Axis axis
    ) {
        return getBlockState()
                .getValue(
                        WaterWheelHubBlock.FACING
                ).getAxis()
                == axis;
    }

    public Component status() {
        return Component.translatable(
                "message.wayaround.water_wheel.status",
                String.format(
                        Locale.ROOT,
                        "%.1f",
                        rpm
                ),
                Math.round(
                        efficiency * 100.0F
                ),
                wetBladeCount,
                bladeCount,
                String.format(
                        Locale.ROOT,
                        "%.1f",
                        mechanicalPower
                )
        );
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

        writeSyncData(
                tag
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

        efficiency =
                tag.getFloat(
                        "Efficiency"
                );

        mechanicalPower =
                tag.getFloat(
                        "MechanicalPower"
                );

        bladeCount =
                tag.getInt(
                        "BladeCount"
                );

        wetBladeCount =
                tag.getInt(
                        "WetBladeCount"
                );
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries
    ) {
        CompoundTag tag =
                super.getUpdateTag(
                        registries
                );

        writeSyncData(
                tag
        );

        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener>
            getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(
                this
        );
    }

    private void writeSyncData(
            CompoundTag tag
    ) {
        tag.putFloat(
                "Rpm",
                rpm
        );

        tag.putFloat(
                "Efficiency",
                efficiency
        );

        tag.putFloat(
                "MechanicalPower",
                mechanicalPower
        );

        tag.putInt(
                "BladeCount",
                bladeCount
        );

        tag.putInt(
                "WetBladeCount",
                wetBladeCount
        );
    }
}
