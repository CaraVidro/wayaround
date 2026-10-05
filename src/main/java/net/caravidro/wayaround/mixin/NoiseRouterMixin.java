package net.caravidro.wayaround.mixin;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldgen.terrain.AntarcticDensityFunction;
import net.caravidro.wayaround.worldgen.terrain.VolcanicDensityFunction;
import net.caravidro.wayaround.worldgen.terrain.GreatRiftDensityFunction;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;

import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(NoiseGeneratorSettings.class)
public abstract class NoiseRouterMixin {
    // Atomic cache entry: source router, feature flags, transformed router.
    // Settings are immutable; only feature configuration invalidates this map.
    @Unique private volatile Object[] wayaround$routerCache;


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

        boolean riftEnabled =
                WorldFeatureRuntime.serverEnabled(
                        WorldFeature.GREAT_RIFTS
                );

        NoiseGeneratorSettings settings=(NoiseGeneratorSettings)(Object)this;
        if(settings.seaLevel()!=63||!settings.defaultBlock().is(net.minecraft.world.level.block.Blocks.STONE)
                ||!settings.defaultFluid().is(net.minecraft.world.level.block.Blocks.WATER))return;
        boolean large=WorldFeatureRuntime.serverEnabled(WorldFeature.LARGE_GEOGRAPHY);
        boolean finite=WorldFeatureRuntime.serverEnabled(WorldFeature.FINITE_WORLD);
        if (!antarcticaEnabled && !volcanicEnabled && !riftEnabled && !large && !finite
                && !WorldFeatureRuntime.serverEnabled(WorldFeature.LIVING_VEGETATION))return;

        NoiseRouter source = cir.getReturnValue();
        int flags = (antarcticaEnabled ? 1 : 0) | (volcanicEnabled ? 2 : 0) | (riftEnabled ? 4 : 0)
                | (WorldFeatureRuntime.serverEnabled(WorldFeature.LIVING_VEGETATION) ? 8 : 0) | (large?16:0) | (finite?32:0);
        Object[] cached = wayaround$routerCache;
        if (cached != null && cached[0] == source && ((Integer) cached[1]) == flags) {
            cir.setReturnValue((NoiseRouter) cached[2]);
            return;
        }
        NoiseRouter vanilla = source;
        if(large||finite)vanilla=net.caravidro.wayaround.worldgen.planet.GeographicNoise.transform(vanilla,large,finite);
        else if (WorldFeatureRuntime.serverEnabled(WorldFeature.LIVING_VEGETATION)) {
            vanilla = net.caravidro.wayaround.worldgen.terrain.OceanContinentalness.scale(vanilla);
            cir.setReturnValue(vanilla);
        }


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

        boolean alreadyRift =
                !riftEnabled
                        || (
                        vanilla.finalDensity()
                                instanceof GreatRiftDensityFunction
                                && vanilla.initialDensityWithoutJaggedness()
                                instanceof GreatRiftDensityFunction
                );

        if (!large && alreadyAntarctic
                && alreadyVolcanic
                && alreadyRift) {
            wayaround$routerCache = new Object[]{source, flags, vanilla};
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

        if(large) {
            initialDensity=new net.caravidro.wayaround.worldgen.planet.RegionalTerrainDensity(initialDensity,vanilla.continents(),vanilla.erosion(),vanilla.ridges(),true);
            finalDensity=new net.caravidro.wayaround.worldgen.planet.RegionalTerrainDensity(finalDensity,vanilla.continents(),vanilla.erosion(),vanilla.ridges(),false);
        }
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

        if (riftEnabled
                && !(initialDensity
                instanceof GreatRiftDensityFunction)) {
            initialDensity =
                    new GreatRiftDensityFunction(
                            initialDensity
                    );
        }

        if (riftEnabled
                && !(finalDensity
                instanceof GreatRiftDensityFunction)) {
            finalDensity =
                    new GreatRiftDensityFunction(
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
        wayaround$routerCache = new Object[]{source, flags, modified};


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
                    "WayAround instalou terrain wrappers: Antarctica={} Volcanic={} GreatRift={}",
                    antarcticaEnabled,
                    volcanicEnabled,
                    riftEnabled
            );
        }
    }
}