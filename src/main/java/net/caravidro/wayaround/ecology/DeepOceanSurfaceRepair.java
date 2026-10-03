package net.caravidro.wayaround.ecology;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;

/** One-time repair of the legacy trench feature's elevated, contiguous water sheet. */
public final class DeepOceanSurfaceRepair {
    private DeepOceanSurfaceRepair() {}
    public static boolean raisedLayer(LevelReader level, BlockPos p) {
        return raisedLayer(level,p,q->false);
    }
    private static boolean raisedLayer(LevelReader level, BlockPos p, java.util.function.Predicate<BlockPos> repairedNeighbor) {
        if (p.getY() != level.getSeaLevel() || !level.getBlockState(p).is(Blocks.WATER)) return false;
        for (int x=-1;x<=1;x++)for(int z=-1;z<=1;z++) {
            BlockPos q=p.offset(x,0,z);
            if(!level.hasChunkAt(q)||!(level.getBlockState(q).is(Blocks.WATER)&&level.getFluidState(q).isSource()
                    ||level.getBlockState(q).isAir()&&repairedNeighbor.test(q))||!level.getBlockState(q.above()).isAir()
                    ||!level.getBlockState(q.below()).is(Blocks.WATER)
                    ||!level.getBlockState(q.below(3)).is(Blocks.WATER))return false;
        }
        return true;
    }
    public static void repair(ServerLevel level, ChunkPos chunk) {
        var data=EcologyWorldData.get(level);
        if(data.isSurfaceRepaired(chunk.x,chunk.z))return;
        var remove=new ArrayList<BlockPos>(256);
        int y=level.getSeaLevel();
        for(int x=0;x<16;x++)for(int z=0;z<16;z++){
            BlockPos p=new BlockPos(chunk.getMinBlockX()+x,y,chunk.getMinBlockZ()+z);
            if(DeepOceanManager.isDeepOcean(level,p)&&raisedLayer(level,p,q->(q.getX()>>4!=chunk.x||q.getZ()>>4!=chunk.z)&&data.isSurfaceRepaired(q.getX()>>4,q.getZ()>>4)))remove.add(p);
        }
        // Collect before editing; removing one cell must not invalidate the next candidate.
        for(BlockPos p:remove)level.setBlock(p,Blocks.AIR.defaultBlockState(),18);
        data.markSurfaceRepaired(chunk.x,chunk.z);
    }
}
