package net.caravidro.wayaround.mixin.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.caravidro.wayaround.ecology.client.DeepOceanClientVisibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Removes the last ambient/sky-light floor from the true abyss.
 * Artificial block light remains readable while unlit geometry reaches #000000.
 */
@Mixin(LightTexture.class)
public abstract class DeepOceanLightTextureMixin {
    @Shadow @Final
    private NativeImage lightPixels;

    @Shadow @Final
    private Minecraft minecraft;

    @Inject(
            method = "updateLightTexture",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/texture/DynamicTexture;upload()V",
                    shift = At.Shift.BEFORE
            )
    )
    private void wayaround$blackAbyss(
            float partialTicks,
            CallbackInfo ci
    ) {
        if (minecraft.level == null
                || minecraft.player == null
                || !minecraft.player.isUnderWater()) {
            return;
        }

        ClientLevel level = minecraft.level;

        if (!DeepOceanClientVisibility.isDeepOcean(
                level,
                minecraft.player.blockPosition()
        )) {
            return;
        }

        double depth =
                level.getSeaLevel()
                        - minecraft.player.getY();

        float abyss =
                net.minecraft.util.Mth.clamp(
                        (float) ((depth - 18.0) / 72.0),
                        0.0F,
                        1.0F
                );

        abyss =
                abyss
                        * abyss
                        * (3.0F - 2.0F * abyss);

        if (abyss <= 0.0F) {
            return;
        }

        for (int skyLight = 0; skyLight < 16; skyLight++) {
            for (int blockLight = 0; blockLight < 16; blockLight++) {
                int color =
                        lightPixels.getPixelRGBA(
                                blockLight,
                                skyLight
                        );

                int red = color & 0xFF;
                int green = (color >>> 8) & 0xFF;
                int blue = (color >>> 16) & 0xFF;
                int alpha = (color >>> 24) & 0xFF;

                /*
                 * At maximum depth:
                 * blockLight 0  -> literal black.
                 * blockLight 15 -> almost fully preserved.
                 *
                 * This keeps deliberately emissive/artificial sources readable
                 * while deleting the fake ambient glow of the vanilla lightmap.
                 */
                float artificial =
                        blockLight
                                / 15.0F;

                artificial *= artificial;

                float abyssFactor =
                        net.minecraft.util.Mth.lerp(
                                abyss,
                                1.0F,
                                artificial * 0.96F
                        );

                red = clamp255(red * abyssFactor);
                green = clamp255(green * abyssFactor);
                blue = clamp255(blue * abyssFactor);

                lightPixels.setPixelRGBA(
                        blockLight,
                        skyLight,
                        (alpha << 24)
                                | (blue << 16)
                                | (green << 8)
                                | red
                );
            }
        }
    }

    private static int clamp255(float value) {
        return Math.max(
                0,
                Math.min(
                        255,
                        Math.round(value)
                )
        );
    }
}
