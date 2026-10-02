package net.caravidro.wayaround.worldgen.water;

import net.caravidro.wayaround.performance.PerformanceProfiler;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

/**
 * Shared cheap water-current approximation.
 *
 * Vanilla flowing water remains the dominant component. Source/still water
 * receives a small environmental drift so lakes and wide rivers are not
 * mathematically dead. The hydropower and particle systems use the same
 * current function.
 */
public final class WaterDynamics {

    private static final Direction[] ALL_DIRECTIONS = {
            Direction.DOWN,
            Direction.UP,
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST
    };

    private static final Direction[] HORIZONTAL_DIRECTIONS = {
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST
    };

    private static final int[][] MECHANICAL_SAMPLE_OFFSETS = {
            {0, 0, 0},
            {0, 0, -1},
            {0, 0, 1},
            {1, 0, 0},
            {-1, 0, 0},
            {0, 1, 0},
            {0, -1, 0}
    };

    private WaterDynamics() {
    }

    public static Vec3 current(
            Level level,
            BlockPos pos
    ) {
        FluidState state =
                level.getFluidState(pos);

        if (!state.is(FluidTags.WATER)) {
            return Vec3.ZERO;
        }

        if (!WorldFeatureRuntime.enabled(
                level,
                WorldFeature.WATER_DYNAMICS
        )) {
            return state.getFlow(
                    level,
                    pos
            );
        }

        Vec3 vanilla =
                state.getFlow(
                        level,
                        pos
                );

        double time =
                level.getGameTime()
                * 0.0022;

        double angle =
                pos.getX() * 0.021
                - pos.getZ() * 0.017
                + Math.sin(
                        pos.getX() * 0.008
                        + pos.getZ() * 0.011
                ) * 1.8
                + time;

        double ambient =
                state.isSource()
                        ? 0.040
                        : 0.022;

        Vec3 drift =
                new Vec3(
                        Math.cos(angle) * ambient,
                        0.0,
                        Math.sin(angle) * ambient
                );

        Vec3 result =
                vanilla.scale(0.90)
                        .add(drift);

        double horizontal =
                Math.sqrt(
                        result.x * result.x
                        + result.z * result.z
                );

        if (horizontal > 1.0) {
            result =
                    new Vec3(
                            result.x / horizontal,
                            result.y,
                            result.z / horizontal
                    );
        }

        return result;
    }

    public static Vec3 currentAround(
            Level level,
            BlockPos center
    ) {
        Vec3 total =
                current(
                        level,
                        center
                );

        int samples =
                level.getFluidState(center)
                        .is(FluidTags.WATER)
                        ? 1
                        : 0;

        BlockPos.MutableBlockPos neighbor =
                new BlockPos.MutableBlockPos();

        for (Direction direction :
                ALL_DIRECTIONS) {

            neighbor.set(
                    center.getX()
                            + direction.getStepX(),
                    center.getY()
                            + direction.getStepY(),
                    center.getZ()
                            + direction.getStepZ()
            );

            if (!level.getFluidState(
                    neighbor
            ).is(
                    FluidTags.WATER
            )) {
                continue;
            }

            total =
                    total.add(
                            current(
                                    level,
                                    neighbor
                            )
                    );

            samples++;
        }

        return samples == 0
                ? Vec3.ZERO
                : total.scale(
                        1.0 / samples
                );
    }

