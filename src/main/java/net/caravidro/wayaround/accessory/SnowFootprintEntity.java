package net.caravidro.wayaround.accessory;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Thin physical boot mark that sits on top of snow without modifying it.
 *
 * The footprint owns no snow state: the supporting snow is the source of
 * truth. If that support disappears, the mark removes itself.
 */
public final class SnowFootprintEntity extends Entity {

    private static final EntityDataAccessor<Float> MARK_YAW =
            SynchedEntityData.defineId(
                    SnowFootprintEntity.class,
                    EntityDataSerializers.FLOAT
            );

    private BlockPos support =
            BlockPos.ZERO;

    public SnowFootprintEntity(
            EntityType<? extends SnowFootprintEntity> type,
            Level level
    ) {
        super(
                type,
                level
        );

        noPhysics =
                true;
    }

    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder
    ) {
        builder.define(
                MARK_YAW,
                0.0F
        );
    }

    public void configure(
            BlockPos support,
            float yaw
    ) {
        this.support =
                support.immutable();

        entityData.set(
                MARK_YAW,
                yaw
        );

        snapToSnow();
    }

    public float markYaw() {
        return entityData.get(
                MARK_YAW
        );
    }

    public BlockPos support() {
        return support;
    }

    @Override
    public void tick() {
        super.tick();

        BlockState state =
                level().getBlockState(
                        support
                );

        if (!isSnow(
                state
        )) {
            if (!level().isClientSide) {
                discard();
            }
            return;
        }

        if (!level().isClientSide) {
            snapToSnow();
        }
    }

    private void snapToSnow() {
        BlockState state =
                level().getBlockState(
                        support
                );

        if (!isSnow(
                state
        )) {
            return;
        }

        double surface =
                surfaceY(
                        support,
                        state
                );

        setPos(
                support.getX()
                        + 0.5,
                surface
                        + 0.004,
                support.getZ()
                        + 0.5
        );
    }

    public static boolean isSnow(
            BlockState state
    ) {
        return state.is(
                Blocks.SNOW
        )
                || state.is(
                Blocks.SNOW_BLOCK
        )
                || state.is(
                Blocks.POWDER_SNOW
        );
    }

    public static double surfaceY(
            BlockPos pos,
            BlockState state
    ) {
        if (state.is(
                Blocks.SNOW
        )
                && state.hasProperty(
                SnowLayerBlock.LAYERS
        )) {
            return pos.getY()
                    + state.getValue(
                    SnowLayerBlock.LAYERS
            ) / 8.0;
        }

        return pos.getY()
                + 1.0;
    }

    @Override
    protected void readAdditionalSaveData(
            CompoundTag tag
    ) {
        support =
                new BlockPos(
                        tag.getInt(
                                "SupportX"
                        ),
                        tag.getInt(
                                "SupportY"
                        ),
                        tag.getInt(
                                "SupportZ"
                        )
                );

        entityData.set(
                MARK_YAW,
                tag.getFloat(
                        "Yaw"
                )
        );
    }

    @Override
    protected void addAdditionalSaveData(
            CompoundTag tag
    ) {
        tag.putInt(
                "SupportX",
                support.getX()
        );

        tag.putInt(
                "SupportY",
                support.getY()
        );

        tag.putInt(
                "SupportZ",
                support.getZ()
        );

        tag.putFloat(
                "Yaw",
                markYaw()
        );
    }
}
