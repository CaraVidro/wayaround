package net.caravidro.wayaround.worldgen.weather;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.world.level.Level;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BlizzardManager {

    public static final class Blizzard {

        public final UUID id;

        public final ResourceKey<Level> dimension;

        /*
         * Onde nasceu.
         */
        public final Vec3 originCenter;

        /*
         * Posição ATUAL.
         *
         * AntarcticBlizzard já pode continuar usando:
         *
         * storm.center
         */
        public Vec3 center;

        /*
         * Raio inicial.
         */
        public final double initialRadius;

        /*
         * Raio atual.
         */
        public double radius;

        /*
         * Não encolhe além daqui.
         */
        public final double minimumRadius;

        /*
         * Movimento.
         */
        public final Vec3 movementDirection;

        /*
         * blocos / tick
         */
        public final double movementSpeed;

        public final long startTick;

        public final long durationTicks;

        public final double maxIntensity;

        private long actualEndTick =
                -1L;

        /*
         * Several systems ask for the same storm many times in one server
         * tick (snow accumulation, player sync, visuals). Avoid recalculating
         * center/radius and allocating a fresh Vec3 for identical gameTime.
         */
        private long lastUpdateTick =
                Long.MIN_VALUE;

        private Blizzard(
                ResourceKey<Level> dimension,
                Vec3 center,
                double radius,
                double maxIntensity,
                long startTick,
                long durationTicks
        ) {

            this.id =
                    UUID.randomUUID();

            this.dimension =
                    dimension;

            this.originCenter =
                    center;

            this.center =
                    center;

            this.initialRadius =
                    radius;

            this.radius =
                    radius;

            /*
             * Encolhe aproximadamente 22%.
             *
             * 500 -> 390
             *
             * Ainda continua sendo uma
             * tempestade ENORME.
             */
            this.minimumRadius =
                    Math.max(
                            24.0,
                            radius * 0.78
                    );

            this.startTick =
                    startTick;

            this.durationTicks =
                    durationTicks;

            this.maxIntensity =
                    maxIntensity;

            /*
             * Direção pseudoaleatória,
             * mas determinística para esta storm.
             */
            double directionNoise =
                    fract(
                            Math.sin(
                                    center.x * 0.017
                                            +
                                            center.z * 0.031
                                            +
                                            startTick * 0.00071
                            )
                                    *
                                    43758.5453123
                    );

            double angle =
                    directionNoise
                            *
                            Math.PI
                            *
                            2.0;

            this.movementDirection =
                    new Vec3(
                            Math.cos(angle),
                            0.0,
                            Math.sin(angle)
                    ).normalize();

            /*
             * Velocidade também varia.
             *
             * ~0.035 - 0.070 bloco/tick
             *
             * =
             *
             * ~0.7 - 1.4 bloco/segundo.
             */
            double speedNoise =
                    fract(
                            Math.sin(
                                    center.x * 0.041
                                            -
                                            center.z * 0.023
                                            +
                                            startTick * 0.0013
                            )
                                    *
                                    24634.6345
                    );

            this.movementSpeed =
                    0.035
                            +
                            speedNoise
                                    *
                                    0.035;
        }

        /*
         * =====================================================
         * UPDATE
         * =====================================================
         */

        public void update(
                long gameTime
        ) {
            if (lastUpdateTick
                    == gameTime) {
                return;
            }

            lastUpdateTick =
                    gameTime;

            long elapsed =
                    Math.max(
                            0L,
                            Math.min(
                                    durationTicks,
                                    gameTime
                                            -
                                            startTick
                            )
                    );

            /*
             * Movimento desde a origem.
             */
            this.center =
                    centerAtTick(
                            startTick
                                    +
                                    elapsed
                    );

            double life =
                    elapsed
                            /
                            (double) durationTicks;

            /*
             * Não começa encolhendo no
             * exato instante em que nasce.
             */
            double shrinkProgress =
                    clamp(
                            (
                                    life
                                            -
                                            0.05
                            )
                                    /
                                    0.85,

                            0.0,
                            1.0
                    );

            shrinkProgress =
                    smoothstep(
                            shrinkProgress
                    );

            this.radius =
                    lerp(
                            initialRadius,
                            minimumRadius,
                            shrinkProgress
                    );
        }

        /*
         * Posição que a tempestade tem
         * em determinado tick.
         */
        public Vec3 centerAtTick(
                long time
        ) {

            long elapsed =
                    Math.max(
                            0L,
                            time
                                    -
                                    startTick
                    );

            elapsed =
                    Math.min(
                            elapsed,
                            durationTicks
                    );

            double distance =
                    movementSpeed
                            *
                            elapsed;

            return new Vec3(
                    originCenter.x
                            + movementDirection.x
                                    * distance,
                    originCenter.y
                            + movementDirection.y
                                    * distance,
                    originCenter.z
                            + movementDirection.z
                                    * distance
            );
        }

        /*
         * =====================================================
         * INTENSITY
         * =====================================================
         */

        public double intensityAt(
                Vec3 position,
                long time
        ) {

            update(time);

            double radial =
                    radialStrengthAt(
                            position
                    );

            if (
                    radial <= 0.0
            ) {
                return 0.0;
            }

            return clamp(
                    radial
                            *
                            globalStrength(time),

                    0.0,
                    1.0
            );
        }

        /*
         * Centro brutal,
         * borda tranquila.
         */
        public double radialStrengthAt(
                Vec3 position
        ) {

            double dx =
                    position.x
                            -
                            center.x;

            double dz =
                    position.z
                            -
                            center.z;

            double distance =
                    Math.sqrt(
                            dx * dx
                                    +
                                    dz * dz
                    );

            if (
                    distance >= radius
            ) {
                return 0.0;
            }

            double radial =
                    1.0
                            -
                            distance / radius;

            radial =
                    smoothstep(
                            radial
                    );

            /*
             * Acentua o centro.
             */
            radial =
                    Math.pow(
                            radial,
                            1.65
                    );

            return radial;
        }

        /*
         * Intensidade global da tempestade.
         *
         * Não considera posição.
         */
        public double globalStrength(
                long time
        ) {

            double life =
                    (
                            time
                                    -
                                    startTick
                    )
                            /
                            (double) durationTicks;

            /*
             * Fade IN.
             */
            double fadeIn =
                    clamp(
                            life / 0.10,
                            0.0,
                            1.0
                    );

            /*
             * Fade OUT.
             *
             * Últimos ~12%.
             */
            double fadeOut =
                    clamp(
                            (
                                    1.0
                                            -
                                            life
                            )
                                    /
                                    0.12,

                            0.0,
                            1.0
                    );

            double temporal =
                    Math.min(
                            fadeIn,
                            fadeOut
                    );

            /*
             * Rajadas.
             */
            double gust =
                    0.72

                            +

                            Math.sin(
                                    time / 22.0
                            ) * 0.11

                            +

                            Math.sin(
                                    time / 61.0
                                            +
                                            1.7
                            ) * 0.09

                            +

                            Math.sin(
                                    time / 141.0
                                            +
                                            4.2
                            ) * 0.08;

            gust =
                    clamp(
                            gust,
                            0.40,
                            1.0
                    );

            return clamp(
                    temporal
                            *
                            gust
                            *
                            maxIntensity,

                    0.0,
                    1.0
            );
        }

        /*
         * =====================================================
         * PATH EXPOSURE
         * =====================================================
         *
         * Muito importante para chunks
         * descarregados.
         *
         * Agora a storm SE MOVE.
         *
         * Então não basta perguntar:
         *
         * "o chunk estava no círculo final?"
         *
         * Precisamos perguntar:
         *
         * "a tempestade passou por cima dele?"
         */
        public double pathExposureAt(
                Vec3 position,
                long untilTick
        ) {

            long end =
                    Math.min(
                            untilTick,
                            endTick()
                    );

            end =
                    Math.max(
                            startTick,
                            end
                    );

            Vec3 start =
                    originCenter;

            Vec3 finish =
                    centerAtTick(
                            end
                    );

            double ax =
                    start.x;

            double az =
                    start.z;

            double bx =
                    finish.x;

            double bz =
                    finish.z;

            double px =
                    position.x;

            double pz =
                    position.z;

            double vx =
                    bx - ax;

            double vz =
                    bz - az;

            double lengthSquared =
                    vx * vx
                            +
                            vz * vz;

            double t;

            if (
                    lengthSquared
                            <= 0.000001
            ) {

                t = 0.0;

            } else {

                t =
                        (
                                (px - ax) * vx
                                        +
                                        (pz - az) * vz
                        )
                                /
                                lengthSquared;

                t =
                        clamp(
                                t,
                                0.0,
                                1.0
                        );
            }

            double closestX =
                    ax
                            +
                            vx * t;

            double closestZ =
                    az
                            +
                            vz * t;

            double dx =
                    px
                            -
                            closestX;

            double dz =
                    pz
                            -
                            closestZ;

            double distance =
                    Math.sqrt(
                            dx * dx
                                    +
                                    dz * dz
                    );

            /*
             * Como ela encolhe lentamente,
             * usamos raio médio para o catch-up.
             */
            double effectiveRadius =
                    (
                            initialRadius
                                    +
                                    minimumRadius
                    )
                            *
                            0.5;

            if (
                    distance >=
                            effectiveRadius
            ) {

                return 0.0;
            }

            double exposure =
                    1.0
                            -
                            distance
                                    /
                                    effectiveRadius;

            exposure =
                    smoothstep(
                            exposure
                    );

            return Math.pow(
                    exposure,
                    1.45
            );
        }

        /*
         * =====================================================
         * LIFE
         * =====================================================
         */

        public boolean isExpired(
                long time
        ) {

            return time >=
                    plannedEndTick();
        }

        public long plannedEndTick() {

            return startTick
                    +
                    durationTicks;
        }

        public long endTick() {

            if (
                    actualEndTick >= 0L
            ) {

                return actualEndTick;
            }

            return plannedEndTick();
        }

        public void stopAt(
                long tick
        ) {

            if (
                    actualEndTick < 0L
            ) {

                actualEndTick =
                        Math.max(
                                startTick,
                                tick
                        );
            }
        }

        public long elapsedTicks(
                long currentTime
        ) {

            long end =
                    actualEndTick >= 0L

                            ? actualEndTick

                            : Math.min(
                            currentTime,
                            plannedEndTick()
                    );

            return Math.max(
                    0L,
                    end
                            -
                            startTick
            );
        }

        public double distanceFromCenter(
                Vec3 position
        ) {

            double dx =
                    position.x
                            -
                            center.x;

            double dz =
                    position.z
                            -
                            center.z;

            return Math.sqrt(
                    dx * dx
                            +
                            dz * dz
            );
        }
    }

    /*
     * =========================================================
     * STORAGE
     * =========================================================
     */

    private static final Map<
            ResourceKey<Level>,
            Blizzard
            > ACTIVE =
            new HashMap<>();

    private static final Map<
            ResourceKey<Level>,
            ArrayDeque<Blizzard>
            > HISTORY =
            new HashMap<>();

    private static final int MAX_HISTORY =
            16;

    private BlizzardManager() {
    }

    /*
     * =========================================================
     * START
     * =========================================================
     */

    public static Blizzard start(
            ServerLevel level,
            Vec3 center,
            double radius,
            long durationTicks,
            double intensity
    ) {

        Blizzard previous =
                ACTIVE.remove(
                        level.dimension()
                );

        if (
                previous != null
        ) {

            previous.update(
                    level.getGameTime()
            );

            previous.stopAt(
                    level.getGameTime()
            );

            archive(
                    previous
            );
        }

        Blizzard storm =
                new Blizzard(
                        level.dimension(),
                        center,
                        radius,
                        intensity,
                        level.getGameTime(),
                        durationTicks
                );

        ACTIVE.put(
                level.dimension(),
                storm
        );

        return storm;
    }

    /*
     * =========================================================
     * STOP
     * =========================================================
     */

    public static void stop(
            ServerLevel level
    ) {

        Blizzard storm =
                ACTIVE.remove(
                        level.dimension()
                );

        if (
                storm == null
        ) {
            return;
        }

        storm.update(
                level.getGameTime()
        );

        storm.stopAt(
                level.getGameTime()
        );

        archive(
                storm
        );
    }

    /*
     * =========================================================
     * GET
     * =========================================================
     */

    public static Blizzard get(
            ServerLevel level
    ) {

        Blizzard storm =
                ACTIVE.get(
                        level.dimension()
                );

        if (
                storm == null
        ) {
            return null;
        }

        long time =
                level.getGameTime();

        storm.update(
                time
        );

        if (
                storm.isExpired(
                        time
                )
        ) {

            /*
             * Atualiza exatamente para o último
             * instante antes de arquivar.
             */
            storm.update(
                    storm.plannedEndTick()
            );

            storm.stopAt(
                    storm.plannedEndTick()
            );

            ACTIVE.remove(
                    level.dimension()
            );

            archive(
                    storm
            );

            return null;
        }

        return storm;
    }

    public static boolean hasActive(
            ServerLevel level
    ) {

        return get(level)
                != null;
    }

    public static double getIntensity(
            ServerLevel level,
            Vec3 position
    ) {

        Blizzard storm =
                get(level);

        if (
                storm == null
        ) {
            return 0.0;
        }

        return storm.intensityAt(
                position,
                level.getGameTime()
        );
    }

    /*
     * =========================================================
     * HISTORY
     * =========================================================
     */

    public static List<Blizzard> getHistory(
            ServerLevel level
    ) {

        ArrayDeque<Blizzard> history =
                HISTORY.get(
                        level.dimension()
                );

        if (
                history == null
        ) {

            return List.of();
        }

        return List.copyOf(
                history
        );
    }

    private static void archive(
            Blizzard storm
    ) {

        ArrayDeque<Blizzard> history =
                HISTORY.computeIfAbsent(

                        storm.dimension,

                        key ->
                                new ArrayDeque<>()
                );

        history.addLast(
                storm
        );

        while (
                history.size()
                        >
                        MAX_HISTORY
        ) {

            history.removeFirst();
        }
    }

    public static void clearAll() {

        ACTIVE.clear();
        HISTORY.clear();
    }

    /*
     * =========================================================
     * HELPERS
     * =========================================================
     */

    private static double smoothstep(
            double value
    ) {

        value =
                clamp(
                        value,
                        0.0,
                        1.0
                );

        return value
                *
                value
                *
                (
                        3.0
                                -
                                2.0 * value
                );
    }

    private static double lerp(
            double a,
            double b,
            double t
    ) {

        return a
                +
                (
                        b - a
                )
                        *
                        t;
    }

    private static double fract(
            double value
    ) {

        return value
                -
                Math.floor(
                        value
                );
    }

    private static double clamp(
            double value,
            double min,
            double max
    ) {

        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }
}