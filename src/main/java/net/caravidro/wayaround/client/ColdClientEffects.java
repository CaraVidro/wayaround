package net.caravidro.wayaround.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.effect.WayAroundEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = WayAround.MODID, value = Dist.CLIENT)
public final class ColdClientEffects {
    private static Player lastPlayer;
    private static float strength;

    private ColdClientEffects() {}

    public static float strength(LivingEntity entity) {
        if (!entity.isAlive() || (entity instanceof Player player && (player.isCreative() || player.isSpectator()))) return 0;
        MobEffectInstance effect = entity.getEffect(WayAroundEffects.SHIVERING);
        if (effect == null) return 0;
        return Math.min(1, (effect.getAmplifier() + 1) / 4.0F) * Math.min(1, effect.getDuration() / 100.0F);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (lastPlayer != mc.player) { lastPlayer = mc.player; strength = 0; }
        if (mc.player == null) { strength = 0; return; }
        if (!mc.isPaused()) strength += (strength(mc.player) - strength) * 0.08F;
    }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.isPaused() || mc.getCameraEntity() != mc.player || strength < 0.001F) return;
        double time = mc.player.tickCount + event.getPartialTick();
        // Add to the camera only; do not alter aim packets or the player's actual rotation.
        event.setYaw(event.getYaw() + (float) Math.sin(time * 2.7) * strength * 0.22F);
        event.setPitch(event.getPitch() + (float) Math.sin(time * 3.9 + 1.2) * strength * 0.16F);
        event.setRoll(event.getRoll() + (float) Math.sin(time * 2.1) * strength * 0.12F);
    }
}
