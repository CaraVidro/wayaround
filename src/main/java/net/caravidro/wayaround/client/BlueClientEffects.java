package net.caravidro.wayaround.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.client.sound.BlueSpatialSound;
import net.caravidro.wayaround.client.weather.LivingCloudRenderer;
import net.caravidro.wayaround.content.WayAroundContent;
import net.caravidro.wayaround.network.BlueScrollPayload;
import net.caravidro.wayaround.network.BlueVisualPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client presentation and input layer for Blue.
 *
 * Server snapshots make every nearby player see/hear the same Blue. The local
 * charge preview is the only predicted piece; destruction and movement remain
 * server authoritative.
 */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class BlueClientEffects {

    private static final double SHAKE_RANGE = 75.0;
    private static final double DARK_RANGE = 50.0;
    private static final int SNAPSHOT_TIMEOUT = 8;
    private static final int OWNER_SOUND_LINGER = 20;

    private static final Map<UUID, ClientBlue> BLUES =
            new HashMap<>();

    private static boolean charging;
    private static int chargeTicks;
    private static Vec3 chargeCenter =
            Vec3.ZERO;

    private static boolean hadLevel;

    private BlueClientEffects() {
    }

    public static void receive(
            BlueVisualPayload payload
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        long tick =
                minecraft.level
                        .getGameTime();

        ClientBlue state =
                BLUES.computeIfAbsent(
                        payload.owner(),
                        owner ->
                                new ClientBlue(
                                        owner,
                                        new Vec3(
                                                payload.x(),
                                                payload.y(),
                                                payload.z()
                                        )
                                )
                );

        byte previousMode =
                state.mode;

        state.position =
                new Vec3(
                        payload.x(),
                        payload.y(),
                        payload.z()
                );

        state.power =
                payload.power();

        state.radius =
                payload.radius();

        state.mode =
                payload.mode();

        if (minecraft.player != null
                && minecraft.player
                        .getUUID()
                        .equals(
                                payload.owner()
                        )
                && payload.mode()
                        != BlueVisualPayload.ACTIVE) {
            minecraft.player
                    .stopUsingItem();
        }

        state.lastSeen =
                tick;

        state.gone =
                false;

        state.lingerTicks =
                0;

        if (payload.mode()
                == BlueVisualPayload.LAUNCHED
                && previousMode
                        != BlueVisualPayload.LAUNCHED
                && state.sound != null) {
            minecraft.getSoundManager()
                    .stop(
                            state.sound
                    );

            state.sound =
                    null;
        }

        if (state.sound == null) {
            state.sound =
                    new BlueSpatialSound(
                            state.owner
                    );

            minecraft.getSoundManager()
                    .play(
                            state.sound
                    );
        }
    }

    @SubscribeEvent
    public static void onScroll(
            InputEvent.MouseScrollingEvent event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || !hasBlueItem(
                        minecraft
                )
                || !hasOwnedControllableBlue(
                        minecraft.player.getUUID()
                )) {
            return;
        }

        double amount =
                event.getScrollDeltaY();

        if (Math.abs(
                amount
        ) < 0.0001) {
            return;
        }

        PacketDistributor.sendToServer(
                new BlueScrollPayload(
                        amount
                )
        );

        event.setCanceled(
                true
        );
    }

    @SubscribeEvent
    public static void onClientTick(
            ClientTickEvent.Post event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || minecraft.player == null) {
            if (hadLevel) {
                BLUES.clear();
                charging = false;
                chargeTicks = 0;
            }

            hadLevel = false;
            return;
        }

        hadLevel = true;

        if (minecraft.isPaused()) {
            return;
        }

        long tick =
                minecraft.level
                        .getGameTime();

        boolean usingBlue =
                minecraft.player.isUsingItem()
                        && minecraft.player
                                .getUseItem()
                                .is(
                                        WayAroundContent.BLUE.get()
                                );

        if (usingBlue
                && !hasOwnedControllableBlue(
                        minecraft.player.getUUID()
                )) {
            charging =
                    true;

            chargeTicks++;

            chargeCenter =
                    minecraft.player
                            .getEyePosition()
                            .add(
                                    minecraft.player
                                            .getLookAngle()
                                            .normalize()
                                            .scale(
                                                    2.25
                                            )
                            );
        } else {
            charging =
                    false;

            chargeTicks =
                    0;
        }

        Iterator<Map.Entry<UUID, ClientBlue>> iterator =
                BLUES.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            ClientBlue state =
                    iterator.next()
                            .getValue();

            long staleFor =
                    tick
                            - state.lastSeen;

            if (staleFor <= SNAPSHOT_TIMEOUT) {
                state.gone =
                        false;

                state.lingerTicks =
                        0;

                if (state.mode
                        != BlueVisualPayload.COLLAPSING) {
                    cutCloud(
                            state.position,
                            9.0
                                    + state.power
                                            * 8.0
                    );
                }

                continue;
            }

            state.gone =
                    true;

            boolean localOwner =
                    minecraft.player.getUUID()
                            .equals(
                                    state.owner
                            );

            if (localOwner
                    && state.lingerTicks
                            < OWNER_SOUND_LINGER) {
                state.lingerTicks++;
                continue;
            }

            iterator.remove();
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(
            ViewportEvent.ComputeCameraAngles event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || minecraft.player == null) {
            return;
        }

        ClientBlue threat =
                strongestRemoteThreat(
                        minecraft
                );

        if (threat == null) {
            return;
        }

        double distance =
                minecraft.player
                        .getEyePosition()
                        .distanceTo(
                                threat.position
                        );

        if (distance > SHAKE_RANGE) {
            return;
        }

        double closeness =
                1.0
                        - distance
                                / SHAKE_RANGE;

        float strength =
                (float) (
                        0.035
                                + Math.pow(
                                        Math.max(
                                                0.0,
                                                closeness
                                        ),
                                        1.45
                                )
                                        * 0.965
                );

        /*
         * Once the player is actually captured, the violent shaking backs
         * off. This makes the transition into orbit readable instead of
         * turning the screen into visual noise.
         */
        if (beingCarried(
                minecraft,
                threat,
                distance
        )) {
            strength *=
                    distance < 4.0
                            ? 0.06F
                            : 0.18F;
        }

        double time =
                minecraft.level
                        .getGameTime()
                        + event.getPartialTick();

        float yaw =
                (float) Math.sin(
                        time
                                * 1.72
                                + threat.owner
                                    .hashCode()
                                        * 0.013
                )
                        * 1.65F
                        * strength;

        float pitch =
                (float) Math.cos(
                        time
                                * 2.11
                                + threat.owner
                                    .hashCode()
                                        * 0.009
                )
                        * 1.20F
                        * strength;

        float roll =
                (float) Math.sin(
                        time
                                * 1.27
                                + 1.4
                )
                        * 1.05F
                        * strength;

        event.setYaw(
                event.getYaw()
                        + yaw
        );

        event.setPitch(
                event.getPitch()
                        + pitch
        );

        event.setRoll(
                event.getRoll()
                        + roll
        );
    }

    @SubscribeEvent
    public static void onFogColor(
            ViewportEvent.ComputeFogColor event
    ) {
        float darkness =
                darknessStrength();

        if (darkness <= 0.001F) {
            return;
        }

        /*
         * Pull most red/green out of the scene while preserving a little
         * blue. The cube renderer itself is saturated cyan/blue, so it reads
         * like the only powerful light source in the darkened environment.
         */
        event.setRed(
                event.getRed()
                        * (
                                1.0F
                                        - darkness
                                                * 0.78F
                        )
        );

        event.setGreen(
                event.getGreen()
                        * (
                                1.0F
                                        - darkness
                                                * 0.70F
                        )
        );

        event.setBlue(
                event.getBlue()
                        * (
                                1.0F
                                        - darkness
                                                * 0.42F
                        )
        );
    }

    @SubscribeEvent
    public static void onGuiLayer(
            RenderGuiLayerEvent.Post event
    ) {
        if (!event.getName()
                .equals(
                        VanillaGuiLayers.CAMERA_OVERLAYS
                )) {
            return;
        }

        float darkness =
                darknessStrength();

        if (darkness <= 0.001F) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        int alpha =
                Mth.clamp(
                        Math.round(
                                darkness
                                        * 118.0F
                        ),
                        0,
                        118
                );

        int argb =
                alpha << 24
                        | 0x0001070D;

        event.getGuiGraphics()
                .fill(
                        0,
                        0,
                        minecraft.getWindow()
                                .getGuiScaledWidth(),
                        minecraft.getWindow()
                                .getGuiScaledHeight(),
                        argb
                );
    }

    public static boolean chargeVisualActive() {
        return charging;
    }

    public static Vec3 chargeVisualCenter() {
        return chargeCenter;
    }

    public static float chargeVisualPower() {
        return Mth.clamp(
                0.08F
                        + chargeTicks
                                / 58.0F,
                0.08F,
                1.35F
        );
    }

    public static List<VisualBlue> visualBlues() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return List.of();
        }

        long tick =
                minecraft.level
                        .getGameTime();

        List<VisualBlue> result =
                new ArrayList<>();

        for (ClientBlue state :
                BLUES.values()) {
            if (state.gone
                    || tick - state.lastSeen
                            > SNAPSHOT_TIMEOUT) {
                continue;
            }

            result.add(
                    new VisualBlue(
                            state.owner,
                            state.position,
                            state.power,
                            state.radius,
                            state.mode
                    )
            );
        }

        return result;
    }

    public static SoundSample soundSample(
            UUID owner
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        ClientBlue state =
                BLUES.get(
                        owner
                );

        if (minecraft.player == null
                || state == null) {
            return null;
        }

        float volume =
                4.0F;

        if (state.gone) {
            if (!minecraft.player
                    .getUUID()
                    .equals(
                            owner
                    )) {
                return null;
            }

            float remaining =
                    1.0F
                            - state.lingerTicks
                                    / (float) OWNER_SOUND_LINGER;

            if (remaining <= 0.0F) {
                return null;
            }

            volume *=
                    remaining;
        } else if (state.mode
                == BlueVisualPayload.COLLAPSING) {
            volume =
                    3.4F;
        }

        return new SoundSample(
                state.position,
                volume,
                state.mode
                        == BlueVisualPayload.LAUNCHED
                                ? 0.97F
                                : 1.0F
        );
    }

    private static ClientBlue strongestRemoteThreat(
            Minecraft minecraft
    ) {
        ClientBlue best =
                null;

        double bestDistance =
                Double.MAX_VALUE;

        UUID local =
                minecraft.player
                        .getUUID();

        for (ClientBlue state :
                BLUES.values()) {
            if (state.gone
                    || state.owner.equals(
                            local
                    )) {
                continue;
            }

            double distance =
                    minecraft.player
                            .getEyePosition()
                            .distanceTo(
                                    state.position
                            );

            if (distance <= SHAKE_RANGE
                    && distance < bestDistance) {
                best =
                        state;

                bestDistance =
                        distance;
            }
        }

        return best;
    }

    private static boolean beingCarried(
            Minecraft minecraft,
            ClientBlue state,
            double distance
    ) {
        if (distance < 4.0) {
            return true;
        }

        if (distance > state.radius
                * 0.80) {
            return false;
        }

        Vec3 toward =
                state.position.subtract(
                        minecraft.player
                                .getEyePosition()
                );

        if (toward.lengthSqr()
                < 0.001) {
            return true;
        }

        toward =
                toward.normalize();

        return minecraft.player
                .getDeltaMovement()
                .dot(
                        toward
                )
                > 0.035;
    }

    private static float darknessStrength() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || minecraft.player == null) {
            return 0.0F;
        }

        double nearest =
                Double.MAX_VALUE;

        float power =
                0.0F;

        Vec3 eye =
                minecraft.player
                        .getEyePosition();

        for (ClientBlue state :
                BLUES.values()) {
            if (state.gone) {
                continue;
            }

            double distance =
                    eye.distanceTo(
                            state.position
                    );

            if (distance < nearest) {
                nearest =
                        distance;

                power =
                        state.power;
            }
        }

        if (nearest > DARK_RANGE) {
            return 0.0F;
        }

        double closeness =
                1.0
                        - nearest
                                / DARK_RANGE;

        return Mth.clamp(
                (float) (
                        Math.pow(
                                closeness,
                                1.18
                        )
                                * (
                                        0.56
                                                + Math.min(
                                                        0.30,
                                                        power
                                                                * 0.20
                                                )
                                )
                ),
                0.0F,
                0.78F
        );
    }

    private static boolean hasOwnedControllableBlue(
            UUID owner
    ) {
        ClientBlue state =
                BLUES.get(
                        owner
                );

        return state != null
                && !state.gone
                && state.mode
                        == BlueVisualPayload.ACTIVE;
    }

    private static boolean hasBlueItem(
            Minecraft minecraft
    ) {
        return minecraft.player
                .getMainHandItem()
                .is(
                        WayAroundContent.BLUE.get()
                )
                || minecraft.player
                        .getOffhandItem()
                        .is(
                                WayAroundContent.BLUE.get()
                        );
    }

    private static void cutCloud(
            Vec3 position,
            double radius
    ) {
        if (LivingCloudRenderer.isInsideCloud(
                position
        )) {
            LivingCloudRenderer.punchHole(
                    position,
                    radius
            );
        }
    }

    public record VisualBlue(
            UUID owner,
            Vec3 position,
            float power,
            float radius,
            byte mode
    ) {
    }

    public record SoundSample(
            Vec3 position,
            float volume,
            float pitch
    ) {
    }

    private static final class ClientBlue {

        private final UUID owner;

        private Vec3 position;

        private float power;
        private float radius;
        private byte mode;

        private long lastSeen;
        private boolean gone;
        private int lingerTicks;

        private BlueSpatialSound sound;

        private ClientBlue(
                UUID owner,
                Vec3 position
        ) {
            this.owner = owner;
            this.position = position;
        }
    }
}
