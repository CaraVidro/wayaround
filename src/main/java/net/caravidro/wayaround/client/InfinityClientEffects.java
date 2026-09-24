package net.caravidro.wayaround.client;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.InfinityVisualPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class InfinityClientEffects {

    private InfinityClientEffects() {
    }

    private static final Map<UUID, ClientInfinity>
            FIELDS =
            new HashMap<>();

    private static boolean rotationLocked;
    private static float lockedYaw;
    private static float lockedPitch;
    private static float currentInfluence;

    public static void receive(
            InfinityVisualPayload payload
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        if (payload.confidence()
                <= 0.001F) {

            FIELDS.remove(
                    payload.owner()
            );

            return;
        }

        FIELDS.put(
                payload.owner(),
                new ClientInfinity(
                        payload.owner(),
                        new Vec3(
                                payload.x(),
                                payload.y(),
                                payload.z()
                        ),
                        payload.confidence(),
                        payload.radius(),
                        minecraft.level
                                .getGameTime()
                )
        );
    }

    public static Collection<ClientInfinity> visualFields() {
        return java.util.List.copyOf(
                FIELDS.values()
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

            FIELDS.clear();
            rotationLocked =
                    false;

            currentInfluence =
                    0.0F;

            return;
        }

        long tick =
                minecraft.level
                        .getGameTime();

        FIELDS.values()
                .removeIf(
                        field ->
                                tick
                                        - field.lastSeenTick
                                        > 8L
                );

        float influence =
                strongestInfluence(
                        minecraft
                );

        currentInfluence =
                Mth.lerp(
                        0.34F,
                        currentInfluence,
                        influence
                );

        if (currentInfluence
                >= 0.88F) {

            if (!rotationLocked) {
                rotationLocked =
                        true;

                lockedYaw =
                        minecraft.player
                                .getYRot();

                lockedPitch =
                        minecraft.player
                                .getXRot();
            }

            minecraft.player
                    .setYRot(
                            lockedYaw
                    );

            minecraft.player
                    .setXRot(
                            lockedPitch
                    );

            minecraft.player.yRotO =
                    lockedYaw;

            minecraft.player.xRotO =
                    lockedPitch;

            minecraft.player
                    .setDeltaMovement(
                            Vec3.ZERO
                    );

        } else {
            rotationLocked =
                    false;
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(
            ViewportEvent.ComputeCameraAngles event
    ) {
        if (!rotationLocked) {
            return;
        }

        event.setYaw(
                lockedYaw
        );

        event.setPitch(
                lockedPitch
        );

        event.setRoll(
                0.0F
        );
    }

    @SubscribeEvent
    public static void onGui(
            RenderGuiEvent.Post event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.level == null) {

            return;
        }

        float remote =
                currentInfluence;

        float owned =
                ownedConfidence(
                        minecraft.player
                                .getUUID()
                );

        float shimmer =
                Math.max(
                        remote,
                        owned * 0.34F
                );

        if (shimmer
                < 0.08F) {

            return;
        }

        GuiGraphics graphics =
                event.getGuiGraphics();

        int width =
                minecraft.getWindow()
                        .getGuiScaledWidth();

        int height =
                minecraft.getWindow()
                        .getGuiScaledHeight();

        int edge =
                Math.max(
                        3,
                        Math.round(
                                5.0F
                                        + shimmer
                                                * 20.0F
                        )
                );

        int alpha =
                Mth.clamp(
                        Math.round(
                                18.0F
                                        + shimmer
                                                * 58.0F
                        ),
                        0,
                        95
                );

        int color =
                alpha << 24
                        | 0x8FDFFF;

        graphics.fill(
                0,
                0,
                width,
                edge,
                color
        );

        graphics.fill(
                0,
                height - edge,
                width,
                height,
                color
        );

        graphics.fill(
                0,
                0,
                edge,
                height,
                color
        );

        graphics.fill(
                width - edge,
                0,
                width,
                height,
                color
        );

        if (remote > 0.72F) {
            int veilAlpha =
                    Mth.clamp(
                            Math.round(
                                    (
                                            remote
                                                    - 0.72F
                                    )
                                            / 0.28F
                                            * 52.0F
                            ),
                            0,
                            52
                    );

            graphics.fill(
                    0,
                    0,
                    width,
                    height,
                    veilAlpha << 24
                            | 0xDFF7FF
            );
        }
    }

    private static float strongestInfluence(
            Minecraft minecraft
    ) {
        float best =
                0.0F;

        UUID local =
                minecraft.player
                        .getUUID();

        Vec3 position =
                minecraft.player
                        .getEyePosition();

        for (ClientInfinity field :
                FIELDS.values()) {

            if (field.owner.equals(
                    local
            )) {
                continue;
            }

            double distance =
                    position.distanceTo(
                            field.position
                    );

            if (distance
                    >= field.radius) {

                continue;
            }

            double proximity =
                    1.0
                            - distance
                                    / Math.max(
                                    0.01,
                                    field.radius
                            );

            float influence =
                    (float) Mth.clamp(
                            field.confidence
                                    * (
                                    0.22
                                            + proximity
                                                    * proximity
                                                    * 1.12
                            ),
                            0.0,
                            1.0
                    );

            best =
                    Math.max(
                            best,
                            influence
                    );
        }

        return best;
    }

    private static float ownedConfidence(
            UUID owner
    ) {
        ClientInfinity field =
                FIELDS.get(
                        owner
                );

        return field == null
                ? 0.0F
                : field.confidence;
    }

    public record ClientInfinity(
            UUID owner,
            Vec3 position,
            float confidence,
            float radius,
            long lastSeenTick
    ) {
    }
}
