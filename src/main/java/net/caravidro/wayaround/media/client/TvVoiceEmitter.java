package net.caravidro.wayaround.media.client;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

import net.caravidro.wayaround.media.TelevisionBlockEntity;
import net.caravidro.wayaround.media.VhsData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class TvVoiceEmitter {

    private TvVoiceEmitter() {
    }

    private static final Map<String, Session>
            SESSIONS =
            new HashMap<>();

    private static long clientTick;

    public static void tick(
            TelevisionBlockEntity television
    ) {
        if (television.getLevel() == null) {
            return;
        }

        clientTick++;

        String key =
                television.getLevel()
                        .dimension()
                        .location()
                        + ":"
                        + television.getBlockPos()
                        .asLong();

        VhsData.Info info =
                VhsData.read(
                        television.tape()
                )
                        .orElse(null);

        if (info == null) {
            stop(
                    key
            );
            return;
        }

        Path path =
                RecordingStore.find(
                        info.recordingId()
                )
                        .orElse(null);

        if (path == null) {
            stop(
                    key
            );
            return;
        }

        Session session =
                SESSIONS.get(
                        key
                );

        if (session == null
                || !session.recordingId
                .equals(
                        info.recordingId()
                )
                || session.playbackStart
                != television
                .playbackStartGameTime()) {

            stop(
                    key
            );

            long elapsedMillis =
                    Math.max(
                            0L,
                            (
                                    television.getLevel()
                                            .getGameTime()
                                            - television
                                            .playbackStartGameTime()
                            )
                                    * 50L
                    );

            session =
                    new Session(
                            info.recordingId(),
                            television
                                    .playbackStartGameTime(),
                            path,
                            television
                                    .getBlockPos(),
                            elapsedMillis
                    );

            SESSIONS.put(
                    key,
                    session
            );

            session.start();
        }

        session.lastSeenTick =
                clientTick;

        session.updateDistance();
    }

    public static void cleanup() {
        Iterator<Map.Entry<String, Session>>
                iterator =
                SESSIONS.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            Session session =
                    iterator.next()
                            .getValue();

            if (clientTick
                    - session.lastSeenTick
                    > 60L
                    || session.finished) {

                session.stop();
                iterator.remove();
            }
        }
    }

    private static void stop(
            String key
    ) {
        Session old =
                SESSIONS.remove(
                        key
                );

        if (old != null) {
            old.stop();
        }
    }

    private static final class Session {

        private final String recordingId;
        private final long playbackStart;
        private final Path path;
        private final BlockPos pos;
        private final long startMillis;

        private volatile boolean stopped;
        private volatile boolean finished;
        private volatile float gainDb =
                -80.0F;

        private volatile long lastSeenTick;

        private Session(
                String recordingId,
                long playbackStart,
                Path path,
                BlockPos pos,
                long startMillis
        ) {
            this.recordingId =
                    recordingId;

            this.playbackStart =
                    playbackStart;

            this.path =
                    path;

            this.pos =
                    pos.immutable();

            this.startMillis =
                    startMillis;
        }

        private void start() {
            Thread thread =
                    new Thread(
                            this::play,
                            "WayAround-TVVoice-"
                                    + pos.asLong()
                    );

            thread.setDaemon(
                    true
            );

            thread.start();
        }

        private void updateDistance() {
            Minecraft minecraft =
                    Minecraft.getInstance();

            if (minecraft.player == null) {
                gainDb = -80.0F;
                return;
            }

            double distance =
                    Math.sqrt(
                            minecraft.player
                                    .distanceToSqr(
                                            pos.getX()
                                                    + 0.5,
                                            pos.getY()
                                                    + 0.5,
                                            pos.getZ()
                                                    + 0.5
                                    )
                    );

            if (distance <= 2.0) {
                gainDb = 0.0F;

            } else if (distance >= 32.0) {
                gainDb = -80.0F;

            } else {
                gainDb =
                        (float) Math.max(
                                -48.0,
                                -(distance - 2.0)
                                        * 1.6
                        );
            }
        }

        private void play() {
            SourceDataLine line = null;

            try (
                    RecordingReader reader =
                            new RecordingReader(
                                    path
                            )
            ) {
                if (reader.audioSamples()
                        <= 0) {

                    finished = true;
                    return;
                }

                AudioFormat format =
                        new AudioFormat(
                                reader.sampleRate(),
                                16,
                                1,
                                true,
                                false
                        );

                DataLine.Info info =
                        new DataLine.Info(
                                SourceDataLine.class,
                                format
                        );

                line =
                        (SourceDataLine)
                                AudioSystem.getLine(
                                        info
                                );

                line.open(
                        format,
                        reader.sampleRate()
                                / 5
                                * 2
                );

                line.start();

                FloatControl gain =
                        line.isControlSupported(
                                FloatControl.Type.MASTER_GAIN
                        )
                                ? (FloatControl)
                                        line.getControl(
                                                FloatControl.Type.MASTER_GAIN
                                        )
                                : null;

                int sample =
                        (int) Math.min(
                                reader.audioSamples(),
                                startMillis
                                        * reader.sampleRate()
                                        / 1000L
                        );

                while (!stopped
                        && sample
                        < reader.audioSamples()) {

                    if (gain != null) {
                        float clamped =
                                Math.max(
                                        gain.getMinimum(),
                                        Math.min(
                                                gain.getMaximum(),
                                                gainDb
                                        )
                                );

                        gain.setValue(
                                clamped
                        );
                    }

                    byte[] pcm =
                            reader.readAudio(
                                    sample,
                                    2048
                            );

                    if (pcm.length == 0) {
                        break;
                    }

                    line.write(
                            pcm,
                            0,
                            pcm.length
                    );

                    sample +=
                            pcm.length / 2;
                }

            } catch (Exception exception) {
                System.err.println(
                        "[WayAround Media] Falha no audio da TV: "
                                + exception.getMessage()
                );

            } finally {
                finished = true;

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

        private void stop() {
            stopped = true;
        }
    }
}
