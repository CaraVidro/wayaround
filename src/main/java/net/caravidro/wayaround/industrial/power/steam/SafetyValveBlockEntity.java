package net.caravidro.wayaround.industrial.power.steam;

import net.caravidro.wayaround.industrial.power.PowerContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class SafetyValveBlockEntity extends BlockEntity implements SteamNode {
    private final SteamStorage steam = new SteamStorage(SteamUnits.VALVE_STEAM_CAPACITY);
    private int manualTicks;

    public SafetyValveBlockEntity(BlockPos pos, BlockState state) {
        super(PowerContent.SAFETY_VALVE_ENTITY.get(), pos, state);
    }

    public void openManually(int ticks) {
        manualTicks = Math.max(manualTicks, ticks);
        setChanged();
    }

    @Override public int steamStored() { return steam.stored(); }
    @Override public int steamCapacity() { return steam.capacity(); }
    @Override public double pressureBar() { return steam.pressureBar(); }
    @Override public int receiveSteam(int amount, boolean simulate) { return steam.receive(amount, simulate); }
    @Override public int extractSteam(int amount, boolean simulate) { return steam.extract(amount, simulate); }
    @Override public boolean canConnectSteam(Direction side) { return true; }

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, SafetyValveBlockEntity valve) {
        if (!(level instanceof ServerLevel server)) return;

        SteamTransfer.balanceAdjacent(server, pos, valve);
        if (valve.manualTicks > 0) valve.manualTicks--;

        boolean shouldOpen = valve.manualTicks > 0
            || valve.pressureBar() >= SteamUnits.SAFETY_VALVE_OPEN_BAR;

        if (state.getValue(SafetyValveBlock.OPEN) != shouldOpen) {
            level.setBlock(pos, state.setValue(SafetyValveBlock.OPEN, shouldOpen), 3);
            state = level.getBlockState(pos);
        }

        if (shouldOpen && valve.steamStored() > 0) {
            int vented = valve.steam.extract(SteamUnits.SAFETY_VALVE_VENT_PER_TICK, false);
            long time = level.getGameTime() + pos.asLong();

            if (vented > 0 && Math.floorMod(time, 2) == 0)
                SteamEffects.leak(server, pos.above(), 2, 0.075);
            if (vented > 0 && Math.floorMod(time, 24) == 0)
                SteamEffects.hiss(server, pos, 0.48F);

            valve.setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Steam", steam.stored());
        tag.putInt("ManualTicks", manualTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        steam.load(tag.getInt("Steam"));
        manualTicks = Math.max(0, tag.getInt("ManualTicks"));
    }
}
