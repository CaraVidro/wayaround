package net.caravidro.wayaround.client.debris;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;

import net.caravidro.wayaround.WayAround;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Lightweight visual debris.
 *
 * These are deliberately not Entities: no collision, no network identity,
 * no ticking chunks and no physics queries per fragment. One client owns a
 * capped list, ticks it once and renders every cube through one vertex buffer.
 */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class DebrisSystem {

    private DebrisSystem() {
    }

    private static final int MAX_DEBRIS =
            260;

    private static final int FUGA_SMALL_COUNT =
            76;

    private static final int FUGA_GIANT_COUNT =
            10;

    private static final double RENDER_DISTANCE_SQR =
            1400.0
                    * 1400.0;

    private static final List<Debris>
            ACTIVE =
            new ArrayList<>();

    private static final Random RANDOM =
            new Random();

    public static void spawnFugaBurst(
            Vec3 center
    ) {
        trimFor(
                FUGA_SMALL_COUNT
                        + FUGA_GIANT_COUNT
        );

        for (int i = 0;
             i < FUGA_SMALL_COUNT;
             i++) {

            double angle =
                    RANDOM.nextDouble()
                            * Math.PI
                            * 2.0;

            double horizontal =
                    0.20
                            + RANDOM.nextDouble()
                                    * 1.15;

            Vec3 velocity =
                    new Vec3(
                            Math.cos(angle)
                                    * horizontal,
                            0.72
                                    + RANDOM.nextDouble()
                                            * 2.45,
                            Math.sin(angle)
                                    * horizontal
                    );

            Vec3 position =
                    center.add(
                            (
                                    RANDOM.nextDouble()
                                            - 0.5
                            )
                                    * 13.0,
                            RANDOM.nextDouble()
                                    * 3.2,
                            (
                                    RANDOM.nextDouble()
                                            - 0.5
                            )
                                    * 13.0
                    );

            double size =
                    0.24
                            + RANDOM.nextDouble()
                                    * 1.15;

            ACTIVE.add(
                    new Debris(
                            position,
                            velocity,
                            size,
                            46
                                    + RANDOM.nextInt(
                                    42
                            ),
                            0.075
                                    + RANDOM.nextDouble()
                                            * 0.025,
                            false,
                            RANDOM.nextLong()
                    )
            );
        }

        /*
         * Giant fragments are intentionally ridiculous: they reach hundreds
         * of blocks high, keep gaining horizontal speed and can cross chunks
         * before falling back to the heightmap.
         */
        for (int i = 0;
             i < FUGA_GIANT_COUNT;
             i++) {

            double angle =
                    RANDOM.nextDouble()
                            * Math.PI
                            * 2.0;

            double horizontal =
                    0.72
                            + RANDOM.nextDouble()
                                    * 1.55;

            Vec3 velocity =
                    new Vec3(
                            Math.cos(angle)
                                    * horizontal,
                            4.0
                                    + RANDOM.nextDouble()
                                            * 2.75,
                            Math.sin(angle)
                                    * horizontal
                    );

            Vec3 position =
                    center.add(
                            (
                                    RANDOM.nextDouble()
                                            - 0.5
                            )
                                    * 15.0,
                            1.2
                                    + RANDOM.nextDouble()
                                            * 5.0,
                            (
                                    RANDOM.nextDouble()
                                            - 0.5
                            )
                                    * 15.0
                    );

            ACTIVE.add(
                    new Debris(
                            position,
                            velocity,
                            2.4
                                    + RANDOM.nextDouble()
                                            * 4.2,
                            220
                                    + RANDOM.nextInt(
                                    120
                            ),
                            0.050
                                    + RANDOM.nextDouble()
                                            * 0.020,
                            true,
                            RANDOM.nextLong()
                    )
            );
        }
    }

    private static void trimFor(
            int incoming
    ) {
        int overflow =
                ACTIVE.size()
                        + incoming
                        - MAX_DEBRIS;

        if (overflow <= 0) {
            return;
        }

        int removed =
                0;

        Iterator<Debris> iterator =
                ACTIVE.iterator();

        while (iterator.hasNext()
                && removed < overflow) {

            Debris debris =
                    iterator.next();

            if (!debris.giant) {
                iterator.remove();
                removed++;
            }
        }

        while (ACTIVE.size()
                + incoming
                > MAX_DEBRIS
                && !ACTIVE.isEmpty()) {

            ACTIVE.remove(
                    0
            );
        }
    }

    @SubscribeEvent
    public static void tick(
            ClientTickEvent.Post event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            ACTIVE.clear();
            return;
        }

        Iterator<Debris> iterator =
                ACTIVE.iterator();

        while (iterator.hasNext()) {
            Debris debris =
                    iterator.next();

            debris.previous =
                    debris.position;

            debris.age++;

            if (debris.age
                    >= debris.maxAge) {
                iterator.remove();
                continue;
            }

            if (debris.giant) {
                debris.velocity =
                        new Vec3(
                                debris.velocity.x
                                        * (
                                        debris.age < 95
                                                ? 1.012
                                                : 0.997
                                ),
                                debris.velocity.y
                                        - debris.gravity,
                                debris.velocity.z
                                        * (
                                        debris.age < 95
                                                ? 1.012
                                                : 0.997
                                )
                        );

            } else {
                debris.velocity =
                        debris.velocity
                                .add(
                                        0.0,
                                        -debris.gravity,
                                        0.0
                                )
                                .scale(
                                        0.992
                                );
            }

            debris.position =
                    debris.position.add(
                            debris.velocity
                    );

            debris.rotX +=
                    6.0F
                            + (
                            float
                    ) (
                            debris.size
                                    * 0.55
                    );

            debris.rotY +=
                    8.0F
                            + (
                            debris.seed
                                    & 7L
                    );

            debris.rotZ +=
                    4.0F
                            + (
                            (
                                    debris.seed
                                            >>> 4
                            )
                                    & 5L
                    );

            if (debris.giant) {
                emitTrail(
                        minecraft,
                        debris
                );

                if (debris.age > 24
                        && debris.velocity.y < 0.0
                        && touchedSurface(
                        minecraft,
                        debris
                )) {

                    emitImpactSmoke(
                            minecraft,
                            debris
                    );

                    iterator.remove();
                }
            }
        }
    }

    private static boolean touchedSurface(
            Minecraft minecraft,
            Debris debris
    ) {
        int x =
                Mth.floor(
                        debris.position.x
                );

        int z =
                Mth.floor(
                        debris.position.z
                );

        int surface =
                minecraft.level
                        .getHeight(
                                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                x,
                                z
                        );

        return debris.position.y
                - debris.size
                        * 0.5
                <= surface
                + 0.45;
    }

    private static void emitTrail(
            Minecraft minecraft,
            Debris debris
    ) {
        int count =
                Mth.clamp(
                        Mth.ceil(
                                debris.size
                                        * 0.70
                        ),
                        2,
                        5
                );

        Vec3 backwards =
                debris.velocity.lengthSqr()
                        < 0.0001
                        ? Vec3.ZERO
                        : debris.velocity
                                .normalize()
                                .scale(
                                        -debris.size
                                                * 0.52
                                );

        for (int i = 0;
             i < count;
             i++) {

            double spread =
                    debris.size
                            * 0.14;

            minecraft.level
                    .addParticle(
                            i == 0
                                    ? ParticleTypes.LARGE_SMOKE
                                    : ParticleTypes.SMOKE,
                            debris.position.x
                                    + backwards.x
                                    + (
                                    RANDOM.nextDouble()
                                            - 0.5
                            )
                                    * spread,
                            debris.position.y
                                    + backwards.y
                                    + (
                                    RANDOM.nextDouble()
                                            - 0.5
                            )
                                    * spread,
                            debris.position.z
                                    + backwards.z
                                    + (
                                    RANDOM.nextDouble()
                                            - 0.5
                            )
                                    * spread,
                            -debris.velocity.x
                                    * 0.035,
                            0.035,
                            -debris.velocity.z
                                    * 0.035
                    );
        }
    }

    private static void emitImpactSmoke(
            Minecraft minecraft,
            Debris debris
    ) {
        int count =
                Mth.clamp(
                        Mth.ceil(
                                debris.size
                                        * 12.0
                        ),
                        20,
                        82
                );

        for (int i = 0;
             i < count;
             i++) {

            double angle =
                    RANDOM.nextDouble()
                            * Math.PI
                            * 2.0;

            double speed =
                    0.08
                            + RANDOM.nextDouble()
                                    * (
                                    0.10
                                            + debris.size
                                                    * 0.055
                            );

            minecraft.level
                    .addParticle(
                            i % 4 == 0
                                    ? ParticleTypes.LARGE_SMOKE
                                    : ParticleTypes.POOF,
                            debris.position.x,
                            debris.position.y,
                            debris.position.z,
                            Math.cos(angle)
                                    * speed,
                            0.08
                                    + RANDOM.nextDouble()
                                            * 0.28,
                            Math.sin(angle)
                                    * speed
                    );
        }
    }

    @SubscribeEvent
    public static void render(
            RenderLevelStageEvent event
    ) {
        if (event.getStage()
                != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || ACTIVE.isEmpty()) {

            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        Vec3 camera =
                event.getCamera()
                        .getPosition();

        PoseStack pose =
                event.getPoseStack();

        float partialTick =
                event.getPartialTick()
                        .getGameTimeDeltaPartialTick(
                                true
                        );

        BufferBuilder buffer =
                Tesselator.getInstance()
                        .begin(
                                VertexFormat.Mode.TRIANGLES,
                                DefaultVertexFormat.POSITION_COLOR
                        );

        boolean emitted =
                false;

        for (Debris debris :
                ACTIVE) {

            Vec3 position =
                    debris.previous.lerp(
                            debris.position,
                            partialTick
                    );

            if (position.distanceToSqr(
                    camera
            )
                    > RENDER_DISTANCE_SQR) {
                continue;
            }

            pose.pushPose();

            pose.translate(
                    position.x
                            - camera.x,
                    position.y
                            - camera.y,
                    position.z
                            - camera.z
            );

            pose.mulPose(
                    Axis.XP.rotationDegrees(
                            debris.rotX
                                    + partialTick
                                            * 6.0F
                    )
            );

            pose.mulPose(
                    Axis.YP.rotationDegrees(
                            debris.rotY
                                    + partialTick
                                            * 8.0F
                    )
            );

            pose.mulPose(
                    Axis.ZP.rotationDegrees(
                            debris.rotZ
                                    + partialTick
                                            * 4.0F
                    )
            );

            float half =
                    (
                            float
                    ) debris.size
                            * 0.5F;

            int shade =
                    debris.giant
                            ? 104
                            + (
                            int
                    ) (
                            Math.abs(
                                    debris.seed
                            )
                                    % 52L
                    )
                            : 118
                            + (
                            int
                    ) (
                            Math.abs(
                                    debris.seed
                            )
                                    % 70L
                    );

            cube(
                    buffer,
                    pose.last()
                            .pose(),
                    half,
                    shade,
                    Mth.clamp(
                            shade - 18,
                            0,
                            255
                    ),
                    Mth.clamp(
                            shade - 36,
                            0,
                            255
                    ),
                    255
            );

            pose.popPose();

            emitted =
                    true;
        }

        if (!emitted) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(
                GameRenderer::getPositionColorShader
        );

        BufferUploader.drawWithShader(
                buffer.buildOrThrow()
        );

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void cube(
            BufferBuilder buffer,
            Matrix4f matrix,
            float h,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        face(buffer, matrix,
                -h, -h, -h,
                 h, -h, -h,
                 h,  h, -h,
                -h,  h, -h,
                red, green, blue, alpha);

        face(buffer, matrix,
                 h, -h,  h,
                -h, -h,  h,
                -h,  h,  h,
                 h,  h,  h,
                red, green, blue, alpha);

        face(buffer, matrix,
                -h, -h,  h,
                -h, -h, -h,
                -h,  h, -h,
                -h,  h,  h,
                red, green, blue, alpha);

        face(buffer, matrix,
                 h, -h, -h,
                 h, -h,  h,
                 h,  h,  h,
                 h,  h, -h,
                red, green, blue, alpha);

        face(buffer, matrix,
                -h,  h, -h,
                 h,  h, -h,
                 h,  h,  h,
                -h,  h,  h,
                red, green, blue, alpha);

        face(buffer, matrix,
                -h, -h,  h,
                 h, -h,  h,
                 h, -h, -h,
                -h, -h, -h,
                red, green, blue, alpha);
    }

    private static void face(
            BufferBuilder buffer,
            Matrix4f matrix,
            float ax,
            float ay,
            float az,
            float bx,
            float by,
            float bz,
            float cx,
            float cy,
            float cz,
            float dx,
            float dy,
            float dz,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        vertex(buffer, matrix, ax, ay, az, red, green, blue, alpha);
        vertex(buffer, matrix, bx, by, bz, red, green, blue, alpha);
        vertex(buffer, matrix, cx, cy, cz, red, green, blue, alpha);

        vertex(buffer, matrix, ax, ay, az, red, green, blue, alpha);
        vertex(buffer, matrix, cx, cy, cz, red, green, blue, alpha);
        vertex(buffer, matrix, dx, dy, dz, red, green, blue, alpha);
    }

    private static void vertex(
            BufferBuilder buffer,
            Matrix4f matrix,
            float x,
            float y,
            float z,
            int red,
            int green,
            int blue,
            int alpha
    ) {
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
    }

    private static final class Debris {

        private Vec3 previous;
        private Vec3 position;
        private Vec3 velocity;

        private final double size;
        private final int maxAge;
        private final double gravity;
        private final boolean giant;
        private final long seed;

        private int age;

        private float rotX;
        private float rotY;
        private float rotZ;

        private Debris(
                Vec3 position,
                Vec3 velocity,
                double size,
                int maxAge,
                double gravity,
                boolean giant,
                long seed
        ) {
            this.previous =
                    position;

            this.position =
                    position;

            this.velocity =
                    velocity;

            this.size =
                    size;

            this.maxAge =
                    maxAge;

            this.gravity =
                    gravity;

            this.giant =
                    giant;

            this.seed =
                    seed;

            this.rotX =
                    (
                            float
                    ) (
                            seed
                                    & 255L
                    );

            this.rotY =
                    (
                            float
                    ) (
                            (
                                    seed
                                            >>> 8
                            )
                                    & 255L
                    );

            this.rotZ =
                    (
                            float
                    ) (
                            (
                                    seed
                                            >>> 16
                            )
                                    & 255L
                    );
        }
    }
}
