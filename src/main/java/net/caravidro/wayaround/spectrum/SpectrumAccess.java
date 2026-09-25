package net.caravidro.wayaround.spectrum;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.cursed.TukunaManager;
import net.caravidro.wayaround.network.SpectrumUnlockS2CPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Permanent Spectrum unlocks earned by picking up their relic items. */
@EventBusSubscriber(modid = WayAround.MODID)
public final class SpectrumAccess {
    private SpectrumAccess() {}

    private static final String KEY_PREFIX = "WayAroundSpectrum_";
    private static final Map<UUID, Integer> CLIENT_UNLOCKS = new HashMap<>();

    public static boolean isSpectrum(ItemStack stack, SpectrumType type) {
        return !stack.isEmpty() && stack.getItem() instanceof SpectrumItem spectrum
                && spectrum.spectrumType() == type;
    }

    public static boolean has(Player player, SpectrumType type) {
        if (player instanceof ServerPlayer serverPlayer) {
            bindHeldItems(serverPlayer);
            if (serverPlayer.getPersistentData().getBoolean(key(type))) return true;

            // Tukuna's spirit uses the host's unlocked powers while possessing them.
            ServerPlayer host = TukunaManager.possessedHostForSpirit(serverPlayer);
            return host != null && has(host, type);
        }

        int mask = CLIENT_UNLOCKS.getOrDefault(player.getUUID(), 0);
        return (mask & bit(type)) != 0 || hasStack(player, type);
    }

    public static void unlock(ServerPlayer player, SpectrumType type) {
        boolean newlyUnlocked = !player.getPersistentData().getBoolean(key(type));
        player.getPersistentData().putBoolean(key(type), true);
        consumeItems(player, type);
        if (newlyUnlocked) {
            sync(player);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Spectrum desbloqueado permanentemente: " + type.path()
            ).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    public static void sync(ServerPlayer player) {
        int mask = ownMask(player);
        PacketDistributor.sendToPlayer(player,
                new SpectrumUnlockS2CPayload(player.getUUID(), mask));
    }

    public static void syncPossession(ServerPlayer host, ServerPlayer spirit) {
        int sharedMask = ownMask(host) | ownMask(spirit);
        PacketDistributor.sendToPlayer(spirit,
                new SpectrumUnlockS2CPayload(spirit.getUUID(), sharedMask));
    }

    public static void applyClientUnlocks(UUID player, int mask) {
        CLIENT_UNLOCKS.put(player, mask);
    }

    public static void copyUnlocks(ServerPlayer original, ServerPlayer replacement) {
        for (SpectrumType type : SpectrumType.values()) {
            if (original.getPersistentData().getBoolean(key(type))) {
                replacement.getPersistentData().putBoolean(key(type), true);
            }
        }
        sync(replacement);
    }

    private static String key(SpectrumType type) {
        return KEY_PREFIX + type.path();
    }

    private static int bit(SpectrumType type) {
        return 1 << type.ordinal();
    }

    private static int ownMask(ServerPlayer player) {
        int mask = 0;
        for (SpectrumType type : SpectrumType.values()) {
            if (player.getPersistentData().getBoolean(key(type))) mask |= bit(type);
        }
        return mask;
    }

    private static boolean hasStack(Player player, SpectrumType type) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (isSpectrum(player.getInventory().getItem(slot), type)) return true;
        }
        return false;
    }

    private static void bindHeldItems(ServerPlayer player) {
        boolean changed = false;
        for (SpectrumType type : SpectrumType.values()) {
            if (player.getPersistentData().getBoolean(key(type)) || !hasStack(player, type)) continue;
            player.getPersistentData().putBoolean(key(type), true);
            consumeItems(player, type);
            changed = true;
        }
        if (changed) sync(player);
    }

    private static void consumeItems(ServerPlayer player, SpectrumType type) {
        boolean changed = false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isSpectrum(stack, type)) {
                stack.setCount(0);
                changed = true;
            }
        }
        if (changed) player.getInventory().setChanged();
    }

    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Post event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        for (SpectrumType type : SpectrumType.values()) {
            if (!isSpectrum(event.getOriginalStack(), type)) continue;
            unlock(player, type);
            event.getCurrentStack().setCount(0);
            event.getItemEntity().discard();
            return;
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        bindHeldItems(player);
        sync(player);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original
                && event.getEntity() instanceof ServerPlayer replacement) {
            copyUnlocks(original, replacement);
        }
    }
}
