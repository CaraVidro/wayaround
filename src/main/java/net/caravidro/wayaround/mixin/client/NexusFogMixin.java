package net.caravidro.wayaround.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;

import net.caravidro.wayaround.nexus.NexusPortalManager;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The Nexus is intentionally almost black. The biome already has black fog;
 * this pins the final shader fog too so sky/effect modifiers cannot wash the
 * cavern into blue/gray.
 */
@Mixin(value=FogRenderer.class,priority=900)
public abstract class NexusFogMixin {

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
                && camera.getFluidInCamera()==net.minecraft.world.level.material.FogType.NONE) {
            boolean day=net.caravidro.wayaround.daybreak.client.DaysBreakClient.day();
            float r=day?.86F:0F,g=day?.008F:0F,b=day?.004F:0F;
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

        RenderSystem.setShaderFogColor(
                0.002F,
                0.002F,
                0.003F,
                1.0F
        );
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

        RenderSystem.setShaderFogStart(
                5.0F
        );

        RenderSystem.setShaderFogEnd(
                Math.min(
                        farPlaneDistance,
                        196.0F
                )
        );
    }
}
