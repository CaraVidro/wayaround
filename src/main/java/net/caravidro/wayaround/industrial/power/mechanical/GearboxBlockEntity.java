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

public final class GearboxBlockEntity extends AbstractMechanicalBlockEntity {
    public GearboxBlockEntity(BlockPos pos, BlockState state) {
        super(PowerContent.GEARBOX_ENTITY.get(), pos, state);
    }

    @Override public double inertia() { return 2.4; }
    @Override public double frictionTorque() { return 0.16; }
    @Override public double maxSafeRpm() { return MechanicalUnits.GEARBOX_SAFE_RPM; }

    @Override
    public double portFactor(Direction side) {
        BlockState state = getBlockState();
        Direction.Axis axis = state.getValue(GearboxBlock.AXIS);
        if (side.getAxis() != axis) return 0.0;

        if (side.getAxisDirection() == Direction.AxisDirection.NEGATIVE)
            return 1.0;

        return switch (state.getValue(GearboxBlock.RATIO)) {
            case 1 -> -2.0;   // speed-up
            case 2 -> -0.5;   // torque-up
            default -> -1.0;  // direct, but gears reverse direction
        };
    }

    @Override
    public void afterMechanicalStep(ServerLevel level, boolean jammed) {
        updateOverspeed();
        if (jammed && Math.floorMod(level.getGameTime() + worldPosition.asLong(), 16) == 0) {
            level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND,
                SoundSource.BLOCKS, 0.30F, 0.95F);
        }
    }

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, GearboxBlockEntity gearbox) {
        if (!(level instanceof ServerLevel server)) return;
        MechanicalNetwork.solveIfNeeded(server, gearbox);

        if (gearbox.overspeedTicks > 100) {
            level.playSound(null, pos, SoundEvents.ANVIL_LAND,
                SoundSource.BLOCKS, 0.6F, 1.35F);
            server.sendParticles(ParticleTypes.CRIT,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                12, 0.25, 0.25, 0.25, 0.1);
            server.destroyBlock(pos, true);
        }
    }
}
