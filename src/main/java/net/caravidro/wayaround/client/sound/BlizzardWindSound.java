package net.caravidro.wayaround.client.sound;

import net.caravidro.wayaround.client.BlizzardClientEvents;
import net.caravidro.wayaround.client.ClientBlizzardState;
import net.caravidro.wayaround.sounds.WayAroundSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class BlizzardWindSound
        extends AbstractTickableSoundInstance {

    /*
     * Quantos ticks o som pode ficar
     * sem tempestade antes de morrer.
     *
     * 20 ticks = 1 segundo
     * 100 ticks = 5 segundos
     */
    private static final int STOP_DELAY =
            100;

    /*
     * Suavidade do volume.
     *
     * Maior = responde mais rápido.
     */
    private static final float VOLUME_SMOOTH =
            0.045F;

    private int silentTicks =
            0;

    public BlizzardWindSound() {

        super(
                WayAroundSounds
                        .BLIZZARD_WIND
                        .get(),

                SoundSource.WEATHER,

                RandomSource.create()
        );

        /*
         * MUITO importante:
         *
         * quando o arquivo termina,
         * Minecraft recomeça.
         */
        this.looping = true;

        this.delay = 0;

        /*
         * Som ambiente.
         *
         * Não vem de uma coordenada
         * específica.
         */
        this.relative = true;

        this.attenuation =
                SoundInstance.Attenuation.NONE;

        /*
         * Começa silencioso e cresce.
         */
        this.volume =
                0.0F;

        this.pitch =
                0.90F;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {

        Minecraft minecraft =
                Minecraft.getInstance();

        /*
         * Saiu do mundo.
         */
        if (
                minecraft.player == null
                ||
                minecraft.level == null
        ) {

            stop();
            return;
        }

        float rawIntensity =
                ClientBlizzardState
                        .getIntensity();

        float targetIntensity =
                ClientBlizzardState
                        .getTargetIntensity();

        float exposedIntensity =
                BlizzardClientEvents
                        .getExposedIntensity();

        /*
         * -------------------------------------------------
         * TOLERÂNCIA
         * -------------------------------------------------
         *
         * Não mata o áudio imediatamente.
         *
         * Isso evita:
         *
         * Blizzard = 0.7
         * pacote estranho = 0
         * SOM MORRE
         *
         * Agora precisamos ficar vários
         * segundos realmente sem tempestade.
         */
        boolean completelySilent =
                rawIntensity <= 0.001F
                &&
                targetIntensity <= 0.001F;

        if (
                completelySilent
        ) {

            silentTicks++;

        } else {

            silentTicks = 0;
        }

        /*
         * Só encerra depois de
         * 5 segundos sem Blizzard.
         */
        if (
                silentTicks >= STOP_DELAY
        ) {

            stop();
            return;
        }

        /*
         * -------------------------------------------------
         * VOLUME
         * -------------------------------------------------
         */

        float targetVolume;

        if (
                minecraft.level.canSeeSky(minecraft.player.blockPosition().above())
        ) {

            /*
             * Ao ar livre.
             *
             * Borda = vento moderado.
             * Centro = vento brutal.
             */
            targetVolume =
                    0.10F
                    +
                    exposedIntensity
                    *
                    0.90F;

        } else {

            /*
             * Dentro de casa/caverna.
             *
             * Ainda dá para ouvir o vento
             * abafado do lado de fora.
             */
            targetVolume =
                    rawIntensity
                    *
                    0.08F;
        }

        /*
         * Se estamos no período de tolerância,
         * fazemos fade-out em vez de cortar.
         */
        if (
                completelySilent
        ) {

            float fade =
                    1.0F
                    -
                    (
                            silentTicks
                            /
                            (float) STOP_DELAY
                    );

            targetVolume *=
                    Mth.clamp(
                            fade,
                            0.0F,
                            1.0F
                    );
        }

        targetVolume =
                Mth.clamp(
                        targetVolume,
                        0.0F,
                        1.0F
                );

        /*
         * Transição suave.
         */
        this.volume +=
                (
                        targetVolume
                        -
                        this.volume
                )
                *
                VOLUME_SMOOTH;

        /*
         * -------------------------------------------------
         * PITCH
         * -------------------------------------------------
         *
         * Centro da tempestade:
         * vento um pouco mais agressivo.
         */

        float targetPitch =
                0.86F
                +
                rawIntensity
                *
                0.10F;

        this.pitch +=
                (
                        targetPitch
                        -
                        this.pitch
                )
                *
                0.025F;
    }
}
