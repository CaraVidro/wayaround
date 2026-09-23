package net.caravidro.wayaround.particle;

import net.caravidro.wayaround.worldgen.water.WaterDynamics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.Vec3;

public final class AirBubbleParticle
        extends TextureSheetParticle {

    private final SpriteSet sprites;
    private boolean surface;
    private int surfaceTicks;
    private int popAfter;

    private AirBubbleParticle(
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

        this.hasPhysics =
                false;

        this.gravity =
                0.0F;

        this.friction =
                0.96F;

        this.quadSize =
                0.055F
                + level.random.nextFloat()
                * 0.065F;

        this.lifetime =
                180;

        this.popAfter =
                8
                + level.random.nextInt(16);

        this.alpha =
                0.82F;

        pickSprite(
                sprites
        );
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;

        if (surface) {
            surfaceTicks++;

            alpha =
                    0.82F
                    * (
                            1.0F
                            - surfaceTicks
                            / (float) popAfter
                            * 0.35F
                    );

            xd *= 0.86;
            zd *= 0.86;

            x += xd;
            z += zd;

            if (surfaceTicks >= popAfter) {
                level.addParticle(
                        ParticleTypes.BUBBLE_POP,
                        x,
                        y,
                        z,
                        xd,
                        0.015,
                        zd
                );

                remove();
            }

            return;
        }

        BlockPos pos =
                BlockPos.containing(
                        x,
                        y,
                        z
                );

        boolean water =
                level.getFluidState(pos)
                        .is(FluidTags.WATER);

        if (!water) {
            BlockPos below =
                    pos.below();

            if (level.getFluidState(below)
                    .is(FluidTags.WATER)) {

                surface =
                        true;

                y =
                        pos.getY()
                        + 0.015;

                yd =
                        0.0;

                return;
            }

            remove();
            return;
        }

        Vec3 flow =
                WaterDynamics.current(
                        level,
                        pos
                );

        xd +=
                flow.x * 0.012
                + (
                        random.nextDouble()
                        - 0.5
                ) * 0.003;

        zd +=
                flow.z * 0.012
                + (
                        random.nextDouble()
                        - 0.5
                ) * 0.003;

        yd =
                Math.min(
                        0.085,
                        yd * 0.82
                        + 0.012
                );

        x += xd;
        y += yd;
        z += zd;

        xd *= 0.92;
        zd *= 0.92;

        age++;

        if (age >= lifetime) {
            remove();
        }

        setSpriteFromAge(
                sprites
        );
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider
            implements ParticleProvider<SimpleParticleType> {

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
            return new AirBubbleParticle(
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
