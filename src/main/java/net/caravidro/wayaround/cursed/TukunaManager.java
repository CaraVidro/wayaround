package net.caravidro.wayaround.cursed;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.content.WayAroundContent;
import net.caravidro.wayaround.network.TukunaPossessionS2CPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Tukuna lifecycle:
 *
 * Spectrum holder -> death -> ghost -> someone eats the named finger ->
 * voice-haunts that host from anywhere -> both say "trocar" -> temporary
 * possession -> host watches first-person and cannot speak -> automatic return.
 *
 * The actual body transfer deliberately does not move inventories. The ghost
 * player becomes the temporary physical controller at the host's position,
 * while the host is server-side spectator-camerad to that controller. This
 * avoids inventory duplication while preserving the two-real-player mechanic.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class TukunaManager {

    private TukunaManager() {
    }

    public static final int MAX_FINGERS = 20;

    private static final int SWAP_CONFIRM_WINDOW_TICKS = 160;
    private static final int POSSESSION_BASE_TICKS = 400;
    private static final int POSSESSION_PER_FINGER_TICKS = 20;
    private static final int POSSESSION_NEAR_RETURN_TICKS = 100;
    private static final int POSSESSION_RETURNING_TICKS = 20;
    private static final int SWAP_COOLDOWN_TICKS = 300;

    private static final int FUGA_CHARGE_TICKS =
            80;

    private static final int FUGA_PROJECTILE_LIFE =
            140;

    private static final int FUGA_SUBSTEPS =
            7;

    private static final double FUGA_SPEED =
            3.8;

    private static final double FUGA_CRATER_RADIUS_XZ =
            18.0;

    private static final double FUGA_CRATER_RADIUS_Y =
            11.0;

    private static final String FINGER_OWNER_KEY =
            "WayAroundTukunaFingerOwner";

    private static final String FINGER_OWNER_NAME_KEY =
            "WayAroundTukunaFingerOwnerName";

    private static final String HOST_FINGER_COUNT_KEY =
            "WayAroundTukunaFingerCount";

    private static final String HOST_SPIRIT_KEY =
            "WayAroundTukunaSpirit";

    private static final String GHOST_KEY =
            "WayAroundTukunaGhost";

    private static final String FUGA_PHRASE_KEY =
            "WayAroundTukunaFugaPhrase";

    private static final String FUGA_PROMPT_KEY =
            "WayAroundTukunaFugaPhrasePrompted";

    private static final ResourceLocation HOST_ARMOR_ID =
            ResourceLocation.fromNamespaceAndPath(
                    WayAround.MODID,
                    "tukuna_host_armor"
            );

    private static final ResourceLocation HOST_DAMAGE_ID =
            ResourceLocation.fromNamespaceAndPath(
                    WayAround.MODID,
                    "tukuna_host_damage"
            );

    private static final ResourceLocation HOST_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(
                    WayAround.MODID,
                    "tukuna_host_speed"
            );

    private static final ResourceLocation POSSESSION_ARMOR_ID =
            ResourceLocation.fromNamespaceAndPath(
                    WayAround.MODID,
                    "tukuna_possession_armor"
            );

    private static final ResourceLocation POSSESSION_DAMAGE_ID =
            ResourceLocation.fromNamespaceAndPath(
                    WayAround.MODID,
                    "tukuna_possession_damage"
            );

    private static final ResourceLocation POSSESSION_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(
                    WayAround.MODID,
                    "tukuna_possession_speed"
            );

    private static final Map<UUID, Long> SWAP_CONFIRMATIONS =
            new HashMap<>();

    private static final Map<UUID, Long> SWAP_COOLDOWNS =
            new HashMap<>();

    private static final Map<UUID, Long> DESMARTELAR_COOLDOWNS =
            new HashMap<>();

    private static final Map<UUID, FugaCharge> FUGA_CHARGES =
            new HashMap<>();

    private static final List<FugaProjectile> FUGA_PROJECTILES =
            new ArrayList<>();

    private static final Map<UUID, Possession> POSSESSIONS =
            new HashMap<>();

    private static final Set<UUID> SPECTRUM_PRESENT =
            new HashSet<>();

    private static final Set<UUID> GHOST_NOTIFIED =
            new HashSet<>();

    @SubscribeEvent
    public static void onDeath(
            LivingDeathEvent event
    ) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack spectrum =
                removeSpectrum(
                        player
                );

        if (spectrum.isEmpty()) {
            return;
        }

        player.getPersistentData()
                .putBoolean(
                        GHOST_KEY,
                        true
                );

        String ownerName =
                player.getGameProfile()
                        .getName();

        /*
         * One Spectrum death creates the complete set of 20 cursed fingers.
         * They are grouped into four physical stacks to avoid spawning twenty
         * ItemEntity objects at the same coordinates.
         */
        for (int group = 0;
             group < 4;
             group++) {

            ItemStack fingers =
                    createFinger(
                            player.getUUID(),
                            ownerName
                    );

            fingers.setCount(
                    5
            );

            ItemEntity entity =
                    new ItemEntity(
                            player.serverLevel(),
                            player.getX(),
                            player.getY() + 0.45,
                            player.getZ(),
                            fingers
                    );

            double angle =
                    group
                            * Math.PI
                            * 0.5;

            entity.setDeltaMovement(
                    Math.cos(angle) * 0.18,
                    0.22 + group * 0.015,
                    Math.sin(angle) * 0.18
            );

            player.serverLevel()
                    .addFreshEntity(
                            entity
                    );
        }

        player.displayClientMessage(
                Component.translatable(
                                "message.wayaround.tukuna.spectrum_lost"
                        )
                        .withStyle(
                                ChatFormatting.DARK_RED,
                                ChatFormatting.BOLD
                        ),
                false
        );
    }

    @SubscribeEvent
    public static void onClone(
            PlayerEvent.Clone event
    ) {
        if (!(event.getOriginal()
                instanceof ServerPlayer original)
                || !(event.getEntity()
                        instanceof ServerPlayer replacement)) {
            return;
        }

        copyInt(
                original,
                replacement,
                HOST_FINGER_COUNT_KEY
        );

        copyUuid(
                original,
                replacement,
                HOST_SPIRIT_KEY
        );

        if (original.getPersistentData()
                .getBoolean(
                        GHOST_KEY
                )) {

            replacement.getPersistentData()
                    .putBoolean(
                            GHOST_KEY,
                            true
                    );
        }
    }

    @SubscribeEvent
    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        long tick =
                server.getTickCount();

        tickPossessions(
                server,
                tick
        );

        tickFugaCharges(
                server,
                tick
        );

        tickFugaProjectiles(
                server,
                tick
        );

        if (tick % 20L != 0L) {
            return;
        }

        cleanupTimers(
                tick
        );

        for (ServerPlayer player :
                server.getPlayerList()
                        .getPlayers()) {

            boolean spectrum =
                    hasSpectrum(
                            player
                    );

            if (spectrum) {
                if (SPECTRUM_PRESENT.add(
                        player.getUUID()
                )) {
                    player.displayClientMessage(
                            Component.translatable(
                                            "message.wayaround.tukuna.spectrum_warning"
                                    )
                                    .withStyle(
                                            ChatFormatting.DARK_RED,
                                            ChatFormatting.BOLD
                                    ),
                            false
                    );

                    promptFugaPhrase(
                            player
                    );
                }
            } else {
                SPECTRUM_PRESENT.remove(
                        player.getUUID()
                );
            }

            if (isGhost(player)
                    && !isPossessingSpirit(
                    player
            )) {

                removeHostBuffs(
                        player
                );

                removePossessionBuffs(
                        player
                );

                if (!player.isSpectator()) {
                    player.setGameMode(
                            GameType.SPECTATOR
                    );
                }

                if (GHOST_NOTIFIED.add(
                        player.getUUID()
                )) {
                    player.displayClientMessage(
                            Component.translatable(
                                            "message.wayaround.tukuna.ghost"
                                    )
                                    .withStyle(
                                            ChatFormatting.DARK_PURPLE,
                                            ChatFormatting.ITALIC
                                    ),
                            false
                    );
                }

                continue;
            }

            GHOST_NOTIFIED.remove(
                    player.getUUID()
            );

            if (isPossessionHost(
                    player
            )) {
                removeHostBuffs(
                        player
                );
                continue;
            }

            if (isPossessingSpirit(
                    player
            )) {
                Possession possession =
                        possessionForSpirit(
                                player.getUUID()
                        );

                if (possession != null) {
                    applyPossessionBuffs(
                            player,
                            possession.fingers
                    );
                }
                continue;
            }

            removePossessionBuffs(
                    player
            );

            int fingers =
                    fingerCount(
                            player
                    );

            if (fingers > 0) {
                applyHostBuffs(
                        player,
                        fingers
                );
            } else {
                removeHostBuffs(
                        player
                );
            }
        }
    }

    @SubscribeEvent
    public static void onChat(
            ServerChatEvent event
    ) {
        ServerPlayer player =
                event.getPlayer();

        String raw =
                event.getRawText()
                        .trim();

        if (isSilencedHost(
                player
        )) {
            event.setCanceled(
                    true
            );

            player.displayClientMessage(
                    Component.translatable(
                                    "message.wayaround.tukuna.silenced"
                            )
                            .withStyle(
                                    ChatFormatting.DARK_GRAY
                            ),
                    true
            );

            return;
        }

        if (handleSpectrumSpeech(
                player,
                raw
        )) {

            event.setCanceled(
                    true
            );

            return;
        }

        if (raw.equalsIgnoreCase(
                "trocar"
        )) {
            event.setCanceled(
                    true
            );

            confirmSwap(
                    player
            );
        }
    }

    public static void onVoiceStatement(
            ServerPlayer player,
            String transcript
    ) {
        if (transcript == null
                || transcript.isBlank()
                || isSilencedHost(
                player
        )) {
            return;
        }

        if (handleSpectrumSpeech(
                player,
                transcript
        )) {
            return;
        }

        if ("trocar".equals(
                normalizeSpeech(
                        transcript
                )
        )) {
            confirmSwap(
                    player
            );
        }
    }

    @SubscribeEvent
    public static void onServerStopped(
            ServerStoppedEvent event
    ) {
        SWAP_CONFIRMATIONS.clear();
        SWAP_COOLDOWNS.clear();
        DESMARTELAR_COOLDOWNS.clear();
        FUGA_CHARGES.clear();
        FUGA_PROJECTILES.clear();
        POSSESSIONS.clear();
        SPECTRUM_PRESENT.clear();
        GHOST_NOTIFIED.clear();
    }

    public static boolean consumeFinger(
            ServerPlayer host,
            ItemStack stack
    ) {
        UUID owner =
                fingerOwner(
                        stack
                );

        if (owner == null) {
            host.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.tukuna.finger_empty"
                    ),
                    true
            );
            return false;
        }

        if (owner.equals(
                host.getUUID()
        )) {
            host.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.tukuna.own_finger"
                    ),
                    true
            );
            return false;
        }

        UUID existing =
                spiritOwner(
                        host
                );

        if (existing != null
                && !existing.equals(
                        owner
                )) {

            host.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.tukuna.other_spirit"
                    ),
                    true
            );
            return false;
        }

        ServerPlayer existingHost =
                hostForSpirit(
                        host.server,
                        owner
                );

        if (existingHost != null
                && existingHost != host) {

            host.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.tukuna.spirit_taken",
                            existingHost.getGameProfile()
                                    .getName()
                    ),
                    true
            );
            return false;
        }

        int current =
                fingerCount(
                        host
                );

        if (current >= MAX_FINGERS) {
            host.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.tukuna.max_fingers"
                    ),
                    true
            );
            return false;
        }

        host.getPersistentData()
                .putUUID(
                        HOST_SPIRIT_KEY,
                        owner
                );

        int next =
                Math.min(
                        MAX_FINGERS,
                        current + 1
                );

        host.getPersistentData()
                .putInt(
                        HOST_FINGER_COUNT_KEY,
                        next
                );

        String spiritName =
                fingerOwnerName(
                        stack
                );

        host.displayClientMessage(
                Component.translatable(
                                "message.wayaround.tukuna.finger_eaten",
                                next,
                                MAX_FINGERS,
                                spiritName.isBlank()
                                        ? owner.toString()
                                                .substring(
                                                        0,
                                                        8
                                                )
                                        : spiritName
                        )
                        .withStyle(
                                ChatFormatting.DARK_RED
                        ),
                false
        );

        ServerPlayer spirit =
                host.server
                        .getPlayerList()
                        .getPlayer(
                                owner
                        );

        if (spirit != null
                && isGhost(
                spirit
        )) {
            spirit.displayClientMessage(
                    Component.translatable(
                                    "message.wayaround.tukuna.host_found",
                                    host.getGameProfile()
                                            .getName(),
                                    next
                            )
                            .withStyle(
                                    ChatFormatting.DARK_PURPLE
                            ),
                    false
            );
        }

        applyHostBuffs(
                host,
                next
        );

        return true;
    }

    public static ItemStack createFinger(
            UUID owner,
            String ownerName
    ) {
        ItemStack stack =
                new ItemStack(
                        WayAroundContent.TUKUNA_FINGER.get()
                );

        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> {
                    tag.putUUID(
                            FINGER_OWNER_KEY,
                            owner
                    );

                    tag.putString(
                            FINGER_OWNER_NAME_KEY,
                            ownerName
                    );
                }
        );

        stack.set(
                DataComponents.CUSTOM_NAME,
                Component.literal(
                                ownerName
                                        + "'s Finger"
                        )
                        .withStyle(
                                ChatFormatting.DARK_RED
                        )
        );

        return stack;
    }

    public static UUID fingerOwner(
            ItemStack stack
    ) {
        CustomData data =
                stack.get(
                        DataComponents.CUSTOM_DATA
                );

        if (data == null) {
            return null;
        }

        CompoundTag tag =
                data.copyTag();

        if (!tag.hasUUID(
                FINGER_OWNER_KEY
        )) {
            return null;
        }

        return tag.getUUID(
                FINGER_OWNER_KEY
        );
    }

    public static String fingerOwnerName(
            ItemStack stack
    ) {
        CustomData data =
                stack.get(
                        DataComponents.CUSTOM_DATA
                );

        if (data == null) {
            return "";
        }

        return data.copyTag()
                .getString(
                        FINGER_OWNER_NAME_KEY
                );
    }

    public static int fingerCount(
            ServerPlayer player
    ) {
        return Math.max(
                0,
                player.getPersistentData()
                        .getInt(
                                HOST_FINGER_COUNT_KEY
                        )
        );
    }

    public static UUID spiritOwner(
            ServerPlayer player
    ) {
        if (!player.getPersistentData()
                .hasUUID(
                        HOST_SPIRIT_KEY
                )) {
            return null;
        }

        return player.getPersistentData()
                .getUUID(
                        HOST_SPIRIT_KEY
                );
    }

    public static boolean isGhost(
            ServerPlayer player
    ) {
        return player.getPersistentData()
                .getBoolean(
                        GHOST_KEY
                );
    }

    public static boolean isSilencedHost(
            ServerPlayer player
    ) {
        return POSSESSIONS.containsKey(
                player.getUUID()
        );
    }

    public static boolean isPossessionHost(
            ServerPlayer player
    ) {
        return POSSESSIONS.containsKey(
                player.getUUID()
        );
    }

    public static boolean isPossessingSpirit(
            ServerPlayer player
    ) {
        return possessionForSpirit(
                player.getUUID()
        ) != null;
    }

    /**
     * When a disembodied Tukuna speaks, project their mic around the host's
     * body instead of around the ghost's spectator coordinates. This works
     * across dimensions because VoiceServer sends the audio to the host level.
     */
    public static ServerPlayer projectedVoiceHost(
            ServerPlayer spirit
    ) {
        if (!isGhost(spirit)
                || isPossessingSpirit(
                spirit
        )) {
            return null;
        }

        return hostForSpirit(
                spirit.server,
                spirit.getUUID()
        );
    }

    public static ServerPlayer hostForSpirit(
            MinecraftServer server,
            UUID spiritId
    ) {
        for (ServerPlayer player :
                server.getPlayerList()
                        .getPlayers()) {

            UUID owner =
                    spiritOwner(
                            player
                    );

            if (spiritId.equals(
                    owner
            )
                    && fingerCount(
                    player
            ) > 0) {
                return player;
            }
        }

        return null;
    }

    public static void confirmSwap(
            ServerPlayer caller
    ) {
        MinecraftServer server =
                caller.server;

        long tick =
                server.getTickCount();

        ServerPlayer host;
        ServerPlayer spirit;

        if (isGhost(caller)) {
            spirit =
                    caller;

            host =
                    hostForSpirit(
                            server,
                            caller.getUUID()
                    );

        } else {
            host =
                    caller;

            UUID spiritId =
                    spiritOwner(
                            host
                    );

            spirit =
                    spiritId == null
                            ? null
                            : server.getPlayerList()
                                    .getPlayer(
                                            spiritId
                                    );
        }

        if (host == null
                || spirit == null
                || !isGhost(spirit)
                || fingerCount(host) <= 0) {

            caller.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.tukuna.swap_no_pair"
                    ),
                    true
            );
            return;
        }

        if (POSSESSIONS.containsKey(
                host.getUUID()
        )) {
            caller.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.tukuna.swap_already"
                    ),
                    true
            );
            return;
        }

        long cooldownUntil =
                Math.max(
                        SWAP_COOLDOWNS.getOrDefault(
                                host.getUUID(),
                                0L
                        ),
                        SWAP_COOLDOWNS.getOrDefault(
                                spirit.getUUID(),
                                0L
                        )
                );

        if (tick < cooldownUntil) {
            long seconds =
                    Math.max(
                            1L,
                            (cooldownUntil - tick + 19L)
                                    / 20L
                    );

            caller.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.tukuna.swap_cooldown",
                            seconds
                    ),
                    true
            );
            return;
        }

        SWAP_CONFIRMATIONS.put(
                caller.getUUID(),
                tick
        );

        ServerPlayer other =
                caller == host
                        ? spirit
                        : host;

        caller.displayClientMessage(
                Component.translatable(
                                "message.wayaround.tukuna.swap_confirmed_self"
                        )
                        .withStyle(
                                ChatFormatting.DARK_RED
                        ),
                true
        );

        other.displayClientMessage(
                Component.translatable(
                                "message.wayaround.tukuna.swap_waiting_other",
                                caller.getGameProfile()
                                        .getName()
                        )
                        .withStyle(
                                ChatFormatting.DARK_PURPLE
                        ),
                true
        );

        long otherTick =
                SWAP_CONFIRMATIONS.getOrDefault(
                        other.getUUID(),
                        Long.MIN_VALUE / 4L
                );

        if (tick - otherTick
                <= SWAP_CONFIRM_WINDOW_TICKS) {

            SWAP_CONFIRMATIONS.remove(
                    host.getUUID()
            );

            SWAP_CONFIRMATIONS.remove(
                    spirit.getUUID()
            );

            beginPossession(
                    host,
                    spirit,
                    tick
            );
        }
    }

    public static void castPossessedDesmartelar(
            ServerPlayer player
    ) {
        castPossessedDesmartelar(
                player,
                false
        );
    }

    public static void castPossessedDesmartelar(
            ServerPlayer player,
            boolean fire
    ) {
        long tick =
                player.server
                        .getTickCount();

        int fingers;

        if (hasSpectrum(player)) {
            fingers =
                    MAX_FINGERS;

        } else {
            Possession possession =
                    possessionForSpirit(
                            player.getUUID()
                    );

            if (possession == null) {
                return;
            }

            fingers =
                    possession.fingers;
        }

        long until =
                DESMARTELAR_COOLDOWNS.getOrDefault(
                        player.getUUID(),
                        0L
                );

        if (tick < until) {
            return;
        }

        DESMARTELAR_COOLDOWNS.put(
                player.getUUID(),
                tick + 28L
        );

        Desmartelar.cast(
                player,
                fingers,
                fire
        );
    }

    public static boolean prepareFuga(
            ServerPlayer player
    ) {
        if (!hasSpectrum(
                player
        )) {
            return false;
        }

        long tick =
                player.server
                        .getTickCount();

        FugaCharge existing =
                FUGA_CHARGES.get(
                        player.getUUID()
                );

        if (existing != null) {
            return true;
        }

        FUGA_CHARGES.put(
                player.getUUID(),
                new FugaCharge(
                        player.getUUID(),
                        tick,
                        tick + FUGA_CHARGE_TICKS
                )
        );

        player.serverLevel()
                .playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.BLAZE_AMBIENT,
                        SoundSource.PLAYERS,
                        1.05F,
                        0.48F
                );

        player.displayClientMessage(
                Component.literal(
                        "Tukuna: Fuga começou a carregar..."
                ).withStyle(
                        ChatFormatting.GOLD
                ),
                true
        );

        return true;
    }

    public static boolean launchFuga(
            ServerPlayer player
    ) {
        FugaCharge charge =
                FUGA_CHARGES.get(
                        player.getUUID()
                );

        if (charge == null) {
            return false;
        }

        long tick =
                player.server
                        .getTickCount();

        if (tick < charge.readyAt) {
            player.displayClientMessage(
                    Component.literal(
                            "Fuga ainda está carregando."
                    ).withStyle(
                            ChatFormatting.RED
                    ),
                    true
            );

            return false;
        }

        FUGA_CHARGES.remove(
                player.getUUID()
        );

        Vec3 direction =
                player.getLookAngle()
                        .normalize();

        Vec3 position =
                player.getEyePosition()
                        .add(
                                direction.scale(
                                        1.4
                                )
                        );

        FUGA_PROJECTILES.add(
                new FugaProjectile(
                        player.getUUID(),
                        player.serverLevel()
                                .dimension(),
                        position,
                        direction.scale(
                                FUGA_SPEED
                        ),
                        FUGA_PROJECTILE_LIFE
                )
        );

        player.serverLevel()
                .playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.BLAZE_SHOOT,
                        SoundSource.PLAYERS,
                        2.0F,
                        0.42F
                );

        player.serverLevel()
                .sendParticles(
                        ParticleTypes.FLAME,
                        position.x,
                        position.y,
                        position.z,
                        48,
                        0.28,
                        0.28,
                        0.28,
                        0.12
                );

        return true;
    }

    private static void promptFugaPhrase(
            ServerPlayer player
    ) {
        if (!player.getPersistentData()
                .getString(
                        FUGA_PHRASE_KEY
                )
                .isBlank()) {
            return;
        }

        if (player.getPersistentData()
                .getBoolean(
                        FUGA_PROMPT_KEY
                )) {
            return;
        }

        player.getPersistentData()
                .putBoolean(
                        FUGA_PROMPT_KEY,
                        true
                );

        player.sendSystemMessage(
                Component.literal(
                        "Tukuna: escolha sua frase de preparação com: frase de fuga: <sua frase>"
                ).withStyle(
                        ChatFormatting.DARK_RED
                )
        );
    }

    private static boolean handleSpectrumSpeech(
            ServerPlayer player,
            String raw
    ) {
        if (!hasSpectrum(
                player
        )) {
            return false;
        }

        String normalized =
                normalizeSpeech(
                        raw
                );

        if (normalized.startsWith(
                "frase de fuga "
        )) {

            String phrase =
                    normalized.substring(
                            "frase de fuga ".length()
                    )
                            .trim();

            if (phrase.length() < 3
                    || phrase.contains(
                    "fuga"
            )) {

                player.displayClientMessage(
                        Component.literal(
                                "Escolha uma frase com pelo menos 3 caracteres e sem usar a palavra 'fuga'."
                        ).withStyle(
                                ChatFormatting.RED
                        ),
                        true
                );

                return true;
            }

            player.getPersistentData()
                    .putString(
                            FUGA_PHRASE_KEY,
                            phrase
                    );

            player.displayClientMessage(
                    Component.literal(
                            "Frase da Fuga definida: \""
                                    + phrase
                                    + "\""
                    ).withStyle(
                            ChatFormatting.GOLD
                    ),
                    false
            );

            return true;
        }

        String phrase =
                player.getPersistentData()
                        .getString(
                                FUGA_PHRASE_KEY
                        );

        if (!phrase.isBlank()
                && normalized.contains(
                phrase
        )) {

            prepareFuga(
                    player
            );

            return true;
        }

        if ("fuga".equals(
                normalized
        )
                || normalized.endsWith(
                " fuga"
        )) {

            launchFuga(
                    player
            );

            return true;
        }

        if (normalized.startsWith(
                "desmartelar"
        )
                || normalized.startsWith(
                "desmantelar"
        )) {

            castPossessedDesmartelar(
                    player,
                    normalized.contains(
                            "fogo"
                    )
            );

            return true;
        }

        return false;
    }

    private static String normalizeSpeech(
            String text
    ) {
        return Normalizer.normalize(
                text.toLowerCase(
                        Locale.ROOT
                ),
                Normalizer.Form.NFD
        ).replaceAll(
                "\\p{M}+",
                ""
        ).replaceAll(
                "[^a-z0-9 ]",
                " "
        ).replaceAll(
                "\\s+",
                " "
        ).trim();
    }

    private static void tickFugaCharges(
            MinecraftServer server,
            long tick
    ) {
        Iterator<Map.Entry<UUID, FugaCharge>> iterator =
                FUGA_CHARGES.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            FugaCharge charge =
                    iterator.next()
                            .getValue();

            ServerPlayer player =
                    server.getPlayerList()
                            .getPlayer(
                                    charge.owner
                            );

            if (player == null
                    || !player.isAlive()
                    || !hasSpectrum(
                    player
            )) {

                iterator.remove();
                continue;
            }

            double progress =
                    Mth.clamp(
                            (
                                    tick
                                            - charge.startedAt
                            )
                                    / (double) FUGA_CHARGE_TICKS,
                            0.0,
                            1.0
                    );

            Vec3 center =
                    player.getEyePosition()
                            .add(
                                    player.getLookAngle()
                                            .normalize()
                                            .scale(
                                                    0.55
                                            )
                            );

            if (tick % 2L == 0L) {
                player.serverLevel()
                        .sendParticles(
                                progress >= 1.0
                                        ? ParticleTypes.SOUL_FIRE_FLAME
                                        : ParticleTypes.FLAME,
                                center.x,
                                center.y,
                                center.z,
                                progress >= 1.0
                                        ? 8
                                        : 3,
                                0.22 + progress * 0.22,
                                0.22,
                                0.22 + progress * 0.22,
                                0.025
                        );
            }

            if (tick % 20L == 0L) {
                player.serverLevel()
                        .playSound(
                                null,
                                player.blockPosition(),
                                SoundEvents.FIRE_AMBIENT,
                                SoundSource.PLAYERS,
                                0.45F
                                        + (float) progress
                                                * 0.75F,
                                0.48F
                                        + (float) progress
                                                * 0.45F
                        );
            }

            if (!charge.readyAnnounced
                    && tick >= charge.readyAt) {

                charge.readyAnnounced =
                        true;

                player.displayClientMessage(
                        Component.literal(
                                "Fuga pronta. Diga: fuga"
                        ).withStyle(
                                ChatFormatting.GOLD,
                                ChatFormatting.BOLD
                        ),
                        true
                );

                player.serverLevel()
                        .playSound(
                                null,
                                player.blockPosition(),
                                SoundEvents.BEACON_POWER_SELECT,
                                SoundSource.PLAYERS,
                                1.2F,
                                0.62F
                        );
            }
        }
    }

    private static void tickFugaProjectiles(
            MinecraftServer server,
            long tick
    ) {
        Iterator<FugaProjectile> iterator =
                FUGA_PROJECTILES.iterator();

        while (iterator.hasNext()) {
            FugaProjectile projectile =
                    iterator.next();

            ServerLevel level =
                    server.getLevel(
                            projectile.dimension
                    );

            ServerPlayer owner =
                    server.getPlayerList()
                            .getPlayer(
                                    projectile.owner
                            );

            if (level == null
                    || owner == null) {

                iterator.remove();
                continue;
            }

            projectile.life--;

            boolean collide =
                    projectile.life <= 0;

            Vec3 step =
                    projectile.velocity.scale(
                            1.0
                                    / FUGA_SUBSTEPS
                    );

            for (int sub = 0;
                 sub < FUGA_SUBSTEPS
                        && !collide;
                 sub++) {

                projectile.position =
                        projectile.position.add(
                                step
                        );

                BlockPos pos =
                        BlockPos.containing(
                                projectile.position
                        );

                BlockState state =
                        level.getBlockState(
                                pos
                        );

                if (!state.isAir()) {
                    collide =
                            true;

                    break;
                }

                AABB hitbox =
                        new AABB(
                                projectile.position.x - 0.72,
                                projectile.position.y - 0.72,
                                projectile.position.z - 0.72,
                                projectile.position.x + 0.72,
                                projectile.position.y + 0.72,
                                projectile.position.z + 0.72
                        );

                if (!level.getEntitiesOfClass(
                        LivingEntity.class,
                        hitbox,
                        entity ->
                                entity.isAlive()
                                        && entity != owner
                ).isEmpty()) {

                    collide =
                            true;
                }
            }

            level.sendParticles(
                    ParticleTypes.FLAME,
                    projectile.position.x,
                    projectile.position.y,
                    projectile.position.z,
                    12,
                    0.16,
                    0.16,
                    0.16,
                    0.035
            );

            level.sendParticles(
                    ParticleTypes.END_ROD,
                    projectile.position.x,
                    projectile.position.y,
                    projectile.position.z,
                    3,
                    0.08,
                    0.08,
                    0.08,
                    0.012
            );

            if (collide) {
                detonateFuga(
                        level,
                        owner,
                        projectile.position
                );

                iterator.remove();
            }
        }
    }

    private static void detonateFuga(
            ServerLevel level,
            ServerPlayer owner,
            Vec3 center
    ) {
        pulverizeFugaCrater(
                level,
                center
        );

        level.explode(
                owner,
                center.x,
                center.y,
                center.z,
                16.0F,
                true,
                Level.ExplosionInteraction.TNT
        );

        double damageRadius =
                30.0;

        AABB area =
                new AABB(
                        center.x - damageRadius,
                        center.y - damageRadius,
                        center.z - damageRadius,
                        center.x + damageRadius,
                        center.y + damageRadius,
                        center.z + damageRadius
                );

        for (LivingEntity living :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        area,
                        entity ->
                                entity.isAlive()
                                        && entity != owner
                )) {

            double distance =
                    living.position()
                            .distanceTo(
                                    center
                            );

            if (distance > damageRadius) {
                continue;
            }

            double factor =
                    1.0
                            - distance
                                    / damageRadius;

            living.hurt(
                    owner.damageSources()
                            .playerAttack(
                                    owner
                            ),
                    (float) (
                            18.0
                                    + factor
                                            * factor
                                            * 62.0
                    )
            );

            living.igniteForSeconds(
                    18.0F
            );
        }

        for (int y = 0;
             y <= 120;
             y += 2) {

            double width =
                    0.9
                            + y
                                    * 0.018;

            level.sendParticles(
                    y % 8 == 0
                            ? ParticleTypes.END_ROD
                            : ParticleTypes.FLAME,
                    center.x,
                    center.y + y,
                    center.z,
                    8,
                    width,
                    0.9,
                    width,
                    0.035
            );
        }

        for (int i = 0;
             i < 260;
             i++) {

            double theta =
                    level.random.nextDouble()
                            * Math.PI
                            * 2.0;

            double speed =
                    0.8
                            + level.random.nextDouble()
                                    * 3.0;

            level.sendParticles(
                    i % 3 == 0
                            ? ParticleTypes.SOUL_FIRE_FLAME
                            : ParticleTypes.FLAME,
                    center.x,
                    center.y + 1.0,
                    center.z,
                    0,
                    Math.cos(theta) * speed,
                    0.4
                            + level.random.nextDouble()
                                    * 2.6,
                    Math.sin(theta) * speed,
                    1.0
            );
        }

        level.playSound(
                null,
                BlockPos.containing(
                        center
                ),
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS,
                4.0F,
                0.34F
        );
    }

    private static void pulverizeFugaCrater(
            ServerLevel level,
            Vec3 center
    ) {
        int radiusX =
                Mth.ceil(
                        FUGA_CRATER_RADIUS_XZ
                );

        int radiusY =
                Mth.ceil(
                        FUGA_CRATER_RADIUS_Y
                );

        int particleBudget =
                0;

        for (int x = -radiusX;
             x <= radiusX;
             x++) {

            for (int y = -radiusY;
                 y <= radiusY;
                 y++) {

                for (int z = -radiusX;
                     z <= radiusX;
                     z++) {

                    double normalized =
                            (
                                    x * x
                                            + z * z
                            )
                                    / (
                                    FUGA_CRATER_RADIUS_XZ
                                            * FUGA_CRATER_RADIUS_XZ
                            )
                                    + y * y
                                            / (
                                            FUGA_CRATER_RADIUS_Y
                                                    * FUGA_CRATER_RADIUS_Y
                                    );

                    if (normalized > 1.0) {
                        continue;
                    }

                    BlockPos pos =
                            BlockPos.containing(
                                    center.x + x,
                                    center.y + y,
                                    center.z + z
                            );

                    BlockState state =
                            level.getBlockState(
                                    pos
                            );

                    if (state.isAir()
                            || state.getDestroySpeed(
                            level,
                            pos
                    ) < 0.0F) {
                        continue;
                    }

                    level.setBlock(
                            pos,
                            Blocks.AIR
                                    .defaultBlockState(),
                            2
                    );

                    if (particleBudget < 150
                            && level.random.nextFloat()
                            < 0.035F) {

                        Vec3 blockCenter =
                                Vec3.atCenterOf(
                                        pos
                                );

                        level.sendParticles(
                                new BlockParticleOption(
                                        ParticleTypes.BLOCK,
                                        state
                                ),
                                blockCenter.x,
                                blockCenter.y,
                                blockCenter.z,
                                2,
                                0.35,
                                0.35,
                                0.35,
                                0.24
                        );

                        particleBudget++;
                    }
                }
            }
        }

        for (int attempt = 0;
             attempt < 180;
             attempt++) {

            double angle =
                    level.random.nextDouble()
                            * Math.PI
                            * 2.0;

            double radius =
                    FUGA_CRATER_RADIUS_XZ
                            * (
                            0.65
                                    + level.random.nextDouble()
                                            * 0.75
                    );

            int x =
                    Mth.floor(
                            center.x
                                    + Math.cos(
                                    angle
                            )
                                    * radius
                    );

            int z =
                    Mth.floor(
                            center.z
                                    + Math.sin(
                                    angle
                            )
                                    * radius
                    );

            int y =
                    level.getHeight(
                            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            x,
                            z
                    );

            BlockPos firePos =
                    new BlockPos(
                            x,
                            y,
                            z
                    );

            BlockState fire =
                    Blocks.FIRE
                            .defaultBlockState();

            if (level.getBlockState(
                    firePos
            ).isAir()
                    && fire.canSurvive(
                    level,
                    firePos
            )) {

                level.setBlock(
                        firePos,
                        fire,
                        3
                );
            }
        }
    }

    private static void beginPossession(
            ServerPlayer host,
            ServerPlayer spirit,
            long tick
    ) {
        int fingers =
                fingerCount(
                        host
                );

        GameType hostMode =
                host.gameMode
                        .getGameModeForPlayer();

        long endTick =
                tick
                        + POSSESSION_BASE_TICKS
                        + fingers
                                * POSSESSION_PER_FINGER_TICKS;

        Possession possession =
                new Possession(
                        host.getUUID(),
                        spirit.getUUID(),
                        fingers,
                        hostMode,
                        endTick
                );

        POSSESSIONS.put(
                host.getUUID(),
                possession
        );

        host.setGameMode(
                GameType.SPECTATOR
        );

        teleportTo(
                spirit,
                host.serverLevel(),
                host.position(),
                host.getYRot(),
                host.getXRot()
        );

        spirit.setGameMode(
                GameType.SURVIVAL
        );

        host.setCamera(
                spirit
        );

        applyPossessionBuffs(
                spirit,
                fingers
        );

        removeHostBuffs(
                host
        );

        PacketDistributor.sendToPlayer(
                host,
                new TukunaPossessionS2CPayload(
                        true
                )
        );

        host.displayClientMessage(
                Component.translatable(
                                "message.wayaround.tukuna.possession_host",
                                spirit.getGameProfile()
                                        .getName()
                        )
                        .withStyle(
                                ChatFormatting.DARK_RED,
                                ChatFormatting.BOLD
                        ),
                false
        );

        spirit.displayClientMessage(
                Component.translatable(
                                "message.wayaround.tukuna.possession_spirit",
                                host.getGameProfile()
                                        .getName(),
                                fingers
                        )
                        .withStyle(
                                ChatFormatting.DARK_PURPLE,
                                ChatFormatting.BOLD
                        ),
                false
        );
    }

    private static void tickPossessions(
            MinecraftServer server,
            long tick
    ) {
        if (POSSESSIONS.isEmpty()) {
            return;
        }

        var iterator =
                POSSESSIONS.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Possession> entry =
                    iterator.next();

            Possession possession =
                    entry.getValue();

            ServerPlayer host =
                    server.getPlayerList()
                            .getPlayer(
                                    possession.hostId
                            );

            ServerPlayer spirit =
                    server.getPlayerList()
                            .getPlayer(
                                    possession.spiritId
                            );

            if (host == null
                    || spirit == null
                    || !spirit.isAlive()) {

                if (host != null) {
                    emergencyRestoreHost(
                            host
                    );
                }

                if (spirit != null) {
                    spirit.setGameMode(
                            GameType.SPECTATOR
                    );

                    removePossessionBuffs(
                            spirit
                    );
                }

                iterator.remove();
                continue;
            }

            if (!host.isSpectator()) {
                host.setGameMode(
                        GameType.SPECTATOR
                );
            }

            host.setCamera(
                    spirit
            );

            long remaining =
                    possession.endTick
                            - tick;

            if (remaining
                    <= POSSESSION_RETURNING_TICKS
                    && !possession.returningWarned) {

                possession.returningWarned =
                        true;

                Component returning =
                        Component.translatable(
                                        "message.wayaround.tukuna.returning"
                                )
                                .withStyle(
                                        ChatFormatting.RED,
                                        ChatFormatting.BOLD
                                );

                host.displayClientMessage(
                        returning,
                        true
                );

                spirit.displayClientMessage(
                        returning,
                        true
                );

            } else if (remaining
                    <= POSSESSION_NEAR_RETURN_TICKS
                    && !possession.nearWarned) {

                possession.nearWarned =
                        true;

                Component near =
                        Component.translatable(
                                        "message.wayaround.tukuna.near_return"
                                )
                                .withStyle(
                                        ChatFormatting.GOLD
                                );

                host.displayClientMessage(
                        near,
                        true
                );

                spirit.displayClientMessage(
                        near,
                        true
                );
            }

            if (remaining <= 0L) {
                finishPossession(
                        host,
                        spirit,
                        possession,
                        tick
                );

                iterator.remove();
            }
        }
    }

    private static void finishPossession(
            ServerPlayer host,
            ServerPlayer spirit,
            Possession possession,
            long tick
    ) {
        Vec3 returnPosition =
                spirit.position();

        ServerLevel returnLevel =
                spirit.serverLevel();

        float yaw =
                spirit.getYRot();

        float pitch =
                spirit.getXRot();

        host.setCamera(
                host
        );

        teleportTo(
                host,
                returnLevel,
                returnPosition,
                yaw,
                pitch
        );

        host.setGameMode(
                possession.hostMode
        );

        spirit.setGameMode(
                GameType.SPECTATOR
        );

        removePossessionBuffs(
                spirit
        );

        applyHostBuffs(
                host,
                possession.fingers
        );

        long cooldownUntil =
                tick
                        + SWAP_COOLDOWN_TICKS;

        SWAP_COOLDOWNS.put(
                host.getUUID(),
                cooldownUntil
        );

        SWAP_COOLDOWNS.put(
                spirit.getUUID(),
                cooldownUntil
        );

        PacketDistributor.sendToPlayer(
                host,
                new TukunaPossessionS2CPayload(
                        false
                )
        );

        Component returned =
                Component.translatable(
                                "message.wayaround.tukuna.returned"
                        )
                        .withStyle(
                                ChatFormatting.DARK_GRAY
                        );

        host.displayClientMessage(
                returned,
                false
        );

        spirit.displayClientMessage(
                returned,
                false
        );
    }

    private static void emergencyRestoreHost(
            ServerPlayer host
    ) {
        host.setCamera(
                host
        );

        host.setGameMode(
                GameType.SURVIVAL
        );

        PacketDistributor.sendToPlayer(
                host,
                new TukunaPossessionS2CPayload(
                        false
                )
        );
    }

    private static void teleportTo(
            ServerPlayer player,
            ServerLevel level,
            Vec3 position,
            float yaw,
            float pitch
    ) {
        player.teleportTo(
                level,
                position.x,
                position.y,
                position.z,
                Set.<RelativeMovement>of(),
                yaw,
                pitch
        );
    }

    private static void applyHostBuffs(
            ServerPlayer player,
            int fingers
    ) {
        removePossessionBuffs(
                player
        );

        double armor =
                0.25
                        + fingers
                                * 0.035;

        double damage =
                0.10
                        + fingers
                                * 0.025;

        double speed =
                0.010
                        + fingers
                                * 0.0015;

        updateModifier(
                player.getAttribute(
                        Attributes.ARMOR
                ),
                HOST_ARMOR_ID,
                armor,
                AttributeModifier.Operation.ADD_VALUE
        );

        updateModifier(
                player.getAttribute(
                        Attributes.ATTACK_DAMAGE
                ),
                HOST_DAMAGE_ID,
                damage,
                AttributeModifier.Operation.ADD_VALUE
        );

        updateModifier(
                player.getAttribute(
                        Attributes.MOVEMENT_SPEED
                ),
                HOST_SPEED_ID,
                speed,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    private static void applyPossessionBuffs(
            ServerPlayer player,
            int fingers
    ) {
        removeHostBuffs(
                player
        );

        double armor =
                1.50
                        + fingers
                                * 0.18;

        double damage =
                0.75
                        + fingers
                                * 0.13;

        double speed =
                0.050
                        + fingers
                                * 0.008;

        updateModifier(
                player.getAttribute(
                        Attributes.ARMOR
                ),
                POSSESSION_ARMOR_ID,
                armor,
                AttributeModifier.Operation.ADD_VALUE
        );

        updateModifier(
                player.getAttribute(
                        Attributes.ATTACK_DAMAGE
                ),
                POSSESSION_DAMAGE_ID,
                damage,
                AttributeModifier.Operation.ADD_VALUE
        );

        updateModifier(
                player.getAttribute(
                        Attributes.MOVEMENT_SPEED
                ),
                POSSESSION_SPEED_ID,
                speed,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    private static void updateModifier(
            AttributeInstance instance,
            ResourceLocation id,
            double amount,
            AttributeModifier.Operation operation
    ) {
        if (instance == null) {
            return;
        }

        instance.addOrUpdateTransientModifier(
                new AttributeModifier(
                        id,
                        amount,
                        operation
                )
        );
    }

    private static void removeHostBuffs(
            ServerPlayer player
    ) {
        removeModifier(
                player.getAttribute(
                        Attributes.ARMOR
                ),
                HOST_ARMOR_ID
        );

        removeModifier(
                player.getAttribute(
                        Attributes.ATTACK_DAMAGE
                ),
                HOST_DAMAGE_ID
        );

        removeModifier(
                player.getAttribute(
                        Attributes.MOVEMENT_SPEED
                ),
                HOST_SPEED_ID
        );
    }

    private static void removePossessionBuffs(
            ServerPlayer player
    ) {
        removeModifier(
                player.getAttribute(
                        Attributes.ARMOR
                ),
                POSSESSION_ARMOR_ID
        );

        removeModifier(
                player.getAttribute(
                        Attributes.ATTACK_DAMAGE
                ),
                POSSESSION_DAMAGE_ID
        );

        removeModifier(
                player.getAttribute(
                        Attributes.MOVEMENT_SPEED
                ),
                POSSESSION_SPEED_ID
        );
    }

    private static void removeModifier(
            AttributeInstance instance,
            ResourceLocation id
    ) {
        if (instance != null
                && instance.hasModifier(
                id
        )) {
            instance.removeModifier(
                    id
            );
        }
    }

    private static Possession possessionForSpirit(
            UUID spiritId
    ) {
        for (Possession possession :
                POSSESSIONS.values()) {

            if (possession.spiritId.equals(
                    spiritId
            )) {
                return possession;
            }
        }

        return null;
    }

    private static boolean hasSpectrum(
            ServerPlayer player
    ) {
        for (int slot = 0;
             slot < player.getInventory()
                     .getContainerSize();
             slot++) {

            if (player.getInventory()
                    .getItem(
                            slot
                    )
                    .is(
                            WayAroundContent.TUKUNA_SPECTRUM.get()
                    )) {
                return true;
            }
        }

        return false;
    }

    private static ItemStack removeSpectrum(
            ServerPlayer player
    ) {
        for (int slot = 0;
             slot < player.getInventory()
                     .getContainerSize();
             slot++) {

            ItemStack stack =
                    player.getInventory()
                            .getItem(
                                    slot
                            );

            if (!stack.is(
                    WayAroundContent.TUKUNA_SPECTRUM.get()
            )) {
                continue;
            }

            ItemStack result =
                    stack.copyWithCount(
                            1
                    );

            stack.shrink(
                    1
            );

            player.getInventory()
                    .setChanged();

            return result;
        }

        return ItemStack.EMPTY;
    }

    private static void copyInt(
            ServerPlayer original,
            ServerPlayer replacement,
            String key
    ) {
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

    private static void copyUuid(
            ServerPlayer original,
            ServerPlayer replacement,
            String key
    ) {
        if (original.getPersistentData()
                .hasUUID(
                        key
                )) {
            replacement.getPersistentData()
                    .putUUID(
                            key,
                            original.getPersistentData()
                                    .getUUID(
                                            key
                                    )
                    );
        }
    }

    private static void cleanupTimers(
            long tick
    ) {
        SWAP_CONFIRMATIONS.entrySet()
                .removeIf(
                        entry ->
                                tick - entry.getValue()
                                        > SWAP_CONFIRM_WINDOW_TICKS
                );

        SWAP_COOLDOWNS.entrySet()
                .removeIf(
                        entry ->
                                entry.getValue()
                                        <= tick
                );

        DESMARTELAR_COOLDOWNS.entrySet()
                .removeIf(
                        entry ->
                                entry.getValue()
                                        <= tick
                );
    }

    private static final class FugaCharge {
        private final UUID owner;
        private final long startedAt;
        private final long readyAt;
        private boolean readyAnnounced;

        private FugaCharge(
                UUID owner,
                long startedAt,
                long readyAt
        ) {
            this.owner = owner;
            this.startedAt = startedAt;
            this.readyAt = readyAt;
        }
    }

    private static final class FugaProjectile {
        private final UUID owner;
        private final net.minecraft.resources.ResourceKey<Level> dimension;
        private Vec3 position;
        private final Vec3 velocity;
        private int life;

        private FugaProjectile(
                UUID owner,
                net.minecraft.resources.ResourceKey<Level> dimension,
                Vec3 position,
                Vec3 velocity,
                int life
        ) {
            this.owner = owner;
            this.dimension = dimension;
            this.position = position;
            this.velocity = velocity;
            this.life = life;
        }
    }

    private static final class Possession {
        private final UUID hostId;
        private final UUID spiritId;
        private final int fingers;
        private final GameType hostMode;
        private final long endTick;

        private boolean nearWarned;
        private boolean returningWarned;

        private Possession(
                UUID hostId,
                UUID spiritId,
                int fingers,
                GameType hostMode,
                long endTick
        ) {
            this.hostId =
                    hostId;

            this.spiritId =
                    spiritId;

            this.fingers =
                    fingers;

            this.hostMode =
                    hostMode;

            this.endTick =
                    endTick;
        }
    }
}
