package net.caravidro.wayaround.nexus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.caravidro.wayaround.nexus.NexusSludgeEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

public final class NexusSludgeRenderer
        extends EntityRenderer<NexusSludgeEntity> {

    private final BlockRenderDispatcher blocks;

    public NexusSludgeRenderer(
            EntityRendererProvider.Context context
    ) {
        super(context);
        blocks = context.getBlockRenderDispatcher();
        shadowRadius = 0.18F;
    }

    @Override
    public ResourceLocation getTextureLocation(
            NexusSludgeEntity entity
    ) {
        return ResourceLocation.withDefaultNamespace(
                "textures/atlas/blocks.png"
        );
    }

    @Override
    public void render(
            NexusSludgeEntity entity,
            float yaw,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light
    ) {
        pose.pushPose();
        pose.translate(
                -0.24,
                0.02,
                -0.24
        );
        pose.scale(
                0.48F,
                0.28F,
                0.48F
        );

        blocks.renderSingleBlock(
                Blocks.BLACK_CONCRETE.defaultBlockState(),
                pose,
                buffers,
                light,
                OverlayTexture.NO_OVERLAY
        );

        pose.popPose();
    }
}
