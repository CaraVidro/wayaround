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
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class JusticeDomainWorldRenderer {

    private JusticeDomainWorldRenderer() {
    }

    private static final int LAT =
            10;

    private static final int LON =
            20;

    private static final double NEAR_GUARD =
            0.32;

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

        var shells =
                JusticeDomainClientEffects.shells();

        var fragments =
                JusticeDomainClientEffects.fragments();

        if (shells.isEmpty()
                && fragments.isEmpty()) {
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

        for (JusticeDomainClientEffects.ShellVisual shell :
                shells) {

            any |= renderShell(
                    buffer,
                    pose,
                    camera,
                    look,
                    shell
            );
        }

        for (JusticeDomainClientEffects.FragmentVisual fragment :
                fragments) {

            any |= renderFragment(
                    buffer,
                    pose,
                    camera,
                    look,
                    fragment
            );
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

    private static boolean renderShell(
            BufferBuilder buffer,
            PoseStack pose,
            Vec3 camera,
            Vector3f look,
            JusticeDomainClientEffects.ShellVisual shell
    ) {
        float formation =
                shell.formation();

        double radius =
                shell.radius()
                        * (
                        0.16
                                + 0.84
                                        * ease(
                                        formation
                                )
                );

        int red =
                mix(
                        255,
                        218,
                        formation
                );

        int green =
                mix(
                        255,
                        170,
                        formation
                );

        int blue =
                mix(
                        255,
                        42,
                        formation
                );

        int alpha =
                Math.round(
                        72
                                + formation
                                        * 118
                );

        pose.pushPose();

        pose.translate(
                shell.center().x
                        - camera.x,
                shell.center().y
                        - camera.y,
                shell.center().z
                        - camera.z
        );

        Matrix4f matrix =
                pose.last()
                        .pose();

        boolean any =
                false;

        for (int lat = 0;
             lat < LAT;
             lat++) {

            double phi0 =
                    -Math.PI * 0.5
                            + Math.PI
                                    * lat
                                    / LAT;

            double phi1 =
                    -Math.PI * 0.5
                            + Math.PI
                                    * (
                                    lat + 1
                            )
                                    / LAT;

            for (int lon = 0;
                 lon < LON;
                 lon++) {

                double theta0 =
                        Math.PI * 2.0
                                * lon
                                / LON;

                double theta1 =
                        Math.PI * 2.0
                                * (
                                lon + 1
                        )
                                / LON;

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

                int facetAlpha =
                        (
                                lat + lon
                        ) % 2 == 0
                                ? alpha
                                : Math.max(
                                30,
                                alpha - 35
                        );

                any |= triangle(
                        buffer,
                        matrix,
                        shell.center(),
                        camera,
                        look,
                        a,
                        b,
                        c,
                        red,
                        green,
                        blue,
                        facetAlpha
                );

                any |= triangle(
                        buffer,
                        matrix,
                        shell.center(),
                        camera,
                        look,
                        a,
                        c,
                        d,
                        red,
                        green,
                        blue,
                        facetAlpha
                );
            }
        }

        pose.popPose();
        return any;
    }

    private static boolean renderFragment(
            BufferBuilder buffer,
            PoseStack pose,
            Vec3 camera,
            Vector3f look,
            JusticeDomainClientEffects.FragmentVisual fragment
    ) {
        if (!inFront(
                fragment.position(),
                camera,
                look
        )) {
            return false;
        }

        pose.pushPose();

        pose.translate(
                fragment.position().x
                        - camera.x,
                fragment.position().y
                        - camera.y,
                fragment.position().z
                        - camera.z
        );

        pose.mulPose(
                com.mojang.math.Axis.YP.rotationDegrees(
                        fragment.spin()
                )
        );

        pose.mulPose(
                com.mojang.math.Axis.XP.rotationDegrees(
                        fragment.spin()
                                * 0.73F
                )
        );

        Matrix4f matrix =
                pose.last()
                        .pose();

        float size =
                fragment.size();

        int alpha =
                Math.round(
                        fragment.alpha()
                                * 220.0F
                );

        vertex(buffer, matrix, -size, 0.0, 0.0, 224, 177, 49, alpha);
        vertex(buffer, matrix, size, 0.0, 0.0, 255, 245, 205, alpha);
        vertex(buffer, matrix, 0.0, size * 1.45, size * 0.25, 210, 150, 25, alpha);

        vertex(buffer, matrix, -size, 0.0, 0.0, 224, 177, 49, alpha);
        vertex(buffer, matrix, 0.0, -size * 0.55, size * 0.18, 198, 130, 18, alpha);
        vertex(buffer, matrix, size, 0.0, 0.0, 255, 245, 205, alpha);

        pose.popPose();
        return true;
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

        vertex(buffer, matrix, a.x, a.y, a.z, red, green, blue, alpha);
        vertex(buffer, matrix, b.x, b.y, b.z, red, green, blue, alpha);
        vertex(buffer, matrix, c.x, c.y, c.z, red, green, blue, alpha);

        return true;
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

    private static int mix(
            int from,
            int to,
            float t
    ) {
        return Math.round(
                from
                        + (
                        to - from
                )
                        * t
        );
    }

    private static float ease(
            float value
    ) {
        return value
                * value
                * (
                3.0F
                        - 2.0F
                                * value
        );
    }
}
