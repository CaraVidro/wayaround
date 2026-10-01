package net.caravidro.wayaround.industrial.grid;

import net.caravidro.wayaround.worldgen.geography.AntarcticField;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("wayaround_crushing")
@PrefixGameTestTemplate(false)
public final class GridGameTests {

    private GridGameTests() {
    }

    @GameTest(template="assembly_test",batch="grid",timeoutTicks=80)
    public static void highVoltageDistanceCostsEnergy(GameTestHelper helper) {
        float shortLine =
                GridPhysics.highVoltageEfficiency(
                        4,
                        0.96F
                );

        float longLine =
                GridPhysics.highVoltageEfficiency(
                        180,
                        0.96F
                );

        helper.assertTrue(
                shortLine > longLine,
                "Long transmission must lose more energy than a short run"
        );

        helper.assertTrue(
                longLine >= 0.50F
                        && shortLine <= 1.0F,
                "Transmission efficiency must remain physically bounded"
        );

        helper.succeed();
    }

    @GameTest(template="assembly_test",batch="grid",timeoutTicks=80)
    public static void transformerCannotCreateEnergy(GameTestHelper helper) {
        int raw =
                1_000;

        int converted =
                GridPhysics.converted(
                        raw,
                        0.96F
                );

        int cost =
                GridPhysics.rawForConverted(
                        converted,
                        0.96F
                );

        helper.assertTrue(
                converted < raw,
                "Transformer conversion must have a real loss"
        );

        helper.assertTrue(
                cost <= raw
                        && cost >= converted,
                "Recovering the converted amount cannot require less raw energy than the output"
        );

        helper.succeed();
    }

    @GameTest(template="assembly_test",batch="grid",timeoutTicks=80)
    public static void fuseOnlyStressesAboveItsRating(GameTestHelper helper) {
        float normal =
                GridPhysics.overloadRatio(
                        900,
                        960
                );

        float overload =
                GridPhysics.overloadRatio(
                        1_280,
                        960
                );

        helper.assertTrue(
                normal == 0.0F,
                "A protected circuit must tolerate load below its rating"
        );

        helper.assertTrue(
                overload > 0.0F,
                "A pulse above the rating must create overload stress"
        );

        helper.succeed();
    }

    @GameTest(template="assembly_test",batch="grid",timeoutTicks=80)
    public static void electricMotorReturnsToMechanicalLanguage(GameTestHelper helper) {
        float slow =
                GridPhysics.mechanicalPower(
                        30.0F,
                        1.2F
                );

        float working =
                GridPhysics.mechanicalPower(
                        84.0F,
                        2.8F
                );

        helper.assertTrue(
                slow > 0.0F,
                "An energized motor must expose usable mechanical power"
        );

        helper.assertTrue(
                working > slow,
                "More RPM and torque must produce more mechanical power"
        );

        helper.succeed();
    }

    @GameTest(template="assembly_test",batch="polar",timeoutTicks=80)
    public static void polarAmbienceStartsBeforeAntarctica(GameTestHelper helper) {
        double north =
                AntarcticField.polarInfluence(
                        0,
                        11_000
                );

        double southernOcean =
                AntarcticField.polarInfluence(
                        0,
                        17_000
                );

        double coast =
                AntarcticField.polarInfluence(
                        0,
                        25_000
                );

        double continent =
                AntarcticField.polarInfluence(
                        0,
                        33_000
                );

        helper.assertTrue(
                southernOcean > north,
                "Polar ambience must begin while crossing the Southern Ocean"
        );

        helper.assertTrue(
                coast > southernOcean,
                "The approach to Antarctica must intensify gradually"
        );

        helper.assertTrue(
                continent > coast
                        && continent > 0.95,
                "Deep Antarctica must reach nearly full polar influence"
        );

        helper.succeed();
    }
}
