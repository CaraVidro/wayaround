package net.caravidro.wayaround.media.client;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

import com.mojang.blaze3d.platform.NativeImage;

public final class RecordingWriter implements AutoCloseable {

    private final Path path;
    private final RandomAccessFile file;

    private int frameCount;
    private boolean closed;

    public RecordingWriter(Path path) throws Exception {
        this.path = path;
        this.file = new RandomAccessFile(path.toFile(), "rw");

        file.setLength(0L);
        file.writeInt(RecordingFormat.MAGIC);
        file.writeInt(RecordingFormat.VERSION);
        file.writeInt(RecordingFormat.WIDTH);
        file.writeInt(RecordingFormat.HEIGHT);
        file.writeInt(RecordingFormat.FPS);
        file.writeInt(0);
        file.writeLong(System.currentTimeMillis());
    }

    public Path path() {
        return path;
    }

    public int frameCount() {
        return frameCount;
    }

    public void writeFrame(NativeImage image) throws Exception {
        if (closed) {
            throw new IllegalStateException("RecordingWriter fechado");
        }

        if (image.getWidth() != RecordingFormat.WIDTH
                || image.getHeight() != RecordingFormat.HEIGHT) {
            throw new IllegalArgumentException(
                    "Frame possui dimensoes invalidas"
            );
        }

        byte[] encoded =
                new byte[RecordingFormat.FRAME_BYTES];

        int offset = 0;

        for (int y = 0; y < RecordingFormat.HEIGHT; y++) {
            for (int x = 0; x < RecordingFormat.WIDTH; x++) {
                int red =
                        image.getRedOrLuminance(x, y) & 0xFF;
                int green =
                        image.getGreenOrLuminance(x, y) & 0xFF;
                int blue =
                        image.getBlueOrLuminance(x, y) & 0xFF;

                int rgb565 =
                        ((red >> 3) << 11)
                                | ((green >> 2) << 5)
                                | (blue >> 3);

                encoded[offset++] =
                        (byte) ((rgb565 >> 8) & 0xFF);
                encoded[offset++] =
                        (byte) (rgb565 & 0xFF);
            }
        }

        file.write(encoded);
        frameCount++;
    }

    public Path finish() throws Exception {
        if (closed) {
            return path;
        }

        file.seek(RecordingFormat.FRAME_COUNT_OFFSET);
        file.writeInt(frameCount);
        file.close();
        closed = true;

        if (frameCount == 0) {
            Files.deleteIfExists(path);
        }

        return path;
    }

    @Override
    public void close() throws Exception {
        finish();
    }
}
