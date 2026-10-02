package net.caravidro.wayaround.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class StorageRegressionTest {
    private static final long LIMIT = 4 * 1024 * 1024;
    private static int checks;

    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("wayaround-storage-test");
        try {
            Path model = temp.resolve("model");
            Files.createDirectories(model.resolve("am"));
            byte[] original = "model parameter 123456789\n".repeat(40000).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            Files.write(model.resolve("am/final.mdl"), original);
            long saved = ColdDirectoryArchive.freeze(model, LIMIT);
            check(saved > original.length / 2, "compressible model saves space");
            check(!Files.exists(model) && ColdDirectoryArchive.isCold(model), "only cold copy remains");
            fail(() -> ColdDirectoryArchive.restore(model, 100, p -> true), "expanded byte limit");
            check(ColdDirectoryArchive.isCold(model) && !Files.exists(model), "failed restore keeps cold copy");
            fail(() -> ColdDirectoryArchive.restore(model, LIMIT, p -> false), "validation rejection");
            ColdDirectoryArchive.restore(model, LIMIT, p -> Files.exists(p.resolve("am/final.mdl")));
            check(Arrays.equals(original, Files.readAllBytes(model.resolve("am/final.mdl"))), "byte-perfect thaw");
            check(!ColdDirectoryArchive.isCold(model), "warm copy replaces archive");
            ColdDirectoryArchive.freeze(model, LIMIT);
            // Simulate a crash after committing cold storage but during retired-tree deletion.
            Files.createDirectories(temp.resolve("model.retiring"));
            Files.writeString(temp.resolve("model.retiring/leftover"), "partial deletion");
            ColdDirectoryArchive.restore(model, LIMIT, p -> true);
            check(!Files.exists(temp.resolve("model.retiring")), "interrupted retirement recovers");
            // A crash immediately after committing the warm directory can leave both copies.
            Files.writeString(ColdDirectoryArchive.archive(model), "obsolete archive placeholder");
            Files.createDirectories(temp.resolve("model.retiring"));
            ColdDirectoryArchive.restore(model, LIMIT, p -> Files.exists(p.resolve("am/final.mdl")));
            check(!ColdDirectoryArchive.isCold(model) && !Files.exists(temp.resolve("model.retiring")),
                    "post-commit thaw interruption cleaned up");
            check(Arrays.equals(original, Files.readAllBytes(model.resolve("am/final.mdl"))),
                    "recovery keeps committed warm bytes");

            Path random = temp.resolve("random");
            Files.createDirectories(random);
            byte[] noise = new byte[64000];
            new Random(21).nextBytes(noise);
            Files.write(random.resolve("noise"), noise);
            check(ColdDirectoryArchive.freeze(random, LIMIT) == 0, "incompressible cache stays warm");
            check(Arrays.equals(noise, Files.readAllBytes(random.resolve("noise"))), "incompressible data retained");
            fail(() -> ColdDirectoryArchive.freeze(random, 100), "packing byte limit");
            check(Files.exists(random.resolve("noise")), "failed pack preserves source");

            Path malicious = temp.resolve("malicious");
            writeZip(ColdDirectoryArchive.archive(malicious), "../escape", "0".repeat(64));
            fail(() -> ColdDirectoryArchive.restore(malicious, LIMIT, p -> true), "path traversal blocked");
            check(!Files.exists(temp.resolve("escape")), "no escaped output");
            writeZip(ColdDirectoryArchive.archive(malicious), "model", "0".repeat(64));
            fail(() -> ColdDirectoryArchive.restore(malicious, LIMIT, p -> true), "checksum corruption blocked");
            check(!Files.exists(malicious) && ColdDirectoryArchive.isCold(malicious), "corrupt archive retained for recovery");

            Path linked = temp.resolve("linked");
            Files.createDirectories(linked);

            /*
             * Creating a symlink on Windows may require Developer Mode or the
             * SeCreateSymbolicLinkPrivilege. That is an environment capability,
             * not a WayAround archive failure. Exercise the rejection path when
             * the host can create links; otherwise keep the rest of the storage
             * regression suite meaningful instead of failing during test setup.
             */
            boolean symlinkCreated = false;

            try {
                Files.createSymbolicLink(
                        linked.resolve("secret"),
                        random.resolve("noise")
                );

                symlinkCreated = true;

            } catch (UnsupportedOperationException
                     | IOException
                     | SecurityException unavailable) {

                System.out.println(
                        "SKIP: symbolic-link rejection check (host cannot create test symlink: "
                                + unavailable.getClass().getSimpleName()
                                + ")"
                );
            }

            if (symlinkCreated) {
                fail(
                        () -> ColdDirectoryArchive.freeze(linked, LIMIT),
                        "symlink rejected"
                );

                check(
                        Files.exists(random.resolve("noise")),
                        "symlink target untouched"
                );
            }

            for (int radius : new int[]{0, 1, 2, 22, 24, 128}) {
                var scan = new IncrementalSquareScan(radius);
                var points = new HashSet<Long>();
                int previousRing = 0;
                while (scan.advance()) {
                    int ring = Math.max(Math.abs(scan.x()), Math.abs(scan.z()));
                    check(ring <= radius && ring >= previousRing, "center-out radius bounds");
                    previousRing = ring;
                    check(points.add(((long) scan.x() << 32) ^ (scan.z() & 0xffffffffL)), "no duplicate columns");
                }
                check(points.size() == (2 * radius + 1) * (2 * radius + 1), "complete coverage");
                check(scan.complete() && !scan.advance(), "completion stable");
            }
            System.out.println("PASS: " + checks + " archive integrity, recovery and scan checks");
        } finally {
            try (var paths = Files.walk(temp)) {
                for (Path p : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(p);
            }
        }
    }

    private static void writeZip(Path file, String name, String checksum) throws Exception {
        try (var zip = new ZipOutputStream(Files.newOutputStream(file))) {
            var entry = new ZipEntry(name);
            entry.setComment(checksum);
            zip.putNextEntry(entry);
            zip.write(new byte[]{1, 2, 3});
            zip.closeEntry();
        }
    }
    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError(description);
    }
    private static void fail(IORunnable operation, String description) throws Exception {
        boolean failed = false;
        try { operation.run(); } catch (IOException expected) { failed = true; }
        check(failed, description);
    }
    private interface IORunnable { void run() throws Exception; }
}
