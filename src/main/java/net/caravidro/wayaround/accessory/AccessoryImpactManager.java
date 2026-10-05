package net.caravidro.wayaround.accessory;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.caravidro.wayaround.worldgen.weather.local.LocalWeatherField;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Event driven, server-authoritative impacts; walls protect lenses and hats. */
@EventBusSubscriber(modid = WayAround.MODID)
public final class AccessoryImpactManager {
    private AccessoryImpactManager() {}

    @SubscribeEvent public static void explosion(ExplosionEvent.Detonate event) {
        if (!WorldFeatureRuntime.serverEnabled(WorldFeature.ACCESSORIES)) return;
        Vec3 origin = event.getExplosion().center();
        for (var entity : event.getAffectedEntities())
            if (entity instanceof ServerPlayer player) blast(player, origin);
    }

    /** Two short rays only for affected players, not a scan over world blocks/entities. */
    public static double exposure(ServerPlayer player, Vec3 origin) {
        double distance = origin.distanceTo(player.getEyePosition());
        if (distance > 10) return 0;
        int visible = 0;
        for (double y : new double[] {-.12, -.46}) {
            var hit = player.level().clip(new ClipContext(origin, player.getEyePosition().add(0, y, 0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.MISS) visible++;
        }
        return (1 - distance / 10) * visible / 2.0;
    }

    public static void blast(ServerPlayer player, Vec3 origin) {
        double exposure = exposure(player, origin);
        if (exposure < .12) return;
        if (AccessoryManager.damageBreakableGlass(player, exposure >= .4 ? 2 : 1))
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK,
                    SoundSource.PLAYERS, .7F, 1.2F);
        Vec3 outward = player.getEyePosition().subtract(origin);
        if (outward.lengthSqr() < .001) outward = new Vec3(1, 0, 0);
        flyChefHat(player, outward.normalize().scale(.35 + exposure * .55).add(0, .38, 0));
    }

    public static boolean flyChefHat(ServerPlayer player, Vec3 impulse) {
        if (AccessoryManager.equipped(player, AccessorySlot.HEAD) != AccessoryKind.CHEF_HAT) return false;
        var hat = TopHatContent.FLYING_TOP_HAT.get().create(player.serverLevel());
        if (hat == null) return false;
        var stack = AccessoryManager.takeEquipped(player, AccessorySlot.HEAD);
        if (stack.isEmpty()) return false;
        hat.setHatStack(stack);
        hat.moveTo(player.getX(), player.getEyeY() + .18, player.getZ(), player.getYRot(), 0);
        hat.setDeltaMovement(impulse.add(player.getDeltaMovement().scale(.4)));
        if (!player.serverLevel().addFreshEntity(hat)) {
            if (!player.getInventory().add(stack)) player.drop(stack, false);
            return false;
        }
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.WOOL_STEP,
                SoundSource.PLAYERS, .55F, .8F);
        return true;
    }

    @SubscribeEvent public static void struck(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getNewDamage() < 5
                || event.getSource().is(DamageTypeTags.IS_EXPLOSION)
                || !WorldFeatureRuntime.serverEnabled(WorldFeature.ACCESSORIES)) return;
        Vec3 direction = event.getSource().getSourcePosition();
        direction = direction == null ? player.getLookAngle().scale(-1)
                : player.position().subtract(direction).normalize();
        flyChefHat(player, direction.scale(.4).add(0, .4, 0));
    }

    @SubscribeEvent public static void wind(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 40 != 0
                || !WorldFeatureRuntime.serverEnabled(WorldFeature.ACCESSORIES)) return;
        for (var player : event.getServer().getPlayerList().getPlayers()) {
            if (AccessoryManager.equipped(player, AccessorySlot.HEAD) != AccessoryKind.CHEF_HAT
                    || !player.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)
                    || !player.level().canSeeSky(player.blockPosition())) continue;
            var wind = LocalWeatherField.sample(player.getX(), player.getZ(), player.level().getGameTime());
            if (wind.warning() > .8F && player.getRandom().nextFloat() < .06F)
                flyChefHat(player, new Vec3(wind.windX() * .5, .4, wind.windZ() * .5));
        }
    }
}
