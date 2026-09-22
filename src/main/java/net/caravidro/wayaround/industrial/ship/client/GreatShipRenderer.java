package net.caravidro.wayaround.industrial.ship.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.caravidro.wayaround.industrial.ship.GreatShipEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class GreatShipRenderer extends EntityRenderer<GreatShipEntity> {
    private final BlockRenderDispatcher blocks;
    public GreatShipRenderer(EntityRendererProvider.Context context) {
        super(context);
        blocks = context.getBlockRenderDispatcher();
        shadowRadius = 2.8F;
    }
    @Override public ResourceLocation getTextureLocation(GreatShipEntity ship) {
        return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    }
    @Override public void render(GreatShipEntity ship, float yaw, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        block(Blocks.SPRUCE_PLANKS.defaultBlockState(), -1.5, 0, -3.4, 3F, 0.4F, 6.4F, pose, buffers, light);
        block(Blocks.OAK_PLANKS.defaultBlockState(), -1.65, 0.4, -3.4, 3.3F, 0.25F, 6.4F, pose, buffers, light);
        block(Blocks.SPRUCE_PLANKS.defaultBlockState(), -1.85, 0.05, -3.5, 0.22F, 1.1F, 6.6F, pose, buffers, light);
        block(Blocks.SPRUCE_PLANKS.defaultBlockState(), 1.63, 0.05, -3.5, 0.22F, 1.1F, 6.6F, pose, buffers, light);
        block(Blocks.SPRUCE_PLANKS.defaultBlockState(), -1.85, 0.1, -3.75, 3.7F, 1.05F, 0.25F, pose, buffers, light);
        block(Blocks.DARK_OAK_PLANKS.defaultBlockState(), -1.9, 1.1, -3.75, 3.8F, 0.16F, 0.22F, pose, buffers, light);
        block(Blocks.SPRUCE_PLANKS.defaultBlockState(), -1.65, 0.1, 3, 3.3F, 1F, 0.25F, pose, buffers, light);
        block(Blocks.SPRUCE_PLANKS.defaultBlockState(), -1.2249999999999999, 0.1, 3.25, 2.4499999999999997F, 0.9F, 0.25F, pose, buffers, light);
        block(Blocks.SPRUCE_PLANKS.defaultBlockState(), -0.7999999999999999, 0.1, 3.5, 1.5999999999999999F, 0.8F, 0.25F, pose, buffers, light);
        block(Blocks.OAK_PLANKS.defaultBlockState(), -1.63, 0.65, -3.5, 3.26F, 0.5F, 1.4F, pose, buffers, light);
        block(Blocks.DARK_OAK_PLANKS.defaultBlockState(), -1.86, 1.15, -3.5, 0.2F, 0.3F, 1.4F, pose, buffers, light);
        block(Blocks.DARK_OAK_PLANKS.defaultBlockState(), 1.66, 1.15, -3.5, 0.2F, 0.3F, 1.4F, pose, buffers, light);
        block(Blocks.DARK_OAK_PLANKS.defaultBlockState(), -1.87, 1.08, -2.1, 0.18F, 0.17F, 5.2F, pose, buffers, light);
        block(Blocks.DARK_OAK_PLANKS.defaultBlockState(), 1.69, 1.08, -2.1, 0.18F, 0.17F, 5.2F, pose, buffers, light);
        block(Blocks.SPRUCE_LOG.defaultBlockState(), -0.12, 0.65, -1.1, 0.24F, 4.3F, 0.24F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.45, 1.7, -0.9800000000000001, 2.9F, 0.344F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.45, 2.04, -0.9800000000000001, 2.61F, 0.344F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.45, 2.38, -0.9800000000000001, 2.32F, 0.344F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.45, 2.7199999999999998, -0.9800000000000001, 2.03F, 0.344F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.45, 3.0599999999999996, -0.9800000000000001, 1.74F, 0.344F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.45, 3.4, -0.9800000000000001, 1.45F, 0.344F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.45, 3.74, -0.9800000000000001, 1.16F, 0.344F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.45, 4.08, -0.9800000000000001, 0.8700000000000001F, 0.344F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.45, 4.42, -0.9800000000000001, 0.5799999999999998F, 0.344F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.45, 4.76, -0.9800000000000001, 0.2899999999999999F, 0.344F, 0.07F, pose, buffers, light);
        block(Blocks.RED_WOOL.defaultBlockState(), 0.12, 4.62, -0.9600000000000001, 0.65F, 0.22F, 0.07F, pose, buffers, light);
        block(Blocks.SPRUCE_LOG.defaultBlockState(), -0.12, 0.65, 1.1, 0.24F, 5.4F, 0.24F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.95, 1.7, 1.2200000000000002, 3.9F, 0.454F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.95, 2.15, 1.2200000000000002, 3.51F, 0.454F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.95, 2.6, 1.2200000000000002, 3.12F, 0.454F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.95, 3.05, 1.2200000000000002, 2.73F, 0.454F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.95, 3.5, 1.2200000000000002, 2.34F, 0.454F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.95, 3.95, 1.2200000000000002, 1.95F, 0.454F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.95, 4.4, 1.2200000000000002, 1.56F, 0.454F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.95, 4.85, 1.2200000000000002, 1.1700000000000002F, 0.454F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.95, 5.3, 1.2200000000000002, 0.7799999999999998F, 0.454F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.95, 5.75, 1.2200000000000002, 0.3899999999999999F, 0.454F, 0.07F, pose, buffers, light);
        block(Blocks.RED_WOOL.defaultBlockState(), 0.12, 5.720000000000001, 1.2400000000000002, 0.65F, 0.22F, 0.07F, pose, buffers, light);
        block(Blocks.SPRUCE_LOG.defaultBlockState(), -0.12, 0.65, 2.8, 0.24F, 4.3F, 0.24F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.45, 1.7, 2.92, 2.9F, 0.344F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.45, 2.04, 2.92, 2.61F, 0.344F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.45, 2.38, 2.92, 2.32F, 0.344F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.45, 2.7199999999999998, 2.92, 2.03F, 0.344F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.45, 3.0599999999999996, 2.92, 1.74F, 0.344F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.45, 3.4, 2.92, 1.45F, 0.344F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.45, 3.74, 2.92, 1.16F, 0.344F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.45, 4.08, 2.92, 0.8700000000000001F, 0.344F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.45, 4.42, 2.92, 0.5799999999999998F, 0.344F, 0.07F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), -1.45, 4.76, 2.92, 0.2899999999999999F, 0.344F, 0.07F, pose, buffers, light);
        block(Blocks.RED_WOOL.defaultBlockState(), 0.12, 4.62, 2.94, 0.65F, 0.22F, 0.07F, pose, buffers, light);
        block(Blocks.SPRUCE_LOG.defaultBlockState(), -0.08, 0.9, 3.5, 0.16F, 0.16F, 1F, pose, buffers, light);
        block(Blocks.CRAFTING_TABLE.defaultBlockState(), -1.48, 1.15, -3.28, 0.65F, 0.65F, 0.65F, pose, buffers, light);
        block(Blocks.SPRUCE_PLANKS.defaultBlockState(), 0.45, 1.15, -3.15, 0.95F, 0.15F, 1.6F, pose, buffers, light);
        block(Blocks.RED_WOOL.defaultBlockState(), 0.45, 1.3, -2.75, 0.95F, 0.14F, 1.2F, pose, buffers, light);
        block(Blocks.WHITE_WOOL.defaultBlockState(), 0.45, 1.3, -3.15, 0.95F, 0.14F, 0.4F, pose, buffers, light);
        block(Blocks.DARK_OAK_PLANKS.defaultBlockState(), -1.5, 0.65, -1.8, 0.6F, 0.2F, 0.6F, pose, buffers, light);
        block(Blocks.DARK_OAK_PLANKS.defaultBlockState(), -1.5, 0.85, -1.82, 0.6F, 0.4F, 0.1F, pose, buffers, light);
        block(Blocks.DARK_OAK_PLANKS.defaultBlockState(), 0.8999999999999999, 0.65, -1.8, 0.6F, 0.2F, 0.6F, pose, buffers, light);
        block(Blocks.DARK_OAK_PLANKS.defaultBlockState(), 0.8999999999999999, 0.85, -1.82, 0.6F, 0.4F, 0.1F, pose, buffers, light);
        block(Blocks.DARK_OAK_PLANKS.defaultBlockState(), -1.5, 0.65, 0.39999999999999997, 0.6F, 0.2F, 0.6F, pose, buffers, light);
        block(Blocks.DARK_OAK_PLANKS.defaultBlockState(), -1.5, 0.85, 0.37999999999999995, 0.6F, 0.4F, 0.1F, pose, buffers, light);
        block(Blocks.DARK_OAK_PLANKS.defaultBlockState(), 0.8999999999999999, 0.65, 0.39999999999999997, 0.6F, 0.2F, 0.6F, pose, buffers, light);
        block(Blocks.DARK_OAK_PLANKS.defaultBlockState(), 0.8999999999999999, 0.85, 0.37999999999999995, 0.6F, 0.4F, 0.1F, pose, buffers, light);
        block(Blocks.DARK_OAK_PLANKS.defaultBlockState(), -0.3, 0.65, 2.2, 0.6F, 0.2F, 0.6F, pose, buffers, light);
        block(Blocks.DARK_OAK_PLANKS.defaultBlockState(), -0.3, 0.85, 2.18, 0.6F, 0.4F, 0.1F, pose, buffers, light);
        block(Blocks.DARK_OAK_PLANKS.defaultBlockState(), -0.08, 1.15, -2.9, 0.16F, 0.5F, 0.16F, pose, buffers, light);
        block(Blocks.DARK_OAK_PLANKS.defaultBlockState(), -0.35, 1.55, -2.94, 0.7F, 0.1F, 0.12F, pose, buffers, light);
        block(Blocks.BARREL.defaultBlockState(), -0.6, 0.65, -0.4, 0.55F, 0.6F, 0.55F, pose, buffers, light);
        block(Blocks.BARREL.defaultBlockState(), 0.05, 0.65, -0.4, 0.55F, 0.6F, 0.55F, pose, buffers, light);
        float anchorY = ship.isAnchored() ? -1.6F : .45F;
        block(Blocks.IRON_BLOCK.defaultBlockState(), 1.9, anchorY, -2.8, .08F, 1.1F-anchorY, .08F, pose, buffers, light);
        block(Blocks.IRON_BLOCK.defaultBlockState(), 1.7, anchorY, -2.85, .48F, .12F, .18F, pose, buffers, light);
        block(Blocks.IRON_BLOCK.defaultBlockState(), 1.7, anchorY, -2.85, .1F, .28F, .18F, pose, buffers, light);
        block(Blocks.IRON_BLOCK.defaultBlockState(), 2.08, anchorY, -2.85, .1F, .28F, .18F, pose, buffers, light);
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
