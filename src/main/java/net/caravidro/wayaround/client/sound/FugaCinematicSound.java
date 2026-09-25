package net.caravidro.wayaround.client.sound;

import java.util.UUID;

import net.caravidro.wayaround.client.cinematic.PlayerAnimationController;
import net.caravidro.wayaround.network.PlayerCinematicPayload;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Sustained release rumble. The vanilla beacon ambience is intentionally
 * looped only for the cinematic lifetime, giving Fuga a continuous low roar
 * without requiring a new external audio asset.
 */
public final class FugaCinematicSound
        extends AbstractTickableSoundInstance {

    private final UUID owner;

    public FugaCinematicSound(
            UUID owner
    ) {
        super(
                SoundEvents.BEACON_AMBIENT,
                SoundSource.PLAYERS,
                SoundInstance.createUnseededRandom()
        );

        this.owner =
                owner;

        this.looping =
                true;

        this.delay =
                0;

        this.relative =
                true;

        this.attenuation =
                SoundInstance.Attenuation.NONE;

        this.volume =
                1.15F;

        this.pitch =
                0.58F;
    }

    @Override
    public void tick() {
        if (!PlayerAnimationController
                .isAnimation(
                        owner,
                        PlayerCinematicPayload.FUGA_RELEASE
                )) {
            stop();
        }
    }
}
