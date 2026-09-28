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
import net.caravidro.wayaround.nexus.client.NexusClientState;
import net.caravidro.wayaround.worldgen.weather.local.LocalWeatherField;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
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
    private static final double BASE_VOXEL = 7.5;
    private static final double MAX_VISUAL_RADIUS = 278.0;
    private static final int MAX_HORIZONTAL_VOXELS = 25;
    private static final int MAX_VERTICAL_VOXELS = 10;
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

        if (!WorldFeatureRuntime.clientEnabled(
                WorldFeature.PROCEDURAL_CLOUDS
        )) {
            CACHE.clear();
            return;
        }

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

            double verticalBounds =
                    34.0
                            + visualRadius
                                    * 0.10;

            AABB bounds =
                    new AABB(
                            cell.x() - visualRadius - 30.0,
                            cell.y() - verticalBounds,
                            cell.z() - visualRadius - 30.0,
                            cell.x() + visualRadius + 30.0,
                            cell.y() + verticalBounds,
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
                            ? 34
                            : Mth.clamp(
                                    Math.round(
                                            176.0F
                                                    + cell.storm()
                                                            * 24.0F
                                    ),
                                    168,
                                    200
                            );

            alpha =
                    Math.round(
                            alpha
                                    * NexusClientState.cloudVisibility()
                    );

            if (alpha <= 1) {
                continue;
            }

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
                    alpha,
                    time
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

            /*
             * These are the EXTERNAL shell faces, not smoke sprites.
             * Writing depth prevents the opposite/lower faces from bleeding
             * through the near shell and creating the stacked "lasagna"
             * pattern visible from below.
             */
            RenderSystem.depthMask(true);
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

        private double builtVoxel =
                BASE_VOXEL;

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

            double voxel =
                    voxelSizeFor(
                            cell
                    );

            builtVoxel =
                    voxel;

            int horizontal =
                    Mth.clamp(
                            (int) Math.ceil(
                                    radius / voxel
                            ),
                            4,
                            MAX_HORIZONTAL_VOXELS
                    );

            int vertical =
                    Mth.clamp(
                            (int) Math.ceil(
                                    (25.0 + radius * 0.105)
                                            / voxel
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
                                x * voxel;

                        double wy =
                                y * voxel;

                        double wz =
                                z * voxel;

                        if (occupiedByLobe(
                                lobes,
                                wx,
                                wy,
                                wz,
                                voxel
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
                int alpha,
                long time
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
                        lookZ,
                        builtVoxel
                )) {
                    continue;
                }

                CloudColor shade =
                        cloudColor(
                                cell,
                                voxel,
                                face,
                                red,
                                green,
                                blue,
                                alpha,
                                time,
                                builtVoxel
                        );

                emitFace(
                        buffer,
                        poseStack,
                        cell,
                        voxel,
                        face,
                        shade.red,
                        shade.green,
                        shade.blue,
                        shade.alpha,
                        builtVoxel
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
                            / builtVoxel
                    );

            int vy =
                    (int) Math.round(
                            (y - cell.y())
                            / builtVoxel
                    );

            int vz =
                    (int) Math.round(
                            (z - cell.z())
                            / builtVoxel
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
                36.0,
                MAX_VISUAL_RADIUS
        );
    }

    private static double voxelSizeFor(
            LocalWeatherField.CloudCell cell
    ) {
        double radius =
                visualRadius(
                        cell
                );

        /*
         * Giant clouds get larger voxels instead of exponentially more mesh
         * work. Up close ordinary clouds keep the original 9-block detail.
         */
        double giant =
                Mth.clamp(
                        (radius - 145.0)
                                / 133.0,
                        0.0,
                        1.0
                );

        /*
         * Keep giant fronts detailed enough that one voxel does not become a
         * building-sized plate. MAX_HORIZONTAL_VOXELS was raised alongside
         * this, so a 278-block cloud still fits without clipping its radius.
         */
        return BASE_VOXEL
                + giant
                        * 3.55;
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

        double giantInfluence =
                Mth.clamp(
                        (radius - 120.0)
                                / 158.0,
                        0.0,
                        1.0
                );

        int count =
                7
                + (int) Math.floor(
                        random01(seed ^ 0x44B82A14L)
                        * 7.0
                )
                + (int) Math.round(
                        giantInfluence
                                * 4.0
                );

        float windX =
                LocalWeatherField.windX(
                        time
                );

        float windZ =
                LocalWeatherField.windZ(
                        time
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
                        radius * (
                                0.46
                                        + random01(
                                        seed ^ 0x7C3F12A1L
                                ) * 0.08
                        ),
                        17.0
                                + radius * 0.060
                                + giantInfluence * 11.0
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
                    * (
                            26.0
                                    + giantInfluence
                                            * 32.0
                    )
                    + Math.sin(
                            t * 0.21
                            + phase
                    ) * (
                            4.0
                                    + giantInfluence
                                            * 3.5
                    );

            /*
             * Large fronts visibly lean and stretch with the wind. Higher
             * lobes receive more shear, mimicking faster airflow aloft.
             */
            double verticalBand =
                    Mth.clamp(
                            0.55
                                    + y
                                            / (
                                            52.0
                                                    + radius
                                                            * 0.12
                                    ),
                            0.10,
                            1.20
                    );

            double windShear =
                    giantInfluence
                            * verticalBand
                            * (
                            10.0
                                    + radialBase
                                            * 0.22
                                    + Math.sin(
                                    t * 0.10
                                            + phase
                            ) * 5.0
                    );

            x +=
                    windX
                            * windShear;

            z +=
                    windZ
                            * windShear;

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
                            11.0
                            + random01(
                                    lobeSeed
                                    ^ 0x9FB21C651E98DF25L
                            ) * (
                                    17.0
                                            + giantInfluence
                                                    * 14.0
                            )
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
            double z,
            double voxelSize
    ) {
        for (Lobe lobe : lobes) {
            if (lobe.horizontalRadius < voxelSize * 0.55) {
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
            double lookZ,
            double voxelSize
    ) {
        double cx =
                cell.x()
                        + voxel.x * voxelSize;

        double cy =
                cell.y()
                        + voxel.y * voxelSize;

        double cz =
                cell.z()
                        + voxel.z * voxelSize;

        double half =
                voxelSize * 0.505;

        double minX = cx - half;
        double minY = cy - half;
        double minZ = cz - half;
        double maxX = cx + half;
        double maxY = cy + half;
        double maxZ = cz + half;

        double centerX =
                cx
                        + face.dx * voxelSize * 0.5;

        double centerY =
                cy
                        + face.dy * voxelSize * 0.5;

        double centerZ =
                cz
                        + face.dz * voxelSize * 0.5;

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

    private static CloudColor cloudColor(
            LocalWeatherField.CloudCell cell,
            Voxel voxel,
            Face face,
            int baseRed,
            int baseGreen,
            int baseBlue,
            int baseAlpha,
            long time,
            double voxelSize
    ) {
        double radius =
                visualRadius(
                        cell
                );

        double verticalExtent =
                25.0
                        + radius
                                * 0.105;

        double localY =
                voxel.y
                        * voxelSize;

        double height01 =
                Mth.clamp(
                        (
                                localY
                                        + verticalExtent
                        )
                                / (
                                verticalExtent
                                        * 2.0
                                ),
                        0.0,
                        1.0
                );

        /*
         * Broad lighting first: bottom is heavier, top catches the sky, but
         * the difference is intentionally restrained. Huge per-face contrast
         * was making every voxel edge read like a separate slab.
         */
        double faceShade =
                switch (face) {
                    case DOWN ->
                            0.74;
                    case UP ->
                            1.03;
                    default ->
                            0.84
                                    + height01
                                            * 0.12;
                };

        double verticalShade =
                0.82
                        + height01
                                * 0.18;

        /*
         * Coherent low-frequency variation. Neighboring voxels now receive
         * almost the same shade instead of independent random greys, so the
         * cloud reads as one mass rather than a checkerboard.
         */
        double phase =
                random01(
                        cell.id()
                                ^ 0xD1B54A32D192ED03L
                )
                        * Math.PI
                        * 2.0;

        double broadPatch =
                Math.sin(
                        voxel.x * 0.31
                                + phase
                                + Math.sin(
                                voxel.z * 0.17
                                        - phase
                        ) * 0.45
                )
                        * 0.018
                        + Math.cos(
                        voxel.z * 0.27
                                - phase * 0.63
                )
                        * 0.014;

        /*
         * Very slow whole-cloud breathing. It changes the atmosphere without
         * causing individual cubes to flash independently.
         */
        double temporal =
                0.988
                        + Math.sin(
                                time * 0.0032
                                        + phase
                        ) * 0.012;

        double shade =
                faceShade
                        * verticalShade
                        * (
                        1.0
                                + broadPatch
                        )
                        * temporal;

        double tint =
                signedColorBias(
                        cell.id()
                );

        int red =
                Mth.clamp(
                        (int) Math.round(
                                baseRed
                                        * shade
                                        * (
                                        1.0
                                                + tint
                                                        * 0.025
                                )
                        ),
                        10,
                        255
                );

        int green =
                Mth.clamp(
                        (int) Math.round(
                                baseGreen
                                        * shade
                        ),
                        10,
                        255
                );

        int blue =
                Mth.clamp(
                        (int) Math.round(
                                baseBlue
                                        * shade
                                        * (
                                        1.0
                                                - tint
                                                        * 0.035
                                )
                        ),
                        12,
                        255
                );

        int alpha =
                Mth.clamp(
                        baseAlpha
                                + (
                                face == Face.DOWN
                                        ? 4
                                        : face == Face.UP
                                        ? -4
                                        : 0
                        ),
                        24,
                        236
                );

        return new CloudColor(
                red,
                green,
                blue,
                alpha
        );
    }

    private static double signedColorBias(
            long seed
    ) {
        return random01(
                seed
                        ^ 0xA0761D6478BD642FL
        ) * 2.0
                - 1.0;
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
            int alpha,
            double voxelSize
    ) {
        double cx =
                cell.x()
                + voxel.x * voxelSize;

        double cy =
                cell.y()
                + voxel.y * voxelSize;

        double cz =
                cell.z()
                + voxel.z * voxelSize;

        double half =
                voxelSize * 0.505;

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

    private record CloudColor(
            int red,
            int green,
            int blue,
            int alpha
    ) {
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
