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
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.joml.Matrix4f;

/**
 * Procedural 3D accessories attached directly to vanilla PlayerModel bones.
 *
 * No flat "sticker" quads: glasses, gloves and boots are small volumetric
 * cuboids, while the engineer cape is a chain of thin hinged cuboids whose
 * angle responds to player velocity.
 */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class AccessoryRenderer {

    private AccessoryRenderer() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void render(
            RenderPlayerEvent.Post event
    ) {
        if (!WorldFeatureRuntime.clientEnabled(
                WorldFeature.ACCESSORIES
        )) {
            return;
        }

        AccessoryClientState.State state =
                AccessoryClientState.get(
                        event.getEntity()
                                .getUUID()
                );

        if (state == null
                || (
                state.head().isBlank()
                        && state.hands().isBlank()
                        && state.torso().isBlank()
                        && state.feet().isBlank()
        )) {
            return;
        }

        PoseStack pose =
                event.getPoseStack();

        PlayerModel<?> model =
                event.getRenderer()
                        .getModel();

        pose.pushPose();

        /*
         * RenderPlayerEvent.Post fires after vanilla unwinds the living model
         * transform. Rebuild it before attaching boxes to ModelPart pivots.
         */
        float bodyYaw =
                Mth.rotLerp(
                        event.getPartialTick(),
                        event.getEntity().yBodyRotO,
                        event.getEntity().yBodyRot
                );

        pose.mulPose(
                Axis.YP.rotationDegrees(
                        180.0F - bodyYaw
                )
        );

        pose.scale(
                -1.0F,
                -1.0F,
                1.0F
        );

        pose.translate(
                0.0D,
                -1.501D,
                0.0D
        );

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask(
                true
        );
        RenderSystem.setShader(
                GameRenderer::getPositionColorShader
        );

        BufferBuilder buffer =
                Tesselator.getInstance()
                        .begin(
                                VertexFormat.Mode.QUADS,
                                DefaultVertexFormat.POSITION_COLOR
                        );

        if ("spectral_glasses".equals(
                state.head()
        )) {
            renderGlasses(
                    buffer,
                    pose,
                    model.head,
                    state.glassesMode()
            );
        }

        if ("work_gloves".equals(
                state.hands()
        )) {
            renderGlove(
                    buffer,
                    pose,
                    model.leftArm
            );

            renderGlove(
                    buffer,
                    pose,
                    model.rightArm
            );
        }

        if ("engineer_cape".equals(
                state.torso()
        )) {
            renderCape(
                    buffer,
                    pose,
                    model.body,
                    event
            );
        }

        if ("wind_boots".equals(
                state.feet()
        )) {
            renderBoot(
                    buffer,
                    pose,
                    model.leftLeg
            );

            renderBoot(
                    buffer,
                    pose,
                    model.rightLeg
            );
        }

        BufferUploader.drawWithShader(
                buffer.buildOrThrow()
        );

        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        pose.popPose();
    }

    private static void renderGlasses(
            BufferBuilder buffer,
            PoseStack pose,
            ModelPart head,
            int mode
    ) {
        pose.pushPose();

        head.translateAndRotate(
                pose
        );

        if (mode == 1) {
            /*
             * Forehead/crown mode: move the frame up and tip it slightly back,
             * as if the player pushed the glasses above the eyes.
             */
            pose.translate(
                    0.0F,
                    -0.115F,
                    0.020F
            );

            pose.mulPose(
                    Axis.XP.rotationDegrees(
                            -13.0F
                    )
            );
        }

        Matrix4f matrix =
                pose.last()
                        .pose();

        float y0 =
                -0.315F;

        float y1 =
                -0.185F;

        // Dark frame around each lens.
        box(
                buffer,
                matrix,
                -0.238F,
                y0,
                -0.316F,
                -0.018F,
                y1,
                -0.282F,
                24, 29, 38, 255
        );

        box(
                buffer,
                matrix,
                0.018F,
                y0,
                -0.316F,
                0.238F,
                y1,
                -0.282F,
                24, 29, 38, 255
        );

        // Slightly smaller translucent blue glass sitting inside the frame.
        box(
                buffer,
                matrix,
                -0.216F,
                y0 + 0.020F,
                -0.320F,
                -0.040F,
                y1 - 0.020F,
                -0.316F,
                68, 208, 255, 145
        );

        box(
                buffer,
                matrix,
                0.040F,
                y0 + 0.020F,
                -0.320F,
                0.216F,
                y1 - 0.020F,
                -0.316F,
                68, 208, 255, 145
        );

        // Bridge.
        box(
                buffer,
                matrix,
                -0.040F,
                -0.272F,
                -0.321F,
                0.040F,
                -0.238F,
                -0.288F,
                24, 29, 38, 255
        );

        // Short 3D temples wrapping toward the sides of the head.
        box(
                buffer,
                matrix,
                -0.264F,
                -0.278F,
                -0.284F,
                -0.226F,
                -0.236F,
                0.055F,
                24, 29, 38, 255
        );

        box(
                buffer,
                matrix,
                0.226F,
                -0.278F,
                -0.284F,
                0.264F,
                -0.236F,
                0.055F,
                24, 29, 38, 255
        );

        pose.popPose();
    }

    private static void renderGlove(
            BufferBuilder buffer,
            PoseStack pose,
            ModelPart arm
    ) {
        pose.pushPose();

        arm.translateAndRotate(
                pose
        );

        Matrix4f matrix =
                pose.last()
                        .pose();

        // Main glove shell around the lower third of the arm.
        box(
                buffer,
                matrix,
                -0.142F,
                0.455F,
                -0.142F,
                0.142F,
                0.755F,
                0.142F,
                62, 52, 43, 255
        );

        // Raised cuff gives the otherwise simple model a clear 3D silhouette.
        box(
                buffer,
                matrix,
                -0.154F,
                0.425F,
                -0.154F,
                0.154F,
                0.505F,
                0.154F,
                93, 73, 52, 255
        );

        // Small steel plate on the back of the hand.
        box(
                buffer,
                matrix,
                -0.090F,
                0.560F,
                -0.162F,
                0.090F,
                0.690F,
                -0.142F,
                118, 124, 129, 255
        );

        pose.popPose();
    }

    private static void renderBoot(
            BufferBuilder buffer,
            PoseStack pose,
            ModelPart leg
    ) {
        pose.pushPose();

        leg.translateAndRotate(
                pose
        );

        Matrix4f matrix =
                pose.last()
                        .pose();

        // Boot shaft.
        box(
                buffer,
                matrix,
                -0.144F,
                0.430F,
                -0.145F,
                0.144F,
                0.755F,
                0.150F,
                48, 73, 95, 255
        );

        // Toe extends toward the player's front (-Z).
        box(
                buffer,
                matrix,
                -0.148F,
                0.615F,
                -0.270F,
                0.148F,
                0.755F,
                0.150F,
                57, 89, 116, 255
        );

        // Thin sole.
        box(
                buffer,
                matrix,
                -0.158F,
                0.735F,
                -0.282F,
                0.158F,
                0.790F,
                0.158F,
                24, 28, 33, 255
        );

        pose.popPose();
    }

    private static void renderCape(
            BufferBuilder buffer,
            PoseStack pose,
            ModelPart body,
            RenderPlayerEvent.Post event
    ) {
        pose.pushPose();

        body.translateAndRotate(
                pose
        );

        Vec3 movement =
                event.getEntity()
                        .getDeltaMovement();

        float horizontalSpeed =
                Mth.clamp(
                        (float) movement.horizontalDistance(),
                        0.0F,
                        0.62F
                );

        float speedWeight =
                Mth.clamp(
                        horizontalSpeed
                                * 3.6F,
                        0.0F,
                        1.0F
                );

        float fallLift =
                Mth.clamp(
                        (float) (
                                -movement.y
                                        * 18.0
                        ),
                        -7.0F,
                        17.0F
                );

        float time =
                event.getEntity()
                        .tickCount
                        + event.getPartialTick();

        float flutter =
                Mth.sin(
                        time
                                * (
                                0.24F
                                        + speedWeight
                                                * 0.42F
                        )
                )
                        * (
                        1.4F
                                + speedWeight
                                        * 4.8F
                );

        // Metal clasps at the shoulders.
        Matrix4f bodyMatrix =
                pose.last()
                        .pose();

        box(
                buffer,
                bodyMatrix,
                -0.245F,
                0.015F,
                0.128F,
                -0.145F,
                0.120F,
                0.190F,
                128, 119, 102, 255
        );

        box(
                buffer,
                bodyMatrix,
                0.145F,
                0.015F,
                0.128F,
                0.245F,
                0.120F,
                0.190F,
                128, 119, 102, 255
        );

        /*
         * The cloth is four thin 3D cuboids hinged together. Each segment
         * inherits the transform of the previous one, creating a soft curve
         * instead of a rigid rotating board.
         */
        pose.translate(
                0.0F,
                0.055F,
                0.168F
        );

        float baseAngle =
                4.0F
                        + speedWeight
                                * 27.0F
                        + fallLift;

        float hingeAngle =
                1.8F
                        + speedWeight
                                * 5.2F;

        int[][] colors = {
                {116, 76, 39},
                {104, 66, 34},
                {91, 55, 31},
                {77, 44, 27}
        };

        for (int segment = 0;
             segment < 4;
             segment++) {
            float localFlutter =
                    flutter
                            * (
                            0.30F
                                    + segment
                                            * 0.22F
                    );

            float angle =
                    segment == 0
                            ? baseAngle
                                    + localFlutter
                            : hingeAngle
                                    + localFlutter;

            pose.mulPose(
                    Axis.XP.rotationDegrees(
                            angle
                    )
            );

            float halfWidth =
                    0.285F
                            - segment
                                    * 0.009F;

            int[] color =
                    colors[segment];

            box(
                    buffer,
                    pose.last()
                            .pose(),
                    -halfWidth,
                    0.0F,
                    -0.024F,
                    halfWidth,
                    0.205F,
                    0.024F,
                    color[0],
                    color[1],
                    color[2],
                    255
            );

            // Narrow seam at each hinge makes the articulated model readable.
            if (segment < 3) {
                box(
                        buffer,
                        pose.last()
                                .pose(),
                        -halfWidth,
                        0.190F,
                        -0.030F,
                        halfWidth,
                        0.215F,
                        0.030F,
                        52, 34, 25, 255
                );
            }

            pose.translate(
                    0.0F,
                    0.205F,
                    0.0F
            );
        }

        pose.popPose();
    }

    /**
     * Adds a real cuboid (six faces) to the current POSITION_COLOR buffer.
     */
    private static void box(
            BufferBuilder buffer,
            Matrix4f matrix,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        // Front (-Z).
        face(
                buffer, matrix,
                x0, y0, z0,
                x0, y1, z0,
                x1, y1, z0,
                x1, y0, z0,
                red, green, blue, alpha
        );

        // Back (+Z).
        face(
                buffer, matrix,
                x1, y0, z1,
                x1, y1, z1,
                x0, y1, z1,
                x0, y0, z1,
                red, green, blue, alpha
        );

        // Left.
        face(
                buffer, matrix,
                x0, y0, z1,
                x0, y1, z1,
                x0, y1, z0,
                x0, y0, z0,
                red, green, blue, alpha
        );

        // Right.
        face(
                buffer, matrix,
                x1, y0, z0,
                x1, y1, z0,
                x1, y1, z1,
                x1, y0, z1,
                red, green, blue, alpha
        );

        // Top.
        face(
                buffer, matrix,
                x0, y0, z1,
                x0, y0, z0,
                x1, y0, z0,
                x1, y0, z1,
                red, green, blue, alpha
        );

        // Bottom.
        face(
                buffer, matrix,
                x0, y1, z0,
                x0, y1, z1,
                x1, y1, z1,
                x1, y1, z0,
                red, green, blue, alpha
        );
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
        buffer.addVertex(
                matrix,
                ax, ay, az
        ).setColor(
                red, green, blue, alpha
        );

        buffer.addVertex(
                matrix,
                bx, by, bz
        ).setColor(
                red, green, blue, alpha
        );

        buffer.addVertex(
                matrix,
                cx, cy, cz
        ).setColor(
                red, green, blue, alpha
        );

        buffer.addVertex(
                matrix,
                dx, dy, dz
        ).setColor(
                red, green, blue, alpha
        );
    }
}
