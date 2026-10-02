package net.caravidro.wayaround.performance;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

import net.caravidro.wayaround.WayAround;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Opt-in profiler for Way Around's own hot paths.
 *
 * Disabled cost is intentionally tiny: one volatile read + predictable branch
 * in each instrumented section. When enabled, counters are lock-free so the
 * integrated server and client render thread can contribute to one report.
 */
public final class PerformanceProfiler {

    public enum Section {
        ECOLOGY_SUCCESSION("Ecology succession"),
        FAUNA_AI("Fauna AI"),
        MARINE_AI("Marine interactions"),
        FISH_SPAWN_RULES("Fish spawn rules"),
        DEEP_OCEAN("Deep-ocean management"),
        WATER_FLOW("Water flow sampling"),
        PIPE_ROUTING("Pipe routing"),
        POWER_NETWORK("Mechanical power graph"),
        WATER_WHEEL_SIM("Water-wheel simulation"),
        MACHINE_SIM("Industrial machine simulation"),
        SUBMARINE_LIGHTING("Submarine real lighting"),
        BLIZZARD("Blizzard simulation/effects"),
        VOICE("Voice relay"),
        LOCAL_WEATHER("Local weather sampling"),
        CLOUD_RENDER("Cloud rendering"),
        CLOUD_SHADOW_UPDATE("Cloud shadow sampling"),
        CLOUD_SHADOW_RENDER("Cloud shadow rendering"),
        WATER_RENDER("Water overlay rendering"),
        MACHINE_RENDER("Procedural machine rendering");

        private final String label;

        Section(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private static final class Counter {
        private final LongAdder calls =
                new LongAdder();

        private final LongAdder totalNanos =
                new LongAdder();

        private final LongAccumulator maxNanos =
                new LongAccumulator(
                        Math::max,
                        0L
                );

        void add(long nanos) {
            calls.increment();
            totalNanos.add(nanos);
            maxNanos.accumulate(nanos);
        }

        SectionSnapshot snapshot(Section section) {
            return new SectionSnapshot(
                    section,
                    calls.sum(),
                    totalNanos.sum(),
                    maxNanos.get()
            );
        }

        void reset() {
            calls.reset();
            totalNanos.reset();
            maxNanos.reset();
        }
    }

    public record SectionSnapshot(
            Section section,
            long calls,
            long totalNanos,
            long maxNanos
    ) {
        public double totalMillis() {
            return totalNanos / 1_000_000.0;
        }

        public double averageMicros() {
            return calls == 0L
                    ? 0.0
                    : totalNanos / (double) calls / 1_000.0;
        }

        public double maxMillis() {
            return maxNanos / 1_000_000.0;
        }

        public double callsPerSecond(double elapsedSeconds) {
            return elapsedSeconds <= 0.0
                    ? 0.0
                    : calls / elapsedSeconds;
        }
    }

    public record Snapshot(
            long elapsedNanos,
            long heapStartBytes,
            long heapEndBytes,
            long gcCountDelta,
            long gcTimeMillisDelta,
            long observedServerTicks,
            List<SectionSnapshot> sections
    ) {
        public double elapsedSeconds() {
            return elapsedNanos / 1_000_000_000.0;
        }

        public double heapDeltaMiB() {
            return (heapEndBytes - heapStartBytes)
                    / (1024.0 * 1024.0);
        }

        public double estimatedTps() {
            double seconds =
                    elapsedSeconds();

            return seconds <= 0.0
                    ? 0.0
                    : observedServerTicks
                            / seconds;
        }
    }

    private static final Counter[] COUNTERS =
            new Counter[
                    Section.values().length
            ];

    private static final LongAdder OBSERVED_SERVER_TICKS =
            new LongAdder();

    private static volatile boolean enabled;

    private static volatile long stopAtNano =
            Long.MAX_VALUE;

    private static long startedNano;
    private static long startedHeap;
    private static long startedGcCount;
    private static long startedGcTimeMillis;

    private static volatile Snapshot lastSnapshot;

    static {
        for (int index = 0;
             index < COUNTERS.length;
             index++) {
            COUNTERS[index] =
                    new Counter();
        }
    }

    private PerformanceProfiler() {
    }

    /**
     * Returns zero while disabled so callers can unconditionally invoke end().
     */
    public static long begin(
            Section section
    ) {
        if (!enabled) {
            return 0L;
        }

        return System.nanoTime();
    }

    public static void end(
            Section section,
            long startedAt
    ) {
        if (startedAt == 0L) {
            return;
        }

        long elapsed =
                System.nanoTime()
                        - startedAt;

        if (elapsed < 0L) {
            return;
        }

        COUNTERS[
                section.ordinal()
        ].add(
                elapsed
        );
    }

    public static synchronized void start(
            MinecraftServer server,
            int seconds
    ) {
        enabled =
                false;

        resetCounters();

        OBSERVED_SERVER_TICKS.reset();

        startedNano =
                System.nanoTime();

        startedHeap =
                usedHeap();

        startedGcCount =
                gcCount();

        startedGcTimeMillis =
                gcTimeMillis();

        stopAtNano =
                startedNano
                        + Math.max(
                        1,
                        seconds
                ) * 1_000_000_000L;

        lastSnapshot =
                null;

        enabled =
                true;

        WayAround.LOGGER.info(
                "WayAround profiler iniciado por {}s de tempo real.",
                seconds
        );
    }

    public static synchronized Snapshot stop() {
        if (!enabled) {
            return lastSnapshot;
        }

        enabled =
                false;

        Snapshot snapshot =
                snapshotNow();

        lastSnapshot =
                snapshot;

        stopAtNano =
                Long.MAX_VALUE;

        return snapshot;
    }

    public static boolean enabled() {
        return enabled;
    }

    public static long remainingSeconds(
            MinecraftServer server
    ) {
        if (!enabled
                || stopAtNano
                == Long.MAX_VALUE) {
            return 0L;
        }

        long remainingNanos =
                Math.max(
                        0L,
                        stopAtNano
                                - System.nanoTime()
                );

        return (
                remainingNanos
                        + 999_999_999L
        ) / 1_000_000_000L;
    }

    public static Snapshot reportSnapshot() {
        if (enabled) {
            return snapshotNow();
        }

        return lastSnapshot;
    }

    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        if (!enabled) {
            return;
        }

        OBSERVED_SERVER_TICKS.increment();

        if (System.nanoTime()
                < stopAtNano) {
            return;
        }

        Snapshot snapshot =
                stop();

        if (snapshot == null) {
            return;
        }

        WayAround.LOGGER.info(
                "WayAround profiler finalizado automaticamente."
        );

        for (String line :
                formatLines(
                        snapshot,
                        12
                )) {
            WayAround.LOGGER.info(
                    "{}",
                    line
            );
        }
    }

