package net.caravidro.wayaround.worldgen;

import net.caravidro.wayaround.WayAround;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

public final class WayAroundBiomes {

    /*
     * =========================================================
     * BIOME KEYS
     * =========================================================
     */

    public static final ResourceKey<Biome> ANTARCTIC_ICE_SHEET =
            ResourceKey.create(
                    Registries.BIOME,
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "antarctic_ice_sheet"
                    )
            );


    public static final ResourceKey<Biome> SOUTHERN_OCEAN =
            ResourceKey.create(
                    Registries.BIOME,
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "southern_ocean"
                    )
            );


    public static final ResourceKey<Biome> VOLCANIC_HIGHLANDS =
            ResourceKey.create(
                    Registries.BIOME,
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "volcanic_highlands"
                    )
            );


    /*
     * =========================================================
     * HOLDERS
     * =========================================================
     *
     * Eles só existem depois que o servidor carrega
     * o registry real dos biomas.
     */

    private static Holder<Biome> antarcticIceSheet;

    private static Holder<Biome> southernOcean;

    private static Holder<Biome> volcanicHighlands;


    /*
     * =========================================================
     * CONSTRUCTOR
     * =========================================================
     */

    private WayAroundBiomes() {
    }


    /*
     * =========================================================
     * SERVER REGISTRY
     * =========================================================
     */

    public static void onServerAboutToStart(
            ServerAboutToStartEvent event
    ) {

        Registry<Biome> biomeRegistry =
                event.getServer()
                        .registryAccess()
                        .registryOrThrow(
                                Registries.BIOME
                        );


        /*
         * -----------------------------------------------------
         * ANTARCTIC ICE SHEET
         * -----------------------------------------------------
         */

        antarcticIceSheet =
                biomeRegistry.getHolderOrThrow(
                        ANTARCTIC_ICE_SHEET
                );


        /*
         * -----------------------------------------------------
         * SOUTHERN OCEAN
         * -----------------------------------------------------
         */

        southernOcean =
                biomeRegistry.getHolderOrThrow(
                        SOUTHERN_OCEAN
                );


        volcanicHighlands =
                biomeRegistry.getHolderOrThrow(
                        VOLCANIC_HIGHLANDS
                );


        WayAround.LOGGER.info(
                "WayAround biomes capturados no registry REAL do servidor!"
        );

        WayAround.LOGGER.info(
                " - Antarctic Ice Sheet: OK"
        );

        WayAround.LOGGER.info(
                " - Southern Ocean: OK"
        );

        WayAround.LOGGER.info(
                " - Volcanic Highlands: OK"
        );
    }


    /*
     * =========================================================
     * GET ANTARCTIC
     * =========================================================
     */

    public static Holder<Biome> getAntarcticIceSheet() {

        if (
                antarcticIceSheet == null
        ) {

            throw new IllegalStateException(
                    "Antarctic Ice Sheet biome holder ainda não foi inicializado."
            );
        }

        return antarcticIceSheet;
    }


    /*
     * Alias opcional.
     *
     * Isso é útil porque alguns dos códigos que fomos
     * escrevendo usam getAntarctic().
     *
     * Assim os dois nomes funcionam.
     */

    public static Holder<Biome> getAntarctic() {

        return getAntarcticIceSheet();
    }


    /*
     * =========================================================
     * GET SOUTHERN OCEAN
     * =========================================================
     */

    public static Holder<Biome> getSouthernOcean() {

        if (
                southernOcean == null
        ) {

            throw new IllegalStateException(
                    "Southern Ocean biome holder ainda não foi inicializado."
            );
        }

        return southernOcean;
    }


    public static Holder<Biome> getVolcanicHighlands() {

        if (
                volcanicHighlands == null
        ) {

            throw new IllegalStateException(
                    "Volcanic Highlands biome holder ainda não foi inicializado."
            );
        }

        return volcanicHighlands;
    }


    /*
     * =========================================================
     * READY
     * =========================================================
     */

    public static boolean isReady() {

        return antarcticIceSheet != null
                &&
                southernOcean != null
                &&
                volcanicHighlands != null;
    }


    /*
     * =========================================================
     * CLEAR
     * =========================================================
     */

    public static void clear() {

        antarcticIceSheet = null;

        southernOcean = null;

        volcanicHighlands = null;
    }
}