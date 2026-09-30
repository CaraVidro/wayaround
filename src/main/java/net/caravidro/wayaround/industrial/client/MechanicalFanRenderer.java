package net.caravidro.wayaround.industrial.client;

import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.animation.SmoothObjectAnimation;
import net.caravidro.wayaround.industrial.power.MechanicalFanBlock;
import net.caravidro.wayaround.industrial.power.MechanicalFanBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.Blocks;

public final class MechanicalFanRenderer
        implements BlockEntityRenderer<MechanicalFanBlockEntity> {

    private static final Map<MechanicalFanBlockEntity, SmoothObjectAnimation.Rotation> ROTATIONS =
            new WeakHashMap<>();

    private final BlockRenderDispatcher blockRenderer;

    public MechanicalFanRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        blockRenderer =
                context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            MechanicalFanBlockEntity fan,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (fan.getLevel() == null) {
            return;
        }

        double renderTime =
                fan.getLevel().getGameTime()
                        + partialTick;

        SmoothObjectAnimation.Rotation rotation =
                ROTATIONS.computeIfAbsent(
                        fan,
                        key -> new SmoothObjectAnimation.Rotation(
                                fan.rotationDegrees(),
                                0.48F,
                                0.18F,
                                22.0F
                        )
                );

        float angle =
                rotation.update(
                        renderTime,
                        fan.rpm(),
                        fan.rotationDegrees()
                );

        double vibration =
                Math.sin(
                        renderTime * 2.9
                )
                        * fan.airflowStrength()
                        * 0.0025;

        poseStack.pushPose();
        poseStack.translate(
                0.5 + vibration,
                0.5,
                0.5 - vibration * 0.4
        );

        IndustrialRenderUtil.orientHorizontal(
                poseStack,
                fan.getBlockState()
                        .getValue(
                                MechanicalFanBlock.FACING
                        )
        );

        renderFrame(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        renderRearGuard(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        poseStack.pushPose();
        poseStack.translate(
                0.0,
                0.0,
                -0.03
        );
        poseStack.mulPose(
                Axis.ZP.rotationDegrees(
                        angle
                )
        );

        renderBlades(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        poseStack.popPose();

        renderFrontGuard(
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
        for (double x :
                new double[] {
                        -0.43,
                        0.43
                }) {
            IndustrialRenderUtil.cuboid(
                    blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                    Blocks.IRON_BLOCK.defaultBlockState(),
                    x, 0.0, 0.0,
                    0.10, 0.92, 0.24
            );
        }

        for (double y :
                new double[] {
                        -0.43,
                        0.43
                }) {
            IndustrialRenderUtil.cuboid(
                    blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                    Blocks.IRON_BLOCK.defaultBlockState(),
                    0.0, y, 0.0,
                    0.92, 0.10, 0.24
            );
        }

        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                0.0, -0.48, 0.08,
                0.70, 0.08, 0.34
        );

        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.COPPER_BLOCK.defaultBlockState(),
                0.0, 0.0, 0.12,
                0.18, 0.18, 0.30
        );
    }

    private void renderBlades(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        for (int index =
                     0;
             index < 5;
             index++) {

            poseStack.pushPose();

            poseStack.mulPose(
                    Axis.ZP.rotationDegrees(
                            index * 72.0F
                    )
            );

            IndustrialRenderUtil.cuboid(
                    blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                    Blocks.IRON_BLOCK.defaultBlockState(),
                    0.0, 0.20, 0.0,
                    0.14, 0.36, 0.055,
                    0.0F,
                    0.0F,
                    24.0F
            );

            IndustrialRenderUtil.cuboid(
                    blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                    Blocks.COPPER_BLOCK.defaultBlockState(),
                    0.0, 0.34, 0.0,
                    0.20, 0.16, 0.05,
                    0.0F,
                    0.0F,
                    34.0F
            );

            poseStack.popPose();
        }

        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                0.0, 0.0, 0.0,
                0.20, 0.20, 0.12
        );
    }

    private void renderFrontGuard(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        renderGuard(
                0.16,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );
    }

    private void renderRearGuard(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        renderGuard(
                -0.16,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );
    }

    private void renderGuard(
            double z,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        for (double offset :
                new double[] {
                        -0.28,
                        -0.14,
                        0.0,
                        0.14,
                        0.28
                }) {

            IndustrialRenderUtil.cuboid(
                    blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                    Blocks.IRON_BARS.defaultBlockState(),
                    offset, 0.0, z,
                    0.025, 0.74, 0.025
            );

            IndustrialRenderUtil.cuboid(
                    blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                    Blocks.IRON_BARS.defaultBlockState(),
                    0.0, offset, z,
                    0.74, 0.025, 0.025
            );
        }
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
