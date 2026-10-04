package net.caravidro.wayaround.worldgen.terrain;

import java.util.function.BiConsumer;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/** Bounded current-chunk terrain pass at SURFACE, before cross-chunk decoration. */
public final class DeepOceanTerrain {
    public static boolean deep(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> {
            String path = key.location().getPath();
            return key.location().getNamespace().equals("minecraft") &&
                    (path.equals("deep_ocean") || path.equals("deep_cold_ocean") ||
                     path.equals("deep_frozen_ocean") || path.equals("deep_lukewarm_ocean"));
        }).orElse(false);
    }

    public static void shape(WorldGenRegion region, ChunkAccess chunk, int seaLevel) {
        if (!region.getLevel().dimension().equals(Level.OVERWORLD) ||
                !WorldFeatureRuntime.serverEnabled(WorldFeature.LIVING_VEGETATION)) return;
        var pos = chunk.getPos();
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            int x = pos.getMinBlockX() + dx, z = pos.getMinBlockZ() + dz;
            int oldFloor = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, dx, dz) - 1;
            if (oldFloor >= seaLevel - 7 || !deep(region.getBiome(new BlockPos(x, seaLevel - 8, z)))) continue;
            carveColumn(chunk, (p, state) -> chunk.setBlockState(p, state, false), x, z,
                    oldFloor, seaLevel, chunk.getMinBuildHeight());
        }
    }

    /** Shared real-block pass for noise chunks and dedicated-server regression checks. */
    public static void carveColumn(BlockGetter blocks, BiConsumer<BlockPos, BlockState> write,
            int x, int z, int oldFloor, int seaLevel, int minY) {
        int target = targetFloor(oldFloor, seaLevel, minY, x, z);
        if (oldFloor <= target + 2) return;
        var cursor = new BlockPos.MutableBlockPos(x, oldFloor, z);
        for (int y = oldFloor; y > target; y--) {
            cursor.setY(y);
            var state = blocks.getBlockState(cursor);
            if (!state.hasBlockEntity() && !state.is(Blocks.BEDROCK) && !state.is(Blocks.WATER))
                write.accept(cursor.immutable(), Blocks.WATER.defaultBlockState());
        }
        for (int depth = 0; depth <= 3; depth++) {
            cursor.setY(target - depth);
            var previous = blocks.getBlockState(cursor);
            if (previous.hasBlockEntity() || previous.is(Blocks.BEDROCK)) continue;
            var floor = depth > 0 ? Blocks.DEEPSLATE : ((x * 31 + z * 17) & 7) == 0
                    ? Blocks.GRAVEL : ((x * 13 + z * 37) & 15) == 0 ? Blocks.TUFF : Blocks.DEEPSLATE;
            write.accept(cursor.immutable(), floor.defaultBlockState());
        }
    }

    public static int targetFloor(int oldFloor, int seaLevel, int minY, int x, int z) {
        int minFloor = minY + 4;
        if (oldFloor < minFloor || oldFloor >= seaLevel - 7) return oldFloor;
        double depthBlend = Mth.clamp((seaLevel - oldFloor - 10.0) / 30.0, 0.0, 1.0);
        double variation = Math.sin(x * .031 + z * .017) * .055 + Math.sin(x * .011 - z * .027) * .035;
        double interior = Mth.clamp(.08 + depthBlend * .92 + variation, .06, 1.0);
        double blend = interior * interior * (3 - 2 * interior);
        int abyss = Mth.clamp(-58 + (int)Math.round(Math.sin(x * .021 + z * .013) * 2.8 +
                Math.sin(x * .008 - z * .019) * 2.4 + Math.sin((x + z) * .0047) * 1.8), minFloor, -50);
        return Mth.clamp((int)Math.round(Mth.lerp(blend, oldFloor, abyss)), minFloor, oldFloor);
    }
    private DeepOceanTerrain() {}
}
