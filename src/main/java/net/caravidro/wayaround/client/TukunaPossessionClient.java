package net.caravidro.wayaround.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.client.sound.TukunaContractSound;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Client-side half of "the host only watches".
 *
 * The server owns the actual spectator camera target. This class only prevents
 * the host from escaping the intended first-person view with F5.
 */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class TukunaPossessionClient {

    private static boolean possessed;
    private static boolean contractMusic;
    private static TukunaContractSound music;
    private static CameraType previousCamera;

    private TukunaPossessionClient() {
    }

    public static boolean isContractMusicPlaying() {
        return contractMusic;
    }

    public static void setPossessed(
            boolean active,
            boolean playContractMusic
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (active
                && !possessed) {
            previousCamera =
                    minecraft.options
                            .getCameraType();
        }

        possessed =
                active;

        boolean wasPlaying =
                contractMusic;

        /*
         * Publish the state before starting the tickable sound. The sound
         * checks this flag from its own tick(), so it must never observe a
         * stale false value during startup.
         */
        contractMusic =
                playContractMusic;

        if (playContractMusic && !wasPlaying) {
            music = new TukunaContractSound();
            minecraft.getSoundManager().play(music);
        } else if (!playContractMusic && wasPlaying && music != null) {
            minecraft.getSoundManager().stop(music);
            music = null;
        }

        if (!active
                && previousCamera != null) {
            minecraft.options
                    .setCameraType(
                            previousCamera
                    );

            previousCamera =
                    null;
        }
    }

    @SubscribeEvent
    public static void onTick(
            ClientTickEvent.Post event
    ) {
        if (!possessed) {
            if (Minecraft.getInstance().level == null) {
                contractMusic = false;
                music = null;
            }
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.level == null) {
            possessed =
                    false;
            contractMusic = false;
            music = null;
            previousCamera =
                    null;
            return;
        }

        if (minecraft.options
                .getCameraType()
                != CameraType.FIRST_PERSON) {

            minecraft.options
                    .setCameraType(
                            CameraType.FIRST_PERSON
                    );
        }
    }
}
