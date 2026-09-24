package net.caravidro.wayaround.media.client;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.media.TelevisionBlock;
import net.caravidro.wayaround.media.TelevisionBlockEntity;
import net.caravidro.wayaround.media.VhsData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;

public final class TelevisionRenderer
        implements BlockEntityRenderer<TelevisionBlockEntity> {

    private static final Map<BlockPos, ScreenTexture>
            CACHE =
            new HashMap<>();

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy HH:mm"
            );

    private static long lastCleanupTick;

    private static final int FULLBRIGHT =
            15728880;

    private static final java.util.Set<String>
            REPORTED_ERRORS =
            new java.util.HashSet<>();

    private final ItemRenderer itemRenderer;
    private final Font font;

    public TelevisionRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        this.itemRenderer =
                context.getItemRenderer();

        this.font =
                context.getFont();
    }

    @Override
    public void render(
            TelevisionBlockEntity television,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        if (television.getLevel() == null) {
            return;
        }

        VhsData.Info info =
                VhsData.read(
                        television.tape()
                )
                        .orElse(null);

        if (info == null) {
            release(
                    television.getBlockPos()
            );
            return;
        }

        renderTape(
                television,
                partialTick,
                poseStack,
                buffers,
                packedLight
        );

        if (television.isEjected()) {
            release(
                    television.getBlockPos()
            );
            return;
        }

        Path path =
                RecordingStore.find(
                        info.recordingId()
                )
                        .orElse(null);

        if (path == null) {
            reportOnce(
                    "missing:"
                            + info.recordingId(),
                    "TV em "
                            + television.getBlockPos()
                            + " recebeu VHS "
                            + info.recordingId()
                            + ", mas o arquivo .wavr nao existe neste cliente."
            );

            release(
                    television.getBlockPos()
            );

            return;
        }

        ScreenTexture screen =
                CACHE.get(
                        television.getBlockPos()
                );

        if (screen == null
                || !screen.recordingId
                .equals(
                        info.recordingId()
                )) {

            release(
                    television.getBlockPos()
            );

            try {
                screen =
                        new ScreenTexture(
                                info.recordingId(),
                                path,
                                television.getBlockPos()
                        );

                CACHE.put(
                        television.getBlockPos(),
                        screen
                );

            } catch (Exception exception) {
                reportOnce(
                        "open:"
                                + info.recordingId(),
                        "Falha abrindo gravacao "
                                + info.recordingId()
                                + " na TV: "
                                + exception.getClass()
                                        .getSimpleName()
                                + ": "
                                + exception.getMessage()
                );

                return;
            }
        }

        screen.lastUsedTick =
                television.getLevel()
                        .getGameTime();

        if (television.isCountingDown()) {
            int countdown =
                    television.countdownNumber();

            if (countdown
                    != screen.lastCountdown) {

                VhsFilter.renderCountdown(
                        screen.image,
                        countdown,
                        screen.filterSeed
                );

                screen.texture.upload();

                screen.lastCountdown =
                        countdown;

                screen.lastFrame =
                        -1;
            }

        } else if (television.isPlaying()) {
            long elapsedTicks =
                    Math.max(
                            0L,
                            television.getLevel()
                                    .getGameTime()
                                    - television
                                    .playbackStartGameTime()
                    );

            int targetFrame =
                    (int) Math.min(
                            Math.max(
                                    0,
                                    screen.reader
                                            .frameCount()
                                            - 1
                            ),
                            elapsedTicks
                                    * screen.reader
                                    .fps()
                                    / 20L
                    );

            if (targetFrame
                    != screen.lastFrame
                    && screen.reader
                    .frameCount() > 0) {

                try {
                    screen.reader
                            .readFrame(
                                    targetFrame,
                                    screen.image
                            );

                    VhsFilter.apply(
                            screen.image,
                            targetFrame,
                            screen.filterSeed
                    );

                    screen.texture.upload();

                    screen.lastFrame =
                            targetFrame;

                    screen.lastCountdown =
                            -1;

                } catch (Exception exception) {
                    reportOnce(
                            "frame:"
                                    + info.recordingId(),
                            "Falha lendo frame da gravacao "
                                    + info.recordingId()
                                    + ": "
                                    + exception.getClass()
                                            .getSimpleName()
                                    + ": "
                                    + exception.getMessage()
                    );
                }
            }
        }

        if (television.isCountingDown()
                || television.isPlaying()) {

            renderScreen(
                    television,
                    poseStack,
                    buffers,
                    screen.location
            );
        }

        if (television.isPlaying()) {
            renderMetadata(
                    television,
                    info,
                    poseStack,
                    buffers
            );
        }

        long currentTick =
                television.getLevel()
                        .getGameTime();

        if (currentTick
                - lastCleanupTick
                > 200L) {

            lastCleanupTick =
                    currentTick;

            cleanup(
                    currentTick
            );
        }
    }

    private void renderTape(
            TelevisionBlockEntity television,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int packedLight
    ) {
        if (television.tape()
                .isEmpty()) {

            return;
        }

        float progress =
                television.tapeVisualProgress(
                        partialTick
                );

        if (progress <= 0.015F
                && !television.isEjected()) {

            return;
        }

        pose.pushPose();

        transformFront(
                television,
                pose
        );

        /*
         * 0 = cassette completely inside the VCR slot.
         * 1 = cassette protruding far enough to grab.
         */
        pose.translate(
                0.46,
                0.115,
                0.17
                        - progress
                        * 0.30
        );

        pose.scale(
                0.48F,
                0.48F,
                0.48F
        );

        itemRenderer.renderStatic(
                television.tape(),
                ItemDisplayContext.FIXED,
                Math.max(
                        packedLight,
                        0x00D000D0
                ),
                OverlayTexture.NO_OVERLAY,
                pose,
                buffers,
                television.getLevel(),
                (int) television.getBlockPos()
                        .asLong()
        );

        pose.popPose();
    }

    private void renderMetadata(
            TelevisionBlockEntity television,
            VhsData.Info info,
            PoseStack pose,
            MultiBufferSource buffers
    ) {
        pose.pushPose();

        transformFront(
                television,
                pose
        );

        pose.translate(
                0.115,
                0.765,
                0.050
        );

        pose.mulPose(
                Axis.YP.rotationDegrees(
                        180.0F
                )
        );

        pose.scale(
                0.0032F,
                -0.0032F,
                0.0032F
        );

        String title =
                trim(
                        info.title(),
                        27
                );

        drawText(
                title,
                0.0F,
                0.0F,
                pose,
                buffers
        );

        float lineY = 10.0F;

        if (info.showCoordinates()) {
            drawText(
                    "XYZ "
                            + info.x()
                            + " "
                            + info.y()
                            + " "
                            + info.z(),
                    0.0F,
                    lineY,
                    pose,
                    buffers
            );

            lineY += 10.0F;
        }

        if (info.showDateTime()) {
            String time =
                    DATE_FORMAT.format(
                            Instant.ofEpochMilli(
                                            info.startedAtMillis()
                                    )
                                    .atZone(
                                            ZoneId.systemDefault()
                                    )
                    );

            drawText(
                    time,
                    0.0F,
                    lineY,
                    pose,
                    buffers
            );
        }

        pose.popPose();
    }

    private void drawText(
            String text,
            float x,
            float y,
            PoseStack pose,
            MultiBufferSource buffers
    ) {
        font.drawInBatch(
                text,
                x,
                y,
                0xFFF1F1F1,
                false,
                pose.last()
                        .pose(),
                buffers,
                Font.DisplayMode.POLYGON_OFFSET,
                0x65000000,
                FULLBRIGHT
        );
    }

    private static String trim(
            String text,
            int maximum
    ) {
        if (text == null
                || text.length()
                <= maximum) {

            return text == null
                    ? ""
                    : text;
        }

        return text.substring(
                0,
                Math.max(
                        0,
                        maximum - 3
                )
        )
                + "...";
    }

    private static void renderScreen(
            TelevisionBlockEntity television,
            PoseStack pose,
            MultiBufferSource buffers,
            ResourceLocation texture
    ) {
        pose.pushPose();

        transformFront(
                television,
                pose
        );

        Matrix4f matrix =
                pose.last()
                        .pose();

        VertexConsumer consumer =
                buffers.getBuffer(
                        RenderType.entityCutoutNoCull(
                                texture
                        )
                );

        float left = 0.105F;
        float right = 0.720F;
        float bottom = 0.205F;
        float top = 0.790F;
        float z = 0.055F;

        vertex(
                consumer,
                matrix,
                pose,
                left,
                bottom,
                z,
                1.0F,
                1.0F
        );

        vertex(
                consumer,
                matrix,
                pose,
                right,
                bottom,
                z,
                0.0F,
                1.0F
        );

        vertex(
                consumer,
                matrix,
                pose,
                right,
                top,
                z,
                0.0F,
                0.0F
        );

        vertex(
                consumer,
                matrix,
                pose,
                left,
                top,
                z,
                1.0F,
                0.0F
        );

        pose.popPose();
    }

    private static void transformFront(
            TelevisionBlockEntity television,
            PoseStack pose
    ) {
        Direction facing =
                television.getBlockState()
                        .getValue(
                                TelevisionBlock.FACING
                        );

        switch (facing) {
            case SOUTH -> {
                pose.translate(
                        1.0,
                        0.0,
                        1.0
                );

                pose.mulPose(
                        Axis.YP.rotationDegrees(
                                180.0F
                        )
                );
            }

            case WEST -> {
                pose.translate(
                        0.0,
                        0.0,
                        1.0
                );

                pose.mulPose(
                        Axis.YP.rotationDegrees(
                                90.0F
                        )
                );
            }

            case EAST -> {
                pose.translate(
                        1.0,
                        0.0,
                        0.0
                );

                pose.mulPose(
                        Axis.YP.rotationDegrees(
                                -90.0F
                        )
                );
            }

            default -> {
            }
        }
    }

    private static void vertex(
            VertexConsumer consumer,
            Matrix4f matrix,
            PoseStack pose,
            float x,
            float y,
            float z,
            float u,
            float v
    ) {
        consumer.addVertex(
                        matrix,
                        x,
                        y,
                        z
                )
                .setColor(
                        255,
                        255,
                        255,
                        255
                )
                .setUv(
                        u,
                        v
                )
                .setOverlay(
                        OverlayTexture.NO_OVERLAY
                )
                .setLight(
                        FULLBRIGHT
                )
                .setNormal(
                        pose.last(),
                        0.0F,
                        0.0F,
                        -1.0F
                );
    }

    private static void reportOnce(
            String key,
            String message
    ) {
        if (REPORTED_ERRORS.add(
                key
        )) {
            WayAround.LOGGER.warn(
                    "[Media/TV] {}",
                    message
            );
        }
    }

    private static void cleanup(
            long currentTick
    ) {
        Iterator<Map.Entry<BlockPos, ScreenTexture>>
                iterator =
                CACHE.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            ScreenTexture screen =
                    iterator.next()
                            .getValue();

            if (currentTick
                    - screen.lastUsedTick
                    > 200L) {

                screen.close();
                iterator.remove();
            }
        }
    }

    private static void release(
            BlockPos pos
    ) {
        ScreenTexture old =
                CACHE.remove(
                        pos
                );

        if (old != null) {
            old.close();
        }
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    private static final class ScreenTexture {

        private final String recordingId;
        private final RecordingReader reader;
        private final NativeImage image;
        private final DynamicTexture texture;
        private final ResourceLocation location;
        private final int filterSeed;

        private int lastFrame = -1;
        private int lastCountdown = -1;
        private long lastUsedTick;

        private ScreenTexture(
                String recordingId,
                Path path,
                BlockPos pos
        ) throws Exception {

            this.recordingId =
                    recordingId;

            this.filterSeed =
                    recordingId.hashCode();

            this.reader =
                    new RecordingReader(
                            path
                    );

            this.image =
                    new NativeImage(
                            NativeImage.Format.RGBA,
                            reader.width(),
                            reader.height(),
                            false
                    );

            this.texture =
                    new DynamicTexture(
                            image
                    );

            this.texture.setFilter(
                    false,
                    false
            );

            this.location =
                    Minecraft.getInstance()
                            .getTextureManager()
                            .register(
                                    "wayaround_tv_"
                                            + Long.toUnsignedString(
                                                    pos.asLong()
                                            )
                                            + "_"
                                            + recordingId
                                            .substring(
                                                    0,
                                                    8
                                            ),
                                    texture
                            );
        }

        private void close() {
            try {
                reader.close();
            } catch (Exception ignored) {
            }

            texture.close();
        }
    }
}
