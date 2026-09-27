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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Infinity is deliberately not rendered as a colored shield.
 *
 * This renderer approximates hot-air refraction with extremely faint,
 * color-neutral warped cylindrical ribbons. It does not paint an obvious aura:
 * at rest the field is almost invisible, while movement makes the air around
 * the owner look subtly unstable.
 */
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
                        + event.getPartialTick()
                                .getGameTimeDeltaPartialTick(
                                        false
                                );

        boolean drewAnything =
                false;

        for (InfinityClientEffects.ClientInfinity field :
                fields) {

            if (field.confidence()
                    < 0.16F) {

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

            float confidence =
                    field.confidence();

            /*
             * Most of the physical field stays invisible. These three bands
             * merely hint at the space being optically bent.
             */
            float baseRadius =
                    Math.max(
                            2.0F,
                            field.radius()
                                    * 0.58F
                    );

            heatRibbon(
                    buffer,
                    pose.last()
                            .pose(),
                    baseRadius,
                    -1.20F,
                    1.15F,
                    time * 0.055F,
                    confidence,
                    4
            );

            heatRibbon(
                    buffer,
                    pose.last()
                            .pose(),
                    baseRadius * 0.78F,
                    -0.45F,
                    1.85F,
                    -time * 0.071F + 1.7F,
                    confidence,
                    3
            );

            heatRibbon(
                    buffer,
                    pose.last()
                            .pose(),
                    baseRadius * 1.07F,
                    -1.65F,
                    0.45F,
                    time * 0.043F + 3.4F,
                    confidence,
                    2
            );

            pose.popPose();
            drewAnything =
                    true;
        }

        if (!drewAnything) {
            return;
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

    private static void heatRibbon(
            BufferBuilder buffer,
            org.joml.Matrix4f matrix,
            float radius,
            float bottomY,
            float topY,
            float phase,
            float confidence,
            int baseAlpha
    ) {
        int segments =
                48;

        float height =
                topY
                        - bottomY;

        for (int segment = 0;
             segment < segments;
             segment++) {

            float a0 =
                    (float) (
                            Math.PI
                                    * 2.0
                                    * segment
                                    / segments
                    );

            float a1 =
                    (float) (
                            Math.PI
                                    * 2.0
                                    * (
                                    segment + 1
                            )
                                    / segments
                    );

            float wobble0 =
                    Mth.sin(
                            a0 * 3.0F
                                    + phase
                    )
                            * (
                            0.055F
                                    + confidence
                                            * 0.075F
                    );

            float wobble1 =
                    Mth.sin(
                            a1 * 3.0F
                                    + phase
                    )
                            * (
                            0.055F
                                    + confidence
                                            * 0.075F
                    );

            float r0 =
                    radius
                            + wobble0;

            float r1 =
                    radius
                            + wobble1;

            float yWave0 =
                    Mth.sin(
                            a0 * 2.0F
                                    - phase * 1.7F
                    )
                            * 0.14F;

            float yWave1 =
                    Mth.sin(
                            a1 * 2.0F
                                    - phase * 1.7F
                    )
                            * 0.14F;

            float x0 =
                    Mth.cos(a0)
                            * r0;

            float z0 =
                    Mth.sin(a0)
                            * r0;

            float x1 =
                    Mth.cos(a1)
                            * r1;

            float z1 =
                    Mth.sin(a1)
                            * r1;

            int alpha =
                    Mth.clamp(
                            Math.round(
                                    baseAlpha
                                            + confidence
                                                    * 5.0F
                                            + (
                                            0.5F
                                                    + 0.5F
                                                    * Mth.sin(
                                                    phase
                                                            * 5.0F
                                                            + a0
                                            )
                                    )
                                                    * 2.0F
                            ),
                            2,
                            12
                    );

            /*
             * Neutral white only. Low alpha and moving geometry provide the
             * heat-haze cue without turning Infinity into a colored bubble.
             */
            quad(
                    buffer,
                    matrix,
                    x0,
                    bottomY + yWave0,
                    z0,
                    x1,
                    bottomY + yWave1,
                    z1,
                    x1,
                    bottomY + height + yWave1,
                    z1,
                    x0,
                    bottomY + height + yWave0,
                    z0,
                    246,
                    246,
                    246,
                    alpha
            );
        }
    }

    private static void quad(
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
            float dx,
            float dy,
            float dz,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        vertex(
                buffer,
                matrix,
                ax,
                ay,
                az,
                red,
                green,
                blue,
                alpha
        );

        vertex(
                buffer,
                matrix,
                bx,
                by,
                bz,
                red,
                green,
                blue,
                alpha
        );

        vertex(
                buffer,
                matrix,
                cx,
                cy,
                cz,
                red,
                green,
                blue,
                alpha
        );

        vertex(
                buffer,
                matrix,
                ax,
                ay,
                az,
                red,
                green,
                blue,
                alpha
        );

        vertex(
                buffer,
                matrix,
                cx,
                cy,
                cz,
                red,
                green,
                blue,
                alpha
        );

        vertex(
                buffer,
                matrix,
                dx,
                dy,
                dz,
                red,
                green,
                blue,
                alpha
        );
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
