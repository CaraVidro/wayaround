package net.caravidro.wayaround.worldgen.feature;

import com.mojang.serialization.Codec;
import net.caravidro.wayaround.block.PrioriteBlock;
import net.caravidro.wayaround.content.WayAroundContent;
import net.caravidro.wayaround.worldgen.geography.AntarcticField;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class PrioriteCraterFeature extends Feature<NoneFeatureConfiguration> {
    public PrioriteCraterFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int centerX = (origin.getX() & ~15) + 8;
        int centerZ = (origin.getZ() & ~15) + 8;
        if (!AntarcticField.isAntarctic(centerX, centerZ)) {
            return false;
        }

        // O raio alcanca apenas o chunk central e seus vizinhos imediatos.
        int radius = 10 + random.nextInt(5);
        int depth = 12 + random.nextInt(9);
        int centerSurface = level.getHeight(Heightmap.Types.WORLD_SURFACE, centerX, centerZ) - 1;
        if (centerSurface <= level.getSeaLevel() + 2) {
            return false;
        }
        int bottomY = Math.max(level.getMinBuildHeight() + 4, centerSurface - depth);
        int poolRadius = 2 + random.nextInt(2);
        int rimRadius = poolRadius + 1;
        double flatRadius = rimRadius + 2.0;
        int diameter = radius * 2 + 1;
        int[][] surfaces = new int[diameter][diameter];

        // Captura o terreno antes de escavar e valida toda a area de escrita.
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                int x = centerX + dx;
                int z = centerZ + dz;
                if (!level.ensureCanWrite(new BlockPos(x, bottomY, z))) {
                    return false;
                }
                surfaces[dx + radius][dz + radius] =
                        level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            }
        }

        // Fundo amplo e plano, com paredes que sobem suavemente ate a borda.
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > radius) {
                    continue;
                }
                int x = centerX + dx;
                int z = centerZ + dz;
                int surfaceY = surfaces[dx + radius][dz + radius];
                double slope = Math.clamp((distance - flatRadius) / (radius - flatRadius), 0.0, 1.0);
                double wall = slope * slope * (3.0 - 2.0 * slope);
                int floorY = bottomY + (int) Math.round(Math.max(0, surfaceY - bottomY) * wall);
                for (int y = surfaceY; y > floorY; y--) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }

                // Base solida de tres blocos, com manchas de pedra e gelo.
                double material = Math.sin(x * 0.37 + z * 0.21)
                        + Math.sin(x * 0.15 - z * 0.43);
                BlockState floor = material > 0.35
                        ? Blocks.STONE.defaultBlockState()
                        : Blocks.PACKED_ICE.defaultBlockState();
                for (int y = floorY - 2; y <= floorY; y++) {
                    level.setBlock(new BlockPos(x, y, z), floor, 2);
                }
            }
        }

        // Bacia central: base de gelo azul e borda um bloco acima do piso.
        // A priorita fica dentro da borda; o restante do fundo fica exposto.
        int prioritePlaced = 0;
        for (int dx = -rimRadius; dx <= rimRadius; dx++) {
            for (int dz = -rimRadius; dz <= rimRadius; dz++) {
                int distanceSquared = dx * dx + dz * dz;
                if (distanceSquared > rimRadius * rimRadius) {
                    continue;
                }
                BlockPos base = new BlockPos(centerX + dx, bottomY, centerZ + dz);
                level.setBlock(base, Blocks.BLUE_ICE.defaultBlockState(), 2);
                if (distanceSquared > poolRadius * poolRadius) {
                    level.setBlock(base.above(), Blocks.BLUE_ICE.defaultBlockState(), 2);
                } else {
                    BlockState priorite = WayAroundContent.PRIORITE.get().defaultBlockState()
                            .setValue(PrioriteBlock.AMOUNT, 4);
                    if (level.setBlock(base.above(), priorite, 2)) {
                        prioritePlaced++;
                    }
                }
            }
        }
        return prioritePlaced > 0;
    }
}
