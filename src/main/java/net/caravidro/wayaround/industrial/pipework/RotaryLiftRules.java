package net.caravidro.wayaround.industrial.pipework;

/**
 * Static family rules for rotary water-lift heads.
 */
public final class RotaryLiftRules {

    public enum TransferMode {
        VISUAL,
        PHYSICAL
    }

    private RotaryLiftRules() {
    }

    public static boolean supportsWaterLift(
            PipeSpec spec
    ) {
        return spec != null
                && spec.supports(
                        PipeSpec.PipeMedium.LIQUID
                );
    }

    public static TransferMode mode(
            PipeSpec spec
    ) {
        if (!supportsWaterLift(
                spec
        )) {
            throw new IllegalArgumentException(
                    "Rotary water lift requires a liquid-capable pipe family"
            );
        }

        return spec == PipeCatalog.LARGE_WATER_MAIN
                ? TransferMode.PHYSICAL
                : TransferMode.VISUAL;
    }

    public static boolean physicalTransfer(
            PipeSpec spec
    ) {
        return mode(spec)
                == TransferMode.PHYSICAL;
    }
}