    public static MechanicalFlow mechanicalFlow(
            Level level,
            BlockPos center
    ) {
        long wayperfStartedAt =
                PerformanceProfiler.begin(
                        PerformanceProfiler.Section.WATER_FLOW
                );

        try {
        if (!WorldFeatureRuntime.enabled(
                level,
                WorldFeature.WATER_DYNAMICS
        )) {
            return new MechanicalFlow(
                    Vec3.ZERO,
                    0.0F,
                    false,
                    0
            );
        }

        Vec3 sum =
                Vec3.ZERO;

        double sumMagnitude =
                0.0;

        int waterSamples =
                0;

        int movingSamples =
                0;

        BlockPos.MutableBlockPos sample =
                new BlockPos.MutableBlockPos();

        for (int[] offset :
                MECHANICAL_SAMPLE_OFFSETS) {

            sample.set(
                    center.getX()
                            + offset[0],
                    center.getY()
                            + offset[1],
                    center.getZ()
                            + offset[2]
            );

            FluidState state =
                    level.getFluidState(
                            sample
                    );

            if (!state.is(FluidTags.WATER)) {
                continue;
            }

            waterSamples++;

            Vec3 raw =
                    state.getFlow(
                            level,
                            sample
                    );

            double magnitude =
                    raw.length();

            if (magnitude < 0.008) {
                continue;
            }

            movingSamples++;

            sum =
                    sum.add(raw);

            sumMagnitude +=
                    magnitude;
        }

        if (movingSamples > 0
                && sumMagnitude > 0.0) {

            double resultMagnitude =
                    sum.length();

            float coherence =
                    (float) Math.max(
                            0.0,
                            Math.min(
                                    1.0,
                                    resultMagnitude
                                    / sumMagnitude
                            )
                    );

            double averageSpeed =
                    sumMagnitude
                    / movingSamples;

            if (coherence >= 0.52F
                    && averageSpeed >= 0.025) {

                Vec3 direction =
                        resultMagnitude > 0.0001
                                ? sum.scale(
                                        1.0 / resultMagnitude
                                )
                                : Vec3.ZERO;

                return new MechanicalFlow(
                        direction.scale(
                                averageSpeed
                        ),
                        coherence,
                        true,
                        waterSamples
                );
            }
        }

        /*
         * Natural Minecraft rivers are often made of source blocks and have
         * no vanilla flow vector at all. In that case, infer a directed
         * channel only when the surrounding water geometry is clearly longer
         * along one horizontal axis than the other. Open lakes therefore do
         * not become free generators.
         */
        int east =
                waterRun(
                        level,
                        center,
                        Direction.EAST,
                        5
                );

        int west =
                waterRun(
                        level,
                        center,
                        Direction.WEST,
                        5
                );

        int north =
                waterRun(
                        level,
                        center,
                        Direction.NORTH,
                        5
                );

        int south =
                waterRun(
                        level,
                        center,
                        Direction.SOUTH,
                        5
                );

        int eastWest =
                east + west;

        int northSouth =
                north + south;

        int longest =
                Math.max(
                        eastWest,
                        northSouth
                );

        int shortest =
                Math.min(
                        eastWest,
                        northSouth
                );

        if (longest >= 4) {
            float anisotropy =
                    (longest - shortest)
                    / (float) Math.max(
                            1,
                            longest
                    );

            if (anisotropy >= 0.30F) {
                Vec3 ambient =
                        current(
                                level,
                                center
                        );

                boolean xAxis =
                        eastWest > northSouth;

                double sign;

                if (xAxis) {
                    sign =
                            Math.abs(ambient.x) > 0.001
                                    ? Math.signum(ambient.x)
                                    : 1.0;
                } else {
                    sign =
                            Math.abs(ambient.z) > 0.001
                                    ? Math.signum(ambient.z)
                                    : 1.0;
                }

                double inferredSpeed =
                        0.075
                        + anisotropy
                        * 0.105;

                Vec3 inferred =
                        xAxis
                                ? new Vec3(
                                        sign * inferredSpeed,
                                        0.0,
                                        0.0
                                )
                                : new Vec3(
                                        0.0,
                                        0.0,
                                        sign * inferredSpeed
                                );

                return new MechanicalFlow(
                        inferred,
                        anisotropy,
                        true,
                        waterSamples
                );
            }
        }

        return new MechanicalFlow(
                Vec3.ZERO,
                0.0F,
                false,
                waterSamples
        );
    
        } finally {
            PerformanceProfiler.end(
                    PerformanceProfiler.Section.WATER_FLOW,
                    wayperfStartedAt
            );
        }
    }

