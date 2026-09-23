package net.caravidro.wayaround.industrial.mechanical;

import net.minecraft.core.Direction;

/**
 * Generic mechanical output.
 *
 * Nothing here mentions Forge Energy. Consumers are free to turn rotation
 * into electricity, motion, pumping, cutting, crushing, etc.
 */
public interface IRotationalPower {

    float rpm();

    float torque();

    float power();

    Direction.Axis axis();

    /**
     * -1 or +1 while rotating, 0 while stopped.
     */
    int rotationDirection();

    default boolean active() {
        return Math.abs(rpm()) > 0.01F
                && power() > 0.01F;
    }
}
