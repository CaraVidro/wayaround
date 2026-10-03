package net.caravidro.wayaround.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.caravidro.wayaround.ecology.client.DeepOceanClientVisibility;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Smooth abyss darkness. This replaces the old Night Vision/Darkness fight with
 * one cheap depth curve and lets the capsule preserve a small lit viewing range.
 */
@Mixin(FogRenderer.class)
public abstract class DeepOceanFogMixin {
    @Inject(method = "setupColor", at = @At("TAIL"))
    private static void wayaround$deepOceanColor(
            Camera camera,
            float partialTick,
            ClientLevel level,
            int renderDistanceChunks,
            float bossColorModifier,
            CallbackInfo ci
    ) {
        if (camera.getEntity() == null
                || !DeepOceanClientVisibility.submerged(camera.getEntity(), level, camera.getPosition())
                || !DeepOceanClientVisibility.isDeepOcean(level, camera.getBlockPosition())) {
            return;
        }

        double depth = level.getSeaLevel() - camera.getPosition().y;
        /*
         * At true abyss depth there is no blue-grey safety floor: the ambient
         * ocean light reaches literal black. The transition still starts gently
         * so descending does not look like crossing a shader wall.
         */
        float t = Mth.clamp((float) ((depth - 12.0) / 36.0), 0.0F, 1.0F);
        t = t * t * (3.0F - 2.0F * t);

        // A lamp lights real nearby geometry, never the water's ambient fog.
        float r = Mth.lerp(t, 0.030F, 0.0F);
        float g = Mth.lerp(t, 0.060F, 0.0F);
        float b = Mth.lerp(t, 0.080F, 0.0F);
        RenderSystem.setShaderFogColor(r, g, b, 1.0F);
    }

    @Inject(method = "setupFog", at = @At("TAIL"))
    private static void wayaround$deepOceanDistance(
            Camera camera,
            FogRenderer.FogMode mode,
            float farPlaneDistance,
            boolean thickFog,
            float partialTick,
            CallbackInfo ci
    ) {
        if (camera.getEntity() == null
                || !(camera.getEntity().level() instanceof ClientLevel level)
                || !DeepOceanClientVisibility.submerged(camera.getEntity(), level, camera.getPosition())
                || !DeepOceanClientVisibility.isDeepOcean(level, camera.getBlockPosition())) {
            return;
        }

        double depth = level.getSeaLevel() - camera.getPosition().y;
        float end = Math.min(
                farPlaneDistance,
                DeepOceanClientVisibility.fogEnd(
                        depth,
                        camera.getEntity()
                )
        );

        RenderSystem.setShaderFogStart(Math.max(0.0F, end * 0.12F));
        RenderSystem.setShaderFogEnd(end);

        float black =
                Mth.clamp(
                        (float) ((depth - 12.0) / 36.0),
                        0.0F,
                        1.0F
                );

        black =
                black
                        * black
                        * (3.0F - 2.0F * black);

        if (black >= 0.995F) {
            RenderSystem.setShaderFogColor(
                    0.0F,
                    0.0F,
                    0.0F,
                    1.0F
            );
        }
    }
}
