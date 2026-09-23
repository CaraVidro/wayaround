package net.caravidro.wayaround.client.water;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldgen.weather.local.LocalWeatherField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Animated surface layer above vanilla water.
 *
 * The search radius follows half of the configured chunk render distance.
 * Surface block discovery is cached, while wave height is still evaluated
 * every frame, keeping distant water animated without rescanning thousands
 * of blocks every frame.
 */
@EventBusSubscriber(modid = WayAround.MODID, value = Dist.CLIENT)
public final class WaterSurfaceRenderer {

    private static final List<BlockPos> SURFACES =
            new ArrayList<>();

    private static int cachedCenterX =
            Integer.MIN_VALUE;

    private static int cachedCenterZ =
            Integer.MIN_VALUE;

    private static int cachedRadius =
            -1;

    private static long cachedAt =
            Long.MIN_VALUE;

    private WaterSurfaceRenderer() {
    }

    @SubscribeEvent
    public static void render(
            RenderLevelStageEvent event
    ) {
        if (event.getStage()
                != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || minecraft.player == null
                || !minecraft.level.dimension().equals(Level.OVERWORLD)) {
            clearCache();
            return;
        }

        int radius =
                Math.max(
                        24,
                        Math.min(
                                128,
                                minecraft.options.renderDistance().get()
                                * 8
                        )
                );

        int centerX =
                minecraft.player.getBlockX();

        int centerZ =
                minecraft.player.getBlockZ();

        long time =
                minecraft.level.getGameTime();

        if (needsRebuild(
                centerX,
                centerZ,
                radius,
                time
        )) {
            rebuildSurfaces(
                    minecraft,
                    centerX,
                    centerZ,
                    radius,
                    time
            );
        }

        Vec3 camera =
                event.getCamera().getPosition();

        LocalWeatherField.Sample weather =
                LocalWeatherField.sample(
                        camera.x,
                        camera.z,
                        time
                );

        double amplitude =
                0.018
                + weather.warning()
                * 0.055;

        PoseStack stack =
                event.getPoseStack();

        stack.pushPose();
        stack.translate(
                -camera.x,
                -camera.y,
                -camera.z
        );

        BufferBuilder buffer =
                Tesselator.getInstance().begin(
                        VertexFormat.Mode.QUADS,
                        DefaultVertexFormat.POSITION_COLOR
                );

        boolean any =
                false;

        double radiusSquared =
                radius * (double) radius;

        for (BlockPos surface :
                SURFACES) {

            double dx =
                    surface.getX()
                    + 0.5
                    - camera.x;

            double dz =
                    surface.getZ()
                    + 0.5
                    - camera.z;

            double distanceSquared =
                    dx * dx
                    + dz * dz;

            if (distanceSquared > radiusSquared) {
                continue;
            }

            FluidState fluid =
                    minecraft.level.getFluidState(
                            surface
                    );

            if (!fluid.is(FluidTags.WATER)) {
                continue;
            }

            int x =
                    surface.getX();

            int z =
                    surface.getZ();

            double base =
                    surface.getY()
                    + fluid.getHeight(
                            minecraft.level,
                            surface
                    )
                    + 0.006;

            double y00 =
                    base
                    + wave(
                            x,
                            z,
                            time,
                            amplitude
                    );

            double y10 =
                    base
                    + wave(
                            x + 1,
                            z,
                            time,
                            amplitude
                    );

            double y11 =
                    base
                    + wave(
                            x + 1,
                            z + 1,
                            time,
                            amplitude
                    );

            double y01 =
                    base
                    + wave(
                            x,
                            z + 1,
                            time,
                            amplitude
                    );

            int waterColor =
                    minecraft.level.getBiome(
                            surface
                    ).value()
                            .getWaterColor();

            int red =
                    waterColor >> 16
                    & 255;

            int green =
                    waterColor >> 8
                    & 255;

            int blue =
                    waterColor
                    & 255;

            double distance =
                    Math.sqrt(
                            distanceSquared
                    );

            float edgeFade =
                    (float) Math.max(
                            0.0,
                            Math.min(
                                    1.0,
                                    (
                                            radius
                                            - distance
                                    )
                                    / 18.0
                            )
                    );

            int alpha =
                    Math.max(
                            0,
                            Math.min(
                                    46,
                                    Math.round(
                                            42.0F
                                            * edgeFade
                                    )
                            )
                    );

            if (alpha <= 0) {
                continue;
            }

            var matrix =
                    stack.last().pose();

            buffer.addVertex(
                            matrix,
                            x,
                            (float) y00,
                            z
                    )
                    .setColor(
                            red,
                            green,
                            blue,
                            alpha
                    );

            buffer.addVertex(
                            matrix,
                            x + 1,
                            (float) y10,
                            z
                    )
                    .setColor(
                            red,
                            green,
                            blue,
                            alpha
                    );

            buffer.addVertex(
                            matrix,
                            x + 1,
                            (float) y11,
                            z + 1
                    )
                    .setColor(
                            red,
                            green,
                            blue,
                            alpha
                    );

            buffer.addVertex(
                            matrix,
                            x,
                            (float) y01,
                            z + 1
                    )
                    .setColor(
                            red,
                            green,
                            blue,
                            alpha
                    );

            any =
                    true;
        }

        if (any) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.setShader(
                    GameRenderer::getPositionColorShader
            );

            BufferUploader.drawWithShader(
                    buffer.buildOrThrow()
            );

            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }

