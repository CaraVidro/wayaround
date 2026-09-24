package net.caravidro.wayaround.media.client;

import java.io.RandomAccessFile;
import java.nio.file.Path;

import com.mojang.blaze3d.platform.NativeImage;

public final class RecordingReader implements AutoCloseable {

    private final RandomAccessFile file;

    private final int width;
    private final int height;
    private final int fps;
    private final int frameCount;
    private final long startedAt;

    public RecordingReader(Path path) throws Exception {
        file = new RandomAccessFile(path.toFile(), "r");

        int magic = file.readInt();
        int version = file.readInt();

        if (magic != RecordingFormat.MAGIC) {
            throw new IllegalArgumentException(
                    "Arquivo nao e uma gravacao WayAround"
            );
        }

        if (version != RecordingFormat.VERSION) {
            throw new IllegalArgumentException(
                    "Versao de gravacao nao suportada: " + version
            );
        }

        width = file.readInt();
        height = file.readInt();
        fps = file.readInt();
        frameCount = file.readInt();
        startedAt = file.readLong();

        if (width <= 0
                || height <= 0
                || fps <= 0
                || frameCount < 0) {
            throw new IllegalArgumentException(
                    "Cabecalho de gravacao invalido"
            );
        }

        long expected =
                RecordingFormat.HEADER_BYTES
                        + (long) frameCount
                        * width
                        * height
                        * RecordingFormat.BYTES_PER_PIXEL;

        if (file.length() < expected) {
            throw new IllegalArgumentException(
                    "Gravacao incompleta"
            );
        }
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int fps() {
        return fps;
    }

    public int frameCount() {
        return frameCount;
    }

    public long startedAt() {
        return startedAt;
    }

    public void readFrame(
            int index,
            NativeImage target
    ) throws Exception {
        if (index < 0 || index >= frameCount) {
            throw new IndexOutOfBoundsException(
                    "Frame " + index
            );
        }

        if (target.getWidth() != width
                || target.getHeight() != height) {
            throw new IllegalArgumentException(
                    "Textura com dimensoes erradas"
            );
        }

        int frameBytes =
                width
                        * height
                        * RecordingFormat.BYTES_PER_PIXEL;

        byte[] encoded = new byte[frameBytes];

        long offset =
                RecordingFormat.HEADER_BYTES
                        + (long) index * frameBytes;

        file.seek(offset);
        file.readFully(encoded);

        int cursor = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb565 =
                        ((encoded[cursor++] & 0xFF) << 8)
                                | (encoded[cursor++] & 0xFF);

                int red5 = (rgb565 >> 11) & 0x1F;
                int green6 = (rgb565 >> 5) & 0x3F;
                int blue5 = rgb565 & 0x1F;

                int red =
                        (red5 << 3) | (red5 >> 2);
                int green =
                        (green6 << 2) | (green6 >> 4);
                int blue =
                        (blue5 << 3) | (blue5 >> 2);

                int abgr =
                        0xFF000000
                                | (blue << 16)
                                | (green << 8)
                                | red;

                target.setPixelRGBA(x, y, abgr);
            }
        }
    }

    @Override
    public void close() throws Exception {
        file.close();
    }
}
