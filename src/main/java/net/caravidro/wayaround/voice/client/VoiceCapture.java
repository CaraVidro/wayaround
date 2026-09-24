package net.caravidro.wayaround.voice.client;

import java.util.Arrays;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

import net.caravidro.wayaround.network.VoiceFrameC2SPayload;
import net.caravidro.wayaround.voice.VoiceConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class VoiceCapture {

    private VoiceCapture() {
    }

    private static volatile boolean running = false;
    private static volatile TargetDataLine activeLine;

    public static boolean isRunning() {
        return running;
    }

    public static synchronized void start() {
        if (running
                || !VoiceConfig.isEnabled()) {
            return;
        }

        running = true;

        Thread thread =
                new Thread(
                        VoiceCapture::captureLoop,
                        "WayAround-VoiceCapture"
                );

        thread.setDaemon(true);
        thread.start();
    }

    public static synchronized void stop() {
        running = false;

        TargetDataLine line = activeLine;
        activeLine = null;

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

    private static void captureLoop() {
        TargetDataLine line = null;

        try {
            DataLine.Info lineInfo =
                    new DataLine.Info(
                            TargetDataLine.class,
                            VoiceConstants.audioFormat()
                    );

            VoiceDevices.InputDevice device =
                    VoiceDevices.selectedOrDefault();

            if (device.mixerInfo() == null) {
                line =
                        (TargetDataLine)
                                AudioSystem.getLine(
                                        lineInfo
                                );
            } else {
                Mixer mixer =
                        AudioSystem.getMixer(
                                device.mixerInfo()
                        );

                line =
                        (TargetDataLine)
                                mixer.getLine(
                                        lineInfo
                                );
            }

            line.open(
                    VoiceConstants.audioFormat(),
                    VoiceConstants.FRAME_BYTES * 8
            );

            line.start();
            activeLine = line;

            byte[] buffer =
                    new byte[
                            VoiceConstants.FRAME_BYTES
                            ];

            while (running) {
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

                Minecraft minecraft =
                        Minecraft.getInstance();

                minecraft.execute(
                        () -> {
                            if (!running
                                    || !VoiceConfig.isEnabled()
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

        } catch (Exception exception) {
            boolean unexpectedStop = running;
            running = false;

            if (unexpectedStop) {
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

        } finally {
            activeLine = null;

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
}
