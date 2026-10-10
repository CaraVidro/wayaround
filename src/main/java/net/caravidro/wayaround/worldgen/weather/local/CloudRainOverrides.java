package net.caravidro.wayaround.worldgen.weather.local;

import java.util.HashMap;
import java.util.Map;
import net.caravidro.wayaround.network.CloudRainS2CPayload;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Short-lived, server-authoritative cloud rain overrides by deterministic cell id.
 * The base procedural field is never mutated; turning either world feature off
 * immediately restores its previous behavior. Synced to late-joining clients.
 */
public final class CloudRainOverrides {
    private static final int MAX_ACTIVE = 64;
    private static final Map<Long, Long> UNTIL = new HashMap<>();

    private CloudRainOverrides() {}

    /** Called from the same-thread S2C handler or from server commands. */
    public static synchronized void receive(long id, long untilTick) {
        if (untilTick <= 0) UNTIL.remove(id);
        else {
            if (!UNTIL.containsKey(id) && UNTIL.size() >= MAX_ACTIVE) {
                UNTIL.remove(UNTIL.keySet().iterator().next());
            }
            UNTIL.put(id, untilTick);
        }
    }

    public static LocalWeatherField.CloudCell apply(Level level,
            LocalWeatherField.CloudCell cell, long gameTime) {
        if (level == null || !WorldFeatureRuntime.enabled(level, WorldFeature.PROCEDURAL_CLOUDS)
                || !WorldFeatureRuntime.enabled(level, WorldFeature.LIVING_WEATHER)) return cell;
        Long until;
        synchronized (CloudRainOverrides.class) {
            if (UNTIL.isEmpty()) return cell;
            until = UNTIL.get(cell.id());
        }
        if (until == null || until <= gameTime) return cell;
        if (cell.storm() >= 1.0F) return cell;
        return new LocalWeatherField.CloudCell(
                cell.id(), cell.x(), cell.z(), cell.y(), cell.radius(), 1.0F);
    }

    public static synchronized void prune(long now) {
        UNTIL.entrySet().removeIf(e -> e.getValue() <= now);
    }

    public static synchronized void clear() {
        UNTIL.clear();
    }

    /** Locate the first cloud volume intersected by the player's gaze. */
    public static LocalWeatherField.CloudCell findAimed(ServerPlayer player) {
        if (!player.serverLevel().dimension().equals(Level.OVERWORLD)
                || !WorldFeatureRuntime.serverEnabled(WorldFeature.PROCEDURAL_CLOUDS)
                || !WorldFeatureRuntime.serverEnabled(WorldFeature.LIVING_WEATHER)) return null;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        double closest = Double.POSITIVE_INFINITY;
        LocalWeatherField.CloudCell hit = null;
        for (var cloud : LocalWeatherField.nearbyCells(player.serverLevel(),
                eye.x, eye.z, player.serverLevel().getGameTime(), 800)) {
            // Bound the pick volume so giant regional fronts do not select
            // clouds the player is not actually facing.
            double radius = Math.max(65.0D, Math.min(180.0D, cloud.radius() * .80D));
            Vec3 delta = new Vec3(cloud.x(), cloud.y(), cloud.z()).subtract(eye);
            double along = delta.dot(look);
            if (along < -radius || along > 800.0D + radius) continue;
            double sidewaysSquared = Math.max(0.0D, delta.lengthSqr() - along * along);
            if (sidewaysSquared > radius * radius) continue;
            double entry = Math.max(0.0D, along - Math.sqrt(radius * radius - sidewaysSquared));
            if (entry >= closest) continue;
            closest = entry;
            hit = cloud;
        }
        return hit;
    }

    public static LocalWeatherField.CloudCell setAimed(ServerPlayer player, boolean rainy) {
        LocalWeatherField.CloudCell target = findAimed(player);
        if (target == null) return null;
        long until = rainy ? player.serverLevel().getGameTime() + 6000L : 0L;
        receive(target.id(), until);
        var payload = new CloudRainS2CPayload(target.id(), until);
        for (ServerPlayer viewer : player.serverLevel().players()) {
            PacketDistributor.sendToPlayer(viewer, payload);
        }
        return target;
    }

    public static void sync(ServerPlayer player) {
        if (!player.serverLevel().dimension().equals(Level.OVERWORLD)) return;
        long now = player.serverLevel().getGameTime();
        Map<Long, Long> copy;
        synchronized (CloudRainOverrides.class) {
            copy = Map.copyOf(UNTIL);
        }
        for (var row : copy.entrySet()) {
            if (row.getValue() > now) PacketDistributor.sendToPlayer(player,
                    new CloudRainS2CPayload(row.getKey(), row.getValue()));
        }
    }
}
