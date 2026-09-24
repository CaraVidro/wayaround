package net.caravidro.wayaround.media.client;

import java.nio.file.Path;
import java.util.Optional;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class TvPlaybackScreen extends Screen {

    private final Path recordingPath;

    private RecordingReader reader;
    private DynamicTexture texture;
    private ResourceLocation textureLocation;

    private long playbackStartedNanos;
    private int currentFrame = -1;

    private String error = "";

    private TvPlaybackScreen(Path recordingPath) {
        super(
                Component.translatable(
                        "block.wayaround.television"
                )
        );

        this.recordingPath = recordingPath;
    }

    public static void openLatest() {
        Minecraft minecraft = Minecraft.getInstance();
        Optional<Path> latest = RecordingStore.latest();

        if (latest.isEmpty()) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.translatable(
                                "message.wayaround.media.no_recording"
                        ),
                        false
                );
            }

            return;
        }

        minecraft.setScreen(
                new TvPlaybackScreen(latest.get())
        );
    }

    @Override
    protected void init() {
        super.init();

        try {
            reader =
                    new RecordingReader(recordingPath);

            texture =
                    new DynamicTexture(
                            reader.width(),
                            reader.height(),
                            true
                    );

            textureLocation =
                    this.minecraft
                            .getTextureManager()
                            .register(
                                    "wayaround_tv_"
                                            + System.nanoTime(),
                                    texture
                            );

            playbackStartedNanos =
                    System.nanoTime();
        } catch (Exception exception) {
            error =
                    exception.getClass().getSimpleName()
                            + ": "
                            + exception.getMessage();
        }
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        graphics.fill(
                0,
                0,
                this.width,
                this.height,
                0xFF080808
        );

        if (!error.isBlank()) {
            graphics.drawCenteredString(
                    this.font,
                    Component.literal(error)
                            .withStyle(ChatFormatting.RED),
                    this.width / 2,
                    this.height / 2,
                    0xFFFFFFFF
            );

            super.render(
                    graphics,
                    mouseX,
                    mouseY,
                    partialTick
            );
            return;
        }

        if (reader == null
                || texture == null
                || textureLocation == null
                || reader.frameCount() <= 0) {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable(
                            "message.wayaround.media.no_recording"
                    ),
                    this.width / 2,
                    this.height / 2,
                    0xFFFFFFFF
            );

            super.render(
                    graphics,
                    mouseX,
                    mouseY,
                    partialTick
            );
            return;
        }

        updateFrame();

        int availableWidth =
                Math.max(1, this.width - 24);
        int availableHeight =
                Math.max(1, this.height - 46);

        double scale =
                Math.min(
                        availableWidth
                                / (double) reader.width(),
                        availableHeight
                                / (double) reader.height()
                );

        int drawWidth =
                Math.max(
                        1,
                        (int) Math.floor(
                                reader.width() * scale
                        )
                );

        int drawHeight =
                Math.max(
                        1,
                        (int) Math.floor(
                                reader.height() * scale
                        )
                );

        int x =
                (this.width - drawWidth) / 2;
        int y =
                (this.height - drawHeight) / 2;

        graphics.blit(
                textureLocation,
                x,
                y,
                drawWidth,
                drawHeight,
                0.0F,
                0.0F,
                reader.width(),
                reader.height(),
                reader.width(),
                reader.height()
        );

        graphics.drawCenteredString(
                this.font,
                recordingPath.getFileName().toString(),
                this.width / 2,
                8,
                0xFFC0C0C0
        );

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );
    }

    private void updateFrame() {
        long elapsedNanos =
                System.nanoTime()
                        - playbackStartedNanos;

        long elapsedFrames =
                elapsedNanos
                        * reader.fps()
                        / 1_000_000_000L;

        int targetFrame =
                (int) (
                        elapsedFrames
                                % reader.frameCount()
                );

        if (targetFrame == currentFrame) {
            return;
        }

        NativeImage pixels = texture.getPixels();

        if (pixels == null) {
            error = "Textura da TV sem pixels";
            return;
        }

        try {
            reader.readFrame(
                    targetFrame,
                    pixels
            );

            texture.upload();
            currentFrame = targetFrame;
        } catch (Exception exception) {
            error =
                    exception.getClass().getSimpleName()
                            + ": "
                            + exception.getMessage();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        if (reader != null) {
            try {
                reader.close();
            } catch (Exception ignored) {
            }
            reader = null;
        }

        if (textureLocation != null
                && this.minecraft != null) {
            this.minecraft
                    .getTextureManager()
                    .release(textureLocation);

            textureLocation = null;
            texture = null;
        } else if (texture != null) {
            texture.close();
            texture = null;
        }

        super.removed();
    }
}
