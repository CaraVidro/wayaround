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
import net.caravidro.wayaround.network.BlueVisualPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Full-bright-looking procedural 3D cube for every synced Blue.
 */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class BlueWorldRenderer {

    private BlueWorldRenderer() {
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

        boolean charge =
                BlueClientEffects.chargeVisualActive();

        var blues =
                BlueClientEffects.visualBlues();

        if (!charge
                && blues.isEmpty()) {
            return;
        }

        Vec3 camera =
                event.getCamera()
                        .getPosition();

        long time =
                minecraft.level
                        .getGameTime();

        PoseStack poseStack =
                event.getPoseStack();

        BufferBuilder buffer =
                Tesselator.getInstance()
                        .begin(
                                VertexFormat.Mode.QUADS,
                                DefaultVertexFormat.POSITION_COLOR
                        );

        if (charge) {
            emitBlue(
                    buffer,
                    poseStack,
                    camera,
                    BlueClientEffects.chargeVisualCenter(),
                    BlueClientEffects.chargeVisualPower(),
                    BlueClientEffects.chargeVisualTicks(),
                    0.0F,
                    false
            );
        }

        for (BlueClientEffects.VisualBlue blue :
                blues) {
            emitBlue(
                    buffer,
                    poseStack,
                    camera,
                    blue.position(),
                    blue.power(),
                    blue.musicTicks(),
                    blue.owner()
                            .hashCode()
                            * 0.017F,
                    blue.mode()
                            == BlueVisualPayload.COLLAPSING
            );
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

    private static void emitBlue(
            BufferBuilder buffer,
            PoseStack poseStack,
            Vec3 camera,
            Vec3 center,
            float power,
            float musicTicks,
            float phaseOffset,
            boolean collapsing
    ) {
        poseStack.pushPose();

        poseStack.translate(
                center.x - camera.x,
                center.y - camera.y,
                center.z - camera.z
        );

        /*
         * Rotation accelerates with the same timeline as the 55 s theme.
         * The quadratic term means it begins almost calm and becomes
         * increasingly violent as the music approaches its end.
         */
        float songProgress =
                Mth.clamp(
                        musicTicks
                                / (55.0F * 20.0F),
                        0.0F,
                        1.0F
                );

        float slowRotation =
                musicTicks
                        * 0.42F
                        + musicTicks
                                * musicTicks
                                * 0.0043F
                        + phaseOffset;

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        slowRotation
                )
        );

        poseStack.mulPose(
                Axis.XP.rotationDegrees(
                        slowRotation
                                * 0.39F
                )
        );

        poseStack.mulPose(
                Axis.ZP.rotationDegrees(
                        slowRotation
                                * 0.18F
                )
        );

        float pulse =
                0.5F
                        + 0.5F
                                * Mth.sin(
                                        musicTicks
                                                * (
                                                        0.10F
                                                                + songProgress
                                                                        * 0.18F
                                                )
                                                + phaseOffset
                                );

        float visiblePower =
                Mth.clamp(
                        power,
                        0.03F,
                        1.35F
                );

        /*
         * A freshly tapped Blue can be about slab-height. Size then rises
         * non-linearly with charge, while a fully charged Blue remains huge.
         */
        float normalizedPower =
                Mth.clamp(
                        visiblePower / 1.35F,
                        0.0F,
                        1.0F
                );

        float coreHalf =
                0.14F
                        + (float) Math.pow(
                                normalizedPower,
                                1.50
                        )
                                * 2.31F;

        if (collapsing) {
            coreHalf *=
                    0.82F;
        }

        float shellHalf =
                coreHalf
                        * (
                                1.48F
                                        + pulse
                                                * 0.07F
                        );

        float shell2Half =
                shellHalf
                        * 1.22F;

        var matrix =
                poseStack.last()
                        .pose();

        cube(
                buffer,
                matrix,
                shell2Half,
                20,
                112,
                255,
                24
        );

        cube(
                buffer,
                matrix,
                shellHalf,
                12,
                155
                        + Math.round(
                                pulse
                                        * 45.0F
                        ),
                255,
                64
        );

        /*
         * The core intentionally stays saturated and opaque even when the
         * nearby world is darkened by the client effect. This makes Blue read
         * like a violent light source without placing fake light blocks.
         */
        cube(
                buffer,
                matrix,
                coreHalf,
                4,
                82
                        + Math.round(
                                pulse
                                        * 42.0F
                        ),
                240
                        + Math.round(
                                pulse
                                        * 15.0F
                        ),
                224
        );

        poseStack.popPose();
    }

    private static void cube(
            BufferBuilder buffer,
            org.joml.Matrix4f matrix,
            float half,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        float min =
                -half;

        float max =
                half;

        vertex(buffer, matrix, min, min, max, red, green, blue, alpha);
        vertex(buffer, matrix, max, min, max, red, green, blue, alpha);
        vertex(buffer, matrix, max, min, min, red, green, blue, alpha);
        vertex(buffer, matrix, min, min, min, red, green, blue, alpha);

        vertex(buffer, matrix, min, max, min, red, green, blue, alpha);
        vertex(buffer, matrix, max, max, min, red, green, blue, alpha);
        vertex(buffer, matrix, max, max, max, red, green, blue, alpha);
        vertex(buffer, matrix, min, max, max, red, green, blue, alpha);

        vertex(buffer, matrix, min, min, min, red, green, blue, alpha);
        vertex(buffer, matrix, max, min, min, red, green, blue, alpha);
        vertex(buffer, matrix, max, max, min, red, green, blue, alpha);
        vertex(buffer, matrix, min, max, min, red, green, blue, alpha);

        vertex(buffer, matrix, min, max, max, red, green, blue, alpha);
        vertex(buffer, matrix, max, max, max, red, green, blue, alpha);
        vertex(buffer, matrix, max, min, max, red, green, blue, alpha);
        vertex(buffer, matrix, min, min, max, red, green, blue, alpha);

        vertex(buffer, matrix, min, min, max, red, green, blue, alpha);
        vertex(buffer, matrix, min, min, min, red, green, blue, alpha);
        vertex(buffer, matrix, min, max, min, red, green, blue, alpha);
        vertex(buffer, matrix, min, max, max, red, green, blue, alpha);

        vertex(buffer, matrix, max, min, min, red, green, blue, alpha);
        vertex(buffer, matrix, max, min, max, red, green, blue, alpha);
        vertex(buffer, matrix, max, max, max, red, green, blue, alpha);
        vertex(buffer, matrix, max, max, min, red, green, blue, alpha);
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
