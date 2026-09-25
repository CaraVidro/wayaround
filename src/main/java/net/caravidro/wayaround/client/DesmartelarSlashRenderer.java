package net.caravidro.wayaround.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.cursed.DesmartelarSlashEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class DesmartelarSlashRenderer
        extends EntityRenderer<DesmartelarSlashEntity> {

    public DesmartelarSlashRenderer(
            EntityRendererProvider.Context context
    ) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(
            DesmartelarSlashEntity entity,
            float yaw,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int packedLight
    ) {
        float life =
                Math.max(
                        0.0F,
                        1.0F
                                - (
                                entity.tickCount
                                        + partialTick
                        )
                                / 8.0F
                );

        float length =
                entity.length()
                        * (
                        0.90F
                                + life * 0.10F
                );

        float width =
                entity.width()
                        * (
                        0.45F
                                + life * 0.55F
                );

        pose.pushPose();

        pose.mulPose(
                Axis.YP.rotationDegrees(
                        -entity.getYRot()
                )
        );

        pose.mulPose(
                Axis.XP.rotationDegrees(
                        entity.getXRot()
                )
        );

        renderBand(
                Blocks.RED_NETHER_BRICKS.defaultBlockState(),
                length,
                width,
                0.11F,
                pose,
                buffers
        );

        pose.translate(
                0.0,
                0.0,
                -0.02
        );

        renderBand(
                Blocks.REDSTONE_BLOCK.defaultBlockState(),
                length * 0.94F,
                width * 0.28F,
                0.055F,
                pose,
                buffers
        );

        pose.popPose();

        super.render(
                entity,
                yaw,
                partialTick,
                pose,
                buffers,
                packedLight
        );
    }

    private static void renderBand(
            BlockState state,
            float length,
            float width,
            float thickness,
            PoseStack pose,
            MultiBufferSource buffers
    ) {
        pose.pushPose();

        pose.scale(
                width,
                thickness,
                length
        );

        pose.translate(
                -0.5F,
                -0.5F,
                -0.5F
        );

        Minecraft.getInstance()
                .getBlockRenderer()
                .renderSingleBlock(
                        state,
                        pose,
                        buffers,
                        LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY
                );

        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(
            DesmartelarSlashEntity entity
    ) {
        return ResourceLocation.withDefaultNamespace(
                "textures/atlas/blocks.png"
        );
    }
}
