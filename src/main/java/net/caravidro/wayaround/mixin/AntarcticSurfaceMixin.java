package net.caravidro.wayaround.mixin;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldgen.WayAroundBiomes;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NoiseGeneratorSettings.class)
public abstract class AntarcticSurfaceMixin {

    private static boolean wayaround$logged = false;

    @Inject(
            method = "surfaceRule",
            at = @At("RETURN"),
            cancellable = true
    )
    private void wayaround$antarcticSurface(
            CallbackInfoReturnable<SurfaceRules.RuleSource> cir
    ) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.ANTARCTICA
        )) {
            return;
        }

        SurfaceRules.RuleSource vanilla =
                cir.getReturnValue();

        /*
         * =====================================================
         * SUPERFÍCIE EXTERNA
         * =====================================================
         *
         * Essa condição é a parte importante.
         *
         * Ela evita tratar o chão das cavernas como superfície
         * externa da Antártida.
         */
        SurfaceRules.ConditionSource exposedSurface =
                SurfaceRules.abovePreliminarySurface();

        /*
         * =====================================================
         * NEVE NO TOPO
         * =====================================================
         */

        SurfaceRules.RuleSource snowTop =
                SurfaceRules.ifTrue(

                        exposedSurface,

                        SurfaceRules.ifTrue(

                                SurfaceRules.ON_FLOOR,

                                SurfaceRules.state(
                                        Blocks.SNOW_BLOCK
                                                .defaultBlockState()
                                )
                        )
                );

        /*
         * =====================================================
         * NEVE ABAIXO DO TOPO
         * =====================================================
         */

        SurfaceRules.RuleSource snowBelow =
                SurfaceRules.ifTrue(

                        exposedSurface,

                        SurfaceRules.ifTrue(

                                SurfaceRules.UNDER_FLOOR,

                                SurfaceRules.state(
                                        Blocks.SNOW_BLOCK
                                                .defaultBlockState()
                                )
                        )
                );

        /*
         * =====================================================
         * GELO MAIS PROFUNDO
         * =====================================================
         *
         * Continua restrito à região próxima da superfície.
         */
        SurfaceRules.RuleSource packedIce =
                SurfaceRules.ifTrue(

                        exposedSurface,

                        SurfaceRules.ifTrue(

                                SurfaceRules.VERY_DEEP_UNDER_FLOOR,

                                SurfaceRules.state(
                                        Blocks.PACKED_ICE
                                                .defaultBlockState()
                                )
                        )
                );

        /*
         * =====================================================
         * CAMADA ANTÁRTICA
         * =====================================================
         */

        SurfaceRules.RuleSource antarcticSurface =
                SurfaceRules.sequence(

                        snowTop,

                        snowBelow,

                        packedIce
                );

        /*
         * Apenas no biome Antarctic Ice Sheet.
         */
        SurfaceRules.RuleSource antarcticBiome =
                SurfaceRules.ifTrue(

                        SurfaceRules.isBiome(
                                WayAroundBiomes
                                        .ANTARCTIC_ICE_SHEET
                        ),

                        antarcticSurface
                );

        /*
         * Se não for uma superfície externa antártica,
         * deixa o vanilla cuidar.
         *
         * Isso é exatamente o que queremos para cavernas.
         */
        cir.setReturnValue(
                SurfaceRules.sequence(

                        antarcticBiome,

                        vanilla
                )
        );

        if (!wayaround$logged) {

            wayaround$logged = true;

            WayAround.LOGGER.info(
                    "SurfaceRules externas da Antarctic Ice Sheet instaladas!"
            );
        }
    }
}