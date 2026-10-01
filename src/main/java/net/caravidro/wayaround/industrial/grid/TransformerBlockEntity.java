package net.caravidro.wayaround.industrial.grid;

import net.caravidro.wayaround.industrial.power.EnergyBudget;
import net.caravidro.wayaround.industrial.power.EnergyNetwork;
import net.caravidro.wayaround.industrial.power.PowerContent;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class TransformerBlockEntity
        extends BlockEntity
        implements HighVoltageReceiver {

    public enum Mode {
        STEP_UP,
        STEP_DOWN
    }

    private static final int CAPACITY = 64_000;
    private static final float TRANSFORMER_EFFICIENCY = 0.96F;

    private final EnergyBudget buffer =
            new EnergyBudget(
                    CAPACITY
            );

    private Mode mode =
            Mode.STEP_UP;

    private int nextReceiver;
    private boolean active;
    private float lastLineEfficiency = 1.0F;

    private final IEnergyStorage lowInput =
            new IEnergyStorage() {
                @Override
                public int receiveEnergy(
                        int amount,
                        boolean simulate
                ) {
                    if (mode != Mode.STEP_UP) {
                        return 0;
                    }

                    int accepted =
                            Math.min(
                                    Math.max(
                                            0,
                                            amount
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
                    return mode == Mode.STEP_UP;
                }
            };

    public TransformerBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                PowerContent.TRANSFORMER_ENTITY.get(),
                pos,
                state
        );
    }

    public IEnergyStorage lowInput(
            Direction side
    ) {
        if (mode != Mode.STEP_UP
                || side == null
                || side != inputSide()) {
            return null;
        }

        return lowInput;
    }

    @Override
    public int receiveHighVoltage(
            int amount,
            Direction side
    ) {
        if (mode != Mode.STEP_DOWN
                || side == null
                || side != inputSide()
                || amount <= 0) {
            return 0;
        }

        int room =
                buffer.capacity()
                        - buffer.stored();

        if (room <= 0) {
            return 0;
        }

        int rawAccepted =
                Math.min(
                        amount,
                        (int) Math.floor(
                                room
                                        / TRANSFORMER_EFFICIENCY
                        )
                );

        if (rawAccepted <= 0) {
            return 0;
        }

        int converted =
                Math.max(
                        1,
                        (int) Math.floor(
                                rawAccepted
                                        * TRANSFORMER_EFFICIENCY
                        )
                );

        buffer.add(
                converted
        );

        active = true;
        setChanged();

        return rawAccepted;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            TransformerBlockEntity transformer
    ) {
        if (!WorldFeatureRuntime.enabled(
                level,
                WorldFeature.POWER_NETWORKS
        )
                || !(level instanceof ServerLevel server)) {
            return;
        }

        transformer.active =
                false;

        if (transformer.buffer.stored() <= 0
                || Math.floorMod(
                level.getGameTime()
                        + pos.asLong(),
                10
        ) != 0) {
            return;
        }

        if (transformer.mode == Mode.STEP_UP) {
            HighVoltageNetwork.Delivery delivery =
                    HighVoltageNetwork.distribute(
                            server,
                            pos,
                            transformer.outputSide(),
                            transformer.buffer.stored(),
                            transformer.nextReceiver,
                            TRANSFORMER_EFFICIENCY
                    );

            if (delivery.rawSpent() > 0) {
                transformer.buffer.extract(
                        delivery.rawSpent(),
                        false
                );

                transformer.active =
                        true;

                transformer.lastLineEfficiency =
                        delivery.efficiency();

                transformer.nextReceiver =
                        delivery.nextReceiver();

                transformer.setChanged();
            }

        } else {
            int before =
                    transformer.buffer.stored();

            transformer.nextReceiver =
                    EnergyNetwork.distributeFromSide(
                            server,
                            pos,
                            transformer.outputSide(),
                            transformer.buffer,
                            transformer.nextReceiver
                    );

            transformer.active =
                    transformer.buffer.stored()
                            < before;

            if (transformer.active) {
                transformer.setChanged();
            }
        }
    }

    public boolean toggleMode() {
        if (buffer.stored() > 0) {
            return false;
        }

        mode =
                mode == Mode.STEP_UP
                        ? Mode.STEP_DOWN
                        : Mode.STEP_UP;

        active =
                false;

        setChanged();

        return true;
    }

    public Component status() {
        return Component.translatable(
                "message.wayaround.transformer.status",
                Component.translatable(
                        mode == Mode.STEP_UP
                                ? "message.wayaround.transformer.step_up"
                                : "message.wayaround.transformer.step_down"
                ),
                Component.translatable(
                        active
                                ? "message.wayaround.grid.energized"
                                : buffer.stored() > 0
                                        ? "message.wayaround.grid.charged"
                                        : "message.wayaround.grid.idle"
                )
        );
    }

    public float lineEfficiency() {
        return lastLineEfficiency;
    }

    private Direction inputSide() {
        return getBlockState()
                .getValue(
                        TransformerBlock.FACING
                )
                .getOpposite();
    }

    private Direction outputSide() {
        return getBlockState()
                .getValue(
                        TransformerBlock.FACING
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

        tag.putString(
                "Mode",
                mode.name()
        );

        tag.putInt(
                "NextReceiver",
                nextReceiver
        );

        tag.putFloat(
                "LineEfficiency",
                lastLineEfficiency
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

        try {
            mode =
                    Mode.valueOf(
                            tag.getString(
                                    "Mode"
                            )
                    );
        } catch (IllegalArgumentException ignored) {
            mode =
                    Mode.STEP_UP;
        }

        nextReceiver =
                tag.getInt(
                        "NextReceiver"
                );

        float efficiency =
                tag.getFloat(
                        "LineEfficiency"
                );

        lastLineEfficiency =
                Float.isFinite(
                        efficiency
                )
                        ? Math.clamp(
                        efficiency,
                        0.0F,
                        1.0F
                )
                        : 1.0F;
    }
}
