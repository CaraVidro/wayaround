package net.caravidro.wayaround.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.caravidro.wayaround.WayAround;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * World-space Void Domain renderer.
 *
 * The interior is intentionally not a flat sky texture. It is a deterministic
 * 3D cosmic scene built around the pocket:
 *
 * - black enclosing shell;
 * - parallax star field;
 * - a suspended black rectangular singularity with a warm luminous outline;
 * - broken white/blue accretion ribbons;
 * - dark Minecraft-like debris cubes orbiting the singularity;
 * - distant pale cubic matter clouds.
 *
 * Normal world blocks/entities keep their depth and can occlude the effect.
 */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class VoidDomainWorldRenderer {

    private VoidDomainWorldRenderer() {
    }

    private static final int EXTERIOR_LATITUDE_SEGMENTS = 9;
    private static final int EXTERIOR_LONGITUDE_SEGMENTS = 18;

    private static final int INTERIOR_LATITUDE_SEGMENTS = 12;
    private static final int INTERIOR_LONGITUDE_SEGMENTS = 24;

    private static final int STAR_COUNT = 420;
    private static final int DARK_DEBRIS_COUNT = 82;
    private static final int PALE_FRAGMENT_COUNT = 58;

    private static final double INTERIOR_SHELL_RADIUS = 96.0;
    private static final double STAR_MIN_RADIUS = 54.0;
    private static final double STAR_MAX_RADIUS = 91.0;
    private static final double NEAR_GUARD = 0.35;

    private static final Vec3 SINGULARITY =
            new Vec3(
                    0.0,
                    24.0,
                    0.0
            );

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
                || minecraft.player == null) {
            return;
        }

        Vec3 camera =
                event.getCamera()
                        .getPosition();

        Vector3f look =
                event.getCamera()
                        .getLookVector();

        PoseStack pose =
                event.getPoseStack();

        VoidDomainClientEffects.VisualDomain interior =
                VoidDomainClientEffects.localInterior();

        if (interior != null) {
            renderInterior(
                    minecraft,
                    pose,
                    camera,
                    look,
                    interior
            );
            return;
        }

        renderExterior(
                event,
                pose,
                camera,
                look
        );
    }

    private static void renderInterior(
            Minecraft minecraft,
            PoseStack pose,
            Vec3 camera,
            Vector3f look,
            VoidDomainClientEffects.VisualDomain domain
    ) {
        /*
         * IMPORTANT:
         *
         * Tesselator owns one shared backing ByteBuffer. Starting an opaque
         * BufferBuilder and then starting a second glow BufferBuilder before
         * the first one is built can make both builders write into the same
         * storage. The domain is one of the first effects large enough to turn
         * that corruption into a hard client/GPU stall instead of a harmless
         * visual glitch.
         *
         * Build + upload each pass completely before beginning the next pass.
         */
        long seed =
                domain.owner()
                        .getMostSignificantBits()
                        ^ domain.owner()
                        .getLeastSignificantBits();

        double time =
                minecraft.level == null
                        ? 0.0
                        : minecraft.level.getGameTime()
                                * 0.0125;

        BufferBuilder opaque =
                Tesselator.getInstance()
                        .begin(
                                VertexFormat.Mode.TRIANGLES,
                                DefaultVertexFormat.POSITION_COLOR
                        );

        pose.pushPose();

        pose.translate(
                domain.center().x - camera.x,
                domain.center().y - camera.y,
                domain.center().z - camera.z
        );

        Matrix4f opaqueMatrix =
                pose.last()
                        .pose();

        renderShell(
                opaque,
                opaqueMatrix,
                domain,
                camera,
                look
        );

        renderStars(
                opaque,
                opaqueMatrix,
                domain,
                camera,
                look,
                seed
        );

        renderSingularityOpaque(
                opaque,
                opaqueMatrix
        );

        renderDarkDebris(
                opaque,
                opaqueMatrix,
                seed,
                time
        );

        pose.popPose();

        drawOpaque(
                opaque
        );

        BufferBuilder glow =
                Tesselator.getInstance()
                        .begin(
                                VertexFormat.Mode.TRIANGLES,
                                DefaultVertexFormat.POSITION_COLOR
                        );

        pose.pushPose();

        pose.translate(
                domain.center().x - camera.x,
                domain.center().y - camera.y,
                domain.center().z - camera.z
        );

        Matrix4f glowMatrix =
                pose.last()
                        .pose();

        renderSingularityGlow(
                glow,
                glowMatrix
        );

        renderAccretion(
                glow,
                glowMatrix,
                seed,
                time
        );

        renderPaleFragments(
                glow,
                glowMatrix,
                seed,
                time
        );

        pose.popPose();

        drawGlow(
                glow
        );
    }

    private static void renderShell(
            BufferBuilder buffer,
            Matrix4f matrix,
            VoidDomainClientEffects.VisualDomain domain,
            Vec3 camera,
            Vector3f look
    ) {
        for (int lat = 0;
             lat < INTERIOR_LATITUDE_SEGMENTS;
             lat++) {

            double phi0 =
                    -Math.PI * 0.5
                            + Math.PI
                            * lat
                            / INTERIOR_LATITUDE_SEGMENTS;

            double phi1 =
                    -Math.PI * 0.5
                            + Math.PI
                            * (lat + 1)
                            / INTERIOR_LATITUDE_SEGMENTS;

            for (int lon = 0;
                 lon < INTERIOR_LONGITUDE_SEGMENTS;
                 lon++) {

                double theta0 =
                        Math.PI * 2.0
                                * lon
                                / INTERIOR_LONGITUDE_SEGMENTS;

                double theta1 =
                        Math.PI * 2.0
                                * (lon + 1)
                                / INTERIOR_LONGITUDE_SEGMENTS;

                Vec3 a =
                        point(
                                INTERIOR_SHELL_RADIUS,
                                phi0,
                                theta0
                        );

                Vec3 b =
                        point(
                                INTERIOR_SHELL_RADIUS,
                                phi1,
                                theta0
                        );

                Vec3 c =
                        point(
                                INTERIOR_SHELL_RADIUS,
                                phi1,
                                theta1
                        );

                Vec3 d =
                        point(
                                INTERIOR_SHELL_RADIUS,
                                phi0,
                                theta1
                        );

                solidTriangle(
                        buffer,
                        matrix,
                        domain.center(),
                        camera,
                        look,
                        a,
                        b,
                        c,
                        0,
                        0,
                        2,
                        255
                );

                solidTriangle(
                        buffer,
                        matrix,
                        domain.center(),
                        camera,
                        look,
                        a,
                        c,
                        d,
                        0,
                        0,
                        4,
                        255
                );
            }
        }
    }

    private static void renderStars(
            BufferBuilder buffer,
            Matrix4f matrix,
            VoidDomainClientEffects.VisualDomain domain,
            Vec3 camera,
            Vector3f look,
            long seed
    ) {
        for (int index = 0;
             index < STAR_COUNT;
             index++) {

            long h =
                    mix(
                            seed
                                    + index
                                    * 0x9E3779B97F4A7C15L
                    );

            double u =
                    (h >>> 11)
                            * 0x1.0p-53;

            long h2 =
                    mix(
                            h
                                    ^ 0xD1B54A32D192ED03L
                    );

            double v =
                    (h2 >>> 11)
                            * 0x1.0p-53;

            long h3 =
                    mix(
                            h2
                                    ^ 0x94D049BB133111EBL
                    );

            double w =
                    (h3 >>> 11)
                            * 0x1.0p-53;

            double y =
                    1.0
                            - 2.0 * u;

            double radial =
                    Math.sqrt(
                            Math.max(
                                    0.0,
                                    1.0 - y * y
                            )
                    );

            double theta =
                    Math.PI
                            * 2.0
                            * v;

            double radius =
                    STAR_MIN_RADIUS
                            + (STAR_MAX_RADIUS - STAR_MIN_RADIUS)
                            * w;

            Vec3 star =
                    new Vec3(
                            Math.cos(theta)
                                    * radial
                                    * radius,
                            y * radius,
                            Math.sin(theta)
                                    * radial
                                    * radius
                    );

            if (!inFront(
                    domain.center()
                            .add(
                                    star
                            ),
                    camera,
                    look
            )) {
                continue;
            }

            float size =
                    (h3 & 31L) == 0L
                            ? 0.27F
                            : (h3 & 7L) == 0L
                            ? 0.15F
                            : 0.072F;

            int brightness =
                    (h3 & 15L) == 0L
                            ? 205
                            : 255;

            starOctahedron(
                    buffer,
                    matrix,
                    star,
                    size,
                    brightness
            );
        }
    }

    private static void renderSingularityOpaque(
            BufferBuilder opaque,
            Matrix4f matrix
    ) {
        /*
         * Reference language: a literal Minecraft-like black rectangular void,
         * outlined by warm light rather than a smooth circular black hole.
         */
        box(
                opaque,
                matrix,
                SINGULARITY,
                9.2,
                12.2,
                8.4,
                0,
                0,
                0,
                255
        );
    }

    private static void renderSingularityGlow(
            BufferBuilder glow,
            Matrix4f matrix
    ) {
        boxEdges(
                glow,
                matrix,
                SINGULARITY,
                9.45,
                12.45,
                8.65,
                0.19,
                255,
                238,
                155,
                230
        );

        boxEdges(
                glow,
                matrix,
                SINGULARITY,
                9.72,
                12.72,
                8.92,
                0.10,
                255,
                250,
                205,
                100
        );
    }

    private static void renderAccretion(
            BufferBuilder glow,
            Matrix4f matrix,
            long seed,
            double time
    ) {
        double[] radii = {
                7.5,
                10.4,
                13.6,
                17.2,
                21.5,
                26.0,
                31.0
        };

        for (int ring = 0;
             ring < radii.length;
             ring++) {

            double radius =
                    radii[ring];

            double width =
                    ring < 2
                            ? 2.3
                            : 1.4
                            + (ring % 3) * 0.55;

            int segments =
                    84;

            double phase =
                    time
                            * (0.45
                            + ring * 0.07)
                            + ring * 0.71;

            for (int segment = 0;
                 segment < segments;
                 segment++) {

                long h =
                        mix(
                                seed
                                        + ring
                                        * 0x632BE59BD9B4E019L
                                        + segment
                                        * 0x9E3779B97F4A7C15L
                        );

                /*
                 * Broken, blocky bands. Missing sections keep the accretion
                 * field from turning into a clean sci-fi torus.
                 */
                if ((h & 15L) == 0L
                        || (h & 63L) == 1L) {
                    continue;
                }

                double a0 =
                        phase
                                + Math.PI * 2.0
                                * segment
                                / segments;

                double a1 =
                        phase
                                + Math.PI * 2.0
                                * (segment + 1)
                                / segments;

                double pulse =
                        0.78
                                + 0.22
                                * Math.sin(
                                a0 * 5.0
                                        + ring
                                * 1.9
                        );

                double inner =
                        radius
                                - width
                                * 0.5
                                * pulse;

                double outer =
                        radius
                                + width
                                * 0.5
                                * pulse;

                Vec3 p0 =
                        accretionPoint(
                                inner,
                                a0
                        );

                Vec3 p1 =
                        accretionPoint(
                                outer,
                                a0
                        );

                Vec3 p2 =
                        accretionPoint(
                                outer,
                                a1
                        );

                Vec3 p3 =
                        accretionPoint(
                                inner,
                                a1
                        );

                int brightness =
                        ring <= 1
                                ? 255
                                : 205
                                + (int) (h & 35L);

                int red =
                        ring <= 1
                                ? 255
                                : Math.min(
                                255,
                                brightness + 16
                        );

                int green =
                        ring <= 1
                                ? 246
                                : brightness;

                int blue =
                        ring <= 1
                                ? 202
                                : Math.min(
                                255,
                                brightness + 38
                        );

                int alpha =
                        ring <= 1
                                ? 205
                                : 85
                                + (int) (h & 63L);

                quad(
                        glow,
                        matrix,
                        p0,
                        p1,
                        p2,
                        p3,
                        red,
                        green,
                        blue,
                        alpha
                );
            }
        }
    }

    private static void renderDarkDebris(
            BufferBuilder buffer,
            Matrix4f matrix,
            long seed,
            double time
    ) {
        for (int index = 0;
             index < DARK_DEBRIS_COUNT;
             index++) {

            long h =
                    mix(
                            seed
                                    ^ index
                                    * 0xD1342543DE82EF95L
                    );

            long h2 =
                    mix(
                            h
                                    ^ 0xC6BC279692B5CC83L
                    );

            double radius =
                    11.0
                            + ((h >>> 12) & 0xFFFFL)
                            / 65535.0
                            * 37.0;

            double angle =
                    ((h2 >>> 10) & 0xFFFFL)
                            / 65535.0
                            * Math.PI
                            * 2.0
                            + time
                            * (0.08
                            + (index % 7) * 0.006);

            Vec3 planar =
                    accretionPoint(
                            radius,
                            angle
                    );

            double vertical =
                    (((h >>> 38) & 0x3FFL)
                            / 1023.0
                            - 0.5)
                            * 17.0;

            Vec3 center =
                    planar.add(
                            0.0,
                            vertical,
                            0.0
                    );

            double size =
                    0.45
                            + ((h2 >>> 34) & 0xFFL)
                            / 255.0
                            * 2.35;

            int shade =
                    1
                            + (int) ((h >>> 5) & 7L);

            box(
                    buffer,
                    matrix,
                    center,
                    size,
                    size
                            * (0.65
                            + ((h >>> 25) & 7L)
                            * 0.07),
                    size
                            * (0.72
                            + ((h >>> 31) & 7L)
                            * 0.06),
                    shade,
                    shade + 1,
                    shade + 5,
                    255
            );
        }
    }

    private static void renderPaleFragments(
            BufferBuilder glow,
            Matrix4f matrix,
            long seed,
            double time
    ) {
        for (int index = 0;
             index < PALE_FRAGMENT_COUNT;
             index++) {

            long h =
                    mix(
                            seed
                                    + index
                                    * 0xA24BAED4963EE407L
                    );

            double radius =
                    30.0
                            + ((h >>> 13) & 0xFFFFL)
                            / 65535.0
                            * 29.0;

            double angle =
                    ((h >>> 32) & 0xFFFFL)
                            / 65535.0
                            * Math.PI
                            * 2.0
                            - time * 0.028;

            Vec3 planar =
                    accretionPoint(
                            radius,
                            angle
                    );

            double vertical =
                    (((h >>> 49) & 0x1FFL)
                            / 511.0
                            - 0.5)
                            * 30.0;

            Vec3 center =
                    planar.add(
                            0.0,
                            vertical,
                            0.0
                    );

            double size =
                    0.8
                            + ((h >>> 7) & 0xFFL)
                            / 255.0
                            * 3.8;

            int warm =
                    205
                            + (int) ((h >>> 23) & 42L);

            box(
                    glow,
                    matrix,
                    center,
                    size,
                    size
                            * (0.45
                            + ((h >>> 18) & 7L)
                            * 0.08),
                    size
                            * (0.65
                            + ((h >>> 28) & 7L)
                            * 0.07),
                    Math.min(
                            255,
                            warm + 18
                    ),
                    Math.min(
                            255,
                            warm + 22
                    ),
                    255,
                    60
                            + (int) ((h >>> 40) & 95L)
            );
        }
    }

    private static Vec3 accretionPoint(
            double radius,
            double angle
    ) {
        double x =
                Math.cos(
                        angle
                ) * radius;

        double y =
                0.0;

        double z =
                Math.sin(
                        angle
                ) * radius;

        /*
         * Fixed skew produces the diagonal accretion plane from the visual
         * reference while keeping the object world-space and deterministic.
         */
        double tiltX =
                Math.toRadians(
                        27.0
                );

        double tiltZ =
                Math.toRadians(
                        -13.0
                );

        double y1 =
                y * Math.cos(
                        tiltX
                )
                        - z * Math.sin(
                        tiltX
                );

        double z1 =
                y * Math.sin(
                        tiltX
                )
                        + z * Math.cos(
                        tiltX
                );

        double x2 =
                x * Math.cos(
                        tiltZ
                )
                        - y1 * Math.sin(
                        tiltZ
                );

        double y2 =
                x * Math.sin(
                        tiltZ
                )
                        + y1 * Math.cos(
                        tiltZ
                );

        return SINGULARITY.add(
                x2,
                y2,
                z1
        );
    }

    private static void renderExterior(
            RenderLevelStageEvent event,
            PoseStack pose,
            Vec3 camera,
            Vector3f look
    ) {
        var domains =
                VoidDomainClientEffects.visuals();

        if (domains.isEmpty()) {
            return;
        }

        BufferBuilder buffer =
                Tesselator.getInstance()
                        .begin(
                                VertexFormat.Mode.TRIANGLES,
                                DefaultVertexFormat.POSITION_COLOR
                        );

        boolean any =
                false;

        for (VoidDomainClientEffects.VisualDomain domain :
                domains) {

            float radius =
                    domain.radius();

            AABB bounds =
                    new AABB(
                            domain.center().x - radius,
                            domain.center().y - radius,
                            domain.center().z - radius,
                            domain.center().x + radius,
                            domain.center().y + radius,
                            domain.center().z + radius
                    );

            if (!event.getFrustum()
                    .isVisible(
                            bounds
                    )) {
                continue;
            }

            pose.pushPose();

            pose.translate(
                    domain.center().x - camera.x,
                    domain.center().y - camera.y,
                    domain.center().z - camera.z
            );

            Matrix4f matrix =
                    pose.last()
                            .pose();

            for (int lat = 0;
                 lat < EXTERIOR_LATITUDE_SEGMENTS;
                 lat++) {

                double phi0 =
                        -Math.PI * 0.5
                                + Math.PI
                                * lat
                                / EXTERIOR_LATITUDE_SEGMENTS;

                double phi1 =
                        -Math.PI * 0.5
                                + Math.PI
                                * (lat + 1)
                                / EXTERIOR_LATITUDE_SEGMENTS;

                for (int lon = 0;
                     lon < EXTERIOR_LONGITUDE_SEGMENTS;
                     lon++) {

                    double theta0 =
                            Math.PI * 2.0
                                    * lon
                                    / EXTERIOR_LONGITUDE_SEGMENTS;

                    double theta1 =
                            Math.PI * 2.0
                                    * (lon + 1)
                                    / EXTERIOR_LONGITUDE_SEGMENTS;

                    Vec3 a =
                            point(
                                    radius,
                                    phi0,
                                    theta0
                            );

                    Vec3 b =
                            point(
                                    radius,
                                    phi1,
                                    theta0
                            );

                    Vec3 c =
                            point(
                                    radius,
                                    phi1,
                                    theta1
                            );

                    Vec3 d =
                            point(
                                    radius,
                                    phi0,
                                    theta1
                            );

                    int alpha =
                            (lat + lon) % 2 == 0
                                    ? 84
                                    : 112;

                    any |= solidTriangle(
                            buffer,
                            matrix,
                            domain.center(),
                            camera,
                            look,
                            a,
                            b,
                            c,
                            255,
                            255,
                            255,
                            alpha
                    );

                    any |= solidTriangle(
                            buffer,
                            matrix,
                            domain.center(),
                            camera,
                            look,
                            a,
                            c,
                            d,
                            242,
                            248,
                            255,
                            alpha
                    );
                }
            }

            pose.popPose();
        }

        if (!any) {
            return;
        }

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

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void drawOpaque(
            BufferBuilder buffer
    ) {
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
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

        RenderSystem.enableCull();
    }

    private static void drawGlow(
            BufferBuilder buffer
    ) {
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

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static Vec3 point(
            double radius,
            double phi,
            double theta
    ) {
        double cosPhi =
                Math.cos(
                        phi
                );

        return new Vec3(
                Math.cos(
                        theta
                )
                        * cosPhi
                        * radius,
                Math.sin(
                        phi
                )
                        * radius,
                Math.sin(
                        theta
                )
                        * cosPhi
                        * radius
        );
    }

    private static boolean solidTriangle(
            BufferBuilder buffer,
            Matrix4f matrix,
            Vec3 center,
            Vec3 camera,
            Vector3f look,
            Vec3 a,
            Vec3 b,
            Vec3 c,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        if (!inFront(
                center.add(
                        a
                ),
                camera,
                look
        )
                || !inFront(
                center.add(
                        b
                ),
                camera,
                look
        )
                || !inFront(
                center.add(
                        c
                ),
                camera,
                look
        )) {
            return false;
        }

        vertex(
                buffer,
                matrix,
                a,
                red,
                green,
                blue,
                alpha
        );

        vertex(
                buffer,
                matrix,
                b,
                red,
                green,
                blue,
                alpha
        );

        vertex(
                buffer,
                matrix,
                c,
                red,
                green,
                blue,
                alpha
        );

        return true;
    }

    private static void quad(
            BufferBuilder buffer,
            Matrix4f matrix,
            Vec3 a,
            Vec3 b,
            Vec3 c,
            Vec3 d,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        triangle(
                buffer,
                matrix,
                a,
                b,
                c,
                red,
                green,
                blue,
                alpha
        );

        triangle(
                buffer,
                matrix,
                a,
                c,
                d,
                red,
                green,
                blue,
                alpha
        );
    }

    private static void box(
            BufferBuilder buffer,
            Matrix4f matrix,
            Vec3 center,
            double sx,
            double sy,
            double sz,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        double hx =
                sx * 0.5;

        double hy =
                sy * 0.5;

        double hz =
                sz * 0.5;

        Vec3 nnn =
                center.add(
                        -hx,
                        -hy,
                        -hz
                );

        Vec3 pnn =
                center.add(
                        hx,
                        -hy,
                        -hz
                );

        Vec3 ppn =
                center.add(
                        hx,
                        hy,
                        -hz
                );

        Vec3 npn =
                center.add(
                        -hx,
                        hy,
                        -hz
                );

        Vec3 nnp =
                center.add(
                        -hx,
                        -hy,
                        hz
                );

        Vec3 pnp =
                center.add(
                        hx,
                        -hy,
                        hz
                );

        Vec3 ppp =
                center.add(
                        hx,
                        hy,
                        hz
                );

        Vec3 npp =
                center.add(
                        -hx,
                        hy,
                        hz
                );

        quad(buffer, matrix, nnn, pnn, ppn, npn, red, green, blue, alpha);
        quad(buffer, matrix, pnp, nnp, npp, ppp, red, green, blue, alpha);
        quad(buffer, matrix, nnp, nnn, npn, npp, red, green, blue, alpha);
        quad(buffer, matrix, pnn, pnp, ppp, ppn, red, green, blue, alpha);
        quad(buffer, matrix, npn, ppn, ppp, npp, red, green, blue, alpha);
        quad(buffer, matrix, nnp, pnp, pnn, nnn, red, green, blue, alpha);
    }

    private static void boxEdges(
            BufferBuilder buffer,
            Matrix4f matrix,
            Vec3 center,
            double sx,
            double sy,
            double sz,
            double thickness,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        double hx =
                sx * 0.5;

        double hy =
                sy * 0.5;

        double hz =
                sz * 0.5;

        for (int yi = -1;
             yi <= 1;
             yi += 2) {
            for (int zi = -1;
                 zi <= 1;
                 zi += 2) {
                box(
                        buffer,
                        matrix,
                        center.add(
                                0.0,
                                yi * hy,
                                zi * hz
                        ),
                        sx,
                        thickness,
                        thickness,
                        red,
                        green,
                        blue,
                        alpha
                );
            }
        }

        for (int xi = -1;
             xi <= 1;
             xi += 2) {
            for (int zi = -1;
                 zi <= 1;
                 zi += 2) {
                box(
                        buffer,
                        matrix,
                        center.add(
                                xi * hx,
                                0.0,
                                zi * hz
                        ),
                        thickness,
                        sy,
                        thickness,
                        red,
                        green,
                        blue,
                        alpha
                );
            }
        }

        for (int xi = -1;
             xi <= 1;
             xi += 2) {
            for (int yi = -1;
                 yi <= 1;
                 yi += 2) {
                box(
                        buffer,
                        matrix,
                        center.add(
                                xi * hx,
                                yi * hy,
                                0.0
                        ),
                        thickness,
                        thickness,
                        sz,
                        red,
                        green,
                        blue,
                        alpha
                );
            }
        }
    }

    private static void triangle(
            BufferBuilder buffer,
            Matrix4f matrix,
            Vec3 a,
            Vec3 b,
            Vec3 c,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        vertex(
                buffer,
                matrix,
                a,
                red,
                green,
                blue,
                alpha
        );

        vertex(
                buffer,
                matrix,
                b,
                red,
                green,
                blue,
                alpha
        );

        vertex(
                buffer,
                matrix,
                c,
                red,
                green,
                blue,
                alpha
        );
    }

    private static void starOctahedron(
            BufferBuilder buffer,
            Matrix4f matrix,
            Vec3 center,
            float radius,
            int brightness
    ) {
        double x =
                center.x;

        double y =
                center.y;

        double z =
                center.z;

        triangleRaw(buffer, matrix, x, y + radius, z, x + radius, y, z, x, y, z + radius, brightness);
        triangleRaw(buffer, matrix, x, y + radius, z, x, y, z + radius, x - radius, y, z, brightness);
        triangleRaw(buffer, matrix, x, y + radius, z, x - radius, y, z, x, y, z - radius, brightness);
        triangleRaw(buffer, matrix, x, y + radius, z, x, y, z - radius, x + radius, y, z, brightness);

        triangleRaw(buffer, matrix, x, y - radius, z, x, y, z + radius, x + radius, y, z, brightness);
        triangleRaw(buffer, matrix, x, y - radius, z, x - radius, y, z, x, y, z + radius, brightness);
        triangleRaw(buffer, matrix, x, y - radius, z, x, y, z - radius, x - radius, y, z, brightness);
        triangleRaw(buffer, matrix, x, y - radius, z, x + radius, y, z, x, y, z - radius, brightness);
    }

    private static void triangleRaw(
            BufferBuilder buffer,
            Matrix4f matrix,
            double ax,
            double ay,
            double az,
            double bx,
            double by,
            double bz,
            double cx,
            double cy,
            double cz,
            int brightness
    ) {
        vertex(buffer, matrix, new Vec3(ax, ay, az), brightness, brightness, brightness, 255);
        vertex(buffer, matrix, new Vec3(bx, by, bz), brightness, brightness, brightness, 255);
        vertex(buffer, matrix, new Vec3(cx, cy, cz), brightness, brightness, brightness, 255);
    }

    private static boolean inFront(
            Vec3 point,
            Vec3 camera,
            Vector3f look
    ) {
        return (point.x - camera.x) * look.x()
                + (point.y - camera.y) * look.y()
                + (point.z - camera.z) * look.z()
                > NEAR_GUARD;
    }

    private static void vertex(
            BufferBuilder buffer,
            Matrix4f matrix,
            Vec3 point,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        buffer.addVertex(
                        matrix,
                        (float) point.x,
                        (float) point.y,
                        (float) point.z
                )
                .setColor(
                        red,
                        green,
                        blue,
                        alpha
                );
    }

    private static long mix(
            long value
    ) {
        value ^=
                value >>> 30;

        value *=
                0xBF58476D1CE4E5B9L;

        value ^=
                value >>> 27;

        value *=
                0x94D049BB133111EBL;

        return value
                ^ value >>> 31;
    }
}
