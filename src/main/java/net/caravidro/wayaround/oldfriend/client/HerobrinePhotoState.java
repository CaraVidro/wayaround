package net.caravidro.wayaround.oldfriend.client;

import com.mojang.blaze3d.platform.NativeImage;

/**
 * Client-only photographic anomaly. The server only synchronizes a boolean;
 * the actual apparition is composited into the captured screenshot locally.
 */
public final class HerobrinePhotoState {

    private static volatile boolean enabled;

    private HerobrinePhotoState() {
    }

    public static void setEnabled(
            boolean value
    ) {
        enabled =
                value;
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void inject(
            NativeImage image,
            String photoId
    ) {
        if (!enabled
                || image == null
                || image.getWidth() < 64
                || image.getHeight() < 48) {
            return;
        }

        int width =
                image.getWidth();

        int height =
                image.getHeight();

        int hash =
                photoId == null
                        ? 0
                        : photoId.hashCode();

        int figureHeight =
                Math.max(
                        18,
                        Math.min(
                                54,
                                height / 8
                        )
                );

        int figureWidth =
                Math.max(
                        6,
                        figureHeight / 3
                );

        int horizontalRange =
                Math.max(
                        1,
                        width - figureWidth * 4
                );

        int x =
                figureWidth * 2
                        + Math.floorMod(
                        hash,
                        horizontalRange
                );

        int groundY =
                Math.max(
                        figureHeight + 4,
                        Math.min(
                                height - 8,
                                (int) (
                                        height * 0.67
                                )
                                        + Math.floorMod(
                                        hash >>> 8,
                                        Math.max(
                                                2,
                                                height / 12
                                        )
                                )
                                        - height / 24
                        )
                );

        int top =
                groundY
                        - figureHeight;

        // Dark silhouette behind the actual colors keeps him readable while
        // still looking like something the camera barely caught.
        fill(
                image,
                x - 1,
                top - 1,
                figureWidth + 2,
                figureHeight + 2,
                rgba(
                        18,
                        18,
                        18
                ),
                0.42F
        );

        int head =
                Math.max(
                        5,
                        figureWidth
                );

        int bodyHeight =
                figureHeight
                        - head
                        - figureHeight / 3;

        int legHeight =
                figureHeight
                        - head
                        - bodyHeight;

        int headX =
                x;

        fill(
                image,
                headX,
                top,
                figureWidth,
                head,
                rgba(
                        133,
                        93,
                        65
                ),
                0.80F
        );

        fill(
                image,
                x,
                top + head,
                figureWidth,
                bodyHeight,
                rgba(
                        42,
                        122,
                        132
                ),
                0.78F
        );

        int legWidth =
                Math.max(
                        2,
                        figureWidth / 2
                );

        fill(
                image,
                x,
                top + head + bodyHeight,
                legWidth,
                legHeight,
                rgba(
                        36,
                        54,
                        107
                ),
                0.80F
        );

        fill(
                image,
                x + figureWidth - legWidth,
                top + head + bodyHeight,
                legWidth,
                legHeight,
                rgba(
                        36,
                        54,
                        107
                ),
                0.80F
        );

        int eyeY =
                top
                        + Math.max(
                                1,
                                head / 2
                        );

        int eyeSize =
                Math.max(
                        1,
                        figureWidth / 5
                );

        fill(
                image,
                x + Math.max(
                        1,
                        figureWidth / 5
                ),
                eyeY,
                eyeSize,
                Math.max(
                        1,
                        eyeSize / 2
                ),
                rgba(
                        255,
                        255,
                        255
                ),
                0.98F
        );

        fill(
                image,
                x + figureWidth
                        - Math.max(
                        1,
                        figureWidth / 5
                )
                        - eyeSize,
                eyeY,
                eyeSize,
                Math.max(
                        1,
                        eyeSize / 2
                ),
                rgba(
                        255,
                        255,
                        255
                ),
                0.98F
        );
    }

    private static void fill(
            NativeImage image,
            int startX,
            int startY,
            int width,
            int height,
            int color,
            float opacity
    ) {
        for (int y = startY;
             y < startY + height;
             y++) {
            if (y < 0
                    || y >= image.getHeight()) {
                continue;
            }

            for (int x = startX;
                 x < startX + width;
                 x++) {
                if (x < 0
                        || x >= image.getWidth()) {
                    continue;
                }

                int background =
                        image.getPixelRGBA(
                                x,
                                y
                        );

                image.setPixelRGBA(
                        x,
                        y,
                        blend(
                                background,
                                color,
                                opacity
                        )
                );
            }
        }
    }

    private static int blend(
            int background,
            int foreground,
            float opacity
    ) {
        float alpha =
                Math.max(
                        0.0F,
                        Math.min(
                                1.0F,
                                opacity
                        )
                );

        int br =
                background & 0xFF;

        int bg =
                (
                        background >>> 8
                ) & 0xFF;

        int bb =
                (
                        background >>> 16
                ) & 0xFF;

        int fr =
                foreground & 0xFF;

        int fg =
                (
                        foreground >>> 8
                ) & 0xFF;

        int fb =
                (
                        foreground >>> 16
                ) & 0xFF;

        int r =
                Math.round(
                        br
                                + (
                                fr - br
                        )
                                * alpha
                );

        int g =
                Math.round(
                        bg
                                + (
                                fg - bg
                        )
                                * alpha
                );

        int b =
                Math.round(
                        bb
                                + (
                                fb - bb
                        )
                                * alpha
                );

        return rgba(
                r,
                g,
                b
        );
    }

    private static int rgba(
            int red,
            int green,
            int blue
    ) {
        return 0xFF000000
                | (
                blue & 0xFF
        ) << 16
                | (
                green & 0xFF
        ) << 8
                | red & 0xFF;
    }
}
