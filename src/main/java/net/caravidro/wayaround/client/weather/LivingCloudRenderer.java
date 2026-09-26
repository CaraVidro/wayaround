package net.caravidro.wayaround.client.weather;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

/**
 * Simple-Clouds-inspired renderer, intentionally much simpler:
 *
 * - cloud bodies are a sparse voxel field;
 * - exposed faces are cached when the mesh is rebuilt, not rediscovered every frame;
 * - distant clouds rebuild less frequently than nearby clouds;
 * - when the camera enters a cloud the outside shell becomes more transparent.
 *
 * This gives Way Around actual cloud volume without ray marching.
 */
@EventBusSubscriber(modid = WayAround.MODID, value = Dist.CLIENT)
public final class LivingCloudRenderer {

    private static final double RENDER_RANGE = 760.0;
    private static final double VOXEL = 9.0;
    private static final double MAX_VISUAL_RADIUS = 156.0;
    private static final int MAX_HORIZONTAL_VOXELS = 19;
    private static final int MAX_VERTICAL_VOXELS = 7;
    private static final int REBUILD_INTERVAL_NEAR = 10;
    private static final int REBUILD_INTERVAL_MID = 20;
    private static final int REBUILD_INTERVAL_FAR = 40;
    private static final int MAX_REBUILDS_PER_FRAME = 2;
    private static final double CAMERA_FACE_CLEAR_RADIUS = 18.0;
    private static final double CAMERA_NEAR_GUARD = 0.35;
    private static final Map<Long, CloudMesh> CACHE = new HashMap<>();

    /*
     * Temporary holes cut by the Blue prototype. Clouds are generated and
     * rendered client-side, so the cut belongs here rather than in world data.
     * The revision number forces cached cloud meshes to rebuild immediately.
     */
    private static final List<CloudHole> HOLES = new ArrayList<>();
    private static long holeRevision;

