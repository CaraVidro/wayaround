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

/**
 * Simple-Clouds-inspired renderer, intentionally much simpler:
 *
 * - cloud bodies are a sparse voxel field;
 * - only exposed voxel faces are emitted;
 * - the voxel field is rebuilt occasionally as its lobes drift/grow/shrink;
 * - when the camera enters a cloud the outside shell becomes much more
 *   transparent and nearby occupied voxels gain faint internal faces.
 *
 * This gives Way Around actual cloud volume without ray marching.
 */
@EventBusSubscriber(modid = WayAround.MODID, value = Dist.CLIENT)
public final class LivingCloudRenderer {

    private static final double RENDER_RANGE = 920.0;
    private static final double VOXEL = 9.0;
    private static final double MAX_VISUAL_RADIUS = 240.0;
    private static final int MAX_HORIZONTAL_VOXELS = 28;
    private static final int MAX_VERTICAL_VOXELS = 7;
    private static final int REBUILD_INTERVAL = 10;
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

        List<LocalWeatherField.CloudCell> cells =
                LocalWeatherField.nearbyCells(
                        camera.x,
                        camera.z,
                        time,
                        RENDER_RANGE
                );

        Set<Long> visibleIds = new HashSet<>();
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

            if (mesh.needsRebuild(time, cell)) {
                mesh.rebuild(cell, time);
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
                            ? 64
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
                    red,
                    green,
                    blue,
                    alpha
            );

            if (inside) {
                anyVertex |= mesh.emitInteriorNearCamera(
                        buffer,
                        poseStack,
                        cell,
                        camera,
                        red,
                        green,
                        blue,
                        30
                );
            }
        }

        Iterator<Map.Entry<Long, CloudMesh>> iterator =
                CACHE.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Long, CloudMesh> entry =
                    iterator.next();

            if (!visibleIds.contains(entry.getKey())
                    && time - entry.getValue().lastUsed > 100L) {
                iterator.remove();
            }
        }

        if (anyVertex) {
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

        poseStack.popPose();
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

            if (mesh.needsRebuild(time, cell)) {
                mesh.rebuild(cell, time);
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
                LocalWeatherField.CloudCell cell
        ) {
            lastUsed = time;

            return builtAt == Long.MIN_VALUE
                    || time - builtAt >= REBUILD_INTERVAL
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

            builtAt = time;
            builtRadius = radius;
            builtHoleRevision = holeRevision;
            lastUsed = time;
        }

        private boolean emitSurface(
                BufferBuilder buffer,
                PoseStack poseStack,
                LocalWeatherField.CloudCell cell,
                int red,
                int green,
                int blue,
                int alpha
        ) {
            if (occupied.isEmpty()) {
                return false;
            }

            boolean emitted =
                    false;

            for (Voxel voxel : occupied) {
                for (Face face : Face.values()) {
                    Voxel neighbor =
                            new Voxel(
                                    voxel.x + face.dx,
                                    voxel.y + face.dy,
                                    voxel.z + face.dz
                            );

                    if (occupied.contains(neighbor)) {
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

                    emitted = true;
                }
            }

            return emitted;
        }

        private boolean emitInteriorNearCamera(
                BufferBuilder buffer,
                PoseStack poseStack,
                LocalWeatherField.CloudCell cell,
                Vec3 camera,
                int red,
                int green,
                int blue,
                int alpha
        ) {
            double localX =
                    camera.x - cell.x();

            double localY =
                    camera.y - cell.y();

            double localZ =
                    camera.z - cell.z();

            double radiusSquared =
                    34.0 * 34.0;

            boolean emitted =
                    false;

            for (Voxel voxel : occupied) {
                double vx =
                        voxel.x * VOXEL;

                double vy =
                        voxel.y * VOXEL;

                double vz =
                        voxel.z * VOXEL;

                double dx =
                        vx - localX;

                double dy =
                        vy - localY;

                double dz =
                        vz - localZ;

                if (dx * dx + dy * dy + dz * dz
                        > radiusSquared) {
                    continue;
                }

                for (Face face : Face.values()) {
                    /*
                     * These faint local faces are only created while inside
                     * the cloud. They give nearby fog some readable shape
                     * instead of leaving the camera inside an empty shell.
                     */
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

                    emitted = true;
                }
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
                        radius * 0.60,
                        17.0 + radius * 0.045
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
                            0.18
                            + random01(
                                    lobeSeed
                                    ^ 0xBF58476D1CE4E5B9L
                            ) * 0.62
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
                            0.18
                            + random01(
                                    lobeSeed
                                    ^ 0xA24BAED4963EE407L
                            ) * 0.27
                    )
                    * pulse;

            double verticalRadius =
                    (
                            11.0
                            + random01(
                                    lobeSeed
                                    ^ 0x9FB21C651E98DF25L
                            ) * 18.0
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
