package net.caravidro.wayaround.security;

import java.util.UUID;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("wayaround_security") @PrefixGameTestTemplate(false)
public final class AntiXrayGameTests {
    private static ResourceEvidence strong() { return new ResourceEvidence(255,0,15,ResourceEvidence.ALL_MASK,"a".repeat(64)); }
    @GameTest(template="assembly_test",batch="security",timeoutTicks=20)
    public static void visualCodecIsFixedAndDoesNotContainImage(GameTestHelper h) {
        float[] rays=new float[96],depth=new float[32];byte[] light=new byte[32];
        for(int i=0;i<32;i++){rays[i*3+2]=1;depth[i]=9.5F;light[i]=100;}
        var sent=new VisibilityReportPayload(43,true,1,2,3,0,0,1,rays,depth,light);
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try {
            VisibilityReportPayload.STREAM_CODEC.encode(buffer,sent);h.assertTrue(buffer.readableBytes()<640,"Measures only, bounded packet without screenshot");
            var restored=VisibilityReportPayload.STREAM_CODEC.decode(buffer);
            h.assertTrue(VisibilityAuditService.valid(restored)&&java.util.Arrays.equals(restored.distances(),sent.distances()),"Finite wire measurements round trip");
            restored.directions()[0]=Float.NaN;h.assertTrue(!VisibilityAuditService.valid(restored),"Invalid client rays rejected before world queries");
        } finally {buffer.release();}h.succeed();
    }
    @GameTest(template="assembly_test",batch="security",timeoutTicks=20)
    public static void actualServerTerrainExplainsHiddenOre(GameTestHelper h) {
        var level=h.getLevel();var start=h.absolutePos(new net.minecraft.core.BlockPos(7,5,1));
        for(int z=0;z<=12;z++)level.setBlock(start.offset(0,0,z),z>=3&&z<=5?net.minecraft.world.level.block.Blocks.STONE.defaultBlockState():z==10?net.minecraft.world.level.block.Blocks.DIAMOND_ORE.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),18);
        var eye=net.minecraft.world.phys.Vec3.atCenterOf(start);
        var ray=VisibilityMath.trace(eye.x,eye.y,eye.z,0,0,1,9.5,100,(x,y,z)->VisibilityAuditService.material(level,new net.minecraft.core.BlockPos(x,y,z)));
        h.assertTrue(ray.hiddenOre()&&ray.covered(),"Server's actual three stone blocks contradict visible diamond depth");
        ray=VisibilityMath.trace(eye.x,eye.y,eye.z,0,0,1,2.5,100,(x,y,z)->VisibilityAuditService.material(level,new net.minecraft.core.BlockPos(x,y,z)));
        h.assertTrue(!ray.hiddenOre()&&!ray.missingWall(),"Actual foreground terrain viewed normally is legitimate");h.succeed();
    }
    @GameTest(template="assembly_test",batch="security",timeoutTicks=20)
    public static void visualHistoryPersistsWithoutPixels(GameTestHelper h) {
        var data=new AntiXrayData();var id=UUID.randomUUID();var c=data.get(id);c.visualFrames=20;c.visualContradictions=4;c.lastVisual="covered=24 missing=18 oreBehind=4 distinctOres=3";
        var tag=data.save(new CompoundTag(),h.getLevel().registryAccess());var loaded=AntiXrayData.load(tag,h.getLevel().registryAccess()).find(id);
        h.assertTrue(loaded.visualFrames==20&&loaded.visualContradictions==4&&loaded.lastVisual.equals(c.lastVisual),"Numeric visual proof survives save");
        h.assertTrue(!tag.toString().contains("brightness")&&!tag.toString().contains("directions")&&!tag.toString().contains("pixels"),"Image and frame measurements are never persisted");h.succeed();
    }
    @GameTest(template="assembly_test",batch="security",timeoutTicks=20)
    public static void savedHistoryKeepsWarningsAndStrikes(GameTestHelper h) {
        var data=new AntiXrayData();var id=UUID.randomUUID();var c=data.get(id);
        c.name="EvidenceTest";c.fingerprint="a".repeat(64);c.lastEvidence=strong().summary();
        c.history.evidenceSeconds=100;c.history.roundSeconds=37;c.history.stage=2;c.history.kicks=2;
        c.history.privateRound=5;c.history.publicRound=30;c.history.lifetimeSeconds=160;
        c.history.enclosedOreBreaks=12;c.history.oreBreaks=25;data.approve(c.fingerprint);
        for(int i=0;i<100;i++)data.audit(id,"observation="+i);
        var loaded=AntiXrayData.load(data.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());
        var restored=loaded.find(id);
        h.assertTrue(restored.history.kicks==2 && restored.history.stage==2 && restored.history.roundSeconds==37 && restored.history.publicRound==30,"Restart preserves strikes, grace and accumulation");
        h.assertTrue(restored.audit.size()==64 && restored.audit.getLast().endsWith("observation=99"),"Audit is bounded and keeps latest evidence");
        h.assertTrue(loaded.approved(c.fingerprint) && restored.history.enclosedOreBreaks==12,"Owner approval and mining context survive save");h.succeed();
    }
    @GameTest(template="assembly_test",batch="security",timeoutTicks=20)
    public static void badSavedNumbersDoNotCreateConvictions(GameTestHelper h) {
        UUID id=UUID.randomUUID();CompoundTag all=new CompoundTag(),record=new CompoundTag(),root=new CompoundTag();
        record.putDouble("seconds",Double.NaN);record.putDouble("round",Double.POSITIVE_INFINITY);record.putInt("kicks",-9);
        all.put(id.toString(),record);all.put("not-a-uuid",record);root.put("cases",all);
        var data=AntiXrayData.load(root,h.getLevel().registryAccess());var c=data.find(id);
        h.assertTrue(c.history.evidenceSeconds==0 && c.history.roundSeconds==0 && c.history.kicks==0,"Malformed values do not punish");h.succeed();
    }
    @GameTest(template="assembly_test",batch="security",timeoutTicks=20)
    public static void packetRejectsReplayAndUnsolicitedEvidence(GameTestHelper h) {
        h.assertTrue(AntiXrayService.accepts(true,42,42,5000,true),"Matching fresh authenticated response accepted");
        h.assertTrue(!AntiXrayService.accepts(false,42,42,5000,true),"A consumed nonce cannot replay");
        h.assertTrue(!AntiXrayService.accepts(true,42,43,5000,true),"Nonce from another session rejected");
        h.assertTrue(!AntiXrayService.accepts(true,42,42,16_000_000_000L,true),"Stale response rejected");
        h.assertTrue(!AntiXrayService.accepts(true,42,42,5000,false),"Invalid evidence rejected");h.succeed();
    }
    @GameTest(template="assembly_test",batch="security",timeoutTicks=20)
    public static void evidenceWireCodecRoundTrips(GameTestHelper h) {
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try {
            var sent=new XrayReportPayload(812L,strong());XrayReportPayload.STREAM_CODEC.encode(buffer,sent);
            h.assertTrue(buffer.readableBytes()<100,"Evidence packet remains bounded below 100 bytes");
            h.assertTrue(sent.equals(XrayReportPayload.STREAM_CODEC.decode(buffer)),"Exact wire contract");
        } finally { buffer.release(); }h.succeed();
    }
    @GameTest(template="assembly_test",batch="security",timeoutTicks=20)
    public static void thirdRealExpulsionRequiresOwnerPardon(GameTestHelper h) {
        TrustHistory history=new TrustHistory();history.expelled();history.expelled();
        h.assertTrue(history.expelled()==TrustHistory.Action.BAN && history.banned,"Third expulsion is permanent ban");
        var profile=new com.mojang.authlib.GameProfile(UUID.randomUUID(),"AntiXrayBanTest");
        var bans=h.getLevel().getServer().getPlayerList().getBans();
        try {
            bans.add(new net.minecraft.server.players.UserBanListEntry(profile,new java.util.Date(),"WayAround Anti-Xray",null,"test"));
            h.assertTrue(bans.isBanned(profile),"Vanilla banlist rejects reconnect");
            bans.remove(profile);history.pardoned();
            h.assertTrue(!history.banned && history.kicks==0 && !bans.isBanned(profile),"Owner pardon is respected");
        } finally { bans.remove(profile); }h.succeed();
    }
    @GameTest(template="assembly_test",batch="security",timeoutTicks=20)
    public static void cleanAndMiningOnlyNeverEscalate(GameTestHelper h) {
        TrustHistory history=new TrustHistory();history.oreBreaks=1000;history.enclosedOreBreaks=1000;
        var clean=new ResourceEvidence(0,0,15,ResourceEvidence.ALL_MASK,"b".repeat(64));
        var unknown=new ResourceEvidence(0,0,0,0,"b".repeat(64));
        for(int i=0;i<100;i++) {
            h.assertTrue(history.observe(clean,5,false,10,30,60)==TrustHistory.Action.NONE,"Opaque texture and efficient miner remain innocent");
            h.assertTrue(history.observe(unknown,5,false,10,30,60)==TrustHistory.Action.NONE,"Unknown renderer never convicts");
            h.assertTrue(history.observe(strong(),5,true,10,30,60)==TrustHistory.Action.NONE,"Owner-approved pack never convicts");
        }
        h.assertTrue(history.evidenceSeconds==0 && history.kicks==0,"No evidence manufactured");h.succeed();
    }
}
