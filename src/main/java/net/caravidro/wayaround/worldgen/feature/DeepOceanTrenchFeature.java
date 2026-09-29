package net.caravidro.wayaround.worldgen.feature;

import com.mojang.serialization.Codec;

import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Turns vanilla deep-ocean biomes into actual abyssal basins.
 *
 * Modern biome JSON does not own terrain depth directly, so this feature runs
 * after the base terrain exists and lowers only the natural sea floor inside
 * deep-ocean chunks. It never writes outside the current 16x16 chunk.
 */
public final class DeepOceanTrenchFeature
        extends Feature<NoneFeatureConfiguration> {

    public DeepOceanTrenchFeature(
            Codec<NoneFeatureConfiguration> codec
    ) {
        super(codec);
    }

    @Override
    public boolean place(
            FeaturePlaceContext<NoneFeatureConfiguration> context
    ) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.LIVING_VEGETATION
        )) {
            return false;
        }

        WorldGenLevel level =
                context.level();

        BlockPos origin =
                context.origin();

        int baseX =
                origin.getX()
                        & ~15;

        int baseZ =
                origin.getZ()
                        & ~15;

        int seaLevel =
                level.getSeaLevel();

        int minFloor =
                Math.max(
                        level.getMinBuildHeight() + 9,
                        -48
                );

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

                int oldFloor =
                        level.getHeight(
                                Heightmap.Types.OCEAN_FLOOR_WG,
                                x,
                                z
                        )
                                - 1;

                /*
                 * Deep ocean should already be below sea level. If another
                 * structure/mod made this column land, do not excavate it.
                 */
                if (oldFloor
                        >= seaLevel - 7) {
                    continue;
                }

                double waveA =
                        Math.sin(
                                x * 0.031
                                        + z * 0.017
                        );

                double waveB =
                        Math.sin(
                                x * 0.011
                                        - z * 0.027
                        );

                int targetFloor =
                        Mth.clamp(
                                -38
                                        + (int)Math.round(
                                        waveA * 6.0
                                                + waveB * 5.0
                                ),
                                minFloor,
                                -24
                        );

                if (oldFloor
                        <= targetFloor + 3) {
                    continue;
                }

                BlockPos.MutableBlockPos cursor =
                        new BlockPos.MutableBlockPos(
                                x,
                                seaLevel,
                                z
                        );

                /*
                 * This feature deliberately runs at top-layer modification.
                 * Clean the entire water column first so vanilla kelp,
                 * seagrass, coral decorations and stale blocks from the old
                 * sea floor cannot remain suspended after the abyss is carved.
                 */
                for (int y = seaLevel;
                     y > targetFloor;
                     y--) {
                    cursor.setY(
                            y
                    );

                    BlockState old =
                            level.getBlockState(
                                    cursor
                            );

                    if (old.is(
                            Blocks.BEDROCK
                    )
                            || level.getBlockEntity(
                            cursor
                    ) != null) {
                        continue;
                    }

                    /*
                     * Everything below sea level becomes an uninterrupted
                     * water column. This intentionally removes kelp,
                     * seagrass/coral and any decoration that was attached to
                     * the pre-trench floor.
                     */
                    if (!old.is(
                            Blocks.WATER
                    )) {
                        level.setBlock(
                                cursor,
                                Blocks.WATER
                                        .defaultBlockState(),
                                2
                        );

                        changed++;
                    }
                }

                cursor.setY(
                        targetFloor
                );

                BlockState floor =
                        ((x * 31 + z * 17) & 7) == 0
                                ? Blocks.GRAVEL
                                .defaultBlockState()
                                : Blocks.DEEPSLATE
                                .defaultBlockState();

                level.setBlock(
                        cursor,
                        floor,
                        2
                );

                /*
                 * Give the abyss a solid base so later decorators have a real
                 * floor rather than a one-block shell above caves.
                 */
                for (int depth = 1;
                     depth <= 2;
                     depth++) {
                    cursor.setY(
                            targetFloor - depth
                    );

                    if (!level.getBlockState(
                            cursor
                    ).is(
                            Blocks.BEDROCK
                    )) {
                        level.setBlock(
                                cursor,
                                Blocks.DEEPSLATE
                                        .defaultBlockState(),
                                2
                        );
                    }
                }
            }
        }

        return changed > 0;
    }
}
