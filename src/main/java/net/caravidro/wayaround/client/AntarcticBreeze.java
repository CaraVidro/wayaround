package net.caravidro.wayaround.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.sounds.WayAroundSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = WayAround.MODID, value = Dist.CLIENT)
public final class AntarcticBreeze {
    private static int waitTicks = 200;
    private static BreezeSound sound;

    private static float polar(Minecraft mc) {
        return AntarcticClientLighting.polarInfluence(
                mc
        );
    }

    private static boolean exposed(Minecraft mc) {
        return mc.level != null
                && mc.player != null
                && polar(mc) > 0.08F
                && mc.level.canSeeSky(mc.player.blockPosition())
                && ClientBlizzardState.getIntensity() < 0.02F
                && ClientBlizzardState.getTargetIntensity() < 0.02F;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) return;
        if (!exposed(mc)) {
            if (sound != null) mc.getSoundManager().stop(sound);
            sound = null;
            waitTicks = 200;
            return;
        }
        if (sound != null && !sound.isStopped() && mc.getSoundManager().isActive(sound)) return;
        if (--waitTicks <= 0) {
            sound = new BreezeSound();
            mc.getSoundManager().play(sound);
            float influence =
                    polar(
                            mc
                    );

            int base =
                    Math.max(
                            180,
                            Math.round(
                                    520
                                            - influence * 260
                            )
                    );

            waitTicks =
                    base
                            + mc.level.random.nextInt(
                            420
                    );
        }
    }

    private static final class BreezeSound extends AbstractTickableSoundInstance {
        private int age;
        BreezeSound() {
            super(WayAroundSounds.BLIZZARD_WIND.get(), SoundSource.WEATHER, RandomSource.create());
            relative = true;
            attenuation = Attenuation.NONE;
            looping = true;
            volume = 0.0F;
            pitch = 0.95F;
        }
        @Override
        public boolean canStartSilent() { return true; }
        @Override
        public void tick() {
            if (!exposed(Minecraft.getInstance()) || ++age >= 160) {
                stop();
                return;
            }
            float influence =
                    polar(
                            Minecraft.getInstance()
                    );

            volume =
                    (
                            0.05F
                                    + influence * 0.15F
                    )
                            * (float) Math.sin(
                            Math.PI * age / 160.0
                    );
        }
    }
}
