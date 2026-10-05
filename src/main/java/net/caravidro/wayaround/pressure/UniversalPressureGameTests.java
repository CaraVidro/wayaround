package net.caravidro.wayaround.pressure;

import net.caravidro.wayaround.physical.MatterPhase;
import net.caravidro.wayaround.physical.MatterState;
import net.caravidro.wayaround.physical.PhysicalMaterials;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Universal pressure regression tests using both pure physics and ordinary
 * vanilla water.
 */
@GameTestHolder("wayaround_nature")
@PrefixGameTestTemplate(false)
public final class UniversalPressureGameTests {

    private UniversalPressureGameTests() {
    }

    @GameTest(
            template = "assembly_test",
            batch = "universal_pressure",
            timeoutTicks = 100
    )
    public static void seawaterPressureComesFromDensityAndDepth(
            GameTestHelper helper
    ) {
        double gaugeKPa =
                PressurePhysics.hydrostaticGaugeKPa(
                        PhysicalMaterials.SALT_WATER,
                        100.0
                );

        double gaugeBar =
                PressureMath.kPaToBar(
                        gaugeKPa
                );

        helper.assertTrue(
                gaugeBar > 10.0
                        && gaugeBar < 10.1,
                "100 m of seawater must add about 10.05 bar; got "
                        + gaugeBar
        );

        helper.succeed();
    }

    @GameTest(
            template = "assembly_test",
            batch = "universal_pressure",
            timeoutTicks = 100
    )
    public static void vanillaWaterColumnProducesNaturalPressure(
            GameTestHelper helper
    ) {
        var level =
                helper.getLevel();

        for (int y = 2;
             y <= 6;
             y++) {
            level.setBlock(
                    helper.absolutePos(
                            new BlockPos(
                                    4,
                                    y,
                                    4
                            )
                    ),
                    Blocks.WATER.defaultBlockState(),
                    18
            );
        }

        BlockPos bottom =
                helper.absolutePos(
                        new BlockPos(
                                4,
                                2,
                                4
                        )
                );

        PressureState pressure =
                NaturalPressure.at(
                        level,
                        bottom
                );

        helper.assertTrue(
                pressure.source()
                        == PressureState.Source.HYDROSTATIC,
                "Vanilla water must resolve through hydrostatic pressure"
        );

        helper.assertTrue(
                pressure.depthM() > 4.0
                        && pressure.depthM() < 5.0,
                "The test water column should expose about 4.5 m depth; got "
                        + pressure.depthM()
        );

        helper.assertTrue(
                pressure.gaugeKPa() > 40.0
                        && pressure.gaugeKPa() < 50.0,
                "The vanilla water column must add physical pressure; got "
                        + pressure.gaugeKPa()
                        + " kPa"
        );

        helper.succeed();
    }

    @GameTest(
            template = "assembly_test",
            batch = "universal_pressure",
            timeoutTicks = 100
    )
    public static void heatingSealedGasRaisesPressure(
            GameTestHelper helper
    ) {
        MatterState air =
                MatterState.forVolume(
                        PhysicalMaterials.AIR,
                        MatterPhase.GAS,
                        1.0,
                        20.0,
                        MatterState.STANDARD_PRESSURE_KPA
                );

        MatterState hot =
                UniversalPressure.resolveGas(
                        air.withTemperature(
                                120.0
                        )
                );

        MatterState cold =
                UniversalPressure.resolveGas(
                        air
                );

        helper.assertTrue(
                hot.pressureKPa()
                        > cold.pressureKPa(),
                "Fixed gas mass and volume must gain pressure when heated"
        );

        helper.assertTrue(
                Math.abs(
                        cold.pressureKPa()
                                - PressureMath.STANDARD_ATMOSPHERE_KPA
                ) < 0.5,
                "Reference air state should remain near one atmosphere"
        );

        helper.succeed();
    }

    @GameTest(
            template = "assembly_test",
            batch = "universal_pressure",
            timeoutTicks = 100
    )
    public static void pressureDifferenceBecomesRealForce(
            GameTestHelper helper
    ) {
        double force =
                PressureMath.pressureForceN(
                        200.0,
                        1.0
                );

        helper.assertTrue(
                Math.abs(
                        force
                                - 200_000.0
                ) < 0.001,
                "200 kPa over 1 m2 must produce 200 kN of wall force"
        );

        helper.succeed();
    }
}
