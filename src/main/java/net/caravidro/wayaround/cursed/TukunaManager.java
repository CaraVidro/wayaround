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
        if ("devolver corpo".equals(statement) || "voltar corpo".equals(statement)) {
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
        if ("devolver corpo".equals(statement) || "voltar corpo".equals(statement)) {
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

        boolean unlockedFuga =
                current <= 10
                        && next > 10;

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

            if (unlockedFuga) {
                spirit.getPersistentData().remove(FUGA_PHRASE_KEY);
                spirit.getPersistentData().remove(FUGA_PROMPT_KEY);
                spirit.getPersistentData().putBoolean(FUGA_UNLOCK_MIGRATION_KEY, true);
                promptFugaPhrase(spirit);
            }
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
        if (possession == null || !possession.negotiated) return;
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

    public static void castPossessedDesmartelar(
            ServerPlayer player,
            boolean fire
    ) {
        if (isPacifistPossession(player) || PlayerControlLockManager.actionsLocked(
                player
        )) {
            return;
        }

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

        pulverizeFugaCrater(
                level,
                center
        );

        level.explode(
                owner,
                center.x,
                center.y,
                center.z,
                28.0F,
                true,
                Level.ExplosionInteraction.TNT
        );

        double damageRadius =
                52.0;

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
             y <= 220;
             y += 2) {

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
             i < 460;
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
        if (POSSESSIONS.containsKey(host.getUUID())
                || PENDING_TAKEOVERS.containsKey(host.getUUID())) return;
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
                        contractMusic
                );

        POSSESSIONS.put(
                host.getUUID(),
                possession
        );

        SpectrumAccess.syncPossession(host, spirit);

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

        // Finish the bowed pose looking forward as control transfers.
        spirit.setYRot(host.getYRot());

        spirit.setXRot(0.0F);

        // The new controller also bows when the physical body changes hands.
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
                    if (possession.contractMusic) {
                        PacketDistributor.sendToPlayer(spirit,
                                new TukunaPossessionS2CPayload(false, false, false));
                    }
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

            if (possession.endTick == Long.MAX_VALUE && tick % 10L == 0L) {
                emitIndefiniteAura(spirit, tick, false);
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
                        false, false, false
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

    private static int effectiveTukunaFingers(
            ServerPlayer player
    ) {
        Possession possession =
                possessionForSpirit(
                        player.getUUID()
                );

        if (possession != null) {
            return possession.fingers;
        }

        if (isGhost(player)) {
            ServerPlayer host =
                    hostForSpirit(
                            player.server,
                            player.getUUID()
                    );

            return host == null
                    ? 0
                    : fingerCount(host);
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
                boolean contractMusic
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
        }
    }
}
