package net.caravidro.wayaround.worldgen.water.wave;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Stateless multi-band realistic ocean spectrum.
 *
 * Inspired by the practical real-time approach of summing a small number of
 * independently moving waves. Long swells carry the silhouette; medium waves
 * break repetition; small chop adds local surface complexity.
 *
 * This intentionally stays CPU-cheap and deterministic. It is not an FFT
 * ocean, but it borrows the useful principle that different wavelengths move
 * at different speeds and interfere naturally.
 */
public final class RealisticWaveSpectrum {

    public record Result(
            double height,
            double dHdx,
            double dHdz,
            double verticalVelocity,
            Vec3 horizontalVelocity,
            float crest,
            float breaking,
            double primaryPhase
    ) {
    }

    private record WaveBand(
            double wavelengthScale,
            double amplitudeWeight,
            double directionOffsetDegrees,
            double steepness,
            double phaseOffset,
            double envelopeScale,
            double envelopeSpeed,
            double directionWander,
            long salt
    ) {
    }

    /*
     * Deliberately few geometric bands. Their independence matters more than
     * raw count: large and small waves can sit beside each other and sometimes
     * reinforce into a much larger crest.
     */
    private static final WaveBand[] BANDS = {
            new WaveBand(
                    1.34,
                    0.48,
                    -7.0,
                    0.38,
                    0.0,
                    0.055,
                    0.045,
                    7.0,
                    0xA11CE001L
            ),
            new WaveBand(
                    0.96,
                    0.31,
                    26.0,
                    0.34,
                    1.7,
                    0.071,
                    0.058,
                    9.0,
                    0xA11CE002L
            ),
            new WaveBand(
                    0.61,
                    0.23,
                    -31.0,
                    0.48,
                    3.1,
                    0.094,
                    0.073,
                    12.0,
                    0xA11CE003L
            ),
            new WaveBand(
                    0.43,
                    0.17,
                    49.0,
                    0.54,
                    4.8,
                    0.123,
                    0.091,
                    15.0,
                    0xA11CE004L
            ),
            new WaveBand(
                    0.24,
                    0.10,
                    76.0,
                    0.42,
                    0.9,
                    0.176,
                    0.121,
                    18.0,
                    0xA11CE005L
            ),
            new WaveBand(
                    0.16,
                    0.065,
                    -68.0,
                    0.32,
                    5.9,
                    0.231,
                    0.153,
                    22.0,
                    0xA11CE006L
            )
    };

    /*
     * Deep-water dispersion is physically based, but slowed for Minecraft's
     * scale/readability. Relative speeds stay believable: long swells travel
     * faster than short chop.
     */
    private static final double GRAVITY =
            9.81;

    private static final double TIME_SCALE =
            0.32;

    private RealisticWaveSpectrum() {
    }

