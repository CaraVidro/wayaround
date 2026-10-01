package net.caravidro.wayaround.accessory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.advancement.WayAroundAdvancements;
import net.caravidro.wayaround.content.OddityContent;
import net.caravidro.wayaround.network.AccessoryActionC2SPayload;
import net.caravidro.wayaround.network.AccessoryStateS2CPayload;
import net.caravidro.wayaround.network.TrouserPocketAnimationS2CPayload;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.nbt.CompoundTag;
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

    private static final String TROUSER_POCKET =
            "WayAroundAccessoryTrouserPocket";

    private static final String HEAD_MATERIAL =
            "WayAroundAccessoryHeadMaterial";

    private static final String HEAD_SIZE =
            "WayAroundAccessoryHeadSize";

    private static final String HEAD_EXTRAS =
            "WayAroundAccessoryHeadExtras";

    private static final String HEAD_WOOL_COLOR =
            "WayAroundAccessoryHeadWoolColor";

    private static final String CUSTOM_COLOR_PREFIX =
            "WayAroundAccessoryCustomColor_";

    private static final int POCKET_ANIMATION_TICKS =
            14;

    private static final int POCKET_HANDOFF_TICKS =
            7;

    private static final Map<UUID, Long> POCKET_RETRIEVALS =
            new HashMap<>();

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

        ItemStack incomingPocket =
                kind == AccessoryKind.ENGINEER_TROUSERS
                        ? TrouserPocketData.read(
                        held,
                        player.registryAccess()
                )
                        : ItemStack.EMPTY;

        AccessoryCustomizationData.Config incomingCustomization =
                AccessoryCustomizationData.supported(
                        kind
                )
                        ? AccessoryCustomizationData.read(
                        held,
                        kind
                )
                        : null;

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

        if (kind == AccessoryKind.ENGINEER_TROUSERS) {
            setTrouserPocket(
                    player,
                    incomingPocket
            );
        }

        WayAroundAdvancements.accessoryEquipped(
                player
        );

        if (kind == AccessoryKind.CARDBOARD_BOX) {
            WayAroundAdvancements.cardboardHead(
                    player
            );
        }

        if (kind == AccessoryKind.GAS_MASK) {
            WayAroundAdvancements.uselessGasMask(
                    player
            );
        }

        if (incomingCustomization != null) {
            if (AccessoryCustomizationData.colorOnly(
                    kind
            )) {
                setCustomColor(
                        player,
                        kind.slot(),
                        incomingCustomization.woolColor()
                );
            } else {
                setHeadCustomization(
                        player,
                        kind,
                        incomingCustomization
                );
            }
        }

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

            ItemStack incomingPocket =
                    accessory.kind()
                            == AccessoryKind.ENGINEER_TROUSERS
                            ? TrouserPocketData.read(
                            carried,
                            player.registryAccess()
                    )
                            : ItemStack.EMPTY;

            AccessoryCustomizationData.Config incomingCustomization =
                    AccessoryCustomizationData.supported(
                            accessory.kind()
                    )
                            ? AccessoryCustomizationData.read(
                            carried,
                            accessory.kind()
                    )
                            : null;

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

            if (accessory.kind()
                    == AccessoryKind.ENGINEER_TROUSERS) {
                setTrouserPocket(
                        player,
                        incomingPocket
                );
            }

            if (incomingCustomization != null) {
                if (AccessoryCustomizationData.colorOnly(
                        accessory.kind()
                )) {
                    setCustomColor(
                            player,
                            accessory.kind()
                                    .slot(),
                            incomingCustomization.woolColor()
                    );
                } else {
                    setHeadCustomization(
                            player,
                            accessory.kind(),
                            incomingCustomization
                    );
                }
            }

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

        if (kind == AccessoryKind.ENGINEER_TROUSERS) {
            TrouserPocketData.write(
                    stack,
                    trouserPocket(
                            player
                    ),
                    player.registryAccess()
            );
        }

        if (AccessoryCustomizationData.supported(
                kind
        )) {
            AccessoryCustomizationData.Config config =
                    AccessoryCustomizationData.colorOnly(
                            kind
                    )
                            ? new AccessoryCustomizationData.Config(
                            0,
                            2,
                            0,
                            customColor(
                                    player,
                                    slot
                            )
                    )
                            : headCustomization(
                            player
                    );

            AccessoryCustomizationData.write(
                    stack,
                    kind,
                    config
            );
        }

        return stack;
    }

    /**
     * Transfers the complete equipped loadout into another world-owned
     * container (currently the player corpse) without creating loose item
     * entities. Wear, glass damage, custom colors and trouser-pocket contents
     * are preserved in the returned ItemStacks.
     */
    public static List<ItemStack> takeAllEquipped(
            ServerPlayer player
    ) {
        migrateLegacySlots(
                player
        );

        List<ItemStack> result =
                new ArrayList<>();

        for (AccessorySlot slot :
                AccessorySlot.values()) {

            ItemStack stack =
                    equippedStack(
                            player,
                            slot
                    );

            if (!stack.isEmpty()) {
                result.add(
                        stack
                );
            }

            setEquipped(
                    player,
                    slot,
                    null,
                    0,
                    0
            );
        }

        sync(
                player
        );

        return List.copyOf(
                result
        );
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

            if (slot == AccessorySlot.LEGS) {
                setTrouserPocket(
                        player,
                        ItemStack.EMPTY
                );
            }

            if (slot == AccessorySlot.HEAD) {
                clearHeadCustomization(
                        player
                );
            }

            clearCustomColor(
                    player,
                    slot
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

        if (slot == AccessorySlot.LEGS
                && kind != AccessoryKind.ENGINEER_TROUSERS) {
            setTrouserPocket(
                    player,
                    ItemStack.EMPTY
            );
        }

        if (slot == AccessorySlot.HEAD
                && !AccessoryCustomizationData.supported(
                kind
        )) {
            clearHeadCustomization(
                    player
            );
        }

        if (!AccessoryCustomizationData.colorOnly(
                kind
        )) {
            clearCustomColor(
                    player,
                    slot
            );
        }
    }

    public static AccessoryCustomizationData.Config headCustomization(
            ServerPlayer player
    ) {
        AccessoryKind kind =
                equipped(
                        player,
                        AccessorySlot.HEAD
                );

        AccessoryCustomizationData.Config defaults =
                AccessoryCustomizationData.defaults(
                        kind
                );

        if (!AccessoryCustomizationData.supported(
                kind
        )) {
            return defaults;
        }

        CompoundTag data =
                player.getPersistentData();

        if (!data.contains(
                HEAD_MATERIAL
        )) {
            return defaults;
        }

        return AccessoryCustomizationData.sanitize(
                kind,
                new AccessoryCustomizationData.Config(
                        data.getInt(
                                HEAD_MATERIAL
                        ),
                        data.getInt(
                                HEAD_SIZE
                        ),
                        data.getInt(
                                HEAD_EXTRAS
                        ),
                        data.getInt(
                                HEAD_WOOL_COLOR
                        )
                )
        );
    }

    private static void setHeadCustomization(
            ServerPlayer player,
            AccessoryKind kind,
            AccessoryCustomizationData.Config config
    ) {
        if (!AccessoryCustomizationData.supported(
                kind
        )) {
            clearHeadCustomization(
                    player
            );
            return;
        }

        AccessoryCustomizationData.Config safe =
                AccessoryCustomizationData.sanitize(
                        kind,
                        config
                );

        CompoundTag data =
                player.getPersistentData();

        data.putInt(
                HEAD_MATERIAL,
                safe.material()
        );

        data.putInt(
                HEAD_SIZE,
                safe.size()
        );

        data.putInt(
                HEAD_EXTRAS,
                safe.extras()
        );

        data.putInt(
                HEAD_WOOL_COLOR,
                safe.woolColor()
        );
    }

    private static void clearHeadCustomization(
            ServerPlayer player
    ) {
        CompoundTag data =
                player.getPersistentData();

        data.remove(
                HEAD_MATERIAL
        );
        data.remove(
                HEAD_SIZE
        );
        data.remove(
                HEAD_EXTRAS
        );
        data.remove(
                HEAD_WOOL_COLOR
        );
    }

    public static int customColor(
            ServerPlayer player,
            AccessorySlot slot
    ) {
        AccessoryKind kind =
                equipped(
                        player,
                        slot
                );

        if (!AccessoryCustomizationData.colorOnly(
                kind
        )) {
            return AccessoryCustomizationData.defaults(
                    kind
            ).woolColor();
        }

        CompoundTag data =
                player.getPersistentData();

        String key =
                customColorKey(
                        slot
                );

        if (!data.contains(
                key
        )) {
            return AccessoryCustomizationData.defaults(
                    kind
            ).woolColor();
        }

        return AccessoryCustomizationData.clampWoolColor(
                data.getInt(
                        key
                )
        );
    }

    private static void setCustomColor(
            ServerPlayer player,
            AccessorySlot slot,
            int color
    ) {
        player.getPersistentData()
                .putInt(
                        customColorKey(
                                slot
                        ),
                        AccessoryCustomizationData.clampWoolColor(
                                color
                        )
                );
    }

    private static void clearCustomColor(
            ServerPlayer player,
            AccessorySlot slot
    ) {
        player.getPersistentData()
                .remove(
                        customColorKey(
                                slot
                        )
                );
    }

    public static ItemStack trouserPocket(
            ServerPlayer player
    ) {
        if (equipped(
                player,
                AccessorySlot.LEGS
        ) != AccessoryKind.ENGINEER_TROUSERS) {
            return ItemStack.EMPTY;
        }

        CompoundTag root =
                player.getPersistentData();

        if (!root.contains(
                TROUSER_POCKET
        )) {
            return ItemStack.EMPTY;
        }

        ItemStack stack =
                ItemStack.parseOptional(
                        player.registryAccess(),
                        root.getCompound(
                                TROUSER_POCKET
                        )
                );

        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack single =
                stack.copy();

        single.setCount(
                1
        );

        return single;
    }

    private static void setTrouserPocket(
            ServerPlayer player,
            ItemStack stack
    ) {
        if (stack == null
                || stack.isEmpty()) {
            player.getPersistentData()
                    .remove(
                            TROUSER_POCKET
                    );
            return;
        }

        ItemStack single =
                stack.copy();

        single.setCount(
                1
        );

        player.getPersistentData()
                .put(
                        TROUSER_POCKET,
                        single.save(
                                player.registryAccess()
                        )
                );
    }

    public static void requestPocketRetrieve(
            ServerPlayer player
    ) {
        if (!WorldFeatureRuntime.serverEnabled(
                WorldFeature.ACCESSORIES
        )
                || equipped(
                player,
                AccessorySlot.LEGS
        ) != AccessoryKind.ENGINEER_TROUSERS
                || trouserPocket(
                player
        ).isEmpty()
                || POCKET_RETRIEVALS.containsKey(
                player.getUUID()
        )) {
            return;
        }

        long handoffAt =
                player.serverLevel()
                        .getGameTime()
                        + POCKET_HANDOFF_TICKS;

        POCKET_RETRIEVALS.put(
                player.getUUID(),
                handoffAt
        );

        TrouserPocketAnimationS2CPayload payload =
                new TrouserPocketAnimationS2CPayload(
                        player.getUUID(),
                        POCKET_ANIMATION_TICKS
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

    private static void finishPocketRetrieve(
            ServerPlayer player
    ) {
        if (equipped(
                player,
                AccessorySlot.LEGS
        ) != AccessoryKind.ENGINEER_TROUSERS) {
            return;
        }

        ItemStack pocket =
                trouserPocket(
                        player
                );

        if (pocket.isEmpty()) {
            return;
        }

        ItemStack current =
                player.getMainHandItem();

        if (!current.isEmpty()) {
            player.setItemInHand(
                    InteractionHand.MAIN_HAND,
                    ItemStack.EMPTY
            );

            if (!player.getInventory()
                    .add(
                            current
                    )) {
                player.drop(
                        current,
                        false
                );
            }
        }

        setTrouserPocket(
                player,
                ItemStack.EMPTY
        );

        player.setItemInHand(
                InteractionHand.MAIN_HAND,
                pocket
        );

        sync(
                player
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

    private static String customColorKey(
            AccessorySlot slot
    ) {
        return CUSTOM_COLOR_PREFIX
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
            POCKET_RETRIEVALS.clear();
            return;
        }

        MinecraftServer server =
                event.getServer();

        long tick =
                server.getTickCount();

        for (ServerPlayer player :
                server.getPlayerList()
                        .getPlayers()) {
            Long handoff =
                    POCKET_RETRIEVALS.get(
                            player.getUUID()
                    );

            if (handoff == null) {
                continue;
            }

            if (player.serverLevel()
                    .getGameTime()
                    >= handoff) {
                POCKET_RETRIEVALS.remove(
                        player.getUUID()
                );

                finishPocketRetrieve(
                        player
                );
            }
        }

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

        int[] customColors =
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

            customColors[i] =
                    customColor(
                            player,
                            slots[i]
                    );
        }

        AccessoryCustomizationData.Config head =
                headCustomization(
                        player
                );

        AccessoryStateS2CPayload payload =
                new AccessoryStateS2CPayload(
                        player.getUUID(),
                        kinds,
                        wear,
                        glass,
                        glassesMode(
                                player
                        ),
                        trouserPocket(
                                player
                        ),
                        head.material(),
                        head.size(),
                        head.extras(),
                        head.woolColor(),
                        customColors
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
            POCKET_RETRIEVALS.remove(
                    player.getUUID()
            );

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

            String customColor =
                    customColorKey(
                            slot
                    );

            if (original.getPersistentData()
                    .contains(
                            customColor
                    )) {
                replacement.getPersistentData()
                        .putInt(
                                customColor,
                                original.getPersistentData()
                                        .getInt(
                                                customColor
                                        )
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

        if (original.getPersistentData()
                .contains(
                        TROUSER_POCKET
                )) {
            replacement.getPersistentData()
                    .put(
                            TROUSER_POCKET,
                            original.getPersistentData()
                                    .getCompound(
                                            TROUSER_POCKET
                                    )
                                    .copy()
                    );
        }

        for (String key : new String[]{
                HEAD_MATERIAL,
                HEAD_SIZE,
                HEAD_EXTRAS,
                HEAD_WOOL_COLOR
        }) {
            if (original.getPersistentData()
                    .contains(
                            key
                    )) {
                replacement.getPersistentData()
                        .putInt(
                                key,
                                original.getPersistentData()
                                        .getInt(
                                                key
                                        )
                        );
            }
        }

        POCKET_RETRIEVALS.remove(
                original.getUUID()
        );
    }
}
