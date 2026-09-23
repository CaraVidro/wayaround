package net.caravidro.wayaround.worldgen.water;

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

        for (Direction direction :
                Direction.values()) {

            BlockPos neighbor =
                    center.relative(
                            direction
                    );

            if (!level.getFluidState(neighbor)
                    .is(FluidTags.WATER)) {
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

    public static float turbulence(
            Level level,
            BlockPos pos
    ) {
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

        for (Direction direction :
                Direction.Plane.HORIZONTAL) {

            BlockPos neighbor =
                    pos.relative(
                            direction
                    );

            if (!level.getFluidState(neighbor)
                    .is(FluidTags.WATER)) {
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
                + current.z * current.z
        );
    }
}
