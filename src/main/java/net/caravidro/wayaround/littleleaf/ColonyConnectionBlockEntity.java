package net.caravidro.wayaround.littleleaf;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ColonyConnectionBlockEntity extends BlockEntity {
    private BlockPos home;
    public ColonyConnectionBlockEntity(BlockPos p,BlockState s){super(LittleLeafContent.CONNECTION_ENTITY.get(),p,s);}
    public BlockPos home(){return home;}
    public boolean bind(ColonyCoreBlockEntity core){if(!core.addConnection(worldPosition))return false;home=core.getBlockPos().immutable();level.setBlock(worldPosition,getBlockState().setValue(ColonyCoreBlock.SPECIES,core.species()),3);setChanged();return true;}
    public static void tick(Level l,BlockPos p,BlockState state,ColonyConnectionBlockEntity c){
        if(!(l instanceof ServerLevel s)||c.home==null||l.getGameTime()%120!=Math.floorMod(p.asLong(),120)||!ColonyCoreBlockEntity.loaded(s,c.home))return;
        if(l.getBlockEntity(c.home) instanceof ColonyCoreBlockEntity core&&!core.abandoned()&&core.giant()&&!core.wetWeather(s))core.birthAt(s,p.above());
    }
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);if(home!=null)t.putLong("Home",home.asLong());}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);home=t.contains("Home")?BlockPos.of(t.getLong("Home")):null;}
}
