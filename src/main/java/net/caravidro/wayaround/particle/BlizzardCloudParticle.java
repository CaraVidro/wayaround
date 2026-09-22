package net.caravidro.wayaround.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

import net.minecraft.core.particles.SimpleParticleType;

public final class BlizzardCloudParticle
        extends TextureSheetParticle {

    private final SpriteSet sprites;

    private final float baseAlpha;

    protected BlizzardCloudParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xd,
            double yd,
            double zd,
            SpriteSet sprites
    ) {

        super(
                level,
                x,
                y,
                z,
                xd,
                yd,
                zd
        );

        this.sprites =
                sprites;

        /*
         * =================================================
         * TAMANHO
         * =================================================
         *
         * NÃO faça isso virar 10~20.
         *
         * A massa visual agora vem da quantidade
         * de partículas, não do tamanho individual.
         */

        this.quadSize =
                2.0F
                +
                level.random.nextFloat()
                *
                2.2F;

        /*
         * Vida relativamente longa.
         *
         * 60 ticks = 3 segundos
         * 110 ticks = 5.5 segundos
         */
        this.lifetime =
                60
                +
                level.random.nextInt(
                        51
                );

        /*
         * =================================================
         * MOVIMENTO
         * =================================================
         */

        /*
         * Mantém um pouquinho da velocidade
         * fornecida pelo servidor.
         */
        this.xd =
                xd * 0.18;

        this.zd =
                zd * 0.18;

        /*
         * Sobe devagar como fumaça/neve em suspensão.
         */
        this.yd =
                0.012
                +
                level.random.nextDouble()
                *
                0.018;

        /*
         * Gravity negativa =
         * tendência muito leve de subir.
         */
        this.gravity =
                -0.0015F;

        this.friction =
                0.985F;

        this.hasPhysics =
                false;

        /*
         * =================================================
         * COR
         * =================================================
         *
         * Branco frio, mas não RGB 255 puro.
         */

        float brightness =
                0.90F
                +
                level.random.nextFloat()
                *
                0.08F;

        this.rCol =
                brightness;

        this.gCol =
                Math.min(
                        1.0F,
                        brightness + 0.025F
                );

        this.bCol =
                Math.min(
                        1.0F,
                        brightness + 0.055F
                );

        /*
         * Alpha máximo varia um pouco.
         *
         * Isso impede todas as partículas
         * de parecerem uma chapa uniforme.
         */
        this.baseAlpha =
                0.28F
                +
                level.random.nextFloat()
                *
                0.20F;

        /*
         * Começa completamente transparente.
         */
        this.alpha =
                0.0F;

        this.pickSprite(
                sprites
        );
    }

    @Override
    public void tick() {

        super.tick();

        if (
                this.removed
        ) {
            return;
        }

        /*
         * =================================================
         * TURBULÊNCIA
         * =================================================
         */

        this.xd +=
                (
                        this.random.nextDouble()
                        -
                        0.5
                )
                *
                0.0014;

        this.zd +=
                (
                        this.random.nextDouble()
                        -
                        0.5
                )
                *
                0.0014;

        /*
         * Expande muito lentamente.
         */
        this.quadSize *=
                1.0025F;

        /*
         * =================================================
         * FADE
         * =================================================
         */

        float life =
                (float) this.age
                /
                (float) this.lifetime;

        /*
         * Primeiros 18%:
         * aparece gradualmente.
         */
        if (
                life < 0.18F
        ) {

            float fadeIn =
                    life
                    /
                    0.18F;

            this.alpha =
                    this.baseAlpha
                    *
                    fadeIn;
        }

        /*
         * Meio da vida:
         * estável.
         */
        else if (
                life < 0.70F
        ) {

            this.alpha =
                    this.baseAlpha;
        }

        /*
         * Últimos 30%:
         * desaparece suavemente.
         */
        else {

            float fadeOut =
                    1.0F
                    -
                    (
                            (
                                    life
                                    -
                                    0.70F
                            )
                            /
                            0.30F
                    );

            this.alpha =
                    this.baseAlpha
                    *
                    Math.max(
                            0.0F,
                            fadeOut
                    );
        }

        /*
         * Se houver vários sprites,
         * anima como fumaça.
         */
        this.setSpriteFromAge(
                sprites
        );
    }

    @Override
    public ParticleRenderType getRenderType() {

        return ParticleRenderType
                .PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider
            implements ParticleProvider<
                    SimpleParticleType
            > {

        private final SpriteSet sprites;

        public Provider(
                SpriteSet sprites
        ) {

            this.sprites =
                    sprites;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,

                double x,
                double y,
                double z,

                double xd,
                double yd,
                double zd
        ) {

            return new BlizzardCloudParticle(
                    level,

                    x,
                    y,
                    z,

                    xd,
                    yd,
                    zd,

                    sprites
            );
        }
    }
}