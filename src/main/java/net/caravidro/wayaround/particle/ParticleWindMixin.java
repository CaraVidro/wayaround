package net.caravidro.wayaround.particle;

import net.caravidro.wayaround.client.weather.ClientWind;

import net.minecraft.client.particle.Particle;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Particle.class)
public abstract class ParticleWindMixin {

    @Shadow
    protected double xd;

    @Shadow
    protected double yd;

    @Shadow
    protected double zd;
    @Shadow
protected double x;

@Shadow
protected double z;

@Shadow
protected int age;
    @Shadow
    protected boolean removed;

    @Inject(
            method = "tick",
            at = @At("HEAD")
    )
    private void wayaround$wind(
            CallbackInfo ci
    ) {

        if (removed) {
            return;
        }

        if (!ClientWind.active()) {
            return;
        }

        double strength =
                ClientWind.getStrength();

        /*
         * =====================================================
         * VELOCIDADE ALVO DO VENTO
         * =====================================================
         */

        double targetX =
                ClientWind.getX()
                        *
                        strength
                        *
                        0.32;

        double targetZ =
                ClientWind.getZ()
                        *
                        strength
                        *
                        0.32;

        /*
         * Em vez de simplesmente:
         *
         * xd += vento
         *
         * nós puxamos a partícula em direção à velocidade
         * do vento.
         *
         * Isso evita:
         *
         * fumaça atingindo Mach 4 depois de 10 segundos.
         */

        double response =
                0.075
                        +
                        strength
                        *
                        0.10;

        xd +=
                (
                        targetX
                                -
                                xd
                )
                        *
                        response;

        zd +=
                (
                        targetZ
                                -
                                zd
                )
                        *
                        response;

        /*
         * =====================================================
         * VENTO FORTE ATRAPALHA A SUBIDA
         * =====================================================
         *
         * Fumaça ainda sobe, mas começa a se inclinar.
         */

        if (yd > 0.0) {

            yd *=
                    1.0
                            -
                            strength
                            *
                            0.012;
        }

        /*
         * =====================================================
         * TURBULÊNCIA
         * =====================================================
         *
         * Muito pequena.
         *
         * Evita partículas perfeitamente paralelas.
         */

        Particle particle =
                (Particle) (Object) this;

        double turbulence =
                Math.sin(x * 0.13 + z * 0.17 + age * 0.25)
                        *
                        0.003
                        *
                        strength;

        xd +=
                ClientWind.getZ()
                        *
                        turbulence;

        zd -=
                ClientWind.getX()
                        *
                        turbulence;
    }
}