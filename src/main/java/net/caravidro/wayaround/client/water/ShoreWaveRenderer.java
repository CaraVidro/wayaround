package net.caravidro.wayaround.client.water;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldconfig.WaveMode;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.caravidro.wayaround.worldgen.water.wave.OceanWaveField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Thin shoreline run-up and fading wet-sand marks.
 *
 * <p>No blocks are replaced. The wave field decides how far a crest reaches,
 * this renderer only paints the transient sheet/mark. Future erosion, sound,
 * footprints or ecology can query OceanWaveField directly without depending on
 * this client class.</p>
 */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class ShoreWaveRenderer {

    private static final List<ShorePatch> PATCHES =
            new ArrayList<>();

    private static final Map<Long, WetMark> WET_MARKS =
            new HashMap<>();

    private static final Map<Long, Long> WALL_SPLASH_AT =
            new HashMap<>();

    private static int cachedX =
            Integer.MIN_VALUE;

    private static int cachedZ =
            Integer.MIN_VALUE;

    private static long cachedAt =
            Long.MIN_VALUE;

    private ShoreWaveRenderer() {
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
        )
                || WorldFeatureRuntime.clientWaveMode()
                != WaveMode.REALISTIC) {
            clear();
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || minecraft.player == null
                || !minecraft.level.dimension()
                .equals(
                        Level.OVERWORLD
                )) {
            clear();
            return;
        }

        long time =
                minecraft.level.getGameTime();

        int playerX =
                minecraft.player.getBlockX();

        int playerZ =
                minecraft.player.getBlockZ();

        if (needsRebuild(
                playerX,
                playerZ,
                time
        )) {
            rebuild(
                    minecraft,
                    playerX,
                    playerZ,
                    time
            );
        }

        if (PATCHES.isEmpty()
                && WET_MARKS.isEmpty()) {
            return;
        }

        if (time % 40L == 0L) {
            WET_MARKS.entrySet()
                    .removeIf(
                            entry -> entry.getValue()
                                    .expiresAt()
                                    < time
                    );

            WALL_SPLASH_AT.entrySet()
                    .removeIf(
                            entry -> entry.getValue()
                                    + 80L
                                    < time
                    );
        }

        Vec3 camera =
                event.getCamera()
                        .getPosition();

        Vector3f look =
                event.getCamera()
                        .getLookVector();

        PoseStack pose =
                event.getPoseStack();

        pose.pushPose();

        pose.translate(
                -camera.x,
                -camera.y,
                -camera.z
        );

        BufferBuilder buffer =
                Tesselator.getInstance()
                        .begin(
                                VertexFormat.Mode.QUADS,
                                DefaultVertexFormat.POSITION_COLOR
                        );

        boolean any =
                false;

        for (ShorePatch patch :
                PATCHES) {
            double centerX =
                    patch.x()
                            + 0.5;

            double centerZ =
                    patch.z()
                            + 0.5;

            double dx =
                    centerX
                            - camera.x;

            double dz =
                    centerZ
                            - camera.z;

            if (dx * dx
                    + dz * dz
                    > 52.0 * 52.0) {
                continue;
            }

            double forward =
                    dx
                            * look.x()
                            + (
                            patch.surfaceY()
                                    - camera.y
                    ) * look.y()
                            + dz
                            * look.z();

            if (forward < 0.16) {
                continue;
            }

            OceanWaveField.Sample sample =
                    OceanWaveField.sample(
                            patch.profile(),
                            patch.waterX()
                                    + 0.5,
                            patch.waterZ()
                                    + 0.5,
                            time
                    );

            boolean covered =
                    sample.crest()
                            > 0.20F
                            && sample.runup()
                            + 0.15F
                            >= patch.distance();

            long key =
                    BlockPos.asLong(
                            patch.x(),
                            patch.blockY(),
                            patch.z()
                    );

            if (covered) {
                if (patch.wall()) {
                    impactWall(
                            minecraft,
                            patch,
                            sample,
                            time
                    );

                    continue;
                }

                float coverage =
                        Mth.clamp(
                                (
                                        sample.runup()
                                                - patch.distance()
                                                + 0.85F
                                ) / 1.65F,
                                0.08F,
                                1.0F
                        );

                float farMark =
                        Mth.clamp(
                                patch.distance()
                                        / Math.max(
                                        1.0F,
                                        patch.profile()
                                                .maxRunup()
                                ),
                                0.0F,
                                1.0F
                        );

                float markStrength =
                        Mth.clamp(
                                0.30F
                                        + coverage
                                        * 0.28F
                                        + farMark
                                        * 0.42F,
                                0.0F,
                                1.0F
                        );

                long life =
                        90L
                                + Math.round(
                                markStrength
                                        * 170.0F
                        )
                                + Math.round(
                                patch.profile()
                                        .rain()
                                        * 120.0F
                        );

                WET_MARKS.put(
                        key,
                        new WetMark(
                                time + life,
                                markStrength
                        )
                );

                int foam =
                        Mth.clamp(
                                Math.round(
                                        sample.breaking()
                                                * 82.0F
                                ),
                                0,
                                82
                        );

                int red =
                        Mth.clamp(
                                patch.red()
                                        + foam,
                                0,
                                255
                        );

                int green =
                        Mth.clamp(
                                patch.green()
                                        + foam,
                                0,
                                255
                        );

                int blue =
                        Mth.clamp(
                                patch.blue()
                                        + foam,
                                0,
                                255
                        );

                int alpha =
                        Mth.clamp(
                                Math.round(
                                        24.0F
                                                + coverage
                                                * 42.0F
                                                + sample.breaking()
                                                * 38.0F
                                ),
                                20,
                                96
                        );

                float waterY =
                        (float) (
                                patch.surfaceY()
                                        + 0.018
                                        + Math.max(
                                        0.0,
                                        sample.height()
                                                * 0.035
                                )
                        );

                emitQuad(
                        buffer,
                        pose,
                        patch.x(),
                        waterY,
                        patch.z(),
                        red,
                        green,
                        blue,
                        alpha
                );

                any =
                        true;
            } else {
                WetMark mark =
                        WET_MARKS.get(
                                key
                        );

                if (mark == null
                        || mark.expiresAt()
                        <= time) {
                    continue;
                }

                float fade =
                        Mth.clamp(
                                (
                                        mark.expiresAt()
                                                - time
                                ) / 220.0F,
                                0.0F,
                                1.0F
                        );

                int alpha =
                        Mth.clamp(
                                Math.round(
                                        12.0F
                                                + mark.strength()
                                                * 42.0F
                                                * fade
                                ),
                                8,
                                54
                        );

                /*
                 * A translucent charcoal-blue film darkens sand without
                 * replacing the block. Farther exceptional run-up leaves the
                 * strongest/longest temporary trace.
                 */
                emitQuad(
                        buffer,
                        pose,
                        patch.x(),
                        (float) patch.surfaceY()
                                + 0.010F,
                        patch.z(),
                        28,
                        42,
                        46,
                        alpha
                );

                any =
                        true;
            }
        }

        if (any) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(
                    false
            );
            RenderSystem.disableCull();
            RenderSystem.setShader(
                    GameRenderer::getPositionColorShader
            );

            BufferUploader.drawWithShader(
                    buffer.buildOrThrow()
            );

            RenderSystem.enableCull();
            RenderSystem.depthMask(
                    true
            );
            RenderSystem.disableBlend();
        }

        pose.popPose();
    }

    private static void rebuild(
            Minecraft minecraft,
            int centerX,
            int centerZ,
            long time
    ) {
        PATCHES.clear();

        int radius =
                Math.max(
                        24,
                        Math.min(
                                48,
                                minecraft.options.renderDistance()
                                        .get()
                                        * 4
                        )
                );

        int radiusSquared =
                radius
                        * radius;

        Map<Long, ShorePatch> unique =
                new LinkedHashMap<>();

        BlockPos.MutableBlockPos mutable =
                new BlockPos.MutableBlockPos();

        for (int x = centerX - radius;
             x <= centerX + radius;
             x += 2) {

            int dx =
                    x - centerX;

            for (int z = centerZ - radius;
                 z <= centerZ + radius;
                 z += 2) {

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

                if (water == null) {
                    continue;
                }

                OceanWaveField.Profile profile =
                        OceanWaveField.profile(
                                minecraft.level,
                                water,
                                time
                        );

                if (profile.shore()
                        < 0.18F
                        || profile.maxRunup()
                        < 1.0F) {
                    continue;
                }

                double shoreX =
                        profile.shoreX();

                double shoreZ =
                        profile.shoreZ();

                if (shoreX * shoreX
                        + shoreZ * shoreZ
                        < 0.08) {
                    continue;
                }

                int waterColor =
                        minecraft.level.getBiome(
                                water
                        ).value()
                                .getWaterColor();

                int maxStep =
                        Mth.clamp(
                                Mth.ceil(
                                        profile.maxRunup()
                                                + 1.0F
                                ),
                                2,
                                8
                        );

                for (int step = 1;
                     step <= maxStep;
                     step++) {

                    int landX =
                            Mth.floor(
                                    water.getX()
                                            + 0.5
                                            + shoreX
                                            * step
                            );

                    int landZ =
                            Mth.floor(
                                    water.getZ()
                                            + 0.5
                                            + shoreZ
                                            * step
                            );

                    int landY =
                            minecraft.level.getHeight(
                                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                    landX,
                                    landZ
                            )
                                    - 1;

                    BlockPos land =
                            new BlockPos(
                                    landX,
                                    landY,
                                    landZ
                            );

                    BlockState state =
                            minecraft.level.getBlockState(
                                    land
                            );

                    if (state.getFluidState()
                            .is(
                                    FluidTags.WATER
                            )) {
                        continue;
                    }

                    float distance =
                            (float) Math.sqrt(
                                    (
                                            landX
                                                    - water.getX()
                                    ) * (
                                            landX
                                                    - water.getX()
                                    )
                                            + (
                                            landZ
                                                    - water.getZ()
                                    ) * (
                                            landZ
                                                    - water.getZ()
                                    )
                            );

                    /*
                     * Run-up is allowed over ANY solid surface whose top sits
                     * at the current shoreline level. Sand gets no privileged
                     * treatment anymore. A higher top becomes an impact wall.
                     */
                    boolean wall =
                            landY
                                    > water.getY();

                    if (landY
                            < water.getY() - 1) {
                        continue;
                    }

                    long key =
                            BlockPos.asLong(
                                    landX,
                                    landY,
                                    landZ
                            );

                    ShorePatch candidate =
                            new ShorePatch(
                                    landX,
                                    landZ,
                                    landY,
                                    wall
                                            ? water.getY() + 1.015
                                            : landY + 1.0,
                                    distance,
                                    water.getX(),
                                    water.getZ(),
                                    waterColor >> 16
                                            & 255,
                                    waterColor >> 8
                                            & 255,
                                    waterColor
                                            & 255,
                                    profile,
                                    wall
                            );

                    ShorePatch previous =
                            unique.get(
                                    key
                            );

                    if (previous == null
                            || candidate.distance()
                            < previous.distance()) {
                        unique.put(
                                key,
                                candidate
                        );
                    }

                    if (wall) {
                        break;
                    }
                }
            }
        }

        PATCHES.addAll(
                unique.values()
        );

        cachedX =
                centerX;

        cachedZ =
                centerZ;

        cachedAt =
                time;
    }

    private static boolean needsRebuild(
            int x,
            int z,
            long time
    ) {
        if (cachedX == Integer.MIN_VALUE) {
            return true;
        }

        int dx =
                x - cachedX;

        int dz =
                z - cachedZ;

        return dx * dx
                + dz * dz
                >= 64
                || time - cachedAt
                >= 60L;
    }

    private static BlockPos findSurfaceWater(
            Level level,
            BlockPos.MutableBlockPos mutable,
            int x,
            int z,
            int surfaceY
    ) {
        for (int y = surfaceY;
             y >= surfaceY - 5;
             y--) {

            mutable.set(
                    x,
                    y,
                    z
            );

            if (!level.getFluidState(
                    mutable
            ).is(
                    FluidTags.WATER
            )) {
                continue;
            }

            if (!level.getFluidState(
                    mutable.above()
            ).is(
                    FluidTags.WATER
            )) {
                return mutable.immutable();
            }

            return null;
        }

        return null;
    }

    private static void impactWall(
            Minecraft minecraft,
            ShorePatch patch,
            OceanWaveField.Sample sample,
            long time
    ) {
        long key =
                BlockPos.asLong(
                        patch.x(),
                        patch.blockY(),
                        patch.z()
                );

        long last =
                WALL_SPLASH_AT.getOrDefault(
                        key,
                        Long.MIN_VALUE
                );

        if (time - last
                < 24L) {
            return;
        }

        WALL_SPLASH_AT.put(
                key,
                time
        );

        int count =
                10
                        + Math.round(
                        sample.breaking()
                                * 34.0F
                );

        double towardWaterX =
                -patch.profile()
                        .shoreX();

        double towardWaterZ =
                -patch.profile()
                        .shoreZ();

        for (int i = 0;
             i < count;
             i++) {
            minecraft.level.addParticle(
                    ParticleTypes.SPLASH,
                    patch.x() + 0.5
                            + (
                            minecraft.level.random.nextDouble()
                                    - 0.5
                    ) * 0.8,
                    patch.surfaceY()
                            + minecraft.level.random.nextDouble()
                            * (
                            0.4
                                    + sample.breaking()
                                    * 1.8
                    ),
                    patch.z() + 0.5
                            + (
                            minecraft.level.random.nextDouble()
                                    - 0.5
                    ) * 0.8,
                    towardWaterX
                            * (
                            0.05
                                    + minecraft.level.random.nextDouble()
                                    * 0.16
                    ),
                    0.12
                            + minecraft.level.random.nextDouble()
                            * (
                            0.16
                                    + sample.breaking()
                                    * 0.32
                    ),
                    towardWaterZ
                            * (
                            0.05
                                    + minecraft.level.random.nextDouble()
                                    * 0.16
                    )
            );
        }

        minecraft.level.playLocalSound(
                patch.x() + 0.5,
                patch.surfaceY(),
                patch.z() + 0.5,
                SoundEvents.GENERIC_SPLASH,
                SoundSource.AMBIENT,
                0.45F
                        + sample.breaking()
                        * 0.85F,
                0.82F
                        + minecraft.level.random.nextFloat()
                        * 0.16F,
                false
        );
    }

    private static void emitQuad(
            BufferBuilder buffer,
            PoseStack pose,
            int x,
            float y,
            int z,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        var matrix =
                pose.last()
                        .pose();

        buffer.addVertex(
                        matrix,
                        x,
                        y,
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
                        y,
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
                        y,
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
                        y,
                        z + 1
                )
                .setColor(
                        red,
                        green,
                        blue,
                        alpha
                );
    }

    private static void clear() {
        PATCHES.clear();
        WET_MARKS.clear();
        WALL_SPLASH_AT.clear();
        cachedX =
                Integer.MIN_VALUE;
        cachedZ =
                Integer.MIN_VALUE;
        cachedAt =
                Long.MIN_VALUE;
    }

    private record ShorePatch(
            int x,
            int z,
            int blockY,
            double surfaceY,
            float distance,
            int waterX,
            int waterZ,
            int red,
            int green,
            int blue,
            OceanWaveField.Profile profile,
            boolean wall
    ) {
    }

    private record WetMark(
            long expiresAt,
            float strength
    ) {
    }
}
