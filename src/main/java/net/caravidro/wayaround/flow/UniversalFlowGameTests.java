package net.caravidro.wayaround.flow;

import net.caravidro.wayaround.industrial.pipework.PipeCatalog;
import net.caravidro.wayaround.physical.MatterPhase;
import net.caravidro.wayaround.physical.PhysicalMaterials;
import net.caravidro.wayaround.physical.PhysicalOpening;
import net.caravidro.wayaround.pressure.PressureState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Regression coverage for the universal flow vocabulary and adapters.
 */
@GameTestHolder("wayaround_nature")
@PrefixGameTestTemplate(false)
public final class UniversalFlowGameTests {

    private UniversalFlowGameTests() {
    }

    @GameTest(
            template = "assembly_test",
            batch = "universal_flow",
            timeoutTicks = 100
    )
    public static void pressureGradientFlowsHighToLow(
            GameTestHelper helper
    ) {
        PhysicalOpening opening =
                new PhysicalOpening(
                        BlockPos.ZERO,
                        BlockPos.ZERO.east(),
                        Direction.EAST,
                        PhysicalOpening.Kind.ATMOSPHERE,
                        1.0
                );

        PressureState high =
                new PressureState(
                        120.0,
                        101.325,
                        0.0,
                        PhysicalMaterials.AIR,
                        PressureState.Source.GAS
                );

        PressureState low =
                new PressureState(
                        100.0,
                        101.325,
                        0.0,
                        PhysicalMaterials.AIR,
                        PressureState.Source.ATMOSPHERE
                );

        FlowState outward =
                OpeningFlow.between(
                        PhysicalMaterials.AIR,
                        MatterPhase.GAS,
                        high,
                        low,
                        opening
                );

        FlowState inward =
                OpeningFlow.between(
                        PhysicalMaterials.AIR,
                        MatterPhase.GAS,
                        low,
                        high,
                        opening
                );

        helper.assertTrue(
                outward.volumetricRateM3PerS() > 0.0
                        && outward.velocityMPerS().x > 0.0,
                "Higher inside pressure must flow outward through the east opening"
        );

        helper.assertTrue(
                inward.volumetricRateM3PerS() > 0.0
                        && inward.velocityMPerS().x < 0.0,
                "Reversing delta-P must reverse the same opening flow"
        );

        helper.succeed();
    }

    @GameTest(
            template = "assembly_test",
            batch = "universal_flow",
            timeoutTicks = 100
    )
    public static void minecraftFluidRateHasPhysicalUnits(
            GameTestHelper helper
    ) {
        double rate =
                FlowMath.minecraftFluidRateM3PerS(
                        500.0
                );

        helper.assertTrue(
                Math.abs(
                        rate - 10.0
                ) < 1.0E-9,
                "500 mB/t must map to 10 m3/s with 1000 mB = 1 m3"
        );

        helper.assertTrue(
                Math.abs(
                        FlowMath.minecraftFluidRateMbPerTick(
                                rate
                        ) - 500.0
                ) < 1.0E-9,
                "Legacy fluid-rate conversion must round-trip"
        );

        helper.succeed();
    }

    @GameTest(
            template = "assembly_test",
            batch = "universal_flow",
            timeoutTicks = 100
    )
    public static void dynamicPressureScalesWithVelocitySquared(
            GameTestHelper helper
    ) {
        double slow =
                FlowMath.dynamicPressureKPa(
                        1000.0,
                        2.0
                );

        double fast =
                FlowMath.dynamicPressureKPa(
                        1000.0,
                        4.0
                );

        helper.assertTrue(
                Math.abs(
                        fast / slow - 4.0
                ) < 1.0E-9,
                "Doubling flow velocity must quadruple dynamic pressure"
        );

        helper.succeed();
    }

    @GameTest(
            template = "assembly_test",
            batch = "universal_flow",
            timeoutTicks = 100
    )
    public static void pipeGeometryTurnsThroughputIntoVelocity(
            GameTestHelper helper
    ) {
        FlowState flow =
                UniversalFlow.minecraftLiquidConduit(
                        PhysicalMaterials.WATER,
                        new Vec3(
                                1.0,
                                0.0,
                                0.0
                        ),
                        250.0,
                        500.0,
                        PipeCatalog.SMALL_COPPER.internalCrossSectionM2(),
                        0.15
                );

        helper.assertTrue(
                Math.abs(
                        flow.volumetricRateM3PerS() - 10.0
                ) < 1.0E-9,
                "Conduit adapter must preserve actual transferred volume"
        );

        helper.assertTrue(
                flow.velocityMPerS().x > 0.0
                        && flow.speedMPerS() > 0.0,
                "Pipe cross-section must turn throughput into directed velocity"
        );

        helper.assertTrue(
                flow.massRateKgPerS() > 9_000.0,
                "Water mass transport must derive from density times volume rate"
        );

        helper.succeed();
    }

    @GameTest(
            template = "assembly_test",
            batch = "universal_flow",
            timeoutTicks = 100
    )
    public static void vanillaWaterUsesUniversalOpenWaterState(
            GameTestHelper helper
    ) {
        BlockPos pos =
                helper.absolutePos(
                        new BlockPos(
                                4,
                                4,
                                4
                        )
                );

        helper.getLevel()
                .setBlock(
                        pos,
                        Blocks.WATER.defaultBlockState(),
                        18
                );

        FlowState flow =
                UniversalFlow.waterAt(
                        helper.getLevel(),
                        pos
                );

        helper.assertTrue(
                flow.phase()
                        == MatterPhase.LIQUID,
                "Vanilla water must enter universal flow as liquid matter"
        );

        helper.assertTrue(
                flow.source()
                        == FlowState.Source.OPEN_WATER,
                "Vanilla water must use the open-water flow adapter"
        );

        helper.assertTrue(
                flow.absolutePressureKPa()
                        > 0.0,
                "Open-water flow must carry a pressure state"
        );

        helper.succeed();
    }

    @GameTest(
            template = "assembly_test",
            batch = "universal_flow",
            timeoutTicks = 100
    )
    public static void explicitDensityControlsTransportedMass(
            GameTestHelper helper
    ) {
        FlowState compressedGas =
                new FlowState(
                        PhysicalMaterials.AIR,
                        MatterPhase.GAS,
                        new Vec3(
                                3.0,
                                0.0,
                                0.0
                        ),
                        4.0,
                        400.0,
                        2.5,
                        0.1,
                        FlowState.Source.CONDUIT_LIQUID
                );

        helper.assertTrue(
                Math.abs(
                        compressedGas.massRateKgPerS() - 10.0
                ) < 1.0E-9,
                "Flow mass rate must use runtime density, not the material reference density"
        );

        helper.succeed();
    }
}
