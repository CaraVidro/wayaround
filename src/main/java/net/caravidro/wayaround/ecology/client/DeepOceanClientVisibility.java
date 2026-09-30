package net.caravidro.wayaround.ecology.client;

import net.caravidro.wayaround.ecology.DeepSeaCapsuleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Client-only abyss visibility budget used to avoid rendering what darkness hides. */
public final class DeepOceanClientVisibility {
    private DeepOceanClientVisibility() {}

    public static boolean isDeepOcean(ClientLevel level, BlockPos pos) {
        return level.getBiome(pos).unwrapKey().map(key -> {
            String path = key.location().getPath();
            return path.equals("deep_ocean")
                    || path.equals("deep_cold_ocean")
                    || path.equals("deep_frozen_ocean")
                    || path.equals("deep_lukewarm_ocean");
        }).orElse(false);
    }

    public static boolean shouldRender(Entity target) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.level == null
                || !minecraft.player.isUnderWater()
                || !isDeepOcean(minecraft.level, minecraft.player.blockPosition())) {
            return true;
        }

        double depth = minecraft.level.getSeaLevel() - minecraft.player.getY();
        if (depth < 34.0) return true;

        Vec3 toTarget = target.position().subtract(minecraft.player.getEyePosition());
        double distance = toTarget.length();

        if (minecraft.player.getVehicle() instanceof DeepSeaCapsuleEntity) {
            Vec3 look = minecraft.player.getLookAngle().normalize();
            double alignment = distance < 0.001 ? 1.0 : look.dot(toTarget.scale(1.0 / distance));
            double range = alignment > 0.72 ? 46.0 : 9.0;
            return distance <= range;
        }

        double range = Math.max(7.0, 22.0 - Math.max(0.0, depth - 34.0) * 0.14);
        return distance <= range;
    }

    public static float fogEnd(double depth, boolean capsule) {
        double t = Math.max(0.0, Math.min(1.0, (depth - 22.0) / 92.0));
        if (capsule) {
            return (float) (34.0 - t * 8.0);
        }
        return (float) (42.0 - t * 36.0);
    }
}
