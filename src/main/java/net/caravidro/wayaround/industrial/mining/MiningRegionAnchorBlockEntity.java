package net.caravidro.wayaround.industrial.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class MiningRegionAnchorBlockEntity extends BlockEntity {
    public enum Stage {
        STRUCTURE,
        ORES,
        STRUCTURES,
        MOBS,
        COMPLETE
    }

    private ComplexOreKind kind = ComplexOreKind.IRON;
    private boolean openPit;
    private int geometryVersion=1;
    private boolean roofRepairOnly;
    private int radius = 20;
    private int cursor;
    private Stage stage = Stage.STRUCTURE;

    public MiningRegionAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(MiningContent.REGION_ANCHOR_ENTITY.get(), pos, state);
    }

    @Override public void onLoad(){super.onLoad();DeferredMiningManager.resume(this);}
    public boolean roofRepairOnly(){return roofRepairOnly;}

    public void configure(ComplexOreKind kind, boolean openPit, int radius) {
        this.kind = kind == null ? ComplexOreKind.IRON : kind;
        this.openPit = openPit;
        this.radius = Mth.clamp(radius, 12, 36);
        this.geometryVersion=1;this.roofRepairOnly=false;
        this.cursor = 0;
        this.stage = Stage.STRUCTURE;
        sync();
    }

    public ComplexOreKind kind() { return kind; }
    public boolean openPit() { return openPit; }
    public int radius() { return radius; }
    public int cursor() { return cursor; }
    public Stage stage() { return stage; }
    public boolean complete() { return stage == Stage.COMPLETE; }

    public void cursor(int cursor) {
        this.cursor = Math.max(0, cursor);
        setChanged();
    }

    public void advance() {
        if (stage == Stage.COMPLETE) return;
        stage = Stage.values()[stage.ordinal() + 1];
        cursor = 0;
        sync();
    }

    public void completeNow() {
        stage = Stage.COMPLETE;
        cursor = 0;
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("GeometryVersion",geometryVersion);
        tag.putBoolean("RoofRepairOnly",roofRepairOnly);
        tag.putInt("Kind", kind.ordinal());
        tag.putBoolean("OpenPit", openPit);
        tag.putInt("Radius", radius);
        tag.putInt("Cursor", cursor);
        tag.putInt("Stage", stage.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ComplexOreKind[] kinds = ComplexOreKind.values();
        Stage[] stages = Stage.values();
        kind = kinds[Math.floorMod(tag.getInt("Kind"), kinds.length)];
        openPit = tag.getBoolean("OpenPit");
        radius = Mth.clamp(tag.getInt("Radius"), 12, 36);
        cursor = Math.max(0, tag.getInt("Cursor"));
        stage = stages[Math.floorMod(tag.getInt("Stage"), stages.length)];
        geometryVersion=tag.getInt("GeometryVersion");roofRepairOnly=tag.getBoolean("RoofRepairOnly");
        if(openPit&&geometryVersion<1){
            roofRepairOnly=stage==Stage.COMPLETE;stage=Stage.STRUCTURE;cursor=0;geometryVersion=1;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Kind", kind.ordinal());
        tag.putBoolean("OpenPit", openPit);
        tag.putInt("Radius", radius);
        tag.putInt("Stage", stage.ordinal());
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
