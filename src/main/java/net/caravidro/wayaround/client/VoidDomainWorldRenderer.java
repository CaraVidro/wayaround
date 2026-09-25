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
 * World-space renderer for BOTH sides of the Void Domain:
 *
 * outside -> compact faceted white sphere at the cast location;
 * inside  -> huge black 3D shell + hundreds of actual world-space stars.
 *
 * The inside stars are not HUD pixels. They have fixed 3D coordinates around
 * the pocket center, so walking as the Void user produces real parallax and
 * placed blocks/players correctly occlude them through the depth buffer.
 */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class VoidDomainWorldRenderer {

    private VoidDomainWorldRenderer() {
    }

    private static final int EXTERIOR_LATITUDE_SEGMENTS =
            9;

    private static final int EXTERIOR_LONGITUDE_SEGMENTS =
            18;

    private static final int INTERIOR_LATITUDE_SEGMENTS =
            12;

    private static final int INTERIOR_LONGITUDE_SEGMENTS =
            24;

    private static final int STAR_COUNT =
            360;

    private static final double INTERIOR_SHELL_RADIUS =
            92.0;

    private static final double STAR_MIN_RADIUS =
            56.0;

    private static final double STAR_MAX_RADIUS =
            86.0;

    private static final double NEAR_GUARD =
            0.35;

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
            PoseStack pose,
            Vec3 camera,
            Vector3f look,
            VoidDomainClientEffects.VisualDomain domain
    ) {
        BufferBuilder buffer =
                Tesselator.getInstance()
                        .begin(
                                VertexFormat.Mode.TRIANGLES,
                                DefaultVertexFormat.POSITION_COLOR
                        );

        boolean any =
                false;

        pose.pushPose();

        pose.translate(
                domain.center().x
                        - camera.x,
                domain.center().y
                        - camera.y,
                domain.center().z
                        - camera.z
        );

        Matrix4f matrix =
                pose.last()
                        .pose();

        /*
         * Opaque black shell. Normal blocks and entities remain visible because
         * they are closer and already own nearer depth values.
         */
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
                                    * (
                                    lat + 1
                            )
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
                                * (
                                lon + 1
                        )
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

                any |= solidTriangle(
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
                        0,
                        255
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
                        0,
                        0,
                        0,
                        255
                );
            }
        }

        /*
         * Deterministic 3D star field. Different radii produce actual parallax.
         */
        long seed =
                domain.owner()
                        .getMostSignificantBits()
                        ^ domain.owner()
                        .getLeastSignificantBits();

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
                    (
                            h >>> 11
                    )
                            * 0x1.0p-53;

            long h2 =
                    mix(
                            h
                                    ^ 0xD1B54A32D192ED03L
                    );

            double v =
                    (
                            h2 >>> 11
                    )
                            * 0x1.0p-53;

            long h3 =
                    mix(
                            h2
                                    ^ 0x94D049BB133111EBL
                    );

            double w =
                    (
                            h3 >>> 11
                    )
                            * 0x1.0p-53;

            double y =
                    1.0
                            - 2.0
                                    * u;

            double radial =
                    Math.sqrt(
                            Math.max(
                                    0.0,
                                    1.0
                                            - y * y
                            )
                    );

            double theta =
                    Math.PI
                            * 2.0
                            * v;

            double radius =
                    STAR_MIN_RADIUS
                            + (
                            STAR_MAX_RADIUS
                                    - STAR_MIN_RADIUS
                    )
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

            Vec3 worldStar =
                    domain.center()
                            .add(
                                    star
                            );

            if (!inFront(
                    worldStar,
                    camera,
                    look
            )) {
                continue;
            }

            float size =
                    (
                            h3 & 31L
                    ) == 0L
                            ? 0.23F
                            : (
                            h3 & 7L
                    ) == 0L
                            ? 0.14F
                            : 0.075F;

            starOctahedron(
                    buffer,
                    matrix,
                    star,
                    size,
                    (
                            h3 & 15L
                    ) == 0L
                            ? 210
                            : 255
            );

            any =
                    true;
        }

        pose.popPose();

        if (!any) {
            return;
        }

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

        RenderSystem.setShaderColor(
                1.0F,
                1.0F,
                1.0F,
                1.0F
        );
        RenderSystem.enableCull();
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
                    domain.center().x
                            - camera.x,
                    domain.center().y
                            - camera.y,
                    domain.center().z
                            - camera.z
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
                                        * (
                                        lat + 1
                                )
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
                                    * (
                                    lon + 1
                            )
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
                            (
                                    lat + lon
                            ) % 2 == 0
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
                Math.cos(theta)
                        * cosPhi
                        * radius,
                Math.sin(phi)
                        * radius,
                Math.sin(theta)
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
                center.add(a),
                camera,
                look
        )
                || !inFront(
                center.add(b),
                camera,
                look
        )
                || !inFront(
                center.add(c),
                camera,
                look
        )) {
            return false;
        }

        vertex(
                buffer,
                matrix,
                a.x,
                a.y,
                a.z,
                red,
                green,
                blue,
                alpha
        );

        vertex(
                buffer,
                matrix,
                b.x,
                b.y,
                b.z,
                red,
                green,
                blue,
                alpha
        );

        vertex(
                buffer,
                matrix,
                c.x,
                c.y,
                c.z,
                red,
                green,
                blue,
                alpha
        );

        return true;
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
        vertex(buffer, matrix, ax, ay, az, brightness, brightness, brightness, 255);
        vertex(buffer, matrix, bx, by, bz, brightness, brightness, brightness, 255);
        vertex(buffer, matrix, cx, cy, cz, brightness, brightness, brightness, 255);
    }

    private static boolean inFront(
            Vec3 point,
            Vec3 camera,
            Vector3f look
    ) {
        return (
                point.x - camera.x
        ) * look.x()
                + (
                point.y - camera.y
        ) * look.y()
                + (
                point.z - camera.z
        ) * look.z()
                > NEAR_GUARD;
    }

    private static void vertex(
            BufferBuilder buffer,
            Matrix4f matrix,
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
