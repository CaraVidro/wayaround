package net.caravidro.wayaround.media.client;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import net.caravidro.wayaround.network.MediaRecordingApproveC2SPayload;
import net.caravidro.wayaround.safety.DownloadConsentContract;
import net.caravidro.wayaround.network.MediaRecordingRequestC2SPayload;
import net.caravidro.wayaround.network.MediaRecordingUploadC2SPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
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

    private static final long DECLINE_COOLDOWN_MS =
            5L * 60L * 1000L;

    private static final long OFFER_TIMEOUT_MS =
            30_000L;

    private static final long APPROVAL_TIMEOUT_MS =
            2L * 60L * 1000L;

    private static final Queue<UploadTask>
            UPLOADS =
            new ArrayDeque<>();

    private static final Map<String, DownloadTask>
            DOWNLOADS =
            new HashMap<>();

    private static final Map<String, Long>
            REQUESTED_AT =
            new HashMap<>();

    /*
     * A recording is allowed to touch disk only while its exact consent grant
     * exists here. Resource ID, byte count, one-time offer token and expiry all
     * have to match every incoming chunk.
     */
    private static final Map<String, DownloadConsentContract.Grant>
            APPROVED_DOWNLOADS =
            new HashMap<>();

    private static final Set<String>
            PENDING_CONSENT =
            new HashSet<>();

    private static final Map<String, Long>
            DECLINED_UNTIL =
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

    /**
     * Ask the server for metadata only. No file content is transferred by this
     * request anymore.
     */
    public static synchronized void request(
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

        DownloadConsentContract.Grant existingGrant =
                APPROVED_DOWNLOADS.get(
                        recordingId
                );

        if (existingGrant != null
                && now > existingGrant.expiresAt()) {
            APPROVED_DOWNLOADS.remove(
                    recordingId
            );
        }

        Long declined =
                DECLINED_UNTIL.get(
                        recordingId
                );

        if (declined != null
                && now < declined) {
            return;
        }

        if (PENDING_CONSENT.contains(
                recordingId
        )
                || APPROVED_DOWNLOADS.containsKey(
                recordingId
        )) {
            return;
        }

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

    /**
     * Called after the server returns file metadata. Still no bytes are written.
     * The player sees source, purpose, destination and exact size before deciding.
     */
    public static synchronized void offer(
            String recordingId,
            long totalLength,
            long offerToken
    ) {
        if (totalLength <= 0L
                || totalLength > MAX_RECORDING_BYTES
                || offerToken == 0L
                || RecordingStore.find(
                recordingId
        ).isPresent()) {
            return;
        }

        try {
            UUID.fromString(
                    recordingId
            );
        } catch (Exception exception) {
            return;
        }

        long now =
                System.currentTimeMillis();

        Long requestedAt =
                REQUESTED_AT.get(
                        recordingId
                );

        /*
         * A server cannot create unsolicited download prompts. Metadata offers
         * are accepted only as the direct response to a recent client request.
         */
        if (requestedAt == null
                || now - requestedAt > OFFER_TIMEOUT_MS) {
            REQUESTED_AT.remove(
                    recordingId
            );
            return;
        }

        if (PENDING_CONSENT.contains(
                recordingId
        )
                || APPROVED_DOWNLOADS.containsKey(
                recordingId
        )) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft == null) {
            return;
        }

        PENDING_CONSENT.add(
                recordingId
        );

        Screen parent =
                minecraft.screen;

        double mebibytes =
                totalLength
                        / (1024.0 * 1024.0);

        String size =
                String.format(
                        java.util.Locale.ROOT,
                        "%.1f MB",
                        mebibytes
                );

        minecraft.setScreen(
                new ConfirmScreen(
                        accepted -> {
                            synchronized (MediaTransferClient.class) {
                                PENDING_CONSENT.remove(
                                        recordingId
                                );

                                REQUESTED_AT.remove(
                                        recordingId
                                );

                                if (accepted) {
                                    DownloadConsentContract.Grant grant =
                                            new DownloadConsentContract.Grant(
                                                    recordingId,
                                                    totalLength,
                                                    offerToken,
                                                    System.currentTimeMillis()
                                                            + APPROVAL_TIMEOUT_MS
                                            );

                                    APPROVED_DOWNLOADS.put(
                                            recordingId,
                                            grant
                                    );

                                    PacketDistributor.sendToServer(
                                            new MediaRecordingApproveC2SPayload(
                                                    recordingId,
                                                    totalLength,
                                                    offerToken
                                            )
                                    );

                                } else {
                                    DECLINED_UNTIL.put(
                                            recordingId,
                                            System.currentTimeMillis()
                                                    + DECLINE_COOLDOWN_MS
                                    );
                                }
                            }

                            Minecraft current =
                                    Minecraft.getInstance();

                            if (current != null) {
                                current.setScreen(
                                        parent
                                );
                            }
                        },
                        Component.literal(
                                "Download Way Around recording?"
                        ),
                        Component.literal(
                                "The current Minecraft server wants to send "
                                        + size
                                        + " of in-game VHS/TV media. It will be saved in wayaround-recordings so this recording can play. No file data is downloaded unless you choose Yes."
                        )
                )
        );
    }

    public static synchronized void acceptChunk(
            String recordingId,
            long totalLength,
            long offerToken,
            long offset,
            byte[] data
    ) {
        DownloadConsentContract.Grant grant =
                APPROVED_DOWNLOADS.get(
                        recordingId
                );

        /*
         * Consent is enforced at the file-writing boundary, not just by trusting
         * the normal server handshake. A chunk from another offer cannot reuse
         * a previous Yes click.
         */
        if (grant == null
                || !grant.matches(
                        recordingId,
                        totalLength,
                        offerToken,
                        System.currentTimeMillis()
                )
                || totalLength <= 0L
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
                    != totalLength
                    || task.offerToken
                    != offerToken) {

                if (task != null) {
                    task.close();
                }

                task =
                        new DownloadTask(
                                recordingId,
                                totalLength,
                                offerToken
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

                APPROVED_DOWNLOADS.remove(
                        recordingId
                );

                DECLINED_UNTIL.remove(
                        recordingId
                );
            }

        } catch (Exception exception) {
            DownloadTask task =
                    DOWNLOADS.remove(
                            recordingId
                    );

            APPROVED_DOWNLOADS.remove(
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
        private final long offerToken;
        private final Path finalPath;
        private final Path temporaryPath;
        private final RandomAccessFile file;

        private long expectedOffset;

        private DownloadTask(
                String recordingId,
                long totalLength,
                long offerToken
        ) throws Exception {

            this.totalLength =
                    totalLength;

            this.offerToken =
                    offerToken;

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
