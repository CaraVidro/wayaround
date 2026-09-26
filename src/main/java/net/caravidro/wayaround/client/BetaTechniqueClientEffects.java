package net.caravidro.wayaround.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.BetaTechniqueVisualPayload;
import net.caravidro.wayaround.sounds.WayAroundSounds;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class BetaTechniqueClientEffects {

    private BetaTechniqueClientEffects() {
    }

    private static final Map<UUID, VisualState>
            STATES =
            new HashMap<>();

    private static boolean hadLevel;

    private static Vec3 shockwaveCenter =
            Vec3.ZERO;

    private static int shockwaveTicks;
    private static final int SHOCKWAVE_DURATION =
            18;

    public static void receive(
            BetaTechniqueVisualPayload payload
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        long tick =
                minecraft.level
                        .getGameTime();

        VisualState state =
                STATES.computeIfAbsent(
                        payload.owner(),
                        owner ->
                                new VisualState(
                                        owner
                                )
                );

        byte previousMode =
                state.mode;

        state.mode =
                payload.mode();

        state.position =
                new Vec3(
                        payload.x(),
                        payload.y(),
                        payload.z()
                );

        state.power =
                payload.power();

        state.progress =
                payload.progress();

        state.lastSeen =
                tick;

        if (payload.mode()
                == BetaTechniqueVisualPayload.BLAST
                && previousMode
                != BetaTechniqueVisualPayload.BLAST) {

            BlueClientEffects.stopOwner(
                    payload.owner()
            );

            /*
             * Music transition is client-timed on the same visual packet as
             * the white flash, so there is no server-distance delay between
             * BLUE going silent and FinalDestination beginning.
             */
            minecraft.getSoundManager()
                    .play(
                            SimpleSoundInstance.forMusic(
                                    WayAroundSounds.FINAL_DESTINATION.get()
                            )
                    );

            shockwaveCenter =
                    state.position;

            shockwaveTicks =
                    SHOCKWAVE_DURATION;

            emitClientShockwave(
                    state.position
            );
        }
    }

    public static boolean hasLocalHeldRed() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return false;
        }

        VisualState state =
                STATES.get(
                        minecraft.player.getUUID()
                );

        return state != null
                && state.mode
                        == BetaTechniqueVisualPayload.RED_HELD
                && minecraft.level != null
                && minecraft.level.getGameTime()
                        - state.lastSeen <= 12L;
    }

    public static boolean hasLocalHeldPurple() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return false;
        VisualState state = STATES.get(minecraft.player.getUUID());
        return state != null && state.mode == BetaTechniqueVisualPayload.PURPLE_HELD
                && minecraft.level.getGameTime() - state.lastSeen <= 12L;
    }

    public static float fusionStrength(
            UUID owner
    ) {
        VisualState state =
                STATES.get(
                        owner
                );

        if (state == null
                || state.mode
                != BetaTechniqueVisualPayload.FUSION) {

            return 0.0F;
        }

        return Mth.clamp(
                state.progress,
                0.0F,
                1.0F
        );
    }

    public static List<VisualTechnique> visuals() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return List.of();
        }

        long tick =
                minecraft.level
                        .getGameTime();

        List<VisualTechnique> result =
                new ArrayList<>();

        for (VisualState state :
                STATES.values()) {

            if (tick - state.lastSeen
                    > 12L) {

                continue;
            }

            result.add(
                    new VisualTechnique(
                            state.owner,
                            state.mode,
                            state.position,
                            state.power,
                            state.progress
                    )
            );
        }

        return result;
    }

    @SubscribeEvent
    public static void onClientTick(
            ClientTickEvent.Post event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            if (hadLevel) {
                STATES.clear();
            }

            hadLevel =
                    false;

            return;
        }

        hadLevel =
                true;

        if (shockwaveTicks > 0) {
            shockwaveTicks--;
        }

        long tick =
                minecraft.level
                        .getGameTime();

        Iterator<Map.Entry<UUID, VisualState>>
                iterator =
                STATES.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            VisualState state =
                    iterator.next()
                            .getValue();

            if (tick - state.lastSeen
                    > 20L) {

                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(
            ViewportEvent.ComputeCameraAngles event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        VisualState state =
                dominantState(
                        minecraft
                );

        if (state == null
                || state.mode
                == BetaTechniqueVisualPayload.RED
                || state.mode
                == BetaTechniqueVisualPayload.RED_HELD
                || state.mode
                == BetaTechniqueVisualPayload.PAIR_BLUE
                || state.mode
                == BetaTechniqueVisualPayload.PURPLE_PROJECTILE) {
            return;
        }

        if (state.mode == BetaTechniqueVisualPayload.PURPLE_HELD) {

            return;
        }

        double time =
                minecraft.level
                        .getGameTime()
                        + event.getPartialTick();

        float strength =
                switch (state.mode) {
                    case BetaTechniqueVisualPayload.FUSION ->
                            0.12F
                                    + state.progress
                                            * state.progress
                                            * 3.35F;

                    case BetaTechniqueVisualPayload.BLAST ->
                            6.50F;

                    case BetaTechniqueVisualPayload.AFTERMATH ->
                            (
                                    1.0F
                                            - state.progress
                            )
                                    * 1.75F;

                    default -> 0.0F;
                };

        float yaw =
                (float) Math.sin(
                        time * 3.17
                                + state.owner
                                    .hashCode()
                                        * 0.019
                )
                        * strength;

        float pitch =
                (float) Math.cos(
                        time * 3.73
                                + 0.9
                )
                        * strength
                        * 0.72F;

        float roll =
                (float) Math.sin(
                        time * 2.81
                                + 2.0
                )
                        * strength
                        * 0.54F;

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
        Minecraft minecraft =
                Minecraft.getInstance();

        VisualState state =
                dominantState(
                        minecraft
                );

        if (state == null
                || state.mode
                == BetaTechniqueVisualPayload.RED
                || state.mode
                == BetaTechniqueVisualPayload.PAIR_BLUE
                || state.mode
                == BetaTechniqueVisualPayload.PURPLE_PROJECTILE) {
            return;
        }

        if (state.mode == BetaTechniqueVisualPayload.PURPLE_HELD) {

            return;
        }

        if (state.mode
                == BetaTechniqueVisualPayload.FUSION) {

            float amount =
                    state.progress
                            * 0.88F;

            event.setRed(
                    event.getRed()
                            * (
                                    1.0F
                                            - amount
                                                    * 0.76F
                            )
                            + 0.12F
                                    * amount
            );

            event.setGreen(
                    event.getGreen()
                            * (
                                    1.0F
                                            - amount
                                                    * 0.90F
                            )
            );

            event.setBlue(
                    event.getBlue()
                            * (
                                    1.0F
                                            - amount
                                                    * 0.52F
                            )
                            + 0.25F
                                    * amount
            );

        } else if (state.mode
                == BetaTechniqueVisualPayload.AFTERMATH) {

            float remaining =
                    1.0F
                            - state.progress;

            event.setRed(
                    event.getRed()
                            * (
                                    0.55F
                                            + state.progress
                                                    * 0.45F
                            )
                            + 0.16F
                                    * remaining
            );

            event.setGreen(
                    event.getGreen()
                            * (
                                    0.48F
                                            + state.progress
                                                    * 0.52F
                            )
            );

            event.setBlue(
                    event.getBlue()
                            * (
                                    0.70F
                                            + state.progress
                                                    * 0.30F
                            )
                            + 0.22F
                                    * remaining
            );
        }
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

        Minecraft minecraft =
                Minecraft.getInstance();

        VisualState state =
                dominantState(
                        minecraft
                );

        if (state == null
                || state.mode
                == BetaTechniqueVisualPayload.RED
                || state.mode
                == BetaTechniqueVisualPayload.PAIR_BLUE
                || state.mode
                == BetaTechniqueVisualPayload.PURPLE_PROJECTILE) {
            return;
        }

        if (state.mode == BetaTechniqueVisualPayload.PURPLE_HELD) {

            return;
        }

        if (state.mode == BetaTechniqueVisualPayload.RED_HELD) {
            int width = minecraft.getWindow().getGuiScaledWidth();
            int height = minecraft.getWindow().getGuiScaledHeight();
            int alpha = Mth.clamp(Math.round(18.0F + state.power * 8.0F), 18, 34);
            event.getGuiGraphics().fill(
                    0, 0, width, height,
                    alpha << 24 | 0x5A0000
            );
            return;
        }

        int width =
                minecraft.getWindow()
                        .getGuiScaledWidth();

        int height =
                minecraft.getWindow()
                        .getGuiScaledHeight();

        if (state.mode
                == BetaTechniqueVisualPayload.FUSION) {

            if (state.progress >= 0.86F) {
                event.getGuiGraphics()
                        .fill(
                                0,
                                0,
                                width,
                                height,
                                0xFF000000
                        );

                return;
            }

            int black =
                    Mth.clamp(
                            Math.round(
                                    48.0F
                                            + state.progress
                                                    * 188.0F
                            ),
                            0,
                            236
                    );

            event.getGuiGraphics()
                    .fill(
                            0,
                            0,
                            width,
                            height,
                            black << 24
                                    | 0x000000
                    );

            int purple =
                    Mth.clamp(
                            Math.round(
                                    state.progress
                                            * 72.0F
                            ),
                            0,
                            86
                    );

            event.getGuiGraphics()
                    .fill(
                            0,
                            0,
                            width,
                            height,
                            purple << 24
                                    | 0x4A00A8
                    );

            return;
        }

        float whiteStrength =
                state.mode
                        == BetaTechniqueVisualPayload.BLAST
                        ? 1.0F
                        : 1.0F
                                - state.progress;

        int alpha =
                Mth.clamp(
                        Math.round(
                                whiteStrength
                                        * 255.0F
                        ),
                        0,
                        255
                );

        event.getGuiGraphics()
                .fill(
                        0,
                        0,
                        width,
                        height,
                        alpha << 24
                                | 0xFFFFFF
                );
    }

    private static VisualState dominantState(
            Minecraft minecraft
    ) {
        if (minecraft.level == null) {
            return null;
        }

        long tick =
                minecraft.level
                        .getGameTime();

        VisualState best =
                null;

        int bestPriority =
                -1;

        for (VisualState state :
                STATES.values()) {

            if (tick - state.lastSeen
                    > 12L) {

                continue;
            }

            int priority =
                    switch (state.mode) {
                        case BetaTechniqueVisualPayload.BLAST -> 4;
                        case BetaTechniqueVisualPayload.AFTERMATH -> 3;
                        case BetaTechniqueVisualPayload.FUSION -> 2;
                        default -> 1;
                    };

            if (priority > bestPriority) {
                best =
                        state;

                bestPriority =
                        priority;
            }
        }

        return best;
    }

    private static void emitClientShockwave(
            Vec3 center
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        for (int index = 0;
             index < 360;
             index++) {

            double y =
                    minecraft.level
                            .random
                            .nextDouble()
                            * 2.0
                            - 1.0;

            double theta =
                    minecraft.level
                            .random
                            .nextDouble()
                            * Math.PI
                            * 2.0;

            double horizontal =
                    Math.sqrt(
                            Math.max(
                                    0.0,
                                    1.0
                                            - y * y
                            )
                    );

            double speed =
                    0.9
                            + minecraft.level
                                    .random
                                    .nextDouble()
                                    * 2.3;

            minecraft.level
                    .addParticle(
                            index % 4 == 0
                                    ? ParticleTypes.END_ROD
                                    : ParticleTypes.ELECTRIC_SPARK,
                            center.x,
                            center.y,
                            center.z,
                            Math.cos(
                                    theta
                            )
                                    * horizontal
                                    * speed,
                            y * speed,
                            Math.sin(
                                    theta
                            )
                                    * horizontal
                                    * speed
                    );
        }
    }

    public static Vec3 particleShockwaveImpulse(
            double x,
            double y,
            double z
    ) {
        if (shockwaveTicks <= 0) {
            return Vec3.ZERO;
        }

        Vec3 away =
                new Vec3(
                        x,
                        y,
                        z
                )
                        .subtract(
                                shockwaveCenter
                        );

        double distance =
                away.length();

        if (distance < 0.08
                || distance > 168.0) {

            return Vec3.ZERO;
        }

        double timeStrength =
                shockwaveTicks
                        / (double) SHOCKWAVE_DURATION;

        double distanceStrength =
                1.0
                        - distance
                                / 168.0;

        double impulse =
                (
                        0.055
                                + distanceStrength
                                        * distanceStrength
                                        * 1.65
                )
                        * timeStrength;

        Vec3 direction =
                away.scale(
                        1.0
                                / distance
                );

        return direction.scale(
                impulse
        )
                .add(
                        0.0,
                        0.035
                                * distanceStrength
                                * timeStrength,
                        0.0
                );
    }

    public record VisualTechnique(
            UUID owner,
            byte mode,
            Vec3 position,
            float power,
            float progress
    ) {
    }

    private static final class VisualState {

        private final UUID owner;

        private byte mode;
        private Vec3 position =
                Vec3.ZERO;
        private float power;
        private float progress;
        private long lastSeen;

        private VisualState(
                UUID owner
        ) {
            this.owner =
                    owner;
        }
    }
}
