package net.caravidro.wayaround.mixin;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldgen.terrain.AntarcticDensityFunction;

import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;

import org.spongepowered.asm.mixin.Mixin;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(NoiseGeneratorSettings.class)
public abstract class NoiseRouterMixin {

    private static boolean wayaround$logged =
            false;


    /*
     * =========================================================
     * NOISE ROUTER
     * =========================================================
     *
     * IMPORTANTE:
     *
     * Não podemos modificar apenas finalDensity.
     *
     * O Minecraft também usa
     * initialDensityWithoutJaggedness
     * para formar uma ideia preliminar da superfície.
     *
     * Isso influencia principalmente sistemas como:
     *
     * - aquifers
     * - nível preliminar do terreno
     * - decisões de fluidos
     *
     * Se initialDensity continuar vanilla enquanto
     * finalDensity usa nossa geografia:
     *
     * vanilla:
     * "aqui existe uma montanha"
     *
     * WayAround:
     * "aqui existe oceano"
     *
     * aquifer:
     * *entra em combustão espontânea*
     */

    @Inject(
            method = "noiseRouter",
            at = @At("RETURN"),
            cancellable = true
    )
    private void wayaround$modifyNoiseRouter(
            CallbackInfoReturnable<NoiseRouter> cir
    ) {

        NoiseRouter vanilla =
                cir.getReturnValue();


        /*
         * =====================================================
         * EVITAR WRAP DUPLO
         * =====================================================
         */

        if (
                vanilla.finalDensity()
                        instanceof AntarcticDensityFunction

                &&

                vanilla.initialDensityWithoutJaggedness()
                        instanceof AntarcticDensityFunction
        ) {

            return;
        }


        /*
         * =====================================================
         * INITIAL DENSITY
         * =====================================================
         *
         * Essa é a peça que estava faltando.
         */

        AntarcticDensityFunction antarcticInitialDensity =
                new AntarcticDensityFunction(
                        vanilla.initialDensityWithoutJaggedness()
                );


        /*
         * =====================================================
         * FINAL DENSITY
         * =====================================================
         *
         * Essa você já tinha.
         */

        AntarcticDensityFunction antarcticFinalDensity =
                new AntarcticDensityFunction(
                        vanilla.finalDensity()
                );


        /*
         * =====================================================
         * NOVO ROUTER
         * =====================================================
         */

        NoiseRouter modified =
                new NoiseRouter(

                        /*
                         * Aquifer noises vanilla.
                         */
                        vanilla.barrierNoise(),

                        vanilla.fluidLevelFloodednessNoise(),

                        vanilla.fluidLevelSpreadNoise(),

                        vanilla.lavaNoise(),


                        /*
                         * Climate.
                         */
                        vanilla.temperature(),

                        vanilla.vegetation(),


                        /*
                         * Terrain climate/noise fields.
                         */
                        vanilla.continents(),

                        vanilla.erosion(),

                        vanilla.depth(),

                        vanilla.ridges(),


                        /*
                         * =================================================
                         * PRELIMINARY TERRAIN
                         * =================================================
                         *
                         * ANTES:
                         *
                         * vanilla.initialDensityWithoutJaggedness()
                         *
                         *
                         * AGORA:
                         *
                         * WayAround também controla.
                         */

                        antarcticInitialDensity,


                        /*
                         * =================================================
                         * FINAL TERRAIN
                         * =================================================
                         */

                        antarcticFinalDensity,


                        /*
                         * Vanilla ores.
                         */
                        vanilla.veinToggle(),

                        vanilla.veinRidged(),

                        vanilla.veinGap()
                );


        cir.setReturnValue(
                modified
        );


        /*
         * =====================================================
         * DEBUG
         * =====================================================
         */

        if (
                !wayaround$logged
        ) {

            wayaround$logged =
                    true;


            WayAround.LOGGER.info(
                    "WayAround substituiu initialDensityWithoutJaggedness + finalDensity!"
            );
        }
    }
}