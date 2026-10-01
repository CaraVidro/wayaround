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

public final class FuseBoxBlockEntity
        extends BlockEntity {

    private static final int CAPACITY = 12_000;
    private static final int SAFE_PULSE = 960;

    private final EnergyBudget buffer =
            new EnergyBudget(
                    CAPACITY
            );

    private float overload;
    private boolean tripped;
    private int nextReceiver;

    private final IEnergyStorage input =
            new IEnergyStorage() {
                @Override
                public int receiveEnergy(
                        int amount,
                        boolean simulate
                ) {
                    if (tripped
                            || amount <= 0) {
                        return 0;
                    }

                    float stress =
                            Math.max(
                                    0.0F,
                                    amount
                                            / (float) SAFE_PULSE
                                            - 1.0F
                            );

                    if (!simulate
                            && stress > 0.0F) {
                        overload +=
                                stress * 0.32F;

                        if (overload >= 1.0F) {
                            tripped =
                                    true;
                            setChanged();
                            return 0;
                        }
                    }

                    int accepted =
                            Math.min(
                                    Math.min(
                                            amount,
                                            SAFE_PULSE
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
                    return !tripped;
                }
            };

    public FuseBoxBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                PowerContent.FUSE_BOX_ENTITY.get(),
                pos,
                state
        );
    }

    public IEnergyStorage input(
            Direction side
    ) {
        return !tripped
                && side == inputSide()
                ? input
                : null;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            FuseBoxBlockEntity fuse
    ) {
        if (!WorldFeatureRuntime.enabled(
                level,
                WorldFeature.POWER_NETWORKS
        )
                || !(level instanceof ServerLevel server)) {
            return;
        }

        if (fuse.overload > 0.0F) {
            fuse.overload =
                    Math.max(
                            0.0F,
                            fuse.overload - 0.005F
                    );
        }

        if (fuse.tripped
                || fuse.buffer.stored() <= 0
                || Math.floorMod(
                level.getGameTime()
                        + pos.asLong(),
                5
        ) != 0) {
            return;
        }

        int before =
                fuse.buffer.stored();

        fuse.nextReceiver =
                EnergyNetwork.distributeFromSide(
                        server,
                        pos,
                        fuse.outputSide(),
                        fuse.buffer,
                        fuse.nextReceiver
                );

        if (fuse.buffer.stored()
                < before) {
            fuse.setChanged();
        }
    }

    public void reset() {
        tripped =
                false;

        overload =
                0.0F;

        setChanged();
    }

    public boolean tripped() {
        return tripped;
    }

    public Component status() {
        Component state =
                Component.translatable(
                        tripped
                                ? "message.wayaround.fuse_box.tripped"
                                : overload > 0.55F
                                        ? "message.wayaround.fuse_box.stressed"
                                        : buffer.stored() > 0
                                                ? "message.wayaround.grid.energized"
                                                : "message.wayaround.grid.idle"
                );

        return Component.translatable(
                "message.wayaround.fuse_box.status",
                state
        );
    }

    private Direction inputSide() {
        return getBlockState()
                .getValue(
                        FuseBoxBlock.FACING
                )
                .getOpposite();
    }

    private Direction outputSide() {
        return getBlockState()
                .getValue(
                        FuseBoxBlock.FACING
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
                "Overload",
                overload
        );

        tag.putBoolean(
                "Tripped",
                tripped
        );

        tag.putInt(
                "NextReceiver",
                nextReceiver
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

        overload =
                Math.clamp(
                        tag.getFloat(
                                "Overload"
                        ),
                        0.0F,
                        1.5F
                );

        tripped =
                tag.getBoolean(
                        "Tripped"
                );

        nextReceiver =
                tag.getInt(
                        "NextReceiver"
                );
    }
}
