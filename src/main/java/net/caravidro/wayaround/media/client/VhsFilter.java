package net.caravidro.wayaround.media.client;

import com.mojang.blaze3d.platform.NativeImage;

public final class VhsFilter {

    private VhsFilter() {
    }

    public static void apply(
            NativeImage image,
            int frameIndex,
            int seed
    ) {
        int width =
                image.getWidth();

        int height =
                image.getHeight();

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

                source[y * width + x] =
                        image.getPixelRGBA(
                                x,
                                y
                        );
            }
        }

        int trackingHash =
                hash(
                        seed,
                        frameIndex,
                        0x51F15E
                );

        boolean tracking =
                (trackingHash & 0xFF)
                        < 14;

        int trackingY =
                Math.floorMod(
                        trackingHash >>> 8,
                        Math.max(
                                1,
                                height
                        )
                );

        for (int y = 0;
             y < height;
             y++) {

            int lineHash =
                    hash(
                            seed,
                            frameIndex,
                            y
                    );

            int jitter =
                    (lineHash & 0xFF)
                            < 28
                            ? Math.floorMod(
                                    lineHash >>> 8,
                                    5
                            )
                                    - 2
                            : 0;

            boolean trackingLine =
                    tracking
                            && Math.abs(
                                    y - trackingY
                            )
                            <= 3;

            if (trackingLine) {
                jitter +=
                        Math.floorMod(
                                lineHash >>> 16,
                                13
                        )
                                - 6;
            }

            for (int x = 0;
                 x < width;
                 x++) {

                int sampleX =
                        clamp(
                                x + jitter,
                                0,
                                width - 1
                        );

                int base =
                        source[
                                y * width
                                        + sampleX
                                ];

                int left =
                        source[
                                y * width
                                        + clamp(
                                                sampleX - 1,
                                                0,
                                                width - 1
                                        )
                                ];

                int right =
                        source[
                                y * width
                                        + clamp(
                                                sampleX + 1,
                                                0,
                                                width - 1
                                        )
                                ];

                int red =
                        base
                                & 0xFF;

                int green =
                        (base >>> 8)
                                & 0xFF;

                int blue =
                        (base >>> 16)
                                & 0xFF;

                int leftRed =
                        left
                                & 0xFF;

                int rightBlue =
                        (right >>> 16)
                                & 0xFF;

                red =
                        (red * 7
                                + leftRed)
                                / 8;

                blue =
                        (blue * 7
                                + rightBlue)
                                / 8;

                int average =
                        (red
                                + green
                                + blue)
                                / 3;

                red =
                        (red * 9
                                + average)
                                / 10;

                green =
                        (green * 9
                                + average)
                                / 10;

                blue =
                        (blue * 9
                                + average)
                                / 10;

                /*
                 * Slight aged/warm bias. The recording on disk stays clean;
                 * this only lives in the playback path.
                 */
                red =
                        red * 108
                                / 100
                                + 6;

                green =
                        green * 103
                                / 100
                                + 3;

                blue =
                        blue * 83
                                / 100;

                int pixelHash =
                        hash(
                                lineHash,
                                x,
                                frameIndex
                        );

                int noise =
                        Math.floorMod(
                                pixelHash,
                                15
                        )
                                - 7;

                red += noise;
                green += noise;
                blue += noise;

                if ((y & 1) == 1) {
                    red =
                            red * 88
                                    / 100;

                    green =
                            green * 88
                                    / 100;

                    blue =
                            blue * 88
                                    / 100;
                }

                if (trackingLine) {
                    int trackingNoise =
                            Math.floorMod(
                                    pixelHash >>> 7,
                                    45
                            )
                                    - 15;

                    red += trackingNoise;
                    green += trackingNoise;
                    blue += trackingNoise;
                }

                image.setPixelRGBA(
                        x,
                        y,
                        0xFF000000
                                | (clamp(
                                        blue,
                                        0,
                                        255
                                ) << 16)
                                | (clamp(
                                        green,
                                        0,
                                        255
                                ) << 8)
                                | clamp(
                                        red,
                                        0,
                                        255
                                )
                );
            }
        }
    }

    public static void renderCountdown(
            NativeImage image,
            int number,
            int seed
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

                int noise =
                        Math.floorMod(
                                hash(
                                        seed,
                                        number,
                                        x + y * width
                                ),
                                11
                        )
                                - 5;

                int value =
                        clamp(
                                24 + noise,
                                0,
                                255
                        );

                image.setPixelRGBA(
                        x,
                        y,
                        0xFF000000
                                | (value << 16)
                                | (value << 8)
                                | value
                );
            }
        }

        boolean[] segments =
                switch (number) {
                    case 3 -> new boolean[] {
                            true,
                            true,
                            true,
                            true,
                            false,
                            false,
                            true
                    };

                    case 2 -> new boolean[] {
                            true,
                            true,
                            false,
                            true,
                            true,
                            false,
                            true
                    };

                    default -> new boolean[] {
                            false,
                            true,
                            true,
                            false,
                            false,
                            false,
                            false
                    };
                };

        int digitWidth = 34;
        int digitHeight = 58;

        int x =
                (width - digitWidth)
                        / 2;

        int y =
                (height - digitHeight)
                        / 2;

        int thickness = 5;
        int color =
                0xFFB8B8B8;

        if (segments[0]) {
            fill(
                    image,
                    x + thickness,
                    y,
                    digitWidth
                            - thickness * 2,
                    thickness,
                    color
            );
        }

        if (segments[1]) {
            fill(
                    image,
                    x + digitWidth
                            - thickness,
                    y + thickness,
                    thickness,
                    digitHeight / 2
                            - thickness,
                    color
            );
        }

        if (segments[2]) {
            fill(
                    image,
                    x + digitWidth
                            - thickness,
                    y + digitHeight / 2,
                    thickness,
                    digitHeight / 2
                            - thickness,
                    color
            );
        }

        if (segments[3]) {
            fill(
                    image,
                    x + thickness,
                    y + digitHeight
                            - thickness,
                    digitWidth
                            - thickness * 2,
                    thickness,
                    color
            );
        }

        if (segments[4]) {
            fill(
                    image,
                    x,
                    y + digitHeight / 2,
                    thickness,
                    digitHeight / 2
                            - thickness,
                    color
            );
        }

        if (segments[5]) {
            fill(
                    image,
                    x,
                    y + thickness,
                    thickness,
                    digitHeight / 2
                            - thickness,
                    color
            );
        }

        if (segments[6]) {
            fill(
                    image,
                    x + thickness,
                    y + digitHeight / 2
                            - thickness / 2,
                    digitWidth
                            - thickness * 2,
                    thickness,
                    color
            );
        }

        apply(
                image,
                number * 17,
                seed
        );
    }

    private static void fill(
            NativeImage image,
            int startX,
            int startY,
            int width,
            int height,
            int color
    ) {
        for (int y = startY;
             y < startY + height;
             y++) {

            for (int x = startX;
                 x < startX + width;
                 x++) {

                if (x >= 0
                        && x < image.getWidth()
                        && y >= 0
                        && y < image.getHeight()) {

                    image.setPixelRGBA(
                            x,
                            y,
                            color
                    );
                }
            }
        }
    }

    private static int hash(
            int a,
            int b,
            int c
    ) {
        int value =
                a * 0x45D9F3B
                        ^ b * 0x119DE1F3
                        ^ c * 0x3449D;

        value ^=
                value >>> 16;

        value *=
                0x45D9F3B;

        value ^=
                value >>> 16;

        return value;
    }

    private static int clamp(
            int value,
            int minimum,
            int maximum
    ) {
        return Math.max(
                minimum,
                Math.min(
                        maximum,
                        value
                )
        );
    }
}
