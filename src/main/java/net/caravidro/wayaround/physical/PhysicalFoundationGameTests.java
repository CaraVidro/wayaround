package net.caravidro.wayaround.physical;

import net.caravidro.wayaround.industrial.assembly.AssemblyPartProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Regression tests for the compatibility layer. These intentionally use
 * ordinary Minecraft blocks so the foundation cannot quietly become a
 * WayAround-only material database.
 */
@GameTestHolder("wayaround_nature")
@PrefixGameTestTemplate(false)
public final class PhysicalFoundationGameTests {

    private PhysicalFoundationGameTests() {
    }

    @GameTest(
            template = "assembly_test",
            batch = "physical_foundation",
            timeoutTicks = 80
    )
    public static void waterKeepsIdentityAcrossPhases(
            GameTestHelper helper
    ) {
        var liquid =
                BlockMatterResolver.resolve(
                        Blocks.WATER.defaultBlockState()
                );

        var ice =
                BlockMatterResolver.resolve(
                        Blocks.ICE.defaultBlockState()
                );

        helper.assertTrue(
                liquid.material()
                        == ice.material(),
                "Water and ice resolve to one material identity"
        );

        helper.assertTrue(
                liquid.phase()
                        == MatterPhase.LIQUID
                        && ice.phase()
                        == MatterPhase.SOLID,
                "Water and ice keep distinct physical phases"
        );

        helper.succeed();
    }

    @GameTest(
            template = "assembly_test",
            batch = "physical_foundation",
            timeoutTicks = 80
    )
    public static void vanillaSurfacesExposeDifferentFuelBehavior(
            GameTestHelper helper
    ) {
        float grass =
                BlockMatterResolver.exposedFlammability(
                        Blocks.GRASS_BLOCK.defaultBlockState()
                );

        float dirt =
                BlockMatterResolver.exposedFlammability(
                        Blocks.DIRT.defaultBlockState()
                );

        float oak =
                BlockMatterResolver.exposedFlammability(
                        Blocks.OAK_PLANKS.defaultBlockState()
                );

        helper.assertTrue(
                grass > dirt,
                "Grass exposes burnable biomass while bare dirt does not"
        );

        helper.assertTrue(
                oak > grass,
                "Wood remains a stronger fuel surface than grass"
        );

        helper.succeed();
    }

    @GameTest(
            template = "assembly_test",
            batch = "physical_foundation",
            timeoutTicks = 80
    )
    public static void assemblyUsesUniversalSteelDefinition(
            GameTestHelper helper
    ) {
        var steel =
                AssemblyPartProfile.Material.STEEL
                        .physicalMaterial();

        helper.assertTrue(
                steel == PhysicalMaterials.STEEL,
                "Assembly steel resolves through PhysicalMaterials"
        );

        helper.assertTrue(
                Math.abs(
                        steel.engineering()
                                .assemblyResistance()
                                - 0.96F
                ) < 0.0001F,
                "Legacy Assembly resistance is preserved during migration"
        );

        helper.succeed();
    }

    @GameTest(
            template = "assembly_test",
            batch = "physical_foundation",
            timeoutTicks = 80
    )
    public static void matterStateDerivesMassFromPhaseDensity(
            GameTestHelper helper
    ) {
        MatterState water =
                MatterState.forVolume(
                        PhysicalMaterials.WATER,
                        MatterPhase.LIQUID,
                        1.0,
                        20.0,
                        MatterState.STANDARD_PRESSURE_KPA
                );

        helper.assertTrue(
                Math.abs(
                        water.massKg()
                                - 997.0
                ) < 0.01,
                "One cubic metre of reference liquid water derives its mass from the material phase"
        );

        helper.succeed();
    }
}
