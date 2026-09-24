package net.caravidro.wayaround.client.sound;

import java.util.UUID;

import net.caravidro.wayaround.client.BlueClientEffects;
import net.caravidro.wayaround.sounds.WayAroundSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/**
 * Moving music source attached to a synced Blue.
 *
 * One sound instance follows the Blue from summon through launch. Keeping
 * that same instance avoids pitch/time discontinuities and keeps nearby
 * clients aligned with the owner's playback.
 */
public final class BlueSpatialSound
        extends AbstractTickableSoundInstance {

    private final UUID owner;

    public BlueSpatialSound(
            UUID owner
    ) {
        super(
                WayAroundSounds.BLUE_THEME.get(),
                SoundSource.PLAYERS,
                SoundInstance.createUnseededRandom()
        );

        this.owner =
                owner;

        /*
         * The ability owns the lifetime now. Looping lets the 55 s track
         * continue through the collapse/tail instead of cutting at the OGG
         * boundary; BlueClientEffects stops it when the synchronized effect
         * is truly over.
         */
        this.looping =
                true;

        this.delay =
                0;

        this.attenuation =
                SoundInstance.Attenuation.LINEAR;

        this.relative =
                false;

        this.volume =
                4.0F;

        this.pitch =
                1.0F;
    }

    @Override
    public void tick() {
        BlueClientEffects.SoundSample sample =
                BlueClientEffects.soundSample(
                        owner
                );

        if (sample == null) {
            stop();
            return;
        }

        this.x =
                sample.position().x;

        this.y =
                sample.position().y;

        this.z =
                sample.position().z;

        this.volume =
                sample.volume();

        this.pitch =
                sample.pitch();
    }
}
