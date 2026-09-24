package net.caravidro.wayaround.client;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.ImmortalWheelVisualPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class ImmortalWheelClientEffects {

    private ImmortalWheelClientEffects() {
    }

    private static final Map<UUID, WheelVisual>
            WHEELS =
            new HashMap<>();

    public static void receive(
            ImmortalWheelVisualPayload payload
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (payload.action()
                == ImmortalWheelVisualPayload.REMOVE) {

            WHEELS.remove(
                    payload.owner()
            );

            return;
        }

        WheelVisual wheel =
                WHEELS.computeIfAbsent(
                        payload.owner(),
                        WheelVisual::new
                );

        wheel.active =
                true;

        wheel.lastPresenceTick =
                minecraft.level == null
                        ? 0L
                        : minecraft.level
                                .getGameTime();

        wheel.steps =
                Math.max(
                        wheel.steps,
                        payload.steps()
                );

        if (payload.action()
                == ImmortalWheelVisualPayload.HIT) {

            wheel.progress =
                    payload.progress();

            wheel.shakeTicks =
                    Math.max(
                            wheel.shakeTicks,
                            10
                                    + payload.progress()
                                            * 4
                    );

        } else if (payload.action()
                == ImmortalWheelVisualPayload.SPIN) {

            wheel.progress =
                    0;

            wheel.steps =
                    payload.steps();

            wheel.spinVelocity =
                    38.0F
                            + payload.steps()
                                    * 6.0F;

            wheel.spinBurstTicks =
                    20;

            wheel.shakeTicks =
                    10;

            if (minecraft.player != null
                    && minecraft.player
                            .getUUID()
                            .equals(
                                    payload.owner()
                            )) {

                minecraft.player
                        .displayClientMessage(
                                Component.translatable(
                                                "message.wayaround.immortal_wheel.adapt",
                                                familyName(
                                                        payload.family()
                                                ),
                                                Math.min(
                                                        100,
                                                        payload.steps()
                                                                * 20
                                                )
                                        )
                                        .withStyle(
                                                ChatFormatting.GOLD
                                        ),
                                true
                        );
            }
        }
    }

    @SubscribeEvent
    public static void tick(
            ClientTickEvent.Post event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            WHEELS.clear();
            return;
        }

        long tick =
                minecraft.level
                        .getGameTime();

        var iterator =
                WHEELS.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            WheelVisual wheel =
                    iterator.next()
                            .getValue();

            if (!wheel.active
                    || tick - wheel.lastPresenceTick
                            > 70L) {

                iterator.remove();
                continue;
            }

            wheel.angle =
                    wrap(
                            wheel.angle
                                    + wheel.spinVelocity
                    );

            wheel.spinVelocity *=
                    0.82F;

            if (Math.abs(
                    wheel.spinVelocity
            )
                    < 0.05F) {

                wheel.spinVelocity =
                        0.0F;
            }

            if (wheel.spinBurstTicks > 0) {
                wheel.spinBurstTicks--;
            }

            if (wheel.shakeTicks > 0) {
                wheel.shakeTicks--;
            }
        }
    }

    public static Collection<WheelVisual> visuals() {
        return WHEELS.values();
    }

    public static Vec3 particleWindImpulse(
            double x,
            double y,
            double z
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || WHEELS.isEmpty()) {

            return Vec3.ZERO;
        }

        Vec3 total =
                Vec3.ZERO;

        for (WheelVisual wheel :
                WHEELS.values()) {

            if (wheel.spinBurstTicks <= 0) {
                continue;
            }

            Player player =
                    minecraft.level
                            .getPlayerByUUID(
                                    wheel.owner
                            );

            if (player == null) {
                continue;
            }

            Vec3 center =
                    player.position()
                            .add(
                                    0.0,
                                    player.getBbHeight()
                                            + 0.72,
                                    0.0
                            );

            Vec3 away =
                    new Vec3(
                            x,
                            y,
                            z
                    )
                            .subtract(
                                    center
                            );

            double distance =
                    away.length();

            if (distance < 0.08
                    || distance > 11.0) {

                continue;
            }

            double life =
                    wheel.spinBurstTicks
                            / 20.0;

            double falloff =
                    1.0
                            - distance
                                    / 11.0;

            Vec3 radial =
                    away.scale(
                            1.0
                                    / distance
                    );

            Vec3 tangent =
                    new Vec3(
                            -radial.z,
                            0.0,
                            radial.x
                    );

            total =
                    total.add(
                            radial.scale(
                                    falloff
                                            * life
                                            * 0.15
                            )
                                    .add(
                                            tangent.scale(
                                                    falloff
                                                            * life
                                                            * 0.09
                                            )
                                    )
                                    .add(
                                            0.0,
                                            falloff
                                                    * life
                                                    * 0.022,
                                            0.0
                                    )
                    );
        }

        return total;
    }

    private static Component familyName(
            String family
    ) {
        String safe =
                switch (family) {
                    case "arrow",
                            "projectile",
                            "explosion",
                            "fire",
                            "fall",
                            "freezing",
                            "lightning",
                            "player_melee",
                            "mob_melee",
                            "magic",
                            "sonic",
                            "drowning",
                            "generic" ->
                            family;

                    default ->
                            "generic";
                };

        return Component.translatable(
                "message.wayaround.immortal_wheel.family."
                        + safe
        );
    }

    private static float wrap(
            float angle
    ) {
        float result =
                angle % 360.0F;

        return result < 0.0F
                ? result + 360.0F
                : result;
    }

    public static final class WheelVisual {

        private final UUID owner;

        private boolean active;
        private int progress;
        private int steps;
        private int shakeTicks;
        private int spinBurstTicks;
        private float angle;
        private float spinVelocity;
        private long lastPresenceTick;

        private Vec3 smoothedAnchor;

        private WheelVisual(
                UUID owner
        ) {
            this.owner =
                    owner;
        }

        public UUID owner() {
            return owner;
        }

        public float angle() {
            return angle;
        }

        public int steps() {
            return steps;
        }

        public float shakeStrength() {
            float progressShake =
                    progress
                            / 4.0F;

            float temporal =
                    shakeTicks > 0
                            ? Math.min(
                            1.0F,
                            shakeTicks
                                    / 12.0F
                    )
                            : 0.0F;

            return Math.max(
                    progressShake
                            * 0.70F,
                    temporal
                            * 0.55F
            );
        }

        public Vec3 updateAnchor(
                Vec3 target
        ) {
            if (smoothedAnchor == null) {
                smoothedAnchor =
                        target;

                return smoothedAnchor;
            }

            /*
             * Intentional cursed-aura lag: the wheel is attached to the
             * holder, but chases their head instead of being welded to it.
             */
            smoothedAnchor =
                    smoothedAnchor.lerp(
                            target,
                            0.17
                    );

            return smoothedAnchor;
        }
    }
}
