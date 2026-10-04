package net.caravidro.wayaround.mixin;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerPlayer.class)
public abstract class EntitySpectatePlayerMixin {
 @Inject(method="isSpectator",at=@At("HEAD"),cancellable=true)
 private void ghost(CallbackInfoReturnable<Boolean> ci){if(net.caravidro.wayaround.observation.EntitySpectate.ghost((ServerPlayer)(Object)this))ci.setReturnValue(true);}
}
