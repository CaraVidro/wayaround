package net.caravidro.wayaround.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.network.AccessoryStateS2CPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(
        modid = net.caravidro.wayaround.WayAround.MODID,
        value = Dist.CLIENT
)
public final class AccessoryClientState {

    private AccessoryClientState() {}

    private static final Map<UUID, State> STATES =
            new HashMap<>();

    public static void receive(
            AccessoryStateS2CPayload payload
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        long seen =
                minecraft.level == null
                        ? 0L
                        : minecraft.level.getGameTime();

        STATES.put(
                payload.player(),
                new State(
                        payload.head(),
                        payload.hands(),
                        payload.torso(),
                        payload.feet(),
                        payload.glassesMode(),
                        seen
                )
        );
    }

    public static State get(
            UUID player
    ) {
        return STATES.get(
                player
        );
    }

    @SubscribeEvent
    public static void tick(
            ClientTickEvent.Post event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            STATES.clear();
            return;
        }

        long now =
                minecraft.level.getGameTime();

        STATES.entrySet()
                .removeIf(
                        entry ->
                                now - entry.getValue().seenAt()
                                        > 100L
                                && (
                                minecraft.player == null
                                        || !entry.getKey()
                                        .equals(
                                                minecraft.player.getUUID()
                                        )
                        )
                );
    }

    public record State(
            String head,
            String hands,
            String torso,
            String feet,
            int glassesMode,
            long seenAt
    ) {
        public String forSlot(
                int ordinal
        ) {
            return switch (ordinal) {
                case 0 -> head;
                case 1 -> hands;
                case 2 -> torso;
                case 3 -> feet;
                default -> "";
            };
        }
    }
}
