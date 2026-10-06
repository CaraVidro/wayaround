package net.caravidro.wayaround.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/**
 * Renders WayAround accessories inside the vanilla player render pipeline.
 *
 * <p>The pose stack supplied to a RenderLayer already contains every
 * LivingEntityRenderer transform (swimming, fall-flying, sleeping, upside-down,
 * crouch, scale, etc). The parent PlayerModel also already contains vanilla and
 * WayAround procedural bone animation for this frame.</p>
 */
public final class AccessoryPlayerLayer
        extends RenderLayer<
                AbstractClientPlayer,
                PlayerModel<AbstractClientPlayer>
        > {

    public AccessoryPlayerLayer(
            RenderLayerParent<
                    AbstractClientPlayer,
                    PlayerModel<AbstractClientPlayer>
            > parent
    ) {
        super(
                parent
        );
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            AbstractClientPlayer player,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        AccessoryRenderer.renderAttached(
                player,
                getParentModel(),
                poseStack,
                buffer,
                packedLight,
                partialTicks
        );
    }
}
