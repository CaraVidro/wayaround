package net.caravidro.wayaround.industrial.electronics;

import net.caravidro.wayaround.industrial.assembly.AssemblyPartProfile;
import net.caravidro.wayaround.industrial.material.MaterialMemory;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("wayaround_crushing")
@PrefixGameTestTemplate(false)
public final class ElectronicsGameTests {

    private ElectronicsGameTests() {
    }

    @GameTest(
            template = "assembly_test",
            batch = "electronics",
            timeoutTicks = 80
    )
    public static void circuitTopologySurvivesItemPersistence(
            GameTestHelper helper
    ) {
        ItemStack stack =
                new ItemStack(
                        ElectronicsContent.CIRCUIT_BOARD.get()
                );

        CircuitBoardData board =
                CircuitBoardData.empty();

        helper.assertTrue(
                board.place(
                        0,
                        CircuitBoardData.ComponentType.INPUT_TERMINAL
                ),
                "Input terminal must occupy a real board cell"
        );

        helper.assertTrue(
                board.place(
                        1,
                        CircuitBoardData.ComponentType.RESISTOR
                ),
                "Electronic parts must occupy independent cells"
        );

        helper.assertTrue(
                board.place(
                        2,
                        CircuitBoardData.ComponentType.OUTPUT_TERMINAL
                ),
                "Output terminal must occupy a real board cell"
        );

        helper.assertTrue(
                board.addTrace(
                        0,
                        1
                )
                        && board.addTrace(
                        1,
                        2
                ),
                "Copper traces form an explicit graph rather than a recipe flag"
        );

        board.write(
                stack
        );

        CircuitBoardData restored =
                CircuitBoardData.read(
                        stack
                );

        helper.assertTrue(
                restored.componentCount() == 3
                        && restored.traceCount() == 2,
                "Circuit components and copper traces persist on the physical board"
        );

        helper.assertTrue(
                restored.hasSignalPath(),
                "A restored board still knows that input and output are electrically connected"
        );

        helper.assertTrue(
                restored.powerDraw() >= 4,
                "Circuit complexity creates a real electrical operating cost"
        );

        helper.succeed();
    }

    @GameTest(
            template = "assembly_test",
            batch = "electronics",
            timeoutTicks = 80
    )
    public static void routedCopperCostDependsOnPhysicalDistance(
            GameTestHelper helper
    ) {
        int adjacent =
                CircuitBoardData.traceCopperCost(
                        CircuitBoardData.cell(0, 0),
                        CircuitBoardData.cell(1, 0)
                );

        int longRun =
                CircuitBoardData.traceCopperCost(
                        CircuitBoardData.cell(0, 0),
                        CircuitBoardData.cell(5, 3)
                );

        helper.assertTrue(
                adjacent > 0,
                "A real copper connection must consume at least one trace item"
        );

        helper.assertTrue(
                longRun > adjacent,
                "Longer PCB routing must consume more copper than neighboring pads"
        );

        CircuitBoardData board =
                CircuitBoardData.empty();

        board.place(
                CircuitBoardData.cell(0, 0),
                CircuitBoardData.ComponentType.INPUT_TERMINAL
        );

        board.place(
                CircuitBoardData.cell(5, 3),
                CircuitBoardData.ComponentType.OUTPUT_TERMINAL
        );

        board.addTrace(
                CircuitBoardData.cell(0, 0),
                CircuitBoardData.cell(5, 3)
        );

        helper.assertTrue(
                board.traceEdges().size() == 1
                        && board.copperCostAt(
                        CircuitBoardData.cell(0, 0)
                ) == longRun,
                "The visible physical trace graph must expose the same routed copper cost used by the editor"
        );

        helper.succeed();
    }

    @GameTest(
            template = "assembly_test",
            batch = "electronics",
            timeoutTicks = 80
    )
    public static void materialHistorySurvivesBeyondOrdinaryWear(
            GameTestHelper helper
    ) {
        MaterialMemory memory =
                MaterialMemory.fresh(
                        1200L
                );

        for (int cycle = 0;
             cycle < 160;
             cycle++) {

            memory.observeMechanicalUse(
                    AssemblyPartProfile.Material.COPPER,
                    1.45F,
                    0.52F,
                    cycle % 20 < 10
                            ? 0.92F
                            : 0.18F
            );
        }

        memory.exposeWet(
                AssemblyPartProfile.Material.COPPER,
                1.0F,
                true
        );

        float aged =
                memory.conditionFactor();

        MaterialMemory restored =
                MaterialMemory.load(
                        memory.save()
                );

        helper.assertTrue(
                restored.loadCycles() >= 160
                        && restored.thermalCycles() > 0,
                "Load and thermal cycling are remembered as history, not inferred from current HP"
        );

        helper.assertTrue(
                restored.corrosion() > 0.0F
                        && restored.deformation() > 0.0F,
                "Copper can remember environmental and mechanical abuse simultaneously"
        );

        helper.assertTrue(
                Math.abs(
                        restored.conditionFactor()
                                - aged
                ) < 0.0001F,
                "Material history survives serialization without becoming a fresh part"
        );

        helper.succeed();
    }
}
