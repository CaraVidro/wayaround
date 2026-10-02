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
        SERVER_TICK("Whole server tick"),
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
        /*
         * Log2 nanosecond histogram. This gives useful p95/p99 numbers without
         * retaining individual samples or allocating during a profiled call.
         */
        private static final int HISTOGRAM_BUCKETS =
                64;

        private final LongAdder calls =
                new LongAdder();

        private final LongAdder totalNanos =
                new LongAdder();

        private final LongAccumulator maxNanos =
                new LongAccumulator(
                        Math::max,
                        0L
                );

        private final LongAdder[] histogram =
                new LongAdder[
                        HISTOGRAM_BUCKETS
                ];

        Counter() {
            for (int index = 0;
                 index < histogram.length;
                 index++) {
                histogram[index] =
                        new LongAdder();
            }
        }

        void add(long nanos) {
            long bounded =
                    Math.max(
                            1L,
                            nanos
                    );

            calls.increment();
            totalNanos.add(
                    bounded
            );
            maxNanos.accumulate(
                    bounded
            );

            int bucket =
                    63
                            - Long.numberOfLeadingZeros(
                            bounded
                    );

            histogram[
                    Math.min(
                            histogram.length - 1,
                            bucket
                    )
            ].increment();
        }

        SectionSnapshot snapshot(Section section) {
            long callCount =
                    calls.sum();

            return new SectionSnapshot(
                    section,
                    callCount,
                    totalNanos.sum(),
                    maxNanos.get(),
                    percentileNanos(
                            callCount,
                            0.95
                    ),
                    percentileNanos(
                            callCount,
                            0.99
                    )
            );
        }

        private long percentileNanos(
                long callCount,
                double percentile
        ) {
            if (callCount <= 0L) {
                return 0L;
            }

            long target =
                    Math.max(
                            1L,
                            (long) Math.ceil(
                                    callCount
                                            * percentile
                            )
                    );

            long seen =
                    0L;

            for (int bucket = 0;
                 bucket < histogram.length;
                 bucket++) {

                seen +=
                        histogram[
                                bucket
                        ].sum();

                if (seen >= target) {
                    if (bucket >= 62) {
                        return Long.MAX_VALUE;
                    }

                    /*
                     * Return the upper edge of the bucket. Percentiles are
                     * intentionally approximate; maxima stay exact.
                     */
                    return 1L
                            << (
                            bucket + 1
                    );
                }
            }

            return maxNanos.get();
        }

        void reset() {
            calls.reset();
            totalNanos.reset();
            maxNanos.reset();

            for (LongAdder bucket :
                    histogram) {
                bucket.reset();
            }
        }
    }

    public record SectionSnapshot(
            Section section,
            long calls,
            long totalNanos,
            long maxNanos,
            long p95Nanos,
            long p99Nanos
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

        public double p95Millis() {
            return p95Nanos / 1_000_000.0;
        }

        public double p99Millis() {
            return p99Nanos / 1_000_000.0;
        }

        public double callsPerSecond(double elapsedSeconds) {
            return elapsedSeconds <= 0.0
                    ? 0.0
                    : calls / elapsedSeconds;
        }

        public double millisPerSecond(double elapsedSeconds) {
            return elapsedSeconds <= 0.0
                    ? 0.0
                    : totalMillis()
                            / elapsedSeconds;
        }

        public double wallSharePercent(double elapsedSeconds) {
            return millisPerSecond(
                    elapsedSeconds
            ) / 10.0;
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

    private static volatile Snapshot baselineSnapshot;

    private static volatile long serverTickStartedAt;

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

        serverTickStartedAt =
                0L;

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

    public static void onServerTickPre(
            ServerTickEvent.Pre event
    ) {
        serverTickStartedAt =
                begin(
                        Section.SERVER_TICK
                );
    }

    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        if (!enabled) {
            serverTickStartedAt =
                    0L;
            return;
        }

        long tickStartedAt =
                serverTickStartedAt;

        serverTickStartedAt =
                0L;

        end(
                Section.SERVER_TICK,
                tickStartedAt
        );

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
                            "%02d. %s | %.2f ms/s (%.2f%% wall) | %d calls | avg %.2f us | p95 %.3f ms | p99 %.3f ms | max %.3f ms | %.1f/s",
                            rank++,
                            stat.section()
                                    .label(),
                            stat.millisPerSecond(
                                    elapsedSeconds
                            ),
                            stat.wallSharePercent(
                                    elapsedSeconds
                            ),
                            stat.calls(),
                            stat.averageMicros(),
                            stat.p95Millis(),
                            stat.p99Millis(),
                            stat.maxMillis(),
                            stat.callsPerSecond(
                                    elapsedSeconds
                            )
                    )
            );
        }

        return lines;
    }

    public static Snapshot pinBaseline() {
        Snapshot snapshot =
                reportSnapshot();

        baselineSnapshot =
                snapshot;

        return snapshot;
    }

    public static Snapshot baselineSnapshot() {
        return baselineSnapshot;
    }

    public static List<String> formatComparison(
            Snapshot baseline,
            Snapshot current,
            int limit
    ) {
        if (baseline == null
                || current == null) {
            return List.of(
                    "Baseline ou captura atual ausente."
            );
        }

        double baselineSeconds =
                Math.max(
                        0.000001,
                        baseline.elapsedSeconds()
                );

        double currentSeconds =
                Math.max(
                        0.000001,
                        current.elapsedSeconds()
                );

        record Delta(
                SectionSnapshot baselineStat,
                SectionSnapshot currentStat,
                double baselineMsPerSecond,
                double currentMsPerSecond,
                double deltaPercent
        ) {
        }

        List<Delta> deltas =
                new ArrayList<>();

        for (Section section :
                Section.values()) {

            SectionSnapshot baselineStat =
                    baseline.sections()
                            .get(
                                    section.ordinal()
                            );

            SectionSnapshot currentStat =
                    current.sections()
                            .get(
                                    section.ordinal()
                            );

            double before =
                    baselineStat.millisPerSecond(
                            baselineSeconds
                    );

            double after =
                    currentStat.millisPerSecond(
                            currentSeconds
                    );

            double deltaPercent =
                    before <= 0.000001
                            ? (
                            after <= 0.000001
                                    ? 0.0
                                    : Double.POSITIVE_INFINITY
                    )
                            : (
                            after - before
                    ) / before * 100.0;

            deltas.add(
                    new Delta(
                            baselineStat,
                            currentStat,
                            before,
                            after,
                            deltaPercent
                    )
            );
        }

        deltas.sort(
                Comparator.comparingDouble(
                                delta ->
                                        Math.abs(
                                                delta.currentMsPerSecond()
                                                        - delta.baselineMsPerSecond()
                                        )
                        )
                        .reversed()
        );

        List<String> lines =
                new ArrayList<>();

        lines.add(
                String.format(
                        Locale.ROOT,
                        "WAYPERF compare | baseline %.2fs -> atual %.2fs | TPS %.2f -> %.2f | heap %+.2f -> %+.2f MiB",
                        baselineSeconds,
                        currentSeconds,
                        baseline.estimatedTps(),
                        current.estimatedTps(),
                        baseline.heapDeltaMiB(),
                        current.heapDeltaMiB()
                )
        );

        int emitted =
                0;

        for (Delta delta :
                deltas) {

            if (delta.baselineStat().calls() == 0L
                    && delta.currentStat().calls() == 0L) {
                continue;
            }

            String percent =
                    Double.isFinite(
                            delta.deltaPercent()
                    )
                            ? String.format(
                            Locale.ROOT,
                            "%+.1f%%",
                            delta.deltaPercent()
                    )
                            : "novo";

            lines.add(
                    String.format(
                            Locale.ROOT,
                            "%s | %.3f -> %.3f ms/s (%s) | p95 %.3f -> %.3f ms | calls/s %.1f -> %.1f",
                            delta.currentStat()
                                    .section()
                                    .label(),
                            delta.baselineMsPerSecond(),
                            delta.currentMsPerSecond(),
                            percent,
                            delta.baselineStat()
                                    .p95Millis(),
                            delta.currentStat()
                                    .p95Millis(),
                            delta.baselineStat()
                                    .callsPerSecond(
                                            baselineSeconds
                                    ),
                            delta.currentStat()
                                    .callsPerSecond(
                                            currentSeconds
                                    )
                    )
            );

            if (++emitted
                    >= Math.max(
                    1,
                    limit
            )) {
                break;
            }
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
                "type,name,value,total_ms,calls,avg_us,p95_ms,p99_ms,max_ms,calls_per_second,ms_per_second,wall_share_percent\n"
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
                            stat.p95Millis()
                    )
            ).append(
                    ','
            ).append(
                    format(
                            stat.p99Millis()
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
                    ','
            ).append(
                    format(
                            stat.millisPerSecond(
                                    elapsedSeconds
                            )
                    )
            ).append(
                    ','
            ).append(
                    format(
                            stat.wallSharePercent(
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
