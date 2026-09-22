package net.caravidro.wayaround.mixin;

import net.caravidro.wayaround.worldgen.weather.AntarcticTorches;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TorchBlock.class)
public abstract class AntarcticTorchMixin extends Block {
    protected AntarcticTorchMixin(Properties properties) { super(properties); }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (!level.isClientSide && AntarcticTorches.isTorch(state)) {
            level.scheduleTick(pos, state.getBlock(), 100);
        }
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        // BlockState caches this during bootstrap, before Blocks.TORCH is assigned.
        Class<?> blockClass = getClass();
        return blockClass == TorchBlock.class || blockClass == WallTorchBlock.class
                || super.isRandomlyTicking(state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Existing torches start their periodic checks when first randomly ticked.
        if (AntarcticTorches.isTorch(state) && !level.getBlockTicks().hasScheduledTick(pos, state.getBlock())) {
            level.scheduleTick(pos, state.getBlock(), 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        AntarcticTorches.check(state, level, pos, random);
    }
}
