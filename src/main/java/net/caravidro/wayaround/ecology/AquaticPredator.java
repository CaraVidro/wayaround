package net.caravidro.wayaround.ecology;

/**
 * Marker/profile for fish that actively hunt other fish through the shared
 * LivingFaunaManager. This keeps predation modular instead of hard-coding
 * every new predator class into the AI loop.
 */
public interface AquaticPredator {

    default double huntRadius() {
        return 13.0;
    }

    default double huntVerticalRadius() {
        return 6.0;
    }

    default double huntSpeed() {
        return 1.58;
    }

    default double biteReach() {
        return 1.35;
    }

    default float biteBaseDamage() {
        return 3.0F;
    }

    default float biteScaleDamage() {
        return 1.0F;
    }

    default int scareTicks() {
        return 120;
    }
}
