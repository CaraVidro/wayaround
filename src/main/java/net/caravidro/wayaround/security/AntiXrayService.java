package net.caravidro.wayaround.security;

import com.mojang.brigadier.arguments.StringArgumentType;
import java.security.SecureRandom;
import java.util.*;
import net.caravidro.wayaround.WayAround;
import net.minecraft.commands.Commands;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.tags.BlockTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** No ore scans, remote chunks, continuous image analysis, or client-selected player identities. */
@EventBusSubscriber(modid=WayAround.MODID)
public final class AntiXrayService {
    private static final SecureRandom RANDOM=new SecureRandom();
    private static final Map<UUID,Session> SESSIONS=new HashMap<>();
    private static int cursor, oreBudget=64;
    private static final class Session {
        long nonce, sent, next, lastAccepted, unansweredSince;
        boolean pending, previousStrong, missingNotified, advisoryNotified;
        String notifiedFingerprint="";
        int reviewStage;
    }
    private static boolean enabled(MinecraftServer server) { return AntiXrayConfig.ENABLED.get() && !server.isSingleplayer(); }

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !enabled(player.server)) return;
        AntiXrayData data=AntiXrayData.get(player.server);
        AntiXrayData.Case c=data.get(player.getUUID());
        c.name=player.getGameProfile().getName();
        if (c.history.banned && !player.server.getPlayerList().getBans().isBanned(player.getGameProfile())) {
            c.history.pardoned(); audit(player,"PARDON vanilla ban removed; escalation reset; historical audit retained");
        }
        SESSIONS.put(player.getUUID(),new Session()); data.setDirty();
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { SESSIONS.remove(event.getEntity().getUUID()); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { SESSIONS.clear(); cursor=0; }

    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        oreBudget=64;
        MinecraftServer server=event.getServer();
        if (!enabled(server)) { SESSIONS.clear(); return; }
        List<ServerPlayer> players=server.getPlayerList().getPlayers();
        int size=players.size(); if(size==0)return;
        long now=System.nanoTime();
        // Round-robin bounded polling; eight-byte challenges at most once per player/five seconds.
        for(int i=0;i<Math.min(128,size);i++) {
            ServerPlayer player=players.get(Math.floorMod(cursor++,size));
            Session s=SESSIONS.computeIfAbsent(player.getUUID(),id->new Session());
            if(s.unansweredSince!=0 && now-s.unansweredSince>30_000_000_000L && !s.missingNotified) {
                s.missingNotified=true; s.previousStrong=false;
                audit(player,"UNKNOWN resource audit unavailable; no x-ray conviction");
                owner(player,"Relatório de recursos indisponível. Isto não é prova de x-ray.");
            }
            if(now<s.next)continue;
            s.nonce=RANDOM.nextLong(); s.sent=now; s.pending=true; s.next=now+5_000_000_000L;
            if(s.unansweredSince==0)s.unansweredSince=now;
            PacketDistributor.sendToPlayer(player,new XrayChallengePayload(s.nonce));
        }
    }

    public static boolean accepts(boolean pending,long expected,long received,long age,boolean valid) {
        return pending && expected==received && age>=0 && age<=15_000_000_000L && valid;
    }
    public static void receive(ServerPlayer player,XrayReportPayload report) {
        if(!enabled(player.server))return;
        Session s=SESSIONS.get(player.getUUID()); if(s==null)return;
        long now=System.nanoTime(); ResourceEvidence evidence=report.evidence();
        if(!accepts(s.pending,s.nonce,report.nonce(),now-s.sent,evidence.valid()))return;
        s.pending=false;s.unansweredSince=0;s.missingNotified=false;
        AntiXrayData data=AntiXrayData.get(player.server); AntiXrayData.Case c=data.get(player.getUUID());
        boolean approved=data.approved(evidence.fingerprint()) || AntiXrayConfig.APPROVED.get().contains(evidence.fingerprint());
        double elapsed=s.previousStrong && evidence.strong() && !approved && s.lastAccepted!=0 && now-s.lastAccepted<=8_000_000_000L
                ? Math.min(5,(now-s.lastAccepted)/1_000_000_000.0):0;
        s.lastAccepted=now; s.previousStrong=evidence.strong() && !approved;
        c.fingerprint=evidence.fingerprint();c.lastEvidence=evidence.summary();c.history.updatedAt=System.currentTimeMillis();
        if(evidence.suspicious() && !approved && !s.notifiedFingerprint.equals(evidence.fingerprint())) {
            s.notifiedFingerprint=evidence.fingerprint();
            audit(player,"RESOURCE_SUSPICION "+evidence.summary());
            owner(player,"Pack com alterações suspeitas: "+evidence.summary()+". Ainda sem punição; /wayanticheat inspect "+player.getUUID());
        }
        if(evidence.clean() && !s.notifiedFingerprint.isEmpty()) {
            s.notifiedFingerprint=""; audit(player,"CLEAN terrain restored; active accumulation paused; history retained");
            player.sendSystemMessage(Component.literal("[WayAround] O pack atual passou na análise de terreno. As evidências anteriores permanecem no histórico."));
        }
        if(approved)s.notifiedFingerprint="";
        int privateAt=AntiXrayConfig.PRIVATE_SECONDS.get();
        int publicAt=Math.max(privateAt+20,AntiXrayConfig.PUBLIC_SECONDS.get());
        int kickAt=Math.max(publicAt+20,AntiXrayConfig.KICK_SECONDS.get());
        int previousStage=c.history.stage;
        double previousPrivate=c.history.privateRound,previousPublic=c.history.publicRound;
        TrustHistory.Action action=c.history.observe(evidence,elapsed,approved,privateAt,publicAt,kickAt);
        data.setDirty();
        if(!AntiXrayConfig.ENFORCE.get()) {
            c.history.stage=previousStage;c.history.privateRound=previousPrivate;c.history.publicRound=previousPublic;
            if(action!=TrustHistory.Action.NONE && s.reviewStage<action.ordinal()) {
                s.reviewStage=action.ordinal(); audit(player,"AUDIT_ONLY verdict="+action); owner(player,"Modo de auditoria: "+action+"; nenhuma punição aplicada.");
            }
            return;
        }
        switch(action) {
            case PRIVATE_WARNING -> {
                audit(player,"PRIVATE_WARNING "+evidence.summary());
                player.sendSystemMessage(Component.literal("[WayAround Anti-Xray] Foram acumuladas evidências de terreno ocultado e minérios preservados. Remova o pack suspeito agora. Se continuar, seu nome será avisado no servidor, seguido de expulsão; a terceira expulsão resulta em banimento permanente. O responsável já recebeu o relatório."));
                owner(player,"Aviso privado enviado; uso confirmado acumulado="+(int)c.history.evidenceSeconds+"s; expulsões="+c.history.kicks);
            }
            case PUBLIC_WARNING -> {
                audit(player,"PUBLIC_WARNING continued after private warning "+evidence.summary());
                player.server.getPlayerList().broadcastSystemMessage(Component.literal("[WayAround Anti-Xray] "+c.name+" continuou com recursos classificados como x-ray após o aviso. Remova o pack para evitar expulsão."),false);
            }
            case KICK -> punish(player,c);
            default -> {}
        }
    }
    private static void punish(ServerPlayer player,AntiXrayData.Case c) {
        TrustHistory.Action result=c.history.expelled();
        String reason="WayAround Anti-Xray: uso continuado após avisos; expulsão "+c.history.kicks+"/3";
        audit(player,result+" "+reason+" "+c.lastEvidence);
        owner(player,reason+(result==TrustHistory.Action.BAN?"; banimento permanente; somente /pardon pelo responsável.":""));
        if(result==TrustHistory.Action.BAN) {
            player.server.getPlayerList().getBans().add(new UserBanListEntry(player.getGameProfile(),new Date(),"WayAround Anti-Xray",null,reason));
        }
        // The history is dirty before logout; reconnect never resets active evidence/round or strikes.
        SESSIONS.remove(player.getUUID());player.connection.disconnect(Component.literal(reason+(result==TrustHistory.Action.BAN?". Banimento permanente; contate o responsável pelo servidor.":". Remova o pack antes de voltar.")));
    }
    private static void owner(ServerPlayer player,String message) {
        Component text=Component.literal("[AntiXray privado] "+player.getGameProfile().getName()+" ("+player.getUUID()+"): "+message);
        for(ServerPlayer admin:player.server.getPlayerList().getPlayers())if(admin.hasPermissions(4))admin.sendSystemMessage(text);
    }
    private static void audit(ServerPlayer player,String message) {
        AntiXrayData.get(player.server).audit(player.getUUID(),message);
        WayAround.LOGGER.warn("[AntiXray] {} ({}) {}",player.getGameProfile().getName(),player.getUUID(),message);
    }

    @SubscribeEvent public static void mining(BlockEvent.BreakEvent event) {
        if(event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player) || !enabled(player.server)
                || player.isCreative() || player.isSpectator())return;
        var state=event.getState();
        if(!(state.is(BlockTags.DIAMOND_ORES)||state.is(BlockTags.GOLD_ORES)||state.is(BlockTags.IRON_ORES)
                ||state.is(BlockTags.REDSTONE_ORES)||state.is(BlockTags.EMERALD_ORES)||state.is(BlockTags.LAPIS_ORES)))return;
        if(oreBudget<6)return;oreBudget-=6;
        boolean enclosed=true;
        for(Direction direction:Direction.values()) {
            var pos=event.getPos().relative(direction);
            if(!player.serverLevel().hasChunkAt(pos))return;
            var neighbor=player.serverLevel().getBlockState(pos);
            if(!neighbor.isSolidRender(player.serverLevel(),pos))enclosed=false;
        }
        var data=AntiXrayData.get(player.server);var c=data.get(player.getUUID());
        c.history.oreBreaks=Math.min(1000000,c.history.oreBreaks+1);
        if(enclosed)c.history.enclosedOreBreaks=Math.min(1000000,c.history.enclosedOreBreaks+1);
        // Context for owner review, never an accusation or sanction by itself (strip mining is valid).
        data.setDirty();
    }

    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("wayanticheat").requires(s->s.hasPermission(4))
            .then(Commands.literal("status").executes(c->{c.getSource().sendSuccess(()->Component.literal("Anti-Xray enabled="+AntiXrayConfig.ENABLED.get()+" enforce="+AntiXrayConfig.ENFORCE.get()+" sessions="+SESSIONS.size()+"; histórico em data/wayaround_antixray.dat; logs [AntiXray]."),false);return 1;}))
            .then(Commands.literal("inspect").then(Commands.argument("uuid",StringArgumentType.word()).executes(c->{
                UUID id;try{id=UUID.fromString(StringArgumentType.getString(c,"uuid"));}catch(IllegalArgumentException e){c.getSource().sendFailure(Component.literal("UUID inválido."));return 0;}
                var saved=AntiXrayData.get(c.getSource().getServer()).find(id);
                if(saved==null){c.getSource().sendFailure(Component.literal("Sem histórico para esse UUID."));return 0;}
                c.getSource().sendSuccess(()->Component.literal(saved.name+" "+id+" active="+(int)saved.history.evidenceSeconds+"s round="+(int)saved.history.roundSeconds+"s lifetime="+(int)saved.history.lifetimeSeconds+"s kicks="+saved.history.kicks+" stage="+saved.history.stage+" banned="+saved.history.banned+" ores="+saved.history.oreBreaks+" enclosed="+saved.history.enclosedOreBreaks+"\n"+saved.lastEvidence+"\n"+String.join("\n",saved.audit)),false);return 1;
            })))
            .then(Commands.literal("approve").then(Commands.argument("fingerprint",StringArgumentType.word()).executes(c->approval(c.getSource(),StringArgumentType.getString(c,"fingerprint"),true))))
            .then(Commands.literal("revoke").then(Commands.argument("fingerprint",StringArgumentType.word()).executes(c->approval(c.getSource(),StringArgumentType.getString(c,"fingerprint"),false)))));
    }
    private static int approval(net.minecraft.commands.CommandSourceStack source,String hash,boolean allow) {
        if(!hash.matches("[0-9a-f]{64}")){source.sendFailure(Component.literal("Use o fingerprint SHA-256 de 64 caracteres mostrado em inspect."));return 0;}
        var data=AntiXrayData.get(source.getServer());if(allow)data.approve(hash);else data.revoke(hash);
        WayAround.LOGGER.warn("[AntiXray] OWNER {} {} by {}",allow?"APPROVE":"REVOKE",hash,source.getTextName());
        source.sendSuccess(()->Component.literal("Fingerprint "+(allow?"aprovado":"revogado")+". Histórico mantido; aprovação não remove banimento vanilla."),true);return 1;
    }
}
