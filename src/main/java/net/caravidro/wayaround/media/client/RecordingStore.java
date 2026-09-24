package net.caravidro.wayaround.media.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import net.minecraft.client.Minecraft;

public final class RecordingStore {

    private RecordingStore() {
    }

    public static Path directory() {
        return Minecraft.getInstance()
                .gameDirectory
                .toPath()
                .resolve("wayaround-recordings");
    }

    public static Path createPath() throws Exception {
        Path directory = directory();
        Files.createDirectories(directory);

        return directory.resolve(
                "recording-"
                        + System.currentTimeMillis()
                        + ".wavr"
        );
    }

    public static Optional<Path> latest() {
        Path directory = directory();

        if (!Files.isDirectory(directory)) {
            return Optional.empty();
        }

        try (Stream<Path> files = Files.list(directory)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(
                            path -> path.getFileName()
                                    .toString()
                                    .endsWith(".wavr")
                    )
                    .max(
                            Comparator.comparingLong(
                                    RecordingStore::lastModified
                            )
                    );
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private static long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (Exception exception) {
            return 0L;
        }
    }
}