        stack.popPose();
    }

    private static boolean needsRebuild(
            int centerX,
            int centerZ,
            int radius,
            long time
    ) {
        if (cachedRadius != radius
                || cachedCenterX == Integer.MIN_VALUE) {
            return true;
        }

        int dx =
                centerX
                - cachedCenterX;

        int dz =
                centerZ
                - cachedCenterZ;

        return dx * dx
                + dz * dz
                >= 64
                || time - cachedAt
                >= 40L;
    }

    private static void rebuildSurfaces(
            Minecraft minecraft,
            int centerX,
            int centerZ,
            int radius,
            long time
    ) {
        SURFACES.clear();

        int radiusSquared =
                radius * radius;

        BlockPos.MutableBlockPos mutable =
                new BlockPos.MutableBlockPos();

        for (int x = centerX - radius;
                x <= centerX + radius;
                x++) {

            int dx =
                    x - centerX;

            for (int z = centerZ - radius;
                    z <= centerZ + radius;
                    z++) {

                int dz =
                        z - centerZ;

                if (dx * dx
                        + dz * dz
                        > radiusSquared) {
                    continue;
                }

                int surfaceY =
                        minecraft.level.getHeight(
                                Heightmap.Types.WORLD_SURFACE,
                                x,
                                z
                        )
                        - 1;

                BlockPos water =
                        findSurfaceWater(
                                minecraft.level,
                                mutable,
                                x,
                                z,
                                surfaceY
                        );

                if (water != null) {
                    SURFACES.add(
                            water
                    );
                }
            }
        }

        cachedCenterX =
                centerX;

        cachedCenterZ =
                centerZ;

        cachedRadius =
                radius;

        cachedAt =
                time;
    }

    private static BlockPos findSurfaceWater(
            Level level,
            BlockPos.MutableBlockPos mutable,
            int x,
            int z,
            int surfaceY
    ) {
        for (int y = surfaceY;
                y >= surfaceY - 7;
                y--) {

            mutable.set(
                    x,
                    y,
                    z
            );

            if (!level.getFluidState(mutable)
                    .is(FluidTags.WATER)) {
                continue;
            }

            BlockPos above =
                    mutable.above();

            if (!level.getFluidState(above)
                    .is(FluidTags.WATER)) {
                return mutable.immutable();
            }

            return null;
        }

        return null;
    }

    private static double wave(
            int x,
            int z,
            long time,
            double amplitude
    ) {
        double t =
                time * 0.10;

        return Math.sin(
                        x * 0.38
                        + z * 0.21
                        + t
                ) * amplitude
                + Math.sin(
                        x * 0.13
                        - z * 0.31
                        + t * 0.63
                ) * amplitude * 0.45;
    }

    private static void clearCache() {
        SURFACES.clear();
        cachedCenterX = Integer.MIN_VALUE;
        cachedCenterZ = Integer.MIN_VALUE;
        cachedRadius = -1;
        cachedAt = Long.MIN_VALUE;
    }
}
