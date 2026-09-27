package net.caravidro.wayaround.media.broadcast;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BroadcastCableBlock extends Block {
    public static final MapCodec<BroadcastCableBlock> CODEC = simpleCodec(BroadcastCableBlock::new);
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 1, 16);

    public BroadcastCableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
