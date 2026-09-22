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

public final class SteamPipeBlockEntity extends BlockEntity implements SteamNode {
    private final SteamStorage steam = new SteamStorage(SteamUnits.PIPE_STEAM_CAPACITY);
    private int stressTicks;

    public SteamPipeBlockEntity(BlockPos pos, BlockState state) {
        super(PowerContent.STEAM_PIPE_ENTITY.get(), pos, state);
    }

    @Override public int steamStored() { return steam.stored(); }
    @Override public int steamCapacity() { return steam.capacity(); }
    @Override public double pressureBar() { return steam.pressureBar(); }
    @Override public int receiveSteam(int amount, boolean simulate) { return steam.receive(amount, simulate); }
    @Override public int extractSteam(int amount, boolean simulate) { return steam.extract(amount, simulate); }
    @Override public boolean canConnectSteam(Direction side) { return true; }

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, SteamPipeBlockEntity pipe) {
        if (!(level instanceof ServerLevel server)) return;

        SteamTransfer.balanceAdjacent(server, pos, pipe);
        double pressure = pipe.pressureBar();
        long time = level.getGameTime() + pos.asLong();

        if (pressure >= SteamUnits.PIPE_WARNING_BAR) {
            if (Math.floorMod(time, pressure >= SteamUnits.PIPE_STRESS_BAR ? 10 : 18) == 0)
                SteamEffects.leak(server, pos, pressure >= SteamUnits.PIPE_STRESS_BAR ? 2 : 1, 0.035);
            if (Math.floorMod(time, 80) == 0)
                SteamEffects.hiss(server, pos, 0.22F);
        }

        if (pressure >= SteamUnits.PIPE_STRESS_BAR)
            pipe.stressTicks++;
        else
            pipe.stressTicks = Math.max(0, pipe.stressTicks - 2);

        if (pressure >= SteamUnits.PIPE_RUPTURE_BAR
                && pipe.stressTicks >= SteamUnits.PIPE_RUPTURE_TICKS) {
            int released = pipe.steam.extract(Integer.MAX_VALUE, false);
            server.destroyBlock(pos, false);
            SteamEffects.rupture(server, pos, released, 2.5, 4.0F);
            return;
        }

        if (pipe.steamStored() > 0 && Math.floorMod(time, 20) == 0)
            pipe.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Steam", steam.stored());
        tag.putInt("StressTicks", stressTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        steam.load(tag.getInt("Steam"));
        stressTicks = Math.max(0, tag.getInt("StressTicks"));
    }
}
