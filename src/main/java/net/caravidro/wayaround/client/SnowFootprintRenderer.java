package net.caravidro.wayaround.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.accessory.SnowFootprintEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

/**
 * Renders a snow footprint as a tiny square plate embedded into the snow
 * surface. It is deliberately geometric to match the engineer boot sole.
 */
public final class SnowFootprintRenderer
        extends EntityRenderer<SnowFootprintEntity> {

    private final BlockRenderDispatcher blocks;

    public SnowFootprintRenderer(
            EntityRendererProvider.Context context
    ) {
        super(
                context
        );

        blocks =
                context.getBlockRenderDispatcher();

        shadowRadius =
                0.0F;
    }

    @Override
    public void render(
            SnowFootprintEntity entity,
            float yaw,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        pose.pushPose();

        pose.mulPose(
                Axis.YP.rotationDegrees(
                        entity.markYaw()
                )
        );

        /*
         * Mostly embedded in the snow: only a thin compressed square remains
         * visible, so it reads as an imprint rather than a tile placed on top.
         */
        pose.translate(
                -0.155,
                -0.007,
                -0.155
        );

        pose.scale(
                0.31F,
                0.012F,
                0.31F
        );

        blocks.renderSingleBlock(
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                pose,
                buffers,
                light,
                OverlayTexture.NO_OVERLAY
        );

        pose.popPose();

        super.render(
                entity,
                yaw,
                partialTick,
                pose,
                buffers,
                light
        );
    }

    @Override
    public ResourceLocation getTextureLocation(
            SnowFootprintEntity entity
    ) {
        return ResourceLocation.withDefaultNamespace(
                "textures/atlas/blocks.png"
        );
    }
}
