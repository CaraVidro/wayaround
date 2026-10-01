package net.caravidro.wayaround.mixin;

import java.util.HashSet;
import java.util.Set;

import net.caravidro.wayaround.worldgen.WayAroundBiomes;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;

import org.spongepowered.asm.mixin.Mixin;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(BiomeSource.class)
public abstract class AntarcticPossibleBiomesMixin {

    /*
     * =========================================================
     * POSSIBLE BIOMES
     * =========================================================
     *
     * OverworldBiomeMixin consegue retornar biomas que não
     * estavam originalmente presentes no MultiNoiseBiomeSource.
     *
     * Então precisamos também avisar ao BiomeSource:
     *
     * "Sim, Antarctic Ice Sheet e Southern Ocean são biomas
     * possíveis desta fonte."
     *
     * Isso é útil principalmente para coisas como:
     *
     * /locate biome
     * busca de biomas
     * sistemas que consultam possibleBiomes()
     */

    @Inject(
            method = "possibleBiomes",
            at = @At("RETURN"),
            cancellable = true
    )
    private void wayaround$includePolarBiomes(
            CallbackInfoReturnable<Set<Holder<Biome>>> cir
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

        /*
         * =====================================================
         * APENAS MULTI NOISE
         * =====================================================
         *
         * Não queremos mexer em qualquer BiomeSource existente.
         */

        if (
                !((Object) this instanceof MultiNoiseBiomeSource)
        ) {
            return;
        }


        /*
         * Os Holders precisam ter sido capturados
         * do registry real do servidor.
         */

        if (
                !WayAroundBiomes.isReady()
        ) {
            return;
        }


        /*
         * =====================================================
         * BIOMES DO WAYAROUND
         * =====================================================
         */

        Holder<Biome> antarctic =
                antarcticaEnabled
                        ? WayAroundBiomes
                        .getAntarcticIceSheet()
                        : null;


        Holder<Biome> southernOcean =
                antarcticaEnabled
                        ? WayAroundBiomes
                        .getSouthernOcean()
                        : null;


        Holder<Biome> volcanicHighlands =
                volcanicEnabled
                        ? WayAroundBiomes
                        .getVolcanicHighlands()
                        : null;


        /*
         * Conjunto original de biomas conhecidos
         * pelo BiomeSource.
         */

        Set<Holder<Biome>> original =
                cir.getReturnValue();


        /*
         * Criamos uma cópia mutável.
         */

        Set<Holder<Biome>> expanded =
                new HashSet<>(
                        original
                );


        /*
         * Adiciona os nossos dois biomas.
         *
         * HashSet já impede duplicatas,
         * então não precisamos fazer contains()
         * manualmente.
         */

        if (antarctic != null) {
            expanded.add(
                    antarctic
            );
        }

        if (southernOcean != null) {
            expanded.add(
                    southernOcean
            );
        }

        if (volcanicHighlands != null) {
            expanded.add(
                    volcanicHighlands
            );
        }


        /*
         * Devolvemos um Set imutável,
         * assim como é esperado pelo restante
         * do código do Minecraft.
         */

        cir.setReturnValue(
                Set.copyOf(
                        expanded
                )
        );
    }
}