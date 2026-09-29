package net.caravidro.wayaround.worldgen.water.wave;

import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Samples the shared ocean field around a vessel footprint.
 *
 * <p>The result is intentionally a target/impulse description rather than a
 * teleported pose. Ships are expected to spring toward it, which keeps large
 * hulls heavy and slightly late compared with the water underneath them.</p>
 */
public final class WaveHullResponse {

    public record Response(
            double meanHeight,
            double meanVerticalVelocity,
            float targetPitch,
            float targetRoll,
            float breaker,
            Vec3 horizontalPush
    ) {
    }

    private WaveHullResponse() {
    }

    public static Response sample(
            Level level,
            Vec3 center,
            float yawDegrees,
            double halfWidth,
            double halfLength,
            long gameTime
    ) {
        Vec3 bow =
                localToWorld(
                        center,
                        yawDegrees,
                        0.0,
                        halfLength
                );

        Vec3 stern =
                localToWorld(
                        center,
                        yawDegrees,
                        0.0,
                        -halfLength
                );

        Vec3 port =
                localToWorld(
                        center,
                        yawDegrees,
                        -halfWidth,
                        0.0
                );

        Vec3 starboard =
                localToWorld(
                        center,
                        yawDegrees,
                        halfWidth,
                        0.0
                );

        OceanWaveField.Sample middle =
                OceanWaveField.sample(
                        level,
                        center.x,
                        center.z,
                        gameTime
                );

        OceanWaveField.Sample bowWave =
                OceanWaveField.sample(
                        level,
                        bow.x,
                        bow.z,
                        gameTime
                );

        OceanWaveField.Sample sternWave =
                OceanWaveField.sample(
                        level,
                        stern.x,
                        stern.z,
                        gameTime
                );

        OceanWaveField.Sample portWave =
                OceanWaveField.sample(
                        level,
                        port.x,
                        port.z,
                        gameTime
                );

        OceanWaveField.Sample starboardWave =
                OceanWaveField.sample(
                        level,
                        starboard.x,
                        starboard.z,
                        gameTime
                );

        double meanHeight =
                middle.height()
                        * 0.34
                        + (
                        bowWave.height()
                                + sternWave.height()
                                + portWave.height()
                                + starboardWave.height()
                ) * 0.165;

        double meanVerticalVelocity =
                middle.verticalVelocity()
                        * 0.34
                        + (
                        bowWave.verticalVelocity()
                                + sternWave.verticalVelocity()
                                + portWave.verticalVelocity()
                                + starboardWave.verticalVelocity()
                ) * 0.165;

        float pitch =
                (float) Math.toDegrees(
                        Math.atan2(
                                bowWave.height()
                                        - sternWave.height(),
                                Math.max(
                                        1.0,
                                        halfLength
                                                * 2.0
                                )
                        )
                );

        float roll =
                (float) Math.toDegrees(
                        Math.atan2(
                                starboardWave.height()
                                        - portWave.height(),
                                Math.max(
                                        1.0,
                                        halfWidth
                                                * 2.0
                                )
                        )
                );

        pitch =
                Mth.clamp(
                        pitch,
                        -12.0F,
                        12.0F
                );

        roll =
                Mth.clamp(
                        roll,
                        -15.0F,
                        15.0F
                );

        float breaker =
                Math.max(
                        middle.breaking(),
                        Math.max(
                                bowWave.breaking(),
                                Math.max(
                                        sternWave.breaking(),
                                        Math.max(
                                                portWave.breaking(),
                                                starboardWave.breaking()
                                        )
                                )
                        )
                );

        Vec3 horizontalPush =
                middle.horizontalVelocity()
                        .scale(
                                0.36
                        )
                        .add(
                                bowWave.horizontalVelocity()
                                        .scale(
                                                0.22
                                        )
                        )
                        .add(
                                sternWave.horizontalVelocity()
                                        .scale(
                                                0.14
                                        )
                        )
                        .add(
                                portWave.horizontalVelocity()
                                        .scale(
                                                0.14
                                        )
                        )
                        .add(
                                starboardWave.horizontalVelocity()
                                        .scale(
                                                0.14
                                        )
                        );

        return new Response(
                meanHeight,
                meanVerticalVelocity,
                pitch,
                roll,
                breaker,
                horizontalPush
        );
    }

    private static Vec3 localToWorld(
            Vec3 center,
            float yawDegrees,
            double localX,
            double localZ
    ) {
        return new Vec3(
                localX,
                0.0,
                localZ
        )
                .yRot(
                        -yawDegrees
                                * Mth.DEG_TO_RAD
                )
                .add(
                        center
                );
    }
}
