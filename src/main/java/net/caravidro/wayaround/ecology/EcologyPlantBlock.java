package net.caravidro.wayaround.ecology;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class EcologyPlantBlock extends BushBlock {

    public static final MapCodec<EcologyPlantBlock> CODEC =
            simpleCodec(EcologyPlantBlock::new);

    public EcologyPlantBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        return state.isSolidRender(level, pos)
                || super.mayPlaceOn(state, level, pos);
    }
    @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.core.Direction face){return state.getFluidState().isEmpty()?100:0;}
    @Override public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.core.Direction face){return state.getFluidState().isEmpty()?60:0;}
}
