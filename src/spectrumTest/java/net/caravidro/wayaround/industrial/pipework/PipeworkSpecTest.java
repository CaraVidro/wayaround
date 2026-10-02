package net.caravidro.wayaround.industrial.pipework;

import net.caravidro.wayaround.industrial.pipework.PipeSpec.PipeMedium;

public final class PipeworkSpecTest {

    public static void main(
            String[] args
    ) {
        mediumCompatibilityMatchesDesign();
        capacitiesStayOrdered();
        steamLineRetainsHighTemperatureRole();
        rotaryWaterLiftModesStayConservative();

        System.out.println(
                "Pipework specification regression tests passed"
        );
    }

    private static void mediumCompatibilityMatchesDesign() {
        PipeSpec copper =
                PipeCatalog.SMALL_COPPER;

        PipeSpec gas =
                PipeCatalog.THIN_GAS;

        PipeSpec pressure =
                PipeCatalog.STEEL_PRESSURE;

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
                PipeCatalog.LARGE_WATER_MAIN.flowPerTick()
                        > PipeCatalog.IRON_WATER.flowPerTick(),
                "Large water main should move more liquid than utility pipe"
        );

        require(
                PipeCatalog.IRON_WATER.flowPerTick()
                        > PipeCatalog.SMALL_COPPER.flowPerTick(),
                "Utility pipe should move more liquid than small copper tubing"
        );

        require(
                PipeCatalog.STEEL_PRESSURE.maxPressureBar()
                        > PipeCatalog.IRON_WATER.maxPressureBar(),
                "Pressure pipe must tolerate more pressure than utility water pipe"
        );
    }

    private static void steamLineRetainsHighTemperatureRole() {
        PipeSpec steam =
                PipeCatalog.INSULATED_STEAM;

        require(
                steam.supports(
                        PipeMedium.STEAM
                ),
                "Insulated steam pipe must explicitly support steam"
        );

        require(
                steam.maxTemperatureC()
                        > PipeCatalog.STEEL_PRESSURE.maxTemperatureC(),
                "Insulated steam pipe should have the highest thermal rating"
        );
    }

    private static void rotaryWaterLiftModesStayConservative() {
        require(
                RotaryLiftRules.supportsWaterLift(
                        PipeCatalog.SMALL_COPPER
                )
                        && RotaryLiftRules.supportsWaterLift(
                        PipeCatalog.IRON_WATER
                )
                        && RotaryLiftRules.supportsWaterLift(
                        PipeCatalog.LARGE_WATER_MAIN
                )
                        && RotaryLiftRules.supportsWaterLift(
                        PipeCatalog.STEEL_PRESSURE
                ),
                "Every liquid-capable ordinary pipe family may have a rotary water-lift head"
        );

        require(
                !RotaryLiftRules.supportsWaterLift(
                        PipeCatalog.THIN_GAS
                )
                        && !RotaryLiftRules.supportsWaterLift(
                        PipeCatalog.INSULATED_STEAM
                ),
                "Gas/steam-only families must not silently become water pipes"
        );

        require(
                RotaryLiftRules.mode(
                        PipeCatalog.LARGE_WATER_MAIN
                ) == RotaryLiftRules.TransferMode.PHYSICAL,
                "Large Water Main is the real-volume rotary transfer family"
        );

        require(
                RotaryLiftRules.mode(
                        PipeCatalog.SMALL_COPPER
                ) == RotaryLiftRules.TransferMode.VISUAL
                        && RotaryLiftRules.mode(
                        PipeCatalog.IRON_WATER
                ) == RotaryLiftRules.TransferMode.VISUAL
                        && RotaryLiftRules.mode(
                        PipeCatalog.STEEL_PRESSURE
                ) == RotaryLiftRules.TransferMode.VISUAL,
                "Narrow rotary lift families remain particle-only and never duplicate/drain world water"
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
