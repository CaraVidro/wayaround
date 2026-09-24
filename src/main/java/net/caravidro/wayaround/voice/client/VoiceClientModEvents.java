package net.caravidro.wayaround.voice.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.caravidro.wayaround.WayAround;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class VoiceClientModEvents {

    private VoiceClientModEvents() {
    }

    public static final KeyMapping PUSH_TO_TALK =
            new KeyMapping(
                    "Way Around - Push to Talk",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_V,
                    "key.categories.misc"
            );

    @SubscribeEvent
    public static void registerKeys(
            RegisterKeyMappingsEvent event
    ) {
        event.register(PUSH_TO_TALK);
    }
}
