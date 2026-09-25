package net.caravidro.wayaround.industrial.power;

import com.mojang.serialization.MapCodec;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A 1:1 mechanical junction. Rotation entering from any face is available to
 * shafts on every other face. Ratios belong to future specialized gearboxes.
 */
public final class MechanicalGearboxBlock
        extends BaseEntityBlock {

    public static final MapCodec<MechanicalGearboxBlock> CODEC =
            simpleCodec(
                    MechanicalGearboxBlock::new
            );

    public MechanicalGearboxBlock(
            Properties properties
    ) {
        super(properties);
    }

    @Override
    protected MapCodec<MechanicalGearboxBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new MechanicalTransmissionBlockEntity(
                pos,
                state
        );
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack
    ) {
        super.setPlacedBy(
                level,
                pos,
                state,
                placer,
                stack
        );

        if (!level.isClientSide
                && level.getBlockEntity(
                pos
        ) instanceof MechanicalTransmissionBlockEntity transmission) {

            transmission.restoreFromItem(
                    stack
            );
        }
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState replacement,
            boolean moving
    ) {
        if (!state.is(
                replacement.getBlock()
        )
                && !level.isClientSide
                && level.getBlockEntity(
                pos
        ) instanceof MechanicalTransmissionBlockEntity transmission) {

            transmission.dropAssembly();
        }

        super.onRemove(
                state,
                level,
                pos,
                replacement,
                moving
        );
    }

    @Nullable
    @Override
    public <T extends BlockEntity>
            BlockEntityTicker<T> getTicker(
                    Level level,
                    BlockState state,
                    BlockEntityType<T> type
            ) {
        return null;
    }
}
