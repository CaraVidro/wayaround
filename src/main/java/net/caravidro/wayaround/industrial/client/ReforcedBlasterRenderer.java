package net.caravidro.wayaround.industrial.client;

import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.animation.SmoothObjectAnimation;
import net.caravidro.wayaround.industrial.ReforcedBlasterBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;

/**
 * Industrial blaster renderer with a visible blower and moving internal fan.
 */
public final class ReforcedBlasterRenderer
        implements BlockEntityRenderer<ReforcedBlasterBlockEntity> {

    private static final Map<ReforcedBlasterBlockEntity, SmoothObjectAnimation.Rotation> ROTATIONS =
            new WeakHashMap<>();

    private final BlockRenderDispatcher blockRenderer;

    public ReforcedBlasterRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        blockRenderer =
                context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            ReforcedBlasterBlockEntity blaster,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (blaster.getLevel() == null) {
            return;
        }

        boolean lit =
                blaster.getBlockState()
                        .getValue(
                                AbstractFurnaceBlock.LIT
                        );

        double renderTime =
                blaster.getLevel().getGameTime()
                        + partialTick;

        SmoothObjectAnimation.Rotation rotation =
                ROTATIONS.computeIfAbsent(
                        blaster,
                        key -> new SmoothObjectAnimation.Rotation(
                                0.0F,
                                0.48F,
                                0.0F,
                                180.0F
                        )
                );

        float blowerAngle =
                rotation.update(
                        renderTime,
                        lit
                                ? 62.0F
                                : 0.0F
                );

        poseStack.pushPose();
        poseStack.translate(
                0.5,
                0.5,
                0.5
        );

        IndustrialRenderUtil.orientHorizontal(
                poseStack,
                blaster.getBlockState()
                        .getValue(
                                AbstractFurnaceBlock.FACING
                        )
        );

        double shake =
                lit
                        ? Math.sin(
                        renderTime * 2.4
                ) * 0.003
                        : 0.0;

        poseStack.translate(
                shake,
                0.0,
                -shake * 0.5
        );

        // Heavy base / shell.
        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                0.0, -0.43, 0.0,
                0.94, 0.14, 0.90
        );

        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                0.0, 0.0, 0.04,
                0.78, 0.72, 0.72
        );

        // Hot central chamber, visible through the front mouth.
        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                lit
                        ? Blocks.MAGMA_BLOCK.defaultBlockState()
                        : Blocks.BLACKSTONE.defaultBlockState(),
                0.0, -0.02, -0.34,
                0.46, 0.40, 0.10
        );

        // Furnace mouth / reinforced rim.
        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.POLISHED_BLACKSTONE.defaultBlockState(),
                0.0, -0.02, -0.405,
                0.60, 0.54, 0.08
        );

        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                lit
                        ? Blocks.MAGMA_BLOCK.defaultBlockState()
                        : Blocks.BLACKSTONE.defaultBlockState(),
                0.0, -0.02, -0.455,
                0.42, 0.34, 0.04
        );

        // Copper heat-transfer bands.
        for (double y :
                new double[] {
                        -0.24,
                        0.24
                }) {
            IndustrialRenderUtil.cuboid(
                    blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                    Blocks.COPPER_BLOCK.defaultBlockState(),
                    0.0, y, 0.05,
                    0.84, 0.07, 0.76
            );
        }

        // Side blower housing.
        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                0.47, 0.02, 0.05,
                0.18, 0.48, 0.48
        );

        poseStack.pushPose();
        poseStack.translate(
                0.575,
                0.02,
                0.05
        );
        poseStack.mulPose(
                Axis.XP.rotationDegrees(
                        blowerAngle
                )
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
                    Blocks.COPPER_BLOCK.defaultBlockState(),
                    0.0, 0.14, 0.0,
                    0.045, 0.22, 0.085,
                    18.0F,
                    0.0F,
                    0.0F
            );

            poseStack.popPose();
        }

        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                0.0, 0.0, 0.0,
                0.08, 0.13, 0.13
        );

        poseStack.popPose();

        // Simple guard bars over the blower.
        for (double offset :
                new double[] {
                        -0.16,
                        0.0,
                        0.16
                }) {
            IndustrialRenderUtil.cuboid(
                    blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                    Blocks.IRON_BARS.defaultBlockState(),
                    0.68, offset, 0.05,
                    0.035, 0.035, 0.42
            );

            IndustrialRenderUtil.cuboid(
                    blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                    Blocks.IRON_BARS.defaultBlockState(),
                    0.68, 0.02, 0.05 + offset,
                    0.035, 0.42, 0.035
            );
        }

        // Exhaust neck.
        IndustrialRenderUtil.cuboid(
                blockRenderer, poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                -0.24, 0.44, 0.18,
                0.18, 0.24, 0.18
        );

        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
