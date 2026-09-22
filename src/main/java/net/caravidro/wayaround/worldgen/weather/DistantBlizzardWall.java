package net.caravidro.wayaround.worldgen.weather;

import net.caravidro.wayaround.particle.WayAroundParticles;

import net.minecraft.core.particles.ParticleTypes;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import net.minecraft.world.level.levelgen.Heightmap;

import net.minecraft.world.phys.Vec3;

public final class DistantBlizzardWall {

    /*
     * =====================================================
     * CONFIG PRINCIPAL
     * =====================================================
     */

    /*
     * Largura total da frente.
     */
    private static final double WALL_WIDTH =
            190.0;

    /*
     * Profundidade da massa.
     *
     * Maior =
     * parece menos uma parede 2D.
     */
    private static final double WALL_DEPTH =
            38.0;

    /*
     * Altura aproximada.
     */
    private static final double WALL_HEIGHT =
            28.0;

    /*
     * Quantas colunas existem horizontalmente.
     */
    private static final int HORIZONTAL_SAMPLES =
            20;

    /*
     * Quantos níveis verticais.
     */
    private static final int VERTICAL_SAMPLES =
            4;

    /*
     * Massa rasteira.
     */
    private static final int GROUND_SAMPLES =
            24;

    private DistantBlizzardWall() {
    }

    public static void spawn(
            ServerLevel level,
            ServerPlayer player,
            BlizzardManager.Blizzard storm
    ) {

        RandomSource random =
                level.getRandom();

        Vec3 playerPosition =
                player.position();

        Vec3 stormCenter =
                storm.center;

        /*
         * Direção:
         *
         * centro da tempestade -> player
         */
        Vec3 outward =
                new Vec3(
                        playerPosition.x
                        -
                        stormCenter.x,

                        0.0,

                        playerPosition.z
                        -
                        stormCenter.z
                );

        if (
                outward.lengthSqr()
                <
                0.0001
        ) {
            return;
        }

        outward =
                outward.normalize();

        /*
         * Vetor lateral.
         */
        Vec3 side =
                new Vec3(
                        -outward.z,
                        0.0,
                        outward.x
                );

        /*
         * Ponto da circunferência da
         * tempestade mais próximo do player.
         */
        Vec3 edgeCenter =
                stormCenter.add(
                        outward.scale(
                                storm.radius
                        )
                );

        spawnMainWall(
                level,
                player,
                random,

                edgeCenter,
                outward,
                side
        );

        spawnGroundCloud(
                level,
                player,
                random,

                edgeCenter,
                outward,
                side
        );
    }

    /*
     * =====================================================
     * PAREDE PRINCIPAL
     * =====================================================
     */

