package net.caravidro.wayaround.mixin.client;

import net.caravidro.wayaround.client.weather.WindAffectedParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineWindMixin {
    @Inject(method = "tickParticle", at = @At("TAIL"))
    private void wayaround$wind(Particle particle, CallbackInfo ci) {
        ((WindAffectedParticle) particle).wayaround$applyWind();
    }
}
