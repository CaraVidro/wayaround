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
 * The OGG is streamed and loop-capable so a launched Blue can keep carrying
 * the theme even if its lifetime extends past the original file duration.
 * BlueClientEffects decides when the source is finally allowed to stop.
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
         * Do not loop the 55 s track during an ordinary controlled Blue.
         * A slingshot launch explicitly starts a fresh moving instance so the
         * projectile always carries music for its whole flight.
         */
        this.looping =
                false;

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
