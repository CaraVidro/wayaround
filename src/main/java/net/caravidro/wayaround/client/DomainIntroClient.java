package net.caravidro.wayaround.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.domain.DomainIntroManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class DomainIntroClient {

    private static byte style;
    private static long startedAtMillis;
    private static int durationTicks;

    private DomainIntroClient() {
    }

    public static void start(
            byte newStyle,
            int ticks
    ) {
        style =
                newStyle;

        durationTicks =
                Math.max(
                        1,
                        ticks
                );

        startedAtMillis =
                System.currentTimeMillis();
    }

    @SubscribeEvent
    public static void render(
            RenderGuiEvent.Post event
    ) {
        if (startedAtMillis <= 0L) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            startedAtMillis =
                    0L;
            return;
        }

        float progress =
                Mth.clamp(
                        (float) (
                                (
                                        System.currentTimeMillis()
                                                - startedAtMillis
                                )
                                        / 50.0
                                        / durationTicks
                        ),
                        0.0F,
                        1.0F
                );

        if (progress >= 1.0F) {
            startedAtMillis =
                    0L;
            return;
        }

        DomainIntroProfile profile =
                DomainIntroProfile.forStyle(
                        style
                );

        int width =
                minecraft.getWindow()
                        .getGuiScaledWidth();

        int height =
                minecraft.getWindow()
                        .getGuiScaledHeight();

        int centerX =
                width / 2;

        int top =
                Math.max(
                        20,
                        Math.round(
                                height * 0.15F
                        )
                );

        float open =
                envelope(
                        progress
                );

        int bandHeight =
                Math.max(
                        2,
                        Math.round(
                                Mth.lerp(
                                        open,
                                        2.0F,
                                        Math.max(
                                                64.0F,
                                                height * 0.31F
                                        )
                                )
                        )
                );

        int bottom =
                Math.min(
                        height - 4,
                        top + bandHeight
                );

        int halfLine =
                Math.round(
                        width
                                * 0.5F
                                * lineSpread(
                                progress
                        )
                );

        event.getGuiGraphics()
                .fill(
                        centerX - halfLine,
                        top,
                        centerX + halfLine,
                        top + 2,
                        profile.line()
                );

        if (bandHeight <= 4) {
            return;
        }

        event.getGuiGraphics()
                .fill(
                        0,
                        top + 2,
                        width,
                        bottom,
                        profile.background()
                );

        renderPlayer(
                event,
                centerX,
                top,
                bottom,
                open
        );

        event.getGuiGraphics()
                .enableScissor(
                        0,
                        top + 2,
                        width,
                        bottom
                );

        String phrase =
                "DOMÍNIO DE EXPANSÃO";

        int row =
                0;

        for (int y = top - 7;
             y < bottom + 16;
             y += 15) {

            int offset =
                    -120
                            + row * 37;

            int color =
                    switch (row % 3) {
                        case 1 ->
                                profile.secondaryText();
                        case 2 ->
                                profile.tertiaryText();
                        default ->
                                profile.primaryText();
                    };

            for (int x = offset;
                 x < width + 150;
                 x += 158) {
                event.getGuiGraphics()
                        .drawString(
                                minecraft.font,
                                phrase,
                                x,
                                y,
                                color,
                                false
                        );
            }

            row++;
        }

        event.getGuiGraphics()
                .disableScissor();

        event.getGuiGraphics()
                .fill(
                        0,
                        bottom - 2,
                        width,
                        bottom,
                        profile.line()
                );
    }

    private static float envelope(
            float progress
    ) {
        if (progress < 0.18F) {
            return smooth(
                    progress / 0.18F
            );
        }

        if (progress < 0.72F) {
            return 1.0F;
        }

        return 1.0F
                - smooth(
                (progress - 0.72F)
                        / 0.28F
        );
    }

    private static float lineSpread(
            float progress
    ) {
        if (progress < 0.12F) {
            return smooth(
                    progress / 0.12F
            );
        }

        if (progress < 0.88F) {
            return 1.0F;
        }

        return 1.0F
                - smooth(
                (progress - 0.88F)
                        / 0.12F
        );
    }

    private static float smooth(
            float value
    ) {
        float t =
                Mth.clamp(
                        value,
                        0.0F,
                        1.0F
                );

        return t * t
                * (
                3.0F
                        - 2.0F * t
        );
    }

    private static void renderPlayer(
            RenderGuiEvent.Post event,
            int centerX,
            int top,
            int bottom,
            float open
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (!(minecraft.player
                instanceof AbstractClientPlayer player)
                || bottom - top < 34) {
            return;
        }

        var baseRenderer =
                minecraft.getEntityRenderDispatcher()
                        .getRenderer(
                                player
                        );

        if (!(baseRenderer
                instanceof PlayerRenderer renderer)) {
            return;
        }

        @SuppressWarnings("unchecked")
        PlayerModel<AbstractClientPlayer> model =
                (PlayerModel<AbstractClientPlayer>)
                        renderer.getModel();

        float oldHeadX = model.head.xRot;
        float oldHeadY = model.head.yRot;
        float oldHeadZ = model.head.zRot;

        float oldLeftX = model.leftArm.xRot;
        float oldLeftY = model.leftArm.yRot;
        float oldLeftZ = model.leftArm.zRot;

        float oldRightX = model.rightArm.xRot;
        float oldRightY = model.rightArm.yRot;
        float oldRightZ = model.rightArm.zRot;

        try {
            model.setupAnim(
                    player,
                    0.0F,
                    0.0F,
                    player.tickCount,
                    0.0F,
                    0.0F
            );

            model.head.xRot =
                    0.0F;

            model.head.yRot =
                    0.0F;

            model.head.zRot =
                    0.0F;

            if (style
                    == DomainIntroManager.TUKUNA) {
                model.rightArm.xRot =
                        -1.50F;
                model.rightArm.yRot =
                        -0.45F;
                model.rightArm.zRot =
                        0.18F;

                model.leftArm.xRot =
                        -1.16F;
                model.leftArm.yRot =
                        0.62F;
                model.leftArm.zRot =
                        -0.32F;

            } else {
                model.rightArm.xRot =
                        -1.30F;
                model.rightArm.yRot =
                        -0.40F;
                model.rightArm.zRot =
                        0.10F;

                model.leftArm.xRot =
                        -1.30F;
                model.leftArm.yRot =
                        0.40F;
                model.leftArm.zRot =
                        -0.10F;
            }

            PoseStack pose =
                    event.getGuiGraphics()
                            .pose();

            pose.pushPose();

            event.getGuiGraphics()
                    .enableScissor(
                            0,
                            top + 2,
                            minecraft.getWindow()
                                    .getGuiScaledWidth(),
                            bottom
                    );

            float scale =
                    Math.min(
                            64.0F,
                            Math.max(
                                    34.0F,
                                    (bottom - top)
                                            * 0.62F
                            )
                    )
                            * open;

            pose.translate(
                    centerX,
                    bottom + 5.0F,
                    180.0F
            );

            pose.scale(
                    scale,
                    -scale,
                    scale
            );

            pose.mulPose(
                    Axis.YP.rotationDegrees(
                            180.0F
                    )
            );

            MultiBufferSource.BufferSource buffers =
                    minecraft.renderBuffers()
                            .bufferSource();

            model.renderToBuffer(
                    pose,
                    buffers.getBuffer(
                            RenderType.entityTranslucent(
                                    renderer.getTextureLocation(
                                            player
                                    )
                            )
                    ),
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    0xFFFFFFFF
            );

            buffers.endBatch();

            event.getGuiGraphics()
                    .disableScissor();

            pose.popPose();

        } finally {
            model.head.xRot = oldHeadX;
            model.head.yRot = oldHeadY;
            model.head.zRot = oldHeadZ;

            model.leftArm.xRot = oldLeftX;
            model.leftArm.yRot = oldLeftY;
            model.leftArm.zRot = oldLeftZ;

            model.rightArm.xRot = oldRightX;
            model.rightArm.yRot = oldRightY;
            model.rightArm.zRot = oldRightZ;
        }
    }
}
