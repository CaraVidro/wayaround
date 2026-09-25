package net.caravidro.wayaround.client.weather;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.client.AntarcticClientLighting;
import net.caravidro.wayaround.worldgen.weather.local.LocalWeatherField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

/**
 * Cheap projected cloud shadows.
 *
 * This is deliberately not a shadow map. Each local weather cell projects a
 * low-resolution translucent footprint onto the terrain below it. The cloud
 * shape still moves smoothly because LocalWeatherField itself moves smoothly.
 */
@EventBusSubscriber(modid = WayAround.MODID, value = Dist.CLIENT)
public final class CloudShadowRenderer {

    private static final double RANGE = 420.0;
    private static final int STEP = 12;
    private static final double CAMERA_NEAR_GUARD = 0.35;

    private CloudShadowRenderer() {
    }

    /**
     * Disabled temporarily.
     *
     * The old projected-shadow mesh used large terrain-following translucent
     * quads. Around the horizon those quads could clip against the camera /
     * projection and appear as a blue-dark serrated sheet that moved with the
     * player. Looking upward hid it only because the previous implementation
     * skipped rendering at high positive camera pitch, confirming this renderer
     * as the source.
     *
     * Keep the class registered so the feature can be rewritten later with a
     * decal/depth-safe approach, but emit no geometry for now.
     */
    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        return;
    }

    private static boolean quadSafelyInFront(
            Vec3 camera,
            double lookX,
            double lookY,
            double lookZ,
            double x0,
            double y0,
            double z0,
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2,
            double x3,
            double y3,
            double z3
    ) {
        return pointSafelyInFront(
                camera,
                lookX,
                lookY,
                lookZ,
                x0,
                y0,
                z0
        )
                && pointSafelyInFront(
                camera,
                lookX,
                lookY,
                lookZ,
                x1,
                y1,
                z1
        )
                && pointSafelyInFront(
                camera,
                lookX,
                lookY,
                lookZ,
                x2,
                y2,
                z2
        )
                && pointSafelyInFront(
                camera,
                lookX,
                lookY,
                lookZ,
                x3,
                y3,
                z3
        );
    }

    private static boolean pointSafelyInFront(
            Vec3 camera,
            double lookX,
            double lookY,
            double lookZ,
            double x,
            double y,
            double z
    ) {
        double forward =
                (
                        x - camera.x
                ) * lookX
                        + (
                        y - camera.y
                ) * lookY
                        + (
                        z - camera.z
                ) * lookZ;

        return forward > CAMERA_NEAR_GUARD;
    }

    private static int floorToStep(
            double value,
            int step
    ) {
        return (int) Math.floor(value / step) * step;
    }

    private static double distanceSquared(
            double ax,
            double az,
            double bx,
            double bz
    ) {
        double dx = ax - bx;
        double dz = az - bz;
        return dx * dx + dz * dz;
    }
}
