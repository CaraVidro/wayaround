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
import net.caravidro.wayaround.industrial.ship.NauticalSeaState;
import net.caravidro.wayaround.worldgen.weather.local.LocalWeatherField;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
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

    private static final List<WaterSurface> SURFACES =
            new ArrayList<>();

    private static int cachedCenterX =
            Integer.MIN_VALUE;

    private static int cachedCenterZ =
            Integer.MIN_VALUE;

    private static int cachedRadius =
            -1;

    private static long cachedAt =
            Long.MIN_VALUE;

    /**
     * Custom translucent geometry must never straddle the camera near plane.
     * If it does, the perspective clip can turn one water quad into a huge
     * screen-space triangle while the player looks away from the surface.
     */
    private static final double CAMERA_NEAR_GUARD =
            0.20;

    /*
     * Far translucent water is expensive on weak/integrated GPUs and sits on
     * top of vanilla water anyway. Keep full one-block detail close to the
     * camera, then progressively sample the cosmetic overlay.
     */
    private static final double LOD_MID_DISTANCE_SQR =
            48.0 * 48.0;

    private static final double LOD_FAR_DISTANCE_SQR =
            72.0 * 72.0;

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

        if (!WorldFeatureRuntime.clientEnabled(
                WorldFeature.WATER_DYNAMICS
        )) {
            clearCache();
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

        /*
         * This is a cosmetic overlay above vanilla water, not the water itself.
         * Keeping it inside roughly six chunks avoids scanning tens of
         * thousands of columns merely to animate sub-pixel waves on the
         * horizon.
         */
        int radius =
                Math.max(
                        24,
                        Math.min(
                                96,
                                minecraft.options.renderDistance().get()
                                * 6
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

        Vector3f cameraLook =
                event.getCamera().getLookVector();

        double lookX =
                cameraLook.x();

        double lookY =
                cameraLook.y();

        double lookZ =
                cameraLook.z();

        LocalWeatherField.Sample weather =
                LocalWeatherField.sample(
                        camera.x,
                        camera.z,
                        time
                );

        NauticalSeaState.Sample sea =
                NauticalSeaState.sample(
                        minecraft.level,
                        BlockPos.containing(
                                camera
                        ),
                        time
                );

        /*
         * Protected/coastal water keeps short chop. Deep ocean receives a
         * deliberately exaggerated long swell so the Great Voyages ships read
         * as being on a real sea rather than a moving blue floor.
         */
        double amplitude =
                (
                        0.030
                                + sea.exposure()
                                * 0.165
                                + weather.warning()
                                * 0.050
                                + sea.storm()
                                * 0.060
                )
                        * (
                        0.74
                                + sea.swell()
                                * 0.42
                );

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

        double fadeStart =
                Math.max(
                        0.0,
                        radius - 18.0
                );

        double fadeStartSquared =
                fadeStart * fadeStart;

        for (WaterSurface surface :
                SURFACES) {

            double dx =
                    surface.x
                    + 0.5
                    - camera.x;

            double dz =
                    surface.z
                    + 0.5
                    - camera.z;

            double distanceSquared =
                    dx * dx
                    + dz * dz;

            if (distanceSquared > radiusSquared) {
                continue;
            }

            int x =
                    surface.x;

            int z =
                    surface.z;

            /*
             * Cosmetic LOD only. Vanilla water remains fully rendered below,
             * so skipping distant overlay tiles does not create missing water.
             * This removes up to ~75% of far translucent quads at high render
             * distances, where individual wave tiles are too small to notice.
             */
            if (distanceSquared > LOD_FAR_DISTANCE_SQR) {
                if ((x & 1) != 0
                        || (z & 1) != 0) {
                    continue;
                }
            } else if (distanceSquared > LOD_MID_DISTANCE_SQR) {
                if (((x + z) & 1) != 0) {
                    continue;
                }
            }

            double base =
                    surface.baseY;

            double y00 =
                    base
                    + wave(
                            x,
                            z,
                            time,
                            amplitude,
                            sea.exposure()
                    );

            double y10 =
                    base
                    + wave(
                            x + 1,
                            z,
                            time,
                            amplitude,
                            sea.exposure()
                    );

            double y11 =
                    base
                    + wave(
                            x + 1,
                            z + 1,
                            time,
                            amplitude,
                            sea.exposure()
                    );

            double y01 =
                    base
                    + wave(
                            x,
                            z + 1,
                            time,
                            amplitude,
                            sea.exposure()
                    );

            /*
             * Do not submit quads that cross or sit behind the camera plane.
             *
             * Previously every discovered water tile was sent to the GPU,
             * even when the player looked straight at the sky. A tile close
             * to the camera could then straddle the perspective near plane
             * and clip into a giant dark/translucent triangle in a screen
             * corner while moving.
             *
             * Requiring all four corners to stay a tiny distance in front of
             * the camera is deliberately conservative. Tiles outside the
             * visible half-space are useless anyway, and this also removes
             * unstable near-plane geometry before rasterization.
             */
            if (!quadSafelyInFront(
                    camera,
                    lookX,
                    lookY,
                    lookZ,
                    x,
                    z,
                    y00,
                    y10,
                    y11,
                    y01
            )) {
                continue;
            }

            double averageWave =
                    (
                            y00
                                    + y10
                                    + y11
                                    + y01
                    ) * 0.25
                            - base;

            double textureWave =
                    Math.sin(
                            x * 1.37
                                    + z * 0.73
                                    + time * 0.031
                    )
                            * 3.8
                            + Math.sin(
                            x * 0.41
                                    - z * 1.61
                                    - time * 0.019
                    )
                                    * 2.4;

            int textureNoise =
                    Mth.clamp(
                            (int) Math.round(
                                    textureWave
                            ),
                            -6,
                            6
                    );

            int crest =
                    Mth.clamp(
                            (int) Math.round(
                                    averageWave
                                            / Math.max(
                                            0.001,
                                            amplitude
                                    )
                                            * 8.0
                            ),
                            -8,
                            8
                    );

            int red =
                    Mth.clamp(
                            surface.red
                                    + textureNoise / 3
                                    + crest / 3,
                            0,
                            255
                    );

            int green =
                    Mth.clamp(
                            surface.green
                                    + textureNoise / 2
                                    + crest / 2,
                            0,
                            255
                    );

            int blue =
                    Mth.clamp(
                            surface.blue
                                    + textureNoise
                                    + crest,
                            0,
                            255
                    );

            float edgeFade =
                    1.0F;

            if (distanceSquared > fadeStartSquared) {
                double distance =
                        Math.sqrt(
                                distanceSquared
                        );

                edgeFade =
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
            }

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
            RenderSystem.setShaderColor(
                    1.0F,
                    1.0F,
                    1.0F,
                    1.0F
            );
            RenderSystem.setShader(
                    GameRenderer::getPositionColorShader
            );

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
            int x,
            int z,
            double y00,
            double y10,
            double y11,
            double y01
    ) {
        return forwardDistance(
                camera,
                lookX,
                lookY,
                lookZ,
                x,
                y00,
                z
        ) > CAMERA_NEAR_GUARD
                && forwardDistance(
                camera,
                lookX,
                lookY,
                lookZ,
                x + 1,
                y10,
                z
        ) > CAMERA_NEAR_GUARD
                && forwardDistance(
                camera,
                lookX,
                lookY,
                lookZ,
                x + 1,
                y11,
                z + 1
        ) > CAMERA_NEAR_GUARD
                && forwardDistance(
                camera,
                lookX,
                lookY,
                lookZ,
                x,
                y01,
                z + 1
        ) > CAMERA_NEAR_GUARD;
    }

    private static double forwardDistance(
            Vec3 camera,
            double lookX,
            double lookY,
            double lookZ,
            double x,
            double y,
            double z
    ) {
        return (
                x - camera.x
        ) * lookX
                + (
                y - camera.y
        ) * lookY
                + (
                z - camera.z
        ) * lookZ;
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
                >= 144
                || time - cachedAt
                >= 100L;
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

                int distanceSquared =
                        dx * dx
                                + dz * dz;

                if (distanceSquared
                        > radiusSquared) {
                    continue;
                }

                /*
                 * Apply the same cosmetic LOD before any height/fluid/biome
                 * lookup. Distant skipped tiles therefore cost essentially a
                 * pair of integer operations instead of several world queries,
                 * and they never enter the per-frame surface list.
                 */
                if (distanceSquared
                        > LOD_FAR_DISTANCE_SQR) {
                    if ((x & 1) != 0
                            || (z & 1) != 0) {
                        continue;
                    }
                } else if (distanceSquared
                        > LOD_MID_DISTANCE_SQR
                        && ((x + z) & 1) != 0) {
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
                    FluidState fluid =
                            minecraft.level.getFluidState(
                                    water
                            );

                    if (!fluid.is(
                            FluidTags.WATER
                    )) {
                        continue;
                    }

                    int waterColor =
                            minecraft.level.getBiome(
                                    water
                            ).value()
                                    .getWaterColor();

                    SURFACES.add(
                            new WaterSurface(
                                    water.getX(),
                                    water.getZ(),
                                    water.getY()
                                            + fluid.getHeight(
                                                    minecraft.level,
                                                    water
                                            )
                                            + 0.006,
                                    waterColor >> 16
                                            & 255,
                                    waterColor >> 8
                                            & 255,
                                    waterColor
                                            & 255
                            )
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
            double amplitude,
            float exposure
    ) {
        double t =
                time * 0.10;

        double shortChop =
                Mth.sin(
                        (float) (
                                x * 0.38
                                        + z * 0.21
                                        + t
                        )
                )
                        * amplitude
                        * (
                        1.0
                                - exposure
                                * 0.48
                );

        double crossChop =
                Mth.sin(
                        (float) (
                                x * 0.13
                                        - z * 0.31
                                        + t * 0.63
                        )
                )
                        * amplitude
                        * 0.35;

        double openSwell =
                Mth.sin(
                        (float) (
                                x * 0.060
                                        + z * 0.047
                                        + t * 0.31
                        )
                )
                        * amplitude
                        * (
                        0.28
                                + exposure
                                * 1.05
                );

        return shortChop
                + crossChop
                + openSwell;
    }

    private record WaterSurface(
            int x,
            int z,
            double baseY,
            int red,
            int green,
            int blue
    ) {
    }

    private static void clearCache() {
        SURFACES.clear();
        cachedCenterX = Integer.MIN_VALUE;
        cachedCenterZ = Integer.MIN_VALUE;
        cachedRadius = -1;
        cachedAt = Long.MIN_VALUE;
    }
}
