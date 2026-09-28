package net.caravidro.wayaround.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.accessory.AccessoryKind;
import net.caravidro.wayaround.accessory.AccessorySlot;
import net.caravidro.wayaround.accessory.TopHatContent;
import net.caravidro.wayaround.network.TopHatAdjustC2SPayload;
import net.caravidro.wayaround.network.TrouserPocketRetrieveC2SPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class TopHatClientEvents {

    public static final KeyMapping ADJUST =
            new KeyMapping(
                    "key.wayaround.adjust_top_hat",
                    KeyConflictContext.IN_GAME,
                    KeyModifier.NONE,
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_R,
                    "key.categories.wayaround.accessories"
            );

    private TopHatClientEvents() {
    }

    @SubscribeEvent
    public static void tick(
            ClientTickEvent.Post event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.level == null
                || minecraft.screen != null) {
            return;
        }

        while (ADJUST.consumeClick()) {
            AccessoryClientState.State accessories =
                    AccessoryClientState.get(
                            minecraft.player.getUUID()
                    );

            if (accessories == null) {
                continue;
            }

            /*
             * R is contextual. A loose top hat always wins; the trouser pocket
             * only receives the key when the hat has nothing urgent to do.
             */
            boolean wearingTopHat =
                    accessories.kind(
                            AccessorySlot.HEAD
                    ) == AccessoryKind.ENGINEER_CAP;

            boolean topHatNeedsAttention =
                    wearingTopHat
                            && TopHatClientState.warningActive(
                            minecraft.player.getUUID()
                    );

            if (topHatNeedsAttention) {
                TopHatClientState.predictAdjust(
                        minecraft.player.getUUID()
                );

                PacketDistributor.sendToServer(
                        new TopHatAdjustC2SPayload()
                );

                continue;
            }

            if (wearingTopHat
                    && TopHatClientState.adjusting(
                    minecraft.player.getUUID()
            )) {
                continue;
            }

            if (accessories.kind(
                    AccessorySlot.LEGS
            ) == AccessoryKind.ENGINEER_TROUSERS
                    && accessories.trouserPocket() != null
                    && !accessories.trouserPocket()
                    .isEmpty()
                    && !TrouserPocketClientState.active(
                    minecraft.player.getUUID()
            )) {

                TrouserPocketClientState.predict(
                        minecraft.player.getUUID()
                );

                PacketDistributor.sendToServer(
                        new TrouserPocketRetrieveC2SPayload()
                );
            }
        }
    }

    @SubscribeEvent
    public static void hud(
            RenderGuiEvent.Post event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.options.hideGui
                || !TopHatClientState.warningActive(
                minecraft.player.getUUID()
        )) {
            return;
        }

        int screenWidth =
                minecraft.getWindow()
                        .getGuiScaledWidth();

        int x =
                screenWidth / 2
                        + 72;

        int y =
                24;

        int pulse =
                (
                        minecraft.player.tickCount / 4
                ) % 2;

        int accent =
                pulse == 0
                        ? 0xFFE2A84A
                        : 0xFFFFD06A;

        event.getGuiGraphics()
                .fill(
                        x,
                        y,
                        x + 106,
                        y + 20,
                        0xB0100D0A
                );

        event.getGuiGraphics()
                .fill(
                        x,
                        y,
                        x + 3,
                        y + 20,
                        accent
                );

        // Tiny top-hat silhouette.
        event.getGuiGraphics()
                .fill(
                        x + 8,
                        y + 12,
                        x + 23,
                        y + 15,
                        0xFFE7E7E7
                );

        event.getGuiGraphics()
                .fill(
                        x + 11,
                        y + 5,
                        x + 20,
                        y + 13,
                        0xFFE7E7E7
                );

        event.getGuiGraphics()
                .drawString(
                        minecraft.font,
                        Component.translatable(
                                "accessory.top_hat.warning"
                        ),
                        x + 29,
                        y + 6,
                        0xFFFFFFFF,
                        true
                );
    }

    @EventBusSubscriber(
            modid = WayAround.MODID,
            value = Dist.CLIENT,
            bus = EventBusSubscriber.Bus.MOD
    )
    public static final class ModEvents {

        private ModEvents() {
        }

        @SubscribeEvent
        public static void keys(
                RegisterKeyMappingsEvent event
        ) {
            event.register(
                    ADJUST
            );
        }

        @SubscribeEvent
        public static void renderers(
                EntityRenderersEvent.RegisterRenderers event
        ) {
            event.registerEntityRenderer(
                    TopHatContent.FLYING_TOP_HAT.get(),
                    FlyingTopHatRenderer::new
            );

            event.registerEntityRenderer(
                    TopHatContent.SNOW_FOOTPRINT.get(),
                    SnowFootprintRenderer::new
            );
        }
    }
}
