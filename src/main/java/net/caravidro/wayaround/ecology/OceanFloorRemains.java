package net.caravidro.wayaround.ecology;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;

/** Moves real wreck containers, retaining unopened loot tables and every stack. */
public final class OceanFloorRemains {
    private OceanFloorRemains() {}
    public static boolean settle(LevelAccessor level, BlockPos from, int floorY) {
        BlockEntity entity=level.getBlockEntity(from);
        if(!(entity instanceof RandomizableContainerBlockEntity container) || !(entity instanceof net.minecraft.world.Container inventory))return false;
        BlockPos to=new BlockPos(from.getX(),floorY+1,from.getZ());
        for(int stacked=0;stacked<4 && level.getBlockEntity(to)!=null;stacked++)to=to.above();
        if(to.equals(from) || !level.getBlockState(to).is(Blocks.WATER) || level.getBlockEntity(to)!=null)return false;
        CompoundTag saved=entity.saveWithFullMetadata(level.registryAccess());BlockState original=level.getBlockState(from),state=original;
        if(state.hasProperty(ChestBlock.TYPE))state=state.setValue(ChestBlock.TYPE,net.minecraft.world.level.block.state.properties.ChestType.SINGLE);
        // Clear before onRemove; the original cannot spill a second inventory.
        container.setLootTable(null);inventory.clearContent();
        level.setBlock(from,Blocks.WATER.defaultBlockState(),2);level.setBlock(to,state,2);
        BlockEntity moved=level.getBlockEntity(to);if(moved!=null)moved.loadWithComponents(saved,level.registryAccess());
        if(moved==null) {
            level.setBlock(to,Blocks.WATER.defaultBlockState(),2);level.setBlock(from,original,2);
            BlockEntity restored=level.getBlockEntity(from);if(restored!=null)restored.loadWithComponents(saved,level.registryAccess());
            return false;
        }
        return true;
    }
    public static void repair(ServerLevel level, ChunkPos pos) {
        if(!level.hasChunk(pos.x,pos.z))return;
        int checked=0;
        for(BlockEntity entity:java.util.List.copyOf(level.getChunk(pos.x,pos.z).getBlockEntities().values())) {
            if(checked++>=32)break;
            BlockPos p=entity.getBlockPos();
            if(!(entity instanceof RandomizableContainerBlockEntity) || !DeepOceanBiomes.contains(level,p)
                    || !level.getFluidState(p.below()).is(net.minecraft.tags.FluidTags.WATER))continue;
            CompoundTag tag=entity.saveWithFullMetadata(level.registryAccess());
            String loot=tag.getString("LootTable");
            if(!loot.startsWith("minecraft:chests/shipwreck") && !loot.startsWith("minecraft:chests/underwater_ruin"))continue;
            for(int y=p.getY()-1;y>level.getMinBuildHeight()+2;y--) {
                BlockPos below=new BlockPos(p.getX(),y,p.getZ());
                if(!level.getBlockState(below).getCollisionShape(level,below).isEmpty()) { settle(level,p,y);break; }
            }
        }
    }
}
