package net.caravidro.wayaround.worldgen.weather.fire;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** Density is sampled once per 8x4x8 cell/window, never 147 reads per fire. */
public final class FireTickLimiter {
    private static final Map<ServerLevel,FireWorkBudget> TICKS=new WeakHashMap<>();
    private static final Map<ServerLevel,Map<Long,Density>> DENSITY=new WeakHashMap<>();
    private static final class Density { long at; int count; Density(long at,int count){this.at=at;this.count=count;} }
    private FireTickLimiter() {}
    public static boolean allowTick(ServerLevel level) {
        return TICKS.computeIfAbsent(level,l->new FireWorkBudget(1,64)).tryUse(level.getGameTime());
    }
    public static boolean shouldThin(ServerLevel level,BlockPos pos) {
        if(level.getBlockState(pos.below()).is(level.dimensionType().infiniburn()))return false;
        int x=pos.getX() & ~7,y=pos.getY() & ~3,z=pos.getZ() & ~7;
        var chunk=level.getChunkSource().getChunkNow(x>>4,z>>4);
        if(chunk==null || level.isOutsideBuildHeight(y))return false;
        var cells=DENSITY.computeIfAbsent(level,l->new HashMap<>());
        long key=BlockPos.asLong(x,y,z),now=level.getGameTime();Density density=cells.get(key);
        if(density==null || now-density.at>=4 || now<density.at) {
            var section=chunk.getSection(chunk.getSectionIndex(y));int count=0;
            if(!section.hasOnlyAir())for(int dx=0;dx<8;dx++)for(int dy=0;dy<4;dy++)for(int dz=0;dz<8;dz++)
                if(section.getBlockState((x+dx)&15,(y+dy)&15,(z+dz)&15).is(Blocks.FIRE))count++;
            density=new Density(now,count);
            if(cells.size()>=512)cells.entrySet().removeIf(e->now-e.getValue().at>=4);
            if(cells.size()>=512)cells.clear();
            cells.put(key,density);
        }
        if(density.count<32)return false;
        density.count--;return true;
    }
    public static void clear(){TICKS.clear();DENSITY.clear();}
}
