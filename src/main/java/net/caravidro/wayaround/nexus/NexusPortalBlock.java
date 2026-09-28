package net.caravidro.wayaround.nexus;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Physical Nexus threshold. It owns no independent destination; the player's
 * stored Nexustor link is the authority, so switching that Nexustor off really
 * can strand somebody on the other side.
 */
public final class NexusPortalBlock
        extends Block {

    public static final MapCodec<NexusPortalBlock> CODEC =
            simpleCodec(
                    NexusPortalBlock::new
            );

    public NexusPortalBlock(
            Properties properties
    ) {
        super(
                properties
        );
    }

    @Override
    protected MapCodec<NexusPortalBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity
    ) {
        NexusPortalManager.touch(
                level,
                pos,
                entity
        );
    }

    @Override
    public void animateTick(
            BlockState state,
            Level level,
            BlockPos pos,
            RandomSource random
    ) {
        for (int i = 0;
             i < 3;
             i++) {
            double x =
                    pos.getX()
                            + random.nextDouble();

            double y =
                    pos.getY()
                            + random.nextDouble();

            double z =
                    pos.getZ()
                            + 0.45
                            + (
                            random.nextDouble()
                                    - 0.5
                    )
                                    * 0.14;

            level.addParticle(
                    i == 0
                            ? ParticleTypes.REVERSE_PORTAL
                            : ParticleTypes.PORTAL,
                    x,
                    y,
                    z,
                    (
                            random.nextDouble()
                                    - 0.5
                    )
                            * 0.035,
                    (
                            random.nextDouble()
                                    - 0.5
                    )
                            * 0.020,
                    (
                            random.nextDouble()
                                    - 0.5
                    )
                            * 0.035
            );
        }
    }
}
