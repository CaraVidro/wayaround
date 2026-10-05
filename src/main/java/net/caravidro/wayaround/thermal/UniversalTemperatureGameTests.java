package net.caravidro.wayaround.thermal;

import net.caravidro.wayaround.industrial.assembly.AssemblyPartProfile;
import net.caravidro.wayaround.industrial.material.MaterialMemory;
import net.caravidro.wayaround.physical.MatterPhase;
import net.caravidro.wayaround.physical.MatterState;
import net.caravidro.wayaround.physical.PhysicalBoundaryFace;
import net.caravidro.wayaround.physical.PhysicalMaterials;
import net.caravidro.wayaround.physical.PhysicalRegionScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Regression coverage for the universal thermal vocabulary and its vanilla
 * enclosure bridge.
 */
@GameTestHolder("wayaround_nature")
@PrefixGameTestTemplate(false)
public final class UniversalTemperatureGameTests {

    private UniversalTemperatureGameTests() {
    }

    @GameTest(
            template = "assembly_test",
            batch = "universal_temperature",
            timeoutTicks = 100
    )
    public static void matterHeatCapacityUsesPhysicalMaterial(
            GameTestHelper helper
    ) {
        MatterState water =
                new MatterState(
                        PhysicalMaterials.WATER,
                        MatterPhase.LIQUID,
                        1.0,
                        0.001,
                        20.0,
                        MatterState.STANDARD_PRESSURE_KPA
                );

        double energy =
                ThermalPhysics.energyForTemperatureChangeJ(
                        water,
                        30.0
                );

        helper.assertTrue(
                Math.abs(
                        energy
                                - 41_840.0
                ) < 0.01,
                "One kilogram of liquid water needs 41840 J for a 10 C rise"
        );

        helper.succeed();
    }

    @GameTest(
            template = "assembly_test",
            batch = "universal_temperature",
            timeoutTicks = 100
    )
    public static void stoneConductsMoreHeatThanWood(
            GameTestHelper helper
    ) {
        PhysicalBoundaryFace stone =
                new PhysicalBoundaryFace(
                        BlockPos.ZERO,
                        BlockPos.ZERO.east(),
                        Direction.EAST,
                        PhysicalMaterials.STONE,
                        1.0,
                        1.0
                );

        PhysicalBoundaryFace wood =
                new PhysicalBoundaryFace(
                        BlockPos.ZERO,
                        BlockPos.ZERO.east(),
                        Direction.EAST,
                        PhysicalMaterials.WOOD,
                        1.0,
                        1.0
                );

        helper.assertTrue(
                ThermalPhysics.boundaryConductanceWPerK(
                        stone
                )
                        > ThermalPhysics.boundaryConductanceWPerK(
                        wood
                ),
                "Stone wall must conduct heat faster than an equal wood wall"
        );

        helper.succeed();
    }

    @GameTest(
            template = "assembly_test",
            batch = "universal_temperature",
            timeoutTicks = 100
    )
    public static void openingRoomLosesHeatFasterThanSealedRoom(
            GameTestHelper helper
    ) {
        buildRoom(
                helper,
                false
        );

        BlockPos seed =
                helper.absolutePos(
                        new BlockPos(
                                4,
                                4,
                                4
                        )
                );

        var closedRegion =
                PhysicalRegionScanner.scan(
                        helper.getLevel(),
                        seed,
                        new PhysicalRegionScanner.ScanLimits(
                                256,
                                8
                        )
                ).orElseThrow();

        ThermalRegionModel closed =
                ThermalRegionModel.from(
                        closedRegion
                );

        buildRoom(
                helper,
                true
        );

        var openRegion =
                PhysicalRegionScanner.scan(
                        helper.getLevel(),
                        seed,
                        new PhysicalRegionScanner.ScanLimits(
                                256,
                                8
                        )
                ).orElseThrow();

        ThermalRegionModel open =
                ThermalRegionModel.from(
                        openRegion
                );

        helper.assertTrue(
                closed.openingConductanceWPerK()
                        == 0.0,
                "Closed room must have no opening heat exchange; closure="
                        + closedRegion.closure()
                        + " openings="
                        + closedRegion.openings().size()
        );

        helper.assertTrue(
                open.openingConductanceWPerK()
                        > 0.0,
                "Open vanilla gate must add ventilation heat exchange"
        );

        helper.assertTrue(
                open.relaxationTicks()
                        < closed.relaxationTicks(),
                "Vented room must return toward ambient faster"
        );

        helper.assertTrue(
                open.steadyTemperatureRiseC(
                        6_000.0
                )
                        < closed.steadyTemperatureRiseC(
                        6_000.0
                ),
                "Same campfire power must sustain a lower rise in a vented room"
        );

        helper.succeed();
    }

    @GameTest(
            template = "assembly_test",
            batch = "universal_temperature",
            timeoutTicks = 100
    )
    public static void materialMemoryAcceptsRealCelsius(
            GameTestHelper helper
    ) {
        MaterialMemory memory =
                MaterialMemory.fresh(
                        0L
                );

        memory.observeTemperatureC(
                AssemblyPartProfile.Material.STEEL,
                20.0
        );

        memory.observeTemperatureC(
                AssemblyPartProfile.Material.STEEL,
                1200.0
        );

        helper.assertTrue(
                memory.thermalCycles()
                        > 0,
                "A real hot-cold transition must register a thermal cycle"
        );

        helper.assertTrue(
                memory.heatDamage()
                        > 0.0F,
                "Severe Celsius exposure must feed existing material heat damage"
        );

        helper.succeed();
    }

    private static void buildRoom(
            GameTestHelper helper,
            boolean gateOpen
    ) {
        var level =
                helper.getLevel();

        for (int x = 2;
             x <= 6;
             x++) {
            for (int y = 2;
                 y <= 6;
                 y++) {
                for (int z = 2;
                     z <= 6;
                     z++) {
                    boolean shell =
                            x == 2
                                    || x == 6
                                    || y == 2
                                    || y == 6
                                    || z == 2
                                    || z == 6;

                    BlockPos pos =
                            helper.absolutePos(
                                    new BlockPos(
                                            x,
                                            y,
                                            z
                                    )
                            );

                    level.setBlock(
                            pos,
                            shell
                                    ? Blocks.STONE.defaultBlockState()
                                    : Blocks.AIR.defaultBlockState(),
                            18
                    );
                }
            }
        }

        level.setBlock(
                helper.absolutePos(
                        new BlockPos(
                                2,
                                4,
                                4
                        )
                ),
                Blocks.OAK_FENCE_GATE
                        .defaultBlockState()
                        .setValue(
                                BlockStateProperties.OPEN,
                                gateOpen
                        ),
                18
        );
    }
}
