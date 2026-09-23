package net.caravidro.wayaround.worldgen;
import net.caravidro.wayaround.worldgen.feature.AntarcticGlacialBodyFeature;
import net.caravidro.wayaround.worldgen.feature.AntarcticCrevasseFeature;
import net.caravidro.wayaround.worldgen.feature.AntarcticRichOreFeature;
import net.caravidro.wayaround.worldgen.feature.PrioriteCraterFeature;
import net.caravidro.wayaround.worldgen.feature.LivingVegetationFeature;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldgen.feature.AntarcticSnowMoundFeature;

import net.minecraft.core.registries.Registries;


import net.neoforged.bus.api.IEventBus;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WayAroundFeatures {

    public static final DeferredRegister<Feature<?>>
            FEATURES =
            DeferredRegister.create(
                    Registries.FEATURE,
                    WayAround.MODID
            );

    public static final DeferredHolder<
            Feature<?>,
            AntarcticSnowMoundFeature
    > ANTARCTIC_SNOW_MOUNDS =
            FEATURES.register(
                    "antarctic_snow_mounds",
                    () -> new AntarcticSnowMoundFeature(
                            NoneFeatureConfiguration.CODEC
                    )
            );
    public static final DeferredHolder<
            Feature<?>,
            AntarcticGlacialBodyFeature
            > ANTARCTIC_GLACIAL_BODY =

            FEATURES.register(
                    "antarctic_glacial_body",

                    () ->
                            new AntarcticGlacialBodyFeature(
                                    NoneFeatureConfiguration.CODEC
                            )
            );


    public static final DeferredHolder<
            Feature<?>,
            AntarcticCrevasseFeature
            > ANTARCTIC_CREVASSE =

            FEATURES.register(
                    "antarctic_crevasse",

                    () ->
                            new AntarcticCrevasseFeature(
                                    NoneFeatureConfiguration.CODEC
                            )
            );


    public static final DeferredHolder<
            Feature<?>,
            AntarcticRichOreFeature
            > ANTARCTIC_RICH_ORES =

            FEATURES.register(
                    "antarctic_rich_ores",

                    () ->
                            new AntarcticRichOreFeature(
                                    NoneFeatureConfiguration.CODEC
                            )
            );
    public static final DeferredHolder<Feature<?>, LivingVegetationFeature> LIVING_VEGETATION =
            FEATURES.register(
                    "living_vegetation",
                    () -> new LivingVegetationFeature(NoneFeatureConfiguration.CODEC)
            );

    public static final DeferredHolder<Feature<?>, PrioriteCraterFeature> PRIORITE_CRATER =
            FEATURES.register(
                    "priorite_crater",
                    () -> new PrioriteCraterFeature(NoneFeatureConfiguration.CODEC)
            );

    private WayAroundFeatures() {
    }

    public static void register(
            IEventBus modEventBus
    ) {
        FEATURES.register(modEventBus);
    }
}
