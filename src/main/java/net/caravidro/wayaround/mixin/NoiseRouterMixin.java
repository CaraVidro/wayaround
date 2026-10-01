package net.caravidro.wayaround.mixin;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldgen.terrain.AntarcticDensityFunction;
import net.caravidro.wayaround.worldgen.terrain.VolcanicDensityFunction;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;

import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.DensityFunction;
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
        boolean antarcticaEnabled =
                WorldFeatureRuntime.serverEnabled(
                        WorldFeature.ANTARCTICA
                );

        boolean volcanicEnabled =
                WorldFeatureRuntime.serverEnabled(
                        WorldFeature.VOLCANIC_REGIONS
                );

        if (!antarcticaEnabled
                && !volcanicEnabled) {
            return;
        }

        NoiseRouter vanilla =
                cir.getReturnValue();


        /*
         * Avoid repeatedly wrapping the same router when another system asks
         * NoiseGeneratorSettings for it again.
         */
        boolean alreadyAntarctic =
                !antarcticaEnabled
                        || (
                        vanilla.finalDensity()
                                instanceof AntarcticDensityFunction
                                && vanilla.initialDensityWithoutJaggedness()
                                instanceof AntarcticDensityFunction
                );

        boolean alreadyVolcanic =
                !volcanicEnabled
                        || (
                        vanilla.finalDensity()
                                instanceof VolcanicDensityFunction
                                && vanilla.initialDensityWithoutJaggedness()
                                instanceof VolcanicDensityFunction
                );

        if (alreadyAntarctic
                && alreadyVolcanic) {
            return;
        }


        /*
         * =====================================================
         * INITIAL DENSITY
         * =====================================================
         *
         * Essa é a peça que estava faltando.
         */

        DensityFunction initialDensity =
                vanilla.initialDensityWithoutJaggedness();

        DensityFunction finalDensity =
                vanilla.finalDensity();

        if (antarcticaEnabled
                && !(initialDensity
                instanceof AntarcticDensityFunction)) {
            initialDensity =
                    new AntarcticDensityFunction(
                            initialDensity
                    );
        }

        if (antarcticaEnabled
                && !(finalDensity
                instanceof AntarcticDensityFunction)) {
            finalDensity =
                    new AntarcticDensityFunction(
                            finalDensity
                    );
        }

        /*
         * Volcanoes wrap the already-polar-aware density. The two regions are
         * geographically exclusive, but this order lets both systems coexist
         * without either one discarding the other's terrain changes.
         */
        if (volcanicEnabled
                && !(initialDensity
                instanceof VolcanicDensityFunction)) {
            initialDensity =
                    new VolcanicDensityFunction(
                            initialDensity
                    );
        }

        if (volcanicEnabled
                && !(finalDensity
                instanceof VolcanicDensityFunction)) {
            finalDensity =
                    new VolcanicDensityFunction(
                            finalDensity
                    );
        }


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

                        initialDensity,


                        /*
                         * =================================================
                         * FINAL TERRAIN
                         * =================================================
                         */

                        finalDensity,


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
                    "WayAround instalou terrain wrappers: Antarctica={} Volcanic={}",
                    antarcticaEnabled,
                    volcanicEnabled
            );
        }
    }
}