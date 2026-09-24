package net.caravidro.wayaround.media.client;

import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.NativeImage;

public final class RecordingReader
        implements AutoCloseable {

    private final RandomAccessFile file;

    private final int version;
    private final int headerBytes;

    private final int width;
    private final int height;
    private final int fps;
    private final int frameCount;
    private final int sampleRate;
    private final int audioSamples;
    private final int soundEventCount;
    private final long startedAt;

    private final long audioOffset;
    private final long soundEventsOffset;

    private final List<RecordedAmbientSound>
            ambientSounds;

    public RecordingReader(
            Path path
    ) throws Exception {

        file =
                new RandomAccessFile(
                        path.toFile(),
                        "r"
                );

        int magic =
                file.readInt();

        version =
                file.readInt();

        if (magic
                != RecordingFormat.MAGIC) {

            throw new IllegalArgumentException(
                    "Arquivo nao e uma gravacao WayAround"
            );
        }

        if (version
                != RecordingFormat.VERSION
                && version
                != RecordingFormat.LEGACY_VERSION) {

            throw new IllegalArgumentException(
                    "Versao de gravacao nao suportada: "
                            + version
            );
        }

        width = file.readInt();
        height = file.readInt();
        fps = file.readInt();
        frameCount = file.readInt();
        sampleRate = file.readInt();
        audioSamples = file.readInt();

        if (version
                >= RecordingFormat.VERSION) {

            soundEventCount =
                    file.readInt();

            startedAt =
                    file.readLong();

            headerBytes =
                    RecordingFormat.HEADER_BYTES;

        } else {
            soundEventCount =
                    0;

            startedAt =
                    file.readLong();

            headerBytes =
                    RecordingFormat
                            .LEGACY_HEADER_BYTES;
        }

        if (width <= 0
                || height <= 0
                || fps <= 0
                || frameCount < 0
                || sampleRate <= 0
                || audioSamples < 0
                || soundEventCount < 0
                || soundEventCount
                > RecordingFormat.MAX_SOUND_EVENTS) {

            throw new IllegalArgumentException(
                    "Cabecalho de gravacao invalido"
            );
        }

        audioOffset =
                headerBytes
                        + (long) frameCount
                        * width
                        * height
                        * RecordingFormat
                                .BYTES_PER_PIXEL;

        soundEventsOffset =
                audioOffset
                        + (long) audioSamples
                        * RecordingFormat
                                .AUDIO_BYTES_PER_SAMPLE;

        if (file.length()
                < soundEventsOffset) {

            throw new IllegalArgumentException(
                    "Gravacao incompleta"
            );
        }

        ambientSounds =
                readAmbientSounds();
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

    public int sampleRate() {
        return sampleRate;
    }

    public int audioSamples() {
        return audioSamples;
    }

    public long startedAt() {
        return startedAt;
    }

    public List<RecordedAmbientSound>
            ambientSounds() {

        return ambientSounds;
    }

    public long durationMillis() {
        return Math.max(
                frameCount
                        * 1000L
                        / fps,
                audioSamples
                        * 1000L
                        / sampleRate
        );
    }

    public synchronized void readFrame(
            int index,
            NativeImage target
    ) throws Exception {

        if (index < 0
                || index >= frameCount) {

            throw new IndexOutOfBoundsException(
                    "Frame "
                            + index
            );
        }

        if (target.getWidth()
                != width
                || target.getHeight()
                != height) {

            throw new IllegalArgumentException(
                    "Textura com dimensoes erradas"
            );
        }

        int frameBytes =
                width
                        * height
                        * RecordingFormat
                                .BYTES_PER_PIXEL;

        byte[] encoded =
                new byte[
                        frameBytes
                        ];

        long offset =
                headerBytes
                        + (long) index
                        * frameBytes;

        file.seek(offset);
        file.readFully(encoded);

        int cursor = 0;

        for (int y = 0;
             y < height;
             y++) {

            for (int x = 0;
                 x < width;
                 x++) {

                int rgb565 =
                        ((encoded[cursor++]
                                & 0xFF) << 8)
                                | (encoded[cursor++]
                                & 0xFF);

                int red5 =
                        (rgb565 >> 11)
                                & 0x1F;

                int green6 =
                        (rgb565 >> 5)
                                & 0x3F;

                int blue5 =
                        rgb565
                                & 0x1F;

                int red =
                        (red5 << 3)
                                | (red5 >> 2);

                int green =
                        (green6 << 2)
                                | (green6 >> 4);

                int blue =
                        (blue5 << 3)
                                | (blue5 >> 2);

                int abgr =
                        0xFF000000
                                | (blue << 16)
                                | (green << 8)
                                | red;

                target.setPixelRGBA(
                        x,
                        y,
                        abgr
                );
            }
        }
    }

    public synchronized byte[] readAudio(
            int startSample,
            int maxSamples
    ) throws Exception {

        if (startSample < 0
                || startSample >= audioSamples
                || maxSamples <= 0) {

            return new byte[0];
        }

        int count =
                Math.min(
                        maxSamples,
                        audioSamples
                                - startSample
                );

        byte[] result =
                new byte[
                        count
                                * RecordingFormat
                                        .AUDIO_BYTES_PER_SAMPLE
                        ];

        file.seek(
                audioOffset
                        + (long) startSample
                        * RecordingFormat
                                .AUDIO_BYTES_PER_SAMPLE
        );

        file.readFully(
                result
        );

        return result;
    }

    public synchronized byte[] readAllAudio()
            throws Exception {

        if (audioSamples <= 0) {
            return new byte[0];
        }

        byte[] result =
                new byte[
                        audioSamples
                                * RecordingFormat
                                        .AUDIO_BYTES_PER_SAMPLE
                        ];

        file.seek(
                audioOffset
        );

        file.readFully(
                result
        );

        return result;
    }

    private List<RecordedAmbientSound>
            readAmbientSounds()
            throws Exception {

        if (soundEventCount <= 0) {
            return List.of();
        }

        file.seek(
                soundEventsOffset
        );

        List<RecordedAmbientSound> result =
                new ArrayList<>(
                        soundEventCount
                );

        for (int index = 0;
             index < soundEventCount;
             index++) {

            long timeMillis =
                    file.readLong();

            String soundId =
                    file.readUTF();

            String source =
                    file.readUTF();

            float volume =
                    file.readFloat();

            float pitch =
                    file.readFloat();

            result.add(
                    new RecordedAmbientSound(
                            timeMillis,
                            soundId,
                            source,
                            volume,
                            pitch
                    )
            );
        }

        return List.copyOf(
                result
        );
    }

    @Override
    public void close()
            throws Exception {
        file.close();
    }
}
