package net.caravidro.wayaround.mixin.client;

import net.caravidro.wayaround.client.ClientBlizzardState;
import net.caravidro.wayaround.worldgen.WayAroundBiomes;
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
        if (minecraft.level.getBiome(minecraft.getCameraEntity().blockPosition()).is(WayAroundBiomes.ANTARCTIC_ICE_SHEET)) {
            callback.setReturnValue(Math.max(callback.getReturnValue(), 0.35F + 0.65F * ClientBlizzardState.getIntensity()));
        }
    }
}
