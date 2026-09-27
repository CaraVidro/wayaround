package net.caravidro.wayaround.media.broadcast;

import net.caravidro.wayaround.media.MediaContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class EditorialBlockEntity extends BlockEntity {
    private BroadcastMode mode = BroadcastMode.ON_AIR;
    private BroadcastEffect effect = BroadcastEffect.CLEAN;

    public EditorialBlockEntity(BlockPos pos, BlockState state) {
        super(MediaContent.EDITORIAL_ENTITY.get(), pos, state);
    }

    public BroadcastMode mode() { return mode; }
    public BroadcastEffect effect() { return effect; }

    public void nextMode() {
        mode = mode.next();
        setChanged();
    }

    public void nextEffect() {
        effect = effect.next();
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Mode", mode.name());
        tag.putString("Effect", effect.name());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        try { mode = BroadcastMode.valueOf(tag.getString("Mode")); } catch (Exception ignored) {}
        try { effect = BroadcastEffect.valueOf(tag.getString("Effect")); } catch (Exception ignored) {}
    }
}