    public static List<String> formatLines(
            Snapshot snapshot,
            int limit
    ) {
        if (snapshot == null) {
            return List.of(
                    "Nenhuma captura disponível."
            );
        }

        double elapsedSeconds =
                Math.max(
                        0.000001,
                        snapshot.elapsedSeconds()
                );

        List<SectionSnapshot> ordered =
                snapshot.sections()
                        .stream()
                        .filter(
                                stat ->
                                        stat.calls()
                                                > 0L
                        )
                        .sorted(
                                Comparator.comparingLong(
                                                SectionSnapshot::totalNanos
                                        )
                                        .reversed()
                        )
                        .limit(
                                Math.max(
                                        1,
                                        limit
                                )
                        )
                        .toList();

        List<String> lines =
                new ArrayList<>(
                        ordered.size()
                                + 3
                );

        lines.add(
                String.format(
                        Locale.ROOT,
                        "WAYPERF %.2fs | TPS ~%.2f | heap %+.2f MiB | GC %d coleta(s), %d ms",
                        elapsedSeconds,
                        snapshot.estimatedTps(),
                        snapshot.heapDeltaMiB(),
                        snapshot.gcCountDelta(),
                        snapshot.gcTimeMillisDelta()
                )
        );

        lines.add(
                "Tempos são inclusivos: uma seção pode chamar outra; não some os totais como se fossem exclusivos."
        );

        if (ordered.isEmpty()) {
            lines.add(
                    "Nenhuma seção instrumentada executou nesta janela."
            );

            return lines;
        }

        int rank =
                1;

        for (SectionSnapshot stat :
                ordered) {

            lines.add(
                    String.format(
                            Locale.ROOT,
                            "%02d. %s | total %.3f ms | %d calls | avg %.2f us | max %.3f ms | %.1f/s",
                            rank++,
                            stat.section()
                                    .label(),
                            stat.totalMillis(),
                            stat.calls(),
                            stat.averageMicros(),
                            stat.maxMillis(),
                            stat.callsPerSecond(
                                    elapsedSeconds
                            )
                    )
            );
        }

        return lines;
    }

