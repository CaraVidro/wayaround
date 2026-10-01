package net.caravidro.wayaround.industrial.grid;

import net.caravidro.wayaround.industrial.mechanical.IRotationalPower;
import net.caravidro.wayaround.industrial.power.EnergyBudget;
import net.caravidro.wayaround.industrial.power.PowerContent;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class ElectricMotorBlockEntity
        extends BlockEntity
        implements IRotationalPower {

    private static final int CAPACITY = 16_000;
    private static final float NOMINAL_RPM = 84.0F;
    private static final float NOMINAL_TORQUE = 2.8F;

    private final EnergyBudget buffer =
            new EnergyBudget(
                    CAPACITY
            );

    private float rpm;
    private float torque;
    private float availablePower;
    private float accumulatedDraw;

    private final IEnergyStorage input =
            new IEnergyStorage() {
                @Override
                public int receiveEnergy(
                        int amount,
                        boolean simulate
                ) {
                    int accepted =
                            Math.min(
                                    Math.min(
                                            Math.max(
                                                    0,
                                                    amount
                                            ),
                                            512
                                    ),
                                    buffer.capacity()
                                            - buffer.stored()
                            );

                    if (!simulate
                            && accepted > 0) {
                        buffer.add(
                                accepted
                        );
                        setChanged();
                    }

                    return accepted;
                }

                @Override
                public int extractEnergy(
                        int amount,
                        boolean simulate
                ) {
                    return 0;
                }

                @Override
                public int getEnergyStored() {
                    return buffer.stored();
                }

                @Override
                public int getMaxEnergyStored() {
                    return buffer.capacity();
                }

                @Override
                public boolean canExtract() {
                    return false;
                }

                @Override
                public boolean canReceive() {
                    return true;
                }
            };

    public ElectricMotorBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                PowerContent.ELECTRIC_MOTOR_ENTITY.get(),
                pos,
                state
        );
    }

    public IEnergyStorage energyInput() {
        return input;
    }

    public IRotationalPower rotationOutput(
            Direction side
    ) {
        return side != null
                && side.getAxis()
                        == axis()
                ? this
                : null;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            ElectricMotorBlockEntity motor
    ) {
        if (!WorldFeatureRuntime.enabled(
                level,
                WorldFeature.POWER_NETWORKS
        )) {
            return;
        }

        float previousDraw =
                motor.accumulatedDraw;

        motor.accumulatedDraw =
                0.0F;

        int demand =
                2
                        + Math.max(
                        0,
                        Math.round(
                                previousDraw * 9.0F
                        )
                );

        int paid =
                motor.buffer.extract(
                        demand,
                        false
                );

        float supply =
                demand <= 0
                        ? 0.0F
                        : paid
                                / (float) demand;

        float targetRpm =
                NOMINAL_RPM
                        * supply;

        float targetTorque =
                NOMINAL_TORQUE
                        * supply;

        motor.rpm +=
                (
                        targetRpm
                                - motor.rpm
                )
                        * 0.14F;

        motor.torque +=
                (
                        targetTorque
                                - motor.torque
                )
                        * 0.16F;

        if (motor.buffer.stored() <= 0
                && paid <= 0) {
            motor.rpm *=
                    0.90F;

            motor.torque *=
                    0.84F;
        }

        if (Math.abs(
                motor.rpm
        ) < 0.03F) {
            motor.rpm =
                    0.0F;
        }

        if (motor.torque < 0.01F) {
            motor.torque =
                    0.0F;
        }

        motor.availablePower =
                GridPhysics.mechanicalPower(
                        motor.rpm,
                        motor.torque
                );

        if (paid > 0
                || motor.active()) {
            motor.setChanged();
        }
    }

    @Override
    public float rpm() {
        return rpm;
    }

    @Override
    public float torque() {
        return torque;
    }

    @Override
    public float power() {
        return availablePower;
    }

    @Override
    public Direction.Axis axis() {
        return getBlockState()
                .getValue(
                        ElectricMotorBlock.AXIS
                );
    }

    @Override
    public int rotationDirection() {
        return rpm > 0.01F
                ? 1
                : 0;
    }

    @Override
    public float consumePower(
            float requestedPower
    ) {
        float accepted =
                Math.min(
                        Math.max(
                                0.0F,
                                requestedPower
                        ),
                        availablePower
                );

        availablePower -=
                accepted;

        accumulatedDraw +=
                accepted;

        return accepted;
    }

    public Component status() {
        return Component.translatable(
                "message.wayaround.electric_motor.status",
                Component.translatable(
                        buffer.stored() <= 0
                                ? "message.wayaround.grid.idle"
                                : active()
                                        ? "message.wayaround.electric_motor.turning"
                                        : "message.wayaround.grid.charged"
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

        tag.putInt(
                "Energy",
                buffer.stored()
        );

        tag.putFloat(
                "Rpm",
                rpm
        );

        tag.putFloat(
                "Torque",
                torque
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

        buffer.load(
                tag.getInt(
                        "Energy"
                )
        );

        rpm =
                Math.max(
                        0.0F,
                        tag.getFloat(
                                "Rpm"
                        )
                );

        torque =
                Math.max(
                        0.0F,
                        tag.getFloat(
                                "Torque"
                        )
                );

        availablePower =
                GridPhysics.mechanicalPower(
                        rpm,
                        torque
                );
    }
}
