package net.caravidro.wayaround.media.blackbox;

import java.io.EOFException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldstate.WorldEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Server-side append-only storage for Black Box recordings.
 *
 * <p>Audio never lives inside an ItemStack NBT payload. The item keeps only a
 * stable UUID pointing at this world-owned archive.</p>
 */
public final class BlackBoxRecordingStore {

    private static final int MAGIC = 0x57414242; // WABB
    private static final int VERSION = 1;

    private static final byte TYPE_VOICE = 1;
    private static final byte TYPE_CHAT = 2;
    private static final byte TYPE_WORLD_EVENT = 3;
    private static final byte TYPE_MARKER = 4;

    private static final long MAX_BYTES =
            96L * 1024L * 1024L;

    private static final Map<UUID, Writer> WRITERS =
            new HashMap<>();

    private BlackBoxRecordingStore() {
    }

    public static synchronized void ensureOpen(
            MinecraftServer server,
            UUID recordingId,
            ResourceLocation dimension,
            BlockPos origin,
            long startGameTime,
            long startedAtMillis
    ) throws Exception {
        if (WRITERS.containsKey(
                recordingId
        )) {
            return;
        }

        Path path =
                path(
                        server,
                        recordingId
                );

        Files.createDirectories(
                path.getParent()
        );

        RandomAccessFile file =
                new RandomAccessFile(
                        path.toFile(),
                        "rw"
                );

        if (file.length() == 0L) {
            file.writeInt(
                    MAGIC
            );
            file.writeInt(
                    VERSION
            );
            file.writeUTF(
                    recordingId.toString()
            );
            file.writeUTF(
                    dimension.toString()
            );
            file.writeLong(
                    origin.asLong()
            );
            file.writeLong(
                    startGameTime
            );
            file.writeLong(
                    startedAtMillis
            );
        } else {
            validateHeader(
                    file,
                    recordingId
            );
        }

        file.seek(
                file.length()
        );

        WRITERS.put(
                recordingId,
                new Writer(
                        file,
                        startGameTime
                )
        );
    }

    public static synchronized void voice(
            UUID recordingId,
            long gameTime,
            UUID speaker,
            String speakerName,
            float gain,
            byte[] pcm
    ) {
        Writer writer =
                WRITERS.get(
                        recordingId
                );

        if (writer == null
                || pcm == null
                || pcm.length == 0) {
            return;
        }

        try {
            if (!writer.canWrite(
                    48L + pcm.length
            )) {
                return;
            }

            writer.file.writeByte(
                    TYPE_VOICE
            );
            writer.file.writeInt(
                    writer.relativeTick(
                            gameTime
                    )
            );
            writer.file.writeLong(
                    speaker.getMostSignificantBits()
            );
            writer.file.writeLong(
                    speaker.getLeastSignificantBits()
            );
            writer.file.writeUTF(
                    trim(
                            speakerName,
                            64
                    )
            );
            writer.file.writeFloat(
                    gain
            );
            writer.file.writeInt(
                    pcm.length
            );
            writer.file.write(
                    pcm
            );
        } catch (Exception exception) {
            fail(
                    recordingId,
                    exception
            );
        }
    }

    public static synchronized void chat(
            UUID recordingId,
            long gameTime,
            String speaker,
            String message
    ) {
        Writer writer =
                WRITERS.get(
                        recordingId
                );

        if (writer == null) {
            return;
        }

        try {
            if (!writer.canWrite(
                    1200L
            )) {
                return;
            }

            writer.file.writeByte(
                    TYPE_CHAT
            );
            writer.file.writeInt(
                    writer.relativeTick(
                            gameTime
                    )
            );
            writer.file.writeUTF(
                    trim(
                            speaker,
                            64
                    )
            );
            writer.file.writeUTF(
                    trim(
                            message,
                            768
                    )
            );
        } catch (Exception exception) {
            fail(
                    recordingId,
                    exception
            );
        }
    }

