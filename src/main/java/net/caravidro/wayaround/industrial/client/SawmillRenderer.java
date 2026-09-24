package net.caravidro.wayaround.industrial.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.industrial.power.SawmillBlock;
import net.caravidro.wayaround.industrial.power.SawmillBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class SawmillRenderer
        implements BlockEntityRenderer<SawmillBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public SawmillRenderer(BlockEntityRendererProvider.Context context) {
        blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            SawmillBlockEntity sawmill,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        Direction facing = sawmill.getBlockState().getValue(SawmillBlock.FACING);
        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        yawFor(facing)
                )
        );

        renderBody(
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        if (sawmill.shaftInstalled()) {
            renderCuboid(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    Blocks.IRON_BLOCK.defaultBlockState(),
                    0.0,
                    0.18,
                    0.0,
                    0.13,
                    0.13,
                    0.92,
                    0.0F
            );
        }

        if (sawmill.bladeInstalled()) {
            renderSaw(
                    sawmill.bladeAngle(),
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay
            );
        }

        if (sawmill.hasInput()) {
            double z =
                    0.34
                    - sawmill.progress()
                    * 0.48;

            renderCuboid(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    Blocks.OAK_LOG.defaultBlockState(),
                    -0.24,
                    -0.02,
                    z,
                    0.28,
                    0.28,
                    0.72,
                    0.0F
            );
        }

        poseStack.popPose();
    }

    private void renderBody(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        BlockState wood = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState darkWood = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();

        renderCuboid(poseStack, bufferSource, packedLight, packedOverlay,
                wood, 0.0, -0.10, 0.0, 0.92, 0.12, 0.88, 0.0F);

        renderCuboid(poseStack, bufferSource, packedLight, packedOverlay,
                darkWood, -0.34, -0.35, -0.31, 0.13, 0.52, 0.13, 0.0F);
        renderCuboid(poseStack, bufferSource, packedLight, packedOverlay,
                darkWood, 0.34, -0.35, -0.31, 0.13, 0.52, 0.13, 0.0F);
        renderCuboid(poseStack, bufferSource, packedLight, packedOverlay,
                darkWood, -0.34, -0.35, 0.31, 0.13, 0.52, 0.13, 0.0F);
        renderCuboid(poseStack, bufferSource, packedLight, packedOverlay,
                darkWood, 0.34, -0.35, 0.31, 0.13, 0.52, 0.13, 0.0F);

        renderCuboid(poseStack, bufferSource, packedLight, packedOverlay,
                darkWood, -0.34, 0.18, 0.0, 0.12, 0.58, 0.12, 0.0F);
        renderCuboid(poseStack, bufferSource, packedLight, packedOverlay,
                darkWood, 0.34, 0.18, 0.0, 0.12, 0.58, 0.12, 0.0F);

        renderCuboid(poseStack, bufferSource, packedLight, packedOverlay,
                Blocks.IRON_BLOCK.defaultBlockState(),
                0.0, 0.43, 0.0, 0.78, 0.08, 0.10, 0.0F);
    }

    private void renderSaw(
            float angle,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();
        poseStack.translate(0.0, 0.18, 0.0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(angle));

        BlockState iron = Blocks.IRON_BLOCK.defaultBlockState();

        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2.0 * i / 12.0;
            double x = Math.cos(a) * 0.285;
            double y = Math.sin(a) * 0.285;

            renderCuboid(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    iron,
                    x,
                    y,
                    0.0,
                    0.13,
                    0.055,
                    0.055,
                    (float) Math.toDegrees(a)
            );
        }

        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2.0 * i / 6.0;
            double x = Math.cos(a) * 0.13;
            double y = Math.sin(a) * 0.13;

            renderCuboid(
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    iron,
                    x,
                    y,
                    0.0,
                    0.24,
                    0.045,
                    0.045,
                    (float) Math.toDegrees(a)
            );
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
                0.16,
                0.16,
                0.12,
                0.0F
        );

        poseStack.popPose();
    }

    private static float yawFor(Direction facing) {
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
            float rotationZ
    ) {
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, centerZ);
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotationZ));
        poseStack.scale((float) sizeX, (float) sizeY, (float) sizeZ);
        poseStack.translate(-0.5, -0.5, -0.5);

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
