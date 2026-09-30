package net.caravidro.wayaround.ecology.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.caravidro.wayaround.ecology.DeepSeaCapsuleEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Small iron pressure bell with a forward-only visible headlamp shaft. */
public final class DeepSeaCapsuleRenderer extends EntityRenderer<DeepSeaCapsuleEntity> {
    private final BlockRenderDispatcher blocks;

    public DeepSeaCapsuleRenderer(EntityRendererProvider.Context context) {
        super(context);
        blocks = context.getBlockRenderDispatcher();
        shadowRadius = 0.65F;
    }

    @Override
    public ResourceLocation getTextureLocation(DeepSeaCapsuleEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    }

    @Override
    public void render(
            DeepSeaCapsuleEntity capsule,
            float yaw,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        float visualYaw = Mth.rotLerp(partialTick, capsule.yRotO, capsule.getYRot());
        float visualPitch = Mth.lerp(partialTick, capsule.xRotO, capsule.getXRot());

        pose.pushPose();
        pose.translate(0.0, 0.78, 0.0);
        pose.mulPose(Axis.YP.rotationDegrees(-visualYaw));
        pose.mulPose(Axis.XP.rotationDegrees(visualPitch));

        // Pressure shell: deliberately compact and chunky.
        cuboid(pose, buffers, light, Blocks.IRON_BLOCK.defaultBlockState(), 0, 0, 0, 1.10, 1.28, 0.92);
        cuboid(pose, buffers, light, Blocks.COPPER_BLOCK.defaultBlockState(), 0, 0.45, 0, 0.92, 0.12, 1.02);
        cuboid(pose, buffers, light, Blocks.COPPER_BLOCK.defaultBlockState(), 0, -0.45, 0, 0.92, 0.12, 1.02);

        // Front porthole and lamp point toward local -Z.
        cuboid(pose, buffers, LightTexture.FULL_BRIGHT, Blocks.BLUE_STAINED_GLASS.defaultBlockState(),
                0, 0.08, -0.49, 0.52, 0.52, 0.055);
        cuboid(pose, buffers, LightTexture.FULL_BRIGHT, Blocks.SEA_LANTERN.defaultBlockState(),
                0, -0.30, -0.53, 0.20, 0.16, 0.10);

        // Very cheap visible light shaft. The actual visibility budget is also
        // directional, so fauna behind the capsule is aggressively culled.
        for (int i = 0; i < 4; i++) {
            double length = 0.85 + i * 0.28;
            double z = -0.78 - i * 0.78;
            double width = 0.12 + i * 0.055;
            cuboid(pose, buffers, LightTexture.FULL_BRIGHT, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(),
                    0, -0.30, z, width, width, length);
        }

        pose.popPose();
    }

    private void cuboid(
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            BlockState state,
            double x,
            double y,
            double z,
            double sx,
            double sy,
            double sz
    ) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.scale((float) sx, (float) sy, (float) sz);
        pose.translate(-0.5, -0.5, -0.5);
        blocks.renderSingleBlock(state, pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
