package net.caravidro.wayaround.industrial.power.mechanical;

import net.caravidro.wayaround.industrial.power.PowerContent;
import net.caravidro.wayaround.industrial.power.steam.SteamNode;
import net.caravidro.wayaround.industrial.power.steam.SteamStorage;
import net.caravidro.wayaround.industrial.power.steam.SteamTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class SteamPistonBlockEntity extends AbstractMechanicalBlockEntity implements SteamNode {
    private final SteamStorage steam = new SteamStorage(MechanicalUnits.PISTON_STEAM_CAPACITY);

    public SteamPistonBlockEntity(BlockPos pos, BlockState state) {
        super(PowerContent.STEAM_PISTON_ENTITY.get(), pos, state);
    }

    @Override public int steamStored() { return steam.stored(); }
    @Override public int steamCapacity() { return steam.capacity(); }
    @Override public double pressureBar() { return steam.pressureBar(); }
    @Override public int receiveSteam(int amount, boolean simulate) { return steam.receive(amount, simulate); }
    @Override public int extractSteam(int amount, boolean simulate) { return steam.extract(amount, simulate); }
    @Override public boolean canConnectSteam(Direction side) { return true; }

    @Override public double inertia() { return 4.0; }
    @Override public double frictionTorque() { return 0.24; }
    @Override public double maxSafeRpm() { return MechanicalUnits.PISTON_SAFE_RPM; }

    @Override
    public double driveTorque() {
        double pressure = pressureBar();
        if (pressure < MechanicalUnits.PISTON_MIN_PRESSURE_BAR) return 0.0;

        double pressureRange = 12.0 - MechanicalUnits.PISTON_MIN_PRESSURE_BAR;
        double pressureFactor = Math.min(1.0,
            (pressure - MechanicalUnits.PISTON_MIN_PRESSURE_BAR) / pressureRange);
        double stallTorque = 7.0 + pressureFactor * 22.0;
        double speedFactor = Math.max(0.0,
            1.0 - Math.abs(mechanicalRpm()) / MechanicalUnits.PISTON_NO_LOAD_RPM);
        return stallTorque * speedFactor;
    }

    @Override
    public double portFactor(Direction side) {
        return side == getBlockState().getValue(SteamPistonBlock.FACING) ? 1.0 : 0.0;
    }

    @Override
    public void afterMechanicalStep(ServerLevel level, boolean jammed) {
        updateOverspeed();

        double torque = driveTorque();
        double speed = Math.abs(mechanicalRpm());
        if (!jammed && torque > 0.1 && speed > MechanicalUnits.MIN_RUNNING_RPM) {
            int used = Math.max(1, Math.min(4, (int) Math.ceil(0.5 + speed / 85.0)));
            steam.extract(used, false);
            setChanged();
        }

        if (jammed && Math.floorMod(level.getGameTime() + worldPosition.asLong(), 20) == 0) {
            level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND,
                SoundSource.BLOCKS, 0.28F, 0.75F);
        }
    }

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, SteamPistonBlockEntity piston) {
        if (!(level instanceof ServerLevel server)) return;

        SteamTransfer.balanceAdjacent(server, pos, piston);
        MechanicalNetwork.solveIfNeeded(server, piston);

        double speed = Math.abs(piston.mechanicalRpm());
        boolean running = speed >= MechanicalUnits.MIN_RUNNING_RPM;
        boolean phase = false;

        if (running) {
            int period = speed >= 140.0 ? 2 : speed >= 70.0 ? 4 : 7;
            phase = Math.floorMod(level.getGameTime() / period, 2) == 1;

            long time = level.getGameTime() + pos.asLong();
            int soundPeriod = speed >= 140.0 ? 8 : speed >= 70.0 ? 12 : 20;
            if (Math.floorMod(time, soundPeriod) == 0) {
                level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN,
                    SoundSource.BLOCKS, 0.22F,
                    0.70F + (float) Math.min(0.55, speed / 400.0));
            }
            if (Math.floorMod(time, 12) == 0) {
                Direction back = state.getValue(SteamPistonBlock.FACING).getOpposite();
                server.sendParticles(ParticleTypes.CLOUD,
                    pos.getX() + 0.5 + back.getStepX() * 0.45,
                    pos.getY() + 0.55,
                    pos.getZ() + 0.5 + back.getStepZ() * 0.45,
                    1, 0.04, 0.04, 0.04, 0.015);
            }
        }

        if (state.getValue(SteamPistonBlock.RUNNING) != running
                || state.getValue(SteamPistonBlock.PHASE) != phase) {
            level.setBlock(pos,
                state.setValue(SteamPistonBlock.RUNNING, running)
                    .setValue(SteamPistonBlock.PHASE, phase), 3);
        }

        if (piston.overspeedTicks > 100) {
            level.playSound(null, pos, SoundEvents.ANVIL_LAND,
                SoundSource.BLOCKS, 0.65F, 1.25F);
            server.sendParticles(ParticleTypes.CRIT,
                pos.getX() + 0.5, pos.getY() + 0.55, pos.getZ() + 0.5,
                14, 0.3, 0.3, 0.3, 0.1);
            server.destroyBlock(pos, true);
        }
    }

    @Override
    protected void saveMechanicalExtra(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("Steam", steam.stored());
    }

    @Override
    protected void loadMechanicalExtra(CompoundTag tag, HolderLookup.Provider registries) {
        steam.load(tag.getInt("Steam"));
    }
}
