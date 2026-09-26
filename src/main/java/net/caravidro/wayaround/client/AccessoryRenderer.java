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
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class AccessoryRenderer {

    private AccessoryRenderer() {}

    @SubscribeEvent
    public static void render(
            RenderPlayerEvent.Post event
    ) {
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
        RenderSystem.depthMask(false);
        RenderSystem.setShader(
                GameRenderer::getPositionColorShader
        );

        if ("spectral_glasses".equals(
                state.head()
        )) {
            renderGlasses(
                    pose,
                    model.head,
                    state.glassesMode()
            );
        }

        if ("work_gloves".equals(
                state.hands()
        )) {
            renderGlove(
                    pose,
                    model.leftArm,
                    true
            );

            renderGlove(
                    pose,
                    model.rightArm,
                    false
            );
        }

        if ("engineer_cape".equals(
                state.torso()
        )) {
            renderCape(
                    pose,
                    model.body,
                    event
            );
        }

        if ("wind_boots".equals(
                state.feet()
        )) {
            renderBoot(
                    pose,
                    model.leftLeg
            );

            renderBoot(
                    pose,
                    model.rightLeg
            );
        }

        RenderSystem.depthMask(
                true
        );

        RenderSystem.disableBlend();

        pose.popPose();
    }

    private static void renderGlasses(
            PoseStack pose,
            ModelPart head,
            int mode
    ) {
        pose.pushPose();

        head.translateAndRotate(
                pose
        );

        float y0 =
                mode == 0
                        ? -0.31F
                        : -0.48F;

        float y1 =
                mode == 0
                        ? -0.19F
                        : -0.39F;

        float z =
                -0.292F;

        BufferBuilder buffer =
                begin();

        Matrix4f matrix =
                pose.last()
                        .pose();

        quad(
                buffer,
                matrix,
                -0.215F,
                y0,
                -0.035F,
                y1,
                z,
                68,
                216,
                255,
                205
        );

        quad(
                buffer,
                matrix,
                0.035F,
                y0,
                0.215F,
                y1,
                z,
                68,
                216,
                255,
                205
        );

        quad(
                buffer,
                matrix,
                -0.035F,
                y0 + 0.045F,
                0.035F,
                y0 + 0.070F,
                z - 0.002F,
                26,
                30,
                38,
                255
        );

        draw(
                buffer
        );

        pose.popPose();
    }

    private static void renderGlove(
            PoseStack pose,
            ModelPart arm,
            boolean left
    ) {
        pose.pushPose();

        arm.translateAndRotate(
                pose
        );

        BufferBuilder buffer =
                begin();

        Matrix4f matrix =
                pose.last()
                        .pose();

        quad(
                buffer,
                matrix,
                left
                        ? -0.058F
                        : -0.183F,
                0.36F,
                left
                        ? 0.183F
                        : 0.058F,
                0.625F,
                -0.146F,
                54,
                47,
                39,
                245
        );

        draw(
                buffer
        );

        pose.popPose();
    }

    private static void renderBoot(
            PoseStack pose,
            ModelPart leg
    ) {
        pose.pushPose();

        leg.translateAndRotate(
                pose
        );

        BufferBuilder buffer =
                begin();

        Matrix4f matrix =
                pose.last()
                        .pose();

        quad(
                buffer,
                matrix,
                -0.128F,
                0.43F,
                0.128F,
                0.755F,
                -0.146F,
                58,
                92,
                120,
                245
        );

        draw(
                buffer
        );

        pose.popPose();
    }

    private static void renderCape(
            PoseStack pose,
            ModelPart body,
            RenderPlayerEvent.Post event
    ) {
        pose.pushPose();

        body.translateAndRotate(
                pose
        );

        double speed =
                event.getEntity()
                        .getDeltaMovement()
                        .horizontalDistance();

        float push =
                Mth.clamp(
                        (float) speed
                                * 2.8F,
                        0.0F,
                        0.78F
                );

        float wave =
                Mth.sin(
                        (
                                event.getEntity()
                                        .tickCount
                                        + event.getPartialTick()
                        )
                                * 0.24F
                )
                        * (
                        0.018F
                                + push
                                        * 0.04F
                );

        BufferBuilder buffer =
                begin();

        Matrix4f matrix =
                pose.last()
                        .pose();

        float width =
                0.29F;

        float z0 =
                0.151F;

        float z1 =
                0.165F
                        + push
                                * 0.18F
                        + wave;

        float z2 =
                0.18F
                        + push
                                * 0.40F
                        - wave;

        float z3 =
                0.19F
                        + push
                                * 0.66F
                        + wave;

        capeSegment(
                buffer,
                matrix,
                -width,
                width,
                0.02F,
                0.28F,
                z0,
                z1,
                112,
                73,
                38,
                238
        );

        capeSegment(
                buffer,
                matrix,
                -width,
                width,
                0.28F,
                0.55F,
                z1,
                z2,
                96,
                60,
                34,
                238
        );

        capeSegment(
                buffer,
                matrix,
                -width,
                width,
                0.55F,
                0.86F,
                z2,
                z3,
                78,
                46,
                29,
                235
        );

        draw(
                buffer
        );

        pose.popPose();
    }

    private static void capeSegment(
            BufferBuilder buffer,
            Matrix4f matrix,
            float x0,
            float x1,
            float y0,
            float y1,
            float z0,
            float z1,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        buffer.addVertex(
                matrix,
                x0,
                y0,
                z0
        ).setColor(
                red,
                green,
                blue,
                alpha
        );

        buffer.addVertex(
                matrix,
                x0,
                y1,
                z1
        ).setColor(
                red,
                green,
                blue,
                alpha
        );

        buffer.addVertex(
                matrix,
                x1,
                y1,
                z1
        ).setColor(
                red,
                green,
                blue,
                alpha
        );

        buffer.addVertex(
                matrix,
                x1,
                y0,
                z0
        ).setColor(
                red,
                green,
                blue,
                alpha
        );
    }

    private static BufferBuilder begin() {
        return Tesselator.getInstance()
                .begin(
                        VertexFormat.Mode.QUADS,
                        DefaultVertexFormat.POSITION_COLOR
                );
    }

    private static void draw(
            BufferBuilder buffer
    ) {
        BufferUploader.drawWithShader(
                buffer.buildOrThrow()
        );
    }

    private static void quad(
            BufferBuilder buffer,
            Matrix4f matrix,
            float x0,
            float y0,
            float x1,
            float y1,
            float z,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        buffer.addVertex(
                matrix,
                x0,
                y0,
                z
        ).setColor(
                red,
                green,
                blue,
                alpha
        );

        buffer.addVertex(
                matrix,
                x0,
                y1,
                z
        ).setColor(
                red,
                green,
                blue,
                alpha
        );

        buffer.addVertex(
                matrix,
                x1,
                y1,
                z
        ).setColor(
                red,
                green,
                blue,
                alpha
        );

        buffer.addVertex(
                matrix,
                x1,
                y0,
                z
        ).setColor(
                red,
                green,
                blue,
                alpha
        );
    }
}
