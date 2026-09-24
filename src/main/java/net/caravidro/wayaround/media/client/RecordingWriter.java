package net.caravidro.wayaround.media.client;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

import com.mojang.blaze3d.platform.NativeImage;

public final class RecordingWriter
        implements AutoCloseable {

    public record Summary(
            String recordingId,
            Path path,
            int frameCount,
            int audioSamples,
            long startedAtMillis,
            long durationMillis
    ) {
    }

    private final String recordingId;
    private final Path path;
    private final RandomAccessFile file;

    private final long startedAtMillis =
            System.currentTimeMillis();

    private final long startedAtNanos =
            System.nanoTime();

    private short[] voiceMix;
    private int voiceSamplesUsed;

    private int frameCount;
    private boolean closed;

    public RecordingWriter(
            String recordingId,
            Path path
    ) throws Exception {

        this.recordingId =
                recordingId;

        this.path =
                path;

        this.file =
                new RandomAccessFile(
                        path.toFile(),
                        "rw"
                );

        file.setLength(0L);

        file.writeInt(
                RecordingFormat.MAGIC
        );

        file.writeInt(
                RecordingFormat.VERSION
        );

        file.writeInt(
                RecordingFormat.WIDTH
        );

        file.writeInt(
                RecordingFormat.HEIGHT
        );

        file.writeInt(
                RecordingFormat.FPS
        );

        file.writeInt(0);

        file.writeInt(
                RecordingFormat.AUDIO_SAMPLE_RATE
        );

        file.writeInt(0);

        file.writeLong(
                startedAtMillis
        );
    }

    public int frameCount() {
        return frameCount;
    }

    public synchronized void writeFrame(
            NativeImage image
    ) throws Exception {

        if (closed) {
            throw new IllegalStateException(
                    "RecordingWriter fechado"
            );
        }

        if (image.getWidth()
                != RecordingFormat.WIDTH
                || image.getHeight()
                != RecordingFormat.HEIGHT) {

            throw new IllegalArgumentException(
                    "Frame possui dimensoes invalidas"
            );
        }

        byte[] encoded =
                new byte[
                        RecordingFormat.FRAME_BYTES
                        ];

        int offset = 0;

        for (int y = 0;
             y < RecordingFormat.HEIGHT;
             y++) {

            for (int x = 0;
                 x < RecordingFormat.WIDTH;
                 x++) {

                int red =
                        image.getRedOrLuminance(
                                x,
                                y
                        )
                                & 0xFF;

                int green =
                        image.getGreenOrLuminance(
                                x,
                                y
                        )
                                & 0xFF;

                int blue =
                        image.getBlueOrLuminance(
                                x,
                                y
                        )
                                & 0xFF;

                int rgb565 =
                        ((red >> 3) << 11)
                                | ((green >> 2) << 5)
                                | (blue >> 3);

                encoded[offset++] =
                        (byte) (
                                (rgb565 >> 8)
                                        & 0xFF
                        );

                encoded[offset++] =
                        (byte) (
                                rgb565
                                        & 0xFF
                        );
            }
        }

        file.write(
                encoded
        );

        frameCount++;
    }

    public synchronized void mixVoiceFrame(
            byte[] pcm
    ) {
        if (closed
                || pcm == null
                || pcm.length < 2) {

            return;
        }

        if (voiceMix == null) {
            voiceMix =
                    new short[
                            RecordingFormat
                                    .MAX_AUDIO_SAMPLES
                            ];
        }

        long elapsedNanos =
                Math.max(
                        0L,
                        System.nanoTime()
                                - startedAtNanos
                );

        int destinationSample =
                (int) Math.min(
                        RecordingFormat
                                .MAX_AUDIO_SAMPLES
                                - 1L,
                        elapsedNanos
                                * RecordingFormat
                                .AUDIO_SAMPLE_RATE
                                / 1_000_000_000L
                );

        int incomingSamples =
                pcm.length / 2;

        int writable =
                Math.min(
                        incomingSamples,
                        RecordingFormat
                                .MAX_AUDIO_SAMPLES
                                - destinationSample
                );

        for (int index = 0;
             index < writable;
             index++) {

            int sourceIndex =
                    index * 2;

            int low =
                    pcm[sourceIndex]
                            & 0xFF;

            int high =
                    pcm[sourceIndex + 1];

            int incoming =
                    (short) (
                            low
                                    | (high << 8)
                    );

            int mixed =
                    voiceMix[
                            destinationSample
                                    + index
                            ]
                            + incoming;

            mixed =
                    Math.max(
                            Short.MIN_VALUE,
                            Math.min(
                                    Short.MAX_VALUE,
                                    mixed
                            )
                    );

            voiceMix[
                    destinationSample
                            + index
                    ] =
                    (short) mixed;
        }

        voiceSamplesUsed =
                Math.max(
                        voiceSamplesUsed,
                        destinationSample
                                + writable
                );
    }

    public synchronized Summary finish()
            throws Exception {

        if (closed) {
            throw new IllegalStateException(
                    "Gravacao ja finalizada"
            );
        }

        int videoSamples =
                (int) Math.min(
                        RecordingFormat
                                .MAX_AUDIO_SAMPLES,
                        (
                                (long) frameCount
                                        * RecordingFormat
                                        .AUDIO_SAMPLE_RATE
                                        + RecordingFormat.FPS
                                        - 1L
                        )
                                / RecordingFormat.FPS
                );

        int audioSamples =
                Math.max(
                        videoSamples,
                        voiceSamplesUsed
                );

        file.seek(
                RecordingFormat
                        .FRAME_COUNT_OFFSET
        );

        file.writeInt(
                frameCount
        );

        file.seek(
                RecordingFormat
                        .AUDIO_SAMPLES_OFFSET
        );

        file.writeInt(
                audioSamples
        );

        file.seek(
                RecordingFormat.HEADER_BYTES
                        + (long) frameCount
                        * RecordingFormat.FRAME_BYTES
        );

        byte[] chunk =
                new byte[
                        8192
                        ];

        int sample = 0;

        while (sample < audioSamples) {
            int samplesThisChunk =
                    Math.min(
                            chunk.length / 2,
                            audioSamples - sample
                    );

            int cursor = 0;

            for (int index = 0;
                 index < samplesThisChunk;
                 index++) {

                short value =
                        voiceMix == null
                                || sample + index
                                >= voiceMix.length
                                ? 0
                                : voiceMix[
                                        sample
                                                + index
                                        ];

                chunk[cursor++] =
                        (byte) (
                                value
                                        & 0xFF
                        );

                chunk[cursor++] =
                        (byte) (
                                (value >> 8)
                                        & 0xFF
                        );
            }

            file.write(
                    chunk,
                    0,
                    cursor
            );

            sample +=
                    samplesThisChunk;
        }

        file.close();
        closed = true;

        if (frameCount == 0) {
            Files.deleteIfExists(
                    path
            );
        }

        long durationMillis =
                Math.max(
                        frameCount
                                * 1000L
                                / RecordingFormat.FPS,
                        audioSamples
                                * 1000L
                                / RecordingFormat
                                        .AUDIO_SAMPLE_RATE
                );

        return new Summary(
                recordingId,
                path,
                frameCount,
                audioSamples,
                startedAtMillis,
                durationMillis
        );
    }

    @Override
    public void close()
            throws Exception {
        if (!closed) {
            finish();
        }
    }
}
