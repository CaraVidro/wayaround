package net.caravidro.wayaround.ecology.client;

import net.caravidro.wayaround.ecology.DeepSeaCapsuleEntity;
import net.caravidro.wayaround.ecology.DeepSeaSubmarineEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Client-only abyss visibility budget used to avoid rendering what darkness hides. */
public final class DeepOceanClientVisibility {
    private DeepOceanClientVisibility() {}

    public static boolean isDeepOcean(ClientLevel level, BlockPos pos) {
        return net.caravidro.wayaround.ecology.DeepOceanBiomes.contains(level,pos);
    }

    public static boolean submerged(Entity entity, ClientLevel level, Vec3 eye) {
        if (entity == null) return false;
        boolean vehicle = entity.getVehicle() instanceof DeepSeaSubmarineEntity
                || entity.getVehicle() instanceof DeepSeaCapsuleEntity;
        return entity.isUnderWater() || (vehicle && eye.y < level.getSeaLevel()
                && level.getFluidState(BlockPos.containing(eye)).is(net.minecraft.tags.FluidTags.WATER));
    }

    public static boolean shouldRender(Entity target) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.level == null
                || !submerged(minecraft.player, minecraft.level, minecraft.gameRenderer.getMainCamera().getPosition())
                || !isDeepOcean(minecraft.level, minecraft.player.blockPosition())) {
            return true;
        }

        double depth = minecraft.level.getSeaLevel() - minecraft.player.getY();
        if (depth < 34.0) return true;

        Vec3 toTarget = target.position().subtract(minecraft.player.getEyePosition());
        double distance = toTarget.length();

        Entity vehicle =
                minecraft.player.getVehicle();

        if (vehicle instanceof DeepSeaCapsuleEntity
                || vehicle instanceof DeepSeaSubmarineEntity) {

            Vec3 look =
                    minecraft.player
                            .getLookAngle()
                            .normalize();

            double alignment =
                    distance < 0.001
                            ? 1.0
                            : look.dot(
                                    toTarget.scale(
                                            1.0 / distance
                                    )
                            );

            boolean submarine =
                    vehicle
                            instanceof DeepSeaSubmarineEntity;

            double range =
                    alignment > 0.70
                            ? (
                            submarine
                                    ? 108.0
                                    : 74.0
                    )
                            : (
                            submarine
                                    ? 18.0
                                    : 13.0
                    );

            return distance <= range;
        }

        double range = Math.max(7.0, 22.0 - Math.max(0.0, depth - 34.0) * 0.14);
        return distance <= range;
    }

    public static float vehicleLampStrength(
            Entity rider
    ) {
        if (rider == null) {
            return 0.0F;
        }

        Entity vehicle =
                rider.getVehicle();

        if (vehicle
                instanceof DeepSeaSubmarineEntity) {
            return 1.0F;
        }

        if (vehicle
                instanceof DeepSeaCapsuleEntity) {
            return 0.72F;
        }

        return 0.0F;
    }

    public static float fogEnd(
            double depth,
            Entity rider
    ) {
        double upward = Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector().y();
        if (depth > 38 && upward > .3) return (float)Math.max(8, Math.min(32,depth*.38));
        double t = Math.max(0.0, Math.min(1.0, (depth - 18.0) / 72.0));
        t = t * t * (3.0 - 2.0 * t);

        Entity vehicle =
                rider == null
                        ? null
                        : rider.getVehicle();

        if (vehicle
                instanceof DeepSeaSubmarineEntity) {
            return (float) (
                    104.0
                            - t * 12.0
            );
        }

        if (vehicle
                instanceof DeepSeaCapsuleEntity) {
            return (float) (
                    70.0
                            - t * 9.0
            );
        }

        // Free-diving in the true abyss should become almost sightless.
        return (float) (42.0 - t * 38.0);
    }
}
