package net.caravidro.wayaround.ecology;

import net.caravidro.wayaround.WayAround;
import net.minecraft.world.entity.animal.AbstractFish;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(
        modid = WayAround.MODID,
        bus = EventBusSubscriber.Bus.MOD
)
public final class EcologyEntityAttributes {

    private EcologyEntityAttributes() {
    }

    @SubscribeEvent
    public static void attributes(
            EntityAttributeCreationEvent event
    ) {
        event.put(
                EcologyContent.SUNFISH.get(),
                AbstractFish.createAttributes()
                        .build()
        );
    }
}
