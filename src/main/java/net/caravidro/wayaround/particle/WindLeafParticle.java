package net.caravidro.wayaround.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public final class WindLeafParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    private WindLeafParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xd,
            double yd,
            double zd,
            SpriteSet sprites
    ) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = sprites;

        this.xd = xd;
        this.yd = yd;
        this.zd = zd;

        this.gravity = 0.018F;
        this.friction = 0.965F;
        this.hasPhysics = false;

        this.quadSize = 0.12F + level.random.nextFloat() * 0.12F;
        this.lifetime = 24 + level.random.nextInt(36);

        float green = 0.62F + level.random.nextFloat() * 0.24F;
        this.rCol = green * 0.62F;
        this.gCol = green;
        this.bCol = green * 0.48F;

        this.roll = level.random.nextFloat() * ((float) Math.PI * 2.0F);
        this.oRoll = this.roll;

        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        oRoll = roll;
        roll += 0.18F + (float) Math.sqrt(xd * xd + zd * zd) * 0.65F;

        xd += (random.nextDouble() - 0.5) * 0.003;
        zd += (random.nextDouble() - 0.5) * 0.003;

        super.tick();
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
            return new WindLeafParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
