package net.caravidro.wayaround.accessory;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.content.OddityContent;
import net.caravidro.wayaround.network.AccessoryActionC2SPayload;
import net.caravidro.wayaround.network.AccessoryStateS2CPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server-side accessory equipment state.
 *
 * Accessories are intentionally cosmetic by default. This manager only owns
 * equip/unequip, visual modes and multiplayer synchronization; gameplay buffs
 * must be explicit features of an individual accessory instead of being baked
 * into the slot system.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class AccessoryManager {

    private AccessoryManager() {}

    private static final String PREFIX =
            "WayAroundAccessory_";

    private static final String GLASSES_MODE =
            "WayAroundAccessoryGlassesMode";

    public static void equipFromHand(
            ServerPlayer player,
            InteractionHand hand,
            AccessoryItem accessory
    ) {
        ItemStack held =
                player.getItemInHand(
                        hand
                );

        if (held.isEmpty()
                || held.getItem()
                        != accessory) {
            return;
        }

        AccessoryKind kind =
                accessory.kind();

        AccessoryKind old =
                equipped(
                        player,
                        kind.slot()
                );

        setEquipped(
                player,
                kind.slot(),
                kind
        );

        if (!player.getAbilities()
                .instabuild) {
            held.shrink(
                    1
            );
        }

        if (old != null) {
            giveOrDrop(
                    player,
                    OddityContent.accessoryStack(
                            old
                    )
            );
        }

        sync(
                player
        );
    }

    public static void handlePanelAction(
            ServerPlayer player,
            byte slotOrdinal,
            byte action
    ) {
        AccessorySlot slot =
                AccessorySlot.byOrdinal(
                        slotOrdinal
                );

        if (slot == null) {
            return;
        }

        if (action
                == AccessoryActionC2SPayload.TOGGLE) {
            toggleAccessoryMode(
                    player,
                    slot
            );
            return;
        }

        ItemStack carried =
                player.containerMenu
                        .getCarried();

        if (!carried.isEmpty()) {
            if (!(carried.getItem()
                    instanceof AccessoryItem accessory)
                    || accessory.kind()
                    .slot()
                    != slot) {
                return;
            }

            AccessoryKind old =
                    equipped(
                            player,
                            slot
                    );

            setEquipped(
                    player,
                    slot,
                    accessory.kind()
            );

            player.containerMenu
                    .setCarried(
                            old == null
                                    ? ItemStack.EMPTY
                                    : OddityContent.accessoryStack(
                                    old
                            )
                    );

            sync(
                    player
            );

            return;
        }

        AccessoryKind old =
                equipped(
                        player,
                        slot
                );

        if (old == null) {
            return;
        }

        setEquipped(
                player,
                slot,
                null
        );

        player.containerMenu
                .setCarried(
                        OddityContent.accessoryStack(
                                old
                        )
                );

        sync(
                player
        );
    }

    private static void toggleAccessoryMode(
            ServerPlayer player,
            AccessorySlot slot
    ) {
        if (slot != AccessorySlot.HEAD
                || equipped(
                player,
                slot
        ) != AccessoryKind.SPECTRAL_GLASSES) {
            return;
        }

        player.getPersistentData()
                .putInt(
                        GLASSES_MODE,
                        glassesMode(
                                player
                        ) == 0
                                ? 1
                                : 0
                );

        sync(
                player
        );
    }

    public static AccessoryKind equipped(
            ServerPlayer player,
            AccessorySlot slot
    ) {
        return AccessoryKind.byPath(
                player.getPersistentData()
                        .getString(
                                key(
                                        slot
                                )
                        )
        );
    }

    public static int glassesMode(
            ServerPlayer player
    ) {
        return Math.floorMod(
                player.getPersistentData()
                        .getInt(
                                GLASSES_MODE
                        ),
                2
        );
    }

    private static void setEquipped(
            ServerPlayer player,
            AccessorySlot slot,
            AccessoryKind kind
    ) {
        String key =
                key(
                        slot
                );

        if (kind == null) {
            player.getPersistentData()
                    .remove(
                            key
                    );

            return;
        }

        player.getPersistentData()
                .putString(
                        key,
                        kind.path()
                );
    }

    private static String key(
            AccessorySlot slot
    ) {
        return PREFIX
                + slot.name();
    }

    private static void giveOrDrop(
            ServerPlayer player,
            ItemStack stack
    ) {
        if (stack.isEmpty()) {
            return;
        }

        if (!player.getInventory()
                .add(
                        stack
                )) {
            player.drop(
                    stack,
                    false
            );
        }
    }

    @SubscribeEvent
    public static void tick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        long tick =
                server.getTickCount();

        /*
         * Changes sync immediately; this slow refresh exists only so players
         * entering another player's tracking range receive the cosmetic state.
         */
        if (tick % 40L != 0L) {
            return;
        }

        for (ServerPlayer player :
                server.getPlayerList()
                        .getPlayers()) {
            sync(
                    player
            );
        }
    }

    public static void sync(
            ServerPlayer player
    ) {
        AccessoryStateS2CPayload payload =
                new AccessoryStateS2CPayload(
                        player.getUUID(),
                        path(
                                equipped(
                                        player,
                                        AccessorySlot.HEAD
                                )
                        ),
                        path(
                                equipped(
                                        player,
                                        AccessorySlot.HANDS
                                )
                        ),
                        path(
                                equipped(
                                        player,
                                        AccessorySlot.TORSO
                                )
                        ),
                        path(
                                equipped(
                                        player,
                                        AccessorySlot.FEET
                                )
                        ),
                        glassesMode(
                                player
                        )
                );

        PacketDistributor.sendToPlayersNear(
                player.serverLevel(),
                player,
                player.getX(),
                player.getY(),
                player.getZ(),
                160.0,
                payload
        );

        PacketDistributor.sendToPlayer(
                player,
                payload
        );
    }

    private static String path(
            AccessoryKind kind
    ) {
        return kind == null
                ? ""
                : kind.path();
    }

    @SubscribeEvent
    public static void login(
            PlayerEvent.PlayerLoggedInEvent event
    ) {
        if (event.getEntity()
                instanceof ServerPlayer player) {
            sync(
                    player
            );
        }
    }

    @SubscribeEvent
    public static void clone(
            PlayerEvent.Clone event
    ) {
        if (!(event.getOriginal()
                instanceof ServerPlayer original)
                || !(event.getEntity()
                instanceof ServerPlayer replacement)) {
            return;
        }

        for (AccessorySlot slot :
                AccessorySlot.values()) {
            String value =
                    original.getPersistentData()
                            .getString(
                                    key(
                                            slot
                                    )
                            );

            if (!value.isBlank()) {
                replacement.getPersistentData()
                        .putString(
                                key(
                                        slot
                                ),
                                value
                        );
            }
        }

        replacement.getPersistentData()
                .putInt(
                        GLASSES_MODE,
                        original.getPersistentData()
                                .getInt(
                                        GLASSES_MODE
                                )
                );
    }
}