    public static Result sample(
            OceanWaveField.Profile profile,
            double x,
            double z,
            long gameTime,
            double localLength,
            double localEnergy,
            float rogue,
            WaveForcing forcing
    ) {
        if (forcing == null) {
            forcing =
                    WaveForcing.NEUTRAL;
        }

        double seconds =
                gameTime
                        / 20.0;

        double baseAngle =
                Math.atan2(
                        profile.directionZ(),
                        profile.directionX()
                )
                        + forcing.directionOffsetRadians();

        double height =
                0.0;

        double dHdx =
                0.0;

        double dHdz =
                0.0;

        double verticalVelocity =
                0.0;

        double pushX =
                0.0;

        double pushZ =
                0.0;

        double expectedAmplitude =
                0.0;

        double primaryPhase =
                0.0;

        double strongestContribution =
                -1.0;

        for (int i = 0;
             i < BANDS.length;
             i++) {

            WaveBand band =
                    BANDS[i];

            double wander =
                    Math.sin(
                            x * 0.00113
                                    + z * 0.00079
                                    + band.phaseOffset()
                    )
                            * band.directionWander()
                            * Mth.DEG_TO_RAD;

            double angle =
                    baseAngle
                            + band.directionOffsetDegrees()
                            * Mth.DEG_TO_RAD
                            + wander;

            double dirX =
                    Math.cos(
                            angle
                    );

            double dirZ =
                    Math.sin(
                            angle
                    );

            double wavelength =
                    Math.max(
                            2.6,
                            profile.wavelength()
                                    * localLength
                                    * band.wavelengthScale()
                                    * forcing.wavelengthMultiplier()
                    );

            /*
             * Rogue energy primarily enlarges the long/medium structure. Tiny
             * chop does not become a six-block saw blade just because a large
             * regional crest is passing through.
             */
            double rogueBandScale =
                    i <= 1
                            ? 1.0
                            + rogue
                            * 2.8
                            : i <= 3
                            ? 1.0
                            + rogue
                            * 1.15
                            : 1.0
                            + rogue
                            * 0.24;

            double amplitude =
                    profile.amplitude()
                            * band.amplitudeWeight()
                            * localEnergy
                            * forcing.amplitudeMultiplier()
                            * rogueBandScale;

            /*
             * Avoid pathological steepness regardless of external forcing.
             * Keeping A/L bounded prevents a few aligned bands from turning
             * into needle-like geometry.
             */
            amplitude =
                    Math.min(
                            amplitude,
                            wavelength
                                    * 0.052
                    );

            double k =
                    Math.PI
                            * 2.0
                            / wavelength;

            double omega =
                    Math.sqrt(
                            GRAVITY
                                    * k
                    )
                            * TIME_SCALE;

            double envelope =
                    0.62
                            + 0.38
                            * (
                            0.5
                                    + 0.5
                                    * Math.sin(
                                    (
                                            x * dirX
                                                    + z * dirZ
                                    )
                                            * band.envelopeScale()
                                            - seconds
                                            * band.envelopeSpeed()
                                            + band.phaseOffset()
                            )
                    );

            /*
             * Each band breathes independently. This is what allows a large
             * swell to sit next to ordinary waves without scaling the entire
             * local ocean up and down as one rigid triangle.
             */
            amplitude *=
                    envelope;

            double phaseNoise =
                    Math.sin(
                            x * 0.0021
                                    - z * 0.0017
                                    + band.salt()
                                    * 1.0E-6
                    )
                            * 0.34;

            double phase =
                    (
                            x * dirX
                                    + z * dirZ
                    )
                            * k
                            - seconds
                            * omega
                            + band.phaseOffset()
                            + phaseNoise;

            double sin =
                    Math.sin(
                            phase
                    );

            double cos =
                    Math.cos(
                            phase
                    );

            double steepness =
                    Mth.clamp(
                            band.steepness()
                                    * forcing.steepnessMultiplier(),
                            0.0,
                            1.25
                    );

            /*
             * Second-order Stokes-like harmonic: sharper crests and broader
             * troughs while retaining a single-valued heightfield.
             */
            double harmonicAmplitude =
                    0.5
                            * k
                            * amplitude
                            * amplitude
                            * steepness;

            double sin2 =
                    Math.sin(
                            phase
                                    * 2.0
                    );

            double cos2 =
                    Math.cos(
                            phase
                                    * 2.0
                    );

            double componentHeight =
                    amplitude
                            * cos
                            + harmonicAmplitude
                            * cos2;

            double phaseGradient =
                    -amplitude
                            * sin
                            - 2.0
                            * harmonicAmplitude
                            * sin2;

            height +=
                    componentHeight;

            dHdx +=
                    phaseGradient
                            * k
                            * dirX;

            dHdz +=
                    phaseGradient
                            * k
                            * dirZ;

            verticalVelocity +=
                    (
                            amplitude
                                    * omega
                                    * sin
                                    + 2.0
                                    * harmonicAmplitude
                                    * omega
                                    * sin2
                    )
                            / 20.0;

            /*
             * Retained as a future-facing orbital motion channel. Current
             * vessels deliberately do not consume this as propulsion.
             */
            double orbital =
                    amplitude
                            * omega
                            * 0.010;

            pushX +=
                    dirX
                            * orbital
                            * cos;

            pushZ +=
                    dirZ
                            * orbital
                            * cos;

            expectedAmplitude +=
                    amplitude;

            double contribution =
                    Math.abs(
                            componentHeight
                    );

            if (contribution
                    > strongestContribution) {
                strongestContribution =
                        contribution;

                primaryPhase =
                        phase;
            }
        }

        double safeAmplitude =
                Math.max(
                        0.08,
                        expectedAmplitude
                                * 0.72
                );

        double normalized =
                height
                        / safeAmplitude;

        float crest =
                Mth.clamp(
                        (float) (
                                0.50
                                        + normalized
                                        * 0.36
                        ),
                        0.0F,
                        1.0F
                );

        double slope =
                Math.sqrt(
                        dHdx * dHdx
                                + dHdz * dHdz
                );

        float breaking =
                Mth.clamp(
                        (float) (
                                Math.max(
                                        0.0,
                                        slope
                                                - 0.16
                                ) * 0.92
                                        + Math.max(
                                        0.0,
                                        crest
                                                - 0.76F
                                ) * 1.05
                                        + rogue
                                        * 0.24
                                        + forcing.breakingBias()
                        ),
                        0.0F,
                        1.0F
                );

        return new Result(
                height,
                dHdx,
                dHdz,
                verticalVelocity,
                new Vec3(
                        pushX,
                        0.0,
                        pushZ
                ),
                crest,
                breaking,
                primaryPhase
        );
    }
}
