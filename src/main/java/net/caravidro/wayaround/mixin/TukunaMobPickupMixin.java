package net.caravidro.wayaround.mixin;

import net.caravidro.wayaround.cursed.TukunaFingerWorld;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Zombies and other mobs cannot steal an unbound finger from a player. */
@Mixin(Mob.class)
public abstract class TukunaMobPickupMixin {
    @Inject(method = "pickUpItem", at = @At("HEAD"), cancellable = true)
    private void wayaround$playersOnlyFinger(ItemEntity item, CallbackInfo ci) {
        if (TukunaFingerWorld.isFinger(item.getItem())) ci.cancel();
    }
}
