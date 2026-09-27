package net.caravidro.wayaround.media.client;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class MediaAmbientRecorder
        implements SoundEventListener {

    public static final MediaAmbientRecorder INSTANCE =
            new MediaAmbientRecorder();

    private MediaAmbientRecorder() {
    }

    @Override
    public void onPlaySound(
            SoundInstance sound,
            WeighedSoundEvents accessor,
            float range
    ) {
        if (sound
                instanceof RecordedWorldSound) {

            return;
        }

        SoundSource source =
                sound.getSource();

        /*
         * MASTER catches UI-ish sounds and MUSIC is not world ambience.
         * The remaining categories are actual world sounds that the camera
         * could plausibly hear: weather, blocks, mobs, players, ambience,
         * jukeboxes, etc.
         */
        if (source == SoundSource.MASTER
                || source == SoundSource.MUSIC) {

            return;
        }

        float volume =
                sound.getVolume();

        BroadcastAmbientCapture.maybeForward(
                sound,
                source,
                volume,
                sound.getPitch()
        );

        if (!MediaRecorder.isRecording()) {
            return;
        }

        Vec3 cameraPosition =
                MediaRecorder.recordingPosition();

        if (!sound.isRelative()
                && cameraPosition != null) {

            double dx =
                    sound.getX()
                            - cameraPosition.x;

            double dy =
                    sound.getY()
                            - cameraPosition.y;

            double dz =
                    sound.getZ()
                            - cameraPosition.z;

            double distance =
                    Math.sqrt(
                            dx * dx
                                    + dy * dy
                                    + dz * dz
                    );

            if (range > 0.0F) {
                if (distance > range) {
                    return;
                }

                volume *=
                        (float) Math.max(
                                0.0,
                                1.0
                                        - distance
                                        / range
                        );
            }
        }

        if (volume < 0.01F) {
            return;
        }

        MediaRecorder.recordAmbientSound(
                sound.getLocation()
                        .toString(),
                source.name(),
                volume,
                sound.getPitch()
        );
    }
}
