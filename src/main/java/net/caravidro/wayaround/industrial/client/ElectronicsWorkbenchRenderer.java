package net.caravidro.wayaround.industrial.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.caravidro.wayaround.industrial.electronics.CircuitBoardData;
import net.caravidro.wayaround.industrial.electronics.ElectronicsWorkbenchBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class ElectronicsWorkbenchRenderer
        implements BlockEntityRenderer<ElectronicsWorkbenchBlockEntity> {

    private static final double BOARD_WIDTH = 0.70;
    private static final double BOARD_DEPTH = 0.48;
    private static final double BOARD_Y = 0.780;

    private final BlockRenderDispatcher blockRenderer;

    public ElectronicsWorkbenchRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        blockRenderer =
                context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            ElectronicsWorkbenchBlockEntity workbench,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int renderLight,
            int packedOverlay
    ) {
        if (!workbench.hasCircuitBoard()) {
            return;
        }

        int renderLight =
                workbench.getLevel() == null
                        ? renderLight
                        : IndustrialRenderUtil.exteriorLight(
                        workbench.getLevel(),
                        workbench.getBlockPos(),
                        renderLight
                );

        poseStack.pushPose();

        poseStack.translate(
                0.5,
                0.0,
                0.5
        );

        IndustrialRenderUtil.cuboid(
                blockRenderer,
                poseStack,
                bufferSource,
                renderLight,
                packedOverlay,
                Blocks.GREEN_CONCRETE.defaultBlockState(),
                0.0,
                BOARD_Y,
                0.0,
                BOARD_WIDTH,
                0.025,
                BOARD_DEPTH
        );

        CircuitBoardData board =
                workbench.boardData();

        for (CircuitBoardData.Trace trace :
                board.traceEdges()) {
            renderTrace(
                    poseStack,
                    bufferSource,
                    renderLight,
                    packedOverlay,
                    trace
            );
        }

        for (int cell = 0;
             cell < CircuitBoardData.CELL_COUNT;
             cell++) {

            CircuitBoardData.ComponentType type =
                    board.component(
                            cell
                    );

            if (type == CircuitBoardData.ComponentType.EMPTY) {
                continue;
            }

            renderComponent(
                    poseStack,
                    bufferSource,
                    renderLight,
                    packedOverlay,
                    cell,
                    type
            );
        }

        poseStack.popPose();
    }

    private void renderTrace(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            CircuitBoardData.Trace trace
    ) {
        double ax =
                cellX(
                        trace.a()
                );

        double az =
                cellZ(
                        trace.a()
                );

        double bx =
                cellX(
                        trace.b()
                );

        double bz =
                cellZ(
                        trace.b()
                );

        double dx =
                bx - ax;

        double dz =
                bz - az;

        double length =
                Math.sqrt(
                        dx * dx
                                + dz * dz
                );

        float rotationY =
                (float) -Math.toDegrees(
                        Math.atan2(
                                dz,
                                dx
                        )
                );

        IndustrialRenderUtil.cuboid(
                blockRenderer,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.COPPER_BLOCK.defaultBlockState(),
                (ax + bx) * 0.5,
                BOARD_Y + 0.020,
                (az + bz) * 0.5,
                length,
                0.010,
                0.014,
                0.0F,
                rotationY,
                0.0F
        );
    }

    private void renderComponent(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            int cell,
            CircuitBoardData.ComponentType type
    ) {
        double x =
                cellX(
                        cell
                );

        double z =
                cellZ(
                        cell
                );

        double y =
                BOARD_Y + 0.045;

        /*
         * Every installed part sits on a visible copper pad. The GUI's
         * connection square therefore represents a real solder/contact point
         * rather than an abstract graph handle.
         */
        part(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.COPPER_BLOCK.defaultBlockState(),
                x,
                BOARD_Y + 0.024,
                z,
                0.042,
                0.009,
                0.042
        );

        switch (type) {
            case INPUT_TERMINAL -> renderTerminal(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    x,
                    y,
                    z,
                    Blocks.COPPER_BLOCK.defaultBlockState()
            );

            case OUTPUT_TERMINAL -> renderTerminal(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    x,
                    y,
                    z,
                    Blocks.IRON_BLOCK.defaultBlockState()
            );

            case RESISTOR -> {
                part(
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay,
                        Blocks.COPPER_BLOCK.defaultBlockState(),
                        x,
                        y,
                        z,
                        0.12,
                        0.012,
                        0.012
                );

                part(
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay,
                        Blocks.BROWN_TERRACOTTA.defaultBlockState(),
                        x,
                        y + 0.016,
                        z,
                        0.065,
                        0.035,
                        0.035
                );

                part(
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay,
                        Blocks.RED_TERRACOTTA.defaultBlockState(),
                        x + 0.012,
                        y + 0.018,
                        z,
                        0.010,
                        0.040,
                        0.040
                );
            }

            case CAPACITOR -> {
                part(
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay,
                        Blocks.COPPER_BLOCK.defaultBlockState(),
                        x - 0.022,
                        y - 0.005,
                        z,
                        0.010,
                        0.075,
                        0.010
                );

                part(
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay,
                        Blocks.COPPER_BLOCK.defaultBlockState(),
                        x + 0.022,
                        y - 0.005,
                        z,
                        0.010,
                        0.075,
                        0.010
                );

                part(
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay,
                        Blocks.CYAN_TERRACOTTA.defaultBlockState(),
                        x,
                        y + 0.035,
                        z,
                        0.065,
                        0.085,
                        0.055
                );
            }

            case DIODE -> {
                part(
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay,
                        Blocks.COPPER_BLOCK.defaultBlockState(),
                        x,
                        y,
                        z,
                        0.12,
                        0.012,
                        0.012
                );

                part(
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay,
                        Blocks.BLACK_CONCRETE.defaultBlockState(),
                        x,
                        y + 0.015,
                        z,
                        0.060,
                        0.030,
                        0.030
                );

                part(
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay,
                        Blocks.QUARTZ_BLOCK.defaultBlockState(),
                        x + 0.020,
                        y + 0.016,
                        z,
                        0.009,
                        0.033,
                        0.033
                );
            }

            case TRANSISTOR -> {
                for (int index = -1;
                     index <= 1;
                     index++) {
                    part(
                            poseStack,
                            bufferSource,
                            packedLight,
                            packedOverlay,
                            Blocks.COPPER_BLOCK.defaultBlockState(),
                            x + index * 0.026,
                            y - 0.003,
                            z + 0.018,
                            0.008,
                            0.065,
                            0.008
                    );
                }

                part(
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay,
                        Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                        x,
                        y + 0.037,
                        z - 0.005,
                        0.075,
                        0.070,
                        0.050
                );
            }

            case RELAY -> {
                part(
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay,
                        Blocks.IRON_BLOCK.defaultBlockState(),
                        x,
                        y + 0.030,
                        z,
                        0.095,
                        0.075,
                        0.070
                );

                part(
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay,
                        Blocks.COPPER_BLOCK.defaultBlockState(),
                        x,
                        y + 0.071,
                        z,
                        0.072,
                        0.012,
                        0.050
                );
            }

            case LED -> {
                part(
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay,
                        Blocks.COPPER_BLOCK.defaultBlockState(),
                        x,
                        y,
                        z,
                        0.060,
                        0.012,
                        0.030
                );

                part(
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay,
                        Blocks.AMETHYST_BLOCK.defaultBlockState(),
                        x,
                        y + 0.035,
                        z,
                        0.040,
                        0.060,
                        0.040
                );

                part(
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay,
                        Blocks.GLASS.defaultBlockState(),
                        x,
                        y + 0.038,
                        z,
                        0.057,
                        0.066,
                        0.057
                );
            }

            case BUZZER -> {
                part(
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay,
                        Blocks.IRON_BLOCK.defaultBlockState(),
                        x,
                        y + 0.025,
                        z,
                        0.100,
                        0.050,
                        0.085
                );

                part(
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay,
                        Blocks.NOTE_BLOCK.defaultBlockState(),
                        x,
                        y + 0.055,
                        z,
                        0.072,
                        0.035,
                        0.060
                );
            }

            case DISTANCE_DETECTOR -> {
                part(
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay,
                        Blocks.IRON_BLOCK.defaultBlockState(),
                        x,
                        y + 0.026,
                        z,
                        0.095,
                        0.052,
                        0.075
                );

                part(
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay,
                        Blocks.AMETHYST_BLOCK.defaultBlockState(),
                        x,
                        y + 0.055,
                        z - 0.028,
                        0.036,
                        0.036,
                        0.030
                );

                part(
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay,
                        Blocks.GLASS.defaultBlockState(),
                        x,
                        y + 0.055,
                        z - 0.046,
                        0.050,
                        0.050,
                        0.018
                );
            }

            case EMPTY -> {
            }
        }
    }

    private void renderTerminal(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            double x,
            double y,
            double z,
            BlockState material
    ) {
        part(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                x,
                y,
                z,
                0.060,
                0.015,
                0.060
        );

        part(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                material,
                x,
                y + 0.045,
                z,
                0.032,
                0.080,
                0.032
        );
    }

    private void part(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            BlockState material,
            double x,
            double y,
            double z,
            double sx,
            double sy,
            double sz
    ) {
        IndustrialRenderUtil.cuboid(
                blockRenderer,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                material,
                x,
                y,
                z,
                sx,
                sy,
                sz
        );
    }

    private static double cellX(
            int cell
    ) {
        return -BOARD_WIDTH * 0.5
                + (
                CircuitBoardData.cellX(
                        cell
                )
                        + 0.5
        )
                * (
                BOARD_WIDTH
                        / CircuitBoardData.WIDTH
        );
    }

    private static double cellZ(
            int cell
    ) {
        return -BOARD_DEPTH * 0.5
                + (
                CircuitBoardData.cellY(
                        cell
                )
                        + 0.5
        )
                * (
                BOARD_DEPTH
                        / CircuitBoardData.HEIGHT
        );
    }

    @Override
    public int getViewDistance() {
        return 72;
    }
}
