package net.caravidro.wayaround.mixin.client;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(AbstractClientPlayer.class)
public abstract class EntitySpectatePlayerMixin {
 @Inject(method="isSpectator",at=@At("HEAD"),cancellable=true)
 private void ghost(CallbackInfoReturnable<Boolean> ci){if(net.caravidro.wayaround.observation.EntitySpectate.ghost((AbstractClientPlayer)(Object)this))ci.setReturnValue(true);}
}
