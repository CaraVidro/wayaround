package net.caravidro.wayaround.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.domain.DomainIntroManager;
import net.caravidro.wayaround.domain.VoidDomainPresentation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
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

    private static final ResourceLocation VOID_ABSOLUTE_IMAGE =
            ResourceLocation.fromNamespaceAndPath(
                    WayAround.MODID,
                    "textures/gui/domain/void_absolute.png"
            );

    private static final int VOID_ABSOLUTE_IMAGE_WIDTH =
            320;

    private static final int VOID_ABSOLUTE_IMAGE_HEIGHT =
            180;

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
        /*
         * Void has a server-owned three-stage presentation state. A local
         * speculative intro cannot know whether the player is INNATE, SIMPLE
         * or ABSOLUTE, so do not flash the wrong UI before the authoritative
         * S2C payload arrives.
         */
        if (newStyle
                == DomainIntroManager.VOID) {
            return;
        }

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

        if (style
                == DomainIntroManager.VOID
                && VoidDomainPresentation.byId(
                variant
        )
                == VoidDomainPresentation.INNATE) {
            renderVoidInnateAttempt(
                    event,
                    minecraft,
                    width,
                    height,
                    progress
            );
            return;
        }

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

            if (style == DomainIntroManager.VOID) {
                renderVoidCosmicPrelude(
                        event,
                        width,
                        top + 2,
                        bottom,
                        progress
                );
            }

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
                    open,
                    progress
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
        if (style
                == DomainIntroManager.VOID) {
            renderVoidRotatingBars(
                    event,
                    profile,
                    centerX,
                    top,
                    bottom,
                    halfLine,
                    progress,
                    bandHeight > 4
            );
        } else {
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
    }

    private static void renderVoidInnateAttempt(
            RenderGuiEvent.Post event,
            Minecraft minecraft,
            int width,
            int height,
            float progress
    ) {
        /*
         * INNATE is the "there is a domain inside you, but no Expansion has
         * been achieved" state. An attempted cast acknowledges the technique
         * name, hesitates, then explicitly fails. No bars, player model, image,
         * whiteout or pocket-space transition are rendered.
         */
        boolean failed =
                progress
                        >= 0.56F;

        float phaseProgress =
                failed
                        ? (progress - 0.56F)
                                / 0.44F
                        : progress
                                / 0.56F;

        float visibility =
                failed
                        ? 1.0F
                                - smooth(
                                Math.max(
                                        0.0F,
                                        (phaseProgress - 0.68F)
                                                / 0.32F
                                )
                        )
                        : smooth(
                                Math.min(
                                        1.0F,
                                        phaseProgress
                                                / 0.30F
                                )
                        );

        String text =
                failed
                        ? "NADA OCORREU."
                        : "DOMÍNIO DE EXPANSÃO";

        int alpha =
                Mth.clamp(
                        Math.round(
                                (
                                        failed
                                                ? 190.0F
                                                : 235.0F
                                )
                                        * visibility
                        ),
                        0,
                        235
                );

        if (alpha <= 0) {
            return;
        }

        float scale =
                failed
                        ? 1.20F
                        : 1.62F;

        PoseStack pose =
                event.getGuiGraphics()
                        .pose();

        pose.pushPose();

        pose.scale(
                scale,
                scale,
                1.0F
        );

        int scaledWidth =
                Math.round(
                        width
                                / scale
                );

        int scaledHeight =
                Math.round(
                        height
                                / scale
                );

        event.getGuiGraphics()
                .drawCenteredString(
                        minecraft.font,
                        text,
                        scaledWidth / 2,
                        scaledHeight / 2
                                - minecraft.font.lineHeight
                                        / 2,
                        alpha << 24
                                | (
                                failed
                                        ? 0x00BFC4CC
                                        : 0x00FFFFFF
                        )
                );

        pose.popPose();
    }

    private static void renderVoidRotatingBars(
            RenderGuiEvent.Post event,
            DomainIntroProfile profile,
            int centerX,
            int top,
            int bottom,
            int halfLine,
            float progress,
            boolean renderBottom
    ) {
        /*
         * Entry: two diagonals rotate into a clean horizontal lock.
         * Exit: they rotate away in opposite directions. The actor swaps pose
         * at this exact same threshold (0.72), so the UI and character motion
         * feel like one animation rather than unrelated layers.
         */
        float angle;

        if (progress < 0.18F) {
            angle =
                    42.0F
                            * (
                            1.0F
                                    - smooth(
                                    progress
                                            / 0.18F
                            )
                    );

        } else if (progress < 0.72F) {
            angle =
                    0.0F;

        } else {
            angle =
                    -58.0F
                            * smooth(
                            (progress - 0.72F)
                                    / 0.28F
                    );
        }

        PoseStack pose =
                event.getGuiGraphics()
                        .pose();

        pose.pushPose();

        pose.translate(
                centerX,
                top + 1.0F,
                0.0F
        );

        pose.mulPose(
                Axis.ZP.rotationDegrees(
                        angle
                )
        );

        event.getGuiGraphics()
                .fill(
                        -halfLine,
                        -1,
                        halfLine,
                        1,
                        profile.line()
                );

        pose.popPose();

        if (!renderBottom) {
            return;
        }

        pose.pushPose();

        pose.translate(
                centerX,
                bottom - 1.0F,
                0.0F
        );

        pose.mulPose(
                Axis.ZP.rotationDegrees(
                        -angle
                )
        );

        event.getGuiGraphics()
                .fill(
                        -halfLine,
                        -1,
                        halfLine,
                        1,
                        profile.line()
                );

        pose.popPose();
    }

    private static void renderVoidCosmicPrelude(
            RenderGuiEvent.Post event,
            int width,
            int top,
            int bottom,
            float progress
    ) {
        if (bottom <= top) {
            return;
        }

        int centerX =
                width / 2;

        int centerY =
                top
                        + (bottom - top) / 2;

        long time =
                System.currentTimeMillis();

        event.getGuiGraphics()
                .enableScissor(
                        0,
                        top,
                        width,
                        bottom
                );

        /*
         * Tiny deterministic stars. They are intentionally square so the GUI
         * already speaks the same blocky visual language as the 3D domain.
         */
        for (int index = 0;
             index < 72;
             index++) {

            long h =
                    mixPrelude(
                            index
                                    * 0x9E3779B97F4A7C15L
                    );

            int x =
                    Math.floorMod(
                            (int) (h >>> 13),
                            Math.max(
                                    1,
                                    width
                            )
                    );

            int y =
                    top
                            + Math.floorMod(
                            (int) (h >>> 37),
                            Math.max(
                                    1,
                                    bottom - top
                            )
                    );

            int size =
                    (h & 31L) == 0L
                            ? 2
                            : 1;

            int alpha =
                    120
                            + (int) (h & 95L);

            event.getGuiGraphics()
                    .fill(
                            x,
                            y,
                            x + size,
                            y + size,
                            alpha << 24
                                    | 0xDDE8FF
                    );
        }

        /*
         * Broken orbiting matter around the central black aperture.
         */
        for (int index = 0;
             index < 82;
             index++) {

            long h =
                    mixPrelude(
                            0xD1B54A32D192ED03L
                                    + index
                                    * 0x94D049BB133111EBL
                    );

            double orbit =
                    22.0
                            + (h & 0xFFL)
                            / 255.0
                            * Math.min(
                            width * 0.30,
                            116.0
                    );

            double angle =
                    index
                            * 2.399963229728653
                            + time
                            / 1700.0
                            * (
                            (index & 1) == 0
                                    ? 1.0
                                    : -0.55
                    );

            double flatten =
                    0.24
                            + ((h >>> 11) & 31L)
                            / 31.0
                            * 0.16;

            int x =
                    centerX
                            + (int) Math.round(
                            Math.cos(
                                    angle
                            )
                                    * orbit
                    );

            int y =
                    centerY
                            + (int) Math.round(
                            Math.sin(
                                    angle
                            )
                                    * orbit
                                    * flatten
                    );

            int size =
                    1
                            + (int) ((h >>> 20) & 3L);

            boolean dark =
                    (h & 7L) == 0L;

            int color =
                    dark
                            ? 0xE9000006
                            : (
                            (h & 15L) == 1L
                                    ? 0xD0FFF0A8
                                    : 0xBFDCE8FF
                    );

            event.getGuiGraphics()
                    .fill(
                            x,
                            y,
                            x + size,
                            y + size,
                            color
                    );
        }

        int apertureW =
                Math.max(
                        18,
                        Math.round(
                                width
                                        * 0.055F
                        )
                );

        int apertureH =
                Math.max(
                        24,
                        Math.round(
                                (bottom - top)
                                        * 0.58F
                        )
                );

        int left =
                centerX
                        - apertureW / 2;

        int right =
                centerX
                        + apertureW / 2;

        int apertureTop =
                centerY
                        - apertureH / 2;

        int apertureBottom =
                centerY
                        + apertureH / 2;

        /*
         * Warm double outline around the literal black rectangular void.
         */
        event.getGuiGraphics()
                .fill(
                        left - 3,
                        apertureTop - 3,
                        right + 3,
                        apertureBottom + 3,
                        0x66FFF2A2
                );

        event.getGuiGraphics()
                .fill(
                        left - 1,
                        apertureTop - 1,
                        right + 1,
                        apertureBottom + 1,
                        0xFFFFE79A
                );

        event.getGuiGraphics()
                .fill(
                        left,
                        apertureTop,
                        right,
                        apertureBottom,
                        0xFF000000
                );

        /*
         * Small pulse as the two UI bars reach their open position.
         */
        if (progress > 0.16F
                && progress < 0.84F) {

            int pulse =
                    18
                            + (int) (
                            8.0
                                    * Math.sin(
                                    time
                                            / 115.0
                            )
                    );

            event.getGuiGraphics()
                    .fill(
                            left - pulse,
                            centerY - 1,
                            left - 4,
                            centerY + 1,
                            0x55DDE8FF
                    );

            event.getGuiGraphics()
                    .fill(
                            right + 4,
                            centerY - 1,
                            right + pulse,
                            centerY + 1,
                            0x55DDE8FF
                    );
        }

        event.getGuiGraphics()
                .disableScissor();
    }

    private static long mixPrelude(
            long value
    ) {
        value ^=
                value >>> 30;

        value *=
                0xBF58476D1CE4E5B9L;

        value ^=
                value >>> 27;

        value *=
                0x94D049BB133111EBL;

        return value
                ^ value >>> 31;
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

        int availableWidth =
                Math.max(
                        1,
                        width
                );

        int availableHeight =
                Math.max(
                        1,
                        bottom - top
                );

        /*
         * Preserve the original art aspect ratio. The old implementation
         * independently stretched X/Y to fill the band, which turned every
         * Domain background into rubber. Uniform cover-scaling plus scissoring
         * keeps the composition intact and crops only the excess edges.
         */
        float scale =
                Math.max(
                        availableWidth
                                / (float) profile.imageWidth(),
                        availableHeight
                                / (float) profile.imageHeight()
                );

        float drawWidth =
                profile.imageWidth()
                        * scale;

        float drawHeight =
                profile.imageHeight()
                        * scale;

        float drawX =
                (availableWidth - drawWidth)
                        * 0.5F;

        float drawY =
                top
                        + (availableHeight - drawHeight)
                                * 0.5F;

        event.getGuiGraphics()
                .enableScissor(
                        0,
                        top,
                        width,
                        bottom
                );

        PoseStack pose =
                event.getGuiGraphics()
                        .pose();

        pose.pushPose();

        pose.translate(
                drawX,
                drawY,
                0.0F
        );

        pose.scale(
                scale,
                scale,
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

        event.getGuiGraphics()
                .disableScissor();
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
                                withAlpha(
                                        profile.secondaryText(),
                                        68
                                );
                        case 2 ->
                                withAlpha(
                                        profile.tertiaryText(),
                                        50
                                );
                        default ->
                                withAlpha(
                                        profile.primaryText(),
                                        84
                                );
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

    private static int withAlpha(
            int color,
            int alpha
    ) {
        return Mth.clamp(
                alpha,
                0,
                255
        ) << 24
                | color
                        & 0x00FFFFFF;
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
            float open,
            float progress
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

            byte activePose =
                    style
                            == DomainIntroManager.VOID
                            && progress >= 0.72F
                            ? DomainIntroProfile.POSE_VOID_EXIT
                            : profile.pose();

            applyPose(
                    model,
                    activePose
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

            /*
             * PlayerModel is already authored in model-space with Y pointing
             * down from the head toward the feet. Mirroring GUI Y here flips
             * the entire character upside-down. Keep Y positive and flip Z
             * only, matching the usual inventory/entity GUI convention.
             */
            pose.scale(
                    scale,
                    scale,
                    -scale
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

            case DomainIntroProfile.POSE_VOID_EXIT -> {
                /*
                 * Exit stance: the hands break away from the casting seal as
                 * the two GUI bars rotate out. It is intentionally distinct
                 * from both SIMPLE and ABSOLUTE hold poses.
                 */
                model.rightArm.xRot =
                        -0.76F;
                model.rightArm.yRot =
                        -1.02F;
                model.rightArm.zRot =
                        0.86F;

                model.leftArm.xRot =
                        -0.76F;
                model.leftArm.yRot =
                        1.02F;
                model.leftArm.zRot =
                        -0.86F;

                model.head.xRot =
                        0.18F;

                model.head.yRot =
                        0.18F;
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
