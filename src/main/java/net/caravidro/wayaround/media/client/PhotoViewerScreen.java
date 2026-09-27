package net.caravidro.wayaround.media.client;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.mojang.blaze3d.platform.NativeImage;

import net.caravidro.wayaround.media.PhotoData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class PhotoViewerScreen
        extends Screen {

    private final String photoId;

    private DynamicTexture texture;
    private ResourceLocation textureLocation;

    private int imageWidth;
    private int imageHeight;

    private String error = "";

    private PhotoViewerScreen(
            String photoId
    ) {
        super(
                Component.translatable(
                        "item.wayaround.photo"
                )
        );

        this.photoId =
                photoId;
    }

    public static void open(
            ItemStack stack
    ) {
        PhotoData.Info info =
                PhotoData.read(stack)
                        .orElse(null);

        if (info == null) {
            return;
        }

        Minecraft.getInstance()
                .setScreen(
                        new PhotoViewerScreen(
                                info.photoId()
                        )
                );
    }

    @Override
    protected void init() {
        super.init();

        try {
            Path path =
                    Minecraft.getInstance()
                            .gameDirectory
                            .toPath()
                            .resolve(
                                    "wayaround-photos"
                            )
                            .resolve(
                                    photoId
                                            + ".png"
                            );

            try (
                    InputStream input =
                            Files.newInputStream(
                                    path
                            )
            ) {
                NativeImage image =
                        NativeImage.read(
                                input
                        );

                applyWarmPhotoLook(
                        image
                );

                sharpen(
                        image
                );

                imageWidth =
                        image.getWidth();

                imageHeight =
                        image.getHeight();

                texture =
                        new DynamicTexture(
                                image
                        );

                texture.setFilter(
                        false,
                        false
                );

                textureLocation =
                        Minecraft.getInstance()
                                .getTextureManager()
                                .register(
                                        "wayaround_photo_"
                                                + photoId
                                                .substring(
                                                        0,
                                                        Math.min(
                                                                8,
                                                                photoId.length()
                                                        )
                                                ),
                                        texture
                                );
            }

        } catch (Exception exception) {
            error =
                    exception.getClass()
                            .getSimpleName()
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
                width,
                height,
                0xFF17130D
        );

        int paperMargin =
                10;

        if (textureLocation != null
                && imageWidth > 0
                && imageHeight > 0) {

            /*
             * Minecraft GUI coordinates are already divided by guiScale.
             * The old code compared raw framebuffer pixels directly against
             * GUI pixels and therefore shrank a 1080p image again, making it
             * look foggy even with nearest-neighbour texture filtering.
             *
             * Prefer a 1:1 physical-pixel presentation. Only scale down when
             * the current window is genuinely smaller than the capture.
             */
            double guiScale =
                    Minecraft.getInstance()
                            .getWindow()
                            .getGuiScale();

            int nativeGuiWidth =
                    Math.max(
                            1,
                            (int) Math.round(
                                    imageWidth
                                            / guiScale
                            )
                    );

            int nativeGuiHeight =
                    Math.max(
                            1,
                            (int) Math.round(
                                    imageHeight
                                            / guiScale
                            )
                    );

            double fitScale =
                    Math.min(
                            1.0,
                            Math.min(
                                    width
                                            / (double) nativeGuiWidth,
                                    height
                                            / (double) nativeGuiHeight
                            )
                    );

            int drawWidth =
                    Math.max(
                            1,
                            (int) Math.round(
                                    nativeGuiWidth
                                            * fitScale
                            )
                    );

            int drawHeight =
                    Math.max(
                            1,
                            (int) Math.round(
                                    nativeGuiHeight
                                            * fitScale
                            )
                    );

            int x =
                    (width - drawWidth)
                            / 2;

            int y =
                    (height - drawHeight)
                            / 2;

            int actualMargin =
                    Math.min(
                            paperMargin,
                            Math.min(
                                    Math.min(
                                            x,
                                            width - (
                                                    x + drawWidth
                                            )
                                    ),
                                    Math.min(
                                            y,
                                            height - (
                                                    y + drawHeight
                                            )
                                    )
                            )
                    );

            if (actualMargin > 0) {
                graphics.fill(
                        x - actualMargin,
                        y - actualMargin,
                        x + drawWidth
                                + actualMargin,
                        y + drawHeight
                                + actualMargin,
                        0xFFF0DEAE
                );
            }

            graphics.blit(
                    textureLocation,
                    x,
                    y,
                    drawWidth,
                    drawHeight,
                    0.0F,
                    0.0F,
                    imageWidth,
                    imageHeight,
                    imageWidth,
                    imageHeight
            );

        } else {
            graphics.drawCenteredString(
                    font,
                    error.isBlank()
                            ? "Missing photo"
                            : error,
                    width / 2,
                    height / 2,
                    0xFFFF7777
            );
        }

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        if (textureLocation != null) {
            Minecraft.getInstance()
                    .getTextureManager()
                    .release(
                            textureLocation
                    );

            textureLocation =
                    null;
            texture =
                    null;

        } else if (texture != null) {
            texture.close();
            texture = null;
        }

        super.removed();
    }

    private static void applyWarmPhotoLook(
            NativeImage image
    ) {
        int width =
                image.getWidth();

        int height =
                image.getHeight();

        for (int y = 0;
             y < height;
             y++) {

            for (int x = 0;
                 x < width;
                 x++) {

                int pixel =
                        image.getPixelRGBA(
                                x,
                                y
                        );

                int red =
                        pixel & 0xFF;

                int green =
                        (pixel >>> 8)
                                & 0xFF;

                int blue =
                        (pixel >>> 16)
                                & 0xFF;

                red =
                        clamp(
                                red * 104 / 100
                                        + 4
                        );

                green =
                        clamp(
                                green * 101 / 100
                                        + 2
                        );

                blue =
                        clamp(
                                blue * 92 / 100
                        );

                image.setPixelRGBA(
                        x,
                        y,
                        0xFF000000
                                | (blue << 16)
                                | (green << 8)
                                | red
                );
            }
        }
    }

    private static void sharpen(
            NativeImage image
    ) {
        int width =
                image.getWidth();

        int height =
                image.getHeight();

        if (width < 3
                || height < 3) {
            return;
        }

        int[] source =
                new int[
                        width * height
                        ];

        for (int y = 0;
             y < height;
             y++) {
            for (int x = 0;
                 x < width;
                 x++) {
                source[
                        y * width + x
                        ] =
                        image.getPixelRGBA(
                                x,
                                y
                        );
            }
        }

        for (int y = 1;
             y < height - 1;
             y++) {
            for (int x = 1;
                 x < width - 1;
                 x++) {
                int center =
                        source[
                                y * width + x
                                ];

                int left =
                        source[
                                y * width + x - 1
                                ];

                int right =
                        source[
                                y * width + x + 1
                                ];

                int up =
                        source[
                                (
                                        y - 1
                                )
                                        * width
                                        + x
                                ];

                int down =
                        source[
                                (
                                        y + 1
                                )
                                        * width
                                        + x
                                ];

                int red =
                        sharpenChannel(
                                center & 0xFF,
                                left & 0xFF,
                                right & 0xFF,
                                up & 0xFF,
                                down & 0xFF
                        );

                int green =
                        sharpenChannel(
                                (
                                        center >>> 8
                                ) & 0xFF,
                                (
                                        left >>> 8
                                ) & 0xFF,
                                (
                                        right >>> 8
                                ) & 0xFF,
                                (
                                        up >>> 8
                                ) & 0xFF,
                                (
                                        down >>> 8
                                ) & 0xFF
                        );

                int blue =
                        sharpenChannel(
                                (
                                        center >>> 16
                                ) & 0xFF,
                                (
                                        left >>> 16
                                ) & 0xFF,
                                (
                                        right >>> 16
                                ) & 0xFF,
                                (
                                        up >>> 16
                                ) & 0xFF,
                                (
                                        down >>> 16
                                ) & 0xFF
                        );

                image.setPixelRGBA(
                        x,
                        y,
                        0xFF000000
                                | (
                                blue << 16
                        )
                                | (
                                green << 8
                        )
                                | red
                );
            }
        }
    }

    private static int sharpenChannel(
            int center,
            int left,
            int right,
            int up,
            int down
    ) {
        int neighbourAverage =
                (
                        left
                                + right
                                + up
                                + down
                ) / 4;

        return clamp(
                Math.round(
                        center * 1.38F
                                - neighbourAverage * 0.38F
                )
        );
    }

    private static int clamp(
            int value
    ) {
        return Math.max(
                0,
                Math.min(
                        255,
                        value
                )
        );
    }
}
