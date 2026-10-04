package net.caravidro.wayaround.littleleaf;

import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;

/** A world-wide ceiling includes unsuccessful probes, placements and birth attempts. */
public final class ColonyBudget {
    private static final WeakHashMap<ServerLevel,ColonyBudget> LEVELS=new WeakHashMap<>();
    private long tick=-1;private int builds,searches,routes,births;
    private static ColonyBudget at(ServerLevel l){var b=LEVELS.computeIfAbsent(l,k->new ColonyBudget());if(b.tick!=l.getGameTime()){b.tick=l.getGameTime();b.builds=64;b.searches=96;b.routes=32;b.births=2;}return b;}
    public static boolean build(ServerLevel l){return at(l).builds-->0;}
    public static boolean reserveBuild(ServerLevel l,int count){var b=at(l);if(b.builds<count)return false;b.builds-=count;return true;}
    public static boolean search(ServerLevel l){return at(l).searches-->0;}
    /** Reserve physical-route probes, borrowing unused discovery capacity without exceeding 128. */
    public static boolean route(ServerLevel l,int entityId,boolean tiny){
        if(tiny&&Math.floorMod(l.getGameTime()+entityId,2)!=0)return false;
        var b=at(l);if(b.routes>0){b.routes--;return true;}return b.searches-->0;
    }
    public static boolean searchAvailable(ServerLevel l){return at(l).searches>0;}
    public static boolean birth(ServerLevel l){return at(l).births-->0;}
    private ColonyBudget(){}
}
