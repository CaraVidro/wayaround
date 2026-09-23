package net.caravidro.wayaround.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * Large soft billboards arranged in clusters by LivingWeatherClient.
 * The goal is readable volumetric-looking cloud masses without a heavy
 * ray-marched cloud renderer.
 */
public final class LivingCloudParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final float baseAlpha;

    private LivingCloudParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xd,
            double storm,
            double zd,
            SpriteSet sprites
    ) {
        super(level, x, y, z, xd, 0.0, zd);
        this.sprites = sprites;

        float storminess = Mth.clamp((float) storm, 0.0F, 1.0F);

        this.xd = xd;
        this.yd = 0.0;
        this.zd = zd;

        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.friction = 1.0F;

        this.quadSize =
                11.0F
                + level.random.nextFloat() * 12.0F
                + storminess * 7.0F;

        this.lifetime =
                95
                + level.random.nextInt(76);

        float brightness =
                0.94F
                - storminess * 0.58F
                + (level.random.nextFloat() - 0.5F) * 0.06F;

        brightness = Mth.clamp(brightness, 0.28F, 0.98F);

        this.rCol = brightness * 0.96F;
        this.gCol = brightness;
        this.bCol = Math.min(1.0F, brightness * 1.04F);

        this.baseAlpha =
                0.18F
                + storminess * 0.18F
                + level.random.nextFloat() * 0.05F;

        this.alpha = 0.0F;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();

        if (removed) {
            return;
        }

        float life = (float) age / (float) lifetime;

        if (life < 0.12F) {
            alpha = baseAlpha * (life / 0.12F);
        } else if (life > 0.78F) {
            alpha = baseAlpha * Math.max(0.0F, (1.0F - life) / 0.22F);
        } else {
            alpha = baseAlpha;
        }

        /*
         * Tiny vertical breathing keeps the mass from looking like a rigid
         * collection of discs.
         */
        yd = Math.sin((age + x * 0.03 + z * 0.03) * 0.08) * 0.0025;

        quadSize *= 1.0008F;
        setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
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
            return new LivingCloudParticle(
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
