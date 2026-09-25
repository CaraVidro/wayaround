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
 * Exterior shell of the Void Domain.
 *
 * It is a deliberately faceted sphere rather than a smooth bubble. Each
 * triangle is camera-plane checked so this renderer cannot reproduce the old
 * cloud-shadow near-plane artifact.
 */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class VoidDomainWorldRenderer {

    private VoidDomainWorldRenderer() {
    }

    private static final int LATITUDE_SEGMENTS =
            9;

    private static final int LONGITUDE_SEGMENTS =
            18;

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

        var domains =
                VoidDomainClientEffects.visuals();

        if (domains.isEmpty()) {
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

            if (VoidDomainClientEffects.isInside(
                    domain.owner()
            )) {
                continue;
            }

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
                 lat < LATITUDE_SEGMENTS;
                 lat++) {

                double phi0 =
                        -Math.PI * 0.5
                                + Math.PI
                                        * lat
                                        / LATITUDE_SEGMENTS;

                double phi1 =
                        -Math.PI * 0.5
                                + Math.PI
                                        * (
                                        lat + 1
                                )
                                        / LATITUDE_SEGMENTS;

                for (int lon = 0;
                     lon < LONGITUDE_SEGMENTS;
                     lon++) {

                    double theta0 =
                            Math.PI * 2.0
                                    * lon
                                    / LONGITUDE_SEGMENTS;

                    double theta1 =
                            Math.PI * 2.0
                                    * (
                                    lon + 1
                            )
                                    / LONGITUDE_SEGMENTS;

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

                    any |= triangle(
                            buffer,
                            matrix,
                            domain.center(),
                            camera,
                            look,
                            a,
                            b,
                            c,
                            alpha
                    );

                    any |= triangle(
                            buffer,
                            matrix,
                            domain.center(),
                            camera,
                            look,
                            a,
                            c,
                            d,
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

    private static boolean triangle(
            BufferBuilder buffer,
            Matrix4f matrix,
            Vec3 center,
            Vec3 camera,
            Vector3f look,
            Vec3 a,
            Vec3 b,
            Vec3 c,
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
                a,
                255,
                255,
                255,
                alpha
        );

        vertex(
                buffer,
                matrix,
                b,
                242,
                248,
                255,
                alpha
        );

        vertex(
                buffer,
                matrix,
                c,
                255,
                255,
                255,
                alpha
        );

        return true;
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
}
