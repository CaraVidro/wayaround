package net.caravidro.wayaround.justice;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.content.WayAroundContent;
import net.caravidro.wayaround.network.JusticeDomainVisualPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class JusticeDomainManager {

    private JusticeDomainManager() {
    }

    private static final int PLAYER_TRIAL_TICKS =
            50 * 20;

    private static final int NPC_TRIAL_TICKS =
            22 * 20;

    private static final int PREPARE_TICKS =
            34;

    private static final int COOLDOWN_TICKS =
            75 * 20;

    private static final int POCKET_BASE_X =
            6_400_000;

    private static final int POCKET_BASE_Z =
            -6_400_000;

    private static final int POCKET_SPACING =
            96;

    private static final double AUTO_TARGET_RADIUS =
            12.0;

    private static final double VISUAL_RANGE =
            160.0;

    private static final Map<UUID, Trial> ACTIVE =
            new HashMap<>();

    private static final Map<UUID, UUID> PARTICIPANT_TO_OWNER =
            new HashMap<>();

    private static final Map<UUID, Long> COOLDOWN =
            new HashMap<>();

    public static boolean beginTrialNearest(
            ServerPlayer owner
    ) {
        if (!hasSpectrum(
                owner
        )) {
            return false;
        }

        AABB box =
                owner.getBoundingBox()
                        .inflate(
                                AUTO_TARGET_RADIUS
                        );

        LivingEntity nearest =
                null;

        double nearestDistance =
                Double.MAX_VALUE;

        for (LivingEntity candidate :
                owner.serverLevel()
                        .getEntitiesOfClass(
                                LivingEntity.class,
                                box,
                                entity ->
                                        entity.isAlive()
                                                && entity != owner
                                                && !(entity instanceof ArmorStand)
                                                && !isJudge(
                                                entity
                                        )
                        )) {

            double distance =
                    owner.distanceToSqr(
                            candidate
                    );

            if (distance
                    < nearestDistance) {

                nearestDistance =
                        distance;

                nearest =
                        candidate;
            }
        }

        if (nearest == null) {
            owner.displayClientMessage(
                    Component.literal(
                            "Spectrum da Justiça: nenhum ser vivo próximo para julgar."
                    ),
                    true
            );

            return false;
        }

        return beginTrial(
                owner,
                nearest
        );
    }

    public static boolean beginTrial(
            ServerPlayer owner,
            LivingEntity defendant
    ) {
        if (owner == defendant
                || !hasSpectrum(
                owner
        )) {
            return false;
        }

        if (PARTICIPANT_TO_OWNER.containsKey(
                owner.getUUID()
        )
                || PARTICIPANT_TO_OWNER.containsKey(
                defendant.getUUID()
        )) {

            owner.displayClientMessage(
                    Component.literal(
                            "Já existe um julgamento envolvendo um de vocês."
                    ),
                    true
            );

            return false;
        }

        long tick =
                owner.server
                        .getTickCount();

        long readyAt =
                COOLDOWN.getOrDefault(
                        owner.getUUID(),
                        0L
                );

        if (tick < readyAt) {
            owner.displayClientMessage(
                    Component.literal(
                            "O tribunal ainda não pode ser reaberto por "
                                    + Math.max(
                                    1L,
                                    (readyAt - tick + 19L)
                                            / 20L
                            )
                                    + "s."
                    ),
                    true
            );

            return false;
        }

        ServerLevel level =
                owner.serverLevel();

        if (defendant.level()
                != level
                || owner.distanceToSqr(
                defendant
        ) > AUTO_TARGET_RADIUS
                * AUTO_TARGET_RADIUS) {

            owner.displayClientMessage(
                    Component.literal(
                            "O acusado precisa estar próximo."
                    ),
                    true
            );

            return false;
        }

        int floorY =
                Math.max(
                        level.getMinBuildHeight() + 20,
                        level.getMaxBuildHeight() - 34
                );

        Vec3 pocketCenter =
                pocketCenter(
                        owner.getUUID(),
                        floorY + 1
                );

        Vec3 ownerStand =
                pocketCenter.add(
                        -3.6,
                        0.0,
                        3.0
                );

        Vec3 defendantStand =
                pocketCenter.add(
                        3.6,
                        0.0,
                        3.0
                );

        EntityState ownerReturn =
                EntityState.capture(
                        owner,
                        ownerStand
                );

        EntityState defendantReturn =
                EntityState.capture(
                        defendant,
                        defendantStand
                );

        JusticeSenseData data =
                JusticeSenseData.get(
                        owner.server
                );

        List<JusticeIncident> evidence =
                data.recent(
                        defendant.getUUID(),
                        10
                );

        boolean npc =
                !(defendant
                        instanceof ServerPlayer);

        int trialTicks =
                npc
                        ? NPC_TRIAL_TICKS
                        : PLAYER_TRIAL_TICKS;

        Vec3 exteriorCenter =
                owner.position()
                        .add(
                                defendant.position()
                        )
                        .scale(
                                0.5
                        );

        float exteriorRadius =
                (float) Math.max(
                        4.25,
                        Math.sqrt(
                                owner.distanceToSqr(
                                        defendant
                                )
                        )
                                * 0.5
                                + 2.75
                );

        Trial trial =
                new Trial(
                        owner.getUUID(),
                        defendant.getUUID(),
                        defendant.getDisplayName()
                                .getString(),
                        npc,
                        level.dimension(),
                        exteriorCenter,
                        exteriorRadius,
                        pocketCenter,
                        floorY,
                        ownerReturn,
                        defendantReturn,
                        evidence,
                        data.credibility(
                                owner.getUUID()
                        ),
                        npc
                                ? 0.12F
                                : data.credibility(
                                defendant.getUUID()
                        ),
                        tick + PREPARE_TICKS,
                        tick
                                + PREPARE_TICKS
                                + trialTicks
                );

        ACTIVE.put(
                owner.getUUID(),
                trial
        );

        PARTICIPANT_TO_OWNER.put(
                owner.getUUID(),
                owner.getUUID()
        );

        PARTICIPANT_TO_OWNER.put(
                defendant.getUUID(),
                owner.getUUID()
        );

        COOLDOWN.put(
                owner.getUUID(),
                trial.endsAt
                        + COOLDOWN_TICKS
        );

        trial.freezeDefendant(
                defendant
        );

        buildCourt(
                level,
                trial
        );

        spawnJudge(
                level,
                trial
        );

        PacketDistributor.sendToPlayersNear(
                level,
                null,
                exteriorCenter.x,
                exteriorCenter.y,
                exteriorCenter.z,
                VISUAL_RANGE,
                JusticeDomainVisualPayload.open(
                        owner.getUUID(),
                        exteriorCenter,
                        exteriorRadius,
                        PREPARE_TICKS
                                + trialTicks
                )
        );

        level.playSound(
                null,
                BlockPos.containing(
                        exteriorCenter
                ),
                SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS,
                1.35F,
                0.62F
        );

        level.playSound(
                null,
                BlockPos.containing(
                        exteriorCenter
                ),
                SoundEvents.ANVIL_LAND,
                SoundSource.PLAYERS,
                1.75F,
                0.66F
        );

        level.playSound(
                null,
                BlockPos.containing(
                        exteriorCenter
                ),
                SoundEvents.ANVIL_USE,
                SoundSource.PLAYERS,
                1.15F,
                0.82F
        );

        owner.sendSystemMessage(
                Component.literal(
                        "EXPANSÃO DE DOMÍNIO — TRIBUNAL DA JUSTIÇA"
                )
        );

        owner.displayClientMessage(
                Component.literal(
                        "O tribunal está se formando..."
                ),
                true
        );

        if (defendant
                instanceof ServerPlayer playerDefendant) {

            playerDefendant.displayClientMessage(
                    Component.literal(
                            "O espaço ao redor está ficando branco..."
                    ),
                    true
            );
        }

        return true;
    }

    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        long tick =
                server.getTickCount();

        Iterator<Map.Entry<UUID, Trial>> iterator =
                ACTIVE.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            Trial trial =
                    iterator.next()
                            .getValue();

            ServerLevel level =
                    server.getLevel(
                            trial.dimension
                    );

            ServerPlayer owner =
                    server.getPlayerList()
                            .getPlayer(
                                    trial.owner
                            );

            LivingEntity defendant =
                    findLiving(
                            server,
                            trial.defendant,
                            level
                    );

            if (level == null
                    || owner == null
                    || defendant == null
                    || !owner.isAlive()
                    || !defendant.isAlive()) {

                close(
                        server,
                        trial,
                        false,
                        "O processo foi interrompido."
                );

                iterator.remove();
                continue;
            }

            if (!trial.entered) {
                trial.freezeDefendant(
                        defendant
                );

                if (tick >= trial.enterAt) {
                    enterTrial(
                            server,
                            level,
                            trial,
                            owner,
                            defendant
                    );
                }

                continue;
            }

            trial.keepInside(
                    owner,
                    defendant,
                    level
            );

            if (trial.confession
                    || tick >= trial.endsAt) {

                float threshold =
                        trial.npc
                                ? 0.72F
                                : 1.05F;

                boolean guilty =
                        trial.confession
                                || trial.score(
                                tick
                        ) >= threshold;

                close(
                        server,
                        trial,
                        guilty,
                        guilty
                                ? "CULPADO."
                                : "Provas insuficientes. O domínio falhou."
                );

                iterator.remove();
                continue;
            }

            if (tick >= trial.nextCommentAt) {
                trial.nextCommentAt =
                        tick + 8L * 20L;

                float score =
                        trial.score(
                                tick
                        );

                String comment =
                        trial.npc
                                ? score >= 0.72F
                                ? "A defesa do acusado permanece... intelectualmente limitada."
                                : "Mesmo para uma criatura, ainda preciso de alguma narrativa."
                                : score >= 0.85F
                                ? "As peças começam a formar uma narrativa consistente."
                                : score >= 0.45F
                                ? "Ainda há contradições. Continuem."
                                : "Até agora, vejo mais alegação do que demonstração.";

                sendJudge(
                        owner,
                        defendant,
                        comment
                );
            }
        }

        COOLDOWN.entrySet()
                .removeIf(
                        entry ->
                                entry.getValue()
                                        <= tick
                );
    }

    public static void onServerChat(
            ServerChatEvent event
    ) {
        ServerPlayer speaker =
                event.getPlayer();

        Trial trial =
                trialFor(
                        speaker.getUUID()
                );

        if (trial != null) {
            if (trial.entered) {
                trial.acceptStatement(
                        speaker,
                        event.getRawText()
                );
            }

            return;
        }

        if (hasSpectrum(
                speaker
        )
                && looksLikeDomainPhrase(
                event.getRawText()
        )) {

            event.setCanceled(
                    true
            );

            beginTrialNearest(
                    speaker
            );
        }
    }

    public static void onVoiceStatement(
            ServerPlayer speaker,
            String transcript
    ) {
        if (transcript == null
                || transcript.isBlank()) {
            return;
        }

        Trial trial =
                trialFor(
                        speaker.getUUID()
                );

        if (trial != null) {
            if (trial.entered) {
                trial.acceptStatement(
                        speaker,
                        transcript
                );
            }

            return;
        }

        if (hasSpectrum(
                speaker
        )
                && looksLikeDomainPhrase(
                transcript
        )) {

            beginTrialNearest(
                    speaker
            );
        }
    }

    public static void onSoundAtEntity(
            PlayLevelSoundEvent.AtEntity event
    ) {
        if (!(event.getLevel()
                instanceof ServerLevel level)) {
            return;
        }

        Entity entity =
                event.getEntity();

        UUID ownerId =
                PARTICIPANT_TO_OWNER.get(
                        entity.getUUID()
                );

        if (ownerId == null) {
            return;
        }

        Trial trial =
                ACTIVE.get(
                        ownerId
                );

        if (trial == null
                || !trial.entered
                || !trial.npc
                || !trial.defendant.equals(
                entity.getUUID()
        )) {
            return;
        }

        long tick =
                level.getServer()
                        .getTickCount();

        if (tick < trial.nextNpcSoundAt) {
            return;
        }

        trial.nextNpcSoundAt =
                tick + 32L;

        trial.unintelligibleSounds =
                Math.min(
                        8,
                        trial.unintelligibleSounds + 1
                );

        ServerPlayer owner =
                level.getServer()
                        .getPlayerList()
                        .getPlayer(
                                trial.owner
                        );

        if (owner != null) {
            sendJudge(
                    owner,
                    entity instanceof LivingEntity living
                            ? living
                            : null,
                    trial.unintelligibleSounds == 1
                            ? "Eu... não entendi esse malandro. Não considerarei isso uma defesa."
                            : "O acusado emitiu outro som. Continuo sem reconhecer uma defesa juridicamente compreensível."
            );
        }
    }

    public static void onBlockBreak(
            BlockEvent.BreakEvent event
    ) {
        ServerPlayer player =
                event.getPlayer()
                instanceof ServerPlayer serverPlayer
                        ? serverPlayer
                        : null;

        if (player == null) {
            return;
        }

        Trial trial =
                trialFor(
                        player.getUUID()
                );

        if (trial != null
                && trial.insideCourt(
                event.getPos()
        )) {

            event.setCanceled(
                    true
            );
        }
    }

    public static void onBlockPlace(
            BlockEvent.EntityPlaceEvent event
    ) {
        if (!(event.getEntity()
                instanceof ServerPlayer player)) {
            return;
        }

        Trial trial =
                trialFor(
                        player.getUUID()
                );

        if (trial != null
                && trial.insideCourt(
                event.getPos()
        )) {

            event.setCanceled(
                    true
            );
        }
    }

    public static void clearAll(
            MinecraftServer server
    ) {
        for (Trial trial :
                new ArrayList<>(
                        ACTIVE.values()
                )) {

            close(
                    server,
                    trial,
                    false,
                    "O tribunal se desfez."
            );
        }

        ACTIVE.clear();
        PARTICIPANT_TO_OWNER.clear();
        COOLDOWN.clear();
    }

    private static void enterTrial(
            MinecraftServer server,
            ServerLevel level,
            Trial trial,
            ServerPlayer owner,
            LivingEntity defendant
    ) {
        if (trial.entered) {
            return;
        }

        trial.entered =
                true;

        PacketDistributor.sendToPlayer(
                owner,
                JusticeDomainVisualPayload.enter(
                        trial.owner
                )
        );

        if (defendant
                instanceof ServerPlayer playerDefendant) {

            PacketDistributor.sendToPlayer(
                    playerDefendant,
                    JusticeDomainVisualPayload.enter(
                            trial.owner
                    )
            );
        }

        teleport(
                owner,
                level,
                trial.ownerReturn
                        .pocketPosition,
                180.0F,
                0.0F
        );

        teleportLiving(
                defendant,
                level,
                trial.defendantReturn
                        .pocketPosition,
                180.0F,
                0.0F
        );

        trial.freezeDefendant(
                defendant
        );

        sendJudge(
                owner,
                defendant,
                "Apresentem suas versões. Eu julgo coerência, contexto, confiança e provas."
        );

        owner.sendSystemMessage(
                Component.literal(
                        "[Senso de Justiça] O arquivo de "
                                + trial.defendantName
                                + " contém "
                                + trial.evidence.size()
                                + " ocorrência(s)."
                )
        );

        if (trial.evidence.isEmpty()) {
            owner.sendSystemMessage(
                    Component.literal(
                            trial.npc
                                    ? "[Senso de Justiça] Entidade não-humana: nenhum histórico civil. O tribunal vai considerar natureza do acusado, sua argumentação e a ausência de defesa compreensível."
                                    : "[Senso de Justiça] Nenhuma prova real conhecida. Você ainda pode argumentar — ou fabricar uma alegação e tentar sustentá-la."
                    )
            );

        } else {
            int number =
                    1;

            for (JusticeIncident incident :
                    trial.evidence) {

                owner.sendSystemMessage(
                        Component.literal(
                                "[PROVA "
                                        + number++
                                        + "] "
                                        + incident.summary()
                                        + " | confiança "
                                        + Math.round(
                                        incident.confidence()
                                                * 100.0F
                                )
                                        + "%"
                        )
                );
            }
        }

        owner.sendSystemMessage(
                Component.literal(
                        "Argumente pelo chat ou pelo Voice Chat. O juiz cruza sua fala com o Senso de Justiça."
                )
        );

        if (defendant
                instanceof ServerPlayer playerDefendant) {

            playerDefendant.sendSystemMessage(
                    Component.literal(
                            "Você pode confessar, negar ou explicar pelo chat/voz. Uma confissão encerra o caso."
                    )
            );
        }

        level.playSound(
                null,
                BlockPos.containing(
                        trial.pocketCenter
                ),
                SoundEvents.TRIAL_SPAWNER_AMBIENT,
                SoundSource.BLOCKS,
                0.9F,
                0.72F
        );
    }

    @Nullable
    private static Trial trialFor(
            UUID participant
    ) {
        UUID owner =
                PARTICIPANT_TO_OWNER.get(
                        participant
                );

        return owner == null
                ? null
                : ACTIVE.get(
                owner
        );
    }

    private static void close(
            MinecraftServer server,
            Trial trial,
            boolean guilty,
            String verdict
    ) {
        ServerLevel level =
                server.getLevel(
                        trial.dimension
                );

        ServerPlayer owner =
                server.getPlayerList()
                        .getPlayer(
                                trial.owner
                        );

        LivingEntity defendant =
                findLiving(
                        server,
                        trial.defendant,
                        level
                );

        if (owner != null) {
            sendJudge(
                    owner,
                    defendant,
                    verdict
            );

            if (guilty
                    && defendant != null) {

                JusticeRewardManager.grant(
                        owner,
                        defendant
                );

                owner.sendSystemMessage(
                        Component.literal(
                                "A sentença concedeu velocidade e a Lâmina da Sentença. Ela reconhece "
                                        + trial.defendantName
                                        + "."
                        )
                );
            } else {
                owner.sendSystemMessage(
                        Component.literal(
                                "Nenhuma sentença foi concedida."
                        )
                );
            }

            JusticeSenseData data =
                    JusticeSenseData.get(
                            server
                    );

            if (!trial.npc
                    && defendant
                    instanceof ServerPlayer playerDefendant) {

                if (guilty) {
                    if (trial.confession) {
                        data.adjustCredibility(
                                playerDefendant.getUUID(),
                                0.025F
                        );

                    } else if (trial.supportedClaims > 0) {
                        data.adjustCredibility(
                                owner.getUUID(),
                                0.018F
                        );
                    }

                    if (trial.denials > 0
                            && trial.realEvidenceStrength(
                            server.getTickCount()
                    ) > 0.8F) {

                        data.adjustCredibility(
                                playerDefendant.getUUID(),
                                -0.025F
                        );
                    }

                } else if (trial.unsupportedClaims
                        > trial.supportedClaims) {

                    data.adjustCredibility(
                            owner.getUUID(),
                            -0.03F
                    );
                }
            }
        }

        if (level != null) {
            if (owner != null) {
                trial.ownerReturn.restore(
                        owner,
                        level
                );
            }

            if (defendant != null) {
                trial.defendantReturn.restore(
                        defendant,
                        level
                );
            }

            if (trial.judgeId != null) {
                Entity judge =
                        level.getEntity(
                                trial.judgeId
                        );

                if (judge != null) {
                    judge.discard();
                }
            }

            for (Long packed :
                    trial.temporaryBlocks) {

                BlockPos pos =
                        BlockPos.of(
                                packed
                        );

                level.setBlock(
                        pos,
                        Blocks.AIR
                                .defaultBlockState(),
                        2
                );
            }

            PacketDistributor.sendToPlayersNear(
                    level,
                    null,
                    trial.exteriorCenter.x,
                    trial.exteriorCenter.y,
                    trial.exteriorCenter.z,
                    VISUAL_RANGE,
                    JusticeDomainVisualPayload.close(
                            trial.owner,
                            trial.exteriorCenter,
                            trial.exteriorRadius
                    )
            );
        }

        if (owner != null) {
            PacketDistributor.sendToPlayer(
                    owner,
                    JusticeDomainVisualPayload.close(
                            trial.owner,
                            trial.exteriorCenter,
                            trial.exteriorRadius
                    )
            );
        }

        if (defendant
                instanceof ServerPlayer playerDefendant) {

            PacketDistributor.sendToPlayer(
                    playerDefendant,
                    JusticeDomainVisualPayload.close(
                            trial.owner,
                            trial.exteriorCenter,
                            trial.exteriorRadius
                    )
            );
        }

        PARTICIPANT_TO_OWNER.remove(
                trial.owner
        );

        PARTICIPANT_TO_OWNER.remove(
                trial.defendant
        );
    }

    private static void spawnJudge(
            ServerLevel level,
            Trial trial
    ) {
        Villager judge =
                EntityType.VILLAGER.create(
                        level
                );

        if (judge == null) {
            return;
        }

        judge.setCustomName(
                Component.literal(
                        "Juiz"
                )
        );

        judge.setCustomNameVisible(
                true
        );

        judge.setNoAi(
                true
        );

        judge.setInvulnerable(
                true
        );

        judge.setSilent(
                true
        );

        judge.moveTo(
                trial.pocketCenter.x,
                trial.pocketCenter.y + 1.05,
                trial.pocketCenter.z - 8.0,
                0.0F,
                0.0F
        );

        level.addFreshEntity(
                judge
        );

        trial.judgeId =
                judge.getUUID();
    }

    private static void buildCourt(
            ServerLevel level,
            Trial trial
    ) {
        int cx =
                (int) Math.floor(
                        trial.pocketCenter.x
                );

        int cz =
                (int) Math.floor(
                        trial.pocketCenter.z
                );

        int y =
                trial.floorY;

        for (int x = -11;
             x <= 11;
             x++) {

            for (int z = -13;
                 z <= 13;
                 z++) {

                place(
                        level,
                        trial,
                        new BlockPos(
                                cx + x,
                                y,
                                cz + z
                        ),
                        Blocks.POLISHED_DEEPSLATE
                                .defaultBlockState()
                );

                if (Math.abs(x) == 11
                        || Math.abs(z) == 13) {

                    for (int dy = 1;
                         dy <= 5;
                         dy++) {

                        place(
                                level,
                                trial,
                                new BlockPos(
                                        cx + x,
                                        y + dy,
                                        cz + z
                                ),
                                Blocks.BLACK_CONCRETE
                                        .defaultBlockState()
                        );
                    }
                }
            }
        }

        for (int x = -11;
             x <= 11;
             x++) {

            for (int z = -13;
                 z <= 13;
                 z++) {

                place(
                        level,
                        trial,
                        new BlockPos(
                                cx + x,
                                y + 6,
                                cz + z
                        ),
                        Blocks.BLACK_CONCRETE
                                .defaultBlockState()
                );
            }
        }

        for (int x = -4;
             x <= 4;
             x++) {

            for (int z = -12;
                 z <= -8;
                 z++) {

                place(
                        level,
                        trial,
                        new BlockPos(
                                cx + x,
                                y + 1,
                                cz + z
                        ),
                        z <= -10
                                ? Blocks.QUARTZ_BLOCK
                                .defaultBlockState()
                                : Blocks.DARK_OAK_PLANKS
                                .defaultBlockState()
                );
            }
        }

        for (int xSign :
                new int[] {
                        -1,
                        1
                }) {

            int baseX =
                    xSign * 4;

            for (int x = baseX - 1;
                 x <= baseX + 1;
                 x++) {

                for (int z = 1;
                     z <= 5;
                     z++) {

                    place(
                            level,
                            trial,
                            new BlockPos(
                                    cx + x,
                                    y + 1,
                                    cz + z
                            ),
                            Blocks.SMOOTH_STONE
                                    .defaultBlockState()
                    );
                }
            }
        }

        for (int z :
                new int[] {
                        -3,
                        7
                }) {

            for (int x = -8;
                 x <= 8;
                 x++) {

                if (Math.abs(x) < 2) {
                    continue;
                }

                place(
                        level,
                        trial,
                        new BlockPos(
                                cx + x,
                                y + 1,
                                cz + z
                        ),
                        Blocks.DARK_OAK_STAIRS
                                .defaultBlockState()
                );
            }
        }
    }

    private static void place(
            ServerLevel level,
            Trial trial,
            BlockPos pos,
            BlockState state
    ) {
        trial.temporaryBlocks.add(
                pos.asLong()
        );

        level.setBlock(
                pos,
                state,
                2
        );
    }

    private static void sendJudge(
            ServerPlayer owner,
            @Nullable LivingEntity defendant,
            String message
    ) {
        Component line =
                Component.literal(
                        "Juiz: "
                                + message
                );

        owner.sendSystemMessage(
                line
        );

        if (defendant
                instanceof ServerPlayer playerDefendant
                && playerDefendant != owner) {

            playerDefendant.sendSystemMessage(
                    line
            );
        }
    }

    @Nullable
    private static LivingEntity findLiving(
            MinecraftServer server,
            UUID id,
            @Nullable ServerLevel preferred
    ) {
        ServerPlayer player =
                server.getPlayerList()
                        .getPlayer(
                                id
                        );

        if (player != null) {
            return player;
        }

        if (preferred != null) {
            Entity entity =
                    preferred.getEntity(
                            id
                    );

            if (entity
                    instanceof LivingEntity living) {
                return living;
            }
        }

        for (ServerLevel level :
                server.getAllLevels()) {

            Entity entity =
                    level.getEntity(
                            id
                    );

            if (entity
                    instanceof LivingEntity living) {
                return living;
            }
        }

        return null;
    }

    private static Vec3 pocketCenter(
            UUID owner,
            int y
    ) {
        long mixed =
                owner.getMostSignificantBits()
                        ^ Long.rotateLeft(
                        owner.getLeastSignificantBits(),
                        17
                );

        int gx =
                (int) (
                        mixed
                                & 0x3FFL
                );

        int gz =
                (int) (
                        mixed >>> 10
                                & 0x3FFL
                );

        return new Vec3(
                POCKET_BASE_X
                        + gx
                                * POCKET_SPACING,
                y + 0.05,
                POCKET_BASE_Z
                        + gz
                                * POCKET_SPACING
        );
    }

    private static void teleport(
            ServerPlayer player,
            ServerLevel level,
            Vec3 pos,
            float yaw,
            float pitch
    ) {
        player.setDeltaMovement(
                Vec3.ZERO
        );

        player.fallDistance =
                0.0F;

        player.teleportTo(
                level,
                pos.x,
                pos.y,
                pos.z,
                Set.<RelativeMovement>of(),
                yaw,
                pitch
        );
    }

    private static void teleportLiving(
            LivingEntity entity,
            ServerLevel level,
            Vec3 pos,
            float yaw,
            float pitch
    ) {
        entity.setDeltaMovement(
                Vec3.ZERO
        );

        entity.fallDistance =
                0.0F;

        if (entity
                instanceof ServerPlayer player) {

            teleport(
                    player,
                    level,
                    pos,
                    yaw,
                    pitch
            );

            return;
        }

        entity.teleportTo(
                pos.x,
                pos.y,
                pos.z
        );

        entity.setYRot(
                yaw
        );

        entity.setXRot(
                pitch
        );

        entity.setYHeadRot(
                yaw
        );
    }

    public static boolean hasSpectrum(
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

            if (stack.is(
                    WayAroundContent.JUSTICE_SPECTRUM.get()
            )) {
                return true;
            }
        }

        return false;
    }

    public static boolean looksLikeDomainPhrase(
            String text
    ) {
        String normalized =
                normalize(
                        text
                );

        return containsAny(
                normalized,
                "expansao de dominio",
                "expansao do dominio",
                "expansao dominio",
                "dominio de expansao",
                "dominio expansao"
        )
                || (
                normalized.contains(
                        "expansao"
                )
                        && normalized.contains(
                        "dominio"
                )
        );
    }

    private static boolean isJudge(
            LivingEntity entity
    ) {
        return entity.hasCustomName()
                && "juiz".equals(
                normalize(
                        entity.getCustomName()
                                .getString()
                )
        );
    }

    private static String normalize(
            String text
    ) {
        String value =
                Normalizer.normalize(
                        text.toLowerCase(
                                Locale.ROOT
                        ),
                        Normalizer.Form.NFD
                ).replaceAll(
                        "\\p{M}+",
                        ""
                );

        return value.replaceAll(
                "[^a-z0-9 _-]",
                " "
        ).replaceAll(
                "\\s+",
                " "
        ).trim();
    }

    private static EnumSet<JusticeIncident.Type> mentionedTypes(
            String text
    ) {
        EnumSet<JusticeIncident.Type> result =
                EnumSet.noneOf(
                        JusticeIncident.Type.class
                );

        if (containsAny(
                text,
                "bau",
                "roubou",
                "roubo",
                "furtou",
                "furto"
        )) {
            result.add(
                    JusticeIncident.Type.CHEST_THEFT
            );
        }

        if (containsAny(
                text,
                "cachorro",
                "cao",
                "lobo"
        )
                && containsAny(
                text,
                "matou",
                "matei",
                "morte",
                "assassin"
        )) {

            result.add(
                    JusticeIncident.Type.DOG_KILL
            );
        }

        if (containsAny(
                text,
                "player",
                "jogador",
                "pessoa",
                "ele"
        )
                && containsAny(
                text,
                "matou",
                "matei",
                "morte",
                "assassin"
        )) {

            result.add(
                    JusticeIncident.Type.PLAYER_KILL
            );
        }

        if (containsAny(
                text,
                "queimou",
                "queimei",
                "incendi",
                "fogo",
                "casa"
        )) {

            result.add(
                    JusticeIncident.Type.ARSON
            );
        }

        return result;
    }

    private static boolean containsAny(
            String text,
            String... needles
    ) {
        for (String needle :
                needles) {

            if (text.contains(
                    needle
            )) {
                return true;
            }
        }

        return false;
    }

    private static float coherence(
            String normalized,
            List<JusticeIncident> evidence
    ) {
        if (normalized.isBlank()) {
            return 0.0F;
        }

        float score =
                0.12F;

        int length =
                normalized.length();

        if (length >= 18) score += 0.16F;
        if (length >= 42) score += 0.15F;
        if (length >= 80) score += 0.10F;

        if (normalized.matches(
                ".*\\d+.*"
        )) {
            score += 0.12F;
        }

        if (containsAny(
                normalized,
                "porque",
                "quando",
                "depois",
                "antes",
                "entao",
                "por isso",
                "ai",
                "dai"
        )) {
            score += 0.15F;
        }

        if (containsAny(
                normalized,
                "coordenada",
                "perto",
                "longe",
                "casa",
                "bau",
                "fogo",
                "ontem",
                "agora"
        )) {
            score += 0.10F;
        }

        for (JusticeIncident incident :
                evidence) {

            String victim =
                    normalize(
                            incident.victimName()
                    );

            if (!victim.isBlank()
                    && normalized.contains(
                    victim
            )) {
                score += 0.12F;
                break;
            }
        }

        return Math.min(
                1.0F,
                score
        );
    }

    private static boolean confession(
            String text
    ) {
        return containsAny(
                text,
                "confesso",
                "eu fiz",
                "fui eu",
                "eu matei",
                "matei ele",
                "eu roubei",
                "roubei o bau",
                "eu queimei",
                "queimei a casa"
        );
    }

    private static boolean denial(
            String text
    ) {
        return containsAny(
                text,
                "nao fui",
                "nao fiz",
                "nao matei",
                "nao roubei",
                "nao queimei",
                "sou inocente",
                "mentira"
        );
    }

    private static float typeWeight(
            JusticeIncident.Type type
    ) {
        return switch (type) {
            case PLAYER_KILL -> 1.0F;
            case ARSON -> 0.92F;
            case DOG_KILL -> 0.82F;
            case CHEST_THEFT -> 0.72F;
        };
    }

    private static final class Trial {

        private final UUID owner;
        private final UUID defendant;
        private final String defendantName;
        private final boolean npc;
        private final ResourceKey<Level> dimension;
        private final Vec3 exteriorCenter;
        private final float exteriorRadius;
        private final Vec3 pocketCenter;
        private final int floorY;
        private final EntityState ownerReturn;
        private final EntityState defendantReturn;
        private final List<JusticeIncident> evidence;
        private final float ownerTrust;
        private final float defendantTrust;
        private final long enterAt;
        private final long endsAt;

        private final Set<Long> temporaryBlocks =
                new HashSet<>();

        private final Set<JusticeIncident.Type> supportedPresented =
                EnumSet.noneOf(
                        JusticeIncident.Type.class
                );

        private final Set<String> repeatedStatements =
                new LinkedHashSet<>();

        @Nullable
        private UUID judgeId;

        private float argumentScore;
        private float fabricatedScore;
        private float defenseScore;
        private float contradictionScore;

        private int supportedClaims;
        private int unsupportedClaims;
        private int denials;
        private int unintelligibleSounds;

        private boolean confession;
        private boolean entered;

        private long nextCommentAt;
        private long nextNpcSoundAt;

        private Trial(
                UUID owner,
                UUID defendant,
                String defendantName,
                boolean npc,
                ResourceKey<Level> dimension,
                Vec3 exteriorCenter,
                float exteriorRadius,
                Vec3 pocketCenter,
                int floorY,
                EntityState ownerReturn,
                EntityState defendantReturn,
                List<JusticeIncident> evidence,
                float ownerTrust,
                float defendantTrust,
                long enterAt,
                long endsAt
        ) {
            this.owner = owner;
            this.defendant = defendant;
            this.defendantName = defendantName;
            this.npc = npc;
            this.dimension = dimension;
            this.exteriorCenter = exteriorCenter;
            this.exteriorRadius = exteriorRadius;
            this.pocketCenter = pocketCenter;
            this.floorY = floorY;
            this.ownerReturn = ownerReturn;
            this.defendantReturn = defendantReturn;
            this.evidence = evidence;
            this.ownerTrust = ownerTrust;
            this.defendantTrust = defendantTrust;
            this.enterAt = enterAt;
            this.endsAt = endsAt;
            this.nextCommentAt =
                    enterAt + 7L * 20L;
        }

        private void acceptStatement(
                ServerPlayer speaker,
                String raw
        ) {
            String text =
                    normalize(
                            raw
                    );

            if (text.isBlank()) {
                return;
            }

            float coherence =
                    JusticeDomainManager.coherence(
                            text,
                            evidence
                    );

            if (!repeatedStatements.add(
                    speaker.getUUID()
                            + "|"
                            + text
            )) {

                coherence *=
                        0.22F;
            }

            if (speaker.getUUID()
                    .equals(
                            owner
                    )) {

                EnumSet<JusticeIncident.Type> mentioned =
                        mentionedTypes(
                                text
                        );

                if (mentioned.isEmpty()) {
                    argumentScore +=
                            (
                                    npc
                                            ? 0.20F
                                            : 0.035F
                            )
                                    * coherence
                                    * (
                                    0.70F
                                            + ownerTrust
                            );
                    return;
                }

                for (JusticeIncident.Type type :
                        mentioned) {

                    JusticeIncident matching =
                            newestEvidence(
                                    type
                            );

                    if (matching != null) {
                        float recency =
                                recency(
                                        matching,
                                        speaker.serverLevel()
                                                .getGameTime()
                                );

                        float value =
                                typeWeight(
                                        type
                                )
                                        * matching.confidence()
                                        * recency
                                        * (
                                        0.55F
                                                + coherence
                                                        * 0.45F
                                );

                        if (supportedPresented.add(
                                type
                        )) {
                            argumentScore +=
                                    value;

                            supportedClaims++;
                        } else {
                            argumentScore +=
                                    value
                                            * 0.16F;
                        }

                        speaker.displayClientMessage(
                                Component.literal(
                                        "Juiz: Essa alegação encontra respaldo no registro."
                                ),
                                false
                        );

                    } else {
                        unsupportedClaims++;

                        fabricatedScore +=
                                0.18F
                                        + ownerTrust
                                                * 0.24F
                                        + coherence
                                                * 0.26F;

                        speaker.displayClientMessage(
                                Component.literal(
                                        "Juiz: Não existe registro direto disso. Vou julgar a consistência da alegação."
                                ),
                                false
                        );
                    }
                }

                return;
            }

            if (!speaker.getUUID()
                    .equals(
                            defendant
                    )) {
                return;
            }

            if (JusticeDomainManager.confession(
                    text
            )) {

                confession =
                        true;

                speaker.sendSystemMessage(
                        Component.literal(
                                "Juiz: Confissão registrada."
                        )
                );

                return;
            }

            if (JusticeDomainManager.denial(
                    text
            )) {
                denials++;

                float realStrength =
                        realEvidenceStrength(
                                speaker.serverLevel()
                                        .getGameTime()
                        );

                defenseScore +=
                        0.12F
                                + defendantTrust
                                        * 0.22F
                                + coherence
                                        * 0.18F;

                if (realStrength > 0.65F) {
                    contradictionScore +=
                            Math.min(
                                    0.35F,
                                    realStrength
                                            * 0.20F
                            );
                }

                return;
            }

            defenseScore +=
                    coherence
                            * (
                            0.10F
                                    + defendantTrust
                                            * 0.16F
                    );
        }

        private float score(
                long tick
        ) {
            float hidden =
                    realEvidenceStrength(
                            tick
                    )
                            * 0.12F;

            float unsupportedPenalty =
                    Math.max(
                            0,
                            unsupportedClaims
                                    - 2
                    )
                            * 0.07F;

            float npcPresumption =
                    npc
                            ? 0.78F
                                    + Math.min(
                                    0.10F,
                                    unintelligibleSounds
                                            * 0.018F
                            )
                            : 0.0F;

            return npcPresumption
                    + hidden
                    + argumentScore
                    + fabricatedScore
                            * 0.68F
                    + contradictionScore
                    - defenseScore
                    - unsupportedPenalty;
        }

        private float realEvidenceStrength(
                long tick
        ) {
            float sum =
                    0.0F;

            for (JusticeIncident incident :
                    evidence) {

                sum +=
                        typeWeight(
                                incident.type()
                        )
                                * incident.confidence()
                                * recency(
                                incident,
                                tick
                        );
            }

            return sum;
        }

        @Nullable
        private JusticeIncident newestEvidence(
                JusticeIncident.Type type
        ) {
            for (JusticeIncident incident :
                    evidence) {

                if (incident.type()
                        == type) {
                    return incident;
                }
            }

            return null;
        }

        private static float recency(
                JusticeIncident incident,
                long now
        ) {
            long age =
                    Math.max(
                            0L,
                            now
                                    - incident.gameTime()
                    );

            if (age <= 10L * 60L * 20L) return 1.0F;
            if (age <= 30L * 60L * 20L) return 0.86F;
            if (age <= 2L * 60L * 60L * 20L) return 0.66F;
            return 0.46F;
        }

        private void freezeDefendant(
                LivingEntity defendantEntity
        ) {
            defendantEntity.setDeltaMovement(
                    Vec3.ZERO
            );

            defendantEntity.fallDistance =
                    0.0F;

            if (defendantEntity
                    instanceof Mob mob) {
                mob.setNoAi(
                        true
                );
            }
        }

        private void keepInside(
                ServerPlayer ownerPlayer,
                LivingEntity defendantEntity,
                ServerLevel level
        ) {
            keepParticipant(
                    ownerPlayer,
                    ownerReturn.pocketPosition,
                    level,
                    false
            );

            keepParticipant(
                    defendantEntity,
                    defendantReturn.pocketPosition,
                    level,
                    true
            );
        }

        private void keepParticipant(
                LivingEntity entity,
                Vec3 fallback,
                ServerLevel level,
                boolean lock
        ) {
            if (lock) {
                teleportLiving(
                        entity,
                        level,
                        fallback,
                        entity.getYRot(),
                        entity.getXRot()
                );

                if (entity
                        instanceof Mob mob) {
                    mob.setNoAi(
                            true
                    );
                }

                return;
            }

            double dx =
                    Math.abs(
                            entity.getX()
                                    - pocketCenter.x
                    );

            double dz =
                    Math.abs(
                            entity.getZ()
                                    - pocketCenter.z
                    );

            if (entity.level()
                    != level
                    || dx > 10.2
                    || dz > 12.2
                    || entity.getY()
                    < floorY) {

                teleportLiving(
                        entity,
                        level,
                        fallback,
                        180.0F,
                        0.0F
                );
            }
        }

        private boolean insideCourt(
                BlockPos pos
        ) {
            return Math.abs(
                    pos.getX()
                            - pocketCenter.x
            ) <= 12.0
                    && Math.abs(
                    pos.getZ()
                            - pocketCenter.z
            ) <= 14.0
                    && pos.getY()
                    >= floorY
                    && pos.getY()
                    <= floorY + 7;
        }
    }

    private static final class EntityState {

        private final Vec3 returnPosition;
        private final float returnYaw;
        private final float returnPitch;
        private final Vec3 pocketPosition;
        private final boolean wasInvulnerable;
        private final boolean wasNoAi;

        private EntityState(
                Vec3 returnPosition,
                float returnYaw,
                float returnPitch,
                Vec3 pocketPosition,
                boolean wasInvulnerable,
                boolean wasNoAi
        ) {
            this.returnPosition = returnPosition;
            this.returnYaw = returnYaw;
            this.returnPitch = returnPitch;
            this.pocketPosition = pocketPosition;
            this.wasInvulnerable = wasInvulnerable;
            this.wasNoAi = wasNoAi;
        }

        private static EntityState capture(
                LivingEntity entity,
                Vec3 pocketPosition
        ) {
            return new EntityState(
                    entity.position(),
                    entity.getYRot(),
                    entity.getXRot(),
                    pocketPosition,
                    entity.isInvulnerable(),
                    entity instanceof Mob mob
                            && mob.isNoAi()
            );
        }

        private void restore(
                LivingEntity entity,
                ServerLevel level
        ) {
            entity.setInvulnerable(
                    wasInvulnerable
            );

            if (entity
                    instanceof Mob mob) {
                mob.setNoAi(
                        wasNoAi
                );
            }

            teleportLiving(
                    entity,
                    level,
                    returnPosition,
                    returnYaw,
                    returnPitch
            );
        }
    }
}
