package net.caravidro.wayaround.littleleaf;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("wayaround_nature")
@PrefixGameTestTemplate(false)
public final class ColonySocietyGameTests {
    private static ColonyCoreBlockEntity nest(GameTestHelper h){return nest(h,0);}
    private static ColonyCoreBlockEntity nest(GameTestHelper h,int yOffset){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(7,5+yOffset,7));for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)for(int y=-4;y<=5;y++)l.setBlock(p.offset(x,y,z),y<0?Blocks.DIRT.defaultBlockState():Blocks.AIR.defaultBlockState(),18);
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=-3;y<=-2;y++)l.setBlock(p.offset(x,y,z),Blocks.AIR.defaultBlockState(),18);
        l.setBlock(p,LittleLeafContent.BLACK_COLONY.get().defaultBlockState(),18);var core=(ColonyCoreBlockEntity)l.getBlockEntity(p);core.initialize(l.getGameTime(),0,false);return core;
    }
    private static ColonyInsectEntity ant(GameTestHelper h,ColonyCoreBlockEntity c,int caste,boolean giant,int job,BlockPos at){var e=LittleLeafContent.BLACK_ANT.get().create(h.getLevel());e.bind(c.getBlockPos(),caste,false,giant);e.assignJob(job);e.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);h.getLevel().addFreshEntity(e);return e;}
    @GameTest(template="assembly_test",batch="colony_society",timeoutTicks=100)
    public static void workerAndDefenderDamageReflectSize(GameTestHelper h){
        var c=nest(h);var p=c.getBlockPos();var small=ant(h,c,0,false,0,p.offset(3,0,0));var large=ant(h,c,0,true,0,p.offset(-3,0,0));var defender=ant(h,c,1,true,0,p.offset(0,0,4));
        h.assertTrue(small.getAttributeValue(Attributes.ATTACK_DAMAGE)<.2&&large.getAttributeValue(Attributes.ATTACK_DAMAGE)>=2&&defender.getAttributeValue(Attributes.ATTACK_DAMAGE)>large.getAttributeValue(Attributes.ATTACK_DAMAGE),"Tiny workers barely hurt, giants have weight, defenders specialize in combat");
        h.assertTrue(c.buildSite(h.getLevel(),small)==null,"Tiny colonies cannot construct expansion");h.succeed();
    }
    @GameTest(template="assembly_test",batch="colony_society",timeoutTicks=300)
    public static void defendersAttackHostileMobsButDoNotMarkPeacefulPlayers(GameTestHelper h){
        var c=nest(h);var l=h.getLevel();var p=c.getBlockPos();var guard=ant(h,c,1,true,0,p.offset(0,0,3));var zombie=new Zombie(l);zombie.setNoAi(true);zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_HELMET));zombie.moveTo(p.getX()+3.5,p.getY(),p.getZ()+3.5,0,0);l.addFreshEntity(zombie);var player=h.makeMockPlayer(GameType.SURVIVAL);
        h.succeedWhen(()->{h.assertTrue(zombie.getLastHurtByMob()==guard,"A defender discovers and physically attacks a hostile mob");h.assertTrue(!c.hostile(player),"A peaceful player does not become an intruder just by being present");});
    }
    @GameTest(template="assembly_test",batch="colony_society",timeoutTicks=100)
    public static void descendingTrafficGetsSafeRightOfWay(GameTestHelper h){
        var c=nest(h);var p=c.getBlockPos();var up=ant(h,c,0,true,0,p.offset(3,0,3));var down=ant(h,c,0,true,0,p.offset(3,2,3));up.routeClimbing(true);up.setDeltaMovement(0,.1,0);down.setDeltaMovement(0,-.1,0);
        h.assertTrue(!ColonyTraffic.permit(up,up.position().add(0,2,0))&&up.yielding()&&up.getDeltaMovement().y<=0,"An ascending body stops or retreats rather than knocking the descending body off a narrow route");h.assertTrue(ColonyTraffic.precedes(down,up),"Descending traffic wins a stable priority decision");h.succeed();
    }
    @GameTest(template="assembly_test",batch="colony_society",timeoutTicks=100)
    public static void satelliteSharesQueenAndRaisesSavedCapacity(GameTestHelper h){
        var c=nest(h);var l=h.getLevel();c.invertColony();var p=c.getBlockPos().offset(4,0,0);l.setBlock(p,LittleLeafContent.CONNECTION.get().defaultBlockState(),18);var link=(ColonyConnectionBlockEntity)l.getBlockEntity(p);int before=c.capacity();
        h.assertTrue(link.bind(c)&&c.connectionCount()==1&&c.capacity()==before+3,"An attached entrance raises the existing colony limit rather than creating another queen");
        var restored=new ColonyConnectionBlockEntity(p,link.getBlockState());restored.loadWithComponents(link.saveWithFullMetadata(l.registryAccess()),l.registryAccess());h.assertTrue(restored.home().equals(c.getBlockPos()),"Connection home survives reload");var copy=new ColonyCoreBlockEntity(c.getBlockPos(),c.getBlockState());copy.loadWithComponents(c.saveWithFullMetadata(l.registryAccess()),l.registryAccess());h.assertTrue(copy.capacity()==c.capacity(),"Raised population capacity persists with the core");h.succeed();
    }
    @GameTest(template="assembly_test",batch="colony_society",timeoutTicks=1000)
    public static void nurseFeedsARealLarvaBeforeItBecomesAWorker(GameTestHelper h){
        var c=nest(h);var l=h.getLevel();var p=c.getBlockPos();var child=ant(h,c,0,false,ColonyInsectEntity.FORAGER,p.offset(3,0,0));child.makeLarva();var nurse=ant(h,c,0,false,ColonyInsectEntity.NURSE,p.offset(0,0,3));ColonyTransitData.get(l.getServer()).feed(c.identity(),3);final boolean[] fed={false};
        h.succeedWhen(()->{fed[0]|=child.feeds()==3;h.assertTrue(fed[0]&&child.activeAdult(),"Nurse reaches a real larva, spends three meals and the fed larva matures after its ecological interval: "+nurse.workStatus());h.assertTrue(ColonyTransitData.get(l.getServer()).food(c.identity())==0,"Brood feeding consumes colony food");});
    }
    @GameTest(template="assembly_test",batch="colony_society",timeoutTicks=1800)
    public static void corpsePersistsAndUndertakerPhysicallyBringsItInside(GameTestHelper h){
        var c=nest(h);var l=h.getLevel();var p=c.getBlockPos();var victim=ant(h,c,0,false,0,p.offset(4,0,0));victim.hurt(l.damageSources().generic(),100);
        var bodies=l.getEntitiesOfClass(ColonyInsectEntity.class,new AABB(p).inflate(8),ColonyInsectEntity::corpse);h.assertTrue(bodies.size()==1,"Death leaves one physical insect body");var body=bodies.get(0);var saved=new CompoundTag();body.saveWithoutId(saved);var restored=LittleLeafContent.BLACK_ANT.get().create(l);restored.load(saved);h.assertTrue(restored.corpse(),"Corpse life state survives entity reload");
        h.runAfterDelay(80,()->{h.assertTrue(!body.isRemoved(),"The body survives the vanilla death disappearance interval");ant(h,c,0,false,ColonyInsectEntity.UNDERTAKER,p.above());});
        h.succeedWhen(()->{var record=ColonyTransitData.get(l.getServer()).waitingBody(c.identity());h.assertTrue(record!=null&&record.id().equals(body.getUUID())&&body.isRemoved(),"An undertaker reaches, visibly carries and brings the actual body into the linked cemetery");});
    }
    @GameTest(template="assembly_test",batch="colony_society",timeoutTicks=100)
    public static void accessStaircaseRequiresOneRealCarriedBlock(GameTestHelper h){
        var c=nest(h);var l=h.getLevel();var p=c.getBlockPos();c.invertColony();var worker=ant(h,c,0,true,ColonyInsectEntity.BUILDER,p.offset(4,0,0));var source=p.offset(9,-1,0);l.setBlock(source,Blocks.DIRT.defaultBlockState(),18);worker.moveTo(source.getX()+.5,source.getY()+1,source.getZ()+.5,0,0);
        boolean old=l.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);try{l.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true,l.getServer());h.assertTrue(c.excavate(l,source,worker),"Access work extracts an actual source block");worker.moveTo(p.getX()+4.5,p.getY(),p.getZ()+.5,0,0);c.requestAccess(l,worker,p.offset(8,4,0));var site=c.buildSite(l,worker);h.assertTrue(site!=null,"A height objective produces an accessible staircase foundation");worker.moveTo(site.getX()-1.75,site.getY(),site.getZ()+.5,0,0);h.assertTrue(c.placeMaterial(l,site,worker)&&!worker.carryingMaterial()&&l.getBlockState(source).isAir(),"The staircase placement spends the carried terrain block");h.succeed();}finally{l.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(old,l.getServer());}
    }

    @GameTest(template="assembly_test",batch="colony_outpost",timeoutTicks=2000)
    public static void builderConstructsASatelliteFromActualTerrain(GameTestHelper h){
        var c=nest(h,16);var l=h.getLevel();var p=c.getBlockPos();for(int x=-14;x<=14;x++)for(int z=-14;z<=14;z++)for(int y=-1;y<7;y++)if(x!=0||z!=0||y!=0)l.setBlock(p.offset(x,y,z),y<0?Blocks.DIRT.defaultBlockState():Blocks.AIR.defaultBlockState(),18);l.setBlock(p,c.getBlockState(),18);c.initialize(l.getGameTime(),24,false);c.invertColony();h.assertTrue(!c.abandoned(),"Outpost fixture uses a fresh colony identity");ColonyTransitData.get(l.getServer()).feed(c.identity(),8);
        boolean old=l.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);l.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true,l.getServer());h.runAtTickTime(1990,()->l.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(old,l.getServer()));var builder=ant(h,c,0,true,ColonyInsectEntity.BUILDER,p.offset(0,0,8));
        h.succeedWhen(()->{h.assertTrue(c.connectionCount()>0,"A real builder extracts soil, carries it and molds an attached colony entrance: "+builder.position()+", giant="+c.giant()+", abandoned="+c.abandoned()+", stage="+c.stage()+", food="+ColonyTransitData.get(l.getServer()).food(c.identity())+", "+builder.workStatus());h.assertTrue(ColonyTransitData.get(l.getServer()).food(c.identity())==4,"Satellite construction spends four meals");l.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(old,l.getServer());});
    }
}
