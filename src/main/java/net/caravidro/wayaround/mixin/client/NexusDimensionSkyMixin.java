package net.caravidro.wayaround.mixin.client;

import net.caravidro.wayaround.nexus.NexusPortalManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class NexusDimensionSkyMixin {
    @Inject(method="renderSky",at=@At("HEAD"),cancellable=true)
    private void wayaround$fracturedSky(Matrix4f view,Matrix4f projection,float partial,Camera camera,boolean foggy,Runnable setupFog,CallbackInfo ci) {
        var level=Minecraft.getInstance().level;
        if(level!=null&&level.dimension().equals(NexusPortalManager.NEXUS)){setupFog.run();ci.cancel();}
    }
    @Inject(method="renderClouds",at=@At("HEAD"),cancellable=true)
    private void wayaround$noNexusClouds(CallbackInfo ci){var level=Minecraft.getInstance().level;if(level!=null&&level.dimension().equals(NexusPortalManager.NEXUS))ci.cancel();}
}
