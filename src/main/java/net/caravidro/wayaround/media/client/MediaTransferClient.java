package net.caravidro.wayaround.media.client;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import net.caravidro.wayaround.network.MediaRecordingRequestC2SPayload;
import net.caravidro.wayaround.network.MediaRecordingUploadC2SPayload;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MediaTransferClient {

    private MediaTransferClient() {
    }

    private static final int UPLOAD_CHUNK_BYTES =
            24 * 1024;

    private static final int UPLOAD_CHUNKS_PER_TICK =
            4;

    private static final long MAX_RECORDING_BYTES =
            96L * 1024L * 1024L;

    private static final Queue<UploadTask>
            UPLOADS =
            new ArrayDeque<>();

    private static final Map<String, DownloadTask>
            DOWNLOADS =
            new HashMap<>();

    private static final Map<String, Long>
            REQUESTED_AT =
            new HashMap<>();

    public static void queueUpload(
            String recordingId,
            Path path
    ) {
        try {
            UUID.fromString(
                    recordingId
            );

            long length =
                    Files.size(
                            path
                    );

            if (length <= 0L
                    || length > MAX_RECORDING_BYTES) {

                return;
            }

            synchronized (UPLOADS) {
                UPLOADS.add(
                        new UploadTask(
                                recordingId,
                                path,
                                length
                        )
                );
            }

        } catch (Exception exception) {
            System.err.println(
                    "[WayAround Media] Falha preparando upload: "
                            + exception.getMessage()
            );
        }
    }

    public static void tick() {
        for (int sent = 0;
             sent < UPLOAD_CHUNKS_PER_TICK;
             sent++) {

            UploadTask task;

            synchronized (UPLOADS) {
                task =
                        UPLOADS.peek();
            }

            if (task == null) {
                return;
            }

            try {
                if (task.sendNext()) {
                    synchronized (UPLOADS) {
                        UPLOADS.poll();
                    }

                    task.close();
                }

            } catch (Exception exception) {
                synchronized (UPLOADS) {
                    UPLOADS.poll();
                }

                task.close();

                System.err.println(
                        "[WayAround Media] Upload de gravacao falhou: "
                                + exception.getMessage()
                );
            }
        }
    }

    public static void request(
            String recordingId
    ) {
        if (RecordingStore.find(
                recordingId
        )
                .isPresent()) {

            return;
        }

        long now =
                System.currentTimeMillis();

        Long previous =
                REQUESTED_AT.get(
                        recordingId
                );

        if (previous != null
                && now - previous
                < 2_000L) {

            return;
        }

        REQUESTED_AT.put(
                recordingId,
                now
        );

        PacketDistributor.sendToServer(
                new MediaRecordingRequestC2SPayload(
                        recordingId
                )
        );
    }

    public static synchronized void acceptChunk(
            String recordingId,
            long totalLength,
            long offset,
            byte[] data
    ) {
        if (totalLength <= 0L
                || totalLength > MAX_RECORDING_BYTES
                || offset < 0L
                || data == null
                || data.length == 0
                || offset + data.length
                > totalLength) {

            return;
        }

        try {
            UUID.fromString(
                    recordingId
            );

            DownloadTask task =
                    DOWNLOADS.get(
                            recordingId
                    );

            if (task == null
                    || task.totalLength
                    != totalLength) {

                if (task != null) {
                    task.close();
                }

                task =
                        new DownloadTask(
                                recordingId,
                                totalLength
                        );

                DOWNLOADS.put(
                        recordingId,
                        task
                );
            }

            if (offset
                    != task.expectedOffset) {

                return;
            }

            task.file.seek(
                    offset
            );

            task.file.write(
                    data
            );

            task.expectedOffset +=
                    data.length;

            if (task.expectedOffset
                    >= totalLength) {

                task.finish();

                DOWNLOADS.remove(
                        recordingId
                );

                REQUESTED_AT.remove(
                        recordingId
                );
            }

        } catch (Exception exception) {
            DownloadTask task =
                    DOWNLOADS.remove(
                            recordingId
                    );

            if (task != null) {
                task.close();
            }

            System.err.println(
                    "[WayAround Media] Download de gravacao falhou: "
                            + exception.getMessage()
            );
        }
    }

    private static final class UploadTask {

        private final String recordingId;
        private final long totalLength;
        private final RandomAccessFile file;

        private long offset;

        private UploadTask(
                String recordingId,
                Path path,
                long totalLength
        ) throws Exception {

            this.recordingId =
                    recordingId;

            this.totalLength =
                    totalLength;

            this.file =
                    new RandomAccessFile(
                            path.toFile(),
                            "r"
                    );
        }

        private boolean sendNext()
                throws Exception {

            if (offset >= totalLength) {
                return true;
            }

            int length =
                    (int) Math.min(
                            UPLOAD_CHUNK_BYTES,
                            totalLength
                                    - offset
                    );

            byte[] data =
                    new byte[
                            length
                            ];

            file.seek(
                    offset
            );

            file.readFully(
                    data
            );

            PacketDistributor.sendToServer(
                    new MediaRecordingUploadC2SPayload(
                            recordingId,
                            totalLength,
                            offset,
                            data
                    )
            );

            offset +=
                    length;

            return offset
                    >= totalLength;
        }

        private void close() {
            try {
                file.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static final class DownloadTask {

        private final long totalLength;
        private final Path finalPath;
        private final Path temporaryPath;
        private final RandomAccessFile file;

        private long expectedOffset;

        private DownloadTask(
                String recordingId,
                long totalLength
        ) throws Exception {

            this.totalLength =
                    totalLength;

            Files.createDirectories(
                    RecordingStore.directory()
            );

            this.finalPath =
                    RecordingStore.pathForId(
                            recordingId
                    );

            this.temporaryPath =
                    RecordingStore.directory()
                            .resolve(
                                    recordingId
                                            + ".download"
                            );

            Files.deleteIfExists(
                    temporaryPath
            );

            this.file =
                    new RandomAccessFile(
                            temporaryPath.toFile(),
                            "rw"
                    );

            file.setLength(
                    totalLength
            );
        }

        private void finish()
                throws Exception {

            file.close();

            Files.move(
                    temporaryPath,
                    finalPath,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }

        private void close() {
            try {
                file.close();
            } catch (Exception ignored) {
            }

            try {
                Files.deleteIfExists(
                        temporaryPath
                );
            } catch (Exception ignored) {
            }
        }
    }
}
