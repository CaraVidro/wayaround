package net.caravidro.wayaround.mixin.client;

import net.caravidro.wayaround.client.AntarcticClientLighting;
import net.caravidro.wayaround.client.ClientBlizzardState;
import net.caravidro.wayaround.client.VoidDomainClientEffects;
import net.caravidro.wayaround.worldgen.weather.local.LocalWeatherField;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Feed local Antarctic snowfall into vanilla's precipitation renderer. */
@Mixin(Level.class)
public abstract class AntarcticPrecipitationMixin {
    @Inject(method = "getRainLevel", at = @At("RETURN"), cancellable = true)
    private void wayaround$snowfall(float partialTick, CallbackInfoReturnable<Float> callback) {
        Minecraft minecraft = Minecraft.getInstance();
        if ((Object) this != minecraft.level || minecraft.level == null || minecraft.getCameraEntity() == null
                || !minecraft.level.dimension().equals(Level.OVERWORLD)) return;

        /*
         * The Void pocket lives at technical Overworld coordinates. Weather
         * must be completely suppressed there; otherwise the remote biome and
         * local weather field can still feed snow through vanilla rendering.
         */
        if (VoidDomainClientEffects.localInterior()
                != null) {
            callback.setReturnValue(
                    0.0F
            );
            return;
        }

        float polar =
                AntarcticClientLighting.polarInfluence(
                        minecraft.level,
                        minecraft.getCameraEntity().blockPosition()
                );

        LocalWeatherField.Sample weather =
                LocalWeatherField.sample(
                        minecraft.getCameraEntity().getX(),
                        minecraft.getCameraEntity().getZ(),
                        minecraft.level.getGameTime()
                );

        /*
         * Normal Overworld rain is spatial. As the player moves south, the
         * local rain field is gradually replaced by persistent polar
         * precipitation instead of snapping at the Antarctic biome border.
         */
        float ordinary =
                weather.rain()
                        * (
                        1.0F
                                - polar * 0.65F
                );

        float polarSnow =
                polar
                        * (
                        0.10F
                                + polar * 0.25F
                                + ClientBlizzardState.getIntensity()
                                        * 0.65F
                );

        callback.setReturnValue(
                Math.max(
                        ordinary,
                        polarSnow
                )
        );
    }
}
