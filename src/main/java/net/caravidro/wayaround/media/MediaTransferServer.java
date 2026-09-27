package net.caravidro.wayaround.media;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.MediaRecordingChunkS2CPayload;
import net.caravidro.wayaround.network.MediaRecordingOfferS2CPayload;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(
        modid = WayAround.MODID
)
public final class MediaTransferServer {

    private MediaTransferServer() {
    }

    private static final long MAX_RECORDING_BYTES =
            96L * 1024L * 1024L;

    private static final int MAX_UPLOAD_CHUNK =
            24 * 1024;

    private static final int DOWNLOAD_CHUNK =
            256 * 1024;

    private static final int DOWNLOAD_CHUNKS_PER_TICK =
            2;

    private static final Map<String, UploadSession>
            UPLOADS =
            new HashMap<>();

    private static final List<DownloadSession>
            DOWNLOADS =
            new ArrayList<>();

    public static synchronized void acceptUpload(
            ServerPlayer player,
            String recordingId,
            long totalLength,
            long offset,
            byte[] data
    ) {
        if (!WorldFeatureRuntime.serverEnabled(WorldFeature.MEDIA)) return;
        if (!validId(
                recordingId
        )
                || totalLength <= 0L
                || totalLength > MAX_RECORDING_BYTES
                || offset < 0L
                || data == null
                || data.length == 0
                || data.length > MAX_UPLOAD_CHUNK
                || offset + data.length
                > totalLength) {

            return;
        }

        String key =
                player.getUUID()
                        + ":"
                        + recordingId;

        try {
            UploadSession session =
                    UPLOADS.get(
                            key
                    );

            if (session == null) {
                if (offset != 0L) {
                    return;
                }

                session =
                        new UploadSession(
                                player.server,
                                recordingId,
                                totalLength
                        );

                UPLOADS.put(
                        key,
                        session
                );
            }

            if (session.totalLength
                    != totalLength
                    || session.expectedOffset
                    != offset) {

                return;
            }

            session.file.seek(
                    offset
            );

            session.file.write(
                    data
            );

            session.expectedOffset +=
                    data.length;

            if (session.expectedOffset
                    >= totalLength) {

                session.finish();

                UPLOADS.remove(
                        key
                );
            }

        } catch (Exception exception) {
            UploadSession session =
                    UPLOADS.remove(
                            key
                    );

            if (session != null) {
                session.close();
            }

            WayAround.LOGGER.warn(
                    "[Media] upload {} falhou: {}",
                    recordingId,
                    exception.getMessage()
            );
        }
    }

