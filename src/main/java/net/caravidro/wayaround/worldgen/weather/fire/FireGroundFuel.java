package net.caravidro.wayaround.worldgen.weather.fire;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
public final class FireGroundFuel {
    public static void scorch(ServerLevel l,BlockPos p){
        if(!l.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK))return;
        for(Direction d:Direction.values()){var q=p.relative(d);if(l.getChunkSource().getChunkNow(q.getX()>>4,q.getZ()>>4)==null)continue;
            if(l.getBlockState(q).is(Blocks.GRASS_BLOCK))l.setBlock(q,Blocks.DIRT.defaultBlockState(),3);
        }
    }
}
