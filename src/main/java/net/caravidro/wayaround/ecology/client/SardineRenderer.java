package net.caravidro.wayaround.ecology.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.caravidro.wayaround.ecology.SardineEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class SardineRenderer extends EntityRenderer<SardineEntity> {

    private final BlockRenderDispatcher blocks;

    public SardineRenderer(EntityRendererProvider.Context context) {
        super(context);
        blocks = context.getBlockRenderDispatcher();
        shadowRadius = 0.12F;
    }

    @Override
    public ResourceLocation getTextureLocation(SardineEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    }

    @Override
    public void render(
            SardineEntity fish,
            float yaw,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        pose.pushPose();
        float scale = fish.getScale();
        pose.scale(scale, scale, scale);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));

        block(Blocks.IRON_BLOCK.defaultBlockState(), -0.32, -0.08, -0.07, 0.58F, 0.16F, 0.14F, pose, buffers, light);
        block(Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(), -0.26, 0.055, -0.055, 0.44F, 0.055F, 0.11F, pose, buffers, light);
        block(Blocks.GRAY_CONCRETE.defaultBlockState(), 0.22, -0.055, -0.04, 0.22F, 0.10F, 0.08F, pose, buffers, light);

        pose.popPose();
        super.render(fish, yaw, partialTick, pose, buffers, light);
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
