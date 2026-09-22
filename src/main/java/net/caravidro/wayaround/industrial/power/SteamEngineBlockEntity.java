package net.caravidro.wayaround.industrial.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class SteamEngineBlockEntity extends BlockEntity {
    private final EnergyBudget buffer = new EnergyBudget(SteamCycle.CAPACITY);
    private int water;
    private int coal;
    private int fuel;
    private int heat;
    private int nextReceiver;
    private final IEnergyStorage output = new IEnergyStorage() {
        public int receiveEnergy(int amount, boolean simulate) { return 0; }
        public int extractEnergy(int amount, boolean simulate) {
            int result = buffer.extract(Math.min(1280, amount), simulate);
            if (!simulate && result > 0) setChanged();
            return result;
        }
        public int getEnergyStored() { return buffer.stored(); }
        public int getMaxEnergyStored() { return buffer.capacity(); }
        public boolean canExtract() { return true; }
        public boolean canReceive() { return false; }
    };

    public SteamEngineBlockEntity(BlockPos pos, BlockState state) {
        super(PowerContent.STEAM_ENGINE_ENTITY.get(), pos, state);
    }
    public IEnergyStorage energyOutput() { return output; }
    public boolean addWater() {
        if (water > SteamCycle.WATER_CAPACITY - 1000) return false;
        water += 1000;
        setChanged();
        return true;
    }
    public boolean addCoal() {
        if (coal >= 64) return false;
        coal++;
        setChanged();
        return true;
    }
    public Component status() {
        return Component.translatable("message.wayaround.steam_engine.status",
                water, SteamCycle.WATER_CAPACITY, heat, coal, buffer.stored(), buffer.capacity());
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state, SteamEngineBlockEntity engine) {
        if (!(level instanceof ServerLevel server)) return;
        if (Math.floorMod(level.getGameTime(), 20) == 0) {
            SteamCycle.Step step = SteamCycle.tick(engine.water, engine.fuel, engine.heat,
                    engine.coal > 0, engine.buffer.capacity() - engine.buffer.stored());
            engine.water = step.water();
            engine.fuel = step.fuel();
            engine.heat = step.heat();
            if (step.consumeCoal()) engine.coal--;
            engine.buffer.add(step.generated());
            boolean lit = engine.fuel > 0;
            if (state.getValue(SteamEngineBlock.LIT) != lit) {
                level.setBlock(pos, state.setValue(SteamEngineBlock.LIT, lit), 3);
            }
            if (step.generated() > 0) {
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                        pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 2, 0.1, 0.1, 0.1, 0.02);
            }
            engine.setChanged();
        }
        if (engine.buffer.stored() > 0 && Math.floorMod(level.getGameTime() + pos.asLong(), 10) == 0) {
            engine.nextReceiver = EnergyNetwork.distribute(server, pos, engine.buffer, engine.nextReceiver);
            engine.setChanged();
        }
    }
    public int storedCoal() { return coal; }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Water", water);
        tag.putInt("Coal", coal);
        tag.putInt("Fuel", fuel);
        tag.putInt("Heat", heat);
        tag.putInt("Energy", buffer.stored());
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        water = Math.clamp(tag.getInt("Water"), 0, SteamCycle.WATER_CAPACITY);
        coal = Math.clamp(tag.getInt("Coal"), 0, 64);
        fuel = Math.clamp(tag.getInt("Fuel"), 0, SteamCycle.COAL_SECONDS);
        heat = Math.clamp(tag.getInt("Heat"), 0, 100);
        buffer.load(tag.getInt("Energy"));
    }
}
