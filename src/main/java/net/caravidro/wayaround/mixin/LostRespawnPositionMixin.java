package net.caravidro.wayaround.mixin;

import net.caravidro.wayaround.worldgen.planet.LostRespawnService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class LostRespawnPositionMixin {
    @Inject(method="findRespawnPositionAndUseSpawnBlock",at=@At("HEAD"),cancellable=true)
    private void wayaround$alreadyPreparedLand(CallbackInfoReturnable<DimensionTransition> cir) {
        var transition=LostRespawnService.preparedTransition((ServerPlayer)(Object)this);
        if(transition!=null)cir.setReturnValue(transition); // Do not load/consume a remote bed/anchor before overriding it.
    }
}
