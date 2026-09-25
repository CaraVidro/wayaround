package net.caravidro.wayaround.client;

import net.caravidro.wayaround.WayAround;
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
    private static CameraType previousCamera;

    private TukunaPossessionClient() {
    }

    public static void setPossessed(
            boolean active
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
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.level == null) {
            possessed =
                    false;
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
