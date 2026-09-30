package net.caravidro.wayaround.industrial.power;

import net.caravidro.wayaround.advancement.WayAroundAdvancements;
import javax.annotation.Nullable;

import net.caravidro.wayaround.industrial.mechanical.IRotationalPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ManualCrankBlockEntity
        extends BlockEntity {

    private static final float ACTIVE_RPM =
            22.0F;

    private static final float MAX_POWER =
            1.85F;

    private int crankTicks;
    private float rpm;
    private float angle;
    private float availablePower;

    private final IRotationalPower output =
            new IRotationalPower() {
                @Override
                public float rpm() {
                    return ManualCrankBlockEntity.this.rpm;
                }

                @Override
                public float torque() {
                    double omega =
                            Math.abs(
                                    rpm
                            )
                                    * Math.PI
                                    * 2.0
                                    / 60.0;

                    return omega < 0.05
                            ? 0.0F
                            : (float) (
                            MAX_POWER
                                    / omega
                    );
                }

                @Override
                public float power() {
                    return crankTicks > 0
                            ? availablePower
                            : 0.0F;
                }

                @Override
                public Direction.Axis axis() {
                    return getBlockState()
                            .getValue(
                                    ManualCrankBlock.FACING
                            )
                            .getAxis();
                }

                @Override
                public float consumePower(
                        float requestedPower
                ) {
                    if (crankTicks <= 0) {
                        return 0.0F;
                    }

                    float granted = Math.min(Math.max(0, requestedPower), availablePower);
                    availablePower -= granted;
                    return granted;
                }

                @Override
                public int rotationDirection() {
                    return crankTicks > 0
                            ? 1
                            : 0;
                }
            };

    public ManualCrankBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                PowerContent.MANUAL_CRANK_ENTITY.get(),
                pos,
                state
        );
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            ManualCrankBlockEntity crank
    ) {
        if (crank.crankTicks > 0) {
            crank.crankTicks--;
        }

        crank.availablePower = crank.crankTicks > 0 ? MAX_POWER : 0;

        float target =
                crank.crankTicks > 0
                        ? ACTIVE_RPM
                        : 0.0F;

        crank.rpm +=
                (
                        target
                                - crank.rpm
                )
                        * 0.28F;

        if (Math.abs(
                crank.rpm
        ) < 0.01F
                && target == 0.0F) {
            crank.rpm =
                    0.0F;
        }

        crank.angle =
                wrap(
                        crank.angle
                                + crank.rpm
                                        * 0.30F
                );

        if (Math.floorMod(
                level.getGameTime()
                        + pos.asLong(),
                4
        ) == 0
                && (
                crank.crankTicks > 0
                        || Math.abs(
                        crank.rpm
                ) > 0.01F
        )) {
            crank.sync();
        }
    }

    public void crank(
            Player player
    ) {
        crankTicks =
                Math.max(
                        crankTicks,
                        32
                );

        if (level != null) {
            level.playSound(
                    null,
                    worldPosition,
                    SoundEvents.LEVER_CLICK,
                    SoundSource.BLOCKS,
                    0.45F,
                    0.86F
            );
        }

        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            WayAroundAdvancements.manualCrank(
                    serverPlayer
            );
        }

        sync();
    }

    @Nullable
    public IRotationalPower rotationOutput(
            @Nullable Direction side
    ) {
        if (side != null
                && side.getAxis()
                != output.axis()) {
            return null;
        }

        return output;
    }

    public float rpm() {
        return rpm;
    }

    public float angle() {
        return angle;
    }

    public boolean active() {
        return crankTicks > 0;
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

        tag.putInt(
                "CrankTicks",
                crankTicks
        );

        tag.putFloat(
                "Rpm",
                rpm
        );

        tag.putFloat(
                "Angle",
                angle
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

        crankTicks =
                Math.max(
                        0,
                        tag.getInt(
                                "CrankTicks"
                        )
                );

        rpm =
                tag.getFloat(
                        "Rpm"
                );

        angle =
                wrap(
                        tag.getFloat(
                                "Angle"
                        )
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
