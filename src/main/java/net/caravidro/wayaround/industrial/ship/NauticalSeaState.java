package net.caravidro.wayaround.industrial.ship;

import net.caravidro.wayaround.worldconfig.WaveMode;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.caravidro.wayaround.worldgen.water.WaterDynamics;
import net.caravidro.wayaround.worldgen.water.wave.OceanWaveField;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Compatibility facade for Great Voyages.
 *
 * OFF keeps currents but exposes no wave energy.
 * STYLIZED preserves the pre-OceanWaveField lightweight sea-state math.
 * REALISTIC delegates wave energy to the shared OceanWaveField.
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

        float exposure =
                profile.exposure();

        float weatherStorm =
                level.isThundering()
                        ? 1.0F
                        : level.isRaining()
                        ? 0.56F
                        : 0.0F;

        Vec3 current =
                includeCurrent
                        ? WaterDynamics.currentAround(
                        level,
                        pos
                )
                        : Vec3.ZERO;

        WaveMode mode =
                WorldFeatureRuntime.waveMode(
                        level
                );

        if (mode
                == WaveMode.OFF) {
            return new Sample(
                    exposure,
                    weatherStorm,
                    0.0F,
                    0.0F,
                    current
            );
        }

        if (mode
                == WaveMode.STYLIZED) {
            double slow =
                    gameTime * 0.017
                            + pos.getX() * 0.012
                            - pos.getZ() * 0.009;

            double cross =
                    gameTime * 0.031
                            - pos.getX() * 0.006
                            - pos.getZ() * 0.014;

            float swell =
                    (float) Mth.clamp(
                            0.50
                                    + Math.sin(
                                    slow
                            ) * 0.34
                                    + Math.sin(
                                    cross
                            ) * 0.16,
                            0.0,
                            1.0
                    );

            float severity =
                    Mth.clamp(
                            0.12F
                                    + exposure
                                    * 0.62F
                                    + weatherStorm
                                    * 0.28F,
                            0.0F,
                            1.0F
                    );

            return new Sample(
                    exposure,
                    weatherStorm,
                    swell,
                    severity,
                    current
            );
        }

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
                                + exposure
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

        return new Sample(
                exposure,
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
