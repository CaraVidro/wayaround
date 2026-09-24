package net.caravidro.wayaround.media.client;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class RecordedWorldSound
        extends AbstractSoundInstance {

    public RecordedWorldSound(
            ResourceLocation location,
            SoundSource source,
            float volume,
            float pitch,
            double x,
            double y,
            double z
    ) {
        super(
                location,
                source,
                RandomSource.create()
        );

        this.volume =
                Math.max(
                        0.0F,
                        Math.min(
                                4.0F,
                                volume
                        )
                );

        this.pitch =
                Math.max(
                        0.5F,
                        Math.min(
                                2.0F,
                                pitch
                        )
                );

        this.x = x;
        this.y = y;
        this.z = z;

        this.looping = false;
        this.delay = 0;
        this.relative = false;
        this.attenuation =
                SoundInstance.Attenuation.LINEAR;
    }
}
