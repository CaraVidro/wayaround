package net.caravidro.wayaround.littleleaf;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/** A gallery may carve generated dirt, but it must not erase a player's construction. */
@EventBusSubscriber(modid=WayAround.MODID)
public final class ColonyInteriorProtection {
    @SubscribeEvent public static void placed(BlockEvent.EntityPlaceEvent event){
        if(!(event.getLevel() instanceof ServerLevel l)||!l.dimension().equals(ColonyTravel.DIMENSION))return;
        var p=event.getPos();var core=new BlockPos(Math.floorDiv(p.getX(),128)*128+24,32,Math.floorDiv(p.getZ(),128)*128+57);
        if(ColonyCoreBlockEntity.loaded(l,core)&&l.getBlockEntity(core) instanceof ColonyCoreBlockEntity c){
            if(event instanceof BlockEvent.EntityMultiPlaceEvent multi){for(var snapshot:multi.getReplacedBlockSnapshots())c.protectInterior(snapshot.getPos());}else c.protectInterior(p);
        }
    }
    private ColonyInteriorProtection(){}
}
