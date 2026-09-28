package net.caravidro.wayaround.worldgen.weather.fire;

import java.util.HashSet;
import java.util.Set;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Purely visual fire polish.
 *
 * Vanilla fire remains authoritative for burning/spread. This layer breaks the
 * single centered flame silhouette into several sub-block flame fronts. Nearby
 * fire blocks contribute to one larger visual core and produce a tall smoke
 * marker that remains useful at a distance.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class EnhancedFireVisuals {

    private static final int RADIUS =
            11;

    private EnhancedFireVisuals() {
    }

    @SubscribeEvent
    public static void tick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        if (Math.floorMod(
                server.getTickCount(),
                4
        ) != 0) {
            return;
        }

        Set<GlobalPos> visited =
                new HashSet<>();

        for (ServerPlayer player :
                server.getPlayerList()
                        .getPlayers()) {

            ServerLevel level =
                    player.serverLevel();

            BlockPos center =
                    player.blockPosition();

            for (int y = -4;
                 y <= 5;
                 y++) {
                for (int x = -RADIUS;
                     x <= RADIUS;
                     x++) {
                    for (int z = -RADIUS;
                         z <= RADIUS;
                         z++) {

                        if (x * x
                                + z * z
                                > RADIUS
                                        * RADIUS) {
                            continue;
                        }

                        BlockPos pos =
                                center.offset(
                                        x,
                                        y,
                                        z
                                );

                        BlockState state =
                                level.getBlockState(
                                        pos
                                );

                        if (!(state.getBlock()
                                instanceof BaseFireBlock)) {
                            continue;
                        }

                        GlobalPos key =
                                GlobalPos.of(
                                        level.dimension(),
                                        pos
                                );

                        if (!visited.add(
                                key
                        )) {
                            continue;
                        }

                        renderFire(
                                level,
                                player,
                                pos
                        );
                    }
                }
            }
        }
    }

    private static void renderFire(
            ServerLevel level,
            ServerPlayer viewer,
            BlockPos pos
    ) {
        int neighbors =
                adjacentFireCount(
                        level,
                        pos
                );

        int cluster =
                1
                        + neighbors;

        int fronts =
                Math.min(
                        7,
                        2
                                + cluster
                );

        /*
         * Separate flame fronts inside the same block make fire feel like it
         * occupies the surface instead of coming from one exact center point.
         */
        for (int i = 0;
             i < fronts;
             i++) {

            double x =
                    pos.getX()
                            + 0.12
                            + level.random.nextDouble()
                                    * 0.76;

            double y =
                    pos.getY()
                            + 0.08
                            + level.random.nextDouble()
                                    * (
                                    cluster >= 3
                                            ? 0.62
                                            : 0.42
                            );

            double z =
                    pos.getZ()
                            + 0.12
                            + level.random.nextDouble()
                                    * 0.76;

            level.sendParticles(
                    cluster >= 3
                            && i % 3 == 0
                            ? ParticleTypes.FLAME
                            : ParticleTypes.SMALL_FLAME,
                    x,
                    y,
                    z,
                    1,
                    0.035,
                    0.025,
                    0.035,
                    0.006
            );
        }

        if (cluster >= 2
                && primaryInCluster(
                level,
                pos
        )) {

            /*
             * Adjacent fires visually fuse at their shared region. The core is
             * higher/wider instead of just drawing two identical little fires.
             */
            level.sendParticles(
                    ParticleTypes.FLAME,
                    pos.getX()
                            + 0.5,
                    pos.getY()
                            + 0.34,
                    pos.getZ()
                            + 0.5,
                    Math.min(
                            8,
                            2
                                    + cluster
                    ),
                    0.30
                            + cluster
                                    * 0.035,
                    0.22
                            + cluster
                                    * 0.025,
                    0.30
                            + cluster
                                    * 0.035,
                    0.012
            );

            level.sendParticles(
                    ParticleTypes.LARGE_SMOKE,
                    pos.getX()
                            + 0.5,
                    pos.getY()
                            + 0.85,
                    pos.getZ()
                            + 0.5,
                    Math.min(
                            8,
                            cluster
                                    + 1
                    ),
                    0.34,
                    0.26,
                    0.34,
                    0.024
            );
        } else {
            level.sendParticles(
                    ParticleTypes.SMOKE,
                    pos.getX()
                            + 0.5,
                    pos.getY()
                            + 0.68,
                    pos.getZ()
                            + 0.5,
                    1,
                    0.22,
                    0.16,
                    0.22,
                    0.015
            );
        }

        /*
         * One force-visible signal-smoke particle every few passes creates a
         * thin vertical line above larger fires. Up close the ordinary smoke
         * remains much denser than this distant marker.
         */
        if (primaryInCluster(
                level,
                pos
        )) {
            int smokePeriod =
                    cluster >= 2
                            ? 16
                            : 28;

            if (Math.floorMod(
                    level.getGameTime()
                            + pos.asLong(),
                    smokePeriod
            ) < 4) {
                level.sendParticles(
                        viewer,
                        ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                        true,
                        pos.getX()
                                + 0.5,
                        pos.getY()
                                + (
                                cluster >= 2
                                        ? 1.35
                                        : 1.05
                        ),
                        pos.getZ()
                                + 0.5,
                        1,
                        cluster >= 2
                                ? 0.08
                                : 0.035,
                        0.05,
                        cluster >= 2
                                ? 0.08
                                : 0.035,
                        0.004
                );
            }
        }
    }

    private static int adjacentFireCount(
            ServerLevel level,
            BlockPos pos
    ) {
        int count =
                0;

        for (int x = -1;
             x <= 1;
             x++) {
            for (int y = -1;
                 y <= 1;
                 y++) {
                for (int z = -1;
                     z <= 1;
                     z++) {

                    if (x == 0
                            && y == 0
                            && z == 0) {
                        continue;
                    }

                    BlockState state =
                            level.getBlockState(
                                    pos.offset(
                                            x,
                                            y,
                                            z
                                    )
                            );

                    if (state.getBlock()
                            instanceof BaseFireBlock) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    private static boolean primaryInCluster(
            ServerLevel level,
            BlockPos pos
    ) {
        long own =
                pos.asLong();

        for (int x = -1;
             x <= 1;
             x++) {
            for (int y = -1;
                 y <= 1;
                 y++) {
                for (int z = -1;
                     z <= 1;
                     z++) {
                    if (x == 0
                            && y == 0
                            && z == 0) {
                        continue;
                    }

                    BlockPos neighbor =
                            pos.offset(
                                    x,
                                    y,
                                    z
                            );

                    if (level.getBlockState(
                            neighbor
                    ).getBlock()
                            instanceof BaseFireBlock
                            && neighbor.asLong()
                            < own) {
                        return false;
                    }
                }
            }
        }

        return true;
    }
}
