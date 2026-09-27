package net.caravidro.wayaround.client;

import net.caravidro.wayaround.WayAround;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** Client MOD-bus registration kept separate from the GAME-bus input listener. */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD
)
public final class JujutsuClientModEvents {
    private JujutsuClientModEvents() {}

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(JujutsuClientInput.CAST);
    }
}
