package net.caravidro.wayaround.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.EnergyVisionS2CPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = WayAround.MODID, value = Dist.CLIENT)
public final class EnergyVisionClient {
    private static boolean active;
    private static final Map<UUID, Byte> SIGNATURES = new HashMap<>();
    private static final Map<UUID, Boolean> PREVIOUS_GLOW = new HashMap<>();

    private EnergyVisionClient() {}

    public static void receive(EnergyVisionS2CPayload payload) {
        restoreGlow();
        active = payload.active();
        SIGNATURES.clear();

        if (!active) return;
        for (EnergyVisionS2CPayload.Entry entry : payload.entries()) {
            SIGNATURES.put(entry.player(), entry.kind());
        }
        applyGlow();
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            restoreGlow();
            active = false;
            SIGNATURES.clear();
            return;
        }
        if (!active) return;

        applyGlow();

        if ((minecraft.level.getGameTime() & 1L) != 0L) return;
        for (Map.Entry<UUID, Byte> entry : SIGNATURES.entrySet()) {
            Player target = minecraft.level.getPlayerByUUID(entry.getKey());
            if (target == null) continue;

            byte kind = entry.getValue();
            int amount = kind == EnergyVisionS2CPayload.SPECTRUM ? 4 : 1;
            for (int i = 0; i < amount; i++) {
                minecraft.level.addParticle(
                        kind == EnergyVisionS2CPayload.SPECTRUM
                                ? ParticleTypes.ELECTRIC_SPARK
                                : ParticleTypes.ENCHANT,
                        target.getRandomX(0.65),
                        target.getRandomY(),
                        target.getRandomZ(0.65),
                        0.0,
                        kind == EnergyVisionS2CPayload.SPECTRUM ? 0.035 : 0.015,
                        0.0
                );
            }

            if (kind == EnergyVisionS2CPayload.SPECTRUM
                    && minecraft.level.getGameTime() % 5L == 0L) {
                minecraft.level.addParticle(
                        ParticleTypes.END_ROD,
                        target.getRandomX(0.4),
                        target.getY() + 1.0,
                        target.getRandomZ(0.4),
                        0.0, 0.02, 0.0
                );
            }
        }
    }

    @SubscribeEvent
    public static void renderDarkness(RenderGuiEvent.Pre event) {
        if (!active) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) return;

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        event.getGuiGraphics().fill(0, 0, width, height, 0xAA05070C);
    }

    private static void applyGlow() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        for (UUID id : SIGNATURES.keySet()) {
            Player target = minecraft.level.getPlayerByUUID(id);
            if (target == null) continue;
            PREVIOUS_GLOW.putIfAbsent(id, target.hasGlowingTag());
            target.setGlowingTag(true);
        }
    }

    private static void restoreGlow() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            for (Map.Entry<UUID, Boolean> entry : PREVIOUS_GLOW.entrySet()) {
                Player target = minecraft.level.getPlayerByUUID(entry.getKey());
                if (target != null) target.setGlowingTag(entry.getValue());
            }
        }
        PREVIOUS_GLOW.clear();
    }
}
