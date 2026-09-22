package net.caravidro.wayaround.mixin.client;

import net.caravidro.wayaround.client.AntarcticClientLighting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class AntarcticCloudMixin {
    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void wayaround$hidePolarClouds(CallbackInfo ci) {
        if (AntarcticClientLighting.isAntarctic(Minecraft.getInstance())) ci.cancel();
    }
}
