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
import net.caravidro.wayaround.network.BetaTechniqueVisualPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Procedural RED/PURPLE renderer. No static model asset is required: the
 * "rooted cubes" are calculated deterministically from the owner UUID and the
 * fusion timeline.
 */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class BetaTechniqueWorldRenderer {

    private BetaTechniqueWorldRenderer() {
    }

    private static final float FUSION_HIDE_PROGRESS =
            0.86F;

    private static final double GOLDEN_ANGLE =
            Math.PI
                    * (
                    3.0
                            - Math.sqrt(
                            5.0
                    )
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

        var states =
                BetaTechniqueClientEffects
                        .visuals();

        if (states.isEmpty()) {
            return;
        }

        Vec3 camera =
                event.getCamera()
                        .getPosition();

        long time =
                minecraft.level
                        .getGameTime();

        PoseStack pose =
                event.getPoseStack();

        boolean hasRenderableGeometry =
                false;

        for (BetaTechniqueClientEffects.VisualTechnique state :
                states) {

            if (state.mode()
                    == BetaTechniqueVisualPayload.RED
                    || (
                    state.mode()
                            == BetaTechniqueVisualPayload.FUSION
                            && state.progress()
                                    < FUSION_HIDE_PROGRESS
            )
                    || (
                    state.mode()
                            == BetaTechniqueVisualPayload.AFTERMATH
                            && state.progress()
                                    <= 0.78F
            )) {

                hasRenderableGeometry =
                        true;

                break;
            }
        }

        /*
         * AFTERMATH intentionally stops drawing the PURPLE core near the end.
         * In that window the state list is still non-empty, but no vertices
         * are emitted. Calling buildOrThrow() on an empty BufferBuilder crashes
         * the render thread, so simply skip the draw pass.
         */
        if (!hasRenderableGeometry) {
            return;
        }

        BufferBuilder buffer =
                Tesselator.getInstance()
                        .begin(
                                VertexFormat.Mode.QUADS,
                                DefaultVertexFormat.POSITION_COLOR
                        );

        for (BetaTechniqueClientEffects.VisualTechnique state :
                states) {

            if (state.mode()
                    == BetaTechniqueVisualPayload.RED) {

                emitRed(
                        buffer,
                        pose,
                        camera,
                        state,
                        time
                );

            } else {
                emitPurple(
                        buffer,
                        pose,
                        camera,
                        state,
                        time
                );
            }
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

    private static void emitRed(
            BufferBuilder buffer,
            PoseStack pose,
            Vec3 camera,
            BetaTechniqueClientEffects.VisualTechnique state,
            long time
    ) {
        pose.pushPose();

        pose.translate(
                state.position().x
                        - camera.x,
                state.position().y
                        - camera.y,
                state.position().z
                        - camera.z
        );

        float rotation =
                time * 11.0F
                        + state.owner()
                                .hashCode()
                                * 0.03F;

        pose.mulPose(
                Axis.YP.rotationDegrees(
                        rotation
                )
        );

        pose.mulPose(
                Axis.XP.rotationDegrees(
                        rotation * 0.63F
                )
        );

        var matrix =
                pose.last()
                        .pose();

        cube(
                buffer,
                matrix,
                0.46F,
                255,
                15,
                20,
                28
        );

        cube(
                buffer,
                matrix,
                0.31F,
                255,
                24,
                28,
                84
        );

        cube(
                buffer,
                matrix,
                0.19F,
                255,
                72,
                62,
                238
        );

        pose.popPose();
    }

    private static void emitPurple(
            BufferBuilder buffer,
            PoseStack pose,
            Vec3 camera,
            BetaTechniqueClientEffects.VisualTechnique state,
            long time
    ) {
        if (state.mode()
                == BetaTechniqueVisualPayload.AFTERMATH
                && state.progress()
                > 0.78F) {

            return;
        }

        if (state.mode()
                == BetaTechniqueVisualPayload.FUSION
                && state.progress()
                >= FUSION_HIDE_PROGRESS) {

            return;
        }

        float progress =
                state.mode()
                        == BetaTechniqueVisualPayload.FUSION
                        ? Mth.clamp(
                        state.progress(),
                        0.0F,
                        1.0F
                )
                        : 1.0F
                                - Mth.clamp(
                                state.progress(),
                                0.0F,
                                1.0F
                        );

        pose.pushPose();

        pose.translate(
                state.position().x
                        - camera.x,
                state.position().y
                        - camera.y,
                state.position().z
                        - camera.z
        );

        double phase =
                state.owner()
                        .hashCode()
                        * 0.00091;

        float rotation =
                (float) (
                        time
                                * (
                                1.8
                                        + progress
                                                * progress
                                                * 44.0
                        )
                                + time
                                        * progress
                                        * progress
                                        * progress
                                        * 26.0
                                + phase
                                        * 180.0
                );

        pose.mulPose(
                Axis.YP.rotationDegrees(
                        rotation
                )
        );

        pose.mulPose(
                Axis.XP.rotationDegrees(
                        rotation * 0.47F
                )
        );

        pose.mulPose(
                Axis.ZP.rotationDegrees(
                        rotation * 0.22F
                )
        );

        var matrix =
                pose.last()
                        .pose();

        float core =
                0.30F
                        + progress
                                * 1.72F;

        cube(
                buffer,
                matrix,
                core * 1.72F,
                144,
                20,
                255,
                22
        );

        cube(
                buffer,
                matrix,
                core * 1.30F,
                185,
                40,
                255,
                72
        );

        cube(
                buffer,
                matrix,
                core,
                232,
                150,
                255,
                248
        );

        cube(
                buffer,
                matrix,
                core * 0.54F,
                255,
                222,
                255,
                252
        );

        /*
         * "Roots": spherical golden-angle rays made from short cube chains.
         * Their radius, branch count and jitter all rise with fusion progress,
         * giving the impression of cubic lightning growing out of the core.
         */
        int roots =
                12
                        + Math.round(
                                progress
                                        * 22.0F
                        );

        int segments =
                5
                        + Math.round(
                                progress
                                        * 8.0F
                        );

        for (int root = 0;
             root < roots;
             root++) {

            double y =
                    1.0
                            - 2.0
                                    * (
                                    root + 0.5
                            )
                                    / roots;

            double radial =
                    Math.sqrt(
                            Math.max(
                                    0.0,
                                    1.0
                                            - y * y
                            )
                    );

            double theta =
                    root
                            * GOLDEN_ANGLE
                            + phase
                            + time
                                    * (
                                    0.012
                                            + progress
                                                    * 0.055
                            );

            Vec3 direction =
                    new Vec3(
                            Math.cos(
                                    theta
                            )
                                    * radial,
                            y,
                            Math.sin(
                                    theta
                            )
                                    * radial
                    );

            Vec3 side =
                    new Vec3(
                            -direction.z,
                            direction.x
                                    * 0.22,
                            direction.x
                    );

            if (side.lengthSqr()
                    > 0.001) {

                side =
                        side.normalize();
            }

            for (int segment = 1;
                 segment <= segments;
                 segment++) {

                double fraction =
                        segment
                                / (double) segments;

                double distance =
                        core
                                + 0.55
                                + fraction
                                        * (
                                        3.2
                                                + progress
                                                        * 10.5
                                );

                double wobble =
                        Math.sin(
                                root * 2.31
                                        + segment * 1.47
                                        + time
                                                * 0.12
                        )
                                * 0.24
                                * (
                                0.25
                                        + progress
                        );

                Vec3 point =
                        direction.scale(
                                distance
                        )
                                .add(
                                        side.scale(
                                                wobble
                                        )
                                );

                pose.pushPose();

                pose.translate(
                        point.x,
                        point.y,
                        point.z
                );

                float half =
                        0.055F
                                + (
                                1.0F
                                        - (float) fraction
                        )
                                        * 0.085F
                                + progress
                                        * 0.025F;

                cube(
                        buffer,
                        pose.last()
                                .pose(),
                        half,
                        196,
                        70
                                + Math.round(
                                progress
                                        * 72.0F
                        ),
                        255,
                        150
                );

                pose.popPose();
            }
        }

        pose.popPose();
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
