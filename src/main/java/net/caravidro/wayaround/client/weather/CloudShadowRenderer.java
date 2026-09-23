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
        long time = minecraft.level.getGameTime();

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
            RenderSystem.setShader(GameRenderer::getPositionColorShader);

            BufferUploader.drawWithShader(
                    buffer.buildOrThrow()
            );

            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }

        stack.popPose();
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
