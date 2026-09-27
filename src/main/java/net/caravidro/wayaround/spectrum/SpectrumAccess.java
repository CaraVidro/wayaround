package net.caravidro.wayaround.spectrum;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.cursed.TukunaManager;
import net.caravidro.wayaround.jujutsu.JujutsuManager;
import net.caravidro.wayaround.network.SpectrumUnlockS2CPayload;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Permanent Spectrum identity/access.
 *
 * Spectrums are no longer learned by merely picking up a Spectrum item.
 * Natural ownership is assigned by the Jujutsu layer before awakening, while
 * explicit replacement exists for debug/admin commands and scripted events.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class SpectrumAccess {
    private SpectrumAccess() {}

    private static final String KEY_PREFIX = "WayAroundSpectrum_";
    private static final Map<UUID, Integer> CLIENT_UNLOCKS = new HashMap<>();

    public static boolean isSpectrum(ItemStack stack, SpectrumType type) {
        return !stack.isEmpty()
                && stack.getItem() instanceof SpectrumItem spectrum
                && spectrum.spectrumType() == type;
    }

    public static boolean has(Player player, SpectrumType type) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (!WorldFeatureRuntime.serverEnabled(WorldFeature.SPECTRUMS)) return false;
            if (serverPlayer.getPersistentData().getBoolean(key(type))) return true;

            // Tukuna's spirit uses the host's unlocked powers while possessing them.
            ServerPlayer host = TukunaManager.possessedHostForSpirit(serverPlayer);
            return host != null && has(host, type);
        }

        if (!WorldFeatureRuntime.clientEnabled(WorldFeature.SPECTRUMS)) return false;
        int mask = CLIENT_UNLOCKS.getOrDefault(player.getUUID(), 0);
        return (mask & bit(type)) != 0;
    }

    public static boolean hasAny(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (!WorldFeatureRuntime.serverEnabled(WorldFeature.SPECTRUMS)) return false;
            return firstOwned(serverPlayer).isPresent();
        }

        if (!WorldFeatureRuntime.clientEnabled(WorldFeature.SPECTRUMS)) return false;
        return CLIENT_UNLOCKS.getOrDefault(player.getUUID(), 0) != 0;
    }

    /**
     * Reads the raw per-player identity without consulting the world feature
     * toggle. Used by migration/assignment code.
     */
    public static Optional<SpectrumType> firstOwned(ServerPlayer player) {
        for (SpectrumType type : SpectrumType.values()) {
            if (player.getPersistentData().getBoolean(key(type))) return Optional.of(type);
        }
        return Optional.empty();
    }

    /**
     * Kept as a compatibility entry point for old scripted callers.
     * A player now owns at most one Spectrum identity.
     */
    public static void unlock(ServerPlayer player, SpectrumType type) {
        replace(player, type);
    }

    public static void replace(ServerPlayer player, SpectrumType type) {
        if (!WorldFeatureRuntime.serverEnabled(WorldFeature.SPECTRUMS)) return;
        for (SpectrumType other : SpectrumType.values()) {
            player.getPersistentData().putBoolean(key(other), other == type);
        }
        sync(player);
    }

    public static void clearAll(ServerPlayer player) {
        for (SpectrumType type : SpectrumType.values()) {
            player.getPersistentData().putBoolean(key(type), false);
        }
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(
                player,
                new SpectrumUnlockS2CPayload(player.getUUID(), ownMask(player))
        );
    }

    public static void syncPossession(ServerPlayer host, ServerPlayer spirit) {
        int sharedMask = ownMask(host) | ownMask(spirit);
        PacketDistributor.sendToPlayer(
                spirit,
                new SpectrumUnlockS2CPayload(spirit.getUUID(), sharedMask)
        );
    }

    public static void applyClientUnlocks(UUID player, int mask) {
        CLIENT_UNLOCKS.put(player, mask);
    }

    public static void copyUnlocks(ServerPlayer original, ServerPlayer replacement) {
        for (SpectrumType type : SpectrumType.values()) {
            replacement.getPersistentData().putBoolean(
                    key(type),
                    original.getPersistentData().getBoolean(key(type))
            );
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

    @SubscribeEvent
    public static void rareHeldSpectrumAttunement(
            ServerTickEvent.Post event
    ) {
        if ((event.getServer()
                .getTickCount()
                % 20L) != 0L
                || !WorldFeatureRuntime.serverEnabled(
                WorldFeature.SPECTRUMS
        )) {
            return;
        }

        for (ServerPlayer player :
                event.getServer()
                        .getPlayerList()
                        .getPlayers()) {

            if (JujutsuManager.isAwakened(
                    player
            )) {
                continue;
            }

            SpectrumItem spectrum =
                    player.getMainHandItem()
                            .getItem()
                            instanceof SpectrumItem mainSpectrum
                            ? mainSpectrum
                            : player.getOffhandItem()
                                    .getItem()
                                    instanceof SpectrumItem offhandSpectrum
                                    ? offhandSpectrum
                                    : null;

            if (spectrum == null) {
                continue;
            }

            /*
             * Once per second, about one chance in 1200.
             * Rare enough to feel like an anomaly, but not astronomically
             * impossible during a long survival session.
             */
            if (player.getRandom()
                    .nextInt(
                            1200
                    ) != 0) {
                continue;
            }

            JujutsuManager.attuneSpectrumFromItem(
                    player,
                    spectrum.spectrumType()
            );
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original
                && event.getEntity() instanceof ServerPlayer replacement) {
            copyUnlocks(original, replacement);
        }
    }
}
