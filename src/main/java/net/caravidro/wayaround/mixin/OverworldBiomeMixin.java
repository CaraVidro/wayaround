package net.caravidro.wayaround.mixin;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldgen.WayAroundBiomes;
import net.caravidro.wayaround.worldgen.geography.AntarcticField;
import net.caravidro.wayaround.worldgen.geography.VolcanicField;
import net.caravidro.wayaround.worldgen.geography.GreatRiftField;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;

import org.spongepowered.asm.mixin.Mixin;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiNoiseBiomeSource.class)
public abstract class OverworldBiomeMixin {

    /*
     * =========================================================
     * DEBUG
     * =========================================================
     *
     * Só usamos isso para não lotar o log.
     */

    private static boolean wayaround$loggedAntarctica =
            false;

    private static boolean wayaround$loggedSouthernOcean =
            false;

    private static boolean wayaround$loggedVolcanic =
            false;

    private static boolean wayaround$loggedGreatRift =
            false;


    /*
     * =========================================================
     * BIOME SELECTION
     * =========================================================
     *
     * Interceptamos a escolha de bioma feita pelo
     * MultiNoiseBiomeSource.
     *
     * A ordem é:
     *
     * 1. Antártida
     * 2. Southern Ocean
     * 3. Caso contrário, deixa o Minecraft escolher normalmente.
     */

    @Inject(
            method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void wayaround$selectPolarBiome(
            int quartX,
            int quartY,
            int quartZ,
            Climate.Sampler sampler,
            CallbackInfoReturnable<Holder<Biome>> cir
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

        if (!antarcticaEnabled
                && !volcanicEnabled
                && !riftEnabled) {
            return;
        }

        /*
         * Os Holders só existem depois que
         * o registry real do servidor foi carregado.
         */

        if (
                !WayAroundBiomes.isReady()
        ) {
            return;
        }


        /*
         * =====================================================
         * QUART -> BLOCK COORDINATES
         * =====================================================
         *
         * getNoiseBiome trabalha em quart coordinates.
         *
         * 1 quart = 4 blocos.
         */

        int blockX =
                quartX << 2;

        int blockZ =
                quartZ << 2;


        /*
         * =====================================================
         * ANTÁRTIDA
         * =====================================================
         *
         * Ela vem PRIMEIRO.
         *
         * Assim, quando Southern Ocean e continente
         * se encontram na costa, a Antártida possui
         * prioridade.
         */

        if (
                antarcticaEnabled
                &&
                AntarcticField.isAntarctic(
                        blockX,
                        blockZ
                )
        ) {

            Holder<Biome> antarctic =
                    WayAroundBiomes
                            .getAntarcticIceSheet();


            cir.setReturnValue(
                    antarctic
            );


            /*
             * Debug apenas na primeira vez.
             */

            if (
                    !wayaround$loggedAntarctica
            ) {

                wayaround$loggedAntarctica =
                        true;


                WayAround.LOGGER.info(
                        "WayAround selecionou Antarctic Ice Sheet!"
                );


                WayAround.LOGGER.info(
                        "Primeira coordenada Antarctic: X={} Z={}",
                        blockX,
                        blockZ
                );
            }


            /*
             * MUITO IMPORTANTE:
             *
             * Já escolhemos o bioma.
             * Não queremos continuar para
             * Southern Ocean.
             */

            return;
        }


        /*
         * =====================================================
         * VOLCANIC HIGHLANDS
         * =====================================================
         *
         * Giant volcanic provinces live far away from the polar transition.
         * The field itself also refuses to overlap Antarctic influence.
         */

        if (
                volcanicEnabled
                &&
                VolcanicField.isVolcanic(
                        blockX,
                        blockZ
                )
        ) {

            cir.setReturnValue(
                    WayAroundBiomes
                            .getVolcanicHighlands()
            );

            if (
                    !wayaround$loggedVolcanic
            ) {
                wayaround$loggedVolcanic =
                        true;

                WayAround.LOGGER.info(
                        "WayAround selecionou Volcanic Highlands! X={} Z={}",
                        blockX,
                        blockZ
                );
            }

            return;
        }


        /*
         * =====================================================
         * GREAT RIFT
         * =====================================================
         */

        if (
                riftEnabled
                &&
                GreatRiftField.isGreatRift(
                        blockX,
                        blockZ
                )
        ) {

            cir.setReturnValue(
                    WayAroundBiomes
                            .getGreatRift()
            );

            if (
                    !wayaround$loggedGreatRift
            ) {
                wayaround$loggedGreatRift =
                        true;

                WayAround.LOGGER.info(
                        "WayAround selecionou Great Rift! X={} Z={}",
                        blockX,
                        blockZ
                );
            }

            return;
        }


        /*
         * =====================================================
         * SOUTHERN OCEAN
         * =====================================================
         *
         * Se não estamos no continente, verificamos
         * se estamos dentro da região oceânica polar.
         */

        if (
                antarcticaEnabled
                &&
                AntarcticField.isSouthernOcean(
                        blockX,
                        blockZ
                )
        ) {

            Holder<Biome> southernOcean =
                    WayAroundBiomes
                            .getSouthernOcean();


            cir.setReturnValue(
                    southernOcean
            );


            /*
             * Debug apenas na primeira vez.
             */

            if (
                    !wayaround$loggedSouthernOcean
            ) {

                wayaround$loggedSouthernOcean =
                        true;


                WayAround.LOGGER.info(
                        "WayAround selecionou Southern Ocean!"
                );


                WayAround.LOGGER.info(
                        "Primeira coordenada Southern Ocean: X={} Z={}",
                        blockX,
                        blockZ
                );
            }


            return;
        }


        /*
         * =====================================================
         * MUNDO NORMAL
         * =====================================================
         *
         * Não chamamos cir.setReturnValue().
         *
         * Portanto o método vanilla continua
         * normalmente e escolhe plains, forest,
         * ocean, desert etc.
         */
    }
}