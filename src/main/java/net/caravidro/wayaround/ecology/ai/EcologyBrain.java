package net.caravidro.wayaround.ecology.ai;

import java.util.List;

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
