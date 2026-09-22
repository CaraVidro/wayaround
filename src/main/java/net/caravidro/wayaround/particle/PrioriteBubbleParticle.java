package net.caravidro.wayaround.particle;

import net.caravidro.wayaround.block.PrioriteBlock;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;

/** An anchored gas bubble. Smoke, sound and damage are emitted by the server at burst time. */
public final class PrioriteBubbleParticle extends TextureSheetParticle {
    private final float finalSize;
    private final double surfaceY;
    private final double surfaceX;
    private final double surfaceZ;
    private final BlockPos source;

    private PrioriteBubbleParticle(ClientLevel level, double x, double y, double z,
                                  double size, SpriteSet sprites) {
        super(level, x, y, z);
        source = BlockPos.containing(x, y, z).below();
        surfaceY = y;
        surfaceX = x;
        surfaceZ = z;
        finalSize = (float) Math.clamp(size, 0.25, 0.85);
        lifetime = PrioriteBlock.BUBBLE_GROW_TICKS;
        hasPhysics = false;
        xd = yd = zd = 0;
        quadSize = 0.06F;
        setColor(0.85F, 0.95F, 0.22F);
        setAlpha(0.9F);
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        // Collection or replacement cancels the visible bubble as well as the burst.
        if (!(level.getBlockState(source).getBlock() instanceof PrioriteBlock) || ++age >= lifetime) {
            remove();
            return;
        }
        float progress = age / (float) lifetime;
        quadSize = 0.06F + (finalSize - 0.06F) * progress;
        // Stay attached to the source. The shared particle wind adds a small bounded sway.
        setPos(surfaceX, surfaceY + quadSize * 0.65, surfaceZ);
    }

    @Override
    public int getLightColor(float partialTick) {
        int light = super.getLightColor(partialTick);
        return Math.max(light & 0xffff, 64) | (light & 0xffff0000);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) { this.sprites = sprites; }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double size, double unusedY, double unusedZ) {
            return new PrioriteBubbleParticle(level, x, y, z, size, sprites);
        }
    }
}