    /**
     * Metadata phase only. This MUST NOT start a file transfer.
     *
     * The client receives the exact size and asks the player for consent.
     * Only MediaRecordingApproveC2SPayload may reach approveDownload().
     */
    public static synchronized void request(
            ServerPlayer player,
            String recordingId
    ) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.MEDIA
        )) {
            return;
        }

        if (!validId(
                recordingId
        )) {
            return;
        }

        Path path =
                recordingPath(
                        player.server,
                        recordingId
                );

        try {
            if (!Files.isRegularFile(
                    path
            )) {
                return;
            }

            long length =
                    Files.size(
                            path
                    );

            if (length <= 0L
                    || length > MAX_RECORDING_BYTES) {
                return;
            }

            PacketDistributor.sendToPlayer(
                    player,
                    new MediaRecordingOfferS2CPayload(
                            recordingId,
                            length
                    )
            );

        } catch (Exception exception) {
            WayAround.LOGGER.warn(
                    "[Media] nao consegui consultar gravacao {}: {}",
                    recordingId,
                    exception.getMessage()
            );
        }
    }

    /**
     * Starts transfer only after the client explicitly approved the offer.
     */
    public static synchronized void approveDownload(
            ServerPlayer player,
            String recordingId
    ) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.MEDIA
        )
                || !validId(
                recordingId
        )) {
            return;
        }

        for (DownloadSession session
                : DOWNLOADS) {
            if (session.playerId
                    .equals(
                            player.getUUID()
                    )
                    && session.recordingId
                    .equals(
                            recordingId
                    )) {
                return;
            }
        }

        Path path =
                recordingPath(
                        player.server,
                        recordingId
                );

        try {
            if (!Files.isRegularFile(
                    path
            )) {
                return;
            }

            long length =
                    Files.size(
                            path
                    );

            if (length <= 0L
                    || length > MAX_RECORDING_BYTES) {
                return;
            }

            DOWNLOADS.add(
                    new DownloadSession(
                            player.getUUID(),
                            recordingId,
                            path,
                            length
                    )
            );

            WayAround.LOGGER.info(
                    "[Media] download autorizado pelo jogador {}: {} ({} bytes)",
                    player.getGameProfile()
                            .getName(),
                    recordingId,
                    length
            );

        } catch (Exception exception) {
            WayAround.LOGGER.warn(
                    "[Media] nao consegui iniciar download aprovado {}: {}",
                    recordingId,
                    exception.getMessage()
            );
        }
    }

    @SubscribeEvent
    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        if (!WorldFeatureRuntime.serverEnabled(WorldFeature.MEDIA)) return;
        MinecraftServer server =
                event.getServer();

        synchronized (MediaTransferServer.class) {
            int budget =
                    DOWNLOAD_CHUNKS_PER_TICK;

            Iterator<DownloadSession> iterator =
                    DOWNLOADS.iterator();

            while (iterator.hasNext()
                    && budget > 0) {

                DownloadSession session =
                        iterator.next();

                ServerPlayer player =
                        server.getPlayerList()
                                .getPlayer(
                                        session.playerId
                                );

                if (player == null) {
                    session.close();
                    iterator.remove();
                    continue;
                }

                try {
                    if (session.sendNext(
                            player
                    )) {
                        session.close();
                        iterator.remove();
                    }

                    budget--;

                } catch (Exception exception) {
                    session.close();
                    iterator.remove();

                    WayAround.LOGGER.warn(
                            "[Media] download {} falhou: {}",
                            session.recordingId,
                            exception.getMessage()
                    );
                }
            }
        }
    }

    private static boolean validId(
            String recordingId
    ) {
        try {
            UUID.fromString(
                    recordingId
            );

            return true;

        } catch (Exception exception) {
            return false;
        }
    }

    private static Path directory(
            MinecraftServer server
    ) {
        return server.getWorldPath(
                        LevelResource.ROOT
                )
                .resolve(
                        "wayaround-recordings"
                );
    }

    private static Path recordingPath(
            MinecraftServer server,
            String recordingId
    ) {
        return directory(server)
                .resolve(
                        UUID.fromString(
                                recordingId
                        )
                                + ".wavr"
                );
    }

    private static final class UploadSession {

        private final long totalLength;
        private final Path finalPath;
        private final Path temporaryPath;
        private final RandomAccessFile file;

        private long expectedOffset;

        private UploadSession(
                MinecraftServer server,
                String recordingId,
                long totalLength
        ) throws Exception {

            this.totalLength =
                    totalLength;

            Files.createDirectories(
                    directory(server)
            );

            this.finalPath =
                    recordingPath(
                            server,
                            recordingId
                    );

            this.temporaryPath =
                    directory(server)
                            .resolve(
                                    recordingId
                                            + ".uploading"
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

    private static final class DownloadSession {

        private final UUID playerId;
        private final String recordingId;
        private final long totalLength;
        private final RandomAccessFile file;

        private long offset;

        private DownloadSession(
                UUID playerId,
                String recordingId,
                Path path,
                long totalLength
        ) throws Exception {

            this.playerId =
                    playerId;

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

        private boolean sendNext(
                ServerPlayer player
        ) throws Exception {

            if (offset >= totalLength) {
                return true;
            }

            int length =
                    (int) Math.min(
                            DOWNLOAD_CHUNK,
                            totalLength - offset
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

            PacketDistributor.sendToPlayer(
                    player,
                    new MediaRecordingChunkS2CPayload(
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
}
