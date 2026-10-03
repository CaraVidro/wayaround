package net.caravidro.wayaround.nexus;

import net.caravidro.wayaround.WayAround;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/** Lost animals can naturally use solid Nexus floors, without relaxing Overworld spawn rules. */
@EventBusSubscriber(modid=WayAround.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class NexusFauna {
    @SubscribeEvent public static void placements(RegisterSpawnPlacementsEvent e){allow(e,EntityType.SHEEP);allow(e,EntityType.COW);allow(e,EntityType.PIG);}
    private static <T extends Animal> void allow(RegisterSpawnPlacementsEvent e,EntityType<T> type) {
        e.register(type,null,null,(entity,level,reason,pos,random)->level.getLevel().dimension().equals(NexusPortalManager.NEXUS)
                &&pos.getY()>=80&&level.getBlockState(pos.below()).isValidSpawn(level,pos.below(),entity),RegisterSpawnPlacementsEvent.Operation.OR);
    }
    private NexusFauna() {}
}
