package net.caravidro.wayaround.ecology.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.caravidro.wayaround.ecology.ReefSharkEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class ReefSharkRenderer extends EntityRenderer<ReefSharkEntity> {

    private final BlockRenderDispatcher blocks;

    public ReefSharkRenderer(EntityRendererProvider.Context context) {
        super(context);
        blocks = context.getBlockRenderDispatcher();
        shadowRadius = 0.65F;
    }

    @Override
    public ResourceLocation getTextureLocation(ReefSharkEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    }

    @Override
    public void render(
            ReefSharkEntity shark,
            float yaw,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        pose.pushPose();
        float scale = shark.getScale();
        pose.scale(scale, scale, scale);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));

        block(Blocks.GRAY_CONCRETE.defaultBlockState(), -0.72, -0.24, -0.22, 1.35F, 0.48F, 0.44F, pose, buffers, light);
        block(Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), -0.57, 0.20, -0.10, 0.55F, 0.42F, 0.20F, pose, buffers, light);
        block(Blocks.GRAY_CONCRETE.defaultBlockState(), 0.54, -0.14, -0.08, 0.72F, 0.28F, 0.16F, pose, buffers, light);
        block(Blocks.GRAY_CONCRETE.defaultBlockState(), -0.02, -0.05, -0.58, 0.34F, 0.12F, 0.72F, pose, buffers, light);
        block(Blocks.GRAY_CONCRETE.defaultBlockState(), -0.02, -0.05, -0.14, 0.34F, 0.12F, 0.72F, pose, buffers, light);

        pose.popPose();
        super.render(shark, yaw, partialTick, pose, buffers, light);
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
