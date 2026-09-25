package net.caravidro.wayaround.mixin.client;

import net.caravidro.wayaround.client.cinematic.PlayerAnimationController;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs after vanilla has constructed the humanoid pose, making the cinematic
 * pose the final model transform rather than fighting setupAnim.
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidAnimationMixin<T extends LivingEntity> {

    @Inject(
            method = "setupAnim*",
            at = @At("HEAD")
    )
    private void wayaround$beforePlayerAnimation(
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        if (!(entity instanceof AbstractClientPlayer player)
                || !((Object) this
                instanceof PlayerModel<?> model)) {
            return;
        }

        PlayerAnimationController.beforeSetupAnim(
                player,
                model
        );
    }

    @Inject(
            method = "setupAnim*",
            at = @At("RETURN")
    )
    private void wayaround$afterPlayerAnimation(
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        if (!(entity instanceof AbstractClientPlayer player)
                || !((Object) this
                instanceof PlayerModel<?> model)) {
            return;
        }

        PlayerAnimationController.afterSetupAnim(
                player,
                model
        );
    }
}
