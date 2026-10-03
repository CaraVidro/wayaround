package net.caravidro.wayaround.ecology;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;

/** Cave biomes below an ocean must not switch off pressure or abyss darkness. */
public final class DeepOceanBiomes {
    private DeepOceanBiomes() {}
    public static boolean deep(Holder<Biome> biome) {
        return biome.unwrapKey().map(key->{
            String path=key.location().getPath();
            return path.equals("deep_ocean")||path.equals("deep_cold_ocean")
                    ||path.equals("deep_frozen_ocean")||path.equals("deep_lukewarm_ocean");
        }).orElse(false);
    }
    public static boolean contains(LevelReader level, BlockPos pos) {
        return deep(level.getBiome(pos)) || pos.getY()<level.getSeaLevel()-8
                && deep(level.getBiome(pos.atY(level.getSeaLevel()-8)));
    }
}
