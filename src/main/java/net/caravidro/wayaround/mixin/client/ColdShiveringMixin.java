package net.caravidro.wayaround.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.caravidro.wayaround.client.ColdClientEffects;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class ColdShiveringMixin {
    @Inject(method = "setupRotations", at = @At("HEAD"))
    private void wayaround$shiver(LivingEntity entity, PoseStack poses, float bob,
            float bodyYaw, float partialTick, float scale, CallbackInfo ci) {
        float strength = ColdClientEffects.strength(entity);
        if (strength > 0) {
            float rotation = (float) Math.sin((entity.tickCount + partialTick) * 2.7) * strength * 0.6F;
            poses.mulPose(Axis.ZP.rotationDegrees(rotation));
        }
    }
}
