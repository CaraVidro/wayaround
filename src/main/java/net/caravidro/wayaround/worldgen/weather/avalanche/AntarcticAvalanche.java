package net.caravidro.wayaround.worldgen.weather.avalanche;

import net.caravidro.wayaround.particle.WayAroundParticles;
import net.caravidro.wayaround.worldgen.WayAroundBiomes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

public final class AntarcticAvalanche {

    /*
     * Movimento é atualizado todo tick.
     */

    /*
     * Deposição física:
     * 5 vezes por segundo.
     */
    private static final int DEPOSIT_INTERVAL =
            4;

    /*
     * Partículas:
     * 10 vezes por segundo.
     */
    private static final int PARTICLE_INTERVAL =
            2;

    /*
     * Tentativa natural:
     * a cada 20 segundos.
     */
    private static final int NATURAL_CHECK_INTERVAL =
            400;
    private static int getSnowDepth(
        ServerLevel level,
        int x,
        int z
) {

    int top =
            level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    x,
                    z
            ) - 1;

    int depth = 0;

    for (
            int y = top;
            y >= level.getMinBuildHeight();
            y--
    ) {

        BlockPos pos =
                new BlockPos(
                        x,
                        y,
                        z
                );

        var state =
                level.getBlockState(
                        pos
                );

        if (
                state.is(Blocks.SNOW_BLOCK)
                ||
                state.is(Blocks.POWDER_SNOW)
                ||
                state.is(Blocks.SNOW)
        ) {

            depth++;

        } else {

            break;
        }

        /*
         * Não precisamos procurar
         * infinitamente.
         */
        if (
                depth >= 8
        ) {
            break;
        }
    }

