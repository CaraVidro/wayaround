package net.caravidro.wayaround.industrial.power;

import com.mojang.serialization.MapCodec;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
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
