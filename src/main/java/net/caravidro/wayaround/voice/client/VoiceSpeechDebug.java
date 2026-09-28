package net.caravidro.wayaround.voice.client;

import java.util.Locale;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.JusticeVoiceStatementC2SPayload;
import net.caravidro.wayaround.network.TukunaVoiceStatementC2SPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class VoiceSpeechDebug {

    private VoiceSpeechDebug() {
    }

    private static final ThreadPoolExecutor WORKER =
            new ThreadPoolExecutor(
                    1,
                    1,
                    0L,
                    TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(6),
                    runnable -> {
                        Thread thread =
                                new Thread(
                                        runnable,
                                        "WayAround-SpeechDebug"
                                );

                        thread.setDaemon(true);

                        return thread;
                    },
                    new ThreadPoolExecutor.DiscardOldestPolicy()
            );

    private static final ThreadPoolExecutor STREAM_WORKER =
            new ThreadPoolExecutor(
                    1,
                    1,
                    0L,
                    TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(),
                    runnable -> {
                        Thread thread =
                                new Thread(
                                        runnable,
                                        "WayAround-SpeechRealtime"
                                );

                        thread.setDaemon(true);
                        return thread;
                    },
                    new ThreadPoolExecutor.AbortPolicy()
            );

    private static final AtomicLong STREAM_GENERATION =
            new AtomicLong();

    private static volatile VoskSpeechRecognizer.StreamingSession streamSession;

    private static final ByteArrayOutputStream STREAM_PCM = new ByteArrayOutputStream();
    private static boolean drainQueued;
    private static long pcmGeneration;
    private static final AtomicBoolean WARM_REQUESTED = new AtomicBoolean();
    private static VoskSpeechRecognizer.StreamingSession preparedSession;
    private static volatile long retryWarmAt;
    private static volatile String liveDebug = "";
    private static volatile long liveDebugUntil;
    private static long decodeMillis;
    private static long queuedAtNanos;
    private static long queueMillis;

    public static boolean isRealtimeActive() { return streamSession != null; }

    public static String liveDebugText() {
        return System.currentTimeMillis() < liveDebugUntil ? liveDebug : "";
    }

    /** Prepare only already installed model data, before the first syllable. */
    public static void prepareRealtime() {
        if (!VoiceConfig.isEnabled() || System.currentTimeMillis() < retryWarmAt
                || !VoiceIntentClient.wantsContinuousRecognition()
                || !VoskSpeechRecognizer.isModelInstalled()
                || !WARM_REQUESTED.compareAndSet(false, true)) return;
        STREAM_WORKER.execute(() -> {
            try {
                if (VoiceConfig.isEnabled() && preparedSession == null && streamSession == null) {
                    preparedSession = VoskSpeechRecognizer.openStreamingSession();
                }
            } catch (Throwable failure) {
                retryWarmAt = System.currentTimeMillis() + 5_000L;
                WayAround.LOGGER.warn("[Voice/Realtime] warmup: {}", failure.toString());
            } finally {
                WARM_REQUESTED.set(false);
            }
        });
    }

    public static void beginRealtime() {
        Minecraft.getInstance().execute(() -> {
            VoiceIntentClient.beginRealtimeUtterance();
            if (VoiceConfig.isDebugSpeechEnabled()) {
                liveDebug = "Tobias AO VIVO: ouvindo...";
                liveDebugUntil = System.currentTimeMillis() + 4_000L;
            }
        });
        if (!VoiceIntentClient.wantsContinuousRecognition()
                || !VoskSpeechRecognizer.isModelInstalled()) {
            return;
        }

        long generation =
                STREAM_GENERATION.incrementAndGet();

        STREAM_WORKER.execute(
                () -> {
                    closeRealtimeSession();

                    if (generation
                            != STREAM_GENERATION.get()) {
                        return;
                    }

                    try {
                        streamSession = preparedSession != null
                                ? preparedSession : VoskSpeechRecognizer.openStreamingSession();
                        preparedSession = null;

                    } catch (Throwable throwable) {
                        WayAround.LOGGER.debug(
                                "[Voice/Realtime] falha ao abrir decoder: {}",
                                throwable.toString()
                        );
                    }
                }
        );
    }

    public static void feedRealtime(
            byte[] pcm
    ) {
        if (pcm == null
                || pcm.length == 0
                || !VoiceIntentClient.wantsContinuousRecognition()) {
            return;
        }

        long generation = STREAM_GENERATION.get();
        synchronized (STREAM_PCM) {
            if (pcmGeneration != generation) {
                STREAM_PCM.reset();
                pcmGeneration = generation;
            }
            // Bounded memory even if the native decoder stalls. Report overload.
            if (STREAM_PCM.size() + pcm.length > 192_000) {
                STREAM_PCM.reset();
                WayAround.LOGGER.warn("[Voice/Realtime] decoder atrasado: audio pendente excedeu 2s");
            }
            if (STREAM_PCM.size() == 0) queuedAtNanos = System.nanoTime();
            STREAM_PCM.write(pcm, 0, pcm.length);
            if (drainQueued) return;
            drainQueued = true;
        }
        STREAM_WORKER.execute(() -> drainRealtime(generation));
    }

    private static void drainRealtime(long generation) {
        byte[] pcm;
        synchronized (STREAM_PCM) {
            drainQueued = false;
            if (pcmGeneration != generation || generation != STREAM_GENERATION.get()) return;
            queueMillis = Math.max(0, (System.nanoTime() - queuedAtNanos) / 1_000_000L);
            pcm = STREAM_PCM.toByteArray();
            STREAM_PCM.reset();
        }
        VoskSpeechRecognizer.StreamingSession session = streamSession;
        if (session == null || pcm.length == 0) return;
        long started = System.nanoTime();
        try {
            String text = session.accept48k(pcm);
            decodeMillis = (System.nanoTime() - started) / 1_000_000L;
            dispatchRealtime(text, false, generation);
        } catch (Throwable failure) {
            WayAround.LOGGER.warn("[Voice/Realtime] decode: {}", failure.toString());
            closeRealtimeSession();
        }
    }

    public static void endRealtime() {
        long generation =
                STREAM_GENERATION.get();

        STREAM_WORKER.execute(
                () -> {
                    if (generation
                            != STREAM_GENERATION.get()) {
                        return;
                    }

                    drainRealtime(generation);

                    VoskSpeechRecognizer.StreamingSession session =
                            streamSession;

                    streamSession =
                            null;

                    if (session == null) {
                        return;
                    }

                    try {
                        dispatchRealtime(
                                session.finish(),
                                true, generation
                        );

                    } catch (Throwable ignored) {
                    } finally {
                        session.close();
                        prepareRealtime();
                    }
                }
        );
    }

    public static void cancelRealtime() {
        STREAM_GENERATION.incrementAndGet();

        STREAM_WORKER.execute(
                () -> {
                    closeRealtimeSession();
                    if (preparedSession != null) {
                        preparedSession.close();
                        preparedSession = null;
                    }
                    liveDebug = "";
                }
        );
    }

    private static void closeRealtimeSession() {
        VoskSpeechRecognizer.StreamingSession session =
                streamSession;

        streamSession =
                null;

        if (session != null) {
            try {
                session.close();
            } catch (Throwable ignored) {
            }
        }
    }

    private static void dispatchRealtime(
            String transcript,
            boolean finalChunk,
            long generation
    ) {
        if (transcript == null
                || transcript.isBlank()) {
            return;
        }

        String refined =
                BrazilianPortugueseSpeechNormalizer.refine(
                        transcript
                );

        if (refined.isBlank()) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        long processingMillis = decodeMillis;
        long waitingMillis = queueMillis;
        minecraft.execute(() -> {
            if (generation != STREAM_GENERATION.get() || minecraft.player == null
                    || !VoiceConfig.isEnabled()) return;
            if (VoiceConfig.isDebugSpeechEnabled()) {
                liveDebug = "Tobias " + (finalChunk ? "FINAL" : "AO VIVO")
                        + " [fila " + waitingMillis + " ms; decode " + processingMillis + " ms]: " + refined;
                liveDebugUntil = System.currentTimeMillis() + 4_000L;
                WayAround.LOGGER.info("[Voice/Realtime] {}", liveDebug);
            }
            VoiceIntentClient.handleRealtimeTranscript(refined, finalChunk);
        });
    }

    public static void submit(
            byte[] pcm
    ) {
        if (pcm == null
                || pcm.length == 0) {
            return;
        }

        byte[] copy =
                pcm.clone();

        if (!VoskSpeechRecognizer
                .isModelInstalled()
                && !VoskSpeechRecognizer
                .isPreparing()) {

            showClientMessage(
                    Component.literal(
                                    "[Voice Debug] "
                            )
                            .withStyle(
                                    ChatFormatting.AQUA
                            )
                            .append(
                                    Component.literal(
                                            "baixando o modelo PT-BR gratuito (31 MB); isso acontece so uma vez..."
                                    )
                                    .withStyle(
                                            ChatFormatting.GRAY
                                    )
                            )
            );
        }

        WayAround.LOGGER.info(
                "[Voice/STT] QUEUED bytes={} queue={}",
                copy.length,
                WORKER.getQueue()
                        .size()
        );

        WORKER.execute(
                () -> process(copy)
        );
    }

    public static void submitSpeculative(
            byte[] pcm
    ) {
        if (pcm == null
                || pcm.length == 0
                || WORKER.getActiveCount() > 0
                || !WORKER.getQueue()
                        .isEmpty()) {
            return;
        }

        byte[] copy =
                pcm.clone();

        WORKER.execute(
                () ->
                        processSpeculative(
                                copy
                        )
        );
    }

    private static void processSpeculative(
            byte[] pcm
    ) {
        VoskSpeechRecognizer.Result recognition;

        try {
            recognition =
                    VoskSpeechRecognizer.recognize(
                            pcm
                    );

        } catch (Throwable throwable) {
            return;
        }

        if (!recognition.success()) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        minecraft.execute(
                () ->
                        VoiceIntentClient.handleSpeculativeTranscript(
                                BrazilianPortugueseSpeechNormalizer.refine(
                                        recognition.text()
                                )
                        )
        );
    }

    private static void process(
            byte[] pcm
    ) {
        long started =
                System.nanoTime();

        WayAround.LOGGER.info(
                "[Voice/STT] BEGIN bytes={}",
                pcm.length
        );

        VoskSpeechRecognizer.Result recognition;

        try {
            recognition =
                    VoskSpeechRecognizer.recognize(
                            pcm
                    );

        } catch (Throwable throwable) {
            WayAround.LOGGER.error(
                    "[Voice/STT] worker morreu fora do recognizer: {}: {}",
                    throwable.getClass()
                            .getName(),
                    throwable.getMessage(),
                    throwable
            );

            return;
        }

        long recognitionMillis =
                (
                        System.nanoTime()
                                - started
                )
                        / 1_000_000L;

        if (!recognition.success()) {
            WayAround.LOGGER.warn(
                    "[Voice/STT] Falha em {} ms: {}",
                    recognitionMillis,
                    recognition.error()
            );

            if (VoiceConfig
                    .isDebugSpeechEnabled()) {

                showClientMessage(
                        Component.literal(
                                        "[Voice Debug] "
                                )
                                .withStyle(
                                        ChatFormatting.RED
                                )
                                .append(
                                        Component.literal(
                                                recognition.error()
                                        )
                                        .withStyle(
                                                ChatFormatting.GRAY
                                        )
                                )
                );
            }

            return;
        }

        WayAround.LOGGER.info(
                "[Voice/STT] Entendido em {} ms: \"{}\"",
                recognitionMillis,
                recognition.text()
        );

        long toneStarted =
                System.nanoTime();

        VoiceToneAnalyzer.ToneProfile profile =
                VoiceToneAnalyzer.analyze(
                        pcm
                );

        double urgency =
                VoiceToneAnalyzer.urgency(
                        profile
                );

        VoiceToneAnalyzer.KeywordEmphasis blueEmphasis =
                VoiceToneAnalyzer.analyzeKeyword(
                        pcm,
                        recognition.words(),
                        profile,
                        "azul",
                        "blue"
                );

        VoiceToneAnalyzer.KeywordEmphasis redEmphasis =
                VoiceToneAnalyzer.analyzeKeyword(
                        pcm,
                        recognition.words(),
                        profile,
                        "vermelho",
                        "red"
                );

        long toneMillis =
                (
                        System.nanoTime()
                                - toneStarted
                )
                        / 1_000_000L;

        WayAround.LOGGER.info(
                "[Voice/Tone] {} ms urgencia={} blueEmphasis={} redEmphasis={}",
                toneMillis,
                String.format(
                        Locale.ROOT,
                        "%.2f",
                        urgency
                ),
                String.format(
                        Locale.ROOT,
                        "%.2f",
                        blueEmphasis.score()
                ),
                String.format(
                        Locale.ROOT,
                        "%.2f",
                        redEmphasis.score()
                )
        );

        Minecraft minecraft =
                Minecraft.getInstance();

        String refinedTranscript =
                BrazilianPortugueseSpeechNormalizer.refine(
                        recognition.text()
                );

        minecraft.execute(
                () -> {
                    VoiceIntentClient.handleTranscript(
                            refinedTranscript,
                            profile,
                            blueEmphasis.score(),
                            redEmphasis.score()
                    );

                    if (minecraft.player != null
                            && minecraft.getConnection()
                            != null
                            && !refinedTranscript.isBlank()) {

                        PacketDistributor.sendToServer(
                                new JusticeVoiceStatementC2SPayload(
                                        refinedTranscript
                                )
                        );

                        PacketDistributor.sendToServer(
                                new TukunaVoiceStatementC2SPayload(
                                        refinedTranscript
                                )
                        );
                    }
                }
        );

        if (!VoiceConfig
                .isDebugSpeechEnabled()) {

            return;
        }

        minecraft.execute(
                () -> {
                    String expressive =
                            VoiceToneAnalyzer.applyExpression(
                                    recognition.text(),
                                    profile
                            );

                    minecraft.gui
                            .getChat()
                            .addMessage(
                                    Component.literal(
                                                    "[Voice Debug] "
                                            )
                                            .withStyle(
                                                    ChatFormatting.AQUA
                                            )
                                            .append(
                                                    Component.literal(
                                                            expressive
                                                    )
                                                    .withStyle(
                                                            ChatFormatting.WHITE
                                                    )
                                            )
                            );

                    String metrics =
                            String.format(
                                    Locale.ROOT,
                                    "tom=%s | urgencia=%d%% | enfase azul=%d%% | vermelho=%d%% | pitch %.0f->%.0f Hz",
                                    profile.tone(),
                                    Math.round(
                                            urgency
                                                    * 100.0
                                    ),
                                    Math.round(
                                            blueEmphasis.score()
                                                    * 100.0
                                    ),
                                    Math.round(
                                            redEmphasis.score()
                                                    * 100.0
                                    ),
                                    profile.startPitch(),
                                    profile.endPitch()
                            );

                    minecraft.gui
                            .getChat()
                            .addMessage(
                                    Component.literal(
                                                    "  > "
                                                            + metrics
                                            )
                                            .withStyle(
                                                    ChatFormatting.DARK_GRAY
                                            )
                            );
                }
        );
    }

    private static void showClientMessage(
            Component message
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        minecraft.execute(
                () ->
                        minecraft.gui
                                .getChat()
                                .addMessage(
                                        message
                                )
        );
    }
}
