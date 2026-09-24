package net.caravidro.wayaround.voice.client;

import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import net.caravidro.wayaround.voice.VoiceConstants;

public final class VoicePlayback {

    private VoicePlayback() {
    }

    private static final BlockingQueue<byte[]> QUEUE =
            new ArrayBlockingQueue<>(128);

    private static final AtomicBoolean THREAD_STARTED =
            new AtomicBoolean(false);

    public static void enqueue(byte[] pcm) {
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

        thread.setDaemon(true);
        thread.start();
    }

    private static void playbackLoop() {
        SourceDataLine line = null;

        try {
            DataLine.Info info =
                    new DataLine.Info(
                            SourceDataLine.class,
                            VoiceConstants.audioFormat()
                    );

            line =
                    (SourceDataLine)
                            AudioSystem.getLine(info);

            line.open(
                    VoiceConstants.audioFormat(),
                    VoiceConstants.FRAME_BYTES * 16
            );

            line.start();

            while (true) {
                byte[] pcm = QUEUE.take();

                if (!VoiceConfig.isEnabled()) {
                    continue;
                }

                line.write(
                        pcm,
                        0,
                        pcm.length
                );
            }

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

        } catch (Exception exception) {
            System.err.println(
                    "[WayAround Voice] Falha na saida de audio: "
                            + exception.getClass()
                            .getSimpleName()
                            + ": "
                            + exception.getMessage()
            );

        } finally {
            THREAD_STARTED.set(false);

            if (line != null) {
                try {
                    line.drain();
                } catch (Exception ignored) {
                }

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
}
