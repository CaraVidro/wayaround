package net.caravidro.wayaround.industrial.pipework;

import java.util.EnumSet;
import java.util.Set;

public record PipeSpec(
        String id,
        String displayName,
        float radius,
        int flowPerTick,
        float maxPressureBar,
        int maxTemperatureC,
        Set<PipeMedium> media
) {
    public enum PipeMedium {
        LIQUID,
        GAS,
        STEAM
    }

    public PipeSpec {
        media = Set.copyOf(media);
    }

    public boolean supports(PipeMedium medium) {
        return media.contains(medium);
    }

    /**
     * Radius is already expressed in block units; WayAround treats one block
     * as one metre for macroscopic engineering geometry.
     */
    public double internalCrossSectionM2() {
        double radiusM =
                Math.max(
                        0.0,
                        radius
                );

        return Math.PI
                * radiusM
                * radiusM;
    }

    public double internalVolumeM3(
            double lengthM
    ) {
        if (!Double.isFinite(
                lengthM
        )
                || lengthM < 0.0) {
            throw new IllegalArgumentException(
                    "lengthM must be finite and >= 0"
            );
        }

        return internalCrossSectionM2()
                * lengthM;
    }

    public boolean compatible(PipeSpec other) {
        for (PipeMedium medium : media) {
            if (other.media.contains(medium)) {
                return true;
            }
        }
        return false;
    }

    public static Set<PipeMedium> media(PipeMedium... media) {
        EnumSet<PipeMedium> set = EnumSet.noneOf(PipeMedium.class);
        java.util.Collections.addAll(set, media);
        return set;
    }
}