    private static int waterRun(
            Level level,
            BlockPos center,
            Direction direction,
            int maxDistance
    ) {
        int count =
                0;

        BlockPos.MutableBlockPos pos =
                new BlockPos.MutableBlockPos();

        for (int distance = 1;
             distance <= maxDistance;
             distance++) {

            pos.set(
                    center.getX()
                            + direction.getStepX()
                                    * distance,
                    center.getY()
                            + direction.getStepY()
                                    * distance,
                    center.getZ()
                            + direction.getStepZ()
                                    * distance
            );

            if (!level.getFluidState(
                    pos
            ).is(
                    FluidTags.WATER
            )) {
                break;
            }

            count++;
        }

        return count;
    }

    public static float turbulence(
            Level level,
            BlockPos pos
    ) {
        if (!WorldFeatureRuntime.enabled(
                level,
                WorldFeature.WATER_DYNAMICS
        )) {
            return 0.0F;
        }

        if (!level.getFluidState(pos).is(FluidTags.WATER)) {
            return 0.0F;
        }

        Vec3 local =
                current(
                        level,
                        pos
                );

        double localSpeed =
                speed(
                        local
                );

        if (localSpeed < 0.015) {
            return 0.0F;
        }

        float impact =
                hitsObstacle(
                        level,
                        pos,
                        local
                )
                        ? 1.0F
                        : 0.0F;

        double bend =
                0.0;

        BlockPos.MutableBlockPos neighbor =
                new BlockPos.MutableBlockPos();

        for (Direction direction :
                HORIZONTAL_DIRECTIONS) {

            neighbor.set(
                    pos.getX()
                            + direction.getStepX(),
                    pos.getY(),
                    pos.getZ()
                            + direction.getStepZ()
            );

            if (!level.getFluidState(
                    neighbor
            ).is(
                    FluidTags.WATER
            )) {
                continue;
            }

            Vec3 other =
                    current(
                            level,
                            neighbor
                    );

            double otherSpeed =
                    speed(
                            other
                    );

            if (otherSpeed < 0.015) {
                continue;
            }

            double dot =
                    (
                            local.x * other.x
                            + local.z * other.z
                    )
                    / (
                            localSpeed
                            * otherSpeed
                    );

            dot =
                    Math.max(
                            -1.0,
                            Math.min(
                                    1.0,
                                    dot
                            )
                    );

            bend =
                    Math.max(
                            bend,
                            1.0 - dot
                    );
        }

        return (float) Math.min(
                1.0,
                localSpeed * 1.25
                + impact * 0.52
                + bend * 0.48
        );
    }

    public static boolean hitsObstacle(
            Level level,
            BlockPos waterPos,
            Vec3 current
    ) {
        Direction direction =
                dominantDirection(
                        current
                );

        if (direction == null) {
            return false;
        }

        BlockPos ahead =
                waterPos.relative(
                        direction
                );

        if (level.getFluidState(ahead)
                .is(FluidTags.WATER)) {
            return false;
        }

        return !level.getBlockState(ahead)
                .getCollisionShape(
                        level,
                        ahead
                )
                .isEmpty();
    }

    public static Direction dominantDirection(
            Vec3 current
    ) {
        if (current.x * current.x
                + current.z * current.z
                < 0.0004) {
            return null;
        }

        if (Math.abs(current.x)
                > Math.abs(current.z)) {
            return current.x > 0.0
                    ? Direction.EAST
                    : Direction.WEST;
        }

        return current.z > 0.0
                ? Direction.SOUTH
                : Direction.NORTH;
    }

    public static double speed(
            Vec3 current
    ) {
        return Math.sqrt(
                current.x * current.x
                + current.y * current.y
                + current.z * current.z
        );
    }

    public record MechanicalFlow(
            Vec3 vector,
            float coherence,
            boolean stable,
            int waterSamples
    ) {
    }
}
