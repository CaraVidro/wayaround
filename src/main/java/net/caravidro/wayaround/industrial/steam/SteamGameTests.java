package net.caravidro.wayaround.industrial.steam;

import net.caravidro.wayaround.industrial.pipework.PipeCatalog;
import net.caravidro.wayaround.industrial.pipework.PipeSpec;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("wayaround_crushing")
@PrefixGameTestTemplate(false)
public final class SteamGameTests {

    private SteamGameTests() {
    }

    @GameTest(template="assembly_test",batch="steam",timeoutTicks=80)
    public static void boilerNeedsRealWaterToCreateSteam(GameTestHelper helper) {
        SteamThermodynamics.BoilerStep dry =
                SteamThermodynamics.tickSecond(
                        0,
                        0,
                        100,
                        0,
                        true,
                        1.0F,
                        1.0F
                );

        helper.assertTrue(
                dry.steam() == 0,
                "Coal and heat alone cannot manufacture steam without water"
        );

        SteamThermodynamics.BoilerStep wet =
                SteamThermodynamics.tickSecond(
                        1_000,
                        SteamThermodynamics.FUEL_TICKS_PER_COAL,
                        90,
                        0,
                        false,
                        1.0F,
                        1.0F
                );

        helper.assertTrue(
                wet.steam() > 0
                        && wet.water() < 1_000,
                "Boiling must transfer a real water budget into a steam budget"
        );

        helper.succeed();
    }

    @GameTest(template="assembly_test",batch="steam",timeoutTicks=80)
    public static void pressureAndSafetyDependOnPhysicalState(GameTestHelper helper) {
        float low =
                SteamThermodynamics.pressureBar(
                        1_000,
                        55
                );

        float high =
                SteamThermodynamics.pressureBar(
                        9_000,
                        95
                );

        float sameMassCold =
                SteamThermodynamics.pressureBar(
                        6_000,
                        60
                );

        float sameMassHot =
                SteamThermodynamics.pressureBar(
                        6_000,
                        95
                );

        helper.assertTrue(
                high > low,
                "More hot steam must produce greater pressure"
        );

        helper.assertTrue(
                sameMassHot > sameMassCold,
                "At fixed steam mass/volume, higher Celsius must raise gas pressure"
        );

        float healthy =
                SteamThermodynamics.safePressureBar(
                        1.0F,
                        1.0F
                );

        float abused =
                SteamThermodynamics.safePressureBar(
                        0.25F,
                        0.35F
                );

        helper.assertTrue(
                abused < healthy,
                "A damaged vessel/valve must reduce the safe pressure envelope"
        );

        helper.succeed();
    }

    @GameTest(template="assembly_test",batch="steam",timeoutTicks=80)
    public static void steamProducesRotationBeforeElectricity(GameTestHelper helper) {
        float rpm =
                SteamThermodynamics.engineRpm(
                        7.5F,
                        1_800
                );

        float torque =
                SteamThermodynamics.engineTorque(
                        7.5F,
                        210,
                        1_800
                );

        float mechanical =
                SteamThermodynamics.mechanicalPower(
                        rpm,
                        torque
                );

        helper.assertTrue(
                rpm > 0.0F
                        && torque > 0.0F
                        && mechanical > 0.0F,
                "Pressurized steam must become rotational RPM/torque, not direct FE"
        );

        helper.assertTrue(
                SteamThermodynamics.engineRpm(
                        0.0F,
                        1_800
                ) == 0.0F,
                "Stored steam without usable pressure cannot spin the engine"
        );

        helper.succeed();
    }

    @GameTest(template="assembly_test",batch="steam",timeoutTicks=80)
    public static void steamUsesRatedPipework(GameTestHelper helper) {
        helper.assertTrue(
                PipeCatalog.INSULATED_STEAM.supports(
                        PipeSpec.PipeMedium.STEAM
                ),
                "Insulated steam pipe must carry steam"
        );

        helper.assertTrue(
                PipeCatalog.STEEL_PRESSURE.supports(
                        PipeSpec.PipeMedium.STEAM
                ),
                "Rated steel pressure pipe must also be valid for steam"
        );

        helper.assertTrue(
                !PipeCatalog.SMALL_COPPER.supports(
                        PipeSpec.PipeMedium.STEAM
                ),
                "Ordinary small copper liquid pipe must not silently become a steam line"
        );

        helper.succeed();
    }
}
