package net.caravidro.wayaround.mixin.client;

import com.mojang.blaze3d.platform.NativeImage;

import net.caravidro.wayaround.client.AntarcticClientLighting;

import net.minecraft.client.Minecraft;

import net.minecraft.client.renderer.LightTexture;


import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightTexture.class)
public abstract class AntarcticLightTextureMixin {

    /*
     * A LightTexture da 1.21.1 contém justamente
     * uma NativeImage 16x16 chamada lightPixels.
     */
    @Shadow
    @Final
    private NativeImage lightPixels;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(
            method = "updateLightTexture",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/texture/DynamicTexture;upload()V",
                    shift = At.Shift.BEFORE
            )
    )
    private void wayaround$darkPolarNight(
            float partialTicks,
            CallbackInfo ci
    ) {

        if (
                minecraft.level == null
                ||
                minecraft.player == null
        ) {
            return;
        }

        float polar =
                AntarcticClientLighting
                        .polarInfluence(
                                minecraft
                        );

        if (polar <= 0.001F) {
            return;
        }

        float darkness =
                AntarcticClientLighting
                        .getPolarDarkness(
                                minecraft.level
                        )
                        * polar;

        /*
         * Durante o dia ainda damos
         * uma leve palidez/frieza.
         */
        float night =
                AntarcticClientLighting
                        .getNightFactor(
                                minecraft.level
                        );

        /*
         * =====================================================
         * LIGHTMAP
         * =====================================================
         *
         * X = block light
         * Y = sky light
         *
         * Queremos assassinar SKY LIGHT,
         * mas preservar tochas/luz artificial.
         */

        for (
                int skyLight = 0;
                skyLight < 16;
                skyLight++
        ) {

            for (
                    int blockLight = 0;
                    blockLight < 16;
                    blockLight++
            ) {

                int color =
                        lightPixels
                                .getPixelRGBA(
                                        blockLight,
                                        skyLight
                                );

                /*
                 * NativeImage RGBA é armazenado
                 * neste int em ABGR.
                 */

                int red =
                        color
                        &
                        0xFF;

                int green =
                        (
                                color
                                >>> 8
                        )
                        &
                        0xFF;

                int blue =
                        (
                                color
                                >>> 16
                        )
                        &
                        0xFF;

                int alpha =
                        (
                                color
                                >>> 24
                        )
                        &
                        0xFF;

                /*
                 * =================================================
                 * PRESERVAR LUZ ARTIFICIAL
                 * =================================================
                 *
                 * blockLight:
                 *
                 * 0  -> quase totalmente afetado
                 * 15 -> preserva a maior parte
                 */

                float artificial =
                        blockLight
                        /
                        15.0F;

                artificial =
                        artificial
                        *
                        artificial;

                /*
                 * No auge da noite:
                 *
                 * sem iluminação:
                 * ~6% de brilho
                 *
                 * perto de tocha:
                 * mantém bastante.
                 */

                float darkFactor =
                        1.0F
                        -
                        darkness
                        *
                        (
                                1.0F
                                -
                                artificial
                                *
                                0.82F
                        );

                /*
                 * Não deixa virar literalmente 0.
                 *
                 * Queremos:
                 *
                 * "não vejo quase nada"
                 *
                 * não:
                 *
                 * "#000000 absoluto"
                 */
                darkFactor =
                        Math.max(
                                0.055F,
                                darkFactor
                        );

                /*
                 * =================================================
                 * LUZ DO DIA PÁLIDA
                 * =================================================
                 *
                 * Remove um pouquinho do amarelo/vermelho.
                 */

                float dayColdness =
                        (
                                1.0F
                                        - night
                        )
                                * polar;

                float redFactor =
                        1.0F
                        -
                        dayColdness
                        *
                        0.08F;

                float greenFactor =
                        1.0F
                        -
                        dayColdness
                        *
                        0.035F;

                float blueFactor =
                        1.0F;

                red =
                        clamp255(
                                red
                                *
                                darkFactor
                                *
                                redFactor
                        );

                green =
                        clamp255(
                                green
                                *
                                darkFactor
                                *
                                greenFactor
                        );

                blue =
                        clamp255(
                                blue
                                *
                                darkFactor
                                *
                                blueFactor
                        );

                int modified =
                        (
                                alpha
                                << 24
                        )

                        |

                        (
                                blue
                                << 16
                        )

                        |

                        (
                                green
                                << 8
                        )

                        |

                        red;

                lightPixels
                        .setPixelRGBA(
                                blockLight,
                                skyLight,
                                modified
                        );
            }
        }

    }

    private static int clamp255(
            float value
    ) {

        return Math.max(
                0,
                Math.min(
                        255,
                        Math.round(value)
                )
        );
    }
}