    return depth;
}
    /*
     * 1 / 90 a cada 20 segundos.
     *
     * Média aproximada:
     * ~30 minutos,
     * se houver terreno adequado.
     */
    private static final int NATURAL_CHANCE =
            90;

    private AntarcticAvalanche() {
    }

    public static void onServerTick(
            ServerTickEvent.Post event
    ) {

        MinecraftServer server =
                event.getServer();

        long tick =
                server.getTickCount();

        for (
                ServerLevel level
                :
                server.getAllLevels()
        ) {

            AvalancheManager.tick(
                    level
            );

            if (
                    tick
                    %
                    DEPOSIT_INTERVAL
                    ==
                    0
            ) {

                depositSnow(
                        level
                );
            }

            if (
                    tick
                    %
                    PARTICLE_INTERVAL
                    ==
                    0
            ) {

                render(
                        level
                );
            }

            if (
                    tick
                    %
                    NATURAL_CHECK_INTERVAL
                    ==
                    0
            ) {

                tryNaturalSpawn(
                        level
                );
            }
        }
    }

    private static void tryNaturalSpawn(
            ServerLevel level
    ) {

        /*
         * Uma já ativa:
         * não cria outra por enquanto.
         */
        if (
                AvalancheManager.activeCount(
                        level
                )
                > 0
        ) {
            return;
        }

        RandomSource random =
                level.getRandom();

        if (
                random.nextInt(
                        NATURAL_CHANCE
                )
                != 0
        ) {
            return;
        }

        List<ServerPlayer> candidates =
                new ArrayList<>();

        for (
                ServerPlayer player
                :
                level.players()
        ) {

            if (
                    level.getBiome(
                            player.blockPosition()
                    )
                    .is(
                            WayAroundBiomes
                                    .ANTARCTIC_ICE_SHEET
                    )
            ) {

                candidates.add(
                        player
                );
            }
        }

        if (
                candidates.isEmpty()
        ) {
            return;
        }

        ServerPlayer player =
                candidates.get(
                        random.nextInt(
                                candidates.size()
                        )
                );

        /*
         * Procura uma encosta próxima.
         */
        for (
                int attempt = 0;
                attempt < 16;
                attempt++
        ) {

            double angle =
                    random.nextDouble()
                    *
                    Math.PI
                    *
                    2.0;

            double distance =
                    80.0
                    +
                    random.nextDouble()
                    *
                    160.0;

            int x =
                    Mth.floor(
                            player.getX()
                            +
                            Math.cos(angle)
                            *
                            distance
                    );

            int z =
                    Mth.floor(
                            player.getZ()
                            +
                            Math.sin(angle)
                            *
                            distance
                    );

            int y =
                    level.getHeight(
                            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            x,
                            z
                    );

            BlockPos position =
                    new BlockPos(
                            x,
                            y,
                            z
                    );

            if (
                    !level.getBiome(
                            position
                    )
                    .is(
                            WayAroundBiomes
                                    .ANTARCTIC_ICE_SHEET
                    )
            ) {
                continue;
            }

            Vec3 downhill =
                    findDownhill(
                            level,
                            x,
                            z
                    );

            if (
                    downhill == null
            ) {
                continue;
            }

            double slope =
                    slopeStrength(
                            level,
                            x,
                            z
                    );

            /*
             * Precisa ser uma encosta
             * razoavelmente forte.
             */
            if (
                    slope < 7.0
            ) {
                continue;
            }

            double width =
                    130.0
                    +
                    random.nextDouble()
                    *
                    110.0;

            double depth =
                    24.0
                    +
                    random.nextDouble()
                    *
                    20.0;

            double speed =
                    0.65
                    +
                    random.nextDouble()
                    *
                    0.40;

            double maxDistance =
                    220.0
                    +
                    random.nextDouble()
                    *
                    260.0;

            AvalancheManager.start(
                    level,

                    new Vec3(
                            x,
                            y,
                            z
                    ),

                    downhill,

                    width,
                    depth,
                    speed,
                    maxDistance
            );

            return;
        }
    }

    private static void depositSnow(
            ServerLevel level
    ) {

        RandomSource random =
                level.getRandom();

        for (
                AvalancheManager.Avalanche avalanche
                :
                AvalancheManager.get(
                        level
                )
        ) {

            /*
             * Quanto maior a avalanche,
             * mais pontos de depósito.
             */
            int attempts =
                    Mth.clamp(
                            (int) (
                                    avalanche.width
                                    *
                                    0.70
                            ),

                            80,
                            190
                    );

            Vec3 side =
                    avalanche.sideDirection();

            for (
                    int i = 0;
                    i < attempts;
                    i++
            ) {

                double lateral =
                        (
                                random.nextDouble()
                                -
                                0.5
                        )
                        *
                        avalanche.width;

                /*
                 * A maior parte da massa
                 * fica atrás da frente.
                 */
                double behind =
                        random.nextDouble()
                        *
                        avalanche.depth;

                Vec3 position =
                        avalanche.center

                                .add(
                                        side.scale(
                                                lateral
                                        )
                                )

                                .subtract(
                                        avalanche.direction
                                                .scale(
                                                        behind
                                                )
                                );

                int x =
                        Mth.floor(
                                position.x
                        );

                int z =
                        Mth.floor(
                                position.z
                        );

                int y =
                        level.getHeight(
                                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                x,
                                z
                        );

                BlockPos surface =
                        new BlockPos(
                                x,
                                y,
                                z
                        );

                /*
                 * Nada de avalanche
                 * dentro de casas/cavernas.
                 *
                 * Ela cobre o TELHADO.
                 */
                if (
                        !level.canSeeSky(
                                surface
                        )
                ) {
                    continue;
                }

                if (
                    !level.getBiome(
                            surface
                    )
                    .is(
                            WayAroundBiomes
                                    .ANTARCTIC_ICE_SHEET
                    )
                ) {
                    continue;
                }

                /*
                 * Centro lateral da avalanche
                 * deposita mais.
                 */
                double lateralFactor =
                        1.0
                        -
                        Math.abs(
                                lateral
                        )
                        /
                        (
                                avalanche.width
                                *
                                0.5
                        );

                lateralFactor =
                        Mth.clamp(
                                lateralFactor,
                                0.0,
                                1.0
                        );

                /*
                 * Quantos Snow Blocks
                 * empilhar.
                 */
                int blocks =
                        1;

                if (
                        random.nextDouble()
                        <
                        0.28
                        *
                        lateralFactor
                ) {

                    blocks++;
                }

                if (
                        random.nextDouble()
                        <
                        0.08
                        *
                        lateralFactor
                ) {

                    blocks++;
                }

                BlockPos current =
                        surface;

                for (
                        int block = 0;
                        block < blocks;
                        block++
                ) {

                    if (
                            !level.getBlockState(
                                    current
                            ).isAir()
                    ) {
                        break;
                    }

                    /*
                     * Powder Snow raro.
                     */
                    boolean powder =
                            random.nextDouble()
                            <
                            0.06;

                    if (
                            powder
                    ) {

                        level.setBlock(
                                current,
                                Blocks.POWDER_SNOW
                                        .defaultBlockState(),
                                3
                        );

                    } else {

                        /*
                         * Snow Block é
                         * o depósito comum.
                         */
                        level.setBlock(
                                current,
                                Blocks.SNOW_BLOCK
                                        .defaultBlockState(),
                                3
                        );
                    }

                    current =
                            current.above();
                }

                /*
                 * Camada irregular no topo.
                 */
                if (
                        level.getBlockState(
                                current
                        ).isAir()
                        &&
                        random.nextDouble()
                        <
                        0.75
                ) {

                    int layers =
                            2
                            +
                            random.nextInt(
                                    7
                            );

                    level.setBlock(
                            current,

                            Blocks.SNOW
                                    .defaultBlockState()
                                    .setValue(
                                            SnowLayerBlock.LAYERS,
                                            layers
                                    ),

                            3
                    );
                }
            }
        }
    }

    private static void render(
            ServerLevel level
    ) {

        RandomSource random =
                level.getRandom();

        for (
                ServerPlayer player
                :
                level.players()
        ) {

            Vec3 playerPosition =
                    player.position();

            for (
                    AvalancheManager.Avalanche avalanche
                    :
                    AvalancheManager.get(
                            level
                    )
            ) {

                double maxViewDistance =
                        280.0
                        +
                        avalanche.width
                        *
                        0.5;

                if (
                        playerPosition.distanceToSqr(
                                avalanche.center
                        )
                        >
                        maxViewDistance
                        *
                        maxViewDistance
                ) {
                    continue;
                }

                Vec3 side =
                        avalanche.sideDirection();

                /*
                 * Vários pontos da parede.
                 */
                int wallSamples =
                        10;

                for (
                        int i = 0;
                        i < wallSamples;
                        i++
                ) {

                    double lateral =
                            (
                                    random.nextDouble()
                                    -
                                    0.5
                            )
                            *
                            avalanche.width;

                    double back =
                            random.nextDouble()
                            *
                            avalanche.depth
                            *
                            0.5;

                    Vec3 particlePosition =
                            avalanche.center

                                    .add(
                                            side.scale(
                                                    lateral
                                            )
                                    )

                                    .subtract(
                                            avalanche.direction
                                                    .scale(
                                                            back
                                                    )
                                    );

                    int x =
                            Mth.floor(
                                    particlePosition.x
                            );

                    int z =
                            Mth.floor(
                                    particlePosition.z
                            );

                    int ground =
                            level.getHeight(
                                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                    x,
                                    z
                            );

                    double y =
                            ground
                            +
                            2.0
                            +
                            random.nextDouble()
                            *
                            14.0;

                    /*
                     * NUVEM GIGANTE.
                     */
                    level.sendParticles(
                            player,

                            WayAroundParticles
                                    .BLIZZARD_CLOUD
                                    .get(),

                            true,

                            particlePosition.x,
                            y,
                            particlePosition.z,

                            2,

                            4.0,
                            4.0,
                            4.0,

                            0.03
                    );

                    /*
                     * Neve violenta dentro
                     * da parede.
                     */
                    level.sendParticles(
                            player,

                            ParticleTypes
                                    .SNOWFLAKE,

                            true,

                            particlePosition.x,
                            y,
                            particlePosition.z,

                            10,

                            4.5,
                            3.0,
                            4.5,

                            0.35
                    );
                }
            }
        }
    }

    private static Vec3 findDownhill(
            ServerLevel level,
            int x,
            int z
    ) {

        int sample =
                14;

        int east =
                level.getHeight(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        x + sample,
                        z
                );

        int west =
                level.getHeight(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        x - sample,
                        z
                );

        int south =
                level.getHeight(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        x,
                        z + sample
                );

        int north =
                level.getHeight(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        x,
                        z - sample
                );

        Vec3 direction =
                new Vec3(
                        west - east,
                        0.0,
                        north - south
                );

        if (
                direction.lengthSqr()
                <
                4.0
        ) {
            return null;
        }

        return direction.normalize();
    }

    private static double slopeStrength(
            ServerLevel level,
            int x,
            int z
    ) {

        int sample =
                12;

        int east =
                level.getHeight(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        x + sample,
                        z
                );

        int west =
                level.getHeight(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        x - sample,
                        z
                );

        int north =
                level.getHeight(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        x,
                        z - sample
                );

        int south =
                level.getHeight(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        x,
                        z + sample
                );

        double dx =
                east - west;

        double dz =
                south - north;

        return Math.sqrt(
                dx * dx
                +
                dz * dz
        );
    }
}