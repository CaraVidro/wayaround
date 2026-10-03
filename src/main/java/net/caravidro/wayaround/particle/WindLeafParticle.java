package net.caravidro.wayaround.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;

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

        applySourceLeafColor(
                level,
                x,
                y,
                z
        );

        this.roll = level.random.nextFloat() * ((float) Math.PI * 2.0F);
        this.oRoll = this.roll;

        this.pickSprite(sprites);
    }

    private void applySourceLeafColor(
            ClientLevel level,
            double x,
            double y,
            double z
    ) {
        BlockPos origin =
                BlockPos.containing(
                        x,
                        y - 0.55,
                        z
                );

        BlockPos source =
                null;

        /*
         * The mote is emitted just above a leaf. Search a tiny vertical band
         * instead of trusting one exact Y so tall/custom leaf blocks still
         * carry their own biome tint into the particle.
         */
        for (int offset = 2;
             offset >= -3;
             offset--) {
            BlockPos candidate =
                    origin.offset(
                            0,
                            offset,
                            0
                    );

            if (level.getBlockState(
                    candidate
            ).is(
                    BlockTags.LEAVES
            )) {
                source =
                        candidate;
                break;
            }
        }

        if (source == null) {
            float green =
                    0.62F
                            + level.random.nextFloat()
                                    * 0.24F;

            setColor(
                    green * 0.62F,
                    green,
                    green * 0.48F
            );

            return;
        }

        BlockState state =
                level.getBlockState(
                        source
                );

        int color =
                Minecraft.getInstance()
                        .getBlockColors()
                        .getColor(
                                state,
                                level,
                                source,
                                0
                        );

        if (color == -1) {
            color =
                    0x6FA64B;
        }

        float variation =
                0.92F
                        + level.random.nextFloat()
                                * 0.16F;

        float red =
                ((color >> 16) & 255)
                        / 255.0F;

        float green =
                ((color >> 8) & 255)
                        / 255.0F;

        float blue =
                (color & 255)
                        / 255.0F;

        setColor(
                Math.min(
                        1.0F,
                        red * variation
                ),
                Math.min(
                        1.0F,
                        green * variation
                ),
                Math.min(
                        1.0F,
                        blue * variation
                )
        );
    }

    @Override
    public void tick() {
        oRoll = roll;
        roll += 0.18F + (float) Math.sqrt(xd * xd + zd * zd) * 0.65F;

        float wind=net.caravidro.wayaround.client.weather.ClientWind.getStrength();
        xd+=net.caravidro.wayaround.client.weather.ClientWind.getX()*wind*.0025;
        zd+=net.caravidro.wayaround.client.weather.ClientWind.getZ()*wind*.0025;
        var player=Minecraft.getInstance().player;
        if(player==null||player.distanceToSqr(x,y,z)>48*48){remove();return;}
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
