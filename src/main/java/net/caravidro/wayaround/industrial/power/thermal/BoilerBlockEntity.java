package net.caravidro.wayaround.industrial.power.thermal;

import net.caravidro.wayaround.industrial.power.PowerContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class BoilerBlockEntity extends BlockEntity implements HeatReceiver {
    private final ThermalStorage thermal = new ThermalStorage(
        ThermalUnits.BOILER_THERMAL_MASS_HU_PER_C, ThermalUnits.BOILER_MAX_TEMPERATURE_C);
    private int water;

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

    public boolean isBoiling() {
        return water > 0 && temperatureC() >= ThermalUnits.BOILING_TEMPERATURE_C;
    }

    public Component status() {
        double temperature = temperatureC();
        String heatKey = temperature < 40 ? "message.wayaround.boiler.cold"
            : temperature < 100 ? "message.wayaround.boiler.warm"
            : temperature < 250 ? "message.wayaround.boiler.hot"
            : temperature < 500 ? "message.wayaround.boiler.very_hot"
            : "message.wayaround.boiler.danger";

        double fill = water / (double) ThermalUnits.BOILER_WATER_CAPACITY_MB;
        String waterKey = water <= 0 ? "message.wayaround.boiler.water_dry"
            : fill < 0.25 ? "message.wayaround.boiler.water_low"
            : fill < 0.90 ? "message.wayaround.boiler.water_ok"
            : "message.wayaround.boiler.water_full";

        return Component.translatable("message.wayaround.boiler.status",
            Component.translatable(heatKey), Component.translatable(waterKey));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BoilerBlockEntity boiler) {
        if (!(level instanceof ServerLevel server)) return;

        boolean changed = boiler.thermal.coolOneTick() > 0;
        long time = level.getGameTime() + pos.asLong();

        if (boiler.isBoiling()) {
            double temperature = boiler.temperatureC();
            int steamCount = temperature < 180 ? 1 : temperature < 350 ? 2 : 3;

            if (Math.floorMod(time, 5) == 0) {
                server.sendParticles(ParticleTypes.CLOUD,
                    pos.getX() + 0.5, pos.getY() + 1.06, pos.getZ() + 0.5,
                    steamCount, 0.08, 0.03, 0.08, 0.025 + steamCount * 0.005);
            }

            if (Math.floorMod(time, 80) == 0) {
                server.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS,
                    0.22F + steamCount * 0.08F,
                    1.35F + server.random.nextFloat() * 0.2F);
            }

            if (Math.floorMod(time, 20) == 0) {
                boiler.water = Math.max(0,
                    boiler.water - ThermalUnits.BOILER_EVAPORATION_MB_PER_SECOND);
                boiler.thermal.extract(ThermalUnits.BOILER_EVAPORATION_HEAT_HU_PER_SECOND, false);
                changed = true;
            }
        }

        if (changed && Math.floorMod(level.getGameTime(), 20) == 0)
            boiler.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("HeatHU", thermal.storedHu());
        tag.putInt("Water", water);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        thermal.load(tag.getInt("HeatHU"));
        water = Math.max(0, Math.min(ThermalUnits.BOILER_WATER_CAPACITY_MB, tag.getInt("Water")));
    }
}
