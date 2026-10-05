package net.caravidro.wayaround.thermal;

import net.caravidro.wayaround.physical.MatterPhase;
import net.caravidro.wayaround.physical.PhysicalMaterials;
import net.caravidro.wayaround.physical.PhysicalRegionSnapshot;

/**
 * Lumped thermal model for one bounded PhysicalRegion.
 *
 * <p>This is intentionally not CFD. A room/compartment is treated as a
 * well-mixed body of air whose heat capacity comes from its real volume. Walls
 * lose heat through their material conductivity and openings add rapid air
 * exchange. The same contract can later back tanks and vehicle compartments.</p>
 */
public record ThermalRegionModel(
        double heatCapacityJPerK,
        double wallConductanceWPerK,
        double openingConductanceWPerK,
        double totalConductanceWPerK,
        double timeConstantSeconds,
        double relaxationTicks
) {

    public static final double OPENING_EXCHANGE_W_PER_M2K =
            120.0;

    private static final double MIN_CONDUCTANCE_W_PER_K =
            0.25;

    private static final double MIN_RELAXATION_TICKS =
            40.0;

    private static final double MAX_RELAXATION_TICKS =
            24_000.0;

    public ThermalRegionModel {
        heatCapacityJPerK =
                positive(
                        heatCapacityJPerK,
                        "heatCapacityJPerK"
                );

        wallConductanceWPerK =
                nonNegative(
                        wallConductanceWPerK,
                        "wallConductanceWPerK"
                );

        openingConductanceWPerK =
                nonNegative(
                        openingConductanceWPerK,
                        "openingConductanceWPerK"
                );

        totalConductanceWPerK =
                positive(
                        totalConductanceWPerK,
                        "totalConductanceWPerK"
                );

        timeConstantSeconds =
                positive(
                        timeConstantSeconds,
                        "timeConstantSeconds"
                );

        relaxationTicks =
                positive(
                        relaxationTicks,
                        "relaxationTicks"
                );
    }

    public static ThermalRegionModel from(
            PhysicalRegionSnapshot region
    ) {
        var air =
                PhysicalMaterials.AIR.phase(
                        MatterPhase.GAS
                );

        double airMassKg =
                air.densityKgPerM3()
                        * Math.max(
                        0.05,
                        region.volumeM3()
                );

        double heatCapacity =
                airMassKg
                        * air.specificHeatJPerKgK();

        double wallConductance =
                region.boundaries()
                        .stream()
                        .mapToDouble(
                                ThermalPhysics::boundaryConductanceWPerK
                        )
                        .sum();

        double openingConductance =
                region.openingAreaM2()
                        * OPENING_EXCHANGE_W_PER_M2K;

        /*
         * INDETERMINATE regions are never granted perfect insulation. A scan
         * that stopped at a fidelity boundary must cool at least as quickly as
         * one square metre of open exchange until topology is known.
         */
        if (!region.fullyCharacterized()) {
            openingConductance =
                    Math.max(
                            openingConductance,
                            OPENING_EXCHANGE_W_PER_M2K
                    );
        }

        double totalConductance =
                Math.max(
                        MIN_CONDUCTANCE_W_PER_K,
                        wallConductance
                                + openingConductance
                );

        double timeConstant =
                heatCapacity
                        / totalConductance;

        double relaxationTicks =
                Math.clamp(
                        timeConstant
                                * 20.0,
                        MIN_RELAXATION_TICKS,
                        MAX_RELAXATION_TICKS
                );

        return new ThermalRegionModel(
                heatCapacity,
                wallConductance,
                openingConductance,
                totalConductance,
                timeConstant,
                relaxationTicks
        );
    }

    public double steadyTemperatureRiseC(
            double powerW
    ) {
        if (!Double.isFinite(
                powerW
        )
                || powerW <= 0.0) {
            return 0.0;
        }

        return powerW
                / totalConductanceWPerK;
    }

    public double temperatureDeltaForEnergy(
            double energyJ
    ) {
        if (!Double.isFinite(
                energyJ
        )) {
            return 0.0;
        }

        return energyJ
                / heatCapacityJPerK;
    }

    private static double positive(
            double value,
            String name
    ) {
        if (!Double.isFinite(
                value
        )
                || value <= 0.0) {
            throw new IllegalArgumentException(
                    name + " must be finite and > 0"
            );
        }

        return value;
    }

    private static double nonNegative(
            double value,
            String name
    ) {
        if (!Double.isFinite(
                value
        )
                || value < 0.0) {
            throw new IllegalArgumentException(
                    name + " must be finite and >= 0"
            );
        }

        return value;
    }
}
