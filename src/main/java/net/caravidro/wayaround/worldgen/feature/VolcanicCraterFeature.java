package net.caravidro.wayaround.worldgen.feature;

import com.mojang.serialization.Codec;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.caravidro.wayaround.worldgen.geography.VolcanicField;
import net.caravidro.wayaround.worldgen.terrain.VolcanicTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Chunk-local crater finishing pass.
 *
 * The mountain itself is density terrain. This feature only adds the lava lake,
 * magma crust and a few geothermal vents, so it never attempts a giant
 * cross-chunk write.
 */
public final class VolcanicCraterFeature extends Feature<NoneFeatureConfiguration> {

    public VolcanicCraterFeature(
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
                WorldFeature.VOLCANIC_REGIONS
        )) {
            return false;
        }

        WorldGenLevel level =
                context.level();

        BlockPos origin =
                context.origin();

        RandomSource random =
                context.random();

        int baseX =
                origin.getX()
                        & ~15;

        int baseZ =
                origin.getZ()
                        & ~15;

        VolcanicField.Volcano volcano =
                VolcanicField.nearest(
                        baseX + 8,
                        baseZ + 8
                );

        if (volcano == null) {
            return false;
        }

        int changed =
                0;

        for (int dx = 0;
             dx < 16;
             dx++) {
            for (int dz = 0;
                 dz < 16;
                 dz++) {

                int x =
                        baseX + dx;

                int z =
                        baseZ + dz;

                double crater =
                        VolcanicField.craterStrength(
                                x,
                                z
                        );

                if (crater
                        <= 0.02) {
                    continue;
                }

                int floorY =
                        (int) Math.floor(
                                VolcanicTerrain.surfaceHeight(
                                        x,
                                        z
                                )
                        );

                floorY =
                        Math.max(
                                level.getMinBuildHeight()
                                        + 3,
                                Math.min(
                                        level.getMaxBuildHeight()
                                                - 3,
                                        floorY
                                )
                        );

                BlockPos floor =
                        new BlockPos(
                                x,
                                floorY,
                                z
                        );

                if (!level.ensureCanWrite(
                        floor
                )) {
                    continue;
                }

                if (crater
                        > 0.32) {
                    level.setBlock(
                            floor,
                            crater > 0.72
                                    ? Blocks.MAGMA_BLOCK
                                    .defaultBlockState()
                                    : Blocks.BLACKSTONE
                                    .defaultBlockState(),
                            2
                    );

                    int lavaTop =
                            Math.min(
                                    volcano.lavaLevel(),
                                    level.getMaxBuildHeight()
                                            - 2
                            );

                    if (floorY
                            < lavaTop) {

                        for (int y = floorY + 1;
                             y <= lavaTop;
                             y++) {

                            BlockPos pos =
                                    new BlockPos(
                                            x,
                                            y,
                                            z
                                    );

                            if (!level.ensureCanWrite(
                                    pos
                            )) {
                                break;
                            }

                            level.setBlock(
                                    pos,
                                    Blocks.LAVA
                                            .defaultBlockState(),
                                    2
                            );

                            changed++;
                        }
                    }
                }
            }
        }

        /*
         * A handful of chunk-local fumarole scars on the upper slopes.
         * Nothing here forces neighbouring chunks to exist.
         */
        if (volcano.activity()
                > 0.28) {

            int vents =
                    1
                            + (
                            volcano.activity()
                                    > 0.78
                            ? 2
                            : 0
                    );

            for (int i = 0;
                 i < vents;
                 i++) {

                int x =
                        baseX
                                + random.nextInt(
                                16
                        );

                int z =
                        baseZ
                                + random.nextInt(
                                16
                        );

                double distance =
                        volcano.distanceTo(
                                x,
                                z
                        );

                if (distance
                        < volcano.craterRadius()
                                * 1.15
                        || distance
                        > volcano.radius()
                                * 0.78) {
                    continue;
                }

                int y =
                        level.getHeight(
                                Heightmap.Types.WORLD_SURFACE_WG,
                                x,
                                z
                        )
                                - 1;

                BlockPos vent =
                        new BlockPos(
                                x,
                                y,
                                z
                        );

                if (!level.ensureCanWrite(
                        vent
                )) {
                    continue;
                }

                level.setBlock(
                        vent,
                        Blocks.MAGMA_BLOCK
                                .defaultBlockState(),
                        2
                );

                if (volcano.activity()
                        > 0.88
                        && random.nextFloat()
                        < 0.18F) {

                    BlockPos above =
                            vent.above();

                    if (level.ensureCanWrite(
                            above
                    )) {
                        level.setBlock(
                                above,
                                Blocks.LAVA
                                        .defaultBlockState(),
                                2
                        );
                    }
                }

                changed++;
            }
        }

        return changed
                > 0;
    }
}
