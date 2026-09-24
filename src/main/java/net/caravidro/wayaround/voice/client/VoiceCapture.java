package net.caravidro.wayaround.voice.client;

import java.io.ByteArrayOutputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

import net.caravidro.wayaround.media.client.MediaVoiceTap;
import net.caravidro.wayaround.network.VoiceFrameC2SPayload;
import net.caravidro.wayaround.voice.VoiceConstants;
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
        if (!VoiceConfig.isEnabled()) {
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

        running = true;
        transmitting = false;
        voiceActivationSession =
                voiceActivation;

        Thread thread =
                new Thread(
                        () -> captureLoop(
                                voiceActivation
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
            boolean voiceActivation
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

        try {
            line =
                    openInputLine();

            line.open(
                    VoiceConstants.audioFormat(),
                    VoiceConstants.FRAME_BYTES
                            * 10
            );

            line.start();

            activeLine =
                    line;

            byte[] buffer =
                    new byte[
                            VoiceConstants.FRAME_BYTES
                            ];

            if (!voiceActivation) {
                transmitting =
                        true;

                utterance =
                        new ByteArrayOutputStream();
            }

            while (running
                    && voiceActivationSession
                    == voiceActivation) {

                int read =
                        line.read(
                                buffer,
                                0,
                                buffer.length
                        );

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
                            frame
                    );

                    if (shouldTranscribe()) {
                        utterance.write(
                                frame,
                                0,
                                frame.length
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

                    silenceFrames =
                            0;

                    utterance =
                            shouldTranscribe()
                                    ? new ByteArrayOutputStream()
                                    : null;

                    for (byte[] previous :
                            preRoll) {

                        publishFrame(
                                previous
                        );

                        if (utterance != null) {
                            utterance.write(
                                    previous,
                                    0,
                                    previous.length
                            );
                        }
                    }

                    preRoll.clear();

                    /*
                     * The current frame is already inside preRoll, so do not
                     * publish it twice.
                     */
                    continue;
                }

                publishFrame(
                        frame
                );

                if (utterance != null) {
                    utterance.write(
                            frame,
                            0,
                            frame.length
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

                    submitUtterance(
                            utterance
                    );

                    utterance =
                            null;

                    silenceFrames =
                            0;

                    preRoll.clear();
                }
            }

        } catch (Exception exception) {
            boolean unexpectedStop =
                    running;

            running =
                    false;

            transmitting =
                    false;

            if (unexpectedStop) {
                showMicrophoneError(
                        exception
                );
            }

        } finally {
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

            if (!voiceActivation
                    || transmitting) {

                submitUtterance(
                        utterance
                );
            }

            transmitting =
                    false;
            running =
                    false;
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
                || VoiceIntentClient.isEnabled();
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
            byte[] frame
    ) {
        MediaVoiceTap.captureLocal(
                frame
        );

        Minecraft minecraft =
                Minecraft.getInstance();

        minecraft.execute(
                () -> {
                    if (!VoiceConfig.isEnabled()
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

        VoiceSpeechDebug.submit(
                utterance.toByteArray()
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
