package net.caravidro.wayaround.spectrum;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.cursed.TukunaManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/** Keeps Spectrum duels survivable and enforces Tukuna's spoken non-aggression clause. */
@EventBusSubscriber(modid = WayAround.MODID)
public final class SpectrumCombat {
    private SpectrumCombat() {}

    public static boolean isBearer(Player player) {
        for (SpectrumType type : SpectrumType.values()) {
            if (SpectrumAccess.has(player, type)) return true;
        }
        return player instanceof ServerPlayer serverPlayer
                && TukunaManager.isPossessingSpirit(serverPlayer);
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && TukunaManager.isPacifistPossession(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && TukunaManager.isPacifistPossession(attacker)) {
            event.setAmount(0.0F);
            return;
        }
        if (event.getEntity() instanceof ServerPlayer victim
                && isBearer(victim)
                && !event.getSource().is(DamageTypes.GENERIC_KILL)) {
            // Armor still applies afterward; one ability cannot erase a Spectrum.
            event.setAmount(Math.min(event.getAmount() * 0.55F,
                    Math.max(2.0F, victim.getMaxHealth() * 0.38F)));
        }
    }
}
