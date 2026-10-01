package net.caravidro.wayaround.mixin;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.caravidro.wayaround.worldgen.WayAroundBiomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NoiseGeneratorSettings.class)
public abstract class VolcanicSurfaceMixin {

    private static boolean wayaround$logged;

    @Inject(
            method = "surfaceRule",
            at = @At("RETURN"),
            cancellable = true
    )
    private void wayaround$volcanicSurface(
            CallbackInfoReturnable<SurfaceRules.RuleSource> cir
    ) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.VOLCANIC_REGIONS
        )) {
            return;
        }

        SurfaceRules.RuleSource vanilla =
                cir.getReturnValue();

        SurfaceRules.RuleSource volcanicSurface =
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(
                                SurfaceRules.abovePreliminarySurface(),
                                SurfaceRules.ifTrue(
                                        SurfaceRules.ON_FLOOR,
                                        SurfaceRules.state(
                                                Blocks.BASALT
                                                        .defaultBlockState()
                                        )
                                )
                        ),
                        SurfaceRules.ifTrue(
                                SurfaceRules.abovePreliminarySurface(),
                                SurfaceRules.ifTrue(
                                        SurfaceRules.UNDER_FLOOR,
                                        SurfaceRules.state(
                                                Blocks.BLACKSTONE
                                                        .defaultBlockState()
                                        )
                                )
                        ),
                        SurfaceRules.ifTrue(
                                SurfaceRules.abovePreliminarySurface(),
                                SurfaceRules.ifTrue(
                                        SurfaceRules.VERY_DEEP_UNDER_FLOOR,
                                        SurfaceRules.state(
                                                Blocks.TUFF
                                                        .defaultBlockState()
                                        )
                                )
                        )
                );

        SurfaceRules.RuleSource volcanicBiome =
                SurfaceRules.ifTrue(
                        SurfaceRules.isBiome(
                                WayAroundBiomes.VOLCANIC_HIGHLANDS
                        ),
                        volcanicSurface
                );

        cir.setReturnValue(
                SurfaceRules.sequence(
                        volcanicBiome,
                        vanilla
                )
        );

        if (!wayaround$logged) {
            wayaround$logged =
                    true;

            WayAround.LOGGER.info(
                    "Volcanic Highlands surface rules installed."
            );
        }
    }
}
