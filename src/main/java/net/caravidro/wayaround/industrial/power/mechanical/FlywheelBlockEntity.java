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

public final class FlywheelBlockEntity extends AbstractMechanicalBlockEntity {
    public FlywheelBlockEntity(BlockPos pos, BlockState state) {
        super(PowerContent.FLYWHEEL_ENTITY.get(), pos, state);
    }

    @Override public double inertia() { return 12.0; }
    @Override public double frictionTorque() { return 0.085; }
    @Override public double maxSafeRpm() { return MechanicalUnits.FLYWHEEL_SAFE_RPM; }

    @Override
    public double portFactor(Direction side) {
        return side.getAxis() == getBlockState().getValue(FlywheelBlock.AXIS) ? 1.0 : 0.0;
    }

    @Override
    public void afterMechanicalStep(ServerLevel level, boolean jammed) {
        updateOverspeed();
    }

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, FlywheelBlockEntity flywheel) {
        if (!(level instanceof ServerLevel server)) return;
        MechanicalNetwork.solveIfNeeded(server, flywheel);

        double speed = Math.abs(flywheel.mechanicalRpm());
        boolean phase = false;
        if (speed > MechanicalUnits.MIN_RUNNING_RPM) {
            int period = speed > 130 ? 2 : speed > 60 ? 4 : 7;
            phase = Math.floorMod(level.getGameTime() / period, 2) == 1;

            if (Math.floorMod(level.getGameTime() + pos.asLong(), 60) == 0)
                level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE,
                    SoundSource.BLOCKS, 0.12F, 0.55F + (float) Math.min(0.45, speed / 450.0));
        }

        if (state.getValue(FlywheelBlock.PHASE) != phase)
            level.setBlock(pos, state.setValue(FlywheelBlock.PHASE, phase), 3);

        if (flywheel.overspeedTicks > 120) {
            level.playSound(null, pos, SoundEvents.ANVIL_LAND,
                SoundSource.BLOCKS, 0.8F, 0.8F);
            server.sendParticles(ParticleTypes.CRIT,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                24, 0.5, 0.5, 0.5, 0.15);
            server.destroyBlock(pos, true);
        }
    }
}
