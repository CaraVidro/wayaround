package net.caravidro.wayaround.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.client.weather.LivingCloudRenderer;
import net.caravidro.wayaround.content.WayAroundContent;
import net.caravidro.wayaround.network.BlueScrollPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client-only input/prediction for Blue.
 *
 * The server remains authoritative for destruction and entity physics. This
 * class only consumes the mouse wheel and mirrors enough motion locally to
 * punch holes through Way Around's client-rendered volumetric clouds.
 */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class BlueClientEffects {

    private static final double MIN_DISTANCE = 2.0;
    private static final double MAX_DISTANCE = 34.0;
    private static final double SCROLL_STEP = 2.2;

    private static boolean wasHolding;
    private static int heldTicks;
    private static double distance = 7.0;
    private static boolean pulledBack;
    private static long pulledBackTick = Long.MIN_VALUE;
    private static boolean slingReady;
    private static long slingReadyTick = Long.MIN_VALUE;

    private static Vec3 lastCenter = Vec3.ZERO;
    private static Vec3 lastLook = new Vec3(0.0, 0.0, 1.0);

    private static Vec3 projectilePosition = Vec3.ZERO;
    private static Vec3 projectileVelocity = Vec3.ZERO;
    private static int projectileLife;

    private BlueClientEffects() {
    }

    @SubscribeEvent
    public static void onScroll(
            InputEvent.MouseScrollingEvent event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || !isHoldingBlue(minecraft)) {
            return;
        }

        double amount =
                event.getScrollDeltaY();

        if (Math.abs(amount) < 0.0001) {
            return;
        }

        PacketDistributor.sendToServer(
                new BlueScrollPayload(amount)
        );

        long tick =
                minecraft.level == null
                        ? 0L
                        : minecraft.level.getGameTime();

        double oldDistance =
                distance;

        distance =
                Mth.clamp(
                        distance
                                + amount
                                * SCROLL_STEP,
                        MIN_DISTANCE,
                        MAX_DISTANCE
                );

        if (distance <= 3.6) {
            pulledBack = true;
            pulledBackTick = tick;
            slingReady = false;
        }

        if (pulledBack
                && tick - pulledBackTick <= 16L
                && distance >= 8.5
                && distance > oldDistance) {
            slingReady = true;
            slingReadyTick = tick;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientTick(
            ClientTickEvent.Post event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || minecraft.player == null
                || minecraft.isPaused()) {
            return;
        }

        boolean holding =
                isHoldingBlue(minecraft);

        long tick =
                minecraft.level.getGameTime();

        if (holding) {
            if (!wasHolding) {
                heldTicks = 0;
                distance = 7.0;
                pulledBack = false;
                slingReady = false;
                pulledBackTick = Long.MIN_VALUE;
                slingReadyTick = Long.MIN_VALUE;
            }

            heldTicks++;

            lastLook =
                    minecraft.player
                            .getLookAngle()
                            .normalize();

            lastCenter =
                    minecraft.player
                            .getEyePosition()
                            .add(
                                    lastLook.scale(
                                            distance
                                    )
                            );

            cutCloud(
                    lastCenter,
                    12.5
            );
        } else if (wasHolding) {
            stopBlueTheme(
                    minecraft
            );

            if (slingReady
                    && tick - slingReadyTick <= 12L) {
                projectilePosition =
                        lastCenter;

                projectileVelocity =
                        lastLook.scale(
                                0.48
                        );

                projectileLife = 100;
            }
        }

        wasHolding = holding;

        if (!holding
                && projectileLife > 0) {
            projectilePosition =
                    projectilePosition.add(
                            projectileVelocity
                    );

            projectileVelocity =
                    projectileVelocity.scale(
                            0.994
                    );

            double fade =
                    projectileLife
                            / 100.0;

            cutCloud(
                    projectilePosition,
                    8.0
                            + fade
                            * 8.0
            );

            projectileLife--;
        }
    }

    public static boolean heldVisualActive() {
        return wasHolding;
    }

    public static Vec3 heldVisualCenter() {
        return lastCenter;
    }

    public static float heldVisualPower() {
        return Mth.clamp(
                heldTicks
                        / 42.0F,
                0.18F,
                1.0F
        );
    }

    public static boolean projectileVisualActive() {
        return projectileLife > 0;
    }

    public static Vec3 projectileVisualCenter() {
        return projectilePosition;
    }

    public static float projectileVisualPower() {
        return Mth.clamp(
                projectileLife
                        / 100.0F,
                0.0F,
                1.0F
        );
    }

    private static void stopBlueTheme(
            Minecraft minecraft
    ) {
        minecraft.getSoundManager()
                .stop(
                        ResourceLocation.fromNamespaceAndPath(
                                WayAround.MODID,
                                "blue_theme"
                        ),
                        SoundSource.PLAYERS
                );
    }

    private static boolean isHoldingBlue(
            Minecraft minecraft
    ) {
        return minecraft.player.isUsingItem()
                && minecraft.player
                        .getUseItem()
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
}
