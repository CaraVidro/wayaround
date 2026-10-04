package net.caravidro.wayaround.mixin;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class EntitySpectateEffectsMixin {
 @Inject(method="canBeAffected",at=@At("HEAD"),cancellable=true)
 private void ghost(MobEffectInstance effect,CallbackInfoReturnable<Boolean> ci){if(net.caravidro.wayaround.observation.EntitySpectate.ghost((LivingEntity)(Object)this))ci.setReturnValue(false);}
}
