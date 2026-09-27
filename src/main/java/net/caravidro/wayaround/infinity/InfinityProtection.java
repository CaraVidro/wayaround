package net.caravidro.wayaround.infinity;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.cursed.ImmortalWheelManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = WayAround.MODID)
public final class InfinityProtection {
    private InfinityProtection() {}
    private record Swing(java.util.UUID target, long tick) {}
    private static final java.util.Map<java.util.UUID, Swing> SWINGS = new java.util.HashMap<>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void attack(net.neoforged.neoforge.event.entity.player.AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer attacker && InfinityManager.protects(event.getTarget()))
            SWINGS.put(attacker.getUUID(), new Swing(event.getTarget().getUUID(), attacker.server.getTickCount()));
    }
    private static boolean consumeSwing(ServerPlayer attacker, net.minecraft.world.entity.Entity target) {
        Swing swing = SWINGS.remove(attacker.getUUID());
        return swing != null && swing.tick == attacker.server.getTickCount() && swing.target.equals(target.getUUID());
    }
    @SubscribeEvent public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        SWINGS.clear();
    }
    @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        SWINGS.remove(event.getEntity().getUUID());
    }


    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damage(LivingIncomingDamageEvent event) {
        if (!InfinityManager.protects(event.getEntity())) return;
        var source = event.getSource();
        float multiplier = 0;
        // Exact melee type + direct attacker excludes bullets, explosions and remote abilities.
        if (event.getAmount() > 0 && source.is(DamageTypes.PLAYER_ATTACK)
                && source.getEntity() instanceof ServerPlayer attacker
                && source.getDirectEntity() == attacker
                && attacker.distanceToSqr(event.getEntity()) <= 36.0
                && consumeSwing(attacker, event.getEntity())) {
            multiplier = ImmortalWheelManager.learnInfinityMelee(attacker);
        }
        if (multiplier == 0) event.setCanceled(true);
        else event.setAmount(event.getAmount() * multiplier);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void knockback(LivingKnockBackEvent event) {
        if (InfinityManager.protects(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void explosion(net.neoforged.neoforge.event.level.ExplosionKnockbackEvent event) {
        if (InfinityManager.protects(event.getAffectedEntity()))
            event.setKnockbackVelocity(net.minecraft.world.phys.Vec3.ZERO);
    }

    @SubscribeEvent
    public static void projectile(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof Projectile projectile
                && projectile.level() instanceof ServerLevel level
                && InfinityManager.advanceProjectile(level, projectile)) event.setCanceled(true);
    }
}
