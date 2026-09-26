package net.caravidro.wayaround.cursed;

import net.caravidro.wayaround.cinematic.PlayerControlLockManager;

import net.caravidro.wayaround.network.PlayerCinematicPayload;

import net.caravidro.wayaround.spectrum.SpectrumType;

import net.caravidro.wayaround.spectrum.SpectrumAccess;

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
import net.caravidro.wayaround.network.TukunaPossessionVisualS2CPayload;
import net.caravidro.wayaround.network.TukunaViewS2CPayload;
import net.caravidro.wayaround.network.TukunaMarkS2CPayload;
import net.caravidro.wayaround.network.TukunaFugaVisualPayload;
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
 * Possession keeps exactly one moving physical player: Tukuna. The host is
 * frozen as a spectator watching that controller, while clients render the
 * controller with the receptacle's skin/model and Tukuna markings. This avoids
 * per-tick body following/interpolation while preserving the two-real-player
 * mechanic and native Minecraft input.
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
    private static final int DANGEROUS_CONTRACT_TICKS = 20 * 60 * 3;
    private static final int TAKEOVER_TICKS = 80;
    private static final int RETURN_TICKS = 60;
    private static final Map<UUID, ReturnVisual> RETURN_VISUALS = new HashMap<>();
    private static final Set<UUID> DEBUG_SELF_POSSESSIONS = new HashSet<>();
    private static final int PROPOSAL_TICKS = 20 * 60;
    private static final String PACT_SPIRIT_KEY = "WayAroundTukunaPactSpirit";
    private static final String PACT_DURATION_KEY = "WayAroundPactDurationSeconds";
    private static final String PACT_PACIFIST_KEY = "WayAroundPactPacifist";
    private static final String PACT_FORGET_KEY = "WayAroundPactForget";
    private static final String PACT_WORD_KEY = "WayAroundTukunaPactWord";
    private static final String PACT_INDEFINITE_KEY = "WayAroundTukunaPactIndefinite";

    private static final int FUGA_CHARGE_TICKS =
            120;

    private static final int FUGA_PROJECTILE_LIFE =
            140;

    private static final int FUGA_SUBSTEPS =
            7;

    private static final double FUGA_SPEED =
            3.8;

    private static final double FUGA_CRATER_RADIUS_XZ =
            30.0;

    private static final double FUGA_CRATER_RADIUS_Y =
            18.0;

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
    private static final String SELF_FINGER_COUNT_KEY = "WayAroundTukunaSelfFingerCount";
    private static final String BODY_RECLAIMED_KEY = "WayAroundTukunaBodyReclaimed";
    private static final String FORMER_RECEPTACLE_KEY = "WayAroundTukunaFormerReceptacle";
    private static final String FORCED_TAKEOVER_PENDING_KEY = "WayAroundTukunaForcedTakeoverPending";
    private static final int FORCED_POSSESSION_TICKS = 20 * 60 * 5;
    private static final int FORCED_FEED_TICKS = 32;

    private static final String FUGA_PHRASE_KEY =
            "WayAroundTukunaFugaPhrase";

    private static final String FUGA_PROMPT_KEY =
            "WayAroundTukunaFugaPhrasePrompted";

    private static final String FUGA_UNLOCK_MIGRATION_KEY =
            "WayAroundTukunaFugaUnlockV2";

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
    private static final Map<UUID, Long> CONTRACT_CONFIRMATIONS =
            new HashMap<>();

    private static final Map<UUID, Long> SWAP_COOLDOWNS =
            new HashMap<>();

    private static final Map<UUID, Long> DESMARTELAR_COOLDOWNS =
            new HashMap<>();

    private static final Map<UUID, Long> MANUAL_DESMARTELAR_CHARGES =
            new HashMap<>();

    private static final Map<UUID, FugaCharge> FUGA_CHARGES =
            new HashMap<>();

    private static final List<FugaProjectile> FUGA_PROJECTILES =
            new ArrayList<>();

    private static final Map<UUID, Possession> POSSESSIONS =
            new HashMap<>();
    private static final Map<UUID, UUID> VIEW_HOSTS = new HashMap<>();
    private static final Map<UUID, ForcedFeed> FORCED_FEEDS = new HashMap<>();
    private static final Map<UUID, PactProposal> PACT_PROPOSALS = new HashMap<>();
    private static final Map<UUID, PendingTakeover> PENDING_TAKEOVERS = new HashMap<>();

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

        if (!hasSpectrum(player) || isGhost(player)) {
            return;
        }

        player.getPersistentData()
                .putBoolean(
                        GHOST_KEY,
                        true
                );

        if (player.getPersistentData().getBoolean(BODY_RECLAIMED_KEY)) {
            player.getPersistentData().putBoolean(BODY_RECLAIMED_KEY, false);
            player.getPersistentData().putInt(SELF_FINGER_COUNT_KEY, 0);
        }
        VIEW_HOSTS.remove(player.getUUID());

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

            TukunaFingerWorld.protect(entity);

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
                Component.literal("Seu Spectrum continua ligado a você. Agora você existe como espírito de Tukuna.")
                        .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD),
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
        copyInt(original, replacement, SELF_FINGER_COUNT_KEY);
        for (String key : new String[]{BODY_RECLAIMED_KEY, FORMER_RECEPTACLE_KEY, FORCED_TAKEOVER_PENDING_KEY}) {
            if (original.getPersistentData().contains(key)) {
                replacement.getPersistentData().putBoolean(key, original.getPersistentData().getBoolean(key));
            }
        }

        copyUuid(
                original,
                replacement,
                HOST_SPIRIT_KEY
        );
        copyUuid(original, replacement, PACT_SPIRIT_KEY);
        for (String key : new String[]{PACT_DURATION_KEY, PACT_PACIFIST_KEY, PACT_FORGET_KEY, PACT_INDEFINITE_KEY}) {
            if (original.getPersistentData().contains(key)) {
                replacement.getPersistentData().put(key, original.getPersistentData().get(key).copy());
            }
        }
        if (original.getPersistentData().contains(PACT_WORD_KEY)) {
            replacement.getPersistentData().putString(PACT_WORD_KEY,
                    original.getPersistentData().getString(PACT_WORD_KEY));
        }

        if (original.getPersistentData().contains(FUGA_PHRASE_KEY)) {
            replacement.getPersistentData().putString(FUGA_PHRASE_KEY,
                    original.getPersistentData().getString(FUGA_PHRASE_KEY));
        }
        replacement.getPersistentData().putBoolean(FUGA_PROMPT_KEY,
                original.getPersistentData().getBoolean(FUGA_PROMPT_KEY));
        replacement.getPersistentData().putBoolean(FUGA_UNLOCK_MIGRATION_KEY,
                original.getPersistentData().getBoolean(FUGA_UNLOCK_MIGRATION_KEY));

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
        tickTakeovers(server, tick);
        tickReturnVisuals(server, tick);
        tickForcedFeeds(server, tick);
        DEBUG_SELF_POSSESSIONS.removeIf(id -> {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null || !player.isAlive()) return true;
            if (tick % 10L == 0L) emitIndefiniteAura(player, tick, false);
            return false;
        });

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

                ensureFugaUnlockMigrated(
                        player
                );
            } else {
                SPECTRUM_PRESENT.remove(
                        player.getUUID()
                );
            }

            if (isGhost(player)
                    && !isPossessingSpirit(
                    player
            )) {
                removeHostBuffs(player);
                removePossessionBuffs(player);
                if (!player.isSpectator()) player.setGameMode(GameType.SPECTATOR);

                UUID previousView = VIEW_HOSTS.get(player.getUUID());
                ServerPlayer viewedHost = hostForSpirit(server, player.getUUID());
                if (viewedHost != null) {
                    VIEW_HOSTS.put(player.getUUID(), viewedHost.getUUID());
                    player.setCamera(viewedHost);
                    if (!viewedHost.getUUID().equals(previousView)) {
                        PacketDistributor.sendToPlayer(player, new TukunaViewS2CPayload(true, 0));
                        player.displayClientMessage(Component.literal(
                                "Sua presença se ancora em um receptáculo. Você enxerga pelos olhos dele."
                        ).withStyle(ChatFormatting.DARK_PURPLE), false);
                    }
                } else {
                    VIEW_HOSTS.remove(player.getUUID());
                    player.setCamera(player);
                    if (previousView != null) {
                        PacketDistributor.sendToPlayer(player, new TukunaViewS2CPayload(false, 0));
                    }
                    if (GHOST_NOTIFIED.add(player.getUUID())) {
                        player.displayClientMessage(Component.translatable(
                                "message.wayaround.tukuna.ghost"
                        ).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), false);
                    }
                }
                continue;
            }

            GHOST_NOTIFIED.remove(
                    player.getUUID()
            );

            if (player.getPersistentData().getBoolean(FORCED_TAKEOVER_PENDING_KEY)) {
                tryStartForcedTakeover(player);
            }

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

            int selfFingers = selfFingerCount(player);
            if (hasSpectrum(player) && selfFingers > 0) {
                applyPossessionBuffs(player, effectiveTukunaFingers(player));
                continue;
            }

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

        if ("corpo".equals(normalizeSpeech(raw)) && tryReclaimBody(player)) {
            event.setCanceled(true);
            return;
        }

        if (handlePactSpeech(player, raw)) {
            event.setCanceled(true);
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

        String statement = normalizeSpeech(raw);
        if ("contrato de 3 minutos".equals(statement)
                || "contrato de tres minutos".equals(statement)) {
            event.setCanceled(true);
            confirmSwap(player, true);
            return;
        }
        if (isManualReturnStatement(statement)) {
            event.setCanceled(true);
            returnBodyEarly(player);
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
                || PlayerControlLockManager.actionsLocked(
                player
        )
                || isSilencedHost(
                player
        )) {
            return;
        }

        if ("corpo".equals(normalizeSpeech(transcript)) && tryReclaimBody(player)) {
            return;
        }

        if (handlePactSpeech(player, transcript)) {
            return;
        }

        if (handleSpectrumSpeech(
                player,
                transcript
        )) {
            return;
        }

        String statement = normalizeSpeech(transcript);
        if ("contrato de 3 minutos".equals(statement)
                || "contrato de tres minutos".equals(statement)) {
            confirmSwap(player, true);
            return;
        }
        if (isManualReturnStatement(statement)) {
            returnBodyEarly(player);
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
        CONTRACT_CONFIRMATIONS.clear();
        SWAP_COOLDOWNS.clear();
        DESMARTELAR_COOLDOWNS.clear();
        MANUAL_DESMARTELAR_CHARGES.clear();
        FUGA_CHARGES.clear();
        FUGA_PROJECTILES.clear();
        POSSESSIONS.clear();
        VIEW_HOSTS.clear();
        FORCED_FEEDS.clear();
        PACT_PROPOSALS.clear();
        PENDING_TAKEOVERS.clear();
        RETURN_VISUALS.clear();
        DEBUG_SELF_POSSESSIONS.clear();
        SPECTRUM_PRESENT.clear();
        GHOST_NOTIFIED.clear();
    }

    public static int debugSelfPossession(
            ServerPlayer player
    ) {
        UUID id =
                player.getUUID();

        DEBUG_SELF_POSSESSIONS.add(id);
        RETURN_VISUALS.remove(id);
        POSSESSIONS.remove(id);
        PENDING_TAKEOVERS.remove(id);
        SWAP_CONFIRMATIONS.remove(id);
        CONTRACT_CONFIRMATIONS.remove(id);
        FUGA_CHARGES.remove(id);

        player.getPersistentData()
                .putBoolean(
                        GHOST_KEY,
                        false
                );

        player.getPersistentData()
                .putInt(
                        HOST_FINGER_COUNT_KEY,
                        19
                );
        player.getPersistentData().putInt(SELF_FINGER_COUNT_KEY, 19);

        player.getPersistentData()
                .putInt(
                        PACT_DURATION_KEY,
                        -1
                );

        player.getPersistentData()
                .putBoolean(
                        PACT_INDEFINITE_KEY,
                        true
                );

        player.getPersistentData()
                .putBoolean(
                        PACT_PACIFIST_KEY,
                        false
                );

        player.getPersistentData()
                .remove(
                        FUGA_PHRASE_KEY
                );

        player.getPersistentData()
                .remove(
                        FUGA_PROMPT_KEY
                );

        player.getPersistentData()
                .putBoolean(
                        FUGA_UNLOCK_MIGRATION_KEY,
                        true
                );

        SpectrumAccess.unlock(
                player,
                SpectrumType.TUKUNA
        );

        player.setGameMode(
                GameType.SURVIVAL
        );

        removeHostBuffs(
                player
        );

        removePossessionBuffs(
                player
        );

        applyPossessionBuffs(
                player,
                19
        );

        sendFugaCinematic(player, PlayerCinematicPayload.TUKUNA_TAKEOVER,
                TAKEOVER_TICKS, true, 0.0F);
        emitIndefiniteAura(player, player.server.getTickCount(), true);

        PacketDistributor.sendToPlayer(
                player,
                new TukunaPossessionS2CPayload(
                        true,
                        true,
                        true
                )
        );

        promptFugaPhrase(
                player
        );

        player.sendSystemMessage(
                Component.literal(
                        "[Tukuna TEST] Auto-posse local ativada: 19 dedos | duração indeterminada | In My Way=ON | Fuga=destravada."
                ).withStyle(
                        ChatFormatting.GOLD,
                        ChatFormatting.BOLD
                )
        );

        player.sendSystemMessage(
                Component.literal(
                        "[Tukuna TEST] Se a música não tocar, use /tukuna_test sound. Para encerrar: /tukuna_test stop."
                ).withStyle(
                        ChatFormatting.GRAY
                )
        );

        return 1;
    }

    public static int debugSound(
            ServerPlayer player
    ) {
        PacketDistributor.sendToPlayer(
                player,
                new TukunaPossessionS2CPayload(
                        false,
                        true,
                        true
                )
        );

        player.sendSystemMessage(
                Component.literal(
                        "[Tukuna TEST] Payload de In My Way enviado diretamente ao cliente."
                ).withStyle(
                        ChatFormatting.LIGHT_PURPLE
                )
        );

        return 1;
    }

    public static int debugStatus(
            ServerPlayer player
    ) {
        String phrase =
                player.getPersistentData()
                        .getString(
                                FUGA_PHRASE_KEY
                        );

        boolean prompted =
                player.getPersistentData()
                        .getBoolean(
                                FUGA_PROMPT_KEY
                        );

        boolean migrated =
                player.getPersistentData()
                        .getBoolean(
                                FUGA_UNLOCK_MIGRATION_KEY
                        );

        boolean indefinite =
                player.getPersistentData()
                        .getBoolean(
                                PACT_INDEFINITE_KEY
                        )
                        || player.getPersistentData()
                                .getInt(
                                        PACT_DURATION_KEY
                                ) < 0;

        player.sendSystemMessage(
                Component.literal(
                        "[Tukuna TEST] fingers="
                                + effectiveTukunaFingers(player)
                                + " | indefinite="
                                + indefinite
                                + " | spectrum="
                                + hasSpectrum(player)
                                + " | fugaPrompted="
                                + prompted
                                + " | fugaMigrated="
                                + migrated
                                + " | fugaWord="
                                + (phrase.isBlank() ? "<nenhuma>" : phrase)
                ).withStyle(
                        ChatFormatting.AQUA
                )
        );

        return 1;
    }

    public static int debugStop(
            ServerPlayer player
    ) {
        if (DEBUG_SELF_POSSESSIONS.remove(player.getUUID())) {
            beginReturnVisual(player, true, player.server.getTickCount());
        }
        PacketDistributor.sendToPlayer(
                player,
                new TukunaPossessionS2CPayload(
                        false,
                        false,
                        false
                )
        );

        FUGA_CHARGES.remove(
                player.getUUID()
        );

        removePossessionBuffs(
                player
        );

        player.sendSystemMessage(
                Component.literal(
                        "[Tukuna TEST] Auto-posse local e música encerradas."
                ).withStyle(
                        ChatFormatting.GRAY
                )
        );

        return 1;
    }

    public static boolean consumeFinger(
            ServerPlayer host,
            ItemStack stack
    ) {
        UUID owner = fingerOwner(stack);
        if (owner == null) {
            host.displayClientMessage(Component.translatable("message.wayaround.tukuna.finger_empty"), true);
            return false;
        }

        String ownerName = fingerOwnerName(stack);
        if (ownerName.isBlank()) ownerName = owner.toString().substring(0, 8);

        if (owner.equals(host.getUUID())) return consumeOwnFinger(host);

        if (isFormerReceptacle(host)) {
            host.displayClientMessage(Component.literal(
                    "Seu corpo já aprendeu a rejeitar uma nova presença."
            ).withStyle(ChatFormatting.DARK_GRAY), true);
            return false;
        }

        UUID existing = spiritOwner(host);
        if (existing != null && !existing.equals(owner)) {
            host.displayClientMessage(Component.translatable("message.wayaround.tukuna.other_spirit"), true);
            return false;
        }

        int current = fingerCount(host);
        if (current >= MAX_FINGERS) {
            host.displayClientMessage(Component.translatable("message.wayaround.tukuna.max_fingers"), true);
            return false;
        }

        // No global host rejection: one spirit may now own many receptacles.
        host.getPersistentData().putUUID(HOST_SPIRIT_KEY, owner);
        int next = Math.min(MAX_FINGERS, current + 1);
        host.getPersistentData().putInt(HOST_FINGER_COUNT_KEY, next);
        host.displayClientMessage(receptacleProgressMessage(next, ownerName), false);

        ServerPlayer spirit = host.server.getPlayerList().getPlayer(owner);
        if (spirit != null && isGhost(spirit)) {
            spirit.displayClientMessage(Component.literal(
                    "Um dos seus dedos encontrou um receptáculo em " + host.getGameProfile().getName() + "."
            ).withStyle(ChatFormatting.DARK_PURPLE), false);

            if (current <= 10 && next > 10) {
                spirit.getPersistentData().remove(FUGA_PHRASE_KEY);
                spirit.getPersistentData().remove(FUGA_PROMPT_KEY);
                spirit.getPersistentData().putBoolean(FUGA_UNLOCK_MIGRATION_KEY, true);
                promptFugaPhrase(spirit);
            }
        }

        applyHostBuffs(host, next);
        applyReceptacleMilestone(host, next);
        return true;
    }

    private static boolean consumeOwnFinger(ServerPlayer tukuna) {
        if (!hasSpectrum(tukuna) || (isGhost(tukuna) && !isPossessingSpirit(tukuna))) {
            tukuna.displayClientMessage(Component.literal(
                    "Sem um corpo sob seu controle, o dedo atravessa sua presença."
            ).withStyle(ChatFormatting.DARK_PURPLE), true);
            return false;
        }

        int current = selfFingerCount(tukuna);
        if (current >= MAX_FINGERS) {
            tukuna.displayClientMessage(Component.literal(
                    "Sua força já carrega todos os fragmentos."
            ).withStyle(ChatFormatting.DARK_RED), true);
            return false;
        }

        int next = Math.min(MAX_FINGERS, current + 1);
        tukuna.getPersistentData().putInt(SELF_FINGER_COUNT_KEY, next);
        applyPossessionBuffs(tukuna, effectiveTukunaFingers(tukuna));

        String text = switch (next) {
            case 1 -> "Você reconhece o próprio dedo. A força volta como se nunca tivesse ido embora.";
            case 5 -> "Sua própria energia começa a responder mais rápido que seus movimentos.";
            case 10 -> "Grande parte da presença perdida já voltou para você.";
            case 15 -> "O corpo que você controla mal consegue conter sua pressão.";
            case 20 -> "Todos os seus fragmentos obedecem novamente.";
            default -> "Outro fragmento retorna. Sua presença fica mais pesada.";
        };
        tukuna.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.DARK_RED), false);
        return true;
    }

    private static Component receptacleProgressMessage(int stage, String ownerName) {
        String text = switch (stage) {
            case 1 -> "Você comeu o dedo de " + ownerName + ". Você se sente mais forte, mas com uma consequência.";
            case 2 -> "A força cresce. Por um instante, sua respiração parece pertencer a outra pessoa.";
            case 3 -> "Algo acompanha seus movimentos um pouco tarde demais.";
            case 4 -> "Sua sombra parece pesada. A sensação passa quando você tenta encará-la.";
            case 5 -> "Sua visão perde o foco. Seu corpo abaixa a cabeça sozinho — mas o controle ainda é seu.";
            case 6 -> "A presença volta ao silêncio, porém agora você sabe que ela está ouvindo.";
            case 7 -> "Seus músculos respondem melhor. Seus pensamentos, nem tanto.";
            case 8 -> "Um arrepio atravessa o corpo sem motivo aparente.";
            case 9 -> "Por um segundo, você esquece quem iniciou o último movimento.";
            case 10 -> "Seu coração dispara. A visão quase apaga e alguma coisa parece tentar acordar por dentro.";
            case 11 -> "Quando a visão volta, a presença parece muito mais perto.";
            case 12 -> "Sua força continua crescendo, mas o silêncio dentro da cabeça acabou.";
            case 13 -> "Às vezes seus olhos procuram lugares que você não decidiu olhar.";
            case 14 -> "Seu corpo parece ocupado demais para pertencer apenas a você.";
            case 15 -> "A presença não pede mais espaço. Ela toma.";
            case 16 -> "Depois do retorno, seus movimentos ainda carregam uma intenção que não é sua.";
            case 17 -> "A divisão entre hospedeiro e presença está ficando fina demais.";
            case 18 -> "Seu corpo reconhece aquela energia antes mesmo de você pensar nela.";
            case 19 -> "Há uma sensação de porta aberta que você não consegue fechar.";
            case 20 -> "A presença está completa. Em algum lugar dentro de você, uma única palavra ecoa: corpo.";
            default -> "A presença cresce.";
        };
        return Component.literal(text).withStyle(stage >= 15 ? ChatFormatting.DARK_RED : ChatFormatting.RED);
    }

    private static void applyReceptacleMilestone(ServerPlayer host, int stage) {
        if (stage == 5) {
            sendFugaCinematic(host, PlayerCinematicPayload.TUKUNA_FINGER_REACTION, 60, true, 0.0F);
            PacketDistributor.sendToPlayer(host, new TukunaViewS2CPayload(false, 54));
            host.serverLevel().sendParticles(ParticleTypes.SOUL, host.getX(), host.getY()+0.9, host.getZ(),
                    22, 0.45, 0.7, 0.45, 0.02);
            host.serverLevel().playSound(null, host.blockPosition(), SoundEvents.SOUL_ESCAPE.value(),
                    SoundSource.PLAYERS, 0.8F, 0.7F);
        } else if (stage == 10) {
            host.hurt(host.damageSources().generic(), 6.0F);
            sendFugaCinematic(host, PlayerCinematicPayload.TUKUNA_FINGER_REACTION, 86, true, 0.18F);
            PacketDistributor.sendToPlayer(host, new TukunaViewS2CPayload(false, 82));
            host.serverLevel().playSound(null, host.blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                    SoundSource.PLAYERS, 2.0F, 0.72F);
        } else if (stage == 15) {
            host.getPersistentData().putBoolean(FORCED_TAKEOVER_PENDING_KEY, true);
            tryStartForcedTakeover(host);
        } else if (stage == 20) {
            host.displayClientMessage(Component.literal(
                    "Você não sabe por quê, mas a palavra “corpo” parece importante."
            ).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), true);
        }
    }

    private static void tryStartForcedTakeover(ServerPlayer host) {
        if (!host.getPersistentData().getBoolean(FORCED_TAKEOVER_PENDING_KEY)
                || isFormerReceptacle(host)
                || POSSESSIONS.containsKey(host.getUUID())
                || PENDING_TAKEOVERS.containsKey(host.getUUID())) return;

        UUID owner = spiritOwner(host);
        if (owner == null) return;
        ServerPlayer spirit = host.server.getPlayerList().getPlayer(owner);
        if (spirit == null || !isGhost(spirit) || isPossessingSpirit(spirit)) return;

        boolean spiritAlreadyPending =
                PENDING_TAKEOVERS.values().stream()
                        .anyMatch(stage -> stage.spiritId.equals(spirit.getUUID()));
        if (spiritAlreadyPending) return;

        host.getPersistentData().putBoolean(FORCED_TAKEOVER_PENDING_KEY, false);
        VIEW_HOSTS.put(spirit.getUUID(), host.getUUID());
        host.displayClientMessage(Component.literal(
                "Seu corpo para de responder. A presença assume o controle."
        ).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
        beginTakeover(host, spirit, host.server.getTickCount(), true, false,
                FORCED_POSSESSION_TICKS, false);
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
                Component.literal("Dedo Amaldiçoado")
                        .withStyle(ChatFormatting.DARK_RED)
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

    public static int selfFingerCount(ServerPlayer player) {
        return Math.max(0, player.getPersistentData().getInt(SELF_FINGER_COUNT_KEY));
    }

    public static boolean isFormerReceptacle(ServerPlayer player) {
        return player.getPersistentData().getBoolean(FORMER_RECEPTACLE_KEY);
    }

    public static boolean beginForcedFeed(ServerPlayer actor, ServerPlayer target, ItemStack stack) {
        UUID owner = fingerOwner(stack);
        if (owner == null || !owner.equals(actor.getUUID()) || !hasSpectrum(actor)
                || target == actor || !target.isAlive() || isFormerReceptacle(target)
                || FORCED_FEEDS.containsKey(actor.getUUID()) || !aimedAt(actor, target)) return false;

        FORCED_FEEDS.put(actor.getUUID(), new ForcedFeed(target.getUUID(), actor.server.getTickCount()));
        PlayerControlLockManager.lockMovement(actor, FORCED_FEED_TICKS + 12);
        PlayerControlLockManager.lockActions(actor, FORCED_FEED_TICKS + 12);
        PlayerControlLockManager.lockMovement(target, FORCED_FEED_TICKS + 12);
        PlayerControlLockManager.lockActions(target, FORCED_FEED_TICKS + 12);
        sendFugaCinematic(actor, PlayerCinematicPayload.TUKUNA_FORCE_FEED, FORCED_FEED_TICKS + 18, false, 0);
        sendFugaCinematic(target, PlayerCinematicPayload.TUKUNA_FORCED_EAT, FORCED_FEED_TICKS + 22, true, 0);
        return true;
    }

    public static boolean isForceFeeding(ServerPlayer actor) {
        return FORCED_FEEDS.containsKey(actor.getUUID());
    }

    public static boolean tickForcedFeed(ServerPlayer actor, ItemStack stack) {
        ForcedFeed f = FORCED_FEEDS.get(actor.getUUID());
        if (f == null) return false;
        ServerPlayer target = actor.server.getPlayerList().getPlayer(f.targetId);
        if (target == null || !target.isAlive() || !actor.isAlive() || !actor.isUsingItem()
                || !actor.getUseItem().is(stack.getItem()) || !aimedAt(actor, target)) {
            cancelForcedFeed(actor);
            return false;
        }
        return true;
    }

    public static boolean finishForcedFeed(ServerPlayer actor, ItemStack stack) {
        ForcedFeed f = FORCED_FEEDS.remove(actor.getUUID());
        if (f == null) return false;
        ServerPlayer target = actor.server.getPlayerList().getPlayer(f.targetId);
        clearForcedFeedLocks(actor, target);
        if (actor.server.getTickCount() - f.startedAt < FORCED_FEED_TICKS - 3
                || target == null || !target.isAlive() || !aimedAt(actor, target)) return false;
        if (!consumeFinger(target, stack)) return false;

        Vec3 back = actor.getLookAngle().scale(-0.48);
        actor.setDeltaMovement(actor.getDeltaMovement().add(back.x, 0.10, back.z));
        target.serverLevel().sendParticles(ParticleTypes.SOUL,
                target.getX(), target.getEyeY()-0.12, target.getZ(), 18, 0.16, 0.12, 0.16, 0.035);
        target.serverLevel().playSound(null, target.blockPosition(), SoundEvents.SOUL_ESCAPE.value(),
                SoundSource.PLAYERS, 1.0F, 0.62F);
        return true;
    }

    public static void cancelForcedFeed(ServerPlayer actor) {
        ForcedFeed f = FORCED_FEEDS.remove(actor.getUUID());
        if (f == null) return;
        ServerPlayer target = actor.server.getPlayerList().getPlayer(f.targetId);
        clearForcedFeedLocks(actor, target);
        sendFugaCinematic(actor, PlayerCinematicPayload.CLEAR, 0, false, 0);
        if (target != null) sendFugaCinematic(target, PlayerCinematicPayload.CLEAR, 0, false, 0);
    }

    private static void clearForcedFeedLocks(ServerPlayer actor, ServerPlayer target) {
        PlayerControlLockManager.clearMovement(actor);
        PlayerControlLockManager.clearActions(actor);
        if (target != null) {
            PlayerControlLockManager.clearMovement(target);
            PlayerControlLockManager.clearActions(target);
        }
    }

    private static boolean aimedAt(ServerPlayer actor, ServerPlayer target) {
        if (actor.distanceToSqr(target) > 25 || !actor.hasLineOfSight(target)) return false;
        Vec3 to = target.getEyePosition().subtract(actor.getEyePosition());
        return to.lengthSqr() < 1.0E-6 || actor.getLookAngle().dot(to.normalize()) >= 0.90;
    }

    private static void tickForcedFeeds(MinecraftServer server, long tick) {
        List<UUID> cancel = new ArrayList<>();
        for (Map.Entry<UUID, ForcedFeed> e : FORCED_FEEDS.entrySet()) {
            ServerPlayer a = server.getPlayerList().getPlayer(e.getKey());
            ServerPlayer t = server.getPlayerList().getPlayer(e.getValue().targetId);
            if (a == null || t == null || !a.isAlive() || !t.isAlive()
                    || tick - e.getValue().startedAt > FORCED_FEED_TICKS + 20) cancel.add(e.getKey());
        }
        for (UUID id : cancel) {
            ServerPlayer a = server.getPlayerList().getPlayer(id);
            if (a != null) cancelForcedFeed(a); else FORCED_FEEDS.remove(id);
        }
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

    public static ServerPlayer possessedHostForSpirit(ServerPlayer spirit) {
        Possession possession = possessionForSpirit(spirit.getUUID());
        return possession == null ? null
                : spirit.server.getPlayerList().getPlayer(possession.hostId);
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
        UUID preferred = VIEW_HOSTS.get(spiritId);
        if (preferred != null) {
            ServerPlayer p = server.getPlayerList().getPlayer(preferred);
            if (p != null && p.isAlive() && spiritId.equals(spiritOwner(p))
                    && fingerCount(p) > 0 && !isFormerReceptacle(p)) return p;
            VIEW_HOSTS.remove(spiritId);
        }

        ServerPlayer best = null;
        int bestCount = -1;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            int count = fingerCount(p);
            if (!p.isAlive() || isFormerReceptacle(p) || count <= 0
                    || !spiritId.equals(spiritOwner(p))) continue;
            if (count > bestCount) { best = p; bestCount = count; }
        }
        if (best != null) VIEW_HOSTS.put(spiritId, best.getUUID());
        return best;
    }

    private static boolean handlePactSpeech(ServerPlayer speaker, String raw) {
        String said = normalizeSpeech(raw);
        if (said.isBlank()) return false;
        long now = speaker.server.getTickCount();
        ServerPlayer host = isGhost(speaker)
                ? hostForSpirit(speaker.server, speaker.getUUID()) : speaker;
        if (host == null || fingerCount(host) <= 0) return false;
        UUID spiritId = spiritOwner(host);
        ServerPlayer spirit = spiritId == null ? null
                : speaker.server.getPlayerList().getPlayer(spiritId);
        if (spirit == null || !isGhost(spirit)) return false;

        PactProposal proposal = PACT_PROPOSALS.get(host.getUUID());

        if (proposal != null && (proposal.expiresAt < now || !proposal.spiritId.equals(spiritId))) {
            PACT_PROPOSALS.remove(host.getUUID());
            proposal = null;
        }
        if (speaker == spirit && isPactProposalStart(said)) {
            proposal = new PactProposal(spiritId, now + PROPOSAL_TICKS);
            proposal.draft.append(raw);
            PACT_PROPOSALS.put(host.getUUID(), proposal);
            showPactDebug(spirit, proposal, "rascunho iniciado; diga pronto para enviar");
            spirit.displayClientMessage(
                    Component.literal(
                            "Dica do trato: para duração, regras e palavras exatas, prefira escrever no chat. "
                                    + "Para RP, negociação e fala natural, o Voice Chat continua funcionando."
                    ).withStyle(ChatFormatting.AQUA),
                    false
            );
            return true;
        }
        if (speaker == spirit && proposal != null && !proposal.submitted) {
            proposal.expiresAt = now + PROPOSAL_TICKS;
            if (said.equals("cancelar trato") || said.equals("cancelar")) {
                PACT_PROPOSALS.remove(host.getUUID());
                spirit.displayClientMessage(Component.literal("Rascunho cancelado."), false);
            } else if (said.equals("pronto")) {
                if (!proposal.draft.ready()) {
                    showPactDebug(spirit, proposal, "defina uma duração válida antes de enviar; " + proposal.draft.issue());
                } else {
                    proposal.submitted = true;
                    host.displayClientMessage(Component.literal("Tukuna propõe um TRATO: ao falar a palavra escolhida após aceitar, ele controla seu corpo. "
                            + proposal.draft.summary() + ". Diga 'aceito o trato' ou 'recuso o trato' em até 60 segundos.")
                            .withStyle(ChatFormatting.GOLD), false);
                    showPactDebug(spirit, proposal, "enviado; aguardando resposta");
                }
            } else {
                proposal.draft.append(raw);
                showPactDebug(spirit, proposal, "fala salva: " + raw);
            }
            return true;
        }
        if (speaker == host && (isPactAcceptance(said) || isPactRefusal(said))) {
            if (proposal == null || !proposal.submitted || proposal.accepted) {
                host.displayClientMessage(Component.literal("Nenhuma proposta enviada aguardando resposta."), true);
            } else if (isPactRefusal(said)) {
                PACT_PROPOSALS.remove(host.getUUID());
                host.displayClientMessage(Component.literal("Trato recusado."), false);
                spirit.displayClientMessage(Component.literal("O receptáculo recusou o trato."), false);
            } else {
                proposal.accepted = true;
                proposal.expiresAt = now + PROPOSAL_TICKS;
                host.displayClientMessage(Component.literal(proposal.draft.forget()
                        ? "Você aceitou. A lembrança dos termos se desfaz." : "Você aceitou o trato."), false);
                spirit.displayClientMessage(Component.literal(
                        "Trato aceito. Diga 'palavra do trato: <palavra>' em até 60 segundos."), false);
            }
            return true;
        }

        if (speaker == spirit && said.startsWith("palavra do trato ")) {
            if (proposal == null || proposal.expiresAt < now
                    || !proposal.spiritId.equals(spiritId) || !proposal.accepted) {
                spirit.displayClientMessage(Component.literal("O receptáculo precisa aceitar a proposta antes."), true);
                return true;
            }
            String word = said.substring("palavra do trato ".length()).trim();
            if (!word.matches("[a-z]{3,20}") || word.equals("trocar")
                    || word.equals("fuga") || word.equals("trato")) {
                spirit.displayClientMessage(Component.literal("Escolha uma palavra simples de 3 a 20 letras."), true);
                return true;
            }
            host.getPersistentData().putUUID(PACT_SPIRIT_KEY, spiritId);
            host.getPersistentData().putString(PACT_WORD_KEY, word);
            host.getPersistentData().putInt(PACT_DURATION_KEY, proposal.draft.durationSeconds());
            host.getPersistentData().putBoolean(PACT_INDEFINITE_KEY, proposal.draft.indefinite());
            host.getPersistentData().putBoolean(PACT_PACIFIST_KEY, proposal.draft.pacifist());
            host.getPersistentData().putBoolean(PACT_FORGET_KEY, proposal.draft.forget());
            PACT_PROPOSALS.remove(host.getUUID());
            spirit.displayClientMessage(Component.literal("Palavra do trato definida: " + word), false);
            host.displayClientMessage(Component.literal(proposal.draft.forget() ? "Algo foi selado, mas você não recorda os detalhes." : "Trato selado. Palavra: " + word + ". " + proposal.draft.summary()), false);
            return true;
        }

        if (speaker == host && host.getPersistentData().hasUUID(PACT_SPIRIT_KEY)
                && spiritId.equals(host.getPersistentData().getUUID(PACT_SPIRIT_KEY))
                && said.equals(host.getPersistentData().getString(PACT_WORD_KEY))) {
            if (!POSSESSIONS.containsKey(host.getUUID())
                    && !PENDING_TAKEOVERS.containsKey(host.getUUID())) {
                long until = Math.max(SWAP_COOLDOWNS.getOrDefault(host.getUUID(), 0L),
                        SWAP_COOLDOWNS.getOrDefault(spiritId, 0L));
                if (now >= until) {
                    int storedSeconds = host.getPersistentData().contains(PACT_DURATION_KEY)
                            ? host.getPersistentData().getInt(PACT_DURATION_KEY) : 180;
                    boolean indefinite = host.getPersistentData().getBoolean(PACT_INDEFINITE_KEY)
                            || storedSeconds < 0;
                    int seconds = indefinite
                            ? -1
                            : Math.max(1, Math.min(86400, storedSeconds));
                    boolean pacifist = !host.getPersistentData().contains(PACT_PACIFIST_KEY)
                            || host.getPersistentData().getBoolean(PACT_PACIFIST_KEY);
                    int durationTicks = seconds < 0 ? -1 : seconds * 20;
                    boolean dangerous = seconds < 0 || seconds >= 180;
                    beginTakeover(host, spirit, now, dangerous, pacifist, durationTicks, true);
                }
            }
            return true;
        }
        return false;
    }

    private static boolean isManualReturnStatement(
            String said
    ) {
        return "sair".equals(said)
                || "devolver".equals(said)
                || "devolver corpo".equals(said)
                || "devolver o corpo".equals(said)
                || "voltar".equals(said)
                || "voltar corpo".equals(said)
                || "voltar para o corpo".equals(said);
    }

    private static boolean isPactProposalStart(String said) {
        return said.contains("trato")
                && (said.contains("vamos fazer") || said.contains("vamos criar")
                || said.contains("quero fazer") || said.contains("proponho")
                || said.startsWith("fazer um trato"));
    }

    private static boolean isPactAcceptance(String said) {
        return said.equals("aceito") || said.equals("eu aceito")
                || said.equals("aceito o trato") || said.equals("aceito esse trato");
    }

    private static boolean isPactRefusal(String said) {
        return said.equals("recuso") || said.equals("eu recuso")
                || said.equals("recuso o trato") || said.equals("recuso esse trato")
                || said.equals("nao aceito") || said.equals("nao aceito o trato");
    }

    private static void showPactDebug(ServerPlayer spirit, PactProposal proposal, String note) {
        spirit.displayClientMessage(Component.literal("[DEBUG / TRATO] " + note + " — " + proposal.draft.summary())
                .withStyle(ChatFormatting.AQUA), false);
        WayAround.LOGGER.info("[Tukuna/Pact] spirit={} state={} note={}",
                spirit.getGameProfile().getName(), proposal.draft.summary(), note);
    }

    public static boolean isDraftingPact(ServerPlayer speaker) {
        long now = speaker.server.getTickCount();
        return PACT_PROPOSALS.values().stream().anyMatch(p -> p.spiritId.equals(speaker.getUUID())
                && !p.submitted && p.expiresAt >= now);
    }

    public static void confirmSwap(
            ServerPlayer caller
    ) {
        confirmSwap(caller, false);
    }

    public static void confirmSwap(ServerPlayer caller, boolean dangerous) {
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

        if (POSSESSIONS.containsKey(host.getUUID())
                || PENDING_TAKEOVERS.containsKey(host.getUUID())) {
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

        Map<UUID, Long> confirmations = dangerous
                ? CONTRACT_CONFIRMATIONS : SWAP_CONFIRMATIONS;
        confirmations.put(
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

        if (dangerous) {
            caller.displayClientMessage(Component.literal(
                    "Contrato perigoso: até 3 minutos. Tukuna pode devolver antes dizendo 'devolver corpo'."
            ).withStyle(ChatFormatting.GOLD), false);
        }

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
                confirmations.getOrDefault(
                        other.getUUID(),
                        Long.MIN_VALUE / 4L
                );

        if (tick - otherTick
                <= SWAP_CONFIRM_WINDOW_TICKS) {

            confirmations.remove(
                    host.getUUID()
            );

            confirmations.remove(
                    spirit.getUUID()
            );

            SWAP_CONFIRMATIONS.remove(host.getUUID());
            SWAP_CONFIRMATIONS.remove(spirit.getUUID());
            CONTRACT_CONFIRMATIONS.remove(host.getUUID());
            CONTRACT_CONFIRMATIONS.remove(spirit.getUUID());

            beginTakeover(host, spirit, tick, dangerous, false);
        }
    }

    private static void returnBodyEarly(ServerPlayer spirit) {
        Possession possession = possessionForSpirit(spirit.getUUID());
        if (possession == null) return;
        ServerPlayer host = spirit.server.getPlayerList().getPlayer(possession.hostId);
        if (host == null) return;
        finishPossession(host, spirit, possession, spirit.server.getTickCount());
        POSSESSIONS.remove(possession.hostId);
    }

    public static void beginManualDesmartelar(
            ServerPlayer player
    ) {
        if (!hasSpectrum(
                player
        )
                || PlayerControlLockManager.actionsLocked(
                player
        )
                || !player.getMainHandItem()
                        .isEmpty()
                || !player.getOffhandItem()
                        .isEmpty()) {

            return;
        }

        long tick =
                player.server
                        .getTickCount();

        long cooldown =
                DESMARTELAR_COOLDOWNS.getOrDefault(
                        player.getUUID(),
                        0L
                );

        if (tick < cooldown
                || MANUAL_DESMARTELAR_CHARGES.containsKey(
                player.getUUID()
        )) {
            return;
        }

        MANUAL_DESMARTELAR_CHARGES.put(
                player.getUUID(),
                tick
        );

        sendFugaCinematic(
                player,
                PlayerCinematicPayload.DESMARTELAR_CHARGE,
                0,
                false,
                0.0F
        );
    }

    public static void releaseManualDesmartelar(
            ServerPlayer player
    ) {
        Long started =
                MANUAL_DESMARTELAR_CHARGES.remove(
                        player.getUUID()
                );

        if (started == null
                || !hasSpectrum(
                player
        )
                || PlayerControlLockManager.actionsLocked(
                player
        )
                || !player.getMainHandItem()
                        .isEmpty()
                || !player.getOffhandItem()
                        .isEmpty()) {

            cancelManualDesmartelar(
                    player
            );

            return;
        }

        long held =
                player.server
                        .getTickCount()
                        - started;

        if (held < 8L) {
            cancelManualDesmartelar(
                    player
            );

            return;
        }

        castPossessedDesmartelar(
                player,
                false
        );
    }

    public static void cancelManualDesmartelar(
            ServerPlayer player
    ) {
        MANUAL_DESMARTELAR_CHARGES.remove(
                player.getUUID()
        );

        sendFugaCinematic(
                player,
                PlayerCinematicPayload.CLEAR,
                0,
                false,
                0.0F
        );
    }

    public static void castPossessedDesmartelar(
            ServerPlayer player
    ) {
        castPossessedDesmartelar(
                player,
                false
        );
    }

    public static void castRapidDesmartelar(ServerPlayer player, boolean fire) {
        castDesmartelar(player, fire, 4);
    }

    public static void castPossessedDesmartelar(ServerPlayer player, boolean fire) {
        castDesmartelar(player, fire, 28);
    }

    private static void castDesmartelar(ServerPlayer player, boolean fire, int cooldownTicks) {
        if (!player.isAlive() || player.isSpectator() || isSilencedHost(player)
                || isPacifistPossession(player) || PlayerControlLockManager.actionsLocked(player)) return;

        long tick =
                player.server
                        .getTickCount();

        int fingers;

        Possession activePossession = possessionForSpirit(player.getUUID());
        if (activePossession != null) {
            fingers = Math.max(activePossession.fingers, selfFingerCount(player));
        } else if (hasSpectrum(player)) {
            fingers = effectiveTukunaFingers(player);
        } else {
            return;
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
                tick + cooldownTicks
        );

        sendFugaCinematic(
                player,
                PlayerCinematicPayload.DESMARTELAR_RELEASE,
                18,
                false,
                0.0F
        );

        Desmartelar.cast(
                player,
                fingers,
                fire
        );
    }

    public static boolean isFugaCharging(ServerPlayer player) { return FUGA_CHARGES.containsKey(player.getUUID()); }

    public static boolean prepareFuga(
            ServerPlayer player
    ) {
        if (isPacifistPossession(player) || PlayerControlLockManager.actionsLocked(
                player
        )) {
            return false;
        }

        if (!hasSpectrum(
                player
        )
                || effectiveTukunaFingers(player) <= 10) {
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

        PlayerControlLockManager.lockMovement(
                player,
                0
        );

        sendFugaCinematic(
                player,
                PlayerCinematicPayload.FUGA_CHARGE,
                0,
                false,
                0.0F
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
        if (isPacifistPossession(player) || PlayerControlLockManager.actionsLocked(
                player
        )) {
            return false;
        }

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

        PlayerControlLockManager.clearMovement(
                player
        );

        Vec3 direction =
                player.getLookAngle()
                        .normalize();

        Vec3 right = new Vec3(-direction.z, 0, direction.x).normalize();
        Vec3 position = player.getEyePosition().add(direction.scale(1.05)).add(right.scale(.58)).add(0, -.2, 0);

        Vec3 back = direction.scale(-1);
        BlockPos ground = player.blockPosition().below();
        BlockState groundState = player.serverLevel().getBlockState(ground);
        if (!groundState.isAir()) {
            for (int i = 0; i < 40; i++) {
                double spread = (player.getRandom().nextDouble() - .5) * 1.2;
                Vec3 origin = player.position().add(back.scale(.5 + player.getRandom().nextDouble()));
                player.serverLevel().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, groundState),
                        origin.x, origin.y + .15, origin.z, 0,
                        back.x * (1.0 + i * .02) + spread, .18 + player.getRandom().nextDouble() * .3,
                        back.z * (1.0 + i * .02) - spread, 1.0);
            }
        }
        net.caravidro.wayaround.thermal.RegionalTemperature.pulse(player.serverLevel(), position, 8, 3200);
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

        sendFugaCinematic(
                player,
                PlayerCinematicPayload.FUGA_RELEASE,
                36,
                false,
                0.0F
        );

        player.serverLevel()
                .playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.BLAZE_SHOOT,
                        SoundSource.PLAYERS,
                        2.4F,
                        0.38F
                );

        player.serverLevel()
                .playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.END_PORTAL_SPAWN,
                        SoundSource.PLAYERS,
                        1.8F,
                        0.72F
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

    private static void sendFugaCinematic(
            ServerPlayer player,
            byte animation,
            int durationTicks,
            boolean lockCamera,
            float shakeStrength
    ) {
        PacketDistributor.sendToPlayersNear(
                player.serverLevel(),
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                192.0,
                new PlayerCinematicPayload(
                        player.getUUID(),
                        animation,
                        durationTicks,
                        lockCamera,
                        shakeStrength
                )
        );
    }

    private static void ensureFugaUnlockMigrated(
            ServerPlayer player
    ) {
        if (effectiveTukunaFingers(player) <= 10
                || player.getPersistentData().getBoolean(FUGA_UNLOCK_MIGRATION_KEY)) {
            return;
        }

        /*
         * Older saves could already be above ten fingers before the new
         * progression gate existed. Force one clean re-selection so those
         * worlds do not remain stuck with the legacy prompt/phrase state.
         */
        player.getPersistentData().remove(FUGA_PHRASE_KEY);
        player.getPersistentData().remove(FUGA_PROMPT_KEY);
        player.getPersistentData().putBoolean(FUGA_UNLOCK_MIGRATION_KEY, true);

        player.displayClientMessage(
                Component.literal("Tukuna: 11+ dedos detectados. A Fuga precisa de uma nova palavra-chave.")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                false
        );

        promptFugaPhrase(player);
    }

    private static void promptFugaPhrase(
            ServerPlayer player
    ) {
        if (effectiveTukunaFingers(player) <= 10) {
            return;
        }

        if (!player.getPersistentData()
                .getString(
                        FUGA_PHRASE_KEY
                )
                .isBlank()) {
            return;
        }

        /*
         * Do not permanently silence this prompt. Older versions persisted a
         * "prompted" bit even when the player never actually chose a word,
         * which could leave Fuga configuration invisible forever. The call
         * sites are event-like (unlock / possession), so re-prompting while
         * the phrase is still empty is safe and useful.
         */
        player.getPersistentData()
                .putBoolean(
                        FUGA_PROMPT_KEY,
                        true
                );

        player.sendSystemMessage(
                Component.literal(
                        "Tukuna: com mais de 10 dedos, escolha a palavra-chave da Fuga no chat: palavra da fuga: <palavra>"
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

        if (normalized.equals("dominio tukuna") || normalized.equals("expansao de dominio tukuna")
                || normalized.equals("dominio") && !SpectrumAccess.has(player, SpectrumType.VOID)) {
            net.caravidro.wayaround.spectrum.SpectrumActions.perform(player,
                    net.caravidro.wayaround.spectrum.SpectrumAction.TUKUNA_DOMAIN);
            return true;
        }
        if (normalized.equals("preparar desmartelar") || normalized.equals("carregar desmartelar")) {
            net.caravidro.wayaround.spectrum.SpectrumActions.input(player, 1, (byte)0); return true;
        }
        if (normalized.equals("soltar desmartelar") || normalized.equals("lancar desmartelar")) {
            net.caravidro.wayaround.spectrum.SpectrumActions.input(player, 1, (byte)1); return true;
        }
        if (normalized.equals("combinar fogo")) {
            net.caravidro.wayaround.spectrum.SpectrumActions.input(player, 2, (byte)0); return true;
        }
        if (normalized.equals("cancelar tecnica")) {
            net.caravidro.wayaround.spectrum.SpectrumActions.cancel(player); return true;
        }
        String fugaPrefix =
                normalized.startsWith("palavra da fuga ")
                        ? "palavra da fuga "
                        : normalized.startsWith("palavra de fuga ")
                                ? "palavra de fuga "
                                : normalized.startsWith("frase de fuga ")
                                        ? "frase de fuga "
                                        : null;

        if (fugaPrefix != null) {
            if (effectiveTukunaFingers(player) <= 10) {
                player.displayClientMessage(
                        Component.literal("A Fuga só desperta acima de 10 dedos.")
                                .withStyle(ChatFormatting.DARK_RED),
                        true
                );
                return true;
            }

            String phrase =
                    normalized.substring(
                            fugaPrefix.length()
                    )
                            .trim();

            if (!phrase.matches("[a-z0-9]{3,20}")
                    || phrase.contains(
                    "fuga"
            )) {

                player.displayClientMessage(
                        Component.literal(
                                "Escolha uma palavra de 3 a 20 caracteres, sem espaços e sem usar 'fuga'."
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
                            "Palavra-chave da Fuga definida: \""
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

        if (!phrase.isBlank() && java.util.Arrays.asList(normalized.split(" ")).contains(phrase)) {

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

            net.caravidro.wayaround.spectrum.SpectrumActions.perform(player,
                    net.caravidro.wayaround.spectrum.SpectrumAction.FUGA);
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

                if (player != null) {
                    PlayerControlLockManager.clearMovement(
                            player
                    );

                    sendFugaCinematic(
                            player,
                            PlayerCinematicPayload.CLEAR,
                            0,
                            false,
                            0.0F
                    );
                }

                iterator.remove();
                continue;
            }

            long chargeAge = tick - charge.startedAt;
            if (!charge.clapped && chargeAge >= 56) {
                charge.clapped = true;
                Vec3 hands = player.getEyePosition().add(player.getLookAngle().scale(.8));
                player.serverLevel().sendParticles(ParticleTypes.FLAME, hands.x, hands.y, hands.z, 45, .15, .15, .15, .15);
                player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                        SoundSource.PLAYERS, 1.4F, .55F);
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

                BlockState state = level.hasChunkAt(pos) ? level.getBlockState(pos) : Blocks.BEDROCK.defaultBlockState();

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

            if (tick % 2L == 0L || collide) {
                PacketDistributor.sendToPlayersNear(level, null, projectile.position.x, projectile.position.y,
                        projectile.position.z, 192, new net.caravidro.wayaround.network.FugaArrowPayload(
                                projectile.owner, projectile.position.x, projectile.position.y, projectile.position.z,
                                projectile.velocity.x, projectile.velocity.y, projectile.velocity.z, collide ? 0 : projectile.life));
            }
            if (tick % 4L == 0L && !collide) {
                net.caravidro.wayaround.thermal.RegionalTemperature.pulse(level, projectile.position, 6, 3200);
            }
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
        PacketDistributor.sendToPlayersNear(
                level,
                null,
                center.x,
                center.y,
                center.z,
                1024.0,
                new TukunaFugaVisualPayload(
                        owner.getUUID(),
                        center.x,
                        center.y,
                        center.z,
                        180
                )
        );

        net.caravidro.wayaround.thermal.ExpandingFugaBlast.start(level, owner, center);

        for (int y = 0;
             y <= 220;
             y += 8) {

            double width =
                    1.35
                            + y
                                    * 0.026;

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
             i < 80;
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

        level.playSound(
                null,
                BlockPos.containing(
                        center
                ),
                SoundEvents.WITHER_SPAWN,
                SoundSource.PLAYERS,
                4.8F,
                0.62F
        );
    }

    public static boolean isPacifistPossession(ServerPlayer spirit) {
        Possession possession = possessionForSpirit(spirit.getUUID());
        return possession != null && possession.pacifist;
    }

    private static void beginTakeover(ServerPlayer host, ServerPlayer spirit,
                                      long tick, boolean dangerous, boolean pacifist) {
        beginTakeover(host, spirit, tick, dangerous, pacifist, 0, dangerous);
    }

    private static void beginTakeover(ServerPlayer host, ServerPlayer spirit,
                                      long tick, boolean dangerous, boolean pacifist,
                                      int durationTicks, boolean negotiated) {
        boolean spiritAlreadyPending =
                PENDING_TAKEOVERS.values().stream()
                        .anyMatch(stage -> stage.spiritId.equals(spirit.getUUID()));

        if (isFormerReceptacle(host)
                || POSSESSIONS.containsKey(host.getUUID())
                || PENDING_TAKEOVERS.containsKey(host.getUUID())
                || isPossessingSpirit(spirit)
                || spiritAlreadyPending) return;

        VIEW_HOSTS.put(spirit.getUUID(), host.getUUID());
        PacketDistributor.sendToPlayer(host, new TukunaViewS2CPayload(false, TAKEOVER_TICKS));
        syncTukunaMarks(host, true, 54);

        PENDING_TAKEOVERS.put(host.getUUID(), new PendingTakeover(
                spirit.getUUID(), tick + TAKEOVER_TICKS, dangerous, pacifist, durationTicks, negotiated));
        PlayerControlLockManager.lockMovement(host, TAKEOVER_TICKS);
        PlayerControlLockManager.lockActions(host, TAKEOVER_TICKS);
        sendFugaCinematic(host, PlayerCinematicPayload.TUKUNA_TAKEOVER,
                TAKEOVER_TICKS, true, 0.0F);
        host.serverLevel().playSound(null, host.blockPosition(),
                SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.1F, 0.55F);
    }

    private static void tickTakeovers(MinecraftServer server, long tick) {
        Iterator<Map.Entry<UUID, PendingTakeover>> iterator =
                PENDING_TAKEOVERS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingTakeover> entry = iterator.next();
            PendingTakeover stage = entry.getValue();
            ServerPlayer host = server.getPlayerList().getPlayer(entry.getKey());
            ServerPlayer spirit = server.getPlayerList().getPlayer(stage.spiritId);
            if (host == null || spirit == null || !host.isAlive()
                    || !isGhost(spirit) || fingerCount(host) <= 0) {
                if (host != null) {
                    PlayerControlLockManager.clearMovement(host);
                    PlayerControlLockManager.clearActions(host);
                    sendFugaCinematic(host, PlayerCinematicPayload.CLEAR, 0, false, 0.0F);
                }
                iterator.remove();
                continue;
            }
            if (tick % 2L == 0L) {
                float progress = 1.0F - (float)(stage.readyAt - tick) / TAKEOVER_TICKS;
                double radius = 0.25 + progress * 0.72;
                double rotation = tick * 0.11;
                for (int i = 0; i < 8; i++) {
                    double angle = rotation + Math.PI * 2.0 * i / 8.0;
                    double x = host.getX() + Math.cos(angle) * radius;
                    double z = host.getZ() + Math.sin(angle) * radius;
                    double y = host.getY() + 0.2 + ((i * 3 + (int)tick) % 12) * 0.16;
                    host.serverLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            x, y, z, 1, 0.015, 0.025, 0.015, 0.002);
                    if (progress > 0.35F) {
                        host.serverLevel().sendParticles(ParticleTypes.PORTAL,
                                x, y, z, 1, 0.01, 0.02, 0.01, 0.015);
                    }
                }
            }
            if (stage.durationTicks < 0 && tick % 4L == 0L) {
                emitIndefiniteAura(host, tick, true);
            }
            if (tick >= stage.readyAt) {
                iterator.remove();
                PlayerControlLockManager.clearMovement(host);
                PlayerControlLockManager.clearActions(host);
                sendFugaCinematic(host, PlayerCinematicPayload.CLEAR, 0, false, 0.0F);
                beginPossession(host, spirit, tick, stage.dangerous, stage.pacifist, stage.durationTicks, stage.negotiated);
            }
        }
    }

    private static void beginPossession(
            ServerPlayer host,
            ServerPlayer spirit,
            long tick,
            boolean dangerous,
            boolean pacifist,
            int durationTicks,
            boolean negotiated
    ) {
        int fingers =
                fingerCount(
                        host
                );

        GameType hostMode =
                host.gameMode
                        .getGameModeForPlayer();

        boolean indefinite = durationTicks < 0;
        boolean contractMusic = indefinite
                || durationTicks > DANGEROUS_CONTRACT_TICKS;

        long endTick = indefinite
                ? Long.MAX_VALUE
                : durationTicks > 0
                        ? tick + durationTicks
                        : dangerous
                                ? tick + DANGEROUS_CONTRACT_TICKS
                                : tick
                                        + POSSESSION_BASE_TICKS
                                        + fingers
                                                * POSSESSION_PER_FINGER_TICKS;

        Possession possession =
                new Possession(
                        host.getUUID(),
                        spirit.getUUID(),
                        fingers,
                        hostMode,
                        endTick,
                        dangerous,
                        pacifist,
                        negotiated,
                        contractMusic,
                        spirit.isInvisible()
                );

        POSSESSIONS.put(
                host.getUUID(),
                possession
        );

        VIEW_HOSTS.remove(spirit.getUUID());
        PacketDistributor.sendToPlayer(spirit, new TukunaViewS2CPayload(false, 0));

        // The marks are transferred to the ONE entity that will actually
        // move. Its skin/model is overridden client-side to the receptacle.
        syncTukunaMarks(host, false, 1);
        syncTukunaMarks(spirit, true, 1);

        SpectrumAccess.syncPossession(host, spirit);

        PlayerControlLockManager.lockMovement(host, 0);
        PlayerControlLockManager.lockActions(host, 0);

        host.setGameMode(
                GameType.SPECTATOR
        );

        teleportTo(
                spirit,
                host.serverLevel(),
                host.position(),
                host.getYRot(),
                0.0F
        );

        // A ghost/spectator can be invisible for unrelated reasons. While it
        // is the possession controller it must render; the skin mixin makes it
        // look like the receptacle instead of Tukuna.
        spirit.setInvisible(false);

        syncPossessionVisual(
                host,
                spirit.getUUID(),
                true
        );

        spirit.setYRot(host.getYRot());
        spirit.setXRot(0.0F);

        sendFugaCinematic(spirit, PlayerCinematicPayload.TUKUNA_RETURN,
                RETURN_TICKS, true, 0.0F);
        if (indefinite) emitIndefiniteAura(spirit, tick, true);

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

        if (fingers > 10) {
            ensureFugaUnlockMigrated(spirit);
            promptFugaPhrase(spirit);
        }

        removeHostBuffs(
                host
        );

        PacketDistributor.sendToPlayer(
                host,
                new TukunaPossessionS2CPayload(
                        true, contractMusic, indefinite
                )
        );

        if (contractMusic) {
            PacketDistributor.sendToPlayer(spirit,
                    new TukunaPossessionS2CPayload(false, true, indefinite));
        }

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
                    || !host.isAlive()
                    || !spirit.isAlive()) {

                if (host != null) {
                    syncPossessionVisual(
                            host,
                            possession.spiritId,
                            false
                    );

                    if (host.isAlive()) {
                        emergencyRestoreHost(
                                host,
                                possession.hostMode,
                                possession.spiritId
                        );
                    } else {
                        PlayerControlLockManager.clearMovement(
                                host
                        );
                        PlayerControlLockManager.clearActions(
                                host
                        );
                    }
                }

                if (spirit != null) {
                    spirit.setInvisible(
                            possession.spiritInvisibleBefore
                    );

                    removePossessionBuffs(
                            spirit
                    );

                    if (spirit.isAlive()) {
                        spirit.setGameMode(
                                GameType.SPECTATOR
                        );

                        spirit.setCamera(
                                spirit
                        );

                        PacketDistributor.sendToPlayer(
                                spirit,
                                new TukunaViewS2CPayload(
                                        false,
                                        0
                                )
                        );
                    }

                    PacketDistributor.sendToPlayer(
                            spirit,
                            new TukunaPossessionS2CPayload(
                                    false,
                                    false,
                                    false
                            )
                    );

                    PacketDistributor.sendToPlayer(
                            spirit,
                            new TukunaPossessionVisualS2CPayload(
                                    possession.spiritId,
                                    possession.hostId,
                                    false
                            )
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

            // No body-following here. Tukuna is the sole moving entity.
            host.setCamera(
                    spirit
            );

            if (possession.endTick == Long.MAX_VALUE && tick % 10L == 0L) {
                emitIndefiniteAura(spirit, tick, false);
            }

            if (tick % 40L == 0L) {
                syncTukunaMarks(
                        spirit,
                        true,
                        1
                );

                syncPossessionVisual(
                        host,
                        spirit.getUUID(),
                        true
                );
            }

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

        PlayerControlLockManager.clearMovement(
                host
        );
        PlayerControlLockManager.clearActions(
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

        syncPossessionVisual(
                host,
                spirit.getUUID(),
                false
        );

        spirit.setInvisible(
                possession.spiritInvisibleBefore
        );
        spirit.setGameMode(
                GameType.SPECTATOR
        );
        spirit.setCamera(
                host
        );

        SpectrumAccess.sync(spirit);

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
                        false, false, false
                )
        );

        // At the hand-back the physical host appears once at the controller's
        // final location. Transfer the mark state so it can fade on the body
        // instead of briefly appearing on Tukuna's restored skin.
        syncTukunaMarks(spirit, false, 1);
        syncTukunaMarks(host, true, 1);
        syncTukunaMarks(host, false, 54);

        PacketDistributor.sendToPlayer(spirit, new TukunaViewS2CPayload(true, 0));
        VIEW_HOSTS.put(spirit.getUUID(), host.getUUID());
        if (possession.contractMusic) {
            PacketDistributor.sendToPlayer(spirit,
                    new TukunaPossessionS2CPayload(false, false, false));
        }

        beginReturnVisual(host, possession.endTick == Long.MAX_VALUE, tick);

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

    private static void beginReturnVisual(ServerPlayer player, boolean indefinite, long tick) {
        RETURN_VISUALS.put(player.getUUID(), new ReturnVisual(tick + RETURN_TICKS, indefinite));
        sendFugaCinematic(player, PlayerCinematicPayload.TUKUNA_RETURN,
                RETURN_TICKS, true, 0.0F);
        player.serverLevel().sendParticles(ParticleTypes.SOUL,
                player.getX(), player.getY() + 1.0, player.getZ(),
                indefinite ? 48 : 12, 0.55, 0.75, 0.55, indefinite ? 0.12 : 0.035);
    }

    private static void tickReturnVisuals(MinecraftServer server, long tick) {
        RETURN_VISUALS.entrySet().removeIf(entry -> {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            ReturnVisual visual = entry.getValue();
            if (player == null || !player.isAlive() || tick >= visual.endsAt) return true;
            if (tick % 4L != 0L) return false;
            double progress = 1.0 - (visual.endsAt - tick) / (double) RETURN_TICKS;
            double radius = 0.4 + progress * (visual.indefinite ? 2.6 : 0.8);
            int points = visual.indefinite ? 20 : 8;
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2.0 * i / points - tick * 0.16;
                player.serverLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        player.getX() + Math.cos(angle) * radius,
                        player.getY() + 0.2 + progress * 1.6,
                        player.getZ() + Math.sin(angle) * radius,
                        1, 0.01, 0.01, 0.01, 0.006);
            }
            if (visual.indefinite) {
                player.serverLevel().sendParticles(ParticleTypes.SMOKE,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        6, 0.45, 0.65, 0.45, 0.04);
            }
            return false;
        });
    }

    private static void emitIndefiniteAura(ServerPlayer player, long tick, boolean intense) {
        int points = intense ? 20 : 8;
        double radius = intense ? 1.1 : 0.65;
        for (int i = 0; i < points; i++) {
            double angle = tick * 0.16 + Math.PI * 2.0 * i / points;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double y = player.getY() + 0.15 + (i % 10) * 0.18;
            player.serverLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    x, y, z, 1, 0.01, 0.015, 0.01, 0.003);
            if (intense) player.serverLevel().sendParticles(ParticleTypes.SMOKE,
                    x, y, z, 1, 0.02, 0.03, 0.02, 0.015);
        }
    }

    private record ReturnVisual(long endsAt, boolean indefinite) {}

    private static void emergencyRestoreHost(
            ServerPlayer host,
            GameType hostMode,
            UUID spiritId
    ) {
        syncPossessionVisual(
                host,
                spiritId,
                false
        );

        host.setCamera(
                host
        );

        PlayerControlLockManager.clearMovement(
                host
        );
        PlayerControlLockManager.clearActions(
                host
        );

        host.setGameMode(
                hostMode
        );

        PacketDistributor.sendToPlayer(
                host,
                new TukunaPossessionS2CPayload(
                        false, false, false
                )
        );
        syncTukunaMarks(host, false, 24);
        PacketDistributor.sendToPlayer(host, new TukunaViewS2CPayload(false, 0));
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

    private static void syncTukunaMarks(ServerPlayer body, boolean active, int ticks) {
        PacketDistributor.sendToPlayersNear(body.serverLevel(), null,
                body.getX(), body.getY(), body.getZ(), 160,
                new TukunaMarkS2CPayload(body.getUUID(), active, Math.max(1, ticks)));
    }

    private static void syncPossessionVisual(
            ServerPlayer body,
            UUID controller,
            boolean active
    ) {
        TukunaPossessionVisualS2CPayload payload =
                new TukunaPossessionVisualS2CPayload(
                        controller,
                        body.getUUID(),
                        active
                );

        ServerPlayer spirit =
                body.server
                        .getPlayerList()
                        .getPlayer(
                                controller
                        );

        ServerPlayer center =
                spirit != null
                        ? spirit
                        : body;

        PacketDistributor.sendToPlayersNear(
                center.serverLevel(),
                null,
                center.getX(),
                center.getY(),
                center.getZ(),
                192,
                payload
        );

        // Both participants receive it directly; nearby observers receive it
        // around the actual moving avatar.
        PacketDistributor.sendToPlayer(
                body,
                payload
        );

        if (spirit != null
                && spirit != body) {
            PacketDistributor.sendToPlayer(
                    spirit,
                    payload
            );
        }
    }

    private static boolean tryReclaimBody(ServerPlayer spirit) {
        if (!hasSpectrum(spirit)) return false;
        Possession possession = possessionForSpirit(spirit.getUUID());
        ServerPlayer host = possession != null
                ? spirit.server.getPlayerList().getPlayer(possession.hostId)
                : isGhost(spirit) ? hostForSpirit(spirit.server, spirit.getUUID()) : null;
        if (host == null) return false;

        if (fingerCount(host) < MAX_FINGERS || !spirit.getUUID().equals(spiritOwner(host))) {
            spirit.displayClientMessage(Component.literal(
                    "A palavra não encontra força suficiente para separar a presença do receptáculo."
            ).withStyle(ChatFormatting.DARK_GRAY), true);
            return true;
        }

        Vec3 originalHostPos = possession != null
                ? spirit.position()
                : host.position();
        ServerLevel originalHostLevel = possession != null
                ? spirit.serverLevel()
                : host.serverLevel();
        float yaw = possession != null
                ? spirit.getYRot()
                : host.getYRot();
        float pitch = possession != null
                ? spirit.getXRot()
                : host.getXRot();

        if (possession != null) {
            POSSESSIONS.remove(host.getUUID());
            syncPossessionVisual(host, spirit.getUUID(), false);
            host.setCamera(host);
            PlayerControlLockManager.clearMovement(host);
            PlayerControlLockManager.clearActions(host);
            teleportTo(host, spirit.serverLevel(), spirit.position(), spirit.getYRot(), spirit.getXRot());
            host.setGameMode(possession.hostMode);
            PacketDistributor.sendToPlayer(host, new TukunaPossessionS2CPayload(false, false, false));
            removePossessionBuffs(spirit);
        }

        host.getPersistentData().remove(HOST_SPIRIT_KEY);
        host.getPersistentData().putBoolean(FORMER_RECEPTACLE_KEY, true);
        host.getPersistentData().putBoolean(FORCED_TAKEOVER_PENDING_KEY, false);
        host.getPersistentData().putInt(HOST_FINGER_COUNT_KEY, MAX_FINGERS);
        applyHostBuffs(host, MAX_FINGERS);

        spirit.getPersistentData().putBoolean(GHOST_KEY, false);
        spirit.getPersistentData().putBoolean(BODY_RECLAIMED_KEY, true);
        spirit.getPersistentData().putInt(SELF_FINGER_COUNT_KEY, MAX_FINGERS);
        VIEW_HOSTS.remove(spirit.getUUID());

        spirit.setInvisible(false);

        double angle = Math.toRadians(yaw);
        teleportTo(spirit, originalHostLevel,
                originalHostPos.add(-Math.sin(angle)*1.6, 0, Math.cos(angle)*1.6), yaw, pitch);
        spirit.setGameMode(GameType.SURVIVAL);
        spirit.setCamera(spirit);
        PacketDistributor.sendToPlayer(spirit, new TukunaViewS2CPayload(false, 0));
        applyPossessionBuffs(spirit, MAX_FINGERS);
        SpectrumAccess.sync(spirit);

        syncTukunaMarks(host, false, 56);
        syncTukunaMarks(spirit, true, 42);
        sendFugaCinematic(spirit, PlayerCinematicPayload.TUKUNA_RETURN, RETURN_TICKS, true, 0.18F);
        spirit.serverLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                spirit.getX(), spirit.getY()+1, spirit.getZ(), 64, .75, 1, .75, .08);
        spirit.serverLevel().playSound(null, spirit.blockPosition(), SoundEvents.WITHER_SPAWN,
                SoundSource.PLAYERS, 2.4F, .7F);

        host.displayClientMessage(Component.literal(
                "A presença abandona seu corpo. A força fica — a possessão não volta."
        ).withStyle(ChatFormatting.GOLD), false);
        spirit.displayClientMessage(Component.literal(
                "CORPO. A presença se separa do receptáculo e volta a existir por conta própria."
        ).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
        return true;
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

    private static int effectiveTukunaFingers(
            ServerPlayer player
    ) {
        Possession possession = possessionForSpirit(player.getUUID());
        if (possession != null) return Math.max(possession.fingers, selfFingerCount(player));

        if (isGhost(player)) {
            ServerPlayer host = hostForSpirit(player.server, player.getUUID());
            return Math.max(selfFingerCount(player), host == null ? 0 : fingerCount(host));
        }

        if (hasSpectrum(player)) {
            if (player.getPersistentData().getBoolean(BODY_RECLAIMED_KEY)) return MAX_FINGERS;
            return Math.max(1, selfFingerCount(player));
        }

        return fingerCount(player);
    }

    private static boolean hasSpectrum(
            ServerPlayer player
    ) {
        return SpectrumAccess.has(player, SpectrumType.TUKUNA);
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

        PACT_PROPOSALS.entrySet().removeIf(entry -> entry.getValue().expiresAt < tick);

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
        private boolean clapped;

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

    private static final class PactProposal {
        final UUID spiritId;
        long expiresAt;
        boolean submitted;
        boolean accepted;
        final SpokenPactDraft draft = new SpokenPactDraft();
        PactProposal(UUID spiritId, long expiresAt) {
            this.spiritId = spiritId;
            this.expiresAt = expiresAt;
        }
    }

    private record PendingTakeover(UUID spiritId, long readyAt,
                                   boolean dangerous, boolean pacifist,
                                   int durationTicks, boolean negotiated) {}

    private record ForcedFeed(UUID targetId, long startedAt) {}

    private static final class Possession {
        private final UUID hostId;
        private final UUID spiritId;
        private final int fingers;
        private final GameType hostMode;
        private final long endTick;
        private final boolean dangerous;
        private final boolean pacifist;
        private final boolean negotiated;
        private final boolean contractMusic;
        private final boolean spiritInvisibleBefore;

        private boolean nearWarned;
        private boolean returningWarned;

        private Possession(
                UUID hostId,
                UUID spiritId,
                int fingers,
                GameType hostMode,
                long endTick,
                boolean dangerous,
                boolean pacifist,
                boolean negotiated,
                boolean contractMusic,
                boolean spiritInvisibleBefore
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
            this.dangerous = dangerous;
            this.pacifist = pacifist;
            this.negotiated = negotiated;
            this.contractMusic = contractMusic;
            this.spiritInvisibleBefore = spiritInvisibleBefore;
        }
    }
}

