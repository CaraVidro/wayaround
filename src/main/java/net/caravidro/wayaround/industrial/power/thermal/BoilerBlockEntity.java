package net.caravidro.wayaround.industrial.power.thermal;

import net.caravidro.wayaround.industrial.power.PowerContent;
import net.caravidro.wayaround.industrial.power.steam.SteamEffects;
import net.caravidro.wayaround.industrial.power.steam.SteamNode;
import net.caravidro.wayaround.industrial.power.steam.SteamStorage;
import net.caravidro.wayaround.industrial.power.steam.SteamTransfer;
import net.caravidro.wayaround.industrial.power.steam.SteamUnits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class BoilerBlockEntity extends BlockEntity implements HeatReceiver, SteamNode {
    private final ThermalStorage thermal = new ThermalStorage(
        ThermalUnits.BOILER_THERMAL_MASS_HU_PER_C,
        ThermalUnits.BOILER_MAX_TEMPERATURE_C);
    private final SteamStorage steam = new SteamStorage(SteamUnits.BOILER_STEAM_CAPACITY);
    private int water;
    private int pressureStressTicks;

    public BoilerBlockEntity(BlockPos pos, BlockState state) {
        super(PowerContent.BOILER_ENTITY.get(), pos, state);
    }

    @Override
    public int receiveHeat(Direction from, int heatUnits, boolean simulate) {
        int accepted = thermal.receive(heatUnits, simulate);
        if (!simulate && accepted > 0) setChanged();
        return accepted;
    }

    public boolean addWater() {
        if (water > ThermalUnits.BOILER_WATER_CAPACITY_MB - 1_000) return false;
        water += 1_000;
        setChanged();
        return true;
    }

    public double temperatureC() { return thermal.temperatureC(); }
    public int storedHeatHu() { return thermal.storedHu(); }
    public int storedWaterMb() { return water; }

    @Override public int steamStored() { return steam.stored(); }
    @Override public int steamCapacity() { return steam.capacity(); }
    @Override public double pressureBar() { return steam.pressureBar(); }
    @Override public int receiveSteam(int amount, boolean simulate) { return steam.receive(amount, simulate); }
    @Override public int extractSteam(int amount, boolean simulate) { return steam.extract(amount, simulate); }
    @Override public boolean canConnectSteam(Direction side) { return true; }

    public boolean isBoiling() {
        return water > 0 && temperatureC() >= ThermalUnits.BOILING_TEMPERATURE_C;
    }

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, BoilerBlockEntity boiler) {
        if (!(level instanceof ServerLevel server)) return;

        boolean changed = boiler.thermal.coolOneTick() > 0;
        long time = level.getGameTime() + pos.asLong();

        if (boiler.isBoiling() && Math.floorMod(time, 20) == 0) {
            int waterByRoom = boiler.steam.room() / SteamUnits.STEAM_PER_WATER_MB;
            int waterUsed = Math.min(
                SteamUnits.BOILER_WATER_PER_SECOND_MB,
                Math.min(boiler.water, waterByRoom));

            if (waterUsed > 0) {
                boiler.water -= waterUsed;
                boiler.steam.receive(waterUsed * SteamUnits.STEAM_PER_WATER_MB, false);
                boiler.thermal.extract(
                    waterUsed * SteamUnits.BOILER_HEAT_COST_PER_WATER_MB_HU, false);
                changed = true;
            }
        }

        SteamTransfer.balanceAdjacent(server, pos, boiler);
        double pressure = boiler.pressureBar();

        if (boiler.isBoiling() && Math.floorMod(time, pressure >= 7.0 ? 4 : 7) == 0) {
            int count = pressure < 4.0 ? 1 : pressure < 8.0 ? 2 : 3;
            server.sendParticles(ParticleTypes.CLOUD,
                pos.getX() + 0.5, pos.getY() + 1.08, pos.getZ() + 0.5,
                count, 0.055, 0.025, 0.055, 0.025 + count * 0.007);
        }

        if (pressure >= SteamUnits.BOILER_WARNING_BAR) {
            if (Math.floorMod(time, 18) == 0)
                SteamEffects.leak(server, pos,
                    pressure >= SteamUnits.BOILER_STRESS_BAR ? 2 : 1, 0.04);
            if (Math.floorMod(time, 90) == 0)
                SteamEffects.hiss(server, pos, 0.28F);
            if (Math.floorMod(time, 120) == 0)
                server.playSound(null, pos, SoundEvents.ANVIL_LAND,
                    SoundSource.BLOCKS, 0.20F, 1.55F);
        }

        if (pressure >= SteamUnits.BOILER_STRESS_BAR)
            boiler.pressureStressTicks++;
        else
            boiler.pressureStressTicks = Math.max(0, boiler.pressureStressTicks - 2);

        if (pressure >= SteamUnits.BOILER_RUPTURE_BAR
                && boiler.pressureStressTicks >= SteamUnits.BOILER_RUPTURE_TICKS) {
            int released = boiler.steam.extract(Integer.MAX_VALUE, false);
            server.destroyBlock(pos, false);
            SteamEffects.rupture(server, pos, released, 3.5, 7.0F);
            return;
        }

        if (changed || (boiler.steamStored() > 0 && Math.floorMod(time, 20) == 0))
            boiler.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("HeatHU", thermal.storedHu());
        tag.putInt("Water", water);
        tag.putInt("Steam", steam.stored());
        tag.putInt("PressureStressTicks", pressureStressTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        thermal.load(tag.getInt("HeatHU"));
        water = Math.max(0,
            Math.min(ThermalUnits.BOILER_WATER_CAPACITY_MB, tag.getInt("Water")));
        steam.load(tag.getInt("Steam"));
        pressureStressTicks = Math.max(0, tag.getInt("PressureStressTicks"));
    }
}
