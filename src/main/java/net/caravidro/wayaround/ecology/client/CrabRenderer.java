package net.caravidro.wayaround.ecology.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.caravidro.wayaround.ecology.CrabEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class CrabRenderer extends EntityRenderer<CrabEntity> {

    private final BlockRenderDispatcher blocks;

    public CrabRenderer(EntityRendererProvider.Context context) {
        super(context);
        blocks = context.getBlockRenderDispatcher();
        shadowRadius = 0.32F;
    }

    @Override
    public ResourceLocation getTextureLocation(CrabEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    }

    @Override
    public void render(
            CrabEntity crab,
            float yaw,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));

        block(Blocks.RED_TERRACOTTA.defaultBlockState(), -0.34, 0.02, -0.25, 0.68F, 0.22F, 0.50F, pose, buffers, light);

        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < 3; i++) {
                double z = -0.20 + i * 0.20;
                block(
                        Blocks.RED_TERRACOTTA.defaultBlockState(),
                        side < 0 ? -0.58 : 0.34,
                        -0.01,
                        z,
                        0.24F,
                        0.08F,
                        0.08F,
                        pose,
                        buffers,
                        light
                );
            }

            block(
                    Blocks.CUT_COPPER.defaultBlockState(),
                    side < 0 ? -0.67 : 0.49,
                    0.04,
                    -0.28,
                    0.18F,
                    0.14F,
                    0.18F,
                    pose,
                    buffers,
                    light
            );
        }

        block(Blocks.BLACK_CONCRETE.defaultBlockState(), -0.19, 0.20, -0.27, 0.07F, 0.07F, 0.07F, pose, buffers, light);
        block(Blocks.BLACK_CONCRETE.defaultBlockState(), 0.12, 0.20, -0.27, 0.07F, 0.07F, 0.07F, pose, buffers, light);

        pose.popPose();
        super.render(crab, yaw, partialTick, pose, buffers, light);
    }

    private void block(
            BlockState state,
            double x,
            double y,
            double z,
            float sx,
            float sy,
            float sz,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.scale(sx, sy, sz);
        blocks.renderSingleBlock(state, pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
