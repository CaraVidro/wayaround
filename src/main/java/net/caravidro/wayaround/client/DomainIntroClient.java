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
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
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
    private static byte variant;
    private static long startedAtMillis;
    private static int durationTicks;

    private DomainIntroClient() {
    }

    /**
     * Local speculative opening used by Tobias. A later S2C confirmation for
     * the same style does not restart the animation.
     */
    public static void previewLocal(
            byte newStyle,
            byte newVariant,
            int ticks
    ) {
        startInternal(
                newStyle,
                newVariant,
                ticks
        );
    }

    public static void start(
            byte newStyle,
            byte newVariant,
            int ticks
    ) {
        startInternal(
                newStyle,
                newVariant,
                ticks
        );
    }

    private static void startInternal(
            byte newStyle,
            byte newVariant,
            int ticks
    ) {
        long now =
                System.currentTimeMillis();

        if (startedAtMillis > 0L
                && style == newStyle) {
            long elapsed =
                    now - startedAtMillis;

            long duration =
                    Math.max(
                            1,
                            durationTicks
                    ) * 50L;

            if (elapsed < duration) {
                variant =
                        newVariant;

                durationTicks =
                        Math.max(
                                durationTicks,
                                ticks
                        );

                return;
            }
        }

        style =
                newStyle;

        variant =
                newVariant;

        durationTicks =
                Math.max(
                        1,
                        ticks
                );

        startedAtMillis =
                now;
    }

    public static void cancel() {
        startedAtMillis = 0L;
    }

    public static boolean isRunning() {
        return startedAtMillis > 0L;
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

        if (minecraft.player == null
                || minecraft.options.hideGui) {
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
                        style,
                        variant
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
                                                68.0F,
                                                height * 0.32F
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

        if (bandHeight > 4) {
            event.getGuiGraphics()
                    .fill(
                            0,
                            top + 2,
                            width,
                            bottom,
                            profile.background()
                    );

            renderFutureImage(
                    event,
                    profile,
                    width,
                    top + 2,
                    bottom
            );

            /*
             * Player is rendered BEFORE lettering and border lines so he only
             * exists inside the graphic rather than sitting on top of the UI.
             */
            renderPlayer(
                    event,
                    profile,
                    centerX,
                    top,
                    bottom,
                    open
            );

            if (profile.shadowParticles()) {
                renderShadowParticles(
                        event,
                        centerX,
                        top,
                        bottom,
                        open
                );
            }

            renderRepeatedText(
                    event,
                    profile,
                    width,
                    top,
                    bottom
            );
        }

        // Borders are deliberately last: the model stays behind both lines.
        event.getGuiGraphics()
                .fill(
                        centerX - halfLine,
                        top,
                        centerX + halfLine,
                        top + 2,
                        profile.line()
                );

        if (bandHeight > 4) {
            event.getGuiGraphics()
                    .fill(
                            0,
                            bottom - 2,
                            width,
                            bottom,
                            profile.line()
                    );
        }
    }

    private static void renderFutureImage(
            RenderGuiEvent.Post event,
            DomainIntroProfile profile,
            int width,
            int top,
            int bottom
    ) {
        if (!profile.useFutureImage()) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.getResourceManager()
                .getResource(
                        profile.futureImage()
                )
                .isEmpty()) {
            /*
             * Intentionally silent: the three art files are user-provided.
             * The text/pose UI remains the fallback until they are dropped in.
             */
            return;
        }

        int imageHeight =
                Math.max(
                        1,
                        bottom - top
                );

        PoseStack pose =
                event.getGuiGraphics()
                        .pose();

        pose.pushPose();
        pose.translate(
                0.0F,
                top,
                0.0F
        );

        pose.scale(
                width
                        / (float) profile.imageWidth(),
                imageHeight
                        / (float) profile.imageHeight(),
                1.0F
        );

        event.getGuiGraphics()
                .blit(
                        profile.futureImage(),
                        0,
                        0,
                        0,
                        0,
                        profile.imageWidth(),
                        profile.imageHeight(),
                        profile.imageWidth(),
                        profile.imageHeight()
                );

        pose.popPose();
    }

    private static void renderRepeatedText(
            RenderGuiEvent.Post event,
            DomainIntroProfile profile,
            int width,
            int top,
            int bottom
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        event.getGuiGraphics()
                .enableScissor(
                        0,
                        top + 2,
                        width,
                        bottom
                );

        String phrase =
                profile.phrase();

        int phraseStep =
                Math.max(
                        165,
                        minecraft.font.width(
                                phrase
                        ) + 42
                );

        int row =
                0;

        for (int y = top - 7;
             y < bottom + 16;
             y += 15) {

            int offset =
                    -phraseStep
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
                 x < width + phraseStep;
                 x += phraseStep) {
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
            DomainIntroProfile profile,
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

            applyPose(
                    model,
                    profile.pose()
            );

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
                            68.0F,
                            Math.max(
                                    36.0F,
                                    (bottom - top)
                                            * 0.64F
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

    private static void applyPose(
            PlayerModel<AbstractClientPlayer> model,
            byte pose
    ) {
        model.head.xRot =
                0.0F;

        model.head.yRot =
                0.0F;

        model.head.zRot =
                0.0F;

        switch (pose) {
            case DomainIntroProfile.POSE_VOID_APEX -> {
                model.rightArm.xRot =
                        -2.18F;
                model.rightArm.yRot =
                        -0.72F;
                model.rightArm.zRot =
                        0.34F;

                model.leftArm.xRot =
                        -1.74F;
                model.leftArm.yRot =
                        0.78F;
                model.leftArm.zRot =
                        -0.50F;

                model.head.xRot =
                        -0.10F;
            }

            case DomainIntroProfile.POSE_TUKUNA_APEX -> {
                model.rightArm.xRot =
                        -2.35F;
                model.rightArm.yRot =
                        -0.92F;
                model.rightArm.zRot =
                        0.48F;

                model.leftArm.xRot =
                        -1.42F;
                model.leftArm.yRot =
                        0.88F;
                model.leftArm.zRot =
                        -0.58F;

                model.head.xRot =
                        0.12F;
            }

            case DomainIntroProfile.POSE_SHADOWS -> {
                // Both hands low, chin raised toward the top of the frame.
                model.rightArm.xRot =
                        0.16F;
                model.rightArm.yRot =
                        -0.08F;
                model.rightArm.zRot =
                        0.12F;

                model.leftArm.xRot =
                        0.16F;
                model.leftArm.yRot =
                        0.08F;
                model.leftArm.zRot =
                        -0.12F;

                model.head.xRot =
                        -0.48F;
            }

            default -> {
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
            }
        }
    }

    private static void renderShadowParticles(
            RenderGuiEvent.Post event,
            int centerX,
            int top,
            int bottom,
            float open
    ) {
        if (open <= 0.05F) {
            return;
        }

        long time =
                System.currentTimeMillis();

        int span =
                Math.max(
                        1,
                        bottom - top
                );

        event.getGuiGraphics()
                .enableScissor(
                        0,
                        top + 2,
                        Minecraft.getInstance()
                                .getWindow()
                                .getGuiScaledWidth(),
                        bottom
                );

        for (int i = 0;
             i < 18;
             i++) {
            int side =
                    (i & 1) == 0
                            ? -1
                            : 1;

            int x =
                    centerX
                            + side
                            * (
                            10
                                    + (
                                    i % 5
                            ) * 3
                    );

            int cycle =
                    (int) (
                            (
                                    time / 12L
                                            + i * 19L
                            )
                                    % Math.max(
                                    24,
                                    span
                            )
                    );

            int y =
                    top
                            + span / 2
                            + cycle / 2;

            int length =
                    2
                            + i % 4;

            event.getGuiGraphics()
                    .fill(
                            x,
                            y,
                            x + 2,
                            y + length,
                            0xE8000000
                    );
        }

        event.getGuiGraphics()
                .disableScissor();
    }
}
