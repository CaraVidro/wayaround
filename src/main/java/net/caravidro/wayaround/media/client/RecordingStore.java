package net.caravidro.wayaround.media.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.client.Minecraft;

public final class RecordingStore {

    private RecordingStore() {
    }

    public record Target(
            String id,
            Path path
    ) {
    }

    public static Path directory() {
        return Minecraft.getInstance()
                .gameDirectory
                .toPath()
                .resolve(
                        "wayaround-recordings"
                );
    }

    public static Target createTarget()
            throws Exception {

        Files.createDirectories(
                directory()
        );

        String id =
                UUID.randomUUID()
                        .toString();

        return new Target(
                id,
                pathForId(id)
        );
    }

    public static Path pathForId(
            String id
    ) {
        if (id == null) {
            throw new IllegalArgumentException(
                    "ID de gravacao ausente"
            );
        }

        UUID parsed =
                UUID.fromString(
                        id
                );

        return directory()
                .resolve(
                        parsed
                                .toString()
                                + ".wavr"
                );
    }

    public static Optional<Path> find(
            String id
    ) {
        try {
            Path path =
                    pathForId(id);

            return Files.isRegularFile(
                    path
            )
                    ? Optional.of(path)
                    : Optional.empty();

        } catch (Exception exception) {
            return Optional.empty();
        }
    }
}
