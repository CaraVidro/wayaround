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

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null
                || minecraft.player == null
                || !minecraft.level.dimension().equals(Level.OVERWORLD)
                || AntarcticClientLighting.isAntarctic(minecraft)) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();

        Vector3f lookVector =
                event.getCamera().getLookVector();

        double lookX =
                lookVector.x();

        double lookY =
                lookVector.y();

        double lookZ =
                lookVector.z();

        /*
         * Terrain shadows have no visual meaning while staring almost
         * straight into the sky. More importantly, large terrain quads near
         * the player can straddle the camera plane in this orientation and
         * clip into giant translucent polygons at the screen edge.
         */
        if (lookY > 0.52) {
            return;
        }

        long time = minecraft.level.getGameTime();

        Vec3 skyColor =
                minecraft.level.getSkyColor(
                        camera,
                        1.0F
                );

        float skyLuminance =
                (float) (
                        skyColor.x * 0.2126
                        + skyColor.y * 0.7152
                        + skyColor.z * 0.0722
                );

        float sunlight =
                net.minecraft.util.Mth.clamp(
                        (skyLuminance - 0.08F)
                        / 0.72F,
                        0.0F,
                        1.0F
                );

        if (sunlight <= 0.02F) {
            return;
        }

        PoseStack stack = event.getPoseStack();
        stack.pushPose();
        stack.translate(-camera.x, -camera.y, -camera.z);

        BufferBuilder buffer =
                Tesselator.getInstance().begin(
                        VertexFormat.Mode.QUADS,
                        DefaultVertexFormat.POSITION_COLOR
                );

        boolean any = false;

        for (LocalWeatherField.CloudCell cell :
                LocalWeatherField.nearbyCells(
                        camera.x,
                        camera.z,
                        time,
                        RANGE
                )) {

            int minX =
                    floorToStep(
                            cell.x() - cell.radius(),
                            STEP
                    );

            int maxX =
                    floorToStep(
                            cell.x() + cell.radius(),
                            STEP
                    );

            int minZ =
                    floorToStep(
                            cell.z() - cell.radius(),
                            STEP
                    );

            int maxZ =
                    floorToStep(
                            cell.z() + cell.radius(),
                            STEP
                    );

            for (int x = minX; x <= maxX; x += STEP) {
                for (int z = minZ; z <= maxZ; z += STEP) {
                    double centerX = x + STEP * 0.5;
                    double centerZ = z + STEP * 0.5;

                    if (distanceSquared(
                            centerX,
                            centerZ,
                            camera.x,
                            camera.z
                    ) > RANGE * RANGE) {
                        continue;
                    }

                    float density =
                            cell.densityAt(
                                    centerX,
                                    centerZ
                            );

                    if (density < 0.08F) {
                        continue;
                    }

                    int y00 =
                            minecraft.level.getHeight(
                                    Heightmap.Types.WORLD_SURFACE,
                                    x,
                                    z
                            );

                    int y10 =
                            minecraft.level.getHeight(
                                    Heightmap.Types.WORLD_SURFACE,
                                    x + STEP,
                                    z
                            );

                    int y11 =
                            minecraft.level.getHeight(
                                    Heightmap.Types.WORLD_SURFACE,
                                    x + STEP,
                                    z + STEP
                            );

                    int y01 =
                            minecraft.level.getHeight(
                                    Heightmap.Types.WORLD_SURFACE,
                                    x,
                                    z + STEP
                            );

                    if (!quadSafelyInFront(
                            camera,
                            lookX,
                            lookY,
                            lookZ,
                            x,
                            y00 + 0.035,
                            z,
                            x + STEP,
                            y10 + 0.035,
                            z,
                            x + STEP,
                            y11 + 0.035,
                            z + STEP,
                            x,
                            y01 + 0.035,
                            z + STEP
                    )) {
                        continue;
                    }

                    int alpha =
                            Math.min(
                                    86,
                                    Math.round(
                                            density
                                            * (
                                                    34.0F
                                                    + cell.storm()
                                                    * 42.0F
                                            )
                                            * sunlight
                                    )
                            );

                    var matrix = stack.last().pose();

                    buffer.addVertex(matrix, x, y00 + 0.035F, z)
                            .setColor(18, 22, 25, alpha);

                    buffer.addVertex(matrix, x + STEP, y10 + 0.035F, z)
                            .setColor(18, 22, 25, alpha);

                    buffer.addVertex(matrix, x + STEP, y11 + 0.035F, z + STEP)
                            .setColor(18, 22, 25, alpha);

                    buffer.addVertex(matrix, x, y01 + 0.035F, z + STEP)
                            .setColor(18, 22, 25, alpha);

                    any = true;
                }
            }
        }

        if (any) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.setShaderColor(
                    1.0F,
                    1.0F,
                    1.0F,
                    1.0F
            );
            RenderSystem.setShader(GameRenderer::getPositionColorShader);

            BufferUploader.drawWithShader(
                    buffer.buildOrThrow()
            );

            RenderSystem.setShaderColor(
                    1.0F,
                    1.0F,
                    1.0F,
                    1.0F
            );
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }

        stack.popPose();
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
