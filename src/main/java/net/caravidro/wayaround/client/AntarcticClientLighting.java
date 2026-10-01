package net.caravidro.wayaround.client;

import net.caravidro.wayaround.worldgen.WayAroundBiomes;
import net.caravidro.wayaround.worldgen.geography.AntarcticField;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import net.minecraft.core.BlockPos;

public final class AntarcticClientLighting {

    private AntarcticClientLighting() {
    }

    /*
     * =========================================================
     * ESTÁ NA ANTÁRTICA?
     * =========================================================
     */

    public static boolean isAntarctic(
            Minecraft minecraft
    ) {

        if (
                minecraft.level == null
                ||
                minecraft.player == null
        ) {
            return false;
        }

        return polarInfluence(
                minecraft
        ) >= 0.92F;
    }

    public static boolean isAntarctic(
            ClientLevel level,
            BlockPos pos
    ) {

        return polarInfluence(
                level,
                pos
        ) >= 0.92F;
    }

    public static float polarInfluence(
            Minecraft minecraft
    ) {
        if (minecraft.level == null
                || minecraft.player == null) {
            return 0.0F;
        }

        return polarInfluence(
                minecraft.level,
                minecraft.player.blockPosition()
        );
    }

    public static float polarInfluence(
            ClientLevel level,
            BlockPos pos
    ) {
        double field =
                AntarcticField.polarInfluence(
                        pos.getX(),
                        pos.getZ()
                );

        if (level.getBiome(
                pos
        ).is(
                WayAroundBiomes.ANTARCTIC_ICE_SHEET
        )) {
            field =
                    Math.max(
                            field,
                            1.0
                    );
        }

        return (float) Math.max(
                0.0,
                Math.min(
                        1.0,
                        field
                )
        );
    }


    /*
     * =========================================================
     * INTENSIDADE DA NOITE
     * =========================================================
     *
     * 0 = dia
     * 1 = noite profunda
     *
     * Minecraft:
     *
     *  6000  ~ meio-dia
     * 12000  ~ pôr do sol
     * 18000  ~ meia-noite
     * 23000  ~ amanhecendo
     */

    public static float getNightFactor(
            ClientLevel level
    ) {

        long time =
                Math.floorMod(
                        level.getDayTime(),
                        24000L
                );

        /*
         * Anoitecer.
         */
        if (
                time >= 11500L
                &&
                time < 14500L
        ) {

            return smoothstep(
                    (time - 11500L)
                    /
                    3000.0F
            );
        }

        /*
         * Noite profunda.
         */
        if (
                time >= 14500L
                &&
                time <= 21500L
        ) {

            return 1.0F;
        }

        /*
         * Amanhecer.
         */
        if (
                time > 21500L
                &&
                time <= 23500L
        ) {

            return 1.0F
                    -
                    smoothstep(
                            (time - 21500L)
                            /
                            2000.0F
                    );
        }

        return 0.0F;
    }

    /*
     * =========================================================
     * LUZ POLAR
     * =========================================================
     */

    public static float getPolarDarkness(
            ClientLevel level
    ) {

        float night =
                getNightFactor(level);

        /*
         * 0 durante o dia
         *
         * 0.94 no auge da noite.
         *
         * Resultado:
         * praticamente sem luz natural.
         */
        return night
                *
                0.94F;
    }

    private static float smoothstep(
            float value
    ) {

        value =
                Math.max(
                        0.0F,
                        Math.min(
                                1.0F,
                                value
                        )
                );

        return value
                *
                value
                *
                (
                        3.0F
                        -
                        2.0F
                        *
                        value
                );
    }
}