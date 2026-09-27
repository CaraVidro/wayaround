package net.caravidro.wayaround.mixin;

import net.caravidro.wayaround.infinity.InfinityManager;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class InfinityPushMixin {
    @Inject(method = "push(DDD)V", at = @At("HEAD"), cancellable = true)
    private void wayaround$blockImpulse(double x, double y, double z, CallbackInfo ci) {
        if (InfinityManager.protects((Entity) (Object) this)) ci.cancel();
    }
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void wayaround$blockContact(Entity other, CallbackInfo ci) {
        if (InfinityManager.protects((Entity) (Object) this) || InfinityManager.protects(other)) ci.cancel();
    }
}
