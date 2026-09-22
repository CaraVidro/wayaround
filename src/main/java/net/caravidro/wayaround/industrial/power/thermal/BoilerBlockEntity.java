package net.caravidro.wayaround.industrial.power.thermal;

import net.caravidro.wayaround.industrial.power.PowerContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class BoilerBlockEntity extends BlockEntity implements HeatReceiver {
    private final ThermalStorage thermal = new ThermalStorage(
        ThermalUnits.BOILER_THERMAL_MASS_HU_PER_C, ThermalUnits.BOILER_MAX_TEMPERATURE_C);

    public BoilerBlockEntity(BlockPos pos, BlockState state) {
        super(PowerContent.BOILER_ENTITY.get(), pos, state);
    }

    @Override
    public int receiveHeat(Direction from, int heatUnits, boolean simulate) {
        int accepted = thermal.receive(heatUnits, simulate);
        if (!simulate && accepted > 0) setChanged();
        return accepted;
    }

    public double temperatureC() { return thermal.temperatureC(); }
    public int storedHeatHu() { return thermal.storedHu(); }

    public Component status() {
        double temperature = temperatureC();
        String key = temperature < 40 ? "message.wayaround.boiler.cold"
            : temperature < 100 ? "message.wayaround.boiler.warm"
            : temperature < 250 ? "message.wayaround.boiler.hot"
            : temperature < 500 ? "message.wayaround.boiler.very_hot"
            : "message.wayaround.boiler.danger";
        return Component.translatable("message.wayaround.boiler.status", Component.translatable(key));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BoilerBlockEntity boiler) {
        int lost = boiler.thermal.coolOneTick();
        if (lost > 0 && Math.floorMod(level.getGameTime(), 20) == 0) boiler.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("HeatHU", thermal.storedHu());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        thermal.load(tag.getInt("HeatHU"));
    }
}
