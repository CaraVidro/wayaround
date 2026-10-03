package net.caravidro.wayaround.mixin.client;

import net.caravidro.wayaround.daybreak.client.DaysBreakClient;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Event sky replaces vanilla stars, sunrise band, sun and clouds as one unit. */
@Mixin(LevelRenderer.class)
public abstract class DaysBreakSkyMixin {
    @Inject(method="renderSky",at=@At("HEAD"),cancellable=true)
    private void wayaround$daysBreakSky(Matrix4f view,Matrix4f projection,float partial,
            Camera camera,boolean foggy,Runnable setupFog,CallbackInfo ci) {
        if(DaysBreakClient.active()){setupFog.run();ci.cancel();}
    }
    @Inject(method="renderClouds",at=@At("HEAD"),cancellable=true)
    private void wayaround$daysBreakClouds(CallbackInfo ci) {
        if(DaysBreakClient.active())ci.cancel();
    }
}
