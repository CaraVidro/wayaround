package net.caravidro.wayaround.industrial.ship;

import net.caravidro.wayaround.worldgen.water.WaterDynamics;
import net.caravidro.wayaround.worldgen.water.wave.OceanWaveField;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Compatibility facade for older Great Voyages code.
 *
 * <p>OceanWaveField is now the source of truth. This class intentionally keeps
 * the old Sample shape so existing integrations do not need to change all at
 * once, but no independent wave mathematics lives here anymore.</p>
 */
public final class NauticalSeaState {

    public record Sample(
            float exposure,
            float storm,
            float swell,
            float severity,
            Vec3 current
    ) {
    }

    private NauticalSeaState() {
    }

    public static Sample sample(
            Level level,
            BlockPos pos,
            long gameTime
    ) {
        return sample(
                level,
                pos,
                gameTime,
                true
        );
    }

    public static Sample visual(
            Level level,
            BlockPos pos,
            long gameTime
    ) {
        return sample(
                level,
                pos,
                gameTime,
                false
        );
    }

    private static Sample sample(
            Level level,
            BlockPos pos,
            long gameTime,
            boolean includeCurrent
    ) {
        OceanWaveField.Profile profile =
                OceanWaveField.profile(
                        level,
                        pos,
                        gameTime
                );

        OceanWaveField.Sample wave =
                OceanWaveField.sample(
                        profile,
                        pos.getX()
                                + 0.5,
                        pos.getZ()
                                + 0.5,
                        gameTime
                );

        float severity =
                Mth.clamp(
                        0.08F
                                + profile.exposure()
                                * 0.38F
                                + profile.storm()
                                * 0.30F
                                + wave.breaking()
                                * 0.24F
                                + Mth.clamp(
                                profile.amplitude()
                                        / 2.2F,
                                0.0F,
                                0.34F
                        ),
                        0.0F,
                        1.0F
                );

        Vec3 current =
                includeCurrent
                        ? WaterDynamics.currentAround(
                        level,
                        pos
                )
                        : Vec3.ZERO;

        return new Sample(
                profile.exposure(),
                profile.storm(),
                wave.crest(),
                severity,
                current
        );
    }

    public static float oceanExposure(
            Level level,
            BlockPos center
    ) {
        return OceanWaveField.profile(
                level,
                center,
                level.getGameTime()
        ).exposure();
    }
}
