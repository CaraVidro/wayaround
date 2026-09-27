package net.caravidro.wayaround.client.sound;

import net.caravidro.wayaround.client.TukunaPossessionClient;
import net.caravidro.wayaround.sounds.WayAroundSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/**
 * In My Way is a one-shot possession theme.
 *
 * It is slightly quieter for everyone than before. On the receptacle's client
 * only, Tukuna speech temporarily ducks the track so the projected voice stays
 * intelligible without changing what other listeners hear.
 */
public final class TukunaContractSound
        extends AbstractTickableSoundInstance {

    private static final float BASE_VOLUME =
            0.78F;

    private static final float SPEECH_DUCK_VOLUME =
            0.38F;

    private static final float DUCK_SPEED =
            0.28F;

    private static final float RECOVER_SPEED =
            0.10F;

    public TukunaContractSound() {
        super(
                WayAroundSounds.IN_MY_WAY.get(),
                SoundSource.PLAYERS,
                SoundInstance.createUnseededRandom()
        );

        // Indefinite possession must feel ominous, not like a jukebox stuck.
        looping =
                false;

        relative =
                true;

        attenuation =
                SoundInstance.Attenuation.NONE;

        volume =
                BASE_VOLUME;

        pitch =
                1.0F;
    }

    @Override
    public void tick() {
        if (!TukunaPossessionClient.isContractMusicPlaying()) {
            stop();
            return;
        }

        boolean speakingToReceptacle =
                TukunaPossessionClient
                        .shouldDuckContractMusicForSpeech();

        float target =
                speakingToReceptacle
                        ? SPEECH_DUCK_VOLUME
                        : BASE_VOLUME;

        float speed =
                speakingToReceptacle
                        ? DUCK_SPEED
                        : RECOVER_SPEED;

        volume =
                Mth.lerp(
                        speed,
                        volume,
                        target
                );

        if (Math.abs(
                volume - target
        ) < 0.004F) {
            volume =
                    target;
        }
    }
}
