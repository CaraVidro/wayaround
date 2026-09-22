package net.caravidro.wayaround.industrial.power.thermal;

import net.caravidro.wayaround.industrial.power.PowerContent;
import net.minecraft.core.BlockPos;
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

public final class FireboxBlockEntity extends BlockEntity implements HeatSource {
    private int coal;
    private int burnTicks;
    private int nextReceiver;

    public FireboxBlockEntity(BlockPos pos, BlockState state) {
        super(PowerContent.FIREBOX_ENTITY.get(), pos, state);
    }

    public boolean addCoal() {
        if (coal >= 64) return false;
        coal++;
        setChanged();
        return true;
    }

    public boolean ignite() {
        if (burnTicks > 0 || coal <= 0) return false;
        coal--;
        burnTicks = ThermalUnits.COAL_BURN_TICKS;
        setChanged();
        return true;
    }

    public int storedCoal() { return coal; }
    public boolean isBurning() { return burnTicks > 0; }

    @Override
    public int heatOutputPerTick() {
        return isBurning() ? ThermalUnits.FIREBOX_HEAT_PER_TICK : 0;
    }

    public Component status() {
        String fuelKey = coal <= 0
            ? "message.wayaround.firebox.fuel_empty"
            : coal <= 4 ? "message.wayaround.firebox.fuel_low" : "message.wayaround.firebox.fuel_loaded";
        String stateKey = isBurning()
            ? "message.wayaround.firebox.burning"
            : coal > 0 ? "message.wayaround.firebox.ready" : "message.wayaround.firebox.idle";
        return Component.translatable("message.wayaround.firebox.status",
            Component.translatable(fuelKey), Component.translatable(stateKey));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FireboxBlockEntity firebox) {
        if (!(level instanceof ServerLevel server)) return;
        boolean changed = false;

        if (firebox.burnTicks > 0) {
            firebox.nextReceiver = ThermalTransfer.pushNetwork(server, pos, firebox, firebox.nextReceiver);

            long time = level.getGameTime() + pos.asLong();
            if (Math.floorMod(time, 8) == 0) {
                server.sendParticles(ParticleTypes.SMOKE,
                    pos.getX() + 0.5, pos.getY() + 0.82, pos.getZ() + 0.5,
                    1, 0.16, 0.05, 0.16, 0.01);
            }
            if (Math.floorMod(time, 15) == 0) {
                server.sendParticles(ParticleTypes.FLAME,
                    pos.getX() + 0.5, pos.getY() + 0.62, pos.getZ() + 0.5,
                    1, 0.12, 0.04, 0.12, 0.005);
            }
            if (Math.floorMod(time, 70) == 0) {
                server.playSound(null, pos, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS,
                    0.55F, 0.9F + server.random.nextFloat() * 0.2F);
            }

            firebox.burnTicks--;
            if (firebox.burnTicks == 0 && firebox.coal > 0) {
                firebox.coal--;
                firebox.burnTicks = ThermalUnits.COAL_BURN_TICKS;
            }
            changed = true;
        }

        boolean lit = firebox.burnTicks > 0;
        if (state.getValue(FireboxBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(FireboxBlock.LIT, lit), 3);
            changed = true;
        }
        if (changed) firebox.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Coal", coal);
        tag.putInt("BurnTicks", burnTicks);
        tag.putInt("NextReceiver", nextReceiver);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        coal = Math.max(0, Math.min(64, tag.getInt("Coal")));
        burnTicks = Math.max(0, Math.min(ThermalUnits.COAL_BURN_TICKS, tag.getInt("BurnTicks")));
        nextReceiver = tag.contains("NextReceiver")
            ? Math.max(0, tag.getInt("NextReceiver"))
            : Math.max(0, tag.getInt("NextSide"));
    }
}
