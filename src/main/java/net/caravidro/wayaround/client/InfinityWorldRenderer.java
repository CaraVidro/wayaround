package net.caravidro.wayaround.client;

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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class InfinityWorldRenderer {

    private InfinityWorldRenderer() {
    }

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

        var fields =
                InfinityClientEffects.visualFields();

        if (fields.isEmpty()) {
            return;
        }

        boolean renderable =
                fields.stream()
                        .anyMatch(
                                field ->
                                        field.confidence()
                                                >= 0.18F
                        );

        if (!renderable) {
            return;
        }

        Vec3 camera =
                event.getCamera()
                        .getPosition();

        PoseStack pose =
                event.getPoseStack();

        BufferBuilder buffer =
                Tesselator.getInstance()
                        .begin(
                                VertexFormat.Mode.TRIANGLES,
                                DefaultVertexFormat.POSITION_COLOR
                        );

        float time =
                minecraft.level
                        .getGameTime()
                        + event.getPartialTick();

        for (InfinityClientEffects.ClientInfinity field :
                fields) {

            if (field.confidence()
                    < 0.18F) {

                continue;
            }

            pose.pushPose();

            pose.translate(
                    field.position().x
                            - camera.x,
                    field.position().y
                            - camera.y,
                    field.position().z
                            - camera.z
            );

            float pulse =
                    0.5F
                            + 0.5F
                                    * Mth.sin(
                                            time * 0.12F
                                                    + field.owner()
                                                            .hashCode()
                                                            * 0.01F
                                    );

            int shells =
                    field.confidence()
                            >= 0.72F
                            ? 3
                            : 2;

            for (int shell = 0;
                 shell < shells;
                 shell++) {

                pose.pushPose();

                float phase =
                        time
                                * (
                                0.18F
                                        + shell
                                                * 0.09F
                        );

                pose.mulPose(
                        Axis.YP.rotationDegrees(
                                phase
                                        * 13.0F
                        )
                );

                pose.mulPose(
                        Axis.XP.rotationDegrees(
                                phase
                                        * 7.0F
                        )
                );

                float size =
                        (
                                1.35F
                                        + field.confidence()
                                                * 1.65F
                        )
                                * (
                                1.0F
                                        + shell
                                                * 0.13F
                                        + pulse
                                                * 0.025F
                        );

                int alpha =
                        Mth.clamp(
                                Math.round(
                                        12.0F
                                                + field.confidence()
                                                        * 36.0F
                                                - shell
                                                        * 6.0F
                                ),
                                7,
                                54
                        );

                octahedron(
                        buffer,
                        pose.last()
                                .pose(),
                        size,
                        174,
                        232,
                        255,
                        alpha
                );

                pose.popPose();
            }

            pose.popPose();
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
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

    private static void octahedron(
            BufferBuilder buffer,
            org.joml.Matrix4f matrix,
            float radius,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        float topY =
                radius;

        float bottomY =
                -radius;

        triangle(buffer, matrix, 0, topY, 0, radius, 0, 0, 0, 0, radius, red, green, blue, alpha);
        triangle(buffer, matrix, 0, topY, 0, 0, 0, radius, -radius, 0, 0, red, green, blue, alpha);
        triangle(buffer, matrix, 0, topY, 0, -radius, 0, 0, 0, 0, -radius, red, green, blue, alpha);
        triangle(buffer, matrix, 0, topY, 0, 0, 0, -radius, radius, 0, 0, red, green, blue, alpha);

        triangle(buffer, matrix, 0, bottomY, 0, 0, 0, radius, radius, 0, 0, red, green, blue, alpha);
        triangle(buffer, matrix, 0, bottomY, 0, -radius, 0, 0, 0, 0, radius, red, green, blue, alpha);
        triangle(buffer, matrix, 0, bottomY, 0, 0, 0, -radius, -radius, 0, 0, red, green, blue, alpha);
        triangle(buffer, matrix, 0, bottomY, 0, radius, 0, 0, 0, 0, -radius, red, green, blue, alpha);
    }

    private static void triangle(
            BufferBuilder buffer,
            org.joml.Matrix4f matrix,
            float ax,
            float ay,
            float az,
            float bx,
            float by,
            float bz,
            float cx,
            float cy,
            float cz,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        vertex(buffer, matrix, ax, ay, az, red, green, blue, alpha);
        vertex(buffer, matrix, bx, by, bz, red, green, blue, alpha);
        vertex(buffer, matrix, cx, cy, cz, red, green, blue, alpha);
    }

    private static void vertex(
            BufferBuilder buffer,
            org.joml.Matrix4f matrix,
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
}
