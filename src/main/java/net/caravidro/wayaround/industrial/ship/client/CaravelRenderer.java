package net.caravidro.wayaround.industrial.ship.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.caravidro.wayaround.industrial.ship.CaravelEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Separate caravel model; no vanilla hull or paddles are rendered. */
public final class CaravelRenderer extends EntityRenderer<CaravelEntity> {
    private final BlockRenderDispatcher blocks;
    public CaravelRenderer(EntityRendererProvider.Context context) {
        super(context);
        blocks = context.getBlockRenderDispatcher();
        shadowRadius = 1.8F;
    }
    @Override public ResourceLocation getTextureLocation(CaravelEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    }
    @Override public void render(CaravelEntity ship, float yaw, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        float hurt = ship.getHurtTime() - partialTick;
        if (hurt > 0) {
            pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(hurt) * hurt
                    * Math.max(0, ship.getDamage() - partialTick) / 20 * ship.getHurtDir()));
        }
        block(Blocks.SPRUCE_PLANKS.defaultBlockState(), -0.82, 0, -1.9, 1.64F, 0.35F, 3.65F, pose, buffers, light);
        block(Blocks.OAK_PLANKS.defaultBlockState(), -1, 0.35, -1.9, 2F, 0.18F, 3.65F, pose, buffers, light);
        block(Blocks.SPRUCE_PLANKS.defaultBlockState(), -1.16, 0.15, -1.95, 0.18F, 0.85F, 3.5F, pose, buffers, light);
        block(Blocks.SPRUCE_PLANKS.defaultBlockState(), 0.98, 0.15, -1.95, 0.18F, 0.85F, 3.5F, pose, buffers, light);
        block(Blocks.SPRUCE_PLANKS.defaultBlockState(), -1.16, 0.15, -2.2, 2.32F, 0.85F, 0.25F, pose, buffers, light);
        block(Blocks.SPRUCE_PLANKS.defaultBlockState(), -0.95, 0.15, 1.55, 1.9F, 0.8F, 0.25F, pose, buffers, light);
        block(Blocks.SPRUCE_PLANKS.defaultBlockState(), -0.7, 0.15, 1.8, 1.4F, 0.75F, 0.25F, pose, buffers, light);
        block(Blocks.SPRUCE_PLANKS.defaultBlockState(), -0.4, 0.15, 2.05, 0.8F, 0.7F, 0.2F, pose, buffers, light);
        block(Blocks.OAK_PLANKS.defaultBlockState(), -0.98, 0.53, -2, 1.96F, 0.35F, 0.9F, pose, buffers, light);
        block(Blocks.DARK_OAK_PLANKS.defaultBlockState(), -1.18, 0.88, -2.2, 2.36F, 0.16F, 0.2F, pose, buffers, light);
        block(Blocks.DARK_OAK_PLANKS.defaultBlockState(), -1.18, 0.9, -2, 0.14F, 0.18F, 0.9F, pose, buffers, light);
        block(Blocks.DARK_OAK_PLANKS.defaultBlockState(), 1.04, 0.9, -2, 0.14F, 0.18F, 0.9F, pose, buffers, light);
        block(Blocks.DARK_OAK_PLANKS.defaultBlockState(), -1.18, 0.92, -1.1, 0.14F, 0.13F, 2.65F, pose, buffers, light);
        block(Blocks.DARK_OAK_PLANKS.defaultBlockState(), 1.04, 0.92, -1.1, 0.14F, 0.13F, 2.65F, pose, buffers, light);
        block(Blocks.SPRUCE_LOG.defaultBlockState(), -0.1, 0.53, 0.5, 0.2F, 3.9F, 0.2F, pose, buffers, light);
        block(Blocks.SPRUCE_LOG.defaultBlockState(), -0.09, 0.88, -1, 0.18F, 2.9F, 0.18F, pose, buffers, light);
        block(Blocks.SPRUCE_LOG.defaultBlockState(), -0.07, 0.65, 2, 0.14F, 0.14F, 0.8F, pose, buffers, light);
        block(Blocks.BARREL.defaultBlockState(), 0.45, 0.53, -0.85, 0.45F, 0.48F, 0.45F, pose, buffers, light);
        block(Blocks.BARREL.defaultBlockState(), -0.9, 0.53, -0.85, 0.45F, 0.48F, 0.45F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.2, 1.45, 0.55, 2.4F, 0.225F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -0.85, 1.35, -0.95, 1.7F, 0.175F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.2, 1.67, 0.55, 2.1999999999999997F, 0.225F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -0.85, 1.52, -0.95, 1.5583333333333331F, 0.175F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.2, 1.89, 0.55, 2F, 0.225F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -0.85, 1.6900000000000002, -0.95, 1.4166666666666667F, 0.175F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.2, 2.11, 0.55, 1.7999999999999998F, 0.225F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -0.85, 1.86, -0.95, 1.275F, 0.175F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.2, 2.33, 0.55, 1.6F, 0.225F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -0.85, 2.0300000000000002, -0.95, 1.1333333333333335F, 0.175F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.2, 2.55, 0.55, 1.3999999999999997F, 0.225F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -0.85, 2.2, -0.95, 0.9916666666666665F, 0.175F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.2, 2.77, 0.55, 1.2F, 0.225F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -0.85, 2.37, -0.95, 0.85F, 0.175F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.2, 2.99, 0.55, 0.9999999999999999F, 0.225F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -0.85, 2.54, -0.95, 0.7083333333333333F, 0.175F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.2, 3.21, 0.55, 0.8F, 0.225F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -0.85, 2.71, -0.95, 0.5666666666666668F, 0.175F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.2, 3.4299999999999997, 0.55, 0.6F, 0.225F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -0.85, 2.88, -0.95, 0.425F, 0.175F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.2, 3.6500000000000004, 0.55, 0.3999999999999999F, 0.225F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -0.85, 3.0500000000000003, -0.95, 0.28333333333333327F, 0.175F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.2, 3.87, 0.55, 0.2000000000000001F, 0.225F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -0.85, 3.22, -0.95, 0.14166666666666672F, 0.175F, 0.07F, pose, buffers, light);
        block(Blocks.RED_WOOL.defaultBlockState(), 0.12, 4.05, 0.54, 0.5F, 0.22F, 0.08F, pose, buffers, light);
        block(Blocks.DARK_OAK_PLANKS.defaultBlockState(), -0.06, 0.92, -1.65, 0.12F, 0.5F, 0.12F, pose, buffers, light);
        block(Blocks.DARK_OAK_PLANKS.defaultBlockState(), -0.3, 1.27, -1.69, 0.6F, 0.08F, 0.12F, pose, buffers, light);
        pose.popPose();
        super.render(ship, yaw, partialTick, pose, buffers, light);
    }
    private void block(BlockState state, double x, double y, double z, float w, float h, float d,
            PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.scale(w, h, d);
        blocks.renderSingleBlock(state, pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
