package net.caravidro.wayaround.industrial.power;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.caravidro.wayaround.industrial.assembly.AssemblyConnection;
import net.caravidro.wayaround.industrial.assembly.AssemblyMachine;
import net.caravidro.wayaround.industrial.assembly.AssemblyPartNode;
import net.caravidro.wayaround.industrial.assembly.AssemblyPartProfile;
import net.caravidro.wayaround.industrial.assembly.LegacyMachineAssembly;
import net.caravidro.wayaround.worldgen.weather.BlizzardManager;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class SolarPanelBlockEntity
        extends BlockEntity
        implements AssemblyMachine {
    public static final int CAPACITY = 32_000;
    private static final int TRANSFER_INTERVAL = 10;
    private static final int TRANSFER_BUDGET = 1_280;
    private static final int MAX_CABLES = 128;
    private static final int MAX_RECEIVERS = 64;
    private final EnergyBudget buffer = new EnergyBudget(CAPACITY);
    private double fractionalEnergy;
    private double generationPerTick;
    private double fog;
    private long lastEnvironmentCheck = Long.MIN_VALUE;
    private int nextReceiver;
    private float assemblyWear;

    private final IEnergyStorage output = new IEnergyStorage() {
        @Override public int receiveEnergy(int amount, boolean simulate) { return 0; }
        @Override public int extractEnergy(int amount, boolean simulate) {
            int extracted = buffer.extract(Math.min(TRANSFER_BUDGET, amount), simulate);
            if (!simulate && extracted > 0) setChanged();
            return extracted;
        }
        @Override public int getEnergyStored() { return buffer.stored(); }
        @Override public int getMaxEnergyStored() { return buffer.capacity(); }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return false; }
    };

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(PowerContent.SOLAR_PANEL_ENTITY.get(), pos, state);
    }

    public IEnergyStorage energyOutput() { return output; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SolarPanelBlockEntity panel) {
        if (!WorldFeatureRuntime.enabled(level, WorldFeature.POWER_NETWORKS)) return;
        if (!(level instanceof ServerLevel server)) return;
        long time = level.getGameTime();
        if (panel.lastEnvironmentCheck == Long.MIN_VALUE || time - panel.lastEnvironmentCheck >= 20L) {
            panel.updateEnvironment(server);
            panel.lastEnvironmentCheck = time;
        }

        int before = panel.buffer.stored();
        if (before < CAPACITY && panel.generationPerTick > 0) {
            panel.fractionalEnergy += panel.generationPerTick;
            int generated = (int) panel.fractionalEnergy;
            panel.fractionalEnergy -= generated;
            panel.buffer.add(generated);
        }
        if (panel.buffer.stored() > 0 && Math.floorMod(time + pos.asLong(), TRANSFER_INTERVAL) == 0) {
            panel.nextReceiver = EnergyNetwork.distribute(server, pos, panel.buffer, panel.nextReceiver);
        }
        if (before != panel.buffer.stored()) panel.setChanged();
    }

    private void updateEnvironment(ServerLevel level) {
        BlockPos above = worldPosition.above();
        boolean exposed = level.dimension() == Level.OVERWORLD && level.canSeeSky(above)
            && level.getHeight(Heightmap.Types.WORLD_SURFACE, above.getX(), above.getZ()) <= above.getY();
        fog = BlizzardManager.getIntensity(level, Vec3.atCenterOf(worldPosition));
        generationPerTick = SolarPower.generation(level.getDayTime(), worldPosition.getZ(),
            level.getRainLevel(1.0F), level.getThunderLevel(1.0F), fog, exposed);
    }

    public Component status() {
        if (level instanceof ServerLevel server) updateEnvironment(server);
        return Component.translatable("message.wayaround.solar_panel.status",
            String.format(Locale.ROOT, "%.1f", generationPerTick), buffer.stored(), CAPACITY,
            Math.round(SolarPower.latitudeEfficiency(worldPosition.getZ()) * 100), Math.round(fog * 100));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", buffer.stored());
        tag.putDouble("FractionalEnergy", fractionalEnergy);
        tag.putFloat("AssemblyWear", assemblyWear);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        buffer.load(tag.getInt("Energy"));
        double savedFraction = tag.getDouble("FractionalEnergy");
        fractionalEnergy = Double.isFinite(savedFraction) ? Math.max(0, Math.min(0.999999, savedFraction)) : 0;
        assemblyWear = Mth.clamp(tag.getFloat("AssemblyWear"), 0.0F, 1.0F);
        lastEnvironmentCheck = Long.MIN_VALUE;
    }
}