    public static Path dumpCsv(
            Snapshot snapshot
    ) throws IOException {
        if (snapshot == null) {
            throw new IOException(
                    "Nenhuma captura disponível."
            );
        }

        Path directory =
                Path.of(
                        "logs"
                );

        Files.createDirectories(
                directory
        );

        String stamp =
                DateTimeFormatter.ofPattern(
                                "yyyyMMdd-HHmmss"
                        )
                        .withZone(
                                ZoneOffset.UTC
                        )
                        .format(
                                Instant.now()
                        );

        Path output =
                directory.resolve(
                        "wayaround-profile-"
                                + stamp
                                + ".csv"
                );

        StringBuilder csv =
                new StringBuilder(
                        2048
                );

        csv.append(
                "type,name,value,total_ms,calls,avg_us,max_ms,calls_per_second\n"
        );

        csv.append(
                "meta,elapsed_seconds,"
        ).append(
                format(
                        snapshot.elapsedSeconds()
                )
        ).append(
                ",,,,,\n"
        );

        csv.append(
                "meta,observed_server_ticks,"
        ).append(
                snapshot.observedServerTicks()
        ).append(
                ",,,,,\n"
        );

        csv.append(
                "meta,estimated_tps,"
        ).append(
                format(
                        snapshot.estimatedTps()
                )
        ).append(
                ",,,,,\n"
        );

        csv.append(
                "meta,heap_delta_mib,"
        ).append(
                format(
                        snapshot.heapDeltaMiB()
                )
        ).append(
                ",,,,,\n"
        );

        csv.append(
                "meta,gc_count,"
        ).append(
                snapshot.gcCountDelta()
        ).append(
                ",,,,,\n"
        );

        csv.append(
                "meta,gc_time_ms,"
        ).append(
                snapshot.gcTimeMillisDelta()
        ).append(
                ",,,,,\n"
        );

        csv.append(
                "meta,available_processors,"
        ).append(
                Runtime.getRuntime()
                        .availableProcessors()
        ).append(
                ",,,,,\n"
        );

        csv.append(
                "meta,max_heap_mib,"
        ).append(
                format(
                        Runtime.getRuntime()
                                .maxMemory()
                                / (1024.0 * 1024.0)
                )
        ).append(
                ",,,,,\n"
        );

        csv.append(
                "meta,java_version,"
        ).append(
                System.getProperty(
                        "java.version",
                        "unknown"
                ).replace(
                        ',',
                        '_'
                )
        ).append(
                ",,,,,\n"
        );

        double elapsedSeconds =
                Math.max(
                        0.000001,
                        snapshot.elapsedSeconds()
                );

        for (SectionSnapshot stat :
                snapshot.sections()) {

            csv.append(
                    "section,"
            ).append(
                    stat.section()
                            .name()
            ).append(
                    ",,"
            ).append(
                    format(
                            stat.totalMillis()
                    )
            ).append(
                    ','
            ).append(
                    stat.calls()
            ).append(
                    ','
            ).append(
                    format(
                            stat.averageMicros()
                    )
            ).append(
                    ','
            ).append(
                    format(
                            stat.maxMillis()
                    )
            ).append(
                    ','
            ).append(
                    format(
                            stat.callsPerSecond(
                                    elapsedSeconds
                            )
                    )
            ).append(
                    '\n'
            );
        }

        Files.writeString(
                output,
                csv,
                StandardCharsets.UTF_8
        );

        return output;
    }

    private static Snapshot snapshotNow() {
        long elapsed =
                Math.max(
                        0L,
                        System.nanoTime()
                                - startedNano
                );

        List<SectionSnapshot> sections =
                new ArrayList<>(
                        COUNTERS.length
                );

        Section[] values =
                Section.values();

        for (int index = 0;
             index < values.length;
             index++) {

            sections.add(
                    COUNTERS[index]
                            .snapshot(
                                    values[index]
                            )
            );
        }

        return new Snapshot(
                elapsed,
                startedHeap,
                usedHeap(),
                Math.max(
                        0L,
                        gcCount()
                                - startedGcCount
                ),
                Math.max(
                        0L,
                        gcTimeMillis()
                                - startedGcTimeMillis
                ),
                OBSERVED_SERVER_TICKS.sum(),
                List.copyOf(
                        sections
                )
        );
    }

    private static void resetCounters() {
        for (Counter counter :
                COUNTERS) {
            counter.reset();
        }
    }

    private static long usedHeap() {
        Runtime runtime =
                Runtime.getRuntime();

        return runtime.totalMemory()
                - runtime.freeMemory();
    }

    private static long gcCount() {
        long total =
                0L;

        for (GarbageCollectorMXBean bean :
                ManagementFactory.getGarbageCollectorMXBeans()) {

            long count =
                    bean.getCollectionCount();

            if (count > 0L) {
                total +=
                        count;
            }
        }

        return total;
    }

    private static long gcTimeMillis() {
        long total =
                0L;

        for (GarbageCollectorMXBean bean :
                ManagementFactory.getGarbageCollectorMXBeans()) {

            long time =
                    bean.getCollectionTime();

            if (time > 0L) {
                total +=
                        time;
            }
        }

        return total;
    }

    private static String format(
            double value
    ) {
        return String.format(
                Locale.ROOT,
                "%.6f",
                value
        );
    }
}
