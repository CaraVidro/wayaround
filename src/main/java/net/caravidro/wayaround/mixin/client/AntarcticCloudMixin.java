package net.caravidro.wayaround.mixin.client;

import net.caravidro.wayaround.client.AntarcticClientLighting;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
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
         * local moving cloud cells only while that world feature is active.
         * Disabled / other sky scenes must fall through to vanilla rendering.
         */
        if (minecraft.level != null
                && minecraft.level.dimension().equals(Level.OVERWORLD)
                && WorldFeatureRuntime.clientEnabled(WorldFeature.PROCEDURAL_CLOUDS)
                && !AntarcticClientLighting.isAntarctic(minecraft)
                && !net.caravidro.wayaround.daybreak.client.DaysBreakClient.active()) {
            ci.cancel();
        }
    }
}