    private static void spawnMainWall(
            ServerLevel level,
            ServerPlayer player,
            RandomSource random,

            Vec3 edgeCenter,
            Vec3 outward,
            Vec3 side
    ) {

        for (
                int horizontalIndex = 0;
                horizontalIndex
                <
                HORIZONTAL_SAMPLES;
                horizontalIndex++
        ) {

            double horizontalT =
                    horizontalIndex
                    /
                    (double) (
                            HORIZONTAL_SAMPLES
                            -
                            1
                    );

            /*
             * -95 ... +95 aproximadamente.
             */
            double lateral =
                    (
                            horizontalT
                            -
                            0.5
                    )
                    *
                    WALL_WIDTH;

            /*
             * 0 nas pontas
             * 1 no centro.
             */
            double centerStrength =
                    1.0
                    -
                    Math.abs(
                            horizontalT
                            -
                            0.5
                    )
                    *
                    2.0;

            centerStrength =
                    Mth.clamp(
                            centerStrength,
                            0.0,
                            1.0
                    );

            /*
             * Mantém a parede relativamente
             * larga e não só concentrada
             * no meio.
             */
            centerStrength =
                    Math.pow(
                            centerStrength,
                            0.55
                    );

            for (
                    int verticalIndex = 0;
                    verticalIndex
                    <
                    VERTICAL_SAMPLES;
                    verticalIndex++
            ) {

                double verticalT =
                        verticalIndex
                        /
                        (double) (
                                VERTICAL_SAMPLES
                        );

                /*
                 * Profundidade aleatória.
                 */
                double depth =
                        random.nextDouble()
                        *
                        WALL_DEPTH;

                /*
                 * Jitter lateral.
                 *
                 * Quebra a grade perfeita.
                 */
                double lateralJitter =
                        (
                                random.nextDouble()
                                -
                                0.5
                        )
                        *
                        7.0;

                Vec3 position =
                        edgeCenter

                                .add(
                                        side.scale(
                                                lateral
                                                +
                                                lateralJitter
                                        )
                                )

                                /*
                                 * Para dentro da tempestade.
                                 */
                                .subtract(
                                        outward.scale(
                                                depth
                                        )
                                );

                int blockX =
                        Mth.floor(
                                position.x
                        );

                int blockZ =
                        Mth.floor(
                                position.z
                        );

                int groundY =
                        level.getHeight(
                                Heightmap.Types
                                        .MOTION_BLOCKING_NO_LEAVES,

                                blockX,
                                blockZ
                        );

                /*
                 * Mais concentração perto do chão.
                 */
                double height =
                        2.0

                        +
                        verticalT
                        *
                        WALL_HEIGHT

                        +
                        random.nextDouble()
                        *
                        4.0;

                double particleY =
                        groundY
                        +
                        height;

                /*
                 * Quantidade de fumaça.
                 */
                int cloudCount =
                        3
                        +
                        (int) (
                                centerStrength
                                *
                                5.0
                        );

                /*
                 * =================================================
                 * FUMAÇA BRANCA
                 * =================================================
                 */

                level.sendParticles(
                        player,

                        WayAroundParticles
                                .BLIZZARD_CLOUD
                                .get(),

                        true,

                        position.x,
                        particleY,
                        position.z,

                        cloudCount,

                        /*
                         * Spread.
                         */
                        3.4,
                        2.4,
                        3.4,

                        /*
                         * Velocidade.
                         */
                        0.015
                );

                /*
                 * =================================================
                 * NEVE MISTURADA
                 * =================================================
                 */

                if (
                        random.nextFloat()
                        <
                        0.68F
                ) {

                    int snowCount =
                            2
                            +
                            (int) (
                                    centerStrength
                                    *
                                    5.0
                            );

                    level.sendParticles(
                            player,

                            ParticleTypes
                                    .SNOWFLAKE,

                            true,

                            position.x,
                            particleY,
                            position.z,

                            snowCount,

                            4.0,
                            2.8,
                            4.0,

                            0.16
                    );
                }
            }
        }
    }

    /*
     * =====================================================
     * MASSA RASTEIRA
     * =====================================================
     *
     * É essa parte que faz a tempestade
     * parecer realmente tocar o chão.
     */

    private static void spawnGroundCloud(
            ServerLevel level,
            ServerPlayer player,
            RandomSource random,

            Vec3 edgeCenter,
            Vec3 outward,
            Vec3 side
    ) {

        for (
                int i = 0;
                i < GROUND_SAMPLES;
                i++
        ) {

            double lateral =
                    (
                            random.nextDouble()
                            -
                            0.5
                    )
                    *
                    WALL_WIDTH;

            double depth =
                    random.nextDouble()
                    *
                    WALL_DEPTH;

            Vec3 position =
                    edgeCenter

                            .add(
                                    side.scale(
                                            lateral
                                    )
                            )

                            .subtract(
                                    outward.scale(
                                            depth
                                    )
                            );

            int blockX =
                    Mth.floor(
                            position.x
                    );

            int blockZ =
                    Mth.floor(
                            position.z
                    );

            int groundY =
                    level.getHeight(
                            Heightmap.Types
                                    .MOTION_BLOCKING_NO_LEAVES,

                            blockX,
                            blockZ
                    );

            double y =
                    groundY
                    +
                    0.8
                    +
                    random.nextDouble()
                    *
                    4.5;

            /*
             * Fumaça rasteira densa.
             */
            level.sendParticles(
                    player,

                    WayAroundParticles
                            .BLIZZARD_CLOUD
                            .get(),

                    true,

                    position.x,
                    y,
                    position.z,

                    4,

                    4.0,
                    1.1,
                    4.0,

                    0.018
            );

            /*
             * Neve rasteira.
             */
            if (
                    random.nextFloat()
                    <
                    0.55F
            ) {

                level.sendParticles(
                        player,

                        ParticleTypes
                                .SNOWFLAKE,

                        true,

                        position.x,
                        y + 1.0,
                        position.z,

                        4,

                        4.0,
                        1.5,
                        4.0,

                        0.20
                );
            }
        }
    }
}