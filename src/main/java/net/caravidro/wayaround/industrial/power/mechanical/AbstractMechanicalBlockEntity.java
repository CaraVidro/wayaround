package net.caravidro.wayaround.industrial.power.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractMechanicalBlockEntity extends BlockEntity implements MechanicalNode {
    private double rpm;
    private long lastMechanicalTick = Long.MIN_VALUE;
    protected int overspeedTicks;

    protected AbstractMechanicalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override public BlockPos mechanicalPos() { return worldPosition; }
    @Override public double mechanicalRpm() { return rpm; }

    @Override
    public void setMechanicalRpm(double rpm) {
        if (!Double.isFinite(rpm)) rpm = 0.0;
        this.rpm = rpm;
    }

    @Override public long lastMechanicalTick() { return lastMechanicalTick; }
    @Override public void setLastMechanicalTick(long tick) { lastMechanicalTick = tick; }
    @Override public double loadTorque() { return 0.0; }
    @Override public double driveTorque() { return 0.0; }

    protected void updateOverspeed() {
        if (Math.abs(rpm) > maxSafeRpm())
            overspeedTicks++;
        else
            overspeedTicks = Math.max(0, overspeedTicks - 2);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("MechanicalRpm", rpm);
        tag.putInt("OverspeedTicks", overspeedTicks);
        saveMechanicalExtra(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        rpm = Double.isFinite(tag.getDouble("MechanicalRpm"))
            ? tag.getDouble("MechanicalRpm") : 0.0;
        overspeedTicks = Math.max(0, tag.getInt("OverspeedTicks"));
        loadMechanicalExtra(tag, registries);
    }

    protected void saveMechanicalExtra(CompoundTag tag, HolderLookup.Provider registries) {}
    protected void loadMechanicalExtra(CompoundTag tag, HolderLookup.Provider registries) {}
}