    public static synchronized void worldEvent(
            UUID recordingId,
            long gameTime,
            WorldEvent event
    ) {
        Writer writer =
                WRITERS.get(
                        recordingId
                );

        if (writer == null) {
            return;
        }

        try {
            if (!writer.canWrite(
                    1600L
            )) {
                return;
            }

            writer.file.writeByte(
                    TYPE_WORLD_EVENT
            );
            writer.file.writeInt(
                    writer.relativeTick(
                            gameTime
                    )
            );
            writer.file.writeLong(
                    event.sequence()
            );
            writer.file.writeUTF(
                    event.type()
                            .toString()
            );
            writer.file.writeUTF(
                    trim(
                            event.data()
                                    .toString(),
                            1024
                    )
            );
        } catch (Exception exception) {
            fail(
                    recordingId,
                    exception
            );
        }
    }

    public static synchronized void marker(
            UUID recordingId,
            long gameTime,
            String marker
    ) {
        Writer writer =
                WRITERS.get(
                        recordingId
                );

        if (writer == null) {
            return;
        }

        try {
            if (!writer.canWrite(
                    512L
            )) {
                return;
            }

            writer.file.writeByte(
                    TYPE_MARKER
            );
            writer.file.writeInt(
                    writer.relativeTick(
                            gameTime
                    )
            );
            writer.file.writeUTF(
                    trim(
                            marker,
                            384
                    )
            );
        } catch (Exception exception) {
            fail(
                    recordingId,
                    exception
            );
        }
    }

    public static synchronized void seal(
            UUID recordingId
    ) {
        Writer writer =
                WRITERS.remove(
                        recordingId
                );

        if (writer != null) {
            writer.close();
        }
    }

    public static synchronized void closeAll() {
        for (Writer writer :
                WRITERS.values()) {
            writer.close();
        }

        WRITERS.clear();
    }

    public static Reader openReader(
            MinecraftServer server,
            UUID recordingId
    ) throws Exception {
        RandomAccessFile file =
                new RandomAccessFile(
                        path(
                                server,
                                recordingId
                        ).toFile(),
                        "r"
                );

        Header header =
                readHeader(
                        file
                );

        if (!header.recordingId()
                .equals(
                        recordingId
                )) {
            file.close();
            throw new IllegalStateException(
                    "Black Box recording ID mismatch"
            );
        }

        return new Reader(
                file,
                header
        );
    }

    public static boolean exists(
            MinecraftServer server,
            UUID recordingId
    ) {
        return Files.isRegularFile(
                path(
                        server,
                        recordingId
                )
        );
    }

    private static Path path(
            MinecraftServer server,
            UUID recordingId
    ) {
        return server.getWorldPath(
                        LevelResource.ROOT
                )
                .resolve(
                        "wayaround-blackbox"
                )
                .resolve(
                        recordingId
                                + ".wabb"
                );
    }

    private static void validateHeader(
            RandomAccessFile file,
            UUID expected
    ) throws Exception {
        file.seek(
                0L
        );

        Header header =
                readHeader(
                        file
                );

        if (!header.recordingId()
                .equals(
                        expected
                )) {
            throw new IllegalStateException(
                    "Black Box archive belongs to another recording"
            );
        }
    }

    private static Header readHeader(
            RandomAccessFile file
    ) throws Exception {
        int magic =
                file.readInt();

        int version =
                file.readInt();

        if (magic != MAGIC
                || version != VERSION) {
            throw new IllegalStateException(
                    "Unsupported Black Box recording"
            );
        }

        UUID id =
                UUID.fromString(
                        file.readUTF()
                );

        ResourceLocation dimension =
                ResourceLocation.parse(
                        file.readUTF()
                );

        BlockPos origin =
                BlockPos.of(
                        file.readLong()
                );

        long startGameTime =
                file.readLong();

        long startedAtMillis =
                file.readLong();

        return new Header(
                id,
                dimension,
                origin,
                startGameTime,
                startedAtMillis
        );
    }

