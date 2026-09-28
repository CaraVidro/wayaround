package net.caravidro.wayaround.client;

import net.caravidro.wayaround.WayAround;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Remembers what kind of session the title screen was returned from.
 */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class MenuBrandingState {

    public enum ReturnContext {
        NONE,
        SINGLEPLAYER,
        MULTIPLAYER
    }

    private static ReturnContext context =
            ReturnContext.NONE;

    private MenuBrandingState() {
    }

    @SubscribeEvent
    public static void tick(
            ClientTickEvent.Post event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || minecraft.player == null) {
            return;
        }

        context =
                minecraft.hasSingleplayerServer()
                        ? ReturnContext.SINGLEPLAYER
                        : ReturnContext.MULTIPLAYER;
    }

    public static ReturnContext context() {
        return context;
    }
}
