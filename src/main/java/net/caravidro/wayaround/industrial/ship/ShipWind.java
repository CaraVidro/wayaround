package net.caravidro.wayaround.industrial.ship;

import net.caravidro.wayaround.worldgen.weather.BlizzardManager;
import net.caravidro.wayaround.worldgen.weather.BlizzardWind;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Shared V0 wind sample for experimental sailing.
 *
 * <p>Direction reuses the weather wind phase already used by Way Around.
 * Sailing is intentionally modest in calm weather and visibly stronger during
 * rain, thunder and blizzards.</p>
 */
public final class ShipWind {

    private ShipWind() {
    }

    public static Vec3 sample(
            ServerLevel level,
            Vec3 position
    ) {
        double time =
                level.getGameTime();

        double angle =
                BlizzardWind.angle(
                        time
                );

        double weather =
                level.isThundering()
                        ? 1.0
                        : level.isRaining()
                                ? 0.58
                                : 0.18;

        double blizzard =
                BlizzardManager.getIntensity(
                        level,
                        position
                );

        double gust =
                0.86
                        + Math.sin(
                        time / 57.0
                )
                        * 0.09
                        + Math.sin(
                        time / 131.0
                )
                        * 0.05;

        double speed =
                (
                        0.010
                                + weather
                                        * 0.022
                                + blizzard
                                        * 0.032
                )
                        * gust;

        return new Vec3(
                Math.cos(
                        angle
                )
                        * speed,
                0.0,
                Math.sin(
                        angle
                )
                        * speed
        );
    }
}
