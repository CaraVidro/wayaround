package net.caravidro.wayaround.ecology.ai;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.phys.Vec3;

/**
 * Small intent layer that sits above vanilla goals.
 *
 * <p>It does not replace GoalSelector. It periodically gives animals a shared
 * ecological intention (cohesion, forage, return-home, migration), then lets
 * vanilla navigation execute it. Future species can add senses without every
 * animal system directly manipulating navigation.</p>
 */
public final class EcologyBrain {

    public enum IntentType {
        NONE,
        COHESION,
        FORAGE,
        RETURN_HOME,
        MIGRATE,
        SHELTER,
        EXPLORE
    }

    public record Intent(
            IntentType type,
            Vec3 target,
            double speed,
            float urgency
    ) {
        public static Intent none() {
            return new Intent(
                    IntentType.NONE,
                    Vec3.ZERO,
                    0.0,
                    0.0F
            );
        }
    }

    private EcologyBrain() {}

    public static Intent groupIntent(
            Animal animal,
            List<? extends Animal> group
    ) {
        if (group.size() <= 1) {
            return Intent.none();
        }

        double x = 0.0;
        double y = 0.0;
        double z = 0.0;

        for (Animal member : group) {
            x += member.getX();
            y += member.getY();
            z += member.getZ();
        }

        Vec3 center =
                new Vec3(
                        x / group.size(),
                        y / group.size(),
                        z / group.size()
                );

        double distance =
                animal.position()
                        .distanceTo(center);

        boolean flying =
                animal instanceof FlyingAnimal;

        double desiredDistance =
                flying
                        ? 7.5
                        : 6.0;

        if (distance <= desiredDistance) {
            return Intent.none();
        }

        Vec3 target =
                flying
                        ? center.add(
                                0.0,
                                Math.max(
                                        0.5,
                                        Math.min(
                                                4.0,
                                                center.y - animal.getY()
                                        )
                                ),
                                0.0
                        )
                        : new Vec3(
                                center.x,
                                animal.getY(),
                                center.z
                        );

        float urgency =
                (float) Math.min(
                        1.0,
                        (
                                distance - desiredDistance
                        )
                                / 16.0
                );

        return new Intent(
                IntentType.COHESION,
                target,
                flying
                        ? 1.18
                        : 1.02,
                urgency
        );
    }

    public static Intent homeIntent(
            Animal animal,
            Vec3 home
    ) {
        if (home == null) {
            return Intent.none();
        }

        double distance =
                animal.position()
                        .distanceTo(
                                home
                        );

        boolean flying =
                animal instanceof FlyingAnimal;

        double threshold =
                flying
                        ? 34.0
                        : 28.0;

        if (distance <= threshold) {
            return Intent.none();
        }

        return new Intent(
                IntentType.RETURN_HOME,
                flying
                        ? home.add(
                        0.0,
                        2.0,
                        0.0
                )
                        : new Vec3(
                        home.x,
                        animal.getY(),
                        home.z
                ),
                flying
                        ? 1.16
                        : 0.98,
                (float) Math.min(
                        1.0,
                        (
                                distance - threshold
                        )
                                / 28.0
                )
        );
    }

    public static Intent shelterIntent(
            ServerLevel level,
            Animal animal
    ) {
        if (!level.isRainingAt(
                animal.blockPosition()
                        .above()
        )) {
            return Intent.none();
        }

        BlockPos origin =
                animal.blockPosition();

        BlockPos best =
                null;

        double bestDistance =
                Double.MAX_VALUE;

        for (int dx = -8; dx <= 8; dx += 2) {
            for (int dz = -8; dz <= 8; dz += 2) {
                BlockPos candidate =
                        origin.offset(
                                dx,
                                0,
                                dz
                        );

                BlockPos ground =
                        level.getHeightmapPos(
                                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                candidate
                        );

                if (level.canSeeSky(
                        ground
                )) {
                    continue;
                }

                if (!level.getBlockState(
                        ground.below()
                ).isSolidRender(
                        level,
                        ground.below()
                )) {
                    continue;
                }

                double distance =
                        origin.distSqr(
                                ground
                        );

                if (distance < bestDistance) {
                    bestDistance =
                            distance;

                    best =
                            ground.immutable();
                }
            }
        }

        if (best == null) {
            return Intent.none();
        }

        boolean flying =
                animal instanceof FlyingAnimal;

        Vec3 target =
                Vec3.atCenterOf(
                        best
                );

        if (flying) {
            target =
                    target.add(
                            0.0,
                            1.8,
                            0.0
                    );
        }

        return new Intent(
                IntentType.SHELTER,
                target,
                flying
                        ? 1.22
                        : 1.05,
                0.72F
        );
    }

    public static Intent exploreIntent(
            ServerLevel level,
            Animal animal,
            Vec3 home
    ) {
        if (animal.getNavigation()
                .isInProgress()) {
            return Intent.none();
        }

        if (level.random.nextFloat()
                > 0.12F) {
            return Intent.none();
        }

        double radius =
                animal instanceof FlyingAnimal
                        ? 18.0
                        : 12.0;

        double angle =
                level.random.nextDouble()
                        * Math.PI
                        * 2.0;

        Vec3 center =
                home == null
                        ? animal.position()
                        : home;

        Vec3 target =
                new Vec3(
                        center.x
                                + Math.cos(
                                angle
                        )
                                        * radius,
                        animal.getY(),
                        center.z
                                + Math.sin(
                                angle
                        )
                                        * radius
                );

        if (animal instanceof FlyingAnimal) {
            target =
                    target.add(
                            0.0,
                            2.0
                                    + level.random.nextDouble()
                                            * 4.0,
                            0.0
                    );
        }

        return new Intent(
                IntentType.EXPLORE,
                target,
                animal instanceof FlyingAnimal
                        ? 1.12
                        : 0.92,
                0.30F
        );
    }

    public static void apply(
            Animal animal,
            Intent intent
    ) {
        if (intent == null
                || intent.type() == IntentType.NONE
                || intent.urgency() <= 0.05F) {
            return;
        }

        animal.getNavigation()
                .moveTo(
                        intent.target().x,
                        intent.target().y,
                        intent.target().z,
                        intent.speed()
                );

        animal.getLookControl()
                .setLookAt(
                        intent.target().x,
                        intent.target().y,
                        intent.target().z,
                        24.0F,
                        20.0F
                );
    }
}
