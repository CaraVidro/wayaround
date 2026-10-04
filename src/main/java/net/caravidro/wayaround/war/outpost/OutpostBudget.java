package net.caravidro.wayaround.war.outpost;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.caravidro.wayaround.war.WarProjectileEntity;
/** Shared per-level ceilings, not a separate allowance for each device. */
public final class OutpostBudget {
    private static final WeakHashMap<ServerLevel,long[]> COUNTERS=new WeakHashMap<>();
    private static boolean take(ServerLevel l,int i,int cap){var c=COUNTERS.computeIfAbsent(l,k->new long[4]);if(c[0]!=l.getGameTime()){c[0]=l.getGameTime();c[1]=c[2]=c[3]=0;}return c[i]++<cap;}
    public static boolean sensor(ServerLevel l){return take(l,1,64);}
    public static boolean effect(ServerLevel l){return take(l,2,24);}
    public static boolean projectile(ServerLevel l,BlockPos p){return take(l,3,64)&&l.getEntitiesOfClass(WarProjectileEntity.class,new AABB(p).inflate(64)).size()<128;}
    private OutpostBudget(){}
}
