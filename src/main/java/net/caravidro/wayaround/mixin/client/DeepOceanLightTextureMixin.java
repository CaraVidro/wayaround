package net.caravidro.wayaround.mixin.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.caravidro.wayaround.ecology.DeepSeaSubmarineEntity;
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
                || !DeepOceanClientVisibility.submerged(minecraft.player, minecraft.level, minecraft.gameRenderer.getMainCamera().getPosition())) {
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
                        (float) ((depth - 10.0) / 42.0),
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

        float lamp =
                DeepOceanClientVisibility.vehicleLampStrength(
                        minecraft.player
                );

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
                if (minecraft.player.getVehicle() instanceof net.caravidro.wayaround.ecology.AbyssVehicleEntity craft
                        && craft.pressureExposure() >= net.caravidro.wayaround.ecology.OceanPressure.CRITICAL
                        && minecraft.level.getGameTime() % 180 < 6) artificial *= .65F;

                /*
                 * The submarine no longer gets a global fake brightness
                 * floor. Its moving vanilla LIGHT blocks now feed the real
                 * blockLight channel below. Keep the old soft floor only for
                 * the descent capsule until it receives the same treatment.
                 */
                boolean realSubmarineLight =
                        minecraft.player.getVehicle()
                                instanceof DeepSeaSubmarineEntity;

                // Both pressure craft now use real, stable moving light sources.
                float lampFloor = 0.0F;

                float visibleLight =
                        Math.max(
                                lampFloor,
                                artificial * 0.96F
                        );

                float abyssFactor =
                        net.minecraft.util.Mth.lerp(
                                abyss,
                                1.0F,
                                visibleLight
                        );

                red = wayaround$abyssClamp255(red * abyssFactor);
                green = wayaround$abyssClamp255(green * abyssFactor);
                blue = wayaround$abyssClamp255(blue * abyssFactor);

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

    private static int wayaround$abyssClamp255(float value) {
        return Math.max(
                0,
                Math.min(
                        255,
                        Math.round(value)
                )
        );
    }
}