    private static void fail(
            UUID recordingId,
            Exception exception
    ) {
        Writer writer =
                WRITERS.remove(
                        recordingId
                );

        if (writer != null) {
            writer.close();
        }

        WayAround.LOGGER.warn(
                "[BlackBox] archive {} failed: {}",
                recordingId,
                exception.getMessage()
        );
    }

    private static String trim(
            String value,
            int max
    ) {
        if (value == null) {
            return "";
        }

        String clean =
                value.replace(
                                '\n',
                                ' '
                        )
                        .replace(
                                '\r',
                                ' '
                        );

        return clean.length() <= max
                ? clean
                : clean.substring(
                        0,
                        max
                );
    }

    private static final class Writer {
        private final RandomAccessFile file;
        private final long startGameTime;

        private Writer(
                RandomAccessFile file,
                long startGameTime
        ) {
            this.file = file;
            this.startGameTime = startGameTime;
        }

        private int relativeTick(
                long gameTime
        ) {
            return (int) Math.max(
                    0L,
                    Math.min(
                            Integer.MAX_VALUE,
                            gameTime - startGameTime
                    )
            );
        }

        private boolean canWrite(
                long estimatedBytes
        ) throws Exception {
            return file.length()
                    + Math.max(
                    0L,
                    estimatedBytes
            )
                    <= MAX_BYTES;
        }

        private void close() {
            try {
                file.close();
            } catch (Exception ignored) {
            }
        }
    }

    public record Header(
            UUID recordingId,
            ResourceLocation dimension,
            BlockPos origin,
            long startGameTime,
            long startedAtMillis
    ) {
    }

    public sealed interface Entry
            permits VoiceEntry, TextEntry, WorldEntry, MarkerEntry {
        int tick();
    }

    public record VoiceEntry(
            int tick,
            UUID speaker,
            String speakerName,
            float gain,
            byte[] pcm
    ) implements Entry {
    }

    public record TextEntry(
            int tick,
            String speaker,
            String message
    ) implements Entry {
    }

    public record WorldEntry(
            int tick,
            long sequence,
            String type,
            String data
    ) implements Entry {
    }

    public record MarkerEntry(
            int tick,
            String marker
    ) implements Entry {
    }

    public static final class Reader
            implements AutoCloseable {

        private final RandomAccessFile file;
        private final Header header;

        private Reader(
                RandomAccessFile file,
                Header header
        ) {
            this.file = file;
            this.header = header;
        }

        public Header header() {
            return header;
        }

        public Entry next()
                throws Exception {
            try {
                byte type =
                        file.readByte();

                int tick =
                        file.readInt();

                return switch (type) {
                    case TYPE_VOICE -> {
                        UUID speaker =
                                new UUID(
                                        file.readLong(),
                                        file.readLong()
                                );

                        String name =
                                file.readUTF();

                        float gain =
                                file.readFloat();

                        int length =
                                file.readInt();

                        if (length < 0
                                || length > 4096) {
                            throw new IllegalStateException(
                                    "Invalid Black Box voice frame"
                            );
                        }

                        byte[] pcm =
                                new byte[
                                        length
                                ];

                        file.readFully(
                                pcm
                        );

                        yield new VoiceEntry(
                                tick,
                                speaker,
                                name,
                                gain,
                                pcm
                        );
                    }

                    case TYPE_CHAT ->
                            new TextEntry(
                                    tick,
                                    file.readUTF(),
                                    file.readUTF()
                            );

                    case TYPE_WORLD_EVENT ->
                            new WorldEntry(
                                    tick,
                                    file.readLong(),
                                    file.readUTF(),
                                    file.readUTF()
                            );

                    case TYPE_MARKER ->
                            new MarkerEntry(
                                    tick,
                                    file.readUTF()
                            );

                    default ->
                            throw new IllegalStateException(
                                    "Unknown Black Box entry type "
                                            + type
                            );
                };

            } catch (EOFException end) {
                return null;
            }
        }

        @Override
        public void close() {
            try {
                file.close();
            } catch (Exception ignored) {
            }
        }
    }
}
