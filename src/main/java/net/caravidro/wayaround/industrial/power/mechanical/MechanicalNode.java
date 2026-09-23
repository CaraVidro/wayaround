package net.caravidro.wayaround.industrial.power.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

public interface MechanicalNode {
    BlockPos mechanicalPos();
    double mechanicalRpm();
    void setMechanicalRpm(double rpm);

    long lastMechanicalTick();
    void setLastMechanicalTick(long tick);

    double inertia();
    double frictionTorque();
    double loadTorque();
    double driveTorque();
    double maxSafeRpm();

    /**
     * Relative speed of this node's port. Zero means no mechanical connection.
     * Negative values reverse rotation. Gearboxes use this to transform speed/torque.
     */
    double portFactor(Direction side);

    default void afterMechanicalStep(ServerLevel level, boolean jammed) {}
}
