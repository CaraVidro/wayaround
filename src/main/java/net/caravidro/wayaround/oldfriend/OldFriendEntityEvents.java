package net.caravidro.wayaround.oldfriend;

import net.caravidro.wayaround.WayAround;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(
        modid = WayAround.MODID,
        bus = EventBusSubscriber.Bus.MOD
)
public final class OldFriendEntityEvents {

    private OldFriendEntityEvents() {
    }

    @SubscribeEvent
    public static void attributes(
            EntityAttributeCreationEvent event
    ) {
        event.put(
                OldFriendContent.HEROBRINE.get(),
                Mob.createMobAttributes()
                        .add(
                                Attributes.MAX_HEALTH,
                                20.0
                        )
                        .add(
                                Attributes.MOVEMENT_SPEED,
                                0.34
                        )
                        .add(
                                Attributes.FOLLOW_RANGE,
                                40.0
                        )
                        .build()
        );
    }
}
