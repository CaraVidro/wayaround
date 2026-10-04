package net.caravidro.wayaround.worldgen;

import com.mojang.serialization.MapCodec;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldgen.feature.AntarcticIcebergFeature;
import net.caravidro.wayaround.worldgen.terrain.AntarcticDensityFunction;
import net.caravidro.wayaround.worldgen.terrain.VolcanicDensityFunction;
import net.caravidro.wayaround.worldgen.terrain.GreatRiftDensityFunction;

import net.minecraft.core.registries.Registries;

import net.minecraft.world.level.levelgen.DensityFunction;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import net.neoforged.bus.api.IEventBus;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;


public final class WorldgenRegistry {

    private static final DeferredRegister<MapCodec<? extends net.minecraft.world.level.chunk.ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR,WayAround.MODID);
    public static final Supplier<MapCodec<net.caravidro.wayaround.nexus.world.NexusChunkGenerator>> NEXUS_COMPLEX =
            CHUNK_GENERATORS.register("nexus_complex",()->net.caravidro.wayaround.nexus.world.NexusChunkGenerator.CODEC);

    public static final Supplier<MapCodec<net.caravidro.wayaround.littleleaf.world.ColonyChunkGenerator>> LITTLE_LEAF =
            CHUNK_GENERATORS.register("little_leaf_colony",()->net.caravidro.wayaround.littleleaf.world.ColonyChunkGenerator.CODEC);

    /*
     * =========================================================
     * DENSITY FUNCTIONS
     * =========================================================
     */

    public static final DeferredRegister<
            MapCodec<? extends DensityFunction>
    > DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(
                    Registries.DENSITY_FUNCTION_TYPE,
                    WayAround.MODID
            );


    public static final Supplier<
            MapCodec<? extends DensityFunction>
    > ANTARCTIC_DENSITY =
            DENSITY_FUNCTION_TYPES.register(
                    "antarctic_density",

                    () ->
                            AntarcticDensityFunction.DATA_CODEC
            );

    public static final Supplier<
            MapCodec<? extends DensityFunction>
    > VOLCANIC_DENSITY =
            DENSITY_FUNCTION_TYPES.register(
                    "volcanic_density",
                    () -> VolcanicDensityFunction.DATA_CODEC
            );

    public static final Supplier<
            MapCodec<? extends DensityFunction>
    > GREAT_RIFT_DENSITY =
            DENSITY_FUNCTION_TYPES.register(
                    "great_rift_density",
                    () -> GreatRiftDensityFunction.DATA_CODEC
            );


    /*
     * =========================================================
     * FEATURES
     * =========================================================
     *
     * Aqui registramos nossos tipos de Feature.
     *
     * O JSON configured_feature depois aponta para:
     *
     * wayaround:antarctic_iceberg
     */

    public static final DeferredRegister<
            Feature<?>
    > FEATURES =
            DeferredRegister.create(
                    Registries.FEATURE,
                    WayAround.MODID
            );


    /*
     * =========================================================
     * ANTARCTIC ICEBERG
     * =========================================================
     */

    public static final DeferredHolder<
            Feature<?>,
            AntarcticIcebergFeature
    > ANTARCTIC_ICEBERG =

            FEATURES.register(
                    "antarctic_iceberg",

                    () ->
                            new AntarcticIcebergFeature(
                                    NoneFeatureConfiguration.CODEC
                            )
            );


    /*
     * =========================================================
     * CONSTRUCTOR
     * =========================================================
     */

    private WorldgenRegistry() {
    }


    /*
     * =========================================================
     * REGISTER
     * =========================================================
     */

    public static final net.neoforged.neoforge.registries.DeferredHolder<MapCodec<? extends net.minecraft.world.level.levelgen.DensityFunction>, MapCodec<net.caravidro.wayaround.worldgen.terrain.OceanContinentalness>> OCEAN_SCALE =
            DENSITY_FUNCTION_TYPES.register("ocean_continentalness", () -> net.caravidro.wayaround.worldgen.terrain.OceanContinentalness.DATA_CODEC);

    public static void register(
            IEventBus bus
    ) {
        CHUNK_GENERATORS.register(bus);

        /*
         * Density Function customizada.
         */

        DENSITY_FUNCTION_TYPES.register(
                bus
        );


        /*
         * Features customizadas.
         *
         * MUITO IMPORTANTE:
         *
         * sem isso, o JSON:
         *
         * "type": "wayaround:antarctic_iceberg"
         *
         * não vai encontrar nossa feature.
         */

        FEATURES.register(
                bus
        );
    }
}
