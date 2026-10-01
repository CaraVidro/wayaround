package net.caravidro.wayaround.worldgen.feature;

import com.mojang.serialization.Codec;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.caravidro.wayaround.worldgen.geography.GreatRiftField;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Rare chunk-local seep pools at the bottom of the rift.
 */
public final class RiftSpringFeature extends Feature<NoneFeatureConfiguration> {

    public RiftSpringFeature(
            Codec<NoneFeatureConfiguration> codec
    ) {
        super(
                codec
        );
    }

    @Override
    public boolean place(
            FeaturePlaceContext<NoneFeatureConfiguration> context
    ) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.GREAT_RIFTS
        )) {
            return false;
        }

        WorldGenLevel level =
                context.level();

        RandomSource random =
                context.random();

        BlockPos origin =
                context.origin();

        if (random.nextInt(
                11
        ) != 0) {
            return false;
        }

        int baseX =
                origin.getX()
                        & ~15;

        int baseZ =
                origin.getZ()
                        & ~15;

        int centerX =
                baseX
                        + 5
                        + random.nextInt(
                        6
                );

        int centerZ =
                baseZ
                        + 5
                        + random.nextInt(
                        6
                );

        if (GreatRiftField.canyonStrength(
                centerX,
                centerZ
        ) < 0.72) {
            return false;
        }

        int surface =
                level.getHeight(
                        Heightmap.Types.WORLD_SURFACE_WG,
                        centerX,
                        centerZ
                )
                        - 1;

        if (surface
                < level.getSeaLevel()
                        - 8
                || surface
                > level.getMaxBuildHeight()
                        - 8) {
            return false;
        }

        int radius =
                2
                        + random.nextInt(
                        3
                );

        int changed =
                0;

        for (int dx = -radius;
             dx <= radius;
             dx++) {
            for (int dz = -radius;
                 dz <= radius;
                 dz++) {

                double distance =
                        Math.sqrt(
                                dx * dx
                                        + dz * dz
                        );

                if (distance
                        > radius
                        + 0.15) {
                    continue;
                }

                int x =
                        centerX + dx;

                int z =
                        centerZ + dz;

                int localSurface =
                        level.getHeight(
                                Heightmap.Types.WORLD_SURFACE_WG,
                                x,
                                z
                        )
                                - 1;

                if (Math.abs(
                        localSurface - surface
                ) > 3) {
                    continue;
                }

                BlockPos floor =
                        new BlockPos(
                                x,
                                localSurface - 1,
                                z
                        );

                BlockPos water =
                        floor.above();

                if (!level.ensureCanWrite(
                        floor
                )
                        || !level.ensureCanWrite(
                        water
                )) {
                    continue;
                }

                level.setBlock(
                        floor,
                        distance
                                > radius * 0.68
                                ? Blocks.MUD
                                .defaultBlockState()
                                : Blocks.CLAY
                                .defaultBlockState(),
                        2
                );

                level.setBlock(
                        water,
                        Blocks.WATER
                                .defaultBlockState(),
                        2
                );

                changed++;
            }
        }

        return changed
                > 0;
    }
}
