package net.caravidro.wayaround.voice.client;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import net.caravidro.wayaround.media.client.MediaVoiceTap;
import net.caravidro.wayaround.voice.VoiceConstants;

public final class VoicePlayback {

    private VoicePlayback() {
    }

    private static final BlockingQueue<byte[]> QUEUE =
            new ArrayBlockingQueue<>(192);

    private static final AtomicBoolean THREAD_STARTED =
            new AtomicBoolean(false);

    private static volatile Thread playbackThread;

    public static void enqueue(
            byte[] pcm
    ) {
        if (!VoiceConfig.isEnabled()) {
            return;
        }

        if (pcm == null
                || pcm.length == 0
                || pcm.length
                > VoiceConstants.MAX_PACKET_BYTES) {

            return;
        }

        ensureThread();

        byte[] copy =
                Arrays.copyOf(
                        pcm,
                        pcm.length
                );

        if (!QUEUE.offer(copy)) {
            QUEUE.poll();
            QUEUE.offer(copy);
        }
    }

    public static void restartOutput() {
        Thread old =
                playbackThread;

        if (old != null) {
            old.interrupt();
        }

        QUEUE.clear();
    }

    private static void ensureThread() {
        if (!THREAD_STARTED
                .compareAndSet(
                        false,
                        true
                )) {

            return;
        }

        Thread thread =
                new Thread(
                        VoicePlayback::playbackLoop,
                        "WayAround-VoicePlayback"
                );

        playbackThread =
                thread;

        thread.setDaemon(true);
        thread.start();
    }

    private static void playbackLoop() {
        SourceDataLine line = null;

        try {
            line =
                    openBestOutput();

            line.start();

            while (!Thread.currentThread()
                    .isInterrupted()) {

                byte[] pcm =
                        QUEUE.take();

                if (!VoiceConfig.isEnabled()) {
                    continue;
                }

                MediaVoiceTap.captureRemote(
                        pcm
                );

                line.write(
                        pcm,
                        0,
                        pcm.length
                );
            }

        } catch (InterruptedException exception) {
            Thread.currentThread()
                    .interrupt();

        } catch (Exception exception) {
            System.err.println(
                    "[WayAround Voice] Falha na saida de audio: "
                            + exception.getClass()
                            .getSimpleName()
                            + ": "
                            + exception.getMessage()
            );

        } finally {
            playbackThread =
                    null;

            THREAD_STARTED.set(
                    false
            );

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
    }

    private static SourceDataLine openBestOutput()
            throws Exception {

        DataLine.Info wanted =
                new DataLine.Info(
                        SourceDataLine.class,
                        VoiceConstants.audioFormat()
                );

        VoiceDevices.OutputDevice selected =
                VoiceDevices.selectedOutputOrDefault();

        Exception lastFailure = null;

        if (selected.mixerInfo() != null) {
            try {
                return openLine(
                        AudioSystem.getMixer(
                                selected.mixerInfo()
                        ),
                        wanted
                );

            } catch (Exception exception) {
                lastFailure = exception;

                System.err.println(
                        "[WayAround Voice] Saida selecionada falhou; tentando fallback: "
                                + exception.getMessage()
                );
            }
        }

        try {
            SourceDataLine system =
                    (SourceDataLine)
                            AudioSystem.getLine(
                                    wanted
                            );

            system.open(
                    VoiceConstants.audioFormat(),
                    VoiceConstants.FRAME_BYTES * 32
            );

            return system;

        } catch (Exception exception) {
            lastFailure = exception;
        }

        List<VoiceDevices.OutputDevice> outputs =
                VoiceDevices.listOutputs();

        for (VoiceDevices.OutputDevice output
                : outputs) {

            if (output.mixerInfo() == null
                    || (
                    selected.mixerInfo() != null
                            && output.id()
                            .equals(
                                    selected.id()
                            )
            )) {
                continue;
            }

            try {
                return openLine(
                        AudioSystem.getMixer(
                                output.mixerInfo()
                        ),
                        wanted
                );

            } catch (Exception exception) {
                lastFailure = exception;
            }
        }

        throw lastFailure == null
                ? new IllegalStateException(
                        "nenhuma saida de audio compativel"
                )
                : lastFailure;
    }

    private static SourceDataLine openLine(
            Mixer mixer,
            DataLine.Info wanted
    ) throws Exception {

        SourceDataLine line =
                (SourceDataLine)
                        mixer.getLine(
                                wanted
                        );

        line.open(
                VoiceConstants.audioFormat(),
                VoiceConstants.FRAME_BYTES * 32
        );

        return line;
    }
}
