package net.caravidro.wayaround.client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.accessory.AccessoryKind;
import net.caravidro.wayaround.accessory.AccessorySlot;
import net.caravidro.wayaround.accessory.AccessoryWear;
import net.caravidro.wayaround.network.AccessoryStateS2CPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(
        modid = net.caravidro.wayaround.WayAround.MODID,
        value = Dist.CLIENT
)
public final class AccessoryClientState {

    private static final Map<UUID, State> STATES =
            new HashMap<>();

    private AccessoryClientState() {
    }

    public static void receive(
            AccessoryStateS2CPayload payload
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        long seen =
                minecraft.level == null
                        ? 0L
                        : minecraft.level.getGameTime();

        int count =
                AccessorySlot.values()
                        .length;

        String[] kinds =
                new String[
                        count
                        ];

        int[] wear =
                new int[
                        count
                        ];

        int[] glass =
                new int[
                        count
                        ];

        Arrays.fill(
                kinds,
                ""
        );

        System.arraycopy(
                payload.kinds(),
                0,
                kinds,
                0,
                Math.min(
                        count,
                        payload.kinds().length
                )
        );

        System.arraycopy(
                payload.wear(),
                0,
                wear,
                0,
                Math.min(
                        count,
                        payload.wear().length
                )
        );

        System.arraycopy(
                payload.glass(),
                0,
                glass,
                0,
                Math.min(
                        count,
                        payload.glass().length
                )
        );

        STATES.put(
                payload.player(),
                new State(
                        kinds,
                        wear,
                        glass,
                        payload.glassesMode(),
                        payload.trouserPocket() == null
                                ? ItemStack.EMPTY
                                : payload.trouserPocket()
                                .copy(),
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
                                now - entry.getValue()
                                        .seenAt()
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
            String[] kinds,
            int[] wear,
            int[] glass,
            int glassesMode,
            ItemStack trouserPocket,
            long seenAt
    ) {
        public String path(
                AccessorySlot slot
        ) {
            int ordinal =
                    slot.ordinal();

            return ordinal >= 0
                    && ordinal < kinds.length
                    ? kinds[ordinal]
                    : "";
        }

        public AccessoryKind kind(
                AccessorySlot slot
        ) {
            return AccessoryKind.byPath(
                    path(
                            slot
                    )
            );
        }

        public int wear(
                AccessorySlot slot
        ) {
            int ordinal =
                    slot.ordinal();

            return ordinal >= 0
                    && ordinal < wear.length
                    ? wear[ordinal]
                    : 0;
        }

        public int wearStage(
                AccessorySlot slot
        ) {
            return AccessoryWear.stage(
                    kind(
                            slot
                    ),
                    wear(
                            slot
                    )
            );
        }

        public int glass(
                AccessorySlot slot
        ) {
            int ordinal =
                    slot.ordinal();

            return ordinal >= 0
                    && ordinal < glass.length
                    ? glass[ordinal]
                    : 0;
        }

        public boolean empty() {
            for (String kind : kinds) {
                if (kind != null
                        && !kind.isBlank()) {
                    return false;
                }
            }

            return true;
        }
    }
}
