package net.caravidro.wayaround.industrial.client;

import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.animation.SmoothObjectAnimation;
import net.caravidro.wayaround.industrial.power.MechanicalMillBlock;
import net.caravidro.wayaround.industrial.power.MechanicalMillBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class MechanicalMillRenderer
        implements BlockEntityRenderer<MechanicalMillBlockEntity> {

    private static final Map<
            MechanicalMillBlockEntity,
            SmoothObjectAnimation.Rotation
            > ROTATIONS =
            new WeakHashMap<>();

    private final BlockRenderDispatcher blockRenderer;

    public MechanicalMillRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        blockRenderer =
                context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            MechanicalMillBlockEntity mill,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        double renderTime =
                mill.getLevel() == null
                        ? 0.0
                        : mill.getLevel()
                                .getGameTime()
                                + partialTick;

        SmoothObjectAnimation.Rotation rotation =
                ROTATIONS.computeIfAbsent(
                        mill,
                        key -> new SmoothObjectAnimation.Rotation(
                                mill.rotationDegrees()
                        )
                );

        float visualAngle =
                rotation.update(
                        renderTime,
                        mill.rpm(),
                        mill.rotationDegrees()
                );

        poseStack.pushPose();
        poseStack.translate(
                0.5,
                0.5,
                0.5
        );

        Direction facing =
                mill.getBlockState()
                        .getValue(
                                MechanicalMillBlock.FACING
                        );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        yawFor(
                                facing
                        )
                )
        );

        renderFrame(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        renderLowerStone(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        renderUpperStone(
                visualAngle,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        renderDrive(
                visualAngle,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        renderHopper(
                mill.hasInput(),
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        if (mill.hasOutput()) {
            renderFlourTray(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay
            );
        }

        poseStack.popPose();
    }

    private void renderFrame(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            int overlay
    ) {
        BlockState wood =
                Blocks.STRIPPED_SPRUCE_LOG
                        .defaultBlockState();

        BlockState planks =
                Blocks.SPRUCE_PLANKS
                        .defaultBlockState();

        renderCuboid(
                poseStack, bufferSource, light, overlay,
                planks,
                0.0, -0.38, 0.0,
                0.92, 0.10, 0.92
        );

        for (double x :
                new double[] {
                        -0.34,
                        0.34
                }) {

            for (double z :
                    new double[] {
                            -0.34,
                            0.34
                    }) {

                renderCuboid(
                        poseStack, bufferSource, light, overlay,
                        wood,
                        x, -0.16, z,
                        0.11, 0.46, 0.11
                );
            }
        }

        renderCuboid(
                poseStack, bufferSource, light, overlay,
                planks,
                0.0, 0.09, 0.0,
                0.84, 0.08, 0.84
        );
    }

    private void renderLowerStone(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            int overlay
    ) {
        renderStoneDisc(
                0.17,
                0.0F,
                Blocks.STONE.defaultBlockState(),
                poseStack,
                bufferSource,
                light,
                overlay
        );
    }

    private void renderUpperStone(
            float angle,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            int overlay
    ) {
        poseStack.pushPose();
        poseStack.translate(
                0.0,
                0.29,
                0.0
        );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        angle
                )
        );

        renderStoneDisc(
                0.0,
                angle,
                Blocks.SMOOTH_STONE.defaultBlockState(),
                poseStack,
                bufferSource,
                light,
                overlay
        );

        poseStack.popPose();
    }

    private void renderStoneDisc(
            double y,
            float angle,
            BlockState stone,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            int overlay
    ) {
        poseStack.pushPose();
        poseStack.translate(
                0.0,
                y,
                0.0
        );

        for (int index = 0;
             index < 12;
             index++) {

            double a =
                    Math.PI * 2.0
                            * index
                            / 12.0;

            double x =
                    Math.cos(a)
                            * 0.25;

            double z =
                    Math.sin(a)
                            * 0.25;

            renderCuboid(
                    poseStack,
                    bufferSource,
                    light,
                    overlay,
                    stone,
                    x,
                    0.0,
                    z,
                    0.29,
                    0.13,
                    0.18
            );
        }

        renderCuboid(
                poseStack,
                bufferSource,
                light,
                overlay,
                stone,
                0.0,
                0.0,
                0.0,
                0.48,
                0.13,
                0.48
        );

        poseStack.popPose();
    }

    private void renderDrive(
            float angle,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            int overlay
    ) {
        renderCuboid(
                poseStack,
                bufferSource,
                light,
                overlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                0.0,
                0.38,
                0.0,
                0.10,
                0.70,
                0.10
        );

        poseStack.pushPose();
        poseStack.translate(
                0.0,
                0.49,
                0.0
        );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        angle
                )
        );

        for (int index = 0;
             index < 4;
             index++) {

            poseStack.pushPose();

            poseStack.mulPose(
                    Axis.YP.rotationDegrees(
                            index * 90.0F
                    )
            );

            renderCuboid(
                    poseStack,
                    bufferSource,
                    light,
                    overlay,
                    Blocks.COPPER_BLOCK.defaultBlockState(),
                    0.22,
                    0.0,
                    0.0,
                    0.34,
                    0.045,
                    0.055
            );

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private void renderHopper(
            boolean grain,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            int overlay
    ) {
        BlockState wood =
                Blocks.OAK_PLANKS
                        .defaultBlockState();

        renderCuboid(
                poseStack,
                bufferSource,
                light,
                overlay,
                wood,
                0.0,
                0.78,
                0.0,
                0.56,
                0.08,
                0.56
        );

        renderCuboid(
                poseStack,
                bufferSource,
                light,
                overlay,
                wood,
                -0.27,
                0.63,
                0.0,
                0.07,
                0.34,
                0.50
        );

        renderCuboid(
                poseStack,
                bufferSource,
                light,
                overlay,
                wood,
                0.27,
                0.63,
                0.0,
                0.07,
                0.34,
                0.50
        );

        renderCuboid(
                poseStack,
                bufferSource,
                light,
                overlay,
                wood,
                0.0,
                0.63,
                -0.27,
                0.50,
                0.34,
                0.07
        );

        renderCuboid(
                poseStack,
                bufferSource,
                light,
                overlay,
                wood,
                0.0,
                0.63,
                0.27,
                0.50,
                0.34,
                0.07
        );

        if (grain) {
            renderCuboid(
                    poseStack,
                    bufferSource,
                    light,
                    overlay,
                    Blocks.HAY_BLOCK.defaultBlockState(),
                    0.0,
                    0.68,
                    0.0,
                    0.40,
                    0.06,
                    0.40
            );
        }
    }

    private void renderFlourTray(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            int overlay
    ) {
        renderCuboid(
                poseStack,
                bufferSource,
                light,
                overlay,
                Blocks.OAK_SLAB.defaultBlockState(),
                0.0,
                -0.29,
                -0.48,
                0.64,
                0.08,
                0.26
        );

        renderCuboid(
                poseStack,
                bufferSource,
                light,
                overlay,
                Blocks.WHITE_CONCRETE_POWDER.defaultBlockState(),
                0.0,
                -0.23,
                -0.48,
                0.48,
                0.045,
                0.17
        );
    }

    private void renderCuboid(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            int overlay,
            BlockState material,
            double x,
            double y,
            double z,
            double sx,
            double sy,
            double sz
    ) {
        poseStack.pushPose();

        poseStack.translate(
                x,
                y,
                z
        );

        poseStack.scale(
                (float) sx,
                (float) sy,
                (float) sz
        );

        poseStack.translate(
                -0.5,
                -0.5,
                -0.5
        );

        blockRenderer.renderSingleBlock(
                material,
                poseStack,
                bufferSource,
                light,
                overlay
        );

        poseStack.popPose();
    }

    private static float yawFor(
            Direction facing
    ) {
        return switch (facing) {
            case EAST -> -90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
