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

        int paperMargin = 18;

        if (textureLocation != null
                && imageWidth > 0
                && imageHeight > 0) {

            int availableWidth =
                    Math.max(
                            1,
                            width - 64
                    );

            int availableHeight =
                    Math.max(
                            1,
                            height - 64
                    );

            double scale =
                    Math.min(
                            availableWidth
                                    / (double) imageWidth,
                            availableHeight
                                    / (double) imageHeight
                    );

            int drawWidth =
                    Math.max(
                            1,
                            (int) Math.floor(
                                    imageWidth * scale
                            )
                    );

            int drawHeight =
                    Math.max(
                            1,
                            (int) Math.floor(
                                    imageHeight * scale
                            )
                    );

            int x =
                    (width - drawWidth)
                            / 2;

            int y =
                    (height - drawHeight)
                            / 2;

            graphics.fill(
                    x - paperMargin,
                    y - paperMargin,
                    x + drawWidth
                            + paperMargin,
                    y + drawHeight
                            + paperMargin,
                    0xFFF0DEAE
            );

            /*
             * Keep the photograph itself completely raw. No blur, no
             * sharpening kernel, no per-pixel vintage processing. The only
             * treatment is this very light yellow transparent overlay.
             */
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

            graphics.fill(
                    x,
                    y,
                    x + drawWidth,
                    y + drawHeight,
                    0x20FFD84A
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


}
