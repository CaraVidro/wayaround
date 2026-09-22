package net.caravidro.wayaround.worldgen.weather.avalanche;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AvalancheManager {

    private static final Map<
            ResourceKey<Level>,
            List<Avalanche>
    > ACTIVE = new HashMap<>();

    private AvalancheManager() {
    }

    public static Avalanche start(
            ServerLevel level,
            Vec3 origin,
            Vec3 direction,
            double width,
            double depth,
            double speed,
            double maxDistance
    ) {

        Vec3 horizontal =
                new Vec3(
                        direction.x,
                        0.0,
                        direction.z
                );

        if (horizontal.lengthSqr() < 0.001) {
            horizontal =
                    new Vec3(
                            0.0,
                            0.0,
                            1.0
                    );
        }

        horizontal =
                horizontal.normalize();

        int surfaceY =
                level.getHeight(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        (int) Math.floor(origin.x),
                        (int) Math.floor(origin.z)
                );

        Avalanche avalanche =
                new Avalanche(
                        UUID.randomUUID(),
                        level.dimension(),

                        new Vec3(
                                origin.x,
                                surfaceY + 2.0,
                                origin.z
                        ),

                        horizontal,

                        width,
                        depth,
                        speed,
                        maxDistance,

                        level.getGameTime()
                );

        ACTIVE
                .computeIfAbsent(
                        level.dimension(),
                        ignored -> new ArrayList<>()
                )
                .add(avalanche);

        return avalanche;
    }

    public static void tick(
            ServerLevel level
    ) {

        List<Avalanche> avalanches =
                ACTIVE.get(
                        level.dimension()
                );

        if (
                avalanches == null
                ||
                avalanches.isEmpty()
        ) {
            return;
        }

        Iterator<Avalanche> iterator =
                avalanches.iterator();

        while (
                iterator.hasNext()
        ) {

            Avalanche avalanche =
                    iterator.next();

            avalanche.tick(
                    level
            );

            if (
                    avalanche.finished
            ) {
                iterator.remove();
            }
        }

        if (
                avalanches.isEmpty()
        ) {
            ACTIVE.remove(
                    level.dimension()
            );
        }
    }

    public static List<Avalanche> get(
            ServerLevel level
    ) {

        List<Avalanche> avalanches =
                ACTIVE.get(
                        level.dimension()
                );

        if (
                avalanches == null
        ) {
            return List.of();
        }

        return List.copyOf(
                avalanches
        );
    }

    public static int activeCount(
            ServerLevel level
    ) {

        List<Avalanche> avalanches =
                ACTIVE.get(
                        level.dimension()
                );

        return avalanches == null
                ? 0
                : avalanches.size();
    }

    public static void clear(
            ServerLevel level
    ) {

        ACTIVE.remove(
                level.dimension()
        );
    }

    public static void clearAll() {

        ACTIVE.clear();
    }

    public static final class Avalanche {

        public final UUID id;

        public final ResourceKey<Level> dimension;

        public final long startTick;

        public Vec3 center;

        public Vec3 direction;

        public final double width;

        public final double depth;

        public double speed;

        public final double maxDistance;

        public double travelled;

        public boolean finished;

        private Avalanche(
                UUID id,
                ResourceKey<Level> dimension,
                Vec3 center,
                Vec3 direction,
                double width,
                double depth,
                double speed,
                double maxDistance,
                long startTick
        ) {

            this.id = id;

            this.dimension = dimension;

            this.center = center;

            this.direction =
                    direction.normalize();

            this.width = width;

            this.depth = depth;

            this.speed = speed;

            this.maxDistance =
                    maxDistance;

            this.startTick =
                    startTick;

            this.travelled = 0.0;

            this.finished = false;
        }

        private void tick(
                ServerLevel level
        ) {

            if (
                    finished
            ) {
                return;
            }

            /*
             * Descobre direção de descida.
             */
            Vec3 downhill =
                    getDownhillDirection(
                            level,
                            center
                    );

            if (
                    downhill != null
            ) {

                /*
                 * A avalanche não vira
                 * instantaneamente.
                 *
                 * Ela "escorre" na direção
                 * mais baixa.
                 */
                direction =
                        direction
                                .scale(0.94)
                                .add(
                                        downhill.scale(
                                                0.06
                                        )
                                );

                if (
                        direction.lengthSqr()
                        > 0.001
                ) {

                    direction =
                            direction.normalize();
                }
            }

            double slope =
                    getSlopeStrength(
                            level,
                            center
                    );

            /*
             * Quanto maior a inclinação,
             * mais rápida.
             */
            double targetSpeed =
                    0.42
                    +
                    Mth.clamp(
                            slope / 18.0,
                            0.0,
                            1.0
                    )
                    * 0.90;

            /*
             * Mantém parte da velocidade
             * inicial.
             */
            targetSpeed =
                    Math.max(
                            targetSpeed,
                            speed * 0.82
                    );

            speed +=
                    (
                            targetSpeed
                            -
                            speed
                    )
                    * 0.035;

            Vec3 movement =
                    direction.scale(
                            speed
                    );

            center =
                    center.add(
                            movement
                    );

            travelled +=
                    speed;

            /*
             * Cola a parede no terreno.
             */
            int groundY =
                    level.getHeight(
                            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,

                            (int) Math.floor(
                                    center.x
                            ),

                            (int) Math.floor(
                                    center.z
                            )
                    );

            center =
                    new Vec3(
                            center.x,
                            groundY + 2.0,
                            center.z
                    );

            long age =
                    level.getGameTime()
                    -
                    startTick;

            if (
                    travelled >= maxDistance
                    ||
                    age > 20L * 80L
            ) {

                finished = true;
            }
        }

        public Vec3 sideDirection() {

            return new Vec3(
                    -direction.z,
                    0.0,
                    direction.x
            ).normalize();
        }

        private static Vec3 getDownhillDirection(
                ServerLevel level,
                Vec3 position
        ) {

            int x =
                    (int) Math.floor(
                            position.x
                    );

            int z =
                    (int) Math.floor(
                            position.z
                    );

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

            double dx =
                    west - east;

            double dz =
                    north - south;

            Vec3 downhill =
                    new Vec3(
                            dx,
                            0.0,
                            dz
                    );

            if (
                    downhill.lengthSqr()
                    < 1.0
            ) {
                return null;
            }

            return downhill.normalize();
        }

        private static double getSlopeStrength(
                ServerLevel level,
                Vec3 position
        ) {

            int x =
                    (int) Math.floor(
                            position.x
                    );

            int z =
                    (int) Math.floor(
                            position.z
                    );

            int sample =
                    10;

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
}