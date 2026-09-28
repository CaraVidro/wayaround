package net.caravidro.wayaround.voice.client;

import java.io.ByteArrayOutputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.media.client.MediaVoiceTap;
import net.caravidro.wayaround.network.VoiceFrameC2SPayload;
import net.caravidro.wayaround.voice.VoiceConstants;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class VoiceCapture {

    private VoiceCapture() {
    }

    private static final int PRE_ROLL_FRAMES =
            5;

    private static final int SILENCE_FRAMES_TO_CLOSE =
            14;

    private static final double ABSOLUTE_START_THRESHOLD =
            0.0115;

    private static final double MAX_DYNAMIC_THRESHOLD =
            0.080;

    private static final int COMBAT_SPECULATIVE_MIN_FRAMES =
            8;

    private static final int COMBAT_SPECULATIVE_STEP_FRAMES =
            6;

    private static final double COMBAT_SPECULATIVE_MAX_SECONDS =
            2.2;

    /*
     * Tobias now feeds Vosk every capture frame (40 ms) for Void/Tukuna.
     * There are intentionally very few speech-enabled Spectrums, so latency is
     * more valuable here than batching four frames into a 160 ms chunk.
     */
    private static final int REALTIME_BATCH_FRAMES =
            1;

    /*
     * STT is ancillary to voice transport. Never let a stuck PTT key, noisy
     * voice-activation session or broken microphone grow a ByteArrayOutputStream
     * for minutes. Keep only the newest 14 seconds for recognition.
     */
    private static final int MAX_TRANSCRIPTION_BYTES =
            (int) (
                    VoiceConstants.SAMPLE_RATE
                            * VoiceConstants.BYTES_PER_SAMPLE
                            * 14.0
            );

    private static volatile long captureGeneration;
    private static volatile boolean running;
    private static volatile boolean transmitting;
    private static volatile boolean voiceActivationSession;

    private static volatile TargetDataLine activeLine;

    public static boolean isRunning() {
        return running;
    }

    public static boolean isTransmitting() {
        return transmitting;
    }

    public static boolean isVoiceActivationSession() {
        return running
                && voiceActivationSession;
    }

    public static synchronized void startPushToTalk() {
        start(
                false
        );
    }

    public static synchronized void startVoiceActivation() {
        start(
                true
        );
    }

    private static void start(
            boolean voiceActivation
    ) {
        if (!VoiceConfig.isEnabled()
                || !WorldFeatureRuntime.clientEnabled(
                WorldFeature.VOICE_CHAT
        )) {
            return;
        }

        if (running) {
            if (voiceActivationSession
                    == voiceActivation) {

                return;
            }

            stop();
            return;
        }

        long generation = ++captureGeneration;
        running = true;
        transmitting = false;
        voiceActivationSession =
                voiceActivation;

        WayAround.LOGGER.info(
                "[Voice/Capture] START mode={} mic={}",
                voiceActivation
                        ? "VOICE_ACTIVATION"
                        : "PUSH_TO_TALK",
                VoiceDevices.selectedOrDefault()
                        .displayName()
        );

        Thread thread =
                new Thread(
                        () -> captureLoop(
                                voiceActivation, generation
                        ),
                        voiceActivation
                                ? "WayAround-VoiceActivation"
                                : "WayAround-VoiceCapture"
                );

        thread.setDaemon(
                true
        );

        thread.start();
    }

    public static synchronized void stop() {
        if (running) {
            WayAround.LOGGER.info(
                    "[Voice/Capture] STOP requested mode={} transmitting={}",
                    voiceActivationSession
                            ? "VOICE_ACTIVATION"
                            : "PUSH_TO_TALK",
                    transmitting
            );
        }

        running = false;
        transmitting = false;

        TargetDataLine line =
                activeLine;

        activeLine =
                null;

        if (line != null) {
            try {
                line.stop();
            } catch (Exception ignored) {
            }

            try {
                line.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static void captureLoop(
            boolean voiceActivation, long generation
    ) {
        TargetDataLine line = null;

        ByteArrayOutputStream utterance =
                null;

        Deque<byte[]> preRoll =
                new ArrayDeque<>(
                        PRE_ROLL_FRAMES
                );

        double noiseFloor =
                0.0045;

        int silenceFrames =
                0;

        int lastSpeculativeBytes =
                0;

        ByteArrayOutputStream realtimeBatch =
                new ByteArrayOutputStream(
                        VoiceConstants.FRAME_BYTES
                                * REALTIME_BATCH_FRAMES
                );

        int realtimeFrames =
                0;

        boolean realtimeStarted =
                false;

        try {
            line =
                    openInputLine();

            line.open(
                    VoiceConstants.audioFormat(),
                    VoiceConstants.FRAME_BYTES
                            * 4
            );

            line.start();

            synchronized (VoiceCapture.class) {
                if (!running || generation != captureGeneration) return;
                activeLine = line;
            }

            byte[] buffer =
                    new byte[
                            VoiceConstants.FRAME_BYTES
                            ];

            if (!voiceActivation) {
                transmitting =
                        true;

                utterance =
                        new ByteArrayOutputStream();

                if (VoiceIntentClient.wantsContinuousRecognition()) {
                    VoiceSpeechDebug.beginRealtime();
                    realtimeStarted =
                            true;
                }
            }

            while (running && generation == captureGeneration
                    && voiceActivationSession
                    == voiceActivation) {

                int read =
                        line.read(
                                buffer,
                                0,
                                buffer.length
                        );

                if (!running || generation != captureGeneration) break;
                if (read <= 0) {
                    continue;
                }

                byte[] frame =
                        Arrays.copyOf(
                                buffer,
                                read
                        );

                if (!voiceActivation) {
                    publishFrame(
                            frame, generation
                    );

                    if (realtimeStarted) {
                        realtimeBatch.write(
                                frame,
                                0,
                                frame.length
                        );

                        realtimeFrames++;

                        if (realtimeFrames
                                >= REALTIME_BATCH_FRAMES) {
                            VoiceSpeechDebug.feedRealtime(
                                    realtimeBatch.toByteArray()
                            );

                            realtimeBatch.reset();
                            realtimeFrames =
                                    0;
                        }
                    }

                    if (shouldTranscribe()) {
                        utterance.write(
                                frame,
                                0,
                                frame.length
                        );

                        lastSpeculativeBytes =
                                maybeSubmitCombatSpeculative(
                                        utterance,
                                        lastSpeculativeBytes
                                );
                    }

                    continue;
                }

                double rms =
                        rms(
                                frame
                        );

                double threshold =
                        Math.max(
                                ABSOLUTE_START_THRESHOLD,
                                Math.min(
                                        MAX_DYNAMIC_THRESHOLD,
                                        noiseFloor
                                                * 2.85
                                                + 0.004
                                )
                        );

                if (!transmitting) {
                    rememberPreRoll(
                            preRoll,
                            frame
                    );

                    if (rms
                            < threshold * 0.86) {

                        noiseFloor =
                                noiseFloor
                                        * 0.965
                                        + rms
                                                * 0.035;
                    }

                    if (rms < threshold) {
                        continue;
                    }

                    transmitting =
                            true;

                    WayAround.LOGGER.info(
                            "[Voice/Capture] VOICE START rms={} threshold={} preRollFrames={}",
                            String.format(java.util.Locale.ROOT, "%.4f", rms),
                            String.format(java.util.Locale.ROOT, "%.4f", threshold),
                            preRoll.size()
                    );

                    silenceFrames =
                            0;

                    lastSpeculativeBytes =
                            0;

                    utterance =
                            shouldTranscribe()
                                    ? new ByteArrayOutputStream()
                                    : null;

                    if (VoiceIntentClient.wantsContinuousRecognition()) {
                        VoiceSpeechDebug.beginRealtime();
                        realtimeStarted =
                                true;

                        realtimeBatch.reset();
                        realtimeFrames =
                                0;
                    }

                    for (byte[] previous :
                            preRoll) {

                        publishFrame(
                                previous, generation
                        );

                        if (utterance != null) {
                            utterance.write(
                                    previous,
                                    0,
                                    previous.length
                            );
                        }

                        if (realtimeStarted) {
                            realtimeBatch.write(
                                    previous,
                                    0,
                                    previous.length
                            );

                            realtimeFrames++;
                        }
                    }

                    preRoll.clear();
                    if (realtimeStarted && realtimeBatch.size() > 0) {
                        VoiceSpeechDebug.feedRealtime(realtimeBatch.toByteArray());
                        realtimeBatch.reset();
                        realtimeFrames = 0;
                    }

                    /*
                     * The current frame is already inside preRoll, so do not
                     * publish it twice.
                     */
                    continue;
                }

                publishFrame(
                        frame, generation
                );

                if (realtimeStarted) {
                    realtimeBatch.write(
                            frame,
                            0,
                            frame.length
                    );

                    realtimeFrames++;

                    if (realtimeFrames
                            >= REALTIME_BATCH_FRAMES) {
                        VoiceSpeechDebug.feedRealtime(
                                realtimeBatch.toByteArray()
                        );

                        realtimeBatch.reset();
                        realtimeFrames =
                                0;
                    }
                }

                if (utterance != null) {
                    utterance.write(
                            frame,
                            0,
                            frame.length
                    );

                    lastSpeculativeBytes =
                            maybeSubmitCombatSpeculative(
                                    utterance,
                                    lastSpeculativeBytes
                            );
                }

                if (rms
                        < threshold * 0.62) {

                    silenceFrames++;

                } else {
                    silenceFrames =
                            0;
                }

                if (silenceFrames
                        >= SILENCE_FRAMES_TO_CLOSE) {

                    transmitting =
                            false;

                    WayAround.LOGGER.info(
                            "[Voice/Capture] VOICE END after {} silent frames; bytes={}",
                            SILENCE_FRAMES_TO_CLOSE,
                            utterance == null
                                    ? 0
                                    : utterance.size()
                    );

                    if (realtimeStarted) {
                        if (realtimeBatch.size() > 0) {
                            VoiceSpeechDebug.feedRealtime(
                                    realtimeBatch.toByteArray()
                            );
                        }

                        VoiceSpeechDebug.endRealtime();

                        realtimeStarted =
                                false;

                        realtimeBatch.reset();
                        realtimeFrames =
                                0;
                    }

                    submitUtterance(
                            utterance
                    );

                    utterance =
                            null;

                    lastSpeculativeBytes =
                            0;

                    silenceFrames =
                            0;

                    preRoll.clear();
                }
            }

        } catch (Exception exception) {
            boolean unexpectedStop = running && generation == captureGeneration;

            WayAround.LOGGER.warn(
                    "[Voice/Capture] capture loop exception running={} mode={} -> {}: {}",
                    running,
                    voiceActivation
                            ? "VOICE_ACTIVATION"
                            : "PUSH_TO_TALK",
                    exception.getClass()
                            .getSimpleName(),
                    exception.getMessage()
            );

            synchronized (VoiceCapture.class) {
                if (generation == captureGeneration) {
                    running = false;
                    transmitting = false;
                }
            }

            if (unexpectedStop) {
                showMicrophoneError(
                        exception
                );
            }

        } finally {
            if (realtimeStarted) {
                if (realtimeBatch.size() > 0) {
                    VoiceSpeechDebug.feedRealtime(
                            realtimeBatch.toByteArray()
                    );
                }

                VoiceSpeechDebug.endRealtime();
            }

            synchronized (VoiceCapture.class) {
                if (generation == captureGeneration) activeLine = null;
            }

            if (line != null) {
                try {
                    line.stop();
                } catch (Exception ignored) {
                }

                try {
                    line.close();
                } catch (Exception ignored) {
                }
            }

            if (generation == captureGeneration && (!voiceActivation
                    || transmitting)) {

                submitUtterance(
                        utterance
                );
            }

            synchronized (VoiceCapture.class) {
                if (generation == captureGeneration) {
                    transmitting = false;
                    running = false;
                }
            }

            WayAround.LOGGER.info(
                    "[Voice/Capture] LOOP ENDED mode={}",
                    voiceActivation
                            ? "VOICE_ACTIVATION"
                            : "PUSH_TO_TALK"
            );
        }
    }

    private static TargetDataLine openInputLine()
            throws Exception {

        DataLine.Info lineInfo =
                new DataLine.Info(
                        TargetDataLine.class,
                        VoiceConstants.audioFormat()
                );

        VoiceDevices.InputDevice device =
                VoiceDevices.selectedOrDefault();

        if (device.mixerInfo() == null) {
            return (TargetDataLine)
                    AudioSystem.getLine(
                            lineInfo
                    );
        }

        Mixer mixer =
                AudioSystem.getMixer(
                        device.mixerInfo()
                );

        return (TargetDataLine)
                mixer.getLine(
                        lineInfo
                );
    }

    private static boolean shouldTranscribe() {
        return VoiceConfig.isDebugSpeechEnabled()
                || VoiceIntentClient.shouldRecognizeLocalPlayer();
    }

    private static void rememberPreRoll(
            Deque<byte[]> preRoll,
            byte[] frame
    ) {
        if (preRoll.size()
                >= PRE_ROLL_FRAMES) {

            preRoll.removeFirst();
        }

        preRoll.addLast(
                frame
        );
    }

    private static void publishFrame(
            byte[] frame, long generation
    ) {
        MediaVoiceTap.captureLocal(
                frame
        );

        Minecraft minecraft =
                Minecraft.getInstance();

        minecraft.execute(
                () -> {
                    if (generation != captureGeneration
                            || !VoiceConfig.isEnabled()
                            || !WorldFeatureRuntime.clientEnabled(
                            WorldFeature.VOICE_CHAT
                    )
                            || minecraft.player == null
                            || minecraft.getConnection()
                            == null) {

                        return;
                    }

                    PacketDistributor.sendToServer(
                            new VoiceFrameC2SPayload(
                                    frame
                            )
                    );
                }
        );
    }

    private static int maybeSubmitCombatSpeculative(
            ByteArrayOutputStream utterance,
            int lastSubmittedBytes
    ) {
        if (utterance == null
                || VoiceSpeechDebug.isRealtimeActive()
                || !VoiceIntentClient.wantsSpeculativeRecognition()) {
            return lastSubmittedBytes;
        }

        int size =
                utterance.size();

        int minimum =
                VoiceConstants.FRAME_BYTES
                        * COMBAT_SPECULATIVE_MIN_FRAMES;

        int step =
                VoiceConstants.FRAME_BYTES
                        * COMBAT_SPECULATIVE_STEP_FRAMES;

        if (size < minimum
                || size - lastSubmittedBytes
                        < step) {
            return lastSubmittedBytes;
        }

        byte[] audio =
                utterance.toByteArray();

        int maxBytes =
                (int) (
                        VoiceConstants.SAMPLE_RATE
                                * VoiceConstants.BYTES_PER_SAMPLE
                                * COMBAT_SPECULATIVE_MAX_SECONDS
                );

        if (audio.length > maxBytes) {
            audio =
                    Arrays.copyOfRange(
                            audio,
                            audio.length
                                    - maxBytes,
                            audio.length
                    );
        }

        VoiceSpeechDebug.submitSpeculative(
                audio
        );

        return size;
    }

    private static void submitUtterance(
            ByteArrayOutputStream utterance
    ) {
        if (utterance == null
                || utterance.size()
                < VoiceConstants.FRAME_BYTES
                        * 2
                || !shouldTranscribe()) {

            return;
        }

        byte[] audio =
                utterance.toByteArray();

        WayAround.LOGGER.info(
                "[Voice/Capture] SUBMIT STT bytes={} duration~{}ms",
                audio.length,
                Math.round(
                        audio.length
                                / (VoiceConstants.SAMPLE_RATE * 2.0)
                                * 1000.0
                )
        );

        VoiceSpeechDebug.submit(
                audio
        );
    }

    private static double rms(
            byte[] pcm
    ) {
        if (pcm == null
                || pcm.length < 2) {

            return 0.0;
        }

        long squareSum =
                0L;

        int samples =
                pcm.length / 2;

        for (int index = 0;
             index < samples;
             index++) {

            int byteIndex =
                    index * 2;

            int sample =
                    (short) (
                            (pcm[byteIndex]
                                    & 0xFF)
                                    | (pcm[byteIndex + 1]
                                    << 8)
                    );

            squareSum +=
                    (long) sample
                            * sample;
        }

        double meanSquare =
                squareSum
                        / (double) Math.max(
                                1,
                                samples
                        );

        return Math.sqrt(
                meanSquare
        )
                / 32768.0;
    }

    private static final class RollingUtterance
            extends ByteArrayOutputStream {

        private RollingUtterance() {
            super(
                    Math.min(
                            MAX_TRANSCRIPTION_BYTES,
                            VoiceConstants.FRAME_BYTES * 16
                    )
            );
        }

        @Override
        public synchronized void write(
                byte[] source,
                int offset,
                int length
        ) {
            if (source == null
                    || length <= 0) {
                return;
            }

            if (length >= MAX_TRANSCRIPTION_BYTES) {
                reset();

                super.write(
                        source,
                        offset
                                + length
                                - MAX_TRANSCRIPTION_BYTES,
                        MAX_TRANSCRIPTION_BYTES
                );

                return;
            }

            int overflow =
                    count
                            + length
                            - MAX_TRANSCRIPTION_BYTES;

            if (overflow > 0) {
                int remaining =
                        count
                                - overflow;

                if (remaining > 0) {
                    System.arraycopy(
                            buf,
                            overflow,
                            buf,
                            0,
                            remaining
                    );
                }

                count =
                        Math.max(
                                0,
                                remaining
                        );
            }

            super.write(
                    source,
                    offset,
                    length
            );
        }
    }

    private static void showMicrophoneError(
            Exception exception
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        minecraft.execute(
                () -> {
                    if (minecraft.player
                            != null) {

                        minecraft.player
                                .displayClientMessage(
                                        Component.literal(
                                                "Way Around Voice: nao consegui abrir o microfone. "
                                                        + exception.getClass()
                                                        .getSimpleName()
                                                        + ": "
                                                        + exception.getMessage()
                                        ),
                                        false
                                );
                    }
                }
        );

        System.err.println(
                "[WayAround Voice] Falha no microfone: "
                        + exception.getClass()
                        .getSimpleName()
                        + ": "
                        + exception.getMessage()
        );
    }
}

