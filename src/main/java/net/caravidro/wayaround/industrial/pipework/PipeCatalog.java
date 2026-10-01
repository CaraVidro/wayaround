package net.caravidro.wayaround.industrial.pipework;

import java.util.List;

import net.caravidro.wayaround.industrial.pipework.PipeSpec.PipeMedium;

public final class PipeCatalog {

    public static final PipeSpec SMALL_COPPER =
            new PipeSpec(
                    "small_copper_pipe",
                    "Small Copper Pipe",
                    0.105F,
                    240,
                    3.0F,
                    180,
                    PipeSpec.media(
                            PipeMedium.LIQUID
                    )
            );

    public static final PipeSpec IRON_WATER =
            new PipeSpec(
                    "iron_water_pipe",
                    "Iron Utility Pipe",
                    0.145F,
                    720,
                    6.0F,
                    220,
                    PipeSpec.media(
                            PipeMedium.LIQUID
                    )
            );

    public static final PipeSpec LARGE_WATER_MAIN =
            new PipeSpec(
                    "large_water_main",
                    "Large Water Main",
                    0.235F,
                    2400,
                    4.0F,
                    140,
                    PipeSpec.media(
                            PipeMedium.LIQUID
                    )
            );

    public static final PipeSpec THIN_GAS =
            new PipeSpec(
                    "thin_gas_pipe",
                    "Thin Gas Line",
                    0.080F,
                    420,
                    12.0F,
                    160,
                    PipeSpec.media(
                            PipeMedium.GAS
                    )
            );

    public static final PipeSpec STEEL_PRESSURE =
            new PipeSpec(
                    "steel_pressure_pipe",
                    "Steel Pressure Pipe",
                    0.150F,
                    980,
                    18.0F,
                    450,
                    PipeSpec.media(
                            PipeMedium.LIQUID,
                            PipeMedium.GAS,
                            PipeMedium.STEAM
                    )
            );

    public static final PipeSpec INSULATED_STEAM =
            new PipeSpec(
                    "insulated_steam_pipe",
                    "Insulated Steam Pipe",
                    0.175F,
                    760,
                    22.0F,
                    650,
                    PipeSpec.media(
                            PipeMedium.GAS,
                            PipeMedium.STEAM
                    )
            );

    public static final List<PipeSpec> ALL =
            List.of(
                    SMALL_COPPER,
                    IRON_WATER,
                    LARGE_WATER_MAIN,
                    THIN_GAS,
                    STEEL_PRESSURE,
                    INSULATED_STEAM
            );

    private PipeCatalog() {
    }
}
