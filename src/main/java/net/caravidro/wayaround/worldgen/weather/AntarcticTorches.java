package net.caravidro.wayaround.worldgen.weather;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldgen.WayAroundBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AntarcticTorches {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(WayAround.MODID);
    public static final net.neoforged.neoforge.registries.DeferredBlock<TorchBlock> UNLIT =
            BLOCKS.register("unlit_torch", () -> new TorchBlock(ParticleTypes.SMOKE,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.TORCH).lightLevel(state -> 0)) {
                @Override
                public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {}
            });
    public static final net.neoforged.neoforge.registries.DeferredBlock<WallTorchBlock> UNLIT_WALL =
            BLOCKS.register("unlit_wall_torch", () -> new WallTorchBlock(ParticleTypes.SMOKE,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.WALL_TORCH).lightLevel(state -> 0)) {
                @Override
                public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {}
            });

    public static boolean isTorch(BlockState state) {
        return state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH);
    }

    public static void check(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!isTorch(state)) return;
        if (level.canSeeSky(pos) && level.getBiome(pos).is(WayAroundBiomes.ANTARCTIC_ICE_SHEET)
                && (BlizzardManager.getIntensity(level, Vec3.atCenterOf(pos)) > 0.0
                    || random.nextFloat() < 0.05F)) {
            BlockState unlit = state.is(Blocks.WALL_TORCH)
                    ? UNLIT_WALL.get().defaultBlockState().setValue(WallTorchBlock.FACING, state.getValue(WallTorchBlock.FACING))
                    : UNLIT.get().defaultBlockState();
            if (level.setBlock(pos, unlit, 3)) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5,
                        pos.getY() + 0.8, pos.getZ() + 0.5, 12, 0.1, 0.15, 0.1, 0.02);
            }
        } else {
            level.scheduleTick(pos, state.getBlock(), 100);
        }
    }
}
