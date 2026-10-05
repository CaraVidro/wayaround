package net.caravidro.wayaround.environment;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Regression tests for the regional environmental-field layer.
 */
@GameTestHolder("wayaround_nature")
@PrefixGameTestTemplate(false)
public final class EnvironmentalFieldGameTests {

    private EnvironmentalFieldGameTests() {
    }

    @GameTest(
            template = "assembly_test",
            batch = "environmental_fields",
            timeoutTicks = 100
    )
    public static void warmDryWaterEvaporatesMore(
            GameTestHelper helper
    ) {
        double warmDry =
                EnvironmentalFieldMath.evaporation(
                        1.0,
                        0.20,
                        30.0,
                        6.0
                );

        double coldHumid =
                EnvironmentalFieldMath.evaporation(
                        1.0,
                        0.90,
                        2.0,
                        1.0
                );

        helper.assertTrue(
                warmDry > coldHumid
                        && warmDry > 0.0,
                "Warm dry windy water must evaporate faster than cold humid water"
        );

        helper.succeed();
    }

    @GameTest(
            template = "assembly_test",
            batch = "environmental_fields",
            timeoutTicks = 100
    )
    public static void warmRainConsumesCloudWaterAndWetsSoil(
            GameTestHelper helper
    ) {
        var level =
                helper.getLevel();

        BlockPos pos =
                helper.absolutePos(
                        new BlockPos(
                                4,
                                4,
                                4
                        )
                );

        EnvironmentalFields.addHumidity(
                level,
                pos.getX(),
                pos.getZ(),
                0.0,
                1.0
        );

        var before =
                EnvironmentalFields.sample(
                        level,
                        pos
                );

        double precipitated =
                EnvironmentalFields.precipitate(
                        level,
                        pos.getX(),
                        pos.getZ(),
                        1.0,
                        20.0
                );

        var after =
                EnvironmentalFields.sample(
                        level,
                        pos
                );

        helper.assertTrue(
                precipitated > 0.0,
                "A saturated raining cell must precipitate"
        );

        helper.assertTrue(
                after.cloudWater()
                        < before.cloudWater(),
                "Rain must consume regional cloud water"
        );

        helper.assertTrue(
                after.soilMoisture()
                        > before.soilMoisture(),
                "Warm rain must increase regional soil moisture"
        );

        helper.succeed();
    }

    @GameTest(
            template = "assembly_test",
            batch = "environmental_fields",
            timeoutTicks = 100
    )
    public static void coldRainBecomesSnowBudgetWithoutPuddles(
            GameTestHelper helper
    ) {
        var level =
                helper.getLevel();

        BlockPos pos =
                helper.absolutePos(
                        new BlockPos(
                                4,
                                4,
                                4
                        )
                );

        level.setBlock(
                pos,
                Blocks.STONE.defaultBlockState(),
                18
        );

        EnvironmentalFields.addHumidity(
                level,
                pos.getX(),
                pos.getZ(),
                0.0,
                1.0
        );

        var before =
                EnvironmentalFields.sample(
                        level,
                        pos
                );

        EnvironmentalFields.precipitate(
                level,
                pos.getX(),
                pos.getZ(),
                1.0,
                -6.0
        );

        var after =
                EnvironmentalFields.sample(
                        level,
                        pos
                );

        helper.assertTrue(
                after.snowBudget()
                        > before.snowBudget(),
                "Cold precipitation must accumulate an abstract snow budget"
        );

        helper.assertTrue(
                level.getBlockState(
                        pos
                ).is(
                        Blocks.STONE
                ),
                "Regional precipitation must not materialize puddle/water blocks"
        );

        helper.succeed();
    }

    @GameTest(
            template = "assembly_test",
            batch = "environmental_fields",
            timeoutTicks = 100
    )
    public static void smokeLeavesRegionalAirMemory(
            GameTestHelper helper
    ) {
        var level =
                helper.getLevel();

        BlockPos pos =
                helper.absolutePos(
                        new BlockPos(
                                4,
                                4,
                                4
                        )
                );

        var before =
                EnvironmentalFields.sample(
                        level,
                        pos
                );

        EnvironmentalFields.emitSmoke(
                level,
                pos,
                0.05
        );

        var after =
                EnvironmentalFields.sample(
                        level,
                        pos
                );

        helper.assertTrue(
                after.smoke()
                        > before.smoke(),
                "Visible smoke must leave coarse regional smoke memory"
        );

        helper.assertTrue(
                after.pollution()
                        > before.pollution(),
                "Smoke must contribute to persistent regional pollution"
        );

        helper.succeed();
    }

    @GameTest(
            template = "assembly_test",
            batch = "environmental_fields",
            timeoutTicks = 100
    )
    public static void rainWashesPollution(
            GameTestHelper helper
    ) {
        double dryRetention =
                1.0
                        - EnvironmentalFieldMath.rainWashFraction(
                        0.0
                );

        double stormRetention =
                1.0
                        - EnvironmentalFieldMath.rainWashFraction(
                        1.0
                );

        helper.assertTrue(
                stormRetention
                        < dryRetention,
                "Heavy rain must wash air contamination faster than dry weather"
        );

        helper.succeed();
    }
}
