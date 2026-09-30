package net.caravidro.wayaround.industrial.pipework;

import net.caravidro.wayaround.industrial.pipework.PipeSpec.PipeMedium;

public final class PipeworkSpecTest {

    public static void main(
            String[] args
    ) {
        mediumCompatibilityMatchesDesign();
        capacitiesStayOrdered();
        steamLineRetainsHighTemperatureRole();

        System.out.println(
                "Pipework specification regression tests passed"
        );
    }

    private static void mediumCompatibilityMatchesDesign() {
        PipeSpec copper =
                PipeworkContent.SMALL_COPPER_SPEC;

        PipeSpec gas =
                PipeworkContent.THIN_GAS_SPEC;

        PipeSpec pressure =
                PipeworkContent.STEEL_PRESSURE_SPEC;

        require(
                copper.supports(
                        PipeMedium.LIQUID
                ),
                "Copper pipe should carry liquids"
        );

        require(
                !copper.supports(
                        PipeMedium.GAS
                ),
                "Copper pipe should not be a gas line"
        );

        require(
                gas.supports(
                        PipeMedium.GAS
                ),
                "Thin gas pipe should carry gas"
        );

        require(
                !gas.supports(
                        PipeMedium.LIQUID
                ),
                "Thin gas pipe should not carry liquids"
        );

        require(
                copper.compatible(
                        pressure
                ),
                "Steel pressure pipe should bridge liquid systems"
        );

        require(
                gas.compatible(
                        pressure
                ),
                "Steel pressure pipe should bridge gas systems"
        );

        require(
                !copper.compatible(
                        gas
                ),
                "Dedicated liquid and gas pipes must not directly connect"
        );
    }

    private static void capacitiesStayOrdered() {
        require(
                PipeworkContent.LARGE_WATER_MAIN_SPEC.flowPerTick()
                        > PipeworkContent.IRON_WATER_SPEC.flowPerTick(),
                "Large water main should move more liquid than utility pipe"
        );

        require(
                PipeworkContent.IRON_WATER_SPEC.flowPerTick()
                        > PipeworkContent.SMALL_COPPER_SPEC.flowPerTick(),
                "Utility pipe should move more liquid than small copper tubing"
        );

        require(
                PipeworkContent.STEEL_PRESSURE_SPEC.maxPressureBar()
                        > PipeworkContent.IRON_WATER_SPEC.maxPressureBar(),
                "Pressure pipe must tolerate more pressure than utility water pipe"
        );
    }

    private static void steamLineRetainsHighTemperatureRole() {
        PipeSpec steam =
                PipeworkContent.STEAM_SPEC;

        require(
                steam.supports(
                        PipeMedium.STEAM
                ),
                "Insulated steam pipe must explicitly support steam"
        );

        require(
                steam.maxTemperatureC()
                        > PipeworkContent.STEEL_PRESSURE_SPEC.maxTemperatureC(),
                "Insulated steam pipe should have the highest thermal rating"
        );
    }

    private static void require(
            boolean condition,
            String message
    ) {
        if (!condition) {
            throw new AssertionError(
                    message
            );
        }
    }
}
