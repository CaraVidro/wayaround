package net.caravidro.wayaround.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.client.sound.TukunaContractSound;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

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
    private static boolean linkedView;
    private static int obscureTicks;
    private static int obscureTotal;
    private static boolean contractMusic;
    private static boolean loopContractMusic;
    private static TukunaContractSound music;
    private static int musicRetryTicks;
    private static CameraType previousCamera;

    private static final Map<UUID, VisualLink> VISUAL_LINKS =
            new HashMap<>();

    private static final long VISUAL_LINK_TIMEOUT_TICKS = 90L;

    private TukunaPossessionClient() {
    }

    public static boolean isContractMusicPlaying() {
        return contractMusic;
    }

    public static boolean shouldLoopContractMusic() {
        return loopContractMusic;
    }

    public static boolean isHostWatchingPossession() {
        return possessed;
    }

    public static void setVisualLink(
            UUID controller,
            UUID body,
            boolean active
    ) {
        if (!active) {
            VISUAL_LINKS.remove(controller);
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        long seenAt = minecraft.level == null
                ? 0L
                : minecraft.level.getGameTime();

        VISUAL_LINKS.put(
                controller,
                new VisualLink(body, seenAt)
        );
    }

    public static boolean isHiddenController(
            UUID player
    ) {
        return VISUAL_LINKS.containsKey(player);
    }

    public static UUID controllerForBody(
            UUID body
    ) {
        for (Map.Entry<UUID, VisualLink> entry : VISUAL_LINKS.entrySet()) {
            if (entry.getValue().body.equals(body)) {
                return entry.getKey();
            }
        }

        return null;
    }

    public static UUID bodyForController(
            UUID controller
    ) {
        VisualLink link = VISUAL_LINKS.get(controller);
        return link == null ? null : link.body;
    }

    public static void setPossessed(
            boolean active,
            boolean playContractMusic,
            boolean loopMusic
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        boolean wasLocked = cameraLocked();
        possessed = active;
        updateCameraLock(minecraft, wasLocked);

        boolean wasPlaying =
                contractMusic;

        /*
         * Publish the state before starting the tickable sound. The sound
         * checks this flag from its own tick(), so it must never observe a
         * stale false value during startup.
         */
        contractMusic =
                playContractMusic;

        loopContractMusic =
                playContractMusic && loopMusic;

        if (playContractMusic && !wasPlaying) {
            music = new TukunaContractSound();
            minecraft.getSoundManager().play(music);
            musicRetryTicks = 20;
        } else if (!playContractMusic && wasPlaying && music != null) {
            minecraft.getSoundManager().stop(music);
            music = null;
        }

    }

    public static void setLinkedView(
            boolean active,
            int obscureForTicks
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean wasLocked = cameraLocked();
        linkedView = active;

        if (obscureForTicks > 0) {
            obscureTotal = Math.max(1, obscureForTicks);
            obscureTicks = obscureTotal;
        }

        updateCameraLock(minecraft, wasLocked);
    }

    private static boolean cameraLocked() {
        return possessed || linkedView;
    }

    private static void updateCameraLock(
            Minecraft minecraft,
            boolean wasLocked
    ) {
        boolean nowLocked = cameraLocked();

        if (!wasLocked && nowLocked) {
            previousCamera = minecraft.options.getCameraType();
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        } else if (wasLocked && !nowLocked && previousCamera != null) {
            minecraft.options.setCameraType(previousCamera);
            previousCamera = null;
        }
    }

    @SubscribeEvent
    public static void obscure(RenderGuiEvent.Post event) {
        if (obscureTicks <= 0) return;

        Minecraft minecraft = Minecraft.getInstance();
        float progress = 1.0F - obscureTicks / (float)Math.max(1, obscureTotal);
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        float grayPhase = Math.min(1.0F, progress / 0.48F);
        float blackPhase = Math.max(0.0F, (progress - 0.34F) / 0.66F);
        int grayAlpha = (int)(92.0F * (1.0F - blackPhase) * grayPhase);
        int blackAlpha = (int)(232.0F * blackPhase);

        if (grayAlpha > 0) {
            event.getGuiGraphics().fill(
                    0, 0, width, height,
                    (grayAlpha << 24) | 0x909090
            );

            for (int i = 0; i < 5; i++) {
                int y = (int)(
                        height / 5.0F * i
                                + Math.sin((obscureTicks + i * 7) * 0.37D) * 4.0D
                );
                event.getGuiGraphics().fill(
                        0,
                        Math.max(0, y),
                        width,
                        Math.min(height, y + 3),
                        (Math.min(70, grayAlpha) << 24) | 0xC0C0C0
                );
            }
        }

        if (blackAlpha > 0) {
            event.getGuiGraphics().fill(
                    0, 0, width, height,
                    blackAlpha << 24
            );
        }
    }

    @SubscribeEvent
    public static void onTick(
            ClientTickEvent.Post event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            VISUAL_LINKS.clear();
        } else {
            long now = minecraft.level.getGameTime();
            VISUAL_LINKS.entrySet().removeIf(entry ->
                    now - entry.getValue().lastSeen > VISUAL_LINK_TIMEOUT_TICKS
            );
        }

        if (musicRetryTicks > 0) {
            musicRetryTicks--;
        }

        if (obscureTicks > 0) {
            obscureTicks--;
        }

        /*
         * Indefinite control must never silently lose its omen. If the sound
         * engine drops the initial play request or the source stops, retry it.
         */
        if (contractMusic
                && loopContractMusic
                && minecraft.level != null
                && minecraft.player != null
                && musicRetryTicks <= 0
                && (music == null || !minecraft.getSoundManager().isActive(music))) {
            music = new TukunaContractSound();
            minecraft.getSoundManager().play(music);
            musicRetryTicks = 20;
        }

        if (!cameraLocked()) {
            if (minecraft.level == null) {
                contractMusic = false;
                loopContractMusic = false;
                music = null;
                musicRetryTicks = 0;
                obscureTicks = 0;
                obscureTotal = 0;
            }
            return;
        }

        if (minecraft.player == null
                || minecraft.level == null) {
            possessed =
                    false;
            linkedView = false;
            contractMusic = false;
            loopContractMusic = false;
            music = null;
            musicRetryTicks = 0;
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

    private record VisualLink(
            UUID body,
            long lastSeen
    ) {}
}
