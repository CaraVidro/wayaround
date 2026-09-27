package net.caravidro.wayaround.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.JujutsuCastC2SPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = WayAround.MODID, value = Dist.CLIENT)
public final class JujutsuClientInput {
    public static final KeyMapping CAST = new KeyMapping(
            "Jujutsu: usar técnica",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "Way Around - Jujutsu"
    );

    private JujutsuClientInput() {}

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(CAST);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) return;

        while (CAST.consumeClick()) {
            PacketDistributor.sendToServer(new JujutsuCastC2SPayload((byte) 0));
        }
    }
}
