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
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class JusticeDomainManager {

    private JusticeDomainManager() {
    }

    private static final int TRIAL_TICKS =
            50 * 20;

    private static final int COOLDOWN_TICKS =
            75 * 20;

    private static final int POCKET_BASE_X =
            6_400_000;

    private static final int POCKET_BASE_Z =
            -6_400_000;

    private static final int POCKET_SPACING =
            96;

    private static final Map<UUID, Trial> ACTIVE =
            new HashMap<>();

    private static final Map<UUID, UUID> PARTICIPANT_TO_OWNER =
            new HashMap<>();

    private static final Map<UUID, Long> COOLDOWN =
            new HashMap<>();

    public static boolean beginTrial(
            ServerPlayer owner,
            ServerPlayer defendant
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

        if (defendant.serverLevel()
                != level
                || owner.distanceToSqr(
                defendant
        ) > 10.0 * 10.0) {

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

        Vec3 center =
                pocketCenter(
                        owner.getUUID(),
                        floorY + 1
                );

        Vec3 ownerStand =
                center.add(
                        -3.6,
                        0.0,
                        3.0
                );

        Vec3 defendantStand =
                center.add(
                        3.6,
                        0.0,
                        3.0
                );

        ParticipantState ownerReturn =
                ParticipantState.capture(
                        owner,
                        ownerStand
                );

        ParticipantState defendantReturn =
                ParticipantState.capture(
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

        Trial trial =
                new Trial(
                        owner.getUUID(),
                        defendant.getUUID(),
                        level.dimension(),
                        center,
                        floorY,
                        ownerReturn,
                        defendantReturn,
                        evidence,
                        data.credibility(
                                owner.getUUID()
                        ),
                        data.credibility(
                                defendant.getUUID()
                        ),
                        tick + TRIAL_TICKS
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
                tick
                        + TRIAL_TICKS
                        + COOLDOWN_TICKS
        );

        buildCourt(
                level,
                trial
        );

        spawnJudge(
                level,
                trial
        );

        teleport(
                owner,
                level,
                ownerStand,
                180.0F,
                0.0F
        );

        teleport(
                defendant,
                level,
                defendantStand,
                180.0F,
                0.0F
        );

        owner.serverLevel()
                .playSound(
                        null,
                        owner.blockPosition(),
                        SoundEvents.END_PORTAL_SPAWN,
                        SoundSource.PLAYERS,
                        1.3F,
                        0.62F
                );

        owner.sendSystemMessage(
                Component.literal(
                        "EXPANSÃO DE DOMÍNIO — TRIBUNAL DA JUSTIÇA"
                )
        );

        defendant.sendSystemMessage(
                Component.literal(
                        "Você foi levado ao Tribunal da Justiça."
                )
        );

        sendJudge(
                owner,
                defendant,
                "Apresentem suas versões. Eu não julgo palavras isoladas; julgo coerência, contexto, confiança e provas."
        );

        owner.sendSystemMessage(
                Component.literal(
                        "[Senso de Justiça] O arquivo do acusado contém "
                                + evidence.size()
                                + " ocorrência(s)."
                )
        );

        if (evidence.isEmpty()) {
            owner.sendSystemMessage(
                    Component.literal(
                            "[Senso de Justiça] Nenhuma prova real conhecida. Você ainda pode argumentar — ou fabricar uma alegação e tentar sustentá-la."
                    )
            );
        } else {
            int number =
                    1;

            for (JusticeIncident incident :
                    evidence) {

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
                        "Fale no chat para argumentar. Alegações que não existem no arquivo contam como prova fabricada e podem ou não convencer o juiz."
                )
        );

        defendant.sendSystemMessage(
                Component.literal(
                        "Você pode confessar, negar ou explicar pelo chat. Uma confissão encerra o caso."
                )
        );

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

            ServerPlayer defendant =
                    server.getPlayerList()
                            .getPlayer(
                                    trial.defendant
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

            trial.keepInside(
                    owner,
                    defendant,
                    level
            );

            if (trial.confession
                    || tick >= trial.endsAt) {

                boolean guilty =
                        trial.confession
                                || trial.score(
                                tick
                        ) >= 1.05F;

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
                        tick + 15L * 20L;

                float score =
                        trial.score(
                                tick
                        );

                String comment =
                        score >= 0.85F
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

        UUID ownerId =
                PARTICIPANT_TO_OWNER.get(
                        speaker.getUUID()
                );

        if (ownerId == null) {
            return;
        }

        Trial trial =
                ACTIVE.get(
                        ownerId
                );

        if (trial == null) {
            return;
        }

        trial.acceptStatement(
                speaker,
                event.getRawText()
        );
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

        ServerPlayer defendant =
                server.getPlayerList()
                        .getPlayer(
                                trial.defendant
                        );

        if (owner != null
                && defendant != null) {

            sendJudge(
                    owner,
                    defendant,
                    verdict
            );

            JusticeSenseData data =
                    JusticeSenseData.get(
                            server
                    );

            if (guilty) {
                JusticeRewardManager.grant(
                        owner,
                        defendant
                );

                owner.sendSystemMessage(
                        Component.literal(
                                "A sentença concedeu velocidade e a Lâmina da Sentença. Ela reconhece "
                                        + defendant.getGameProfile()
                                        .getName()
                                        + "."
                        )
                );

                if (trial.confession) {
                    data.adjustCredibility(
                            defendant.getUUID(),
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
                            defendant.getUUID(),
                            -0.025F
                    );
                }
            } else {
                owner.sendSystemMessage(
                        Component.literal(
                                "Nenhuma sentença foi concedida."
                        )
                );

                if (trial.unsupportedClaims
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
                var judge =
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
                trial.center.x,
                trial.center.y + 1.05,
                trial.center.z - 8.0,
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
                        trial.center.x
                );

        int cz =
                (int) Math.floor(
                        trial.center.z
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
            ServerPlayer defendant,
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

        if (defendant != owner) {
            defendant.sendSystemMessage(
                    line
            );
        }
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

    private static boolean hasSpectrum(
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
                "por isso"
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
                "fogo"
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
        private final ResourceKey<Level> dimension;
        private final Vec3 center;
        private final int floorY;
        private final ParticipantState ownerReturn;
        private final ParticipantState defendantReturn;
        private final List<JusticeIncident> evidence;
        private final float ownerTrust;
        private final float defendantTrust;
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

        private boolean confession;

        private long nextCommentAt;

        private Trial(
                UUID owner,
                UUID defendant,
                ResourceKey<Level> dimension,
                Vec3 center,
                int floorY,
                ParticipantState ownerReturn,
                ParticipantState defendantReturn,
                List<JusticeIncident> evidence,
                float ownerTrust,
                float defendantTrust,
                long endsAt
        ) {
            this.owner =
                    owner;
            this.defendant =
                    defendant;
            this.dimension =
                    dimension;
            this.center =
                    center;
            this.floorY =
                    floorY;
            this.ownerReturn =
                    ownerReturn;
            this.defendantReturn =
                    defendantReturn;
            this.evidence =
                    evidence;
            this.ownerTrust =
                    ownerTrust;
            this.defendantTrust =
                    defendantTrust;
            this.endsAt =
                    endsAt;
            this.nextCommentAt =
                    endsAt - TRIAL_TICKS + 15L * 20L;
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
                            0.035F
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

            return hidden
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

        private void keepInside(
                ServerPlayer ownerPlayer,
                ServerPlayer defendantPlayer,
                ServerLevel level
        ) {
            keepParticipant(
                    ownerPlayer,
                    ownerReturn.pocketPosition,
                    level
            );

            keepParticipant(
                    defendantPlayer,
                    defendantReturn.pocketPosition,
                    level
            );
        }

        private void keepParticipant(
                ServerPlayer player,
                Vec3 fallback,
                ServerLevel level
        ) {
            double dx =
                    Math.abs(
                            player.getX()
                                    - center.x
                    );

            double dz =
                    Math.abs(
                            player.getZ()
                                    - center.z
                    );

            if (player.serverLevel()
                    != level
                    || dx > 10.2
                    || dz > 12.2
                    || player.getY()
                    < floorY) {

                teleport(
                        player,
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
                            - center.x
            ) <= 12.0
                    && Math.abs(
                    pos.getZ()
                            - center.z
            ) <= 14.0
                    && pos.getY()
                    >= floorY
                    && pos.getY()
                    <= floorY + 7;
        }
    }

    private static final class ParticipantState {

        private final Vec3 returnPosition;
        private final float returnYaw;
        private final float returnPitch;
        private final Vec3 pocketPosition;

        private ParticipantState(
                Vec3 returnPosition,
                float returnYaw,
                float returnPitch,
                Vec3 pocketPosition
        ) {
            this.returnPosition =
                    returnPosition;
            this.returnYaw =
                    returnYaw;
            this.returnPitch =
                    returnPitch;
            this.pocketPosition =
                    pocketPosition;
        }

        private static ParticipantState capture(
                ServerPlayer player,
                Vec3 pocketPosition
        ) {
            return new ParticipantState(
                    player.position(),
                    player.getYRot(),
                    player.getXRot(),
                    pocketPosition
            );
        }

        private void restore(
                ServerPlayer player,
                ServerLevel level
        ) {
            teleport(
                    player,
                    level,
                    returnPosition,
                    returnYaw,
                    returnPitch
            );
        }
    }
}
