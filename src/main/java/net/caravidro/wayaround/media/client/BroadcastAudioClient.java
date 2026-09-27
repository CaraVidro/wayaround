package net.caravidro.wayaround.media.client;

import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import net.caravidro.wayaround.media.broadcast.BroadcastEffect;
import net.caravidro.wayaround.voice.VoiceConstants;

public final class BroadcastAudioClient {
    private static final BlockingQueue<Frame> QUEUE = new ArrayBlockingQueue<>(24);
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    private BroadcastAudioClient() {}

    public static void enqueue(byte[] pcm, float quality, float volume, int effectOrdinal) {
        if (pcm == null || pcm.length == 0) return;

        BroadcastEffect[] effects = BroadcastEffect.values();
        BroadcastEffect effect = effects[Math.max(0, Math.min(effects.length - 1, effectOrdinal))];

        byte[] copy = Arrays.copyOf(pcm, pcm.length);
        process(copy, quality, volume, effect);

        if (!QUEUE.offer(new Frame(copy))) {
            QUEUE.poll();
            QUEUE.offer(new Frame(copy));
        }

        ensureThread();
    }

    private static void ensureThread() {
        if (!STARTED.compareAndSet(false, true)) return;

        Thread thread = new Thread(BroadcastAudioClient::loop, "WayAround-BroadcastAudio");
        thread.setDaemon(true);
        thread.start();
    }

    private static void loop() {
        SourceDataLine line = null;

        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, VoiceConstants.audioFormat());
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(VoiceConstants.audioFormat(), VoiceConstants.FRAME_BYTES * 8);
            line.start();

            while (!Thread.currentThread().isInterrupted()) {
                Frame frame = QUEUE.take();
                line.write(frame.pcm, 0, frame.pcm.length);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            System.err.println("[WayAround Media] Broadcast audio failed: " + exception.getMessage());
        } finally {
            STARTED.set(false);
            if (line != null) {
                try { line.stop(); } catch (Exception ignored) {}
                try { line.close(); } catch (Exception ignored) {}
            }
        }
    }

    private static void process(byte[] pcm, float quality, float volume, BroadcastEffect effect) {
        float q = Math.max(0.0F, Math.min(1.0F, quality));
        float gain = Math.max(0.0F, Math.min(1.0F, volume));
        int previous = 0;

        for (int i = 0; i + 1 < pcm.length; i += 2) {
            int sample = (short) ((pcm[i] & 0xFF) | (pcm[i + 1] << 8));

            if (effect == BroadcastEffect.DISTANT) {
                sample = (sample + previous * 3) / 4;
            } else if (effect == BroadcastEffect.VHS) {
                sample = (sample / 512) * 512;
            } else if (effect == BroadcastEffect.GLITCH && ((i / 2) % 97) < 5) {
                sample = 0;
            }

            previous = sample;

            int hash = i * 1103515245 + pcm.length * 12345;
            int noise = (int) (((hash >>> 16) & 0x7FFF) / 32767.0 * 2.0 * 2100 - 2100);
            sample = (int) (sample * q + noise * (1.0F - q));
            sample = (int) (sample * gain);
            sample = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sample));

            pcm[i] = (byte) (sample & 0xFF);
            pcm[i + 1] = (byte) ((sample >>> 8) & 0xFF);
        }
    }

    private record Frame(byte[] pcm) {}
}
