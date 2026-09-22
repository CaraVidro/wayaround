package net.caravidro.wayaround.worldgen.weather;

import net.caravidro.wayaround.worldgen.WayAroundBiomes;
import net.caravidro.wayaround.worldgen.geography.AntarcticField;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/** Antarctic surface water freezes in clear weather too, including the middle of pools. */
public final class AntarcticWaterFreezing {
    private AntarcticWaterFreezing() {}

    public static void tickChunk(ServerLevel level, LevelChunk chunk, int randomTickSpeed) {
        if (randomTickSpeed <= 0 || !level.dimension().equals(Level.OVERWORLD)) return;

        int column = AntarcticFreezingSampling.column(level.getGameTime(), chunk.getPos().x, chunk.getPos().z);
        int localX = column & 15;
        int localZ = column >>> 4;
        int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, localX, localZ);
        if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight() - 1) return;

        BlockPos pos = new BlockPos(chunk.getPos().getMinBlockX() + localX, y,
                chunk.getPos().getMinBlockZ() + localZ);
        BlockState state = chunk.getBlockState(pos);
        // Never replace flowing water, waterlogged blocks, cauldrons or submerged vegetation.
        if (!state.is(Blocks.WATER) || !state.getFluidState().isSource()) return;
        if (!AntarcticField.isAntarctic(pos.getX(), pos.getZ())
                && !level.getBiome(pos).is(WayAroundBiomes.ANTARCTIC_ICE_SHEET)) return;

        // The highest non-air block is the water itself: roofs (including glass) and caves shelter it.
        if (!chunk.getBlockState(pos.above()).isAir() || !level.canSeeSky(pos.above())) return;
        // Keep the vanilla light/heat protection and never request an unloaded neighbor during updates.
        if (level.getBrightness(LightLayer.BLOCK, pos) >= 10 || !level.isAreaLoaded(pos, 1)) return;

        level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
    }
}
