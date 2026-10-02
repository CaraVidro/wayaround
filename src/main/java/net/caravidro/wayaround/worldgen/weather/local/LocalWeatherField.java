package net.caravidro.wayaround.worldgen.weather.local;

import net.caravidro.wayaround.performance.PerformanceProfiler;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.Mth;

/**
 * Lightweight deterministic weather field shared by client and server.
 *
 * It is intentionally stylized rather than meteorologically realistic:
 * large cloud cells drift across the world and carry their own rain.
 */
public final class LocalWeatherField {

    private static final double CELL_SPACING = 520.0;
    private static final double DRIFT_SPEED = 0.024;
    private static final double MAX_RADIUS = 278.0;

    private LocalWeatherField() {
    }

    public record Sample(
            float cloud,
            float rain,
            float warning,
            float windX,
            float windZ
    ) {
    }

    public record CloudCell(
            long id,
            double x,
            double z,
            double y,
            double radius,
            float storm
    ) {
        public float densityAt(double px, double pz) {
            double dx = px - x;
            double dz = pz - z;
            double distance = Math.sqrt(dx * dx + dz * dz);
            double value = 1.0 - distance / radius;
            return smooth((float) Mth.clamp(value, 0.0, 1.0));
        }

        public float rainAt(double px, double pz) {
            float density = densityAt(px, pz);
            float raininess = Mth.clamp((storm - 0.56F) / 0.44F, 0.0F, 1.0F);
            return density * raininess;
        }
    }

    public static Sample sample(
            double x,
            double z,
            long gameTime
    ) {
        long wayperfStartedAt =
                PerformanceProfiler.begin(
                        PerformanceProfiler.Section.LOCAL_WEATHER
                );

        try {
        /*
         * Hot-path sampling used to call nearbyCells(), allocate a list, then
         * calculate density twice (densityAt + rainAt) and distance again for
         * storm warning. Water/cloud rendering asks for this every frame.
         *
         * Sample directly from the deterministic grid and reuse one sqrt per
         * candidate cell.
         */
        float windX =
                windX(
                        gameTime
                );

        float windZ =
                windZ(
                        gameTime
                );

        double drift =
                gameTime
                        * DRIFT_SPEED;

        double staticX =
                x
                        - windX
                                * drift;

        double staticZ =
                z
                        - windZ
                                * drift;

        int centerX =
                floorCell(
                        staticX
                );

        int centerZ =
                floorCell(
                        staticZ
                );

        double range =
                MAX_RADIUS
                        + 260.0;

        int reach =
                Math.max(
                        2,
                        (int) Math.ceil(
                                (
                                        range
                                                + MAX_RADIUS
                                )
                                        / CELL_SPACING
                        )
                                + 1
                );

        float cloud =
                0.0F;

        float rain =
                0.0F;

        float warning =
                0.0F;

        for (int gx = centerX - reach;
             gx <= centerX + reach;
             gx++) {

            for (int gz = centerZ - reach;
                 gz <= centerZ + reach;
                 gz++) {

                CloudCell cell =
                        cell(
                                gx,
                                gz,
                                gameTime,
                                windX,
                                windZ,
                                drift
                        );

                double dx =
                        x
                                - cell.x;

                double dz =
                        z
                                - cell.z;

                double distanceSquared =
                        dx * dx
                                + dz * dz;

                double maxDistance =
                        range
                                + cell.radius;

                if (distanceSquared
                        > maxDistance
                                * maxDistance) {
                    continue;
                }

                double distance =
                        Math.sqrt(
                                distanceSquared
                        );

                float density =
                        smooth(
                                (float) Mth.clamp(
                                        1.0
                                                - distance
                                                        / cell.radius,
                                        0.0,
                                        1.0
                                )
                        );

                float raininess =
                        Mth.clamp(
                                (
                                        cell.storm
                                                - 0.56F
                                )
                                        / 0.44F,
                                0.0F,
                                1.0F
                        );

                float localRain =
                        density
                                * raininess;

                cloud =
                        Math.max(
                                cloud,
                                density
                        );

                rain =
                        Math.max(
                                rain,
                                localRain
                        );

                double warningRadius =
                        cell.radius
                                + 235.0;

                if (distance < warningRadius
                        && cell.storm > 0.48F) {

                    float proximity =
                            (float) (
                                    1.0
                                            - distance
                                                    / warningRadius
                            );

                    float stormWeight =
                            Mth.clamp(
                                    (
                                            cell.storm
                                                    - 0.48F
                                    )
                                            / 0.52F,
                                    0.0F,
                                    1.0F
                            );

                    double approach =
                            -(
                                    dx * windX
                                            + dz * windZ
                            );

                    float approachWeight =
                            approach
                                    > -cell.radius
                                            * 0.25
                                    ? 1.0F
                                    : 0.45F;

                    warning =
                            Math.max(
                                    warning,
                                    smooth(
                                            proximity
                                    )
                                            * stormWeight
                                            * approachWeight
                            );
                }
            }
        }

        warning =
                Math.max(
                        warning,
                        rain
                );

        warning =
                Math.max(
                        warning,
                        WindTestManager.strengthAt(
                                x,
                                z,
                                gameTime
                        )
                );

        return new Sample(
                Mth.clamp(
                        cloud,
                        0.0F,
                        1.0F
                ),
                Mth.clamp(
                        rain,
                        0.0F,
                        1.0F
                ),
                Mth.clamp(
                        warning,
                        0.0F,
                        1.0F
                ),
                windX,
                windZ
        );
    
        } finally {
            PerformanceProfiler.end(
                    PerformanceProfiler.Section.LOCAL_WEATHER,
                    wayperfStartedAt
            );
        }
    }

