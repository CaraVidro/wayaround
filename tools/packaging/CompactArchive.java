import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/** Recompresses JAR/ZIP bytes losslessly. No runtime unpacking or custom class loader. */
public final class CompactArchive {
    public static void main(String[] args) throws IOException {
        Path archive = Path.of(args[0]);
        Path temporary = archive.resolveSibling(archive.getFileName() + ".compacting");
        long before = Files.size(archive);
        try {
            try (var source = new ZipFile(archive.toFile());
                 var target = new ZipOutputStream(Files.newOutputStream(temporary))) {
                target.setLevel(Deflater.BEST_COMPRESSION);
                var entries = source.entries();
                var names = new HashSet<String>();
                while (entries.hasMoreElements()) {
                    ZipEntry original = entries.nextElement();
                    if (!names.add(original.getName())) throw new IOException("Duplicate archive entry");
                    ZipEntry copy = new ZipEntry(original.getName());
                    copy.setTime(315532800000L); // Stable DOS epoch; no host timestamp metadata.
                    target.putNextEntry(copy);
                    try (var input = source.getInputStream(original)) { input.transferTo(target); }
                    target.closeEntry();
                }
            }
            // Verify both length and CRC against the original central directory.
            try (var source = new ZipFile(archive.toFile()); var packed = new ZipFile(temporary.toFile())) {
                var entries = source.entries();
                byte[] buffer = new byte[65536];
                while (entries.hasMoreElements()) {
                    ZipEntry original = entries.nextElement();
                    ZipEntry entry = packed.getEntry(original.getName());
                    CRC32 crc = new CRC32();
                    long size = 0;
                    try (var input = packed.getInputStream(entry)) {
                        int read;
                        while ((read = input.read(buffer)) != -1) {
                            crc.update(buffer, 0, read);
                            size += read;
                        }
                    }
                    if (size != original.getSize() || crc.getValue() != original.getCrc()) {
                        throw new IOException("Archive verification failed: " + original.getName());
                    }
                }
            }
            long after = Files.size(temporary);
            if (after < before) Files.move(temporary, archive, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Archive: " + before + " -> " + Files.size(archive) + " bytes");
        } finally { Files.deleteIfExists(temporary); }
    }
}
