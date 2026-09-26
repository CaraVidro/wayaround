package net.caravidro.wayaround.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.SpectrumMeleeInputPayload;
import net.caravidro.wayaround.spectrum.SpectrumAccess;
import net.caravidro.wayaround.spectrum.SpectrumType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Void/Tukuna unarmed input.
 *
 * Left click is cancelled before vanilla combat and routed to the server's
 * melee raycast. F is intentionally shared with swap-offhand; while eligible
 * and blocking, the vanilla swap key is suppressed.
 */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class SpectrumMeleeClient {

    private SpectrumMeleeClient() {}

    public static final KeyMapping BLOCK =
            new KeyMapping(
                    "key.wayaround.spectrum_block",
                    KeyConflictContext.IN_GAME,
                    KeyModifier.NONE,
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_F,
                    "Way Around - Spectrums"
            );

    private static boolean localBlocking;

    @SubscribeEvent
    public static void register(
            RegisterKeyMappingsEvent event
    ) {
        event.register(
                BLOCK
        );
    }

    @SubscribeEvent
    public static void attack(
            InputEvent.InteractionKeyMappingTriggered event
    ) {
        if (!event.isAttack()) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (!eligible(
                minecraft
        )
                || localBlocking) {
            return;
        }

        PacketDistributor.sendToServer(
                new SpectrumMeleeInputPayload(
                        SpectrumMeleeInputPayload.ATTACK
                )
        );

        /*
         * Keep the normal local hand swing, but cancel vanilla hit handling so
         * damage, combo and block-catching happen exactly once on the server.
         */
        event.setSwingHand(
                true
        );
        event.setCanceled(
                true
        );
    }

    @SubscribeEvent
    public static void key(
            InputEvent.Key event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.screen != null
                || event.getKey()
                        != GLFW.GLFW_KEY_F) {
            return;
        }

        if (!eligible(
                minecraft
        )) {
            if (localBlocking) {
                stopBlock();
            }
            return;
        }

        suppressSwap(
                minecraft
        );

        if (event.getAction()
                == GLFW.GLFW_PRESS) {
            if (!localBlocking) {
                localBlocking =
                        true;

                PacketDistributor.sendToServer(
                        new SpectrumMeleeInputPayload(
                                SpectrumMeleeInputPayload.BLOCK_START
                        )
                );
            }
        } else if (event.getAction()
                == GLFW.GLFW_RELEASE) {
            stopBlock();
        }
    }

    @SubscribeEvent
    public static void tick(
            ClientTickEvent.Post event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (localBlocking) {
            suppressSwap(
                    minecraft
            );

            if (!eligible(
                    minecraft
            )
                    || minecraft.screen != null) {
                stopBlock();
            }
        }
    }

    private static boolean eligible(
            Minecraft minecraft
    ) {
        if (minecraft.player == null
                || minecraft.level == null
                || !minecraft.player
                        .getMainHandItem()
                        .isEmpty()
                || !minecraft.player
                        .getOffhandItem()
                        .isEmpty()) {
            return false;
        }

        return SpectrumAccess.has(
                minecraft.player,
                SpectrumType.VOID
        )
                || SpectrumAccess.has(
                minecraft.player,
                SpectrumType.TUKUNA
        );
    }

    private static void stopBlock() {
        if (!localBlocking) {
            return;
        }

        localBlocking =
                false;

        PacketDistributor.sendToServer(
                new SpectrumMeleeInputPayload(
                        SpectrumMeleeInputPayload.BLOCK_END
                )
        );
    }

    private static void suppressSwap(
            Minecraft minecraft
    ) {
        minecraft.options
                .keySwapOffhand
                .setDown(
                        false
                );

        while (minecraft.options
                .keySwapOffhand
                .consumeClick()) {
            // Drain the vanilla F action while Spectrum block owns the key.
        }
    }
}