    public static List<CloudCell> nearbyCells(
            double x,
            double z,
            long gameTime,
            double range
    ) {
        float wx = windX(gameTime);
        float wz = windZ(gameTime);
        double drift = gameTime * DRIFT_SPEED;

        double staticX = x - wx * drift;
        double staticZ = z - wz * drift;

        int centerX = floorCell(staticX);
        int centerZ = floorCell(staticZ);

        int reach = Math.max(
                2,
                (int) Math.ceil((range + MAX_RADIUS) / CELL_SPACING) + 1
        );

        List<CloudCell> result = new ArrayList<>();

        for (int gx = centerX - reach; gx <= centerX + reach; gx++) {
            for (int gz = centerZ - reach; gz <= centerZ + reach; gz++) {
                CloudCell cell = cell(gx, gz, gameTime, wx, wz, drift);

                double dx = x - cell.x;
                double dz = z - cell.z;
                double maxDistance = range + cell.radius;

                if (dx * dx + dz * dz <= maxDistance * maxDistance) {
                    result.add(cell);
                }
            }
        }

        return result;
    }

    public static float windX(long gameTime) {
        return (float) Math.cos(windAngle(gameTime));
    }

    public static float windZ(long gameTime) {
        return (float) Math.sin(windAngle(gameTime));
    }

    private static double windAngle(long gameTime) {
        double slow = gameTime / 36000.0;
        return 0.72
                + Math.sin(slow) * 0.34
                + Math.sin(slow * 0.37 + 1.8) * 0.16;
    }

    private static CloudCell cell(
            int gx,
            int gz,
            long gameTime,
            float windX,
            float windZ,
            double drift
    ) {
        long seed = hash(gx, gz);

        double jitterX = signed01(seed ^ 0x6A09E667F3BCC909L) * 92.0;
        double jitterZ = signed01(seed ^ 0xBB67AE8584CAA73BL) * 92.0;

        double sizeRoll =
                unit01(
                        seed
                        ^ 0x3C6EF372FE94F82BL
                );

        double radius;

        if (sizeRoll < 0.20) {
            /*
             * Small isolated puffs. Some are intentionally tiny enough to
             * read as individual wandering clouds rather than weather fronts.
             */
            radius =
                    38.0
                    + sizeRoll / 0.20 * 62.0;
        } else if (sizeRoll < 0.76) {
            /*
             * Ordinary clouds now cover a much wider middle range.
             */
            radius =
                    92.0
                    + (
                            sizeRoll - 0.20
                    ) / 0.56 * 108.0;
        } else if (sizeRoll < 0.94) {
            /*
             * Large banks are common enough to be seen regularly.
             */
            radius =
                    195.0
                    + (
                            sizeRoll - 0.76
                    ) / 0.18 * 48.0;
        } else {
            /*
             * Rare enormous fronts.
             */
            radius =
                    243.0
                    + (
                            sizeRoll - 0.94
                    ) / 0.06 * 35.0;
        }

        float storm = (float) unit01(seed ^ 0xA54FF53A5F1D36F1L);

        /*
         * Keep a good amount of fair clouds. Only the wetter cells become
         * dark rain clouds.
         */
        storm = Mth.clamp((storm - 0.12F) / 0.88F, 0.0F, 1.0F);

        double heightRoll =
                unit01(
                        seed
                                ^ 0x510E527FADE682D1L
                );

        double height;

        if (heightRoll < 0.14) {
            /*
             * Low dramatic banks, occasionally close enough to hills and
             * mountains for the player to enter them.
             */
            height =
                    138.0
                            + heightRoll / 0.14
                                    * 32.0;
        } else if (heightRoll > 0.86) {
            /*
             * High thin-looking masses break the old perfectly level ceiling.
             */
            height =
                    222.0
                            + (
                            heightRoll - 0.86
                    ) / 0.14
                                    * 40.0;
        } else {
            height =
                    168.0
                            + (
                            heightRoll - 0.14
                    ) / 0.72
                                    * 58.0;
        }

        /*
         * Giant fronts receive a slight deterministic vertical offset too,
         * preventing all the largest silhouettes from sharing one horizon.
         */
        if (radius > 225.0) {
            height +=
                    signed01(
                            seed
                                    ^ 0x1F83D9ABFB41BD6BL
                    )
                            * 18.0;
        }

        /*
         * Large-front offsets must not undo the raised cloud floor. Mountains
         * may still enter a low bank, but ordinary terrain should no longer
         * feel as if the cloud ceiling is sitting directly above the player.
         */
        height =
                Math.max(
                        148.0,
                        height
                );

        return new CloudCell(
                seed,
                gx * CELL_SPACING + jitterX + windX * drift,
                gz * CELL_SPACING + jitterZ + windZ * drift,
                height,
                radius,
                storm
        );
    }

    private static int floorCell(double value) {
        return (int) Math.floor(value / CELL_SPACING);
    }

    private static float smooth(float value) {
        value = Mth.clamp(value, 0.0F, 1.0F);
        return value * value * (3.0F - 2.0F * value);
    }

    private static long hash(int x, int z) {
        long value = x * 341873128712L ^ z * 132897987541L;
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    private static double unit01(long value) {
        long bits = mix64(value) >>> 11;
        return bits * 0x1.0p-53;
    }

    private static double signed01(long value) {
        return unit01(value) * 2.0 - 1.0;
    }

    private static long mix64(long value) {
        value ^= value >>> 30;
        value *= 0xbf58476d1ce4e5b9L;
        value ^= value >>> 27;
        value *= 0x94d049bb133111ebL;
        value ^= value >>> 31;
        return value;
    }
}
