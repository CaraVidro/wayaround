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
        super(
                context
        );

        shadowRadius =
                0.0F;
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
                                / 10.0F
                );

        float span =
                entity.length()
                        * (
                        0.92F
                                + life
                                        * 0.08F
                );

        float depth =
                entity.width()
                        * (
                        0.45F
                                + life
                                        * 0.55F
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

        pose.mulPose(
                Axis.ZP.rotationDegrees(
                        entity.roll()
                )
        );

        BlockState outer =
                entity.fiery()
                        ? Blocks.YELLOW_CONCRETE
                                .defaultBlockState()
                        : Blocks.RED_CONCRETE
                                .defaultBlockState();

        BlockState inner =
                entity.fiery()
                        ? Blocks.RED_CONCRETE
                                .defaultBlockState()
                        : Blocks.WHITE_CONCRETE
                                .defaultBlockState();

        renderBand(
                outer,
                span,
                depth,
                0.10F,
                pose,
                buffers
        );

        pose.translate(
                0.0,
                0.0,
                -0.04
        );

        renderBand(
                inner,
                span * 0.92F,
                depth * 0.70F,
                0.14F,
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

    /**
     * Local X is the long axis. The slash therefore reads as a horizontal
     * "—" while the entity itself moves forward through world space.
     */
    private static void renderBand(
            BlockState state,
            float span,
            float depth,
            float thickness,
            PoseStack pose,
            MultiBufferSource buffers
    ) {
        pose.pushPose();

        pose.scale(
                span,
                thickness,
                depth
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

