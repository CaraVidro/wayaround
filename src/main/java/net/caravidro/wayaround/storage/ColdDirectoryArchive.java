package net.caravidro.wayaround.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.function.Predicate;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/** Local, owned, immutable caches only. Never use this for world saves or a loaded JAR.
 * Callers must exclude concurrent readers/writers for the entire operation.
 */
public final class ColdDirectoryArchive {
    private static final int MAX_ENTRIES = 10_000;
    private ColdDirectoryArchive() {}

    public static Path archive(Path directory) {
        return directory.resolveSibling(directory.getFileName() + ".cold.zip");
    }

    public static boolean isCold(Path directory) {
        return Files.isRegularFile(archive(directory), LinkOption.NOFOLLOW_LINKS);
    }

    /** Returns bytes saved; incompressible directories stay warm. */
    public static long freeze(Path directory, long maxBytes) throws IOException {
        Path root = directory.toAbsolutePath().normalize();
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) return 0;
        Path cold = archive(root);
        Path temporary = root.resolveSibling(root.getFileName() + ".packing");
        Path retired = root.resolveSibling(root.getFileName() + ".retiring");
        // This warm owned cache is authoritative; retirement leftovers are disposable.
        // This also recovers a crash after a previous thaw committed its warm copy.
        deleteTree(retired);
        long total = 0;
        try {
            try (var paths = Files.walk(root);
                 var zip = new ZipOutputStream(Files.newOutputStream(temporary))) {
                zip.setLevel(Deflater.BEST_COMPRESSION);
                var entries = paths.limit(MAX_ENTRIES + 1L).sorted().toList();
                if (entries.size() > MAX_ENTRIES) throw new IOException("Too many cache entries");
                for (Path path : entries) {
                    if (Files.isSymbolicLink(path)) throw new IOException("Symlink in cache: " + path);
                    if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) continue;
                    if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                        throw new IOException("Non-regular cache entry: " + path);
                    }
                    var entry = new ZipEntry(root.relativize(path).toString().replace('\\', '/'));
                    entry.setTime(0);
                    zip.putNextEntry(entry);
                    MessageDigest digest = sha256();
                    try (InputStream input = Files.newInputStream(path)) {
                        total += copy(input, zip, digest, maxBytes - total);
                    }
                    entry.setComment(HexFormat.of().formatHex(digest.digest()));
                    zip.closeEntry();
                }
            }
            long saving = total - Files.size(temporary);
            if (saving < 4096) return 0;
            // Read/decompress every byte and check SHA-256 before retiring the source.
            verifyOrExtract(temporary, null, maxBytes);
            Files.move(temporary, cold, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            Files.move(root, retired, StandardCopyOption.ATOMIC_MOVE);
            deleteTree(retired);
            return saving;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    /** Restore locally into staging; failures leave the committed archive intact. */
    public static void restore(Path directory, long maxBytes, Predicate<Path> validator) throws IOException {
        Path root = directory.toAbsolutePath().normalize();
        Path cold = archive(root);
        if (Files.exists(root)) {
            if (!validator.test(root)) throw new IOException("Invalid existing cache: " + root);
            // A crash may have happened after the verified warm copy was committed.
            Files.deleteIfExists(cold);
            deleteTree(root.resolveSibling(root.getFileName() + ".retiring"));
            return;
        }
        Path staging = root.resolveSibling(root.getFileName() + ".restoring");
        deleteTree(staging);
        Files.createDirectories(staging);
        try {
            verifyOrExtract(cold, staging, maxBytes);
            if (!validator.test(staging)) throw new IOException("Restored cache is invalid");
            Files.move(staging, root, StandardCopyOption.ATOMIC_MOVE);
            // The warm copy is now committed. Avoid retaining two copies on disk.
            Files.delete(cold);
            deleteTree(root.resolveSibling(root.getFileName() + ".retiring"));
        } finally {
            deleteTree(staging);
        }
    }

    private static void verifyOrExtract(Path archive, Path target, long maxBytes) throws IOException {
        long total = 0;
        var names = new HashSet<String>();
        try (var zip = new ZipFile(archive.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                Path relative = Path.of(name).normalize();
                if (entry.isDirectory() || name.contains("\\") || name.contains(":")
                        || relative.isAbsolute() || relative.startsWith("..")
                        || !relative.toString().replace('\\', '/').equals(name)
                        || !names.add(name) || names.size() > MAX_ENTRIES) {
                    throw new IOException("Invalid cache entry: " + name);
                }
                String expected = entry.getComment();
                if (expected == null || !expected.matches("[0-9a-f]{64}")) {
                    throw new IOException("Missing cache checksum: " + name);
                }
                Path output = target == null ? null : target.resolve(relative);
                if (output != null) Files.createDirectories(output.getParent());
                MessageDigest digest = sha256();
                try (InputStream input = zip.getInputStream(entry);
                     OutputStream out = output == null ? OutputStream.nullOutputStream() : Files.newOutputStream(output)) {
                    total += copy(input, out, digest, maxBytes - total);
                }
                if (!expected.equals(HexFormat.of().formatHex(digest.digest()))) {
                    throw new IOException("Cache checksum mismatch: " + name);
                }
            }
        }
        if (names.isEmpty()) throw new IOException("Empty cache archive");
    }

    private static long copy(InputStream in, OutputStream out, MessageDigest digest, long limit) throws IOException {
        byte[] buffer = new byte[65536];
        long count = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            count += read;
            if (count > limit) throw new IOException("Cache exceeds size limit");
            digest.update(buffer, 0, read);
            out.write(buffer, 0, read);
        }
        return count;
    }

    private static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
        }
    }
}
