package net.caravidro.wayaround.client.sound;

import net.caravidro.wayaround.client.TukunaPossessionClient;
import net.caravidro.wayaround.sounds.WayAroundSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/** Only the two participants hear the theme. It is deliberately one-shot. */
public final class TukunaContractSound extends AbstractTickableSoundInstance {
    public TukunaContractSound() {
        super(WayAroundSounds.IN_MY_WAY.get(), SoundSource.PLAYERS,
                SoundInstance.createUnseededRandom());
        // Indefinite possession must feel ominous, not like a jukebox stuck.
        looping = false;
        relative = true;
        attenuation = SoundInstance.Attenuation.NONE;
        volume = 0.9F;
        pitch = 1.0F;
    }

    @Override
    public void tick() {
        if (!TukunaPossessionClient.isContractMusicPlaying()) stop();
    }
}
