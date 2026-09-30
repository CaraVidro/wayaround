package net.caravidro.wayaround.industrial.client;

import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.animation.SmoothObjectAnimation;
import net.caravidro.wayaround.industrial.power.MechanicalMillBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.Blocks;

public final class MechanicalMillRenderer
        implements BlockEntityRenderer<MechanicalMillBlockEntity> {

    private static final Map<MechanicalMillBlockEntity, SmoothObjectAnimation.Rotation> ROTATIONS =
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
        if (mill.getLevel() == null) {
            return;
        }

        double renderTime =
                mill.getLevel()
                        .getGameTime()
                        + partialTick;

        SmoothObjectAnimation.Rotation visual =
                ROTATIONS.computeIfAbsent(
                        mill,
                        key -> new SmoothObjectAnimation.Rotation(
                                mill.rotationDegrees(),
                                0.42F,
                                0.16F,
                                24.0F
                        )
                );

        float angle =
                visual.update(
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

        // timber base
        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.DARK_OAK_PLANKS.defaultBlockState(),
                0.0, -0.42, 0.0,
                0.90, 0.13, 0.90
        );

        for (double x :
                new double[] {
                        -0.35,
                        0.35
                }) {
            for (double z :
                    new double[] {
                            -0.35,
                            0.35
                    }) {
                IndustrialRenderUtil.cuboid(
                        blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                        Blocks.DARK_OAK_LOG.defaultBlockState(),
                        x, -0.12, z,
                        0.10, 0.55, 0.10
                );
            }
        }

        // lower fixed millstone
        renderMillstone(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                0.0F,
                -0.10
        );

        // rotating upper stone
        poseStack.pushPose();

        poseStack.translate(
                0.0,
                0.12,
                0.0
        );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        angle
                )
        );

        renderMillstone(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                0.0F,
                0.0
        );

        poseStack.popPose();

        // central spindle
        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                0.0, 0.18, 0.0,
                0.08, 0.72, 0.08
        );

        // simple timber hopper
        for (double x :
                new double[] {
                        -0.18,
                        0.18
                }) {
            IndustrialRenderUtil.cuboid(
                    blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                    Blocks.OAK_PLANKS.defaultBlockState(),
                    x, 0.40, 0.0,
                    0.09, 0.32, 0.42,
                    0.0F,
                    0.0F,
                    x < 0.0
                            ? -12.0F
                            : 12.0F
            );
        }

        for (double z :
                new double[] {
                        -0.18,
                        0.18
                }) {
            IndustrialRenderUtil.cuboid(
                    blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                    Blocks.OAK_PLANKS.defaultBlockState(),
                    0.0, 0.40, z,
                    0.42, 0.32, 0.09,
                    z < 0.0
                            ? 12.0F
                            : -12.0F,
                    0.0F,
                    0.0F
            );
        }

        poseStack.popPose();
    }

    private void renderMillstone(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            float angle,
            double y
    ) {
        poseStack.pushPose();

        poseStack.translate(
                0.0,
                y,
                0.0
        );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        angle
                )
        );

        /*
         * Octagonal-ish stone: crossed slabs make a chunky Minecraft millstone
         * without pretending to be a perfect smooth cylinder.
         */
        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.STONE.defaultBlockState(),
                0.0, 0.0, 0.0,
                0.72, 0.15, 0.72
        );

        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.ANDESITE.defaultBlockState(),
                0.0, 0.0, 0.0,
                0.58, 0.16, 0.82,
                0.0F, 45.0F, 0.0F
        );

        // visible groove/cross
        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.DEEPSLATE.defaultBlockState(),
                0.0, 0.083, 0.0,
                0.62, 0.018, 0.055
        );

        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.DEEPSLATE.defaultBlockState(),
                0.0, 0.083, 0.0,
                0.055, 0.018, 0.62
        );

        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
