package net.caravidro.wayaround.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.caravidro.wayaround.client.AccessoryRenderer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla first person only asks PlayerRenderer to draw one arm at a time, so
 * normal third-person layers are never visited. Render the matching sleeve and
 * hand accessories immediately after the vanilla arm using the exact same
 * first-person pose stack.
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerFirstPersonAccessoryMixin {

    @Inject(
            method = "renderRightHand",
            at = @At("RETURN")
    )
    private void wayaround$rightAccessories(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int combinedLight,
            AbstractClientPlayer player,
            CallbackInfo ci
    ) {
        PlayerModel<?> model =
                ((PlayerRenderer) (Object) this)
                        .getModel();

        AccessoryRenderer.renderFirstPersonArm(
                player,
                model.rightArm,
                poseStack,
                buffer,
                combinedLight,
                false
        );
    }

    @Inject(
            method = "renderLeftHand",
            at = @At("RETURN")
    )
    private void wayaround$leftAccessories(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int combinedLight,
            AbstractClientPlayer player,
            CallbackInfo ci
    ) {
        PlayerModel<?> model =
                ((PlayerRenderer) (Object) this)
                        .getModel();

        AccessoryRenderer.renderFirstPersonArm(
                player,
                model.leftArm,
                poseStack,
                buffer,
                combinedLight,
                true
        );
    }
}
