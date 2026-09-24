package net.caravidro.wayaround.media.client;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

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

        if (info == null
                || !television.isPlaying()) {

            stop(key);
            return;
        }

        Path path =
                RecordingStore.find(
                        info.recordingId()
                )
                        .orElse(null);

        if (path == null) {
            stop(key);
            return;
        }

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

        Session session =
                SESSIONS.get(key);

        if (session == null
                || !session.recordingId
                        .equals(
                                info.recordingId()
                        )
                || session.playbackStart
                != television.playbackStartGameTime()) {

            stop(key);

            try (
                    RecordingReader reader =
                            new RecordingReader(path)
            ) {
                session =
                        new Session(
                                info.recordingId(),
                                television
                                        .playbackStartGameTime(),
                                path,
                                television
                                        .getBlockPos(),
                                elapsedMillis,
                                reader.ambientSounds()
                        );

            } catch (Exception exception) {
                System.err.println(
                        "[WayAround Media] Falha lendo audio/eventos da VHS: "
                                + exception.getMessage()
                );

                return;
            }

            SESSIONS.put(
                    key,
                    session
            );

            session.startVoice();
        }

        session.lastSeenTick =
                clientTick;

        session.updateDistance();
        session.playAmbientDue(
                elapsedMillis
        );
    }

    public static void advanceTick() {
        clientTick++;
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
                    > 60L) {

                session.stop();
                iterator.remove();
            }
        }
    }

    private static void stop(
            String key
    ) {
        Session old =
                SESSIONS.remove(key);

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
        private final List<RecordedAmbientSound>
                ambientSounds;

        private volatile boolean stopped;
        private volatile float gainDb =
                -80.0F;

        private volatile long lastSeenTick;

        private int nextAmbientIndex;

        private Session(
                String recordingId,
                long playbackStart,
                Path path,
                BlockPos pos,
                long startMillis,
                List<RecordedAmbientSound> ambientSounds
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

            this.ambientSounds =
                    ambientSounds;

            while (nextAmbientIndex
                    < ambientSounds.size()
                    && ambientSounds
                    .get(nextAmbientIndex)
                    .timeMillis()
                    < startMillis) {

                nextAmbientIndex++;
            }
        }

        private void startVoice() {
            Thread thread =
                    new Thread(
                            this::playVoice,
                            "WayAround-TVVoice-"
                                    + pos.asLong()
                    );

            thread.setDaemon(true);
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

        private void playAmbientDue(
                long elapsedMillis
        ) {
            Minecraft minecraft =
                    Minecraft.getInstance();

            while (nextAmbientIndex
                    < ambientSounds.size()) {

                RecordedAmbientSound sound =
                        ambientSounds.get(
                                nextAmbientIndex
                        );

                if (sound.timeMillis()
                        > elapsedMillis) {

                    break;
                }

                nextAmbientIndex++;

                ResourceLocation location =
                        ResourceLocation.tryParse(
                                sound.soundId()
                        );

                if (location == null) {
                    continue;
                }

                SoundSource source;

                try {
                    source =
                            SoundSource.valueOf(
                                    sound.source()
                            );
                } catch (Exception exception) {
                    source =
                            SoundSource.AMBIENT;
                }

                minecraft.getSoundManager()
                        .play(
                                new RecordedWorldSound(
                                        location,
                                        source,
                                        sound.volume(),
                                        sound.pitch(),
                                        pos.getX()
                                                + 0.5,
                                        pos.getY()
                                                + 0.55,
                                        pos.getZ()
                                                + 0.5
                                )
                        );
            }
        }

        private void playVoice() {
            SourceDataLine line = null;

            try (
                    RecordingReader reader =
                            new RecordingReader(path)
            ) {
                byte[] audio =
                        reader.readAllAudio();

                if (audio.length == 0) {
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
                                AudioSystem.getLine(info);

                /*
                 * A full second of output buffering gives the JVM plenty of
                 * room to absorb scheduler hiccups. The previous 200 ms buffer
                 * plus tiny reads could underrun and sound chopped on the TV.
                 */
                line.open(
                        format,
                        reader.sampleRate()
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

                int offset =
                        (int) Math.min(
                                audio.length,
                                startMillis
                                        * reader.sampleRate()
                                        / 1000L
                                        * 2L
                        );

                final int chunkBytes =
                        16_384;

                while (!stopped
                        && offset
                        < audio.length) {

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

                    int length =
                            Math.min(
                                    chunkBytes,
                                    audio.length
                                            - offset
                            );

                    int written =
                            line.write(
                                    audio,
                                    offset,
                                    length
                            );

                    if (written <= 0) {
                        break;
                    }

                    offset +=
                            written;
                }

                if (!stopped) {
                    line.drain();
                }

            } catch (Exception exception) {
                System.err.println(
                        "[WayAround Media] Falha no audio da TV: "
                                + exception.getClass()
                                .getSimpleName()
                                + ": "
                                + exception.getMessage()
                );

            } finally {
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
