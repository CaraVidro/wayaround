package net.caravidro.wayaround.worldgen.water.wave;

import net.minecraft.util.Mth;

/**
 * Optional external forcing for the realistic wave spectrum.
 *
 * Nothing in the current V1 wave pass feeds this record: REALISTIC waves are
 * deliberately self-contained. Future systems (wind, storms, Kraken, scripted
 * events) can supply a forcing without rewriting the spectrum itself.
 */
public record WaveForcing(
        double amplitudeMultiplier,
        double wavelengthMultiplier,
        double directionOffsetRadians,
        double steepnessMultiplier,
        double breakingBias,
        double runupMultiplier
) {

    public static final WaveForcing NEUTRAL =
            new WaveForcing(
                    1.0,
                    1.0,
                    0.0,
                    1.0,
                    0.0,
                    1.0
            );

    public WaveForcing {
        amplitudeMultiplier =
                Mth.clamp(
                        amplitudeMultiplier,
                        0.0,
                        8.0
                );

        wavelengthMultiplier =
                Mth.clamp(
                        wavelengthMultiplier,
                        0.20,
                        6.0
                );

        steepnessMultiplier =
                Mth.clamp(
                        steepnessMultiplier,
                        0.0,
                        3.0
                );

        breakingBias =
                Mth.clamp(
                        breakingBias,
                        -1.0,
                        1.0
                );

        runupMultiplier =
                Mth.clamp(
                        runupMultiplier,
                        0.0,
                        6.0
                );
    }

    public WaveForcing combine(
            WaveForcing other
    ) {
        if (other == null) {
            return this;
        }

        return new WaveForcing(
                amplitudeMultiplier
                        * other.amplitudeMultiplier,
                wavelengthMultiplier
                        * other.wavelengthMultiplier,
                directionOffsetRadians
                        + other.directionOffsetRadians,
                steepnessMultiplier
                        * other.steepnessMultiplier,
                breakingBias
                        + other.breakingBias,
                runupMultiplier
                        * other.runupMultiplier
        );
    }
}
