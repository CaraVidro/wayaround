package net.caravidro.wayaround.mixin.client;

import net.minecraft.client.gui.components.SplashRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Way Around splashes are intentionally sentence-like and therefore longer
 * than most vanilla splashes. Keep vanilla's animation/placement but reduce
 * the base scale so long lines remain readable instead of touching both edges
 * of the title screen.
 */
@Mixin(SplashRenderer.class)
public abstract class SplashRendererMixin {

    @ModifyConstant(
            method = "render",
            constant = @Constant(
                    floatValue = 1.8F
            ),
            require = 0
    )
    private float wayaround$smallerSplash(
            float vanillaScale
    ) {
        return vanillaScale
                * 0.78F;
    }
}
