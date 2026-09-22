package net.caravidro.wayaround.worldgen.feature;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import net.minecraft.world.level.WorldGenLevel;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;

import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.level.levelgen.Heightmap;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class AntarcticSnowMoundFeature
        extends Feature<NoneFeatureConfiguration> {

    public AntarcticSnowMoundFeature(
            Codec<NoneFeatureConfiguration> codec
    ) {
        super(codec);
    }

    @Override
    public boolean place(
            FeaturePlaceContext<NoneFeatureConfiguration> context
    ) {

        WorldGenLevel level =
                context.level();

        BlockPos origin =
                context.origin();

        RandomSource random =
                context.random();

        /*
         * ============================================
         * CONFIG PRINCIPAL
         * ============================================
         */

        /*
         * Tamanho do montinho.
         *
         * 2..5
         */
        int radius =
                2
                +
                random.nextInt(4);

        /*
         * Altura máxima em layers no centro.
         *
         * 3..6 layers extras
         */
        int maxCoreLayers =
                3
                +
                random.nextInt(4);

        /*
         * Pequena "manta" ao redor.
         */
        int blanketRadius =
                radius + 2;

        boolean placedAnything =
                false;

        for (
                int dx = -blanketRadius;
                dx <= blanketRadius;
                dx++
        ) {
            for (
                    int dz = -blanketRadius;
                    dz <= blanketRadius;
                    dz++
            ) {

                double dist =
                        Math.sqrt(
                                dx * dx
                                +
                                dz * dz
                        );

                if (
                        dist > blanketRadius
                ) {
                    continue;
                }

                int worldX =
                        origin.getX() + dx;

                int worldZ =
                        origin.getZ() + dz;

                BlockPos topAirPos =
                        level.getHeightmapPos(
                                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                new BlockPos(worldX, 0, worldZ)
                        );

                // The heightmap can include snow layers; reuse that position instead of
                // treating a thin layer as the supporting ground and skipping the mound.
                if (level.getBlockState(topAirPos.below()).is(Blocks.SNOW)) {
                    topAirPos = topAirPos.below();
                }

                BlockPos groundPos =
                        topAirPos.below();

                if (
                        groundPos.getY()
                        <=
                        level.getMinBuildHeight()
                ) {
                    continue;
                }

                BlockState groundState =
                        level.getBlockState(groundPos);

                /*
                 * Só trabalha em superfícies sólidas.
                 */
                if (
                        !groundState.isFaceSturdy(
                                level,
                                groundPos,
                                Direction.UP
                        )
                ) {
                    continue;
                }

                /*
                 * Não queremos mexer com água.
                 */
                if (
                        !groundState.getFluidState().isEmpty()
                ) {
                    continue;
                }

                // Acumula neve apenas sobre o manto de neve existente.
                // Esta feature roda depois das crateras e tambem pode alcancar
                // chunks vizinhos: converter qualquer solido em neve apagava
                // o piso, a borda de gelo azul e a priorita das outras features.
                if (!groundState.is(Blocks.SNOW_BLOCK)) {
                    continue;
                }

                int targetLayers =
                        calculateTargetLayers(
                                dist,
                                radius,
                                blanketRadius,
                                maxCoreLayers,
                                random
                        );

                if (
                        targetLayers <= 0
                ) {
                    continue;
                }

                BlockState existingTop =
                        level.getBlockState(topAirPos);

                /*
                 * Se já existir snow layer,
                 * podemos engrossar.
                 */
                if (
                        existingTop.is(Blocks.SNOW)
                ) {

                    int currentLayers =
                            existingTop.getValue(
                                    SnowLayerBlock.LAYERS
                            );

                    int newLayers =
                            Mth.clamp(
                                    Math.max(
                                            currentLayers,
                                            targetLayers
                                    ),
                                    1,
                                    8
                            );

                    if (
                            newLayers != currentLayers
                    ) {

                        level.setBlock(
                                topAirPos,
                                Blocks.SNOW
                                        .defaultBlockState()
                                        .setValue(
                                                SnowLayerBlock.LAYERS,
                                                newLayers
                                        ),
                                2
                        );

                        placedAnything = true;
                    }

                    continue;
                }

                /*
                 * Só coloca layer se tiver ar em cima.
                 */
                if (
                        !existingTop.isAir()
                ) {
                    continue;
                }

                level.setBlock(
                        topAirPos,
                        Blocks.SNOW
                                .defaultBlockState()
                                .setValue(
                                        SnowLayerBlock.LAYERS,
                                        Mth.clamp(
                                                targetLayers,
                                                1,
                                                8
                                        )
                                ),
                        2
                );

                placedAnything = true;
            }
        }

        return placedAnything;
    }

    private static int calculateTargetLayers(
            double dist,
            int coreRadius,
            int blanketRadius,
            int maxCoreLayers,
            RandomSource random
    ) {

        /*
         * ============================================
         * NÚCLEO DO MONTINHO
         * ============================================
         */
        if (
                dist <= coreRadius
        ) {

            double falloff =
                    1.0
                    -
                    (
                            dist
                            /
                            Math.max(1.0, coreRadius)
                    );

            /*
             * Curva suave:
             * centro mais alto, bordas mais delicadas.
             */
            int layers =
                    1
                    +
                    Mth.floor(
                            falloff
                            *
                            maxCoreLayers
                    );

            /*
             * Variação caótica bonitinha.
             */
            if (
                    random.nextFloat() < 0.20F
            ) {
                layers--;
            }

            return Math.max(1, layers);
        }

        /*
         * ============================================
         * MANTA EXTERNA
         * ============================================
         *
         * Fora do núcleo, ainda colocamos
         * 0..2 layers para o bioma não ficar
         * com cara de piso liso.
         */
        if (
                dist <= blanketRadius
        ) {

            float chance =
                    0.45F;

            if (
                    random.nextFloat() > chance
            ) {
                return 0;
            }

            return random.nextBoolean()
                    ? 1
                    : 2;
        }

        return 0;
    }
}
