package net.caravidro.wayaround.ecology;

import net.caravidro.wayaround.pressure.PressureMath;

/**
 * Compatibility/exposure rules for abyss vehicles.
 *
 * <p>The old API still accepts depth for deterministic tests, but converts that
 * depth into hydrostatic differential pressure immediately. Runtime vehicles
 * use advancePressure directly with NaturalPressure samples.</p>
 */
public final class OceanPressure {

    public static final int WARNING =
            1800;

    public static final int CRITICAL =
            3000;

    public static final int FAILURE =
            3600;

    public static final double SEAWATER_DENSITY_KG_M3 =
            1025.0;

    public static final double CAPSULE_SAFE_DIFFERENTIAL_BAR =
            PressureMath.kPaToBar(
                    PressureMath.hydrostaticGaugeKPa(
                            SEAWATER_DENSITY_KG_M3,
                            70.0
                    )
            );

    public static final double SUBMARINE_SAFE_DIFFERENTIAL_BAR =
            PressureMath.kPaToBar(
                    PressureMath.hydrostaticGaugeKPa(
                            SEAWATER_DENSITY_KG_M3,
                            85.0
                    )
            );

    private OceanPressure() {
    }

    /**
     * Legacy depth adapter. Depth is no longer the failure criterion.
     */
    public static int advance(
            int exposure,
            double depth,
            boolean water,
            boolean capsule
    ) {
        double differentialKPa =
                water
                        ? PressureMath.hydrostaticGaugeKPa(
                                SEAWATER_DENSITY_KG_M3,
                                Math.max(
                                        0.0,
                                        depth
                                )
                        )
                        : 0.0;

        return advanceDifferential(
                exposure,
                differentialKPa,
                water,
                capsule
        );
    }

    public static int advancePressure(
            int exposure,
            double outsideAbsoluteKPa,
            double insideAbsoluteKPa,
            boolean water,
            boolean capsule
    ) {
        return advanceDifferential(
                exposure,
                Math.max(
                        0.0,
                        PressureMath.differentialKPa(
                                outsideAbsoluteKPa,
                                insideAbsoluteKPa
                        )
                ),
                water,
                capsule
        );
    }

    public static int advanceDifferential(
            int exposure,
            double differentialKPa,
            boolean water,
            boolean capsule
    ) {
        double limitBar =
                capsule
                        ? CAPSULE_SAFE_DIFFERENTIAL_BAR
                        : SUBMARINE_SAFE_DIFFERENTIAL_BAR;

        boolean unsafe =
                water
                        && PressureMath.kPaToBar(
                        Math.max(
                                0.0,
                                differentialKPa
                        )
                )
                        >= limitBar;

        return unsafe
                ? Math.min(
                        FAILURE,
                        exposure + 1
                )
                : Math.max(
                        0,
                        exposure - 4
                );
    }
}
