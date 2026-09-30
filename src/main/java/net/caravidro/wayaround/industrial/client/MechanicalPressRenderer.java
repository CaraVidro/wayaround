package net.caravidro.wayaround.industrial.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.animation.ObjectAnimationPose;
import net.caravidro.wayaround.industrial.animation.PressAnimations;
import net.caravidro.wayaround.industrial.power.MechanicalPressBlock;
import net.caravidro.wayaround.industrial.power.MechanicalPressBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Detailed mechanical press assembled from independent animated actors.
 */
public final class MechanicalPressRenderer
        implements BlockEntityRenderer<MechanicalPressBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public MechanicalPressRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        blockRenderer =
                context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            MechanicalPressBlockEntity press,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();

        poseStack.translate(
                0.5,
                0.5,
                0.5
        );

        Direction facing =
                press.getBlockState()
                        .getValue(
                                MechanicalPressBlock.FACING
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

        renderClamp(
                true,
                press.animationPose(
                        PressAnimations.LEFT_CLAMP,
                        partialTick
                ),
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        renderClamp(
                false,
                press.animationPose(
                        PressAnimations.RIGHT_CLAMP,
                        partialTick
                ),
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        renderRam(
                press.animationPose(
                        PressAnimations.RAM,
                        partialTick
                ),
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        renderPlaten(
                press.animationPose(
                        PressAnimations.PLATEN,
                        partialTick
                ),
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        renderFlywheel(
                press.animationPose(
                        PressAnimations.FLYWHEEL,
                        partialTick
                ),
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        renderLever(
                press.animationPose(
                        PressAnimations.LEVER,
                        partialTick
                ),
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
    }

    private void renderFrame(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        BlockState iron =
                Blocks.IRON_BLOCK.defaultBlockState();

        BlockState steel =
                Blocks.POLISHED_DEEPSLATE.defaultBlockState();

        BlockState copper =
                Blocks.WAXED_COPPER_BLOCK.defaultBlockState();

        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                steel,
                0.0,
                -0.42,
                0.0,
                0.94,
                0.16,
                0.88,
                0.0F,
                0.0F,
                0.0F
        );

        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                iron,
                -0.36,
                0.02,
                0.0,
                0.15,
                0.80,
                0.22,
                0.0F,
                0.0F,
                0.0F
        );

        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                iron,
                0.36,
                0.02,
                0.0,
                0.15,
                0.80,
                0.22,
                0.0F,
                0.0F,
                0.0F
        );

        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                steel,
                0.0,
                0.43,
                0.0,
                0.86,
                0.16,
                0.34,
                0.0F,
                0.0F,
                0.0F
        );

        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                iron,
                0.0,
                -0.18,
                0.0,
                0.65,
                0.10,
                0.58,
                0.0F,
                0.0F,
                0.0F
        );

        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                copper,
                0.0,
                0.31,
                0.0,
                0.30,
                0.18,
                0.30,
                0.0F,
                0.0F,
                0.0F
        );

        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                steel,
                0.0,
                0.02,
                0.36,
                0.56,
                0.10,
                0.12,
                0.0F,
                0.0F,
                0.0F
        );

        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                steel,
                0.0,
                0.02,
                -0.36,
                0.56,
                0.10,
                0.12,
                0.0F,
                0.0F,
                0.0F
        );

        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                copper,
                0.47,
                0.19,
                0.0,
                0.16,
                0.16,
                0.16,
                0.0F,
                0.0F,
                0.0F
        );
    }

    private void renderClamp(
            boolean left,
            ObjectAnimationPose animation,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();

        applyPose(
                animation,
                poseStack
        );

        double x =
                left
                        ? -0.27
                        : 0.27;

        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                x,
                -0.08,
                0.0,
                0.16,
                0.18,
                0.44,
                0.0F,
                0.0F,
                0.0F
        );

        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                x
                        + (
                        left
                                ? 0.075
                                : -0.075
                ),
                -0.06,
                0.0,
                0.06,
                0.14,
                0.36,
                0.0F,
                0.0F,
                0.0F
        );

        poseStack.popPose();
    }

    private void renderRam(
            ObjectAnimationPose animation,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();

        poseStack.translate(
                0.0,
                0.30,
                0.0
        );

        applyPose(
                animation,
                poseStack
        );

        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                0.0,
                0.0,
                0.0,
                0.16,
                0.48,
                0.16,
                0.0F,
                0.0F,
                0.0F
        );

        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                0.0,
                -0.26,
                0.0,
                0.38,
                0.10,
                0.38,
                0.0F,
                0.0F,
                0.0F
        );

        poseStack.popPose();
    }

    private void renderPlaten(
            ObjectAnimationPose animation,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();

        poseStack.translate(
                0.0,
                -0.10,
                0.0
        );

        applyPose(
                animation,
                poseStack
        );

        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.SMOOTH_STONE.defaultBlockState(),
                0.0,
                0.0,
                0.0,
                0.42,
                0.08,
                0.42,
                0.0F,
                0.0F,
                0.0F
        );

        poseStack.popPose();
    }

    private void renderFlywheel(
            ObjectAnimationPose animation,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();

        poseStack.translate(
                0.49,
                0.19,
                0.0
        );

        applyPose(
                animation,
                poseStack
        );

        BlockState copper =
                Blocks.WAXED_COPPER_BLOCK.defaultBlockState();

        for (int index =
                     0;
             index < 12;
             index++) {

            double angle =
                    Math.PI
                            * 2.0
                            * index
                            / 12.0;

            double y =
                    Math.cos(
                            angle
                    )
                            * 0.23;

            double z =
                    Math.sin(
                            angle
                    )
                            * 0.23;

            renderCuboid(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    copper,
                    0.0,
                    y,
                    z,
                    0.08,
                    0.09,
                    0.16,
                    (float) Math.toDegrees(
                            angle
                    ),
                    0.0F,
                    0.0F
            );
        }

        for (int index =
                     0;
             index < 4;
             index++) {

            poseStack.pushPose();

            poseStack.mulPose(
                    Axis.XP.rotationDegrees(
                            index
                                    * 45.0F
                    )
            );

            renderCuboid(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    Blocks.IRON_BLOCK.defaultBlockState(),
                    0.0,
                    0.0,
                    0.0,
                    0.10,
                    0.44,
                    0.055,
                    0.0F,
                    0.0F,
                    0.0F
            );

            poseStack.popPose();
        }

        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                0.0,
                0.0,
                0.0,
                0.18,
                0.18,
                0.18,
                0.0F,
                0.0F,
                0.0F
        );

        poseStack.popPose();
    }

    private void renderLever(
            ObjectAnimationPose animation,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();

        poseStack.translate(
                -0.46,
                0.17,
                0.18
        );

        applyPose(
                animation,
                poseStack
        );

        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.COPPER_BLOCK.defaultBlockState(),
                0.0,
                0.12,
                0.0,
                0.055,
                0.28,
                0.055,
                0.0F,
                0.0F,
                -12.0F
        );

        renderCuboid(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                Blocks.REDSTONE_BLOCK.defaultBlockState(),
                0.0,
                0.27,
                0.0,
                0.10,
                0.10,
                0.10,
                0.0F,
                0.0F,
                0.0F
        );

        poseStack.popPose();
    }

    private static void applyPose(
            ObjectAnimationPose animation,
            PoseStack poseStack
    ) {
        poseStack.translate(
                animation.x(),
                animation.y(),
                animation.z()
        );

        poseStack.mulPose(
                Axis.XP.rotationDegrees(
                        animation.xRot()
                )
        );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        animation.yRot()
                )
        );

        poseStack.mulPose(
                Axis.ZP.rotationDegrees(
                        animation.zRot()
                )
        );

        poseStack.scale(
                animation.xScale(),
                animation.yScale(),
                animation.zScale()
        );
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

    private void renderCuboid(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            BlockState material,
            double centerX,
            double centerY,
            double centerZ,
            double sizeX,
            double sizeY,
            double sizeZ,
            float rotationX,
            float rotationY,
            float rotationZ
    ) {
        poseStack.pushPose();

        poseStack.translate(
                centerX,
                centerY,
                centerZ
        );

        poseStack.mulPose(
                Axis.XP.rotationDegrees(
                        rotationX
                )
        );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        rotationY
                )
        );

        poseStack.mulPose(
                Axis.ZP.rotationDegrees(
                        rotationZ
                )
        );

        poseStack.scale(
                (float) sizeX,
                (float) sizeY,
                (float) sizeZ
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
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
