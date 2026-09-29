package net.caravidro.wayaround.mixin;

import net.caravidro.wayaround.ecology.EcologyContent;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The living-ecology fishing system owns bite behavior.
 *
 * <p>Vanilla bobber physics remain intact, but vanilla's invisible fish timer
 * is disabled so fish/loot cannot be generated independently from actual fish
 * entities.</p>
 */
@Mixin(FishingHook.class)
public abstract class EcologyFishingHookMixin {

    @Inject(
            method = "catchingFish",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void wayaround$disableVanillaFishingBites(
            BlockPos pos,
            CallbackInfo ci
    ) {
        FishingHook self =
                (FishingHook) (Object) this;

        if (!WorldFeatureRuntime.enabled(
                self.level(),
                WorldFeature.LIVING_VEGETATION
        )) {
            return;
        }

        if (self.getOwner()
                instanceof Player player
                && (
                player.getMainHandItem()
                        .is(
                                EcologyContent.MAGIC_FISHING_ROD.get()
                        )
                        || player.getOffhandItem()
                        .is(
                                EcologyContent.MAGIC_FISHING_ROD.get()
                        )
        )) {
            return;
        }

        ci.cancel();
    }
}
