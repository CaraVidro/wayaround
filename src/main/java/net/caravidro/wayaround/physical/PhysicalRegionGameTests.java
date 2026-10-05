package net.caravidro.wayaround.physical;

import net.caravidro.wayaround.industrial.pipework.PipeCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Vanilla geometry probes for physical regions.
 */
@GameTestHolder("wayaround_nature")
@PrefixGameTestTemplate(false)
public final class PhysicalRegionGameTests {

    private PhysicalRegionGameTests() {
    }

    @GameTest(
            template = "assembly_test",
            batch = "physical_region",
            timeoutTicks = 100
    )
    public static void closedVanillaRoomIsSealed(
            GameTestHelper helper
    ) {
        buildRoom(
                helper,
                false
        );

        var snapshot =
                PhysicalRegionScanner.scan(
                        helper.getLevel(),
                        helper.absolutePos(
                                new BlockPos(
                                        4,
                                        4,
                                        4
                                )
                        ),
                        new PhysicalRegionScanner.ScanLimits(
                                256,
                                8
                        )
                ).orElseThrow();

        helper.assertTrue(
                snapshot.closure()
                        == PhysicalRegionSnapshot.Closure.SEALED,
                "A closed vanilla stone room must resolve as sealed; got "
                        + snapshot.closure()
                        + " cells="
                        + snapshot.cells().size()
                        + " openings="
                        + snapshot.openings().size()
                        + " cellLimit="
                        + snapshot.hitCellLimit()
                        + " distanceLimit="
                        + snapshot.hitDistanceLimit()
                        + " unloaded="
                        + snapshot.touchedUnloadedChunk()
        );

        helper.assertTrue(
                Math.abs(
                        snapshot.volumeM3()
                                - 27.0
                ) < 0.001,
                "The 3x3x3 interior must expose 27 cubic metres of free volume"
        );

        helper.assertTrue(
                snapshot.openings()
                        .isEmpty(),
                "A sealed room must expose no atmospheric opening"
        );

        helper.succeed();
    }

    @GameTest(
            template = "assembly_test",
            batch = "physical_region",
            timeoutTicks = 100
    )
    public static void openingVanillaFenceGateVentsSameRoom(
            GameTestHelper helper
    ) {
        buildRoom(
                helper,
                true
        );

        var snapshot =
                PhysicalRegionScanner.scan(
                        helper.getLevel(),
                        helper.absolutePos(
                                new BlockPos(
                                        4,
                                        4,
                                        4
                                )
                        ),
                        new PhysicalRegionScanner.ScanLimits(
                                256,
                                8
                        )
                ).orElseThrow();

        helper.assertTrue(
                snapshot.closure()
                        == PhysicalRegionSnapshot.Closure.VENTED,
                "Opening the vanilla fence gate must vent the room"
        );

        helper.assertTrue(
                !snapshot.openings()
                        .isEmpty(),
                "The vented room must report an atmospheric frontier"
        );

        helper.succeed();
    }

    @GameTest(
            template = "assembly_test",
            batch = "physical_region",
            timeoutTicks = 100
    )
    public static void pipeRadiusNowDefinesRealInternalVolume(
            GameTestHelper helper
    ) {
        double small =
                PipeCatalog.SMALL_COPPER.internalVolumeM3(
                        1.0
                );

        double steel =
                PipeCatalog.STEEL_PRESSURE.internalVolumeM3(
                        1.0
                );

        helper.assertTrue(
                small > 0.0,
                "Pipe geometry must expose non-zero physical volume"
        );

        helper.assertTrue(
                steel > small,
                "A larger pipe radius must produce a larger internal volume"
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

        BlockPos gate =
                helper.absolutePos(
                        new BlockPos(
                                2,
                                4,
                                4
                        )
                );

        level.setBlock(
                gate,
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
