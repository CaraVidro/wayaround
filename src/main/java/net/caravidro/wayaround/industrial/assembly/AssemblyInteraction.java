package net.caravidro.wayaround.industrial.assembly;

import javax.annotation.Nullable;

import net.caravidro.wayaround.industrial.power.WaterWheelHubBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class AssemblyInteraction {

    private static final double REACH =
            6.25;

    private AssemblyInteraction() {
    }

    @Nullable
    public static WheelHit raycastWheel(
            Player player,
            Level level
    ) {
        Vec3 eye =
                player.getEyePosition();

        Vec3 look =
                player.getViewVector(
                        1.0F
                ).normalize();

        BlockPos origin =
                player.blockPosition();

        WheelHit best =
                null;

        double bestDistance =
                Double.MAX_VALUE;

        for (BlockPos pos :
                BlockPos.betweenClosed(
                        origin.offset(
                                -6,
                                -6,
                                -6
                        ),
                        origin.offset(
                                6,
                                6,
                                6
                        )
                )) {

            if (!(level.getBlockEntity(pos)
                    instanceof WaterWheelHubBlockEntity hub)) {
                continue;
            }

            Vec3 center =
                    Vec3.atCenterOf(
                            pos
                    );

            Direction.Axis axis =
                    hub.axleAxis();

            Vec3 planeNormal =
                    axis == Direction.Axis.X
                            ? new Vec3(
                                    1.0,
                                    0.0,
                                    0.0
                            )
                            : new Vec3(
                                    0.0,
                                    0.0,
                                    1.0
                            );

            double denominator =
                    look.dot(
                            planeNormal
                    );

            if (Math.abs(
                    denominator
            ) < 0.0001) {
                continue;
            }

            double distance =
                    center.subtract(
                            eye
                    ).dot(
                            planeNormal
                    )
                    / denominator;

            if (distance < 0.0
                    || distance > REACH
                    || distance >= bestDistance) {
                continue;
            }

            Vec3 hit =
                    eye.add(
                            look.scale(
                                    distance
                            )
                    );

            double horizontal =
                    axis == Direction.Axis.X
                            ? hit.z - center.z
                            : hit.x - center.x;

            double vertical =
                    hit.y - center.y;

            double radius =
                    Math.sqrt(
                            horizontal * horizontal
                            + vertical * vertical
                    );

            double worldAngle =
                    Math.atan2(
                            vertical,
                            horizontal
                    );

            int plateIndex =
                    hub.findPlateAt(
                            worldAngle,
                            radius
                    );

            boolean frame =
                    hub.frameHit(
                            radius
                    );

            if (plateIndex < 0
                    && !frame) {
                continue;
            }

            double localAngle =
                    normalizeAngle(
                            worldAngle
                            - Math.toRadians(
                                    hub.rotationDegrees()
                            )
                    );

            best =
                    new WheelHit(
                            hub,
                            hit,
                            radius,
                            worldAngle,
                            localAngle,
                            plateIndex,
                            frame
                    );

            bestDistance =
                    distance;
        }

        return best;
    }

    private static double normalizeAngle(
            double angle
    ) {
        double full =
                Math.PI
                * 2.0;

        angle %=
                full;

        if (angle < 0.0) {
            angle +=
                    full;
        }

        return angle;
    }

    public record WheelHit(
            WaterWheelHubBlockEntity hub,
            Vec3 location,
            double radius,
            double worldAngle,
            double localAngle,
            int plateIndex,
            boolean frame
    ) {
    }
}
