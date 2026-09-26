package net.caravidro.wayaround.worldgen.weather;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.caravidro.wayaround.network.BlizzardStatePayload;
import net.caravidro.wayaround.particle.WayAroundParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.util.RandomSource;

import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class AntarcticBlizzard {

    private static final int PARTICLE_INTERVAL = 2;

    /*
     * Até essa distância FORA da tempestade
     * conseguimos enxergar a massa branca.
     */
    private static final double OUTSIDE_VIEW_DISTANCE =
            280.0;

    /*
     * Snowflakes dentro da tempestade.
     */
    private static final double FLAKE_START_DISTANCE =
            8.0;

    private static final double FLAKE_WIDTH =
            55.0;

    private AntarcticBlizzard() {
    }

    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.ANTARCTICA
        )) {
            return;
        }

        MinecraftServer server =
                event.getServer();

        /*
         * Resolve chunks que acabaram
         * de carregar.
         */
        BlizzardChunkTracker.tick(
                server
        );

        /*
         * ============================
         * TEMPESTADE NATURAL
         * ============================
         *
         * Uma tentativa a cada 20 segundos.
         */
        if (
                server.getTickCount()
                % 400
                == 0
        ) {

            tryNaturalBlizzards(
                    server
            );
        }

        /*
         * ============================
         * ACÚMULO NO MUNDO
         * ============================
         *
         * Independente do jogador.
         */
        if (
                server.getTickCount()
                % 5
                == 0
        ) {

            for (
                    ServerLevel level
                    :
                    server.getAllLevels()
            ) {

                BlizzardChunkTracker
                        .tickAccumulation(
                                level
                        );
            }

            syncPlayers(server);
        }

        /*
         * ============================
         * EFEITOS VISUAIS
         * ============================
         */
        if (
                server.getTickCount()
                % PARTICLE_INTERVAL
                != 0
        ) {
            return;
        }

        for (
                ServerPlayer player
                :
                server.getPlayerList()
                        .getPlayers()
        ) {

            renderStormForPlayer(
                    player
            );
        }
    }

    /*
     * =========================================================
     * NATURAL RANDOM STORMS
     * =========================================================
     */

    private static void tryNaturalBlizzards(
            MinecraftServer server
    ) {

        for (
                ServerLevel level
                :
                server.getAllLevels()
        ) {

            if (
                    BlizzardManager.hasActive(
                            level
                    )
            ) {
                continue;
            }

            RandomSource random =
                    level.getRandom();

            /*
             * Tentativa a cada 20 segundos.
             *
             * 1/30:
             *
             * média aproximada ~10 minutos.
             */
            if (
                    random.nextInt(30)
                    != 0
            ) {
                continue;
            }

            BlockPos position =
                    BlizzardChunkTracker
                            .getRandomKnownAntarcticPosition(
                                    level,
                                    random
                            );

            if (position == null) {
                continue;
            }

            /*
             * 320 - 650 blocos de raio.
             */
            double radius =
                    320.0
                    +
                    random.nextDouble()
                    * 330.0;

            /*
             * 2:30 - 6:00 minutos.
             */
            long duration =
                    20L
                    *
                    (
                            150L
                            +
                            random.nextInt(211)
                    );

            Vec3 center =
                    new Vec3(
                            position.getX() + 0.5,
                            100.0,
                            position.getZ() + 0.5
                    );

            BlizzardManager.start(
                    level,
                    center,
                    radius,
                    duration,
                    1.0
            );

            WayAround.LOGGER.info(
                    "Blizzard natural surgiu em X={} Z={} raio={}",
                    center.x,
                    center.z,
                    radius
            );
        }
    }

    /*
     * =========================================================
     * PLAYER VISUALS
     * =========================================================
     */

    private static void renderStormForPlayer(
            ServerPlayer player
    ) {

        ServerLevel level =
                player.serverLevel();

        BlizzardManager.Blizzard storm =
                BlizzardManager.get(level);

        if (storm == null) {
            return;
        }

        Vec3 position =
                player.position();

        double distance =
        player.position()
                .distanceTo(
                        storm.center
                );

double distanceFromEdge =
        distance
        -
        storm.radius;

/*
 * Player está fora,
 * mas consegue ver a frente chegando.
 */
if (
        distanceFromEdge > 0.0
        &&
        distanceFromEdge < 300.0
) {

    DistantBlizzardWall.spawn(
            level,
            player,
            storm
    );

    return;
}

        /*
         * =====================================================
         * DENTRO DA TEMPESTADE
         * =====================================================
         */

        double intensity =
                storm.intensityAt(
                        position,
                        level.getGameTime()
                );

        if (
                intensity <= 0.01
        ) {
            return;
        }

        /*
         * Está dentro de casa/caverna?
         *
         * Não nasce snowflake magicamente
         * no interior.
         */
        if (
                !level.canSeeSky(
                        player.blockPosition()
                                .above()
                )
        ) {
            return;
        }

        spawnInsideSnowflakes(
                level,
                player,
                intensity
        );
    }

    /*
     * =========================================================
     * GIANT DISTANT CLOUD
     * =========================================================
     */

    private static void spawnDistantStormCloud(
            ServerLevel level,
            ServerPlayer player,
            BlizzardManager.Blizzard storm,
            double distance
    ) {

        RandomSource random =
                level.getRandom();

        Vec3 playerPos =
                player.position();

        /*
         * Vetor do centro da storm
         * em direção ao jogador.
         */
        Vec3 outward =
                new Vec3(
                        playerPos.x
                        -
                        storm.center.x,

                        0.0,

                        playerPos.z
                        -
                        storm.center.z
                );

        if (
                outward.lengthSqr()
                < 0.0001
        ) {
            return;
        }

        outward =
                outward.normalize();

        /*
         * O ponto da borda da tempestade
         * que está mais próximo do jogador.
         */
        Vec3 edge =
                storm.center.add(
                        outward.scale(
                                storm.radius
                        )
                );

        Vec3 side =
                new Vec3(
                        -outward.z,
                        0.0,
                        outward.x
                );

        double strength =
                storm.globalStrength(
                        level.getGameTime()
                );

        int columns =
                3
                +
                (int) (
                        strength * 7.0
                );
                
        for (
                int i = 0;
                i < columns;
                i++
        ) {

            double lateral =
                    (
                            random.nextDouble()
                            -
                            0.5
                    )
                    * 80.0;

            double depth =
                    random.nextDouble()
                    * 25.0;

            /*
             * Altura relativamente próxima
             * ao horizonte do jogador.
             */
            double y =
                    player.getY()
                    +
                    5.0
                    +
                    random.nextDouble()
                    * 20.0;

            Vec3 cloud =
                    edge

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

            /*
             * CLOUD vanilla não deixa aumentar
             * a escala diretamente.
             *
             * Então vários CLOUD juntos parecem
             * uma partícula/massa gigantesca.
             *
             * longDistance=true.
             */
        level.sendParticles(
                player,
                WayAroundParticles.BLIZZARD_CLOUD.get(),
                true,
                cloud.x,
                y,
                cloud.z,
                8,
                5.0,
                3.0,
                5.0,
                0.01
        );

            /*
             * Um pouco de snowflake dentro
             * da "fumaça", mas só de longe.
             */
            if (
                    random.nextBoolean()
            ) {

                level.sendParticles(
                        player,

                        ParticleTypes.SNOWFLAKE,

                        true,

                        cloud.x,
                        y,
                        cloud.z,

                        9,

                        9.0,
                        6.0,
                        9.0,

                        0.10
                );
            }
        }
    }

    /*
     * =========================================================
     * INSIDE SNOWFLAKES
     * =========================================================
     */

    private static void spawnInsideSnowflakes(
            ServerLevel level,
            ServerPlayer player,
            double intensity
    ) {

        RandomSource random =
                level.getRandom();

        Vec3 wind =
                getWind(
                        level
                );

        Vec3 side =
                new Vec3(
                        -wind.z,
                        0.0,
                        wind.x
                ).normalize();

        Vec3 playerCenter =
                player.position()
                        .add(
                                0.0,
                                1.2,
                                0.0
                        );

        /*
         * Borda:
         * poucas.
         *
         * Centro:
         * desgraça branca.
         */                     

double centerBoost =
        Math.pow(
                intensity,
                2.2
        );

int flakes =
        4
        +
        (int) (
                intensity * 320.0
        )
        +
        (int) (
                centerBoost * 1200.0
        );
/*
 * =========================================================
 * WHITEOUT DO CENTRO
 * =========================================================
 */

if (
        intensity > 0.55
) {

    double whiteout =
            (
                    intensity
                    -
                    0.55
            )
            /
            0.45;

    whiteout =
            Math.max(
                    0.0,
                    Math.min(
                            1.0,
                            whiteout
                    )
            );

    int bursts =
            2
            +
            (int) (
                    whiteout * 8.0
            );

    for (
            int i = 0;
            i < bursts;
            i++
    ) {

        double distance =
                12.0
                +
                random.nextDouble()
                * 48.0;

        double lateral =
                (
                        random.nextDouble()
                        -
                        0.5
                )
                * 75.0;

        Vec3 burstPosition =
                playerCenter

                        .subtract(
                                wind.scale(
                                        distance
                                )
                        )

                        .add(
                                side.scale(
                                        lateral
                                )
                        )

                        .add(
                                0.0,

                                -1.0
                                +
                                random.nextDouble()
                                * 11.0,

                                0.0
                        );

        BlockPos burstBlock =
                BlockPos.containing(
                        burstPosition
                );

        /*
         * Não cria nevasca dentro
         * de parede/telhado/caverna.
         */
        if (
                !level.getBlockState(
                        burstBlock
                ).isAir()
        ) {
            continue;
        }

        if (
                !level.canSeeSky(
                        burstBlock
                )
        ) {
            continue;
        }

        /*
         * Pequena nuvem de snowflakes.
         */
        level.sendParticles(
                player,

                ParticleTypes.SNOWFLAKE,

                true,

                burstPosition.x,
                burstPosition.y,
                burstPosition.z,

                4
                +
                (int) (
                        whiteout * 10.0
                ),

                2.8,
                1.7,
                2.8,

                0.30
        );
    }
}
        for (
                int i = 0;
                i < flakes;
                i++
        ) {

            double lateral =
                    (
                            random.nextDouble()
                            -
                            0.5
                    )
                    * FLAKE_WIDTH;

            double height =
                    -2.0
                    +
                    random.nextDouble()
                    * 12.0;

            Vec3 start =
                    playerCenter

                            .subtract(
                                    wind.scale(
                                            FLAKE_START_DISTANCE
                                    )
                            )

                            .add(
                                    side.scale(
                                            lateral
                                    )
                            )

                            .add(
                                    0.0,
                                    height,
                                    0.0
                            );

            BlockPos startPos =
                    BlockPos.containing(
                            start
                    );

            /*
             * Não nasce dentro de montanha,
             * casa etc.
             */
            if (
                    !level.getBlockState(
                            startPos
                    ).isAir()
            ) {
                continue;
            }

            if (
                    !level.canSeeSky(
                            startPos
                    )
            ) {
                continue;
            }

            double fall =
                    -0.045
                    -
                    random.nextDouble()
                    * 0.07;

            /*
             * Enviado especificamente a este
             * jogador com longDistance=true.
             */
            level.sendParticles(
                    player,

                    ParticleTypes.SNOWFLAKE,

                    true,

                    start.x,
                    start.y,
                    start.z,

                    0,

                    // Horizontal transport is applied once by the shared client wind.
                    0.0,
                    fall,
                    0.0,

                    1.0
            );
        }
    }

    private static Vec3 getWind(
            ServerLevel level
    ) {

        double time =
                level.getGameTime();

        double angle = BlizzardWind.angle(time);

        return new Vec3(
                Math.cos(angle),
                0.0,
                Math.sin(angle)
        ).normalize();
    }

    /*
     * =========================================================
     * SERVER -> CLIENT INTENSITY
     * =========================================================
     */

    private static void syncPlayers(
            MinecraftServer server
    ) {

        for (
                ServerPlayer player
                :
                server.getPlayerList()
                        .getPlayers()
        ) {

            ServerLevel level =
                    player.serverLevel();

            double intensity =
                    BlizzardManager.getIntensity(
                            level,
                            player.position()
                    );

            PacketDistributor.sendToPlayer(
                    player,

                    new BlizzardStatePayload(
                            (float) intensity
                    )
            );
        }
    }
}
