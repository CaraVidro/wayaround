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
public abstract class GreatRiftSurfaceMixin {

    private static boolean wayaround$logged;

    @Inject(
            method = "surfaceRule",
            at = @At("RETURN"),
            cancellable = true
    )
    private void wayaround$greatRiftSurface(
            CallbackInfoReturnable<SurfaceRules.RuleSource> cir
    ) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.GREAT_RIFTS
        )) {
            return;
        }

        SurfaceRules.RuleSource vanilla =
                cir.getReturnValue();

        SurfaceRules.RuleSource riftSurface =
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(
                                SurfaceRules.abovePreliminarySurface(),
                                SurfaceRules.ifTrue(
                                        SurfaceRules.ON_FLOOR,
                                        SurfaceRules.state(
                                                Blocks.RED_SAND
                                                        .defaultBlockState()
                                        )
                                )
                        ),
                        SurfaceRules.ifTrue(
                                SurfaceRules.abovePreliminarySurface(),
                                SurfaceRules.ifTrue(
                                        SurfaceRules.UNDER_FLOOR,
                                        SurfaceRules.state(
                                                Blocks.TERRACOTTA
                                                        .defaultBlockState()
                                        )
                                )
                        ),
                        SurfaceRules.ifTrue(
                                SurfaceRules.abovePreliminarySurface(),
                                SurfaceRules.ifTrue(
                                        SurfaceRules.VERY_DEEP_UNDER_FLOOR,
                                        SurfaceRules.state(
                                                Blocks.RED_SANDSTONE
                                                        .defaultBlockState()
                                        )
                                )
                        )
                );

        cir.setReturnValue(
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(
                                SurfaceRules.isBiome(
                                        WayAroundBiomes.GREAT_RIFT
                                ),
                                riftSurface
                        ),
                        vanilla
                )
        );

        if (!wayaround$logged) {
            wayaround$logged =
                    true;

            WayAround.LOGGER.info(
                    "Great Rift surface rules installed."
            );
        }
    }
}