    private LivingCloudRenderer() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null
                || minecraft.player == null
                || !minecraft.level.dimension().equals(Level.OVERWORLD)
                || AntarcticClientLighting.isAntarctic(minecraft)) {
            CACHE.clear();
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

        long time = minecraft.level.getGameTime();

        pruneHoles(time);

        Vec3 skyColor =
                minecraft.level.getSkyColor(
                        camera,
                        1.0F
                );

        float worldShade =
                Mth.clamp(
                        (float) (
                                skyColor.x * 0.2126
                                + skyColor.y * 0.7152
                                + skyColor.z * 0.0722
                        )
                        * 1.35F,
                        0.10F,
                        1.0F
                );

        double renderRange =
                effectiveRenderRange(
                        minecraft
                );

        List<LocalWeatherField.CloudCell> cells =
                LocalWeatherField.nearbyCells(
                        camera.x,
                        camera.z,
                        time,
                        renderRange
                );

        Set<Long> visibleIds = new HashSet<>();
        int rebuildBudget =
                MAX_REBUILDS_PER_FRAME;
        boolean anyVertex = false;

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        BufferBuilder buffer =
                Tesselator.getInstance().begin(
                        VertexFormat.Mode.QUADS,
                        DefaultVertexFormat.POSITION_COLOR
                );

        for (LocalWeatherField.CloudCell cell : cells) {
            double visualRadius =
                    visualRadius(
                            cell
                    );

            AABB bounds =
                    new AABB(
                            cell.x() - visualRadius - 30.0,
                            cell.y() - 42.0,
                            cell.z() - visualRadius - 30.0,
                            cell.x() + visualRadius + 30.0,
                            cell.y() + 48.0,
                            cell.z() + visualRadius + 30.0
                    );

            if (!event.getFrustum().isVisible(bounds)) {
                continue;
            }

            visibleIds.add(cell.id());

            CloudMesh mesh =
                    CACHE.computeIfAbsent(
                            cell.id(),
                            id -> new CloudMesh()
                    );

            double cellDx =
                    cell.x() - camera.x;

            double cellDz =
                    cell.z() - camera.z;

            int rebuildInterval =
                    rebuildIntervalForDistance(
                            cellDx * cellDx
                                    + cellDz * cellDz
                    );

            if (mesh.needsRebuild(
                    time,
                    cell,
                    rebuildInterval
            )) {
                if (rebuildBudget > 0) {
                    mesh.rebuild(
                            cell,
                            time
                    );

                    rebuildBudget--;
                } else if (mesh.builtAt
                        == Long.MIN_VALUE) {
                    /*
                     * New cloud with no mesh yet: skip this frame rather than
                     * rebuilding every nearby cloud in one giant hitch.
                     */
                    continue;
                }
            }

            boolean inside =
                    mesh.containsWorld(
                            cell,
                            camera.x,
                            camera.y,
                            camera.z
                    );

            int alpha =
                    inside
                            ? 40
                            : 194;

            int brightness =
                    Mth.clamp(
                            Math.round(
                                    (
                                            245.0F
                                            - cell.storm() * 150.0F
                                    )
                                    * worldShade
                            ),
                            18,
                            245
                    );

            int red =
                    brightness;

            int green =
                    Mth.clamp(
                            Math.round(
                                    brightness
                                    + 4.0F
                                    * worldShade
                            ),
                            0,
                            255
                    );

            int blue =
                    Mth.clamp(
                            Math.round(
                                    brightness
                                    + 10.0F
                                    * worldShade
                            ),
                            0,
                            255
                    );

            anyVertex |= mesh.emitSurface(
                    buffer,
                    poseStack,
                    cell,
                    camera,
                    lookX,
                    lookY,
                    lookZ,
                    red,
                    green,
                    blue,
                    alpha
            );

            /*
             * Do NOT render arbitrary internal voxel faces around the camera.
             *
             * The old pass emitted all six faces of every occupied voxel up
             * to 34 blocks from the player. Because those quads are
             * translucent, overlapping internal faces produced giant dark /
             * blue polygon sheets at the edge of the screen that appeared to
             * move with the player.
             *
             * The external shell already gives the cloud volume; when the
             * camera is inside, we simply make that shell more transparent.
             */
        }

        Iterator<Map.Entry<Long, CloudMesh>> iterator =
                CACHE.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Long, CloudMesh> entry =
                    iterator.next();

            if (!visibleIds.contains(entry.getKey())
                    && time - entry.getValue().lastUsed > 200L) {
                iterator.remove();
            }
        }

        if (anyVertex) {
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

        poseStack.popPose();
    }

    private static double effectiveRenderRange(
            Minecraft minecraft
    ) {
        /*
         * Custom clouds used to ignore vanilla render distance entirely and
         * always evaluate out to 760 blocks. That defeats the main low-end
         * performance control. Keep a useful minimum, then scale with the
         * player's chunk setting and cap at the original cinematic range.
         */
        double vanillaBlocks =
                minecraft.options
                        .renderDistance()
                        .get()
                        * 16.0;

        return Math.min(
                RENDER_RANGE,
                Math.max(
                        160.0,
                        vanillaBlocks
                                * 1.5
                )
        );
    }

    public static boolean isInsideCloud(
            Vec3 position
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || !minecraft.level.dimension().equals(Level.OVERWORLD)) {
            return false;
        }

        long time =
                minecraft.level.getGameTime();

        for (LocalWeatherField.CloudCell cell :
                LocalWeatherField.nearbyCells(
                        position.x,
                        position.z,
                        time,
                        64.0
                )) {

            CloudMesh mesh =
                    CACHE.computeIfAbsent(
                            cell.id(),
                            id -> new CloudMesh()
                    );

            if (mesh.needsRebuild(
                    time,
                    cell,
                    REBUILD_INTERVAL_NEAR
            )) {
                mesh.rebuild(
                        cell,
                        time
                );
            }

            if (mesh.containsWorld(
                    cell,
                    position.x,
                    position.y,
                    position.z
            )) {
                return true;
            }
        }

        return false;
    }

    public static void punchHole(
            Vec3 center,
            double radius
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || !minecraft.level.dimension().equals(Level.OVERWORLD)) {
            return;
        }

        long time =
                minecraft.level.getGameTime();

        pruneHoles(time);

        /*
         * Merge samples from a moving Blue so a tunnel does not become
         * hundreds of tiny allocations. Nearby samples simply refresh and
         * slightly enlarge the previous cut.
         */
        for (int i = 0;
                i < HOLES.size();
                i++) {
            CloudHole hole =
                    HOLES.get(i);

            double mergeDistance =
                    Math.max(
                            5.0,
                            Math.min(
                                    radius,
                                    hole.radius
                            )
                                    * 0.58
                    );

            if (hole.center.distanceToSqr(center)
                    <= mergeDistance
                            * mergeDistance) {
                HOLES.set(
                        i,
                        new CloudHole(
                                center,
                                Math.max(
                                        radius,
                                        hole.radius
                                ),
                                time + 260L
                        )
                );

                holeRevision++;
                return;
            }
        }

        HOLES.add(
                new CloudHole(
                        center,
                        radius,
                        time + 260L
                )
        );

        while (HOLES.size() > 96) {
            HOLES.remove(0);
        }

        holeRevision++;
    }

    private static void pruneHoles(
            long time
    ) {
        if (HOLES.removeIf(
                hole ->
                        time >= hole.expiresAt
        )) {
            holeRevision++;
        }
    }

    private static boolean cutByBlue(
            double x,
            double y,
            double z
    ) {
        for (CloudHole hole : HOLES) {
            double dx =
                    x - hole.center.x;

            double dy =
                    y - hole.center.y;

            double dz =
                    z - hole.center.z;

            if (dx * dx
                    + dy * dy
                    + dz * dz
                    <= hole.radius
                            * hole.radius) {
                return true;
            }
        }

        return false;
    }

    private static final class CloudMesh {

        private final Set<Voxel> occupied =
                new HashSet<>();

        /*
         * Expensive neighbor tests happen only when the cloud shape changes.
         * Rendering then walks this compact exposed-face list directly.
         */
        private final List<SurfaceFace> surfaceFaces =
                new ArrayList<>();

        private long builtAt =
                Long.MIN_VALUE;

        private long lastUsed =
                Long.MIN_VALUE;

        private double builtRadius =
                -1.0;

        private long builtHoleRevision =
                Long.MIN_VALUE;

        private boolean needsRebuild(
                long time,
                LocalWeatherField.CloudCell cell,
                int rebuildInterval
        ) {
            lastUsed = time;

            return builtAt == Long.MIN_VALUE
                    || time - builtAt >= rebuildInterval
                    || builtHoleRevision != holeRevision
                    || Math.abs(
                            builtRadius
                                    - visualRadius(
                                            cell
                                    )
                    ) > 0.01;
        }

        private void rebuild(
                LocalWeatherField.CloudCell cell,
                long time
        ) {
            occupied.clear();

            double radius =
                    visualRadius(
                            cell
                    );

            int horizontal =
                    Mth.clamp(
                            (int) Math.ceil(
                                    radius / VOXEL
                            ),
                            4,
                            MAX_HORIZONTAL_VOXELS
                    );

            int vertical =
                    Mth.clamp(
                            (int) Math.ceil(
                                    (18.0 + radius * 0.055)
                                            / VOXEL
                            ),
                            2,
                            MAX_VERTICAL_VOXELS
                    );

            List<Lobe> lobes =
                    lobes(
                            cell,
                            time
                    );

            for (int x = -horizontal;
                    x <= horizontal;
                    x++) {

                for (int y = -vertical;
                        y <= vertical;
                        y++) {

                    for (int z = -horizontal;
                            z <= horizontal;
                            z++) {

                        double wx =
                                x * VOXEL;

                        double wy =
                                y * VOXEL;

                        double wz =
                                z * VOXEL;

                        if (occupiedByLobe(
                                lobes,
                                wx,
                                wy,
                                wz
                        )
                                && !cutByBlue(
                                        cell.x() + wx,
                                        cell.y() + wy,
                                        cell.z() + wz
                                )) {
                            occupied.add(
                                    new Voxel(
                                            x,
                                            y,
                                            z
                                    )
                            );
                        }
                    }
                }
            }

            rebuildSurfaceFaces();

            builtAt = time;
            builtRadius = radius;
            builtHoleRevision = holeRevision;
            lastUsed = time;
        }

        private void rebuildSurfaceFaces() {
            surfaceFaces.clear();

            for (Voxel voxel : occupied) {
                for (Face face : Face.values()) {
                    if (occupied.contains(
                            new Voxel(
                                    voxel.x + face.dx,
                                    voxel.y + face.dy,
                                    voxel.z + face.dz
                            )
                    )) {
                        continue;
                    }

                    surfaceFaces.add(
                            new SurfaceFace(
                                    voxel,
                                    face
                            )
                    );
                }
            }
        }

        private boolean emitSurface(
                BufferBuilder buffer,
                PoseStack poseStack,
                LocalWeatherField.CloudCell cell,
                Vec3 camera,
                double lookX,
                double lookY,
                double lookZ,
                int red,
                int green,
                int blue,
                int alpha
        ) {
            if (surfaceFaces.isEmpty()) {
                return false;
            }

            boolean emitted =
                    false;

            for (SurfaceFace surfaceFace :
                    surfaceFaces) {

                Voxel voxel =
                        surfaceFace.voxel;

                Face face =
                        surfaceFace.face;

                if (faceUnsafeForCamera(
                        cell,
                        voxel,
                        face,
                        camera,
                        lookX,
                        lookY,
                        lookZ
                )) {
                    continue;
                }

                emitFace(
                        buffer,
                        poseStack,
                        cell,
                        voxel,
                        face,
                        red,
                        green,
                        blue,
                        alpha
                );

                emitted =
                        true;
            }

            return emitted;
        }

        private boolean containsWorld(
                LocalWeatherField.CloudCell cell,
                double x,
                double y,
                double z
        ) {
            int vx =
                    (int) Math.round(
                            (x - cell.x())
                            / VOXEL
                    );

            int vy =
                    (int) Math.round(
                            (y - cell.y())
                            / VOXEL
                    );

            int vz =
                    (int) Math.round(
                            (z - cell.z())
                            / VOXEL
                    );

            return occupied.contains(
                    new Voxel(
                            vx,
                            vy,
                            vz
                    )
            );
        }
    }

    private static int rebuildIntervalForDistance(
            double distanceSquared
    ) {
        if (distanceSquared > 520.0 * 520.0) {
            return REBUILD_INTERVAL_FAR;
        }

        if (distanceSquared > 280.0 * 280.0) {
            return REBUILD_INTERVAL_MID;
        }

        return REBUILD_INTERVAL_NEAR;
    }

    private static double visualRadius(
            LocalWeatherField.CloudCell cell
    ) {
        double radius =
                cell.radius();

        if (!Double.isFinite(
                radius
        )) {
            return 96.0;
        }

        return Mth.clamp(
                radius,
                40.0,
                MAX_VISUAL_RADIUS
        );
    }

    private static List<Lobe> lobes(
            LocalWeatherField.CloudCell cell,
            long time
    ) {
        long seed =
                cell.id();

        double radius =
                visualRadius(
                        cell
                );

        int count =
                7
                + (int) Math.floor(
                        random01(seed ^ 0x44B82A14L)
                        * 7.0
                );

        List<Lobe> lobes =
                new ArrayList<>(
                        count + 3
                );

        double t =
                time * 0.0125;

        /*
         * Main connected mass.
         */
        lobes.add(
                new Lobe(
                        0.0,
                        0.0,
                        0.0,
                        radius * 0.48,
                        14.0 + radius * 0.035
                )
        );

        for (int i = 0;
                i < count;
                i++) {

            long lobeSeed =
                    mix64(
                            seed
                            + i * 0x9E3779B97F4A7C15L
                    );

            double phase =
                    random01(
                            lobeSeed
                            ^ 0xD1B54A32D192ED03L
                    ) * Math.PI * 2.0;

            double angle =
                    random01(
                            lobeSeed
                            ^ 0x94D049BB133111EBL
                    ) * Math.PI * 2.0;

            double radialBase =
                    radius
                    * (
                            0.16
                            + random01(
                                    lobeSeed
                                    ^ 0xBF58476D1CE4E5B9L
                            ) * 0.48
                    );

            /*
             * Slow drifting lets chunks of a cloud join, separate and
             * reconnect. The satellite lobes can drift far enough to look
             * temporarily detached.
             */
            double separation =
                    0.78
                    + Math.sin(
                            t * 0.17
                            + phase
                    ) * 0.28;

            double radial =
                    radialBase
                    * separation;

            double x =
                    Math.cos(angle) * radial
                    + Math.sin(
                            t * 0.11
                            + phase
                    ) * 12.0;

            double z =
                    Math.sin(angle) * radial
                    + Math.cos(
                            t * 0.09
                            + phase
                    ) * 12.0;

            double y =
                    (
                            random01(
                                    lobeSeed
                                    ^ 0x632BE59BD9B4E019L
                            ) - 0.5
                    )
                    * 20.0
                    + Math.sin(
                            t * 0.21
                            + phase
                    ) * 4.0;

            double pulse =
                    0.70
                    + Math.sin(
                            t * 0.14
                            + phase
                    ) * 0.24;

            /*
             * Some lobes periodically shrink almost to nothing. This is the
             * cheap version of a cloud fragment dissolving and reforming.
             */
            if ((i % 4) == 3) {
                pulse *=
                        0.38
                        + Math.max(
                                0.0,
                                Math.sin(
                                        t * 0.08
                                        + phase
                                )
                        ) * 0.92;
            }

            double horizontalRadius =
                    radius
                    * (
                            0.16
                            + random01(
                                    lobeSeed
                                    ^ 0xA24BAED4963EE407L
                            ) * 0.22
                    )
                    * pulse;

            double verticalRadius =
                    (
                            9.0
                            + random01(
                                    lobeSeed
                                    ^ 0x9FB21C651E98DF25L
                            ) * 13.0
                    )
                    * (
                            0.82
                            + pulse * 0.30
                    );

            lobes.add(
                    new Lobe(
                            x,
                            y,
                            z,
                            horizontalRadius,
                            verticalRadius
                    )
            );
        }

        return lobes;
    }

    private static boolean occupiedByLobe(
            List<Lobe> lobes,
            double x,
            double y,
            double z
    ) {
        for (Lobe lobe : lobes) {
            if (lobe.horizontalRadius < VOXEL * 0.55) {
                continue;
            }

            double dx =
                    (x - lobe.x)
                    / lobe.horizontalRadius;

            double dy =
                    (y - lobe.y)
                    / lobe.verticalRadius;

            double dz =
                    (z - lobe.z)
                    / lobe.horizontalRadius;

            if (dx * dx + dy * dy + dz * dz <= 1.0) {
                return true;
            }
        }

        return false;
    }

    private static boolean faceUnsafeForCamera(
            LocalWeatherField.CloudCell cell,
            Voxel voxel,
            Face face,
            Vec3 camera,
            double lookX,
            double lookY,
            double lookZ
    ) {
        double cx =
                cell.x()
                        + voxel.x * VOXEL;

        double cy =
                cell.y()
                        + voxel.y * VOXEL;

        double cz =
                cell.z()
                        + voxel.z * VOXEL;

        double half =
                VOXEL * 0.505;

        double minX = cx - half;
        double minY = cy - half;
        double minZ = cz - half;
        double maxX = cx + half;
        double maxY = cy + half;
        double maxZ = cz + half;

        double centerX =
                cx
                        + face.dx * VOXEL * 0.5;

        double centerY =
                cy
                        + face.dy * VOXEL * 0.5;

        double centerZ =
                cz
                        + face.dz * VOXEL * 0.5;

        double dx =
                centerX - camera.x;

        double dy =
                centerY - camera.y;

        double dz =
                centerZ - camera.z;

        /*
         * First guard: do not allow a giant voxel wall close enough to fill
         * most of the screen. This is intentionally larger than a single
         * 9-block voxel because the face center can be "safe" while one corner
         * is almost touching the camera.
         */
        if (dx * dx
                + dy * dy
                + dz * dz
                < CAMERA_FACE_CLEAR_RADIUS
                        * CAMERA_FACE_CLEAR_RADIUS) {
            return true;
        }

        /*
         * Second guard: test ALL FOUR vertices against the camera forward
         * plane. If even one corner sits behind / inside the near plane, the
         * quad would be clipped into a huge screen-space polygon. Skip the
         * whole face instead of trusting perspective clipping.
         */
        return switch (face) {
            case DOWN, UP -> {
                double y =
                        face == Face.DOWN
                                ? minY
                                : maxY;

                yield !pointSafelyInFront(
                        camera,
                        lookX,
                        lookY,
                        lookZ,
                        minX,
                        y,
                        minZ
                )
                        || !pointSafelyInFront(
                        camera,
                        lookX,
                        lookY,
                        lookZ,
                        maxX,
                        y,
                        minZ
                )
                        || !pointSafelyInFront(
                        camera,
                        lookX,
                        lookY,
                        lookZ,
                        maxX,
                        y,
                        maxZ
                )
                        || !pointSafelyInFront(
                        camera,
                        lookX,
                        lookY,
                        lookZ,
                        minX,
                        y,
                        maxZ
                );
            }

            case NORTH, SOUTH -> {
                double z =
                        face == Face.NORTH
                                ? minZ
                                : maxZ;

                yield !pointSafelyInFront(
                        camera,
                        lookX,
                        lookY,
                        lookZ,
                        minX,
                        minY,
                        z
                )
                        || !pointSafelyInFront(
                        camera,
                        lookX,
                        lookY,
                        lookZ,
                        maxX,
                        minY,
                        z
                )
                        || !pointSafelyInFront(
                        camera,
                        lookX,
                        lookY,
                        lookZ,
                        maxX,
                        maxY,
                        z
                )
                        || !pointSafelyInFront(
                        camera,
                        lookX,
                        lookY,
                        lookZ,
                        minX,
                        maxY,
                        z
                );
            }

            case WEST, EAST -> {
                double x =
                        face == Face.WEST
                                ? minX
                                : maxX;

                yield !pointSafelyInFront(
                        camera,
                        lookX,
                        lookY,
                        lookZ,
                        x,
                        minY,
                        minZ
                )
                        || !pointSafelyInFront(
                        camera,
                        lookX,
                        lookY,
                        lookZ,
                        x,
                        minY,
                        maxZ
                )
                        || !pointSafelyInFront(
                        camera,
                        lookX,
                        lookY,
                        lookZ,
                        x,
                        maxY,
                        maxZ
                )
                        || !pointSafelyInFront(
                        camera,
                        lookX,
                        lookY,
                        lookZ,
                        x,
                        maxY,
                        minZ
                );
            }
        };
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

    private static void emitFace(
            BufferBuilder buffer,
            PoseStack poseStack,
            LocalWeatherField.CloudCell cell,
            Voxel voxel,
            Face face,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        double cx =
                cell.x()
                + voxel.x * VOXEL;

        double cy =
                cell.y()
                + voxel.y * VOXEL;

        double cz =
                cell.z()
                + voxel.z * VOXEL;

        double half =
                VOXEL * 0.505;

        double minX =
                cx - half;

        double minY =
                cy - half;

        double minZ =
                cz - half;

        double maxX =
                cx + half;

        double maxY =
                cy + half;

        double maxZ =
                cz + half;

        var matrix =
                poseStack.last().pose();

        switch (face) {
            case DOWN -> {
                vertex(buffer, matrix, minX, minY, maxZ, red, green, blue, alpha);
                vertex(buffer, matrix, maxX, minY, maxZ, red, green, blue, alpha);
                vertex(buffer, matrix, maxX, minY, minZ, red, green, blue, alpha);
                vertex(buffer, matrix, minX, minY, minZ, red, green, blue, alpha);
            }
            case UP -> {
                vertex(buffer, matrix, minX, maxY, minZ, red, green, blue, alpha);
                vertex(buffer, matrix, maxX, maxY, minZ, red, green, blue, alpha);
                vertex(buffer, matrix, maxX, maxY, maxZ, red, green, blue, alpha);
                vertex(buffer, matrix, minX, maxY, maxZ, red, green, blue, alpha);
            }
            case NORTH -> {
                vertex(buffer, matrix, minX, minY, minZ, red, green, blue, alpha);
                vertex(buffer, matrix, maxX, minY, minZ, red, green, blue, alpha);
                vertex(buffer, matrix, maxX, maxY, minZ, red, green, blue, alpha);
                vertex(buffer, matrix, minX, maxY, minZ, red, green, blue, alpha);
            }
            case SOUTH -> {
                vertex(buffer, matrix, minX, maxY, maxZ, red, green, blue, alpha);
                vertex(buffer, matrix, maxX, maxY, maxZ, red, green, blue, alpha);
                vertex(buffer, matrix, maxX, minY, maxZ, red, green, blue, alpha);
                vertex(buffer, matrix, minX, minY, maxZ, red, green, blue, alpha);
            }
            case WEST -> {
                vertex(buffer, matrix, minX, minY, maxZ, red, green, blue, alpha);
                vertex(buffer, matrix, minX, minY, minZ, red, green, blue, alpha);
                vertex(buffer, matrix, minX, maxY, minZ, red, green, blue, alpha);
                vertex(buffer, matrix, minX, maxY, maxZ, red, green, blue, alpha);
            }
            case EAST -> {
                vertex(buffer, matrix, maxX, minY, minZ, red, green, blue, alpha);
                vertex(buffer, matrix, maxX, minY, maxZ, red, green, blue, alpha);
                vertex(buffer, matrix, maxX, maxY, maxZ, red, green, blue, alpha);
                vertex(buffer, matrix, maxX, maxY, minZ, red, green, blue, alpha);
            }
        }
    }

    private static void vertex(
            BufferBuilder buffer,
            org.joml.Matrix4f matrix,
            double x,
            double y,
            double z,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        buffer.addVertex(
                        matrix,
                        (float) x,
                        (float) y,
                        (float) z
                )
                .setColor(
                        red,
                        green,
                        blue,
                        alpha
                );
    }

    private static double random01(
            long value
    ) {
        long bits =
                mix64(value) >>> 11;

        return bits * 0x1.0p-53;
    }

    private static long mix64(
            long value
    ) {
        value ^= value >>> 30;
        value *= 0xbf58476d1ce4e5b9L;
        value ^= value >>> 27;
        value *= 0x94d049bb133111ebL;
        value ^= value >>> 31;
        return value;
    }

    private record CloudHole(
            Vec3 center,
            double radius,
            long expiresAt
    ) {
    }

    private record Voxel(
            int x,
            int y,
            int z
    ) {
    }

    private record SurfaceFace(
            Voxel voxel,
            Face face
    ) {
    }

    private record Lobe(
            double x,
            double y,
            double z,
            double horizontalRadius,
            double verticalRadius
    ) {
    }

    private enum Face {
        DOWN(0, -1, 0),
        UP(0, 1, 0),
        NORTH(0, 0, -1),
        SOUTH(0, 0, 1),
        WEST(-1, 0, 0),
        EAST(1, 0, 0);

        private final int dx;
        private final int dy;
        private final int dz;

        Face(
                int dx,
                int dy,
                int dz
        ) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
        }
    }
}
