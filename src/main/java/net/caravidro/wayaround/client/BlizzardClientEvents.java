package net.caravidro.wayaround.client;

import com.mojang.blaze3d.shaders.FogShape;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.client.sound.BlizzardWindSound;
import net.minecraft.client.Minecraft;

import net.minecraft.util.Mth;

import net.minecraft.world.level.material.FogType;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;

import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class BlizzardClientEvents {

    private static BlizzardWindSound windSound;
    private static int soundRetryTicks;

    private BlizzardClientEvents() {
    }

    /*
     * =========================================================
     * CLIENT TICK
     * =========================================================
     */

    @SubscribeEvent
    public static void onClientTick(
            ClientTickEvent.Post event
    ) {

        Minecraft minecraft =
                Minecraft.getInstance();

        if (
                minecraft.player == null
                ||
                minecraft.level == null
        ) {
            stopWindSound(minecraft);
            ClientBlizzardState.clear();
            return;
        }

        if (minecraft.isPaused()) {
            return;
        }

        ClientBlizzardState.tick();

        if (soundRetryTicks > 0) {
            soundRetryTicks--;
        }

        float intensity =
                ClientBlizzardState.getIntensity();

        /*
         * Começa o vento quando a tempestade
         * começa a ficar perceptível.
         */
        if (
                intensity > 0.02F
                && soundRetryTicks == 0
                && minecraft.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MASTER) > 0.0F
                && minecraft.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.WEATHER) > 0.0F
                &&
                (
                        windSound == null
                        ||
                        windSound.isStopped()
                        || !minecraft.getSoundManager().isActive(windSound)
                )
        ) {

            stopWindSound(minecraft);

            windSound =
                    new BlizzardWindSound();

            minecraft.getSoundManager()
                    .play(
                            windSound
                    );
            soundRetryTicks = 20;
        }
    }

    /*
     * =========================================================
     * COR DO FOG
     * =========================================================
     */

    @SubscribeEvent
    public static void onFogColor(
            ViewportEvent.ComputeFogColor event
    ) {

        float intensity =
                getExposedIntensity();

        if (
                intensity <= 0.001F
        ) {
            return;
        }

        /*
         * Branco levemente azulado.
         *
         * Não quero #FFFFFF puro porque
         * fica artificial e mata todo contraste.
         */
        float targetRed =
                0.90F;

        float targetGreen =
                0.94F;

        float targetBlue =
                0.97F;

        /*
         * Quanto maior a tempestade,
         * mais o mundo perde a própria cor.
         */
        float fogStrength =
                intensity
                * 0.92F;

        event.setRed(
                Mth.lerp(
                        fogStrength,
                        event.getRed(),
                        targetRed
                )
        );

        event.setGreen(
                Mth.lerp(
                        fogStrength,
                        event.getGreen(),
                        targetGreen
                )
        );

        event.setBlue(
                Mth.lerp(
                        fogStrength,
                        event.getBlue(),
                        targetBlue
                )
        );
    }

    /*
     * =========================================================
     * DISTÂNCIA DO FOG
     * =========================================================
     */

    @SubscribeEvent
    public static void onRenderFog(
            ViewportEvent.RenderFog event
    ) {

        /*
         * Não sobrescreve fog de água/lava/powder snow.
         */
        if (
                event.getType()
                != FogType.NONE
        ) {
            return;
        }

        float intensity =
                getExposedIntensity();

        if (
                intensity <= 0.001F
        ) {
            return;
        }

        /*
         * Rajada fraca:
         *
         * ainda enxerga longe.
         *
         * Rajada absurda:
         *
         * "onde caralhos está o mundo?"
         */
        float targetFar =
                Mth.lerp(
                        intensity,
                        115.0F,
                        18.0F
                );

        float targetNear =
                Mth.lerp(
                        intensity,
                        18.0F,
                        2.5F
                );

        /*
         * Não aumenta o fog além do vanilla caso
         * o jogador use render distance pequena.
         */
        float far =
                Math.min(
                        event.getFarPlaneDistance(),
                        targetFar
                );

        float near =
                Math.min(
                        targetNear,
                        far * 0.55F
                );

        event.setNearPlaneDistance(
                near
        );

        event.setFarPlaneDistance(
                far
        );

        event.setFogShape(
                FogShape.SPHERE
        );

        /*
         * NECESSÁRIO.
         *
         * RenderFog só aplica as novas distâncias
         * se o evento for cancelado.
         */
        event.setCanceled(true);
    }
    
    /*
     * =========================================================
     * EXPOSIÇÃO
     * =========================================================
     */

    public static float getExposedIntensity() {

        Minecraft minecraft =
                Minecraft.getInstance();

        if (
                minecraft.player == null
                ||
                minecraft.level == null
        ) {

            return 0.0F;
        }

        float storm =
                ClientBlizzardState
                        .getIntensity();

        if (
                storm <= 0.001F
        ) {
            return 0.0F;
        }

        /*
         * MUITO IMPORTANTE.
         *
         * Dentro de:
         *
         * casa
         * caverna
         * túnel
         *
         * o fog quase desaparece.
         *
         * Então continua existindo aquela cena:
         *
         * caverna escura
         *      ↓
         * saída branca
         *      ↓
         * inferno congelado lá fora
         */
        boolean sky =
                minecraft.level.canSeeSky(
                        minecraft.player
                                .blockPosition()
                                .above()
                );

        if (!sky) {

            return storm
                    * 0.035F;
        }

        return storm;
    }

    /*
     * Saiu do mundo.
     */
    @SubscribeEvent
    public static void onLogout(
            ClientPlayerNetworkEvent.LoggingOut event
    ) {

        ClientBlizzardState.clear();

        stopWindSound(Minecraft.getInstance());
    }

    private static void stopWindSound(Minecraft minecraft) {
        if (windSound != null) {
            minecraft.getSoundManager().stop(windSound);
            windSound = null;
        }
        soundRetryTicks = 0;
    }
}
