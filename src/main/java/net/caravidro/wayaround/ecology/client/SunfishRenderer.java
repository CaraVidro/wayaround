package net.caravidro.wayaround.ecology.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.ecology.SunfishEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Simple blocky V1 renderer: a tall flat body with small fins. The SCALE
 * attribute from ecology growth is deliberately respected.
 */
public final class SunfishRenderer
        extends EntityRenderer<SunfishEntity> {

    private final BlockRenderDispatcher blocks;

    public SunfishRenderer(
            EntityRendererProvider.Context context
    ) {
        super(
                context
        );

        blocks =
                context.getBlockRenderDispatcher();

        shadowRadius =
                0.45F;
    }

    @Override
    public ResourceLocation getTextureLocation(
            SunfishEntity entity
    ) {
        return ResourceLocation.withDefaultNamespace(
                "textures/atlas/blocks.png"
        );
    }

    @Override
    public void render(
            SunfishEntity fish,
            float yaw,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        pose.pushPose();

        float scale =
                fish.getScale();

        pose.scale(
                scale,
                scale,
                scale
        );

        pose.mulPose(
                Axis.YP.rotationDegrees(
                        180.0F - yaw
                )
        );

        block(
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                -0.52,
                -0.42,
                -0.12,
                1.04F,
                0.92F,
                0.24F,
                pose,
                buffers,
                light
        );

        block(
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                -0.34,
                0.42,
                -0.08,
                0.68F,
                0.34F,
                0.16F,
                pose,
                buffers,
                light
        );

        block(
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                -0.30,
                -0.67,
                -0.07,
                0.60F,
                0.26F,
                0.14F,
                pose,
                buffers,
                light
        );

        block(
                Blocks.GRAY_CONCRETE.defaultBlockState(),
                0.46,
                -0.12,
                -0.06,
                0.42F,
                0.20F,
                0.12F,
                pose,
                buffers,
                light
        );

        pose.popPose();

        super.render(
                fish,
                yaw,
                partialTick,
                pose,
                buffers,
                light
        );
    }

    private void block(
            BlockState state,
            double x,
            double y,
            double z,
            float width,
            float height,
            float depth,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        pose.pushPose();

        pose.translate(
                x,
                y,
                z
        );

        pose.scale(
                width,
                height,
                depth
        );

        blocks.renderSingleBlock(
                state,
                pose,
                buffers,
                light,
                OverlayTexture.NO_OVERLAY
        );

        pose.popPose();
    }
}
