package net.caravidro.wayaround.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.client.cinematic.PlayerAnimationController;
import net.caravidro.wayaround.network.PlayerCinematicPayload;
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
public final class TukunaFugaWorldRenderer {

    private TukunaFugaWorldRenderer() {
    }

    private static final int SIDES =
            10;

    private static final int HEIGHT_SEGMENTS =
            36;

    private static final double HEIGHT =
            224.0;

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

        var visuals =
                TukunaFugaClientEffects.visuals();

        boolean chargingBow = minecraft.level.players().stream().anyMatch(player ->
                PlayerAnimationController.animationAge(player.getUUID(), PlayerCinematicPayload.FUGA_CHARGE) > 60);
        if (visuals.isEmpty() && !chargingBow && TukunaFugaClientEffects.arrows().isEmpty()) {
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

        for (TukunaFugaClientEffects.PillarVisual pillar :
                visuals) {

            any |= emitPillar(
                    buffer,
                    pose,
                    camera,
                    look,
                    pillar
            );
        }

        if (chargingBow) {
            for (var player : minecraft.level.players()) {
                if (PlayerAnimationController.animationAge(player.getUUID(),
                        PlayerCinematicPayload.FUGA_CHARGE) > 60
                        && player.distanceToSqr(minecraft.player) < 128.0 * 128.0) {
                    any |= emitFireBow(buffer, pose, camera, look, player);
                }
            }
        }

        for (var arrow : TukunaFugaClientEffects.arrows()) {
            double dt = Math.min(2, Math.max(0, minecraft.level.getGameTime() - arrow.receivedAt()));
            any |= emitArrow(buffer, pose, camera, look, arrow.position().add(arrow.velocity().scale(dt)),
                    arrow.velocity().normalize(), 1.0F);
        }
        if (!any) {
            var empty = buffer.build();
            if (empty != null) empty.close();
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

    private static boolean emitPillar(
            BufferBuilder buffer,
            PoseStack pose,
            Vec3 camera,
            Vector3f look,
            TukunaFugaClientEffects.PillarVisual pillar
    ) {
        pose.pushPose();

        pose.translate(
                pillar.center().x
                        - camera.x,
                pillar.center().y
                        - camera.y,
                pillar.center().z
                        - camera.z
        );

        Matrix4f matrix =
                pose.last()
                        .pose();

        float birth =
                pillar.birth();

        float fade =
                pillar.fade();

        float pulse =
                0.86F
                        + 0.14F
                                * (float) Math.sin(
                                pillar.age()
                                        * 0.72F
                        );

        boolean any =
                false;

        double visibleHeight =
                HEIGHT
                        * (
                        0.10
                                + 0.90
                                        * ease(
                                        birth
                                )
                );

        for (int layer = 0;
             layer < 2;
             layer++) {

            double baseRadius =
                    (
                            layer == 0
                                    ? 7.8
                                    : 3.8
                    )
                            * pulse
                            * (
                            0.55
                                    + birth
                                            * 0.45
                    );

            int red =
                    255;

            int green =
                    layer == 0
                            ? 66
                            : 220;

            int blue =
                    layer == 0
                            ? 16
                            : 138;

            int alpha =
                    Math.round(
                            (
                                    layer == 0
                                            ? 118.0F
                                            : 182.0F
                            )
                                    * fade
                    );

            for (int ySegment = 0;
                 ySegment < HEIGHT_SEGMENTS;
                 ySegment++) {

                double t0 =
                        ySegment
                                / (double) HEIGHT_SEGMENTS;

                double t1 =
                        (
                                ySegment + 1
                        )
                                / (double) HEIGHT_SEGMENTS;

                double y0 =
                        visibleHeight
                                * t0;

                double y1 =
                        visibleHeight
                                * t1;

                double taper0 =
                        1.0
                                - t0
                                        * 0.48;

                double taper1 =
                        1.0
                                - t1
                                        * 0.48;

                double wobble0 =
                        1.0
                                + Math.sin(
                                pillar.age()
                                        * 0.22
                                        + ySegment
                                                * 0.83
                                        + layer
                                                * 1.7
                        )
                                        * 0.12;

                double wobble1 =
                        1.0
                                + Math.sin(
                                pillar.age()
                                        * 0.22
                                        + (
                                        ySegment + 1
                                )
                                                * 0.83
                                        + layer
                                                * 1.7
                        )
                                        * 0.12;

                double r0 =
                        baseRadius
                                * taper0
                                * wobble0;

                double r1 =
                        baseRadius
                                * taper1
                                * wobble1;

                int glitchFrame = (int)(pillar.age() / 2);
                int noise = Math.floorMod(glitchFrame * 31 + ySegment * 17 + layer * 13, 23);
                double stretch = noise < 3 ? 1.8 + noise * .65 : 1.0;
                r0 *= stretch; r1 *= stretch;
                double jitterX = Math.sin(glitchFrame*2.7+ySegment)*.38;
                double jitterZ = Math.cos(glitchFrame*3.1+ySegment)*.38;
                for (int side = 0;
                     side < SIDES;
                     side++) {

                    double a0 =
                            Math.PI
                                    * 2.0
                                    * side
                                    / SIDES;

                    double a1 =
                            Math.PI
                                    * 2.0
                                    * (
                                    side + 1
                            )
                                    / SIDES;

                    Vec3 a =
                            new Vec3(
                                    Math.cos(a0) * r0 + jitterX,
                                    y0,
                                    Math.sin(a0) * r0 + jitterZ
                            );

                    Vec3 b =
                            new Vec3(
                                    Math.cos(a1) * r0 + jitterX,
                                    y0,
                                    Math.sin(a1) * r0 + jitterZ
                            );

                    Vec3 c =
                            new Vec3(
                                    Math.cos(a1) * r1 - jitterX,
                                    y1,
                                    Math.sin(a1) * r1 - jitterZ
                            );

                    Vec3 d =
                            new Vec3(
                                    Math.cos(a0) * r1 - jitterX,
                                    y1,
                                    Math.sin(a0) * r1 - jitterZ
                            );

                    any |= triangle(
                            buffer,
                            matrix,
                            pillar.center(),
                            camera,
                            look,
                            a,
                            b,
                            c,
                            red,
                            green,
                            blue,
                            alpha
                    );

                    any |= triangle(
                            buffer,
                            matrix,
                            pillar.center(),
                            camera,
                            look,
                            a,
                            c,
                            d,
                            red,
                            green,
                            blue,
                            alpha
                    );
                }
            }
        }

        any |= emitRing(
                buffer,
                matrix,
                pillar.center(),
                camera,
                look,
                0.55,
                12.0
                        + birth * 20.0,
                2.2,
                255,
                96,
                24,
                Math.round(
                        150.0F
                                * fade
                )
        );

        any |= emitRing(
                buffer,
                matrix,
                pillar.center(),
                camera,
                look,
                visibleHeight
                        * 0.72,
                5.5,
                12.5,
                255,
                210,
                105,
                Math.round(
                        92.0F
                                * fade
                )
        );

        pose.popPose();

        return any;
    }

    private static boolean emitFireBow(BufferBuilder buffer, PoseStack pose, Vec3 camera, Vector3f look,
                                       net.minecraft.world.entity.player.Player player) {
        Vec3 facing = player.getLookAngle().normalize();
        Vec3 right = new Vec3(-facing.z,0,facing.x).normalize();
        Vec3 center = player.getEyePosition().add(facing.scale(1.05)).add(right.scale(.58)).add(0,-.2,0);
        float age = PlayerAnimationController.animationAge(player.getUUID(),PlayerCinematicPayload.FUGA_CHARGE);
        float formation = Math.max(0,Math.min(1,(age-60)/60));
        return emitArrow(buffer,pose,camera,look,center,facing,formation);
    }

    private static boolean emitArrow(BufferBuilder buffer, PoseStack pose, Vec3 camera, Vector3f look,
                                     Vec3 center, Vec3 facing, float formation) {
        pose.pushPose(); pose.translate(center.x-camera.x,center.y-camera.y,center.z-camera.z);
        Matrix4f matrix=pose.last().pose();
        Vec3 right=facing.cross(new Vec3(0,1,0));
        if(right.lengthSqr()<.01)right=new Vec3(1,0,0);else right=right.normalize();
        Vec3 up=right.cross(facing).normalize();
        Vec3 tail=facing.scale(-1.2*formation),tip=facing.scale(1.6*formation);
        int gold=(int)(100+145*formation);
        boolean any=emitRod(buffer,matrix,center,camera,look,tail,tip,.075+formation*.06,255,30,8,230);
        any|=emitRod(buffer,matrix,center,camera,look,tail,tip,.025+formation*.025,255,gold,100,255);
        for(Vec3 axis:new Vec3[]{right,up,right.scale(-1),up.scale(-1)}){
            any|=emitRod(buffer,matrix,center,camera,look,tip,facing.scale(.8*formation).add(axis.scale(.33*formation)),
                    .07,255,gold,45,245);
            any|=emitRod(buffer,matrix,center,camera,look,tail,tail.add(facing.scale(.4)).add(axis.scale(.28*formation)),
                    .045,255,65,10,220);
        }
        pose.popPose();return any;
    }

    private static boolean emitRod(BufferBuilder buffer, Matrix4f matrix,
                                   Vec3 center, Vec3 camera, Vector3f look,
                                   Vec3 from, Vec3 to, double radius,
                                   int red, int green, int blue, int alpha) {
        Vec3 axis = to.subtract(from).normalize();
        Vec3 side = axis.cross(new Vec3(0, 0, 1));
        if (side.lengthSqr() < .001) side = axis.cross(new Vec3(0, 1, 0));
        side = side.normalize().scale(radius);
        Vec3 up = axis.cross(side).normalize().scale(radius);
        Vec3[] ring = { side, up, side.scale(-1), up.scale(-1) };
        boolean any = false;
        for (int i = 0; i < 4; i++) {
            Vec3 a = from.add(ring[i]);
            Vec3 b = from.add(ring[(i + 1) % 4]);
            Vec3 c = to.add(ring[(i + 1) % 4]);
            Vec3 d = to.add(ring[i]);
            any |= triangle(buffer, matrix, center, camera, look,
                    a, b, c, red, green, blue, alpha);
            any |= triangle(buffer, matrix, center, camera, look,
                    a, c, d, red, green, blue, alpha);
        }
        return any;
    }

    private static boolean emitRing(
            BufferBuilder buffer,
            Matrix4f matrix,
            Vec3 center,
            Vec3 camera,
            Vector3f look,
            double y,
            double inner,
            double outer,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        boolean any =
                false;

        for (int side = 0;
             side < 24;
             side++) {

            double a0 =
                    Math.PI
                            * 2.0
                            * side
                            / 24.0;

            double a1 =
                    Math.PI
                            * 2.0
                            * (
                            side + 1
                    )
                            / 24.0;

            Vec3 a =
                    new Vec3(
                            Math.cos(a0) * inner,
                            y,
                            Math.sin(a0) * inner
                    );

            Vec3 b =
                    new Vec3(
                            Math.cos(a1) * inner,
                            y,
                            Math.sin(a1) * inner
                    );

            Vec3 c =
                    new Vec3(
                            Math.cos(a1) * outer,
                            y,
                            Math.sin(a1) * outer
                    );

            Vec3 d =
                    new Vec3(
                            Math.cos(a0) * outer,
                            y,
                            Math.sin(a0) * outer
                    );

            any |= triangle(
                    buffer,
                    matrix,
                    center,
                    camera,
                    look,
                    a,
                    b,
                    c,
                    red,
                    green,
                    blue,
                    alpha
            );

            any |= triangle(
                    buffer,
                    matrix,
                    center,
                    camera,
                    look,
                    a,
                    c,
                    d,
                    red,
                    green,
                    blue,
                    alpha
            );
        }

        return any;
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

    private static boolean inFront(
            Vec3 point,
            Vec3 camera,
            Vector3f look
    ) {
        return (
                point.x - camera.x
        )
                * look.x()
                + (
                point.y - camera.y
        )
                * look.y()
                + (
                point.z - camera.z
        )
                * look.z()
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

