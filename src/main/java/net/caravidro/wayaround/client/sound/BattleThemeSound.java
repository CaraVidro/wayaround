package net.caravidro.wayaround.client.sound;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.sounds.WayAroundSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/**
 * Judes battle theme. Spatial instances follow the fighter in real time while
 * participants use a direct client mix.
 */
public final class BattleThemeSound
        extends AbstractTickableSoundInstance {

    private static final Map<UUID, List<BattleThemeSound>> ACTIVE =
            new HashMap<>();

    private final UUID battle;
    private final UUID sourcePlayer;
    private final boolean direct;

    private BattleThemeSound(
            UUID battle,
            UUID sourcePlayer,
            boolean direct
    ) {
        super(
                WayAroundSounds.BATTLE_JUDES.get(),
                SoundSource.PLAYERS,
                RandomSource.create()
        );

        this.battle =
                battle;

        this.sourcePlayer =
                sourcePlayer;

        this.direct =
                direct;

        this.looping =
                false;

        this.delay =
                0;

        this.pitch =
                1.0F;

        this.volume =
                direct
                        ? 0.82F
                        : 1.45F;

        this.relative =
                direct;

        this.attenuation =
                direct
                        ? SoundInstance.Attenuation.NONE
                        : SoundInstance.Attenuation.LINEAR;
    }

    public static void play(
            UUID battle,
            UUID source,
            boolean direct
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || minecraft.player == null) {
            return;
        }

        BattleThemeSound sound =
                new BattleThemeSound(
                        battle,
                        source,
                        direct
                );

        ACTIVE.computeIfAbsent(
                        battle,
                        ignored ->
                                new ArrayList<>()
                )
                .add(
                        sound
                );

        minecraft.getSoundManager()
                .play(
                        sound
                );
    }

    public static void stopBattle(
            UUID battle
    ) {
        List<BattleThemeSound> sounds =
                ACTIVE.remove(
                        battle
                );

        if (sounds == null) {
            return;
        }

        for (BattleThemeSound sound :
                sounds) {
            sound.stop();
        }
    }

    @Override
    public void tick() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || minecraft.player == null) {
            finish();
            return;
        }

        if (direct) {
            x = 0.0;
            y = 0.0;
            z = 0.0;
            return;
        }

        Entity source =
                minecraft.level.getPlayerByUUID(
                        sourcePlayer
                );

        if (source == null
                || !source.isAlive()) {
            finish();
            return;
        }

        x = source.getX();
        y = source.getY()
                + source.getBbHeight()
                        * 0.62;
        z = source.getZ();
    }

    private void finish() {
        List<BattleThemeSound> sounds =
                ACTIVE.get(
                        battle
                );

        if (sounds != null) {
            sounds.remove(
                    this
            );

            if (sounds.isEmpty()) {
                ACTIVE.remove(
                        battle
                );
            }
        }

        stop();
    }
}
