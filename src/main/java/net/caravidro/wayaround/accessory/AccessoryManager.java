package net.caravidro.wayaround.accessory;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.content.OddityContent;
import net.caravidro.wayaround.network.AccessoryActionC2SPayload;
import net.caravidro.wayaround.network.AccessoryStateS2CPayload;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server-owned accessory loadout.
 *
 * The old implementation stored only a kind per slot. V1.1.1 keeps wear and
 * breakable-glass state with the equipped item as well, so taking a garment
 * off gives the same battered object back instead of a fresh copy.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class AccessoryManager {

    private static final String PREFIX =
            "WayAroundAccessory_";

    private static final String WEAR_PREFIX =
            "WayAroundAccessoryWear_";

    private static final String GLASS_PREFIX =
            "WayAroundAccessoryGlass_";

    private static final String GLASSES_MODE =
            "WayAroundAccessoryGlassesMode";

    private AccessoryManager() {
    }

    public static void equipFromHand(
            ServerPlayer player,
            InteractionHand hand,
            AccessoryItem accessory
    ) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.ACCESSORIES
        )) {
            return;
        }

        migrateLegacySlots(
                player
        );

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

        ItemStack old =
                equippedStack(
                        player,
                        kind.slot()
                );

        setEquipped(
                player,
                kind.slot(),
                kind,
                held.getDamageValue(),
                AccessoryWear.glassState(
                        held
                )
        );

        if (!player.getAbilities()
                .instabuild) {
            held.shrink(1);
        }

        giveOrDrop(
                player,
                old
        );

        sync(
                player
        );
    }

    public static void handlePanelAction(
            ServerPlayer player,
            byte slotOrdinal,
            byte action
    ) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.ACCESSORIES
        )) {
            return;
        }

        migrateLegacySlots(
                player
        );

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

            ItemStack old =
                    equippedStack(
                            player,
                            slot
                    );

            setEquipped(
                    player,
                    slot,
                    accessory.kind(),
                    carried.getDamageValue(),
                    AccessoryWear.glassState(
                            carried
                    )
            );

            player.containerMenu
                    .setCarried(
                            old
                    );

            sync(
                    player
            );

            return;
        }

        ItemStack old =
                equippedStack(
                        player,
                        slot
                );

        if (old.isEmpty()) {
            return;
        }

        setEquipped(
                player,
                slot,
                null,
                0,
                0
        );

        player.containerMenu
                .setCarried(
                        old
                );

        sync(
                player
        );
    }

    private static void toggleAccessoryMode(
            ServerPlayer player,
            AccessorySlot slot
    ) {
        AccessoryKind equipped =
                equipped(
                        player,
                        slot
                );

        if (slot != AccessorySlot.FACE
                || equipped
                != AccessoryKind.SPECTRAL_GLASSES) {
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

    public static int equippedWear(
            ServerPlayer player,
            AccessorySlot slot
    ) {
        AccessoryKind kind =
                equipped(
                        player,
                        slot
                );

        if (kind == null) {
            return 0;
        }

        return Mth.clamp(
                player.getPersistentData()
                        .getInt(
                                wearKey(
                                        slot
                                )
                        ),
                0,
                Math.max(
                        0,
                        kind.maxWear() - 1
                )
        );
    }

    public static int equippedGlass(
            ServerPlayer player,
            AccessorySlot slot
    ) {
        AccessoryKind kind =
                equipped(
                        player,
                        slot
                );

        if (kind == null
                || !kind.breakableGlass()) {
            return 0;
        }

        return Mth.clamp(
                player.getPersistentData()
                        .getInt(
                                glassKey(
                                        slot
                                )
                        ),
                0,
                2
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

    public static void damageSlot(
            ServerPlayer player,
            AccessorySlot slot,
            int amount
    ) {
        AccessoryKind kind =
                equipped(
                        player,
                        slot
                );

        if (kind == null
                || amount <= 0) {
            return;
        }

        int next =
                Mth.clamp(
                        equippedWear(
                                player,
                                slot
                        )
                                + amount,
                        0,
                        Math.max(
                                0,
                                kind.maxWear() - 1
                        )
                );

        player.getPersistentData()
                .putInt(
                        wearKey(
                                slot
                        ),
                        next
                );
    }

    public static void damageAll(
            ServerPlayer player,
            int amount
    ) {
        for (AccessorySlot slot :
                AccessorySlot.values()) {
            damageSlot(
                    player,
                    slot,
                    amount
            );
        }

        sync(
                player
        );
    }

    public static boolean damageBreakableGlass(
            ServerPlayer player,
            int severity
    ) {
        boolean changed =
                false;

        for (AccessorySlot slot :
                AccessorySlot.values()) {
            AccessoryKind kind =
                    equipped(
                            player,
                            slot
                    );

            if (kind == null
                    || !kind.breakableGlass()) {
                continue;
            }

            int old =
                    equippedGlass(
                            player,
                            slot
                    );

            int next =
                    Mth.clamp(
                            old
                                    + Math.max(
                                    1,
                                    severity
                            ),
                            0,
                            2
                    );

            if (next == old) {
                continue;
            }

            player.getPersistentData()
                    .putInt(
                            glassKey(
                                    slot
                            ),
                            next
                    );

            damageSlot(
                    player,
                    slot,
                    12
                            * Math.max(
                            1,
                            severity
                    )
            );

            changed =
                    true;
        }

        if (changed) {
            sync(
                    player
            );
        }

        return changed;
    }

    public static boolean hasRatHost(
            ServerPlayer player
    ) {
        for (AccessorySlot slot :
                AccessorySlot.values()) {
            AccessoryKind kind =
                    equipped(
                            player,
                            slot
                    );

            if (kind != null
                    && kind.ratHost()) {
                return true;
            }
        }

        return false;
    }

    public static ItemStack equippedStack(
            ServerPlayer player,
            AccessorySlot slot
    ) {
        AccessoryKind kind =
                equipped(
                        player,
                        slot
                );

        if (kind == null) {
            return ItemStack.EMPTY;
        }

        ItemStack stack =
                OddityContent.accessoryStack(
                        kind
                );

        AccessoryWear.setWear(
                stack,
                kind,
                equippedWear(
                        player,
                        slot
                )
        );

        AccessoryWear.setGlassState(
                stack,
                equippedGlass(
                        player,
                        slot
                )
        );

        return stack;
    }

    /**
     * Removes an equipped accessory without putting it into the inventory.
     * World mechanics such as the wind-blown top hat can then take ownership
     * of the exact worn ItemStack and drop it later.
     */
    public static ItemStack takeEquipped(
            ServerPlayer player,
            AccessorySlot slot
    ) {
        migrateLegacySlots(
                player
        );

        ItemStack old =
                equippedStack(
                        player,
                        slot
                );

        if (old.isEmpty()) {
            return ItemStack.EMPTY;
        }

        setEquipped(
                player,
                slot,
                null,
                0,
                0
        );

        sync(
                player
        );

        return old;
    }

    private static void setEquipped(
            ServerPlayer player,
            AccessorySlot slot,
            AccessoryKind kind,
            int wear,
            int glass
    ) {
        if (kind == null) {
            player.getPersistentData()
                    .remove(
                            key(
                                    slot
                            )
                    );

            player.getPersistentData()
                    .remove(
                            wearKey(
                                    slot
                            )
                    );

            player.getPersistentData()
                    .remove(
                            glassKey(
                                    slot
                            )
                    );

            return;
        }

        player.getPersistentData()
                .putString(
                        key(
                                slot
                        ),
                        kind.path()
                );

        player.getPersistentData()
                .putInt(
                        wearKey(
                                slot
                        ),
                        Mth.clamp(
                                wear,
                                0,
                                Math.max(
                                        0,
                                        kind.maxWear() - 1
                                )
                        )
                );

        player.getPersistentData()
                .putInt(
                        glassKey(
                                slot
                        ),
                        kind.breakableGlass()
                                ? Mth.clamp(
                                glass,
                                0,
                                2
                        )
                                : 0
                );
    }

    private static String key(
            AccessorySlot slot
    ) {
        return PREFIX
                + slot.name();
    }

    private static String wearKey(
            AccessorySlot slot
    ) {
        return WEAR_PREFIX
                + slot.name();
    }

    private static String glassKey(
            AccessorySlot slot
    ) {
        return GLASS_PREFIX
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
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.ACCESSORIES
        )) {
            return;
        }

        MinecraftServer server =
                event.getServer();

        long tick =
                server.getTickCount();

        if (tick % 200L == 0L) {
            for (ServerPlayer player :
                    server.getPlayerList()
                            .getPlayers()) {
                migrateLegacySlots(
                        player
                );

                int wear =
                        player.isOnFire()
                                ? 4
                                : player.isSprinting()
                                ? 2
                                : player.getDeltaMovement()
                                .horizontalDistanceSqr()
                                > 0.0025
                                ? 1
                                : 0;

                if (wear > 0) {
                    for (AccessorySlot slot :
                            AccessorySlot.values()) {
                        damageSlot(
                                player,
                                slot,
                                wear
                        );
                    }
                }
            }
        }

        /*
         * Changes sync immediately; this slower refresh also handles players
         * entering another player's tracking range.
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
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.ACCESSORIES
        )) {
            return;
        }

        migrateLegacySlots(
                player
        );

        AccessorySlot[] slots =
                AccessorySlot.values();

        String[] kinds =
                new String[
                        slots.length
                        ];

        int[] wear =
                new int[
                        slots.length
                        ];

        int[] glass =
                new int[
                        slots.length
                        ];

        for (int i = 0;
             i < slots.length;
             i++) {
            AccessoryKind kind =
                    equipped(
                            player,
                            slots[i]
                    );

            kinds[i] =
                    kind == null
                            ? ""
                            : kind.path();

            wear[i] =
                    equippedWear(
                            player,
                            slots[i]
                    );

            glass[i] =
                    equippedGlass(
                            player,
                            slots[i]
                    );
        }

        AccessoryStateS2CPayload payload =
                new AccessoryStateS2CPayload(
                        player.getUUID(),
                        kinds,
                        wear,
                        glass,
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

    private static void migrateLegacySlots(
            ServerPlayer player
    ) {
        AccessoryKind oldHead =
                AccessoryKind.byPath(
                        player.getPersistentData()
                                .getString(
                                        key(
                                                AccessorySlot.HEAD
                                        )
                                )
                );

        if (oldHead
                == AccessoryKind.SPECTRAL_GLASSES
                && equipped(
                player,
                AccessorySlot.FACE
        ) == null) {
            int wear =
                    player.getPersistentData()
                            .getInt(
                                    wearKey(
                                            AccessorySlot.HEAD
                                    )
                            );

            int glass =
                    player.getPersistentData()
                            .getInt(
                                    glassKey(
                                            AccessorySlot.HEAD
                                    )
                            );

            setEquipped(
                    player,
                    AccessorySlot.HEAD,
                    null,
                    0,
                    0
            );

            setEquipped(
                    player,
                    AccessorySlot.FACE,
                    oldHead,
                    wear,
                    glass
            );
        }

        AccessoryKind oldTorso =
                AccessoryKind.byPath(
                        player.getPersistentData()
                                .getString(
                                        key(
                                                AccessorySlot.TORSO
                                        )
                                )
                );

        if (oldTorso
                == AccessoryKind.ENGINEER_CAPE
                && equipped(
                player,
                AccessorySlot.BACK
        ) == null) {
            int wear =
                    player.getPersistentData()
                            .getInt(
                                    wearKey(
                                            AccessorySlot.TORSO
                                    )
                            );

            setEquipped(
                    player,
                    AccessorySlot.TORSO,
                    null,
                    0,
                    0
            );

            setEquipped(
                    player,
                    AccessorySlot.BACK,
                    oldTorso,
                    wear,
                    0
            );
        }
    }

    @SubscribeEvent
    public static void login(
            PlayerEvent.PlayerLoggedInEvent event
    ) {
        if (event.getEntity()
                instanceof ServerPlayer player) {
            migrateLegacySlots(
                    player
            );

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
            String kind =
                    original.getPersistentData()
                            .getString(
                                    key(
                                            slot
                                    )
                            );

            if (!kind.isBlank()) {
                replacement.getPersistentData()
                        .putString(
                                key(
                                        slot
                                ),
                                kind
                        );
            }

            replacement.getPersistentData()
                    .putInt(
                            wearKey(
                                    slot
                            ),
                            original.getPersistentData()
                                    .getInt(
                                            wearKey(
                                                    slot
                                            )
                                    )
                    );

            replacement.getPersistentData()
                    .putInt(
                            glassKey(
                                    slot
                            ),
                            original.getPersistentData()
                                    .getInt(
                                            glassKey(
                                                    slot
                                            )
                                    )
                    );
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
