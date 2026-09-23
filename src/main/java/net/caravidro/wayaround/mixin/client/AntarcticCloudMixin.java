package net.caravidro.wayaround.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class AntarcticCloudMixin {
    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void wayaround$hidePolarClouds(CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();

        /*
         * Vanilla's single flat cloud sheet is replaced by Way Around's
         * local moving cloud cells throughout the Overworld.
         */
        if (minecraft.level != null
                && minecraft.level.dimension().equals(Level.OVERWORLD)) {
            ci.cancel();
        }
    }
}
