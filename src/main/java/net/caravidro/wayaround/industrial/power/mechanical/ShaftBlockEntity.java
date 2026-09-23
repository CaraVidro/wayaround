package net.caravidro.wayaround.industrial.power.mechanical;

import net.caravidro.wayaround.industrial.power.PowerContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class ShaftBlockEntity extends AbstractMechanicalBlockEntity {
    public ShaftBlockEntity(BlockPos pos, BlockState state) {
        super(PowerContent.SHAFT_ENTITY.get(), pos, state);
    }

    @Override public double inertia() { return 0.60; }
    @Override public double frictionTorque() { return 0.035; }
    @Override public double maxSafeRpm() { return MechanicalUnits.SHAFT_SAFE_RPM; }

    @Override
    public double portFactor(Direction side) {
        return side.getAxis() == getBlockState().getValue(ShaftBlock.AXIS) ? 1.0 : 0.0;
    }

    @Override
    public void afterMechanicalStep(ServerLevel level, boolean jammed) {
        updateOverspeed();
    }

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, ShaftBlockEntity shaft) {
        if (!(level instanceof ServerLevel server)) return;
        MechanicalNetwork.solveIfNeeded(server, shaft);

        if (shaft.overspeedTicks > 80) {
            level.playSound(null, pos, SoundEvents.ANVIL_LAND,
                SoundSource.BLOCKS, 0.42F, 1.6F);
            server.sendParticles(ParticleTypes.CRIT,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                8, 0.18, 0.18, 0.18, 0.08);
            server.destroyBlock(pos, true);
        }
    }
}
