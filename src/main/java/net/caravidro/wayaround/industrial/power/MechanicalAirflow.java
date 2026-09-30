package net.caravidro.wayaround.industrial.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Small reusable airflow query. Mechanical Fan is the first producer; furnaces,
 * drying, smoke, cooling and compressors can consume the same contract later.
 */
public final class MechanicalAirflow {

    public record Sample(
            float flow,
            float pressure,
            Vec3 direction,
            BlockPos source
    ) {
        public static final Sample NONE =
                new Sample(
                        0.0F,
                        0.0F,
                        Vec3.ZERO,
                        BlockPos.ZERO
                );

        public boolean active() {
            return flow > 0.01F;
        }
    }

    private static final int RANGE =
            8;

    private MechanicalAirflow() {
    }

    public static Sample at(
            Level level,
            BlockPos target
    ) {
        Sample best =
                Sample.NONE;

        for (Direction fromTarget :
                Direction.values()) {

            Direction fanFacing =
                    fromTarget.getOpposite();

            for (int distance = 1;
                 distance <= RANGE;
                 distance++) {

                BlockPos candidate =
                        target.relative(
                                fromTarget,
                                distance
                        );

                if (level.getBlockEntity(
                        candidate
                ) instanceof MechanicalFanBlockEntity fan
                        && fan.facing()
                                == fanFacing) {

                    if (!clearPath(
                            level,
                            candidate,
                            target,
                            fanFacing
                    )) {
                        break;
                    }

                    float attenuation =
                            1.0F
                                    / (
                                    1.0F
                                            + (
                                            distance
                                                    - 1
                                    )
                                            * 0.14F
                            );

                    float flow =
                            fan.airflow()
                                    * attenuation;

                    float pressure =
                            fan.pressure()
                                    * (
                                    0.82F
                                            + attenuation
                                                    * 0.18F
                            );

                    if (flow > best.flow()) {
                        best =
                                new Sample(
                                        flow,
                                        pressure,
                                        Vec3.atLowerCornerOf(
                                                fanFacing.getNormal()
                                        ),
                                        candidate.immutable()
                                );
                    }

                    break;
                }

                if (!level.getBlockState(
                        candidate
                ).isAir()
                        && distance > 1) {
                    break;
                }
            }
        }

        return best;
    }

    private static boolean clearPath(
            Level level,
            BlockPos source,
            BlockPos target,
            Direction direction
    ) {
        BlockPos.MutableBlockPos cursor =
                source.mutable();

        cursor.move(
                direction
        );

        while (!cursor.equals(
                target
        )) {
            if (!level.getBlockState(
                    cursor
            ).isAir()) {
                return false;
            }

            cursor.move(
                    direction
            );
        }

        return true;
    }
}
