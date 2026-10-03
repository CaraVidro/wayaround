package net.caravidro.wayaround.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;

import net.caravidro.wayaround.nexus.NexusPortalManager;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Dark red atmospheric haze for the fractured Nexus exterior. Underwater fog
 * retains its separate fluid treatment.
 */
@Mixin(value=FogRenderer.class,priority=900)
public abstract class NexusFogMixin {
    @Shadow private static float fogRed;
    @Shadow private static float fogGreen;
    @Shadow private static float fogBlue;

    @Inject(
            method = "setupColor",
            at = @At("TAIL")
    )
    private static void wayaround$nexusFogColor(
            Camera camera,
            float partialTick,
            ClientLevel level,
            int renderDistanceChunks,
            float bossColorModifier,
            CallbackInfo ci
    ) {
        if (net.caravidro.wayaround.daybreak.client.DaysBreakClient.active()
                && (camera.getFluidInCamera()==net.minecraft.world.level.material.FogType.NONE
                    || !net.caravidro.wayaround.daybreak.client.DaysBreakClient.day()
                        && camera.getFluidInCamera()==net.minecraft.world.level.material.FogType.WATER)) {
            boolean day=net.caravidro.wayaround.daybreak.client.DaysBreakClient.day();
            float r=day?.86F:0F,g=day?.008F:0F,b=day?.004F:0F;
            fogRed=r;fogGreen=g;fogBlue=b;
            RenderSystem.setShaderFogColor(r,g,b,1);
            RenderSystem.clearColor(r,g,b,0);
            return;
        }
        if (!level.dimension()
                .equals(
                        NexusPortalManager.NEXUS
        )) {
            return;
        }
        if(camera.getFluidInCamera()!=net.minecraft.world.level.material.FogType.NONE)return;
        fogRed=.095F;fogGreen=.009F;fogBlue=.018F;
        RenderSystem.setShaderFogColor(
                fogRed,
                fogGreen,
                fogBlue,
                1.0F
        );
        RenderSystem.clearColor(fogRed,fogGreen,fogBlue,0);
    }

    @Inject(
            method = "setupFog",
            at = @At("TAIL")
    )
    private static void wayaround$nexusFogDistance(
            Camera camera,
            FogRenderer.FogMode mode,
            float farPlaneDistance,
            boolean thickFog,
            float partialTick,
            CallbackInfo ci
    ) {
        if (camera.getEntity() == null
                || !camera.getEntity()
                .level()
                .dimension()
                .equals(
                        NexusPortalManager.NEXUS
        )) {
            return;
        }
        if(camera.getFluidInCamera()!=net.minecraft.world.level.material.FogType.NONE)return;
        RenderSystem.setShaderFogStart(
                Math.min(farPlaneDistance*.55F,140.0F)
        );

        RenderSystem.setShaderFogEnd(
                Math.min(
                        farPlaneDistance,
                        448.0F
                )
        );
    }
}
