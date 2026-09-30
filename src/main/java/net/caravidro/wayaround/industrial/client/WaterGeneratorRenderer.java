package net.caravidro.wayaround.industrial.client;

import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.animation.SmoothObjectAnimation;
import net.caravidro.wayaround.industrial.power.WaterGeneratorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.Blocks;

/**
 * Open-frame generator renderer. The stator stays fixed while the exposed
 * rotor spins continuously according to actual generated power.
 */
public final class WaterGeneratorRenderer
        implements BlockEntityRenderer<WaterGeneratorBlockEntity> {

    private static final Map<WaterGeneratorBlockEntity, SmoothObjectAnimation.Rotation> ROTATIONS =
            new WeakHashMap<>();

    private final BlockRenderDispatcher blockRenderer;

    public WaterGeneratorRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        blockRenderer =
                context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            WaterGeneratorBlockEntity generator,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (generator.getLevel() == null) {
            return;
        }

        double renderTime =
                generator.getLevel().getGameTime()
                        + partialTick;

        float targetRpm =
                generator.generationPerTick()
                        / 640.0F
                        * 58.0F;

        SmoothObjectAnimation.Rotation rotation =
                ROTATIONS.computeIfAbsent(
                        generator,
                        key -> new SmoothObjectAnimation.Rotation(
                                0.0F,
                                0.42F,
                                0.0F,
                                180.0F
                        )
                );

        float angle =
                rotation.update(
                        renderTime,
                        targetRpm
                );

        poseStack.pushPose();
        poseStack.translate(
                0.5,
                0.5,
                0.5
        );

        // Feet and main cage.
        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                0.0, -0.42, 0.0,
                0.88, 0.12, 0.76
        );

        for (double x :
                new double[] {
                        -0.35,
                        0.35
                }) {
            for (double z :
                    new double[] {
                            -0.28,
                            0.28
                    }) {
                IndustrialRenderUtil.cuboid(
                        blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                        Blocks.IRON_BLOCK.defaultBlockState(),
                        x, 0.0, z,
                        0.10, 0.72, 0.10
                );
            }
        }

        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                0.0, 0.38, 0.0,
                0.82, 0.10, 0.66
        );

        // Fixed copper stator rings.
        for (double x :
                new double[] {
                        -0.24,
                        0.24
                }) {
            IndustrialRenderUtil.cuboid(
                    blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                    Blocks.COPPER_BLOCK.defaultBlockState(),
                    x, 0.0, 0.0,
                    0.11, 0.50, 0.50
            );
        }

        // Rotor axis is X, matching the exposed generator shaft.
        poseStack.pushPose();
        poseStack.mulPose(
                Axis.XP.rotationDegrees(
                        angle
                )
        );

        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                0.0, 0.0, 0.0,
                0.78, 0.10, 0.10
        );

        for (int index =
                     0;
             index < 6;
             index++) {

            poseStack.pushPose();
            poseStack.mulPose(
                    Axis.XP.rotationDegrees(
                            index
                                    * 60.0F
                    )
            );

            IndustrialRenderUtil.cuboid(
                    blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                    Blocks.IRON_BLOCK.defaultBlockState(),
                    0.0, 0.17, 0.0,
                    0.40, 0.23, 0.07
            );

            poseStack.popPose();
        }

        poseStack.popPose();

        // Bearing caps.
        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.BLACKSTONE.defaultBlockState(),
                -0.43, 0.0, 0.0,
                0.10, 0.22, 0.22
        );

        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.BLACKSTONE.defaultBlockState(),
                0.43, 0.0, 0.0,
                0.10, 0.22, 0.22
        );

        // Tiny status lamp: greenish copper when mechanically connected,
        // dark otherwise.
        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                generator.mechanicalConnected()
                        ? Blocks.OXIDIZED_COPPER.defaultBlockState()
                        : Blocks.BLACKSTONE.defaultBlockState(),
                0.0, 0.24, -0.37,
                0.12, 0.12, 0.06
        );

        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
