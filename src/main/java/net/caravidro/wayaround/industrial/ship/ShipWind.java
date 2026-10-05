package net.caravidro.wayaround.industrial.ship;

import net.caravidro.wayaround.flow.UniversalFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Sailing adapter over the universal atmospheric flow field.
 */
public final class ShipWind {

    private ShipWind() {
    }

    public static Vec3 sample(
            ServerLevel level,
            Vec3 position
    ) {
        return UniversalFlow.atmosphereAt(
                level,
                position
        ).velocityPerTick();
    }
}
