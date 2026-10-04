package net.caravidro.wayaround.littleleaf;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("wayaround_nature")
@PrefixGameTestTemplate(false)
public final class LittleLeafBehaviorGameTests {
    private static ColonyCoreBlockEntity setup(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(7,5,7));
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)for(int y=-4;y<=5;y++)l.setBlock(p.offset(x,y,z),y<0?Blocks.DIRT.defaultBlockState():Blocks.AIR.defaultBlockState(),18);
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=-3;y<=-2;y++)l.setBlock(p.offset(x,y,z),Blocks.AIR.defaultBlockState(),18);
        l.setBlock(p,LittleLeafContent.COLONY_CORE.get().defaultBlockState(),18);var c=(ColonyCoreBlockEntity)l.getBlockEntity(p);c.initialize(l.getGameTime(),0,false);return c;
    }
    @GameTest(template="assembly_test",batch="littleleaf",timeoutTicks=100)
    public static void fungusHarvestCooldown(GameTestHelper h){
        var c=setup(h);var l=h.getLevel();var p=c.getBlockPos().offset(1,-3,0);l.setBlock(p,LittleLeafContent.COLONY_FUNGUS.get().defaultBlockState(),18);
        var player=h.makeMockPlayer(GameType.SURVIVAL);var hit=new BlockHitResult(Vec3.atCenterOf(p),Direction.UP,p,false);
        l.getBlockState(p).useWithoutItem(l,player,hit);l.getBlockState(p).useWithoutItem(l,player,hit);
        h.assertTrue(player.getInventory().countItem(LittleLeafContent.FUNGUS_ITEM.get())==1&&!l.getBlockState(p).getValue(ColonyFungusBlock.RIPE),"One culture harvest, no repeat-click duplication");h.succeed();
    }
    @GameTest(template="assembly_test",batch="littleleaf",timeoutTicks=100)
    public static void cuttersCarryFragmentsAndGiantsLeaveThemOutside(GameTestHelper h){
        var c=setup(h);var l=h.getLevel();var leaf=c.getBlockPos().offset(3,0,0);l.setBlock(leaf,Blocks.OAK_LEAVES.defaultBlockState(),18);
        var ant=LittleLeafContent.BLACK_ANT.get().create(l);ant.bind(c.getBlockPos(),0,false,false);ant.moveTo(leaf.getX()+.5,leaf.getY(),leaf.getZ()+.5,0,0);
        c.cut(l,leaf,ant);h.assertTrue(ant.carrying()&&l.getBlockState(leaf).is(Blocks.OAK_LEAVES),"Cut a fragment while retaining canopy support");
        InversionEffect.invert(ant);long before=c.work();int stockBefore=ColonyTransitData.get(l.getServer()).food(c.identity());h.assertTrue(c.acceptLoad(ant)&&c.work()==before&&!ant.carrying(),"An enlarged outsider cannot feed the small inner garden");h.assertTrue(ColonyTransitData.get(l.getServer()).food(c.identity())==stockBefore,"Loads left outside do not silently feed the brood");
        h.assertTrue(l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(leaf).inflate(4)).stream().anyMatch(i->i.getItem().is(LittleLeafContent.LEAF_FRAGMENT.get())&&i.getItem().getCount()==4),"Larger real load left outside");
        h.assertTrue(!c.acceptLoad(ant),"The same load cannot be deposited twice");h.succeed();
    }
    @GameTest(template="assembly_test",batch="littleleaf",timeoutTicks=100)
    public static void colonyDoseChangesBirthScaleAndLargeDelivery(GameTestHelper h){
        var c=setup(h);var l=h.getLevel();var ant=LittleLeafContent.TERMITE.get().create(l);
        ColonyEvents.dose(l,Vec3.atCenterOf(c.getBlockPos()).add(0,3,0));h.assertTrue(c.giant(),"Splash on the mound roof changes colony inversion");
        ant.bind(c.getBlockPos(),0,false,c.giant());ant.moveTo(c.getBlockPos().getX()+3,c.getBlockPos().getY(),c.getBlockPos().getZ(),0,0);ant.carry(true);
        long before=c.work();h.assertTrue(ant.getScale()>=3&&c.acceptLoad(ant)&&c.work()==before+8,"New giant inhabitants feed the enhanced colony with larger loads");
        ColonyEvents.dose(l,Vec3.atCenterOf(c.getBlockPos()));h.assertTrue(!c.giant(),"Second colony splash reverses future births");h.succeed();
    }
    @GameTest(template="assembly_test",batch="littleleaf",timeoutTicks=400)
    public static void aWorkerActuallyEntersAndReturnsFromTheGarden(GameTestHelper h){
        var c=setup(h);var l=h.getLevel();var p=c.getBlockPos();var ant=LittleLeafContent.BLACK_ANT.get().create(l);ant.bind(p,0,false,false);ant.carry(true);ant.moveTo(p.getX()+.5,p.getY(),p.getZ()+1.5,0,0);l.addFreshEntity(ant);
        h.succeedWhen(()->h.assertTrue(c.work()>=1&&!ant.carrying()&&ant.getY()>=p.getY(),"Worker entered the physical chamber, delivered once and emerged: "+ant.position()+", "+ant.workStatus()));
    }
    @GameTest(template="assembly_test",batch="littleleaf",timeoutTicks=1000)
    public static void workerFindsCutsAndReturnsALeafWithoutManualLoad(GameTestHelper h){forageCycle(h,false);}
    @GameTest(template="assembly_test",batch="littleleaf",timeoutTicks=1800)
    public static void workerClimbsATrunkToHarvestAnElevatedCanopy(GameTestHelper h){forageCycle(h,true);}
    private static void forageCycle(GameTestHelper h,boolean tree){
        var c=setup(h);var l=h.getLevel();var p=c.getBlockPos();var leaf=p.offset(4,tree?3:0,0);
        if(tree){for(int y=0;y<3;y++)l.setBlock(p.offset(4,y,0),Blocks.OAK_LOG.defaultBlockState(),18);for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)l.setBlock(leaf.offset(x,0,z),Blocks.OAK_LEAVES.defaultBlockState(),18);}
        else l.setBlock(leaf,Blocks.OAK_LEAVES.defaultBlockState(),18);
        var ant=LittleLeafContent.BLACK_ANT.get().create(l);ant.bind(p,0,false,false);ant.moveTo(p.getX()+.5,p.getY(),p.getZ()+2.5,0,0);l.addFreshEntity(ant);
        h.succeedWhen(()->h.assertTrue(c.work()>0&&!ant.carrying(),"Real AI found a leaf, reached it, cut a load and returned it to the fungus; ant="+ant.position()+", load="+ant.carrying()+", work="+c.work()));
    }
    @GameTest(template="assembly_test",batch="littleleaf",timeoutTicks=120)
    public static void speciesCoresAndSupportedBirth(GameTestHelper h){
        var c=setup(h);var l=h.getLevel();var p=c.getBlockPos();
        for(int species=0;species<4;species++){
            var state=LittleLeafContent.core(species).defaultBlockState();
            h.assertTrue(state.getValue(ColonyCoreBlock.SPECIES)==species,"Dedicated core retains its species on placement");
            h.assertTrue(LittleLeafContent.CORE_ENTITY.get().isValid(state),"All species cores support the shared colony block entity");
        }
        h.succeedWhen(()->{c.birth(l);var q=c.queen(l);h.assertTrue(q!=null,"A queen is born at the entrance");h.assertTrue(q.getY()==p.getY()+1&&l.noCollision(q)&&l.getBlockState(q.blockPosition().below()).isFaceSturdy(l,q.blockPosition().below(),Direction.UP),"Birth has a solid floor and clear body above the core");});
    }
    @GameTest(template="assembly_test",batch="littleleaf",timeoutTicks=220)
    public static void rivalSpeciesActuallyFight(GameTestHelper h){
        var c=setup(h);var l=h.getLevel();var p=c.getBlockPos();
        var black=LittleLeafContent.BLACK_ANT.get().create(l);var red=LittleLeafContent.RED_ANT.get().create(l);
        black.bind(p,1,false,false);red.bind(p,1,false,false);
        black.moveTo(p.getX()+.5,p.getY(),p.getZ()+3.5,0,0);red.moveTo(p.getX()+.9,p.getY(),p.getZ()+3.5,0,0);l.addFreshEntity(black);l.addFreshEntity(red);
        h.succeedWhen(()->h.assertTrue(black.getHealth()<black.getMaxHealth()||red.getHealth()<red.getMaxHealth(),"Nearby rival species discover each other and deal actual melee damage: black="+black.position()+", "+black.workStatus()+", red="+red.position()+", "+red.workStatus()));
    }
    @GameTest(template="assembly_test",batch="littleleaf",timeoutTicks=100)
    public static void sameSpeciesRemainFriendly(GameTestHelper h){
        var c=setup(h);var l=h.getLevel();var p=c.getBlockPos();var a=LittleLeafContent.BLACK_ANT.get().create(l);var b=LittleLeafContent.BLACK_ANT.get().create(l);
        a.bind(p,1,false,false);b.bind(p,1,false,false);a.moveTo(p.getX()+.5,p.getY(),p.getZ()+3.5,0,0);b.moveTo(p.getX()+.9,p.getY(),p.getZ()+3.5,0,0);l.addFreshEntity(a);l.addFreshEntity(b);
        h.runAfterDelay(80,()->{h.assertTrue(a.getTarget()==null&&b.getTarget()==null&&a.getHealth()==a.getMaxHealth()&&b.getHealth()==b.getMaxHealth(),"Same-species neighbors do not attack one another");h.succeed();});
    }
    @GameTest(template="assembly_test",batch="littleleaf",timeoutTicks=1800)
    public static void workerGetsPastAnObstacleAndDelivers(GameTestHelper h){
        var c=setup(h);var l=h.getLevel();var p=c.getBlockPos();
        for(int z=-3;z<=3;z++)for(int y=0;y<2;y++)l.setBlock(p.offset(2,y,z),Blocks.STONE.defaultBlockState(),18);
        l.setBlock(p.offset(4,0,0),Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT,true),18);
        var ant=LittleLeafContent.BLACK_ANT.get().create(l);ant.bind(p,0,false,false);ant.moveTo(p.getX()+.5,p.getY(),p.getZ()+2.5,0,0);l.addFreshEntity(ant);
        h.succeedWhen(()->h.assertTrue(c.work()>0&&!ant.carrying(),"Worker passes the obstruction and delivers a real leaf; ant="+ant.position()));
    }
    @GameTest(template="assembly_test",batch="littleleaf",timeoutTicks=100)
    public static void attackingPersistsColonyHostilityAndQueenDeath(GameTestHelper h){
        var c=setup(h);var l=h.getLevel();var p=c.getBlockPos();var soldier=LittleLeafContent.RED_ANT.get().create(l);soldier.bind(p,1,false,false);
        var player=h.makeMockPlayer(GameType.SURVIVAL);soldier.hurt(l.damageSources().playerAttack(player),1);
        h.assertTrue(c.hostile(player),"Direct attack is shared with the colony");var saved=c.saveWithFullMetadata(l.registryAccess());c.loadWithComponents(saved,l.registryAccess());h.assertTrue(c.hostile(player),"Colony remembers the attacker after reload");
        c.initialize(l.getGameTime(),0,true);var queen=LittleLeafContent.RED_ANT.get().create(l);queen.bind(p,2,false,false);queen.die(l.damageSources().generic());c.advance(l.getGameTime()+24000);h.assertTrue(c.work()==0,"Dead queen stops aggregate growth");h.succeed();
    }
    @GameTest(template="assembly_test",batch="littleleaf",timeoutTicks=1200)
    public static void termiteCollectsLeavesAcrossAHighLedge(GameTestHelper h){
        var c=setup(h);var l=h.getLevel();var p=c.getBlockPos();l.setBlock(p,LittleLeafContent.TERMITE_COLONY.get().defaultBlockState(),18);c=(ColonyCoreBlockEntity)l.getBlockEntity(p);c.initialize(l.getGameTime(),0,false);
        for(int y=0;y<4;y++)l.setBlock(p.offset(4,y,0),Blocks.STONE.defaultBlockState(),18);
        l.setBlock(p.offset(4,4,0),Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT,true),18);
        var insect=LittleLeafContent.TERMITE.get().create(l);insect.bind(p,0,false,false);insect.moveTo(p.getX()+.5,p.getY(),p.getZ()+2.5,0,0);l.addFreshEntity(insect);final var colony=c;
        h.succeedWhen(()->h.assertTrue(colony.work()>0&&!insect.carrying(),"A tiny termite climbs solid terrain, cuts an actual elevated leaf and returns it: "+insect.position()));
    }
    @GameTest(template="assembly_test",batch="littleleaf",timeoutTicks=100)
    public static void constructionConservesTerrainAndSavedCargo(GameTestHelper h){
        var c=setup(h);var l=h.getLevel();var p=c.getBlockPos();c.invertColony();var insect=LittleLeafContent.BLACK_ANT.get().create(l);insect.bind(p,0,false,true);
        var source=p.offset(9,-1,0);l.setBlock(source,Blocks.DIRT.defaultBlockState(),18);insect.moveTo(source.getX()+.5,source.getY()+1,source.getZ()+.5,0,0);
        boolean old=l.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);try{l.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true,l.getServer());
            h.assertTrue(c.excavate(l,source,insect)&&l.getBlockState(source).isAir()&&insect.carryingMaterial(),"One real terrain block becomes one carried block");
            var tag=new CompoundTag();insect.saveWithoutId(tag);var copy=LittleLeafContent.BLACK_ANT.get().create(l);copy.load(tag);h.assertTrue(copy.carryingMaterial()&&copy.material().is(Blocks.DIRT),"Actual block cargo survives entity persistence");
            var site=p.offset(3,0,3);l.setBlock(site,Blocks.AIR.defaultBlockState(),18);insect.moveTo(site.getX()-1.75,site.getY(),site.getZ()+.5,0,0);
            h.assertTrue(c.placeMaterial(l,site,insect)&&l.getBlockState(site).is(Blocks.DIRT)&&!insect.carryingMaterial(),"The transported block is physically placed outside the worker body");h.assertTrue(!c.placeMaterial(l,site,insect),"Consumed cargo cannot create another block");
            l.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false,l.getServer());l.setBlock(source,Blocks.DIRT.defaultBlockState(),18);insect.moveTo(source.getX()+.5,source.getY()+1,source.getZ()+.5,0,0);h.assertTrue(!c.excavate(l,source,insect)&&l.getBlockState(source).is(Blocks.DIRT),"mobGriefing disables terrain removal");h.succeed();
        }finally{l.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(old,l.getServer());}
    }
    @GameTest(template="assembly_test",batch="littleleaf",timeoutTicks=100)
    public static void growthDoesNotSummonWalls(GameTestHelper h){
        var c=setup(h);var l=h.getLevel();var p=c.getBlockPos();c.invertColony();c.delivered(true);c.advance(l.getGameTime()+24000*30);
        int before=0;for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)for(int y=0;y<5;y++)if(l.getBlockState(p.offset(x,y,z)).is(Blocks.DIRT))before++;
        for(int i=0;i<40;i++)ColonyCoreBlockEntity.tick(l,p,c.getBlockState(),c);
        int after=0;for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)for(int y=0;y<5;y++)if(l.getBlockState(p.offset(x,y,z)).is(Blocks.DIRT))after++;
        h.assertTrue(after==before,"Ecological age unlocks blueprints, but only actual cargo-bearing workers build walls");h.succeed();
    }
    @GameTest(template="assembly_test",batch="littleleaf",timeoutTicks=100)
    public static void queenDeathKillsTheCultureAndPersistsAbandonment(GameTestHelper h){
        var c=setup(h);var l=h.getLevel();var p=c.getBlockPos().offset(1,-3,0);l.setBlock(p,LittleLeafContent.COLONY_FUNGUS.get().defaultBlockState(),18);c.queenDied();
        h.assertTrue(c.abandoned()&&!l.getBlockState(p).getValue(ColonyFungusBlock.ALIVE),"Queen death abandons the colony and kills its living culture");
        var player=h.makeMockPlayer(GameType.SURVIVAL);l.getBlockState(p).useWithoutItem(l,player,new BlockHitResult(Vec3.atCenterOf(p),Direction.UP,p,false));h.assertTrue(player.getInventory().countItem(LittleLeafContent.FUNGUS_ITEM.get())==0,"Dead fungus cannot produce a brewing culture");
        var data=ColonyTransitData.get(l.getServer());var id=GlobalPos.of(l.dimension(),c.getBlockPos());var saved=data.save(new CompoundTag(),l.registryAccess());h.assertTrue(ColonyTransitData.load(saved,l.registryAccess()).abandoned(id),"Surface/interior death link survives saved-data reload");h.succeed();
    }
    @GameTest(template="assembly_test",batch="littleleaf",timeoutTicks=100)
    public static void invadingSpeciesPhysicallyEnterAndPersistUntilInteriorIsObserved(GameTestHelper h){
        var victim=setup(h);var l=h.getLevel();var p=victim.getBlockPos();var invader=LittleLeafContent.RED_ANT.get().create(l);invader.bind(p.offset(20,0,0),1,false,false);invader.moveTo(p.getX()+.5,p.getY()+1,p.getZ()+.5,0,0);l.addFreshEntity(invader);
        h.assertTrue(ColonyTravel.invade(l,victim,invader)&&invader.isRemoved(),"A rival at the entrance physically leaves the surface for the interior");
        var data=ColonyTransitData.get(l.getServer());var id=GlobalPos.of(l.dimension(),p);var copy=ColonyTransitData.load(data.save(new CompoundTag(),l.registryAccess()),l.registryAccess());h.assertTrue(copy.pending(id).size()==1&&copy.pending(id).get(0).species()==1,"Pending invader species, caste and health persist without loading an unobserved interior");h.succeed();
    }

    @GameTest(template="assembly_test",batch="colony_construction",timeoutTicks=1800)
    public static void giantWorkerActuallyExtractsCarriesAndBuilds(GameTestHelper h){
        var c=setup(h);var l=h.getLevel();boolean original=l.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);l.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true,l.getServer());h.runAtTickTime(1790,()->l.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(original,l.getServer()));var p=c.getBlockPos();for(int x=-14;x<=14;x++)for(int z=-14;z<=14;z++)l.setBlock(p.offset(x,-1,z),Blocks.DIRT.defaultBlockState(),18);c.invertColony();
        var worker=LittleLeafContent.BLACK_ANT.get().create(l);if((worker.getId()&1)!=0)worker=LittleLeafContent.BLACK_ANT.get().create(l);
        worker.bind(p,0,false,true);worker.assignJob(ColonyInsectEntity.BUILDER);worker.moveTo(p.getX()+.5,p.getY(),p.getZ()+8.5,0,0);l.addFreshEntity(worker);final var insect=worker;
        h.succeedWhen(()->{int placed=0;for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)if(l.getBlockState(p.offset(x,0,z)).is(Blocks.DIRT))placed++;
            h.assertTrue(placed>0,"Real giant worker must find soil, remove it, carry it along its path and place a wall: worker="+insect.position()+", material="+insect.material()+", "+insect.workStatus());l.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(original,l.getServer());});
    }
    @GameTest(template="assembly_test",batch="littleleaf",timeoutTicks=100)
    public static void abyssAmbienceHasLongIrregularIntervals(GameTestHelper h){
        var random=net.minecraft.util.RandomSource.create(42);var delays=new java.util.HashSet<Long>();for(int i=0;i<100;i++){long d=net.caravidro.wayaround.ecology.AbyssSoundSchedule.delay(random);h.assertTrue(d>=2400&&d<=12000,"Abyss terror leaves minutes of quiet");delays.add(d);}h.assertTrue(delays.size()>50,"Intervals vary rather than repeat on a short clock");h.succeed();
    }



    @GameTest(template="assembly_test",batch="littleleaf",timeoutTicks=2200)
    public static void antHarvestsAndReturnsAcrossRealRootGeometry(GameTestHelper h){rootForage(h,false);}
    @GameTest(template="assembly_test",batch="littleleaf",timeoutTicks=2200)
    public static void termiteHarvestsAndReturnsAcrossRealRootGeometry(GameTestHelper h){rootForage(h,true);}
    private static void rootForage(GameTestHelper h,boolean termite){
        var c=setup(h);var l=h.getLevel();var p=c.getBlockPos();if(termite){l.setBlock(p,LittleLeafContent.TERMITE_COLONY.get().defaultBlockState(),18);c=(ColonyCoreBlockEntity)l.getBlockEntity(p);c.initialize(l.getGameTime(),0,false);}
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=0;y<2;y++)if(x*x+z*z<=4&&!(x==0&&(z==0||z==1)))l.setBlock(p.offset(x,y,z),Blocks.DIRT.defaultBlockState(),18);
        var root=net.caravidro.wayaround.ecology.EcologyContent.OAK_TREE_SEGMENT.get().defaultBlockState().setValue(net.caravidro.wayaround.ecology.TreeWoodSegmentBlock.ROOT,true).setValue(net.caravidro.wayaround.ecology.TreeWoodSegmentBlock.THICKNESS,4);
        for(int n=-3;n<=3;n++){l.setBlock(p.offset(3,0,n),root.setValue(net.caravidro.wayaround.ecology.TreeWoodSegmentBlock.AXIS,Direction.Axis.Z),18);l.setBlock(p.offset(-3,0,n),root.setValue(net.caravidro.wayaround.ecology.TreeWoodSegmentBlock.AXIS,Direction.Axis.Z),18);l.setBlock(p.offset(n,0,3),root.setValue(net.caravidro.wayaround.ecology.TreeWoodSegmentBlock.AXIS,Direction.Axis.X),18);l.setBlock(p.offset(n,0,-3),root.setValue(net.caravidro.wayaround.ecology.TreeWoodSegmentBlock.AXIS,Direction.Axis.X),18);}
        var leaf=p.offset(5,1,0);l.setBlock(leaf.below(),Blocks.STONE.defaultBlockState(),18);l.setBlock(leaf,Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT,true),18);
        var ant=(termite?LittleLeafContent.TERMITE:LittleLeafContent.BLACK_ANT).get().create(l);ant.bind(p,0,false,false);ant.assignJob(ColonyInsectEntity.FORAGER);ant.moveTo(p.getX()+.5,p.getY()+1,p.getZ()+.5,0,0);l.addFreshEntity(ant);final var colony=c;
        h.succeedWhen(()->h.assertTrue(colony.work()>0&&!ant.carrying()&&!ant.sheltered()&&ant.getY()>=p.getY(),"Actual mod roots and a dirt mound permit a complete harvest, physical garden delivery and emergence: "+ant.position()+", "+ant.workStatus()));
    }
    @GameTest(template="assembly_test",batch="littleleaf",timeoutTicks=1200)
    public static void workerHarvestsGrassBlockAndReturns(GameTestHelper h){groundForage(h,false);}
    @GameTest(template="assembly_test",batch="littleleaf",timeoutTicks=1200)
    public static void workerHarvestsNewModFoliageAndReturns(GameTestHelper h){groundForage(h,true);}
    private static void groundForage(GameTestHelper h,boolean plant){
        var c=setup(h);var l=h.getLevel();var p=c.getBlockPos();var source=p.offset(4,plant?0:-1,0);l.setBlock(source,plant?net.caravidro.wayaround.ecology.EcologyContent.DAMP_FERN.get().defaultBlockState():Blocks.GRASS_BLOCK.defaultBlockState(),18);
        var ant=LittleLeafContent.BLACK_ANT.get().create(l);ant.bind(p,0,false,false);ant.moveTo(p.getX()+.5,p.getY(),p.getZ()+2.5,0,0);l.addFreshEntity(ant);
        h.succeedWhen(()->{h.assertTrue(c.work()>0&&!ant.carrying()&&ant.getY()>=p.getY(),"Forager gathers actual ground foliage and physically returns it: "+ant.position()+", "+ant.workStatus());h.assertTrue(!l.getBlockState(source).isAir(),"Fragment collection keeps the supporting grass/plant");});
    }
    @GameTest(template="assembly_test",batch="littleleaf",timeoutTicks=100)
    public static void tinyAntsSurviveNormalFalls(GameTestHelper h){
        var c=setup(h);var l=h.getLevel();var ant=LittleLeafContent.BLACK_ANT.get().create(l);ant.bind(c.getBlockPos(),0,false,false);float health=ant.getHealth();
        ant.causeFallDamage(20,1,l.damageSources().fall());h.assertTrue(ant.getHealth()==health,"A twenty-block tumble does not kill a tiny insect");ant.causeFallDamage(64,1,l.damageSources().fall());h.assertTrue(ant.getHealth()<health,"Only a much higher fall damages the insect");h.succeed();
    }
    @GameTest(template="assembly_test",batch="colony_weather",timeoutTicks=1000)
    public static void rainPrioritizesDeliveryAndShelterUntilDry(GameTestHelper h){
        var c=setup(h);var l=h.getLevel();var p=c.getBlockPos();boolean old=l.isRaining();l.setWeatherParameters(0,10000,true,false);h.runAtTickTime(950,()->l.setWeatherParameters(10000,0,old,false));
        var ant=LittleLeafContent.BLACK_ANT.get().create(l);ant.bind(p,0,false,false);ant.carry(true);ant.moveTo(p.getX()+.5,p.getY(),p.getZ()+3.5,0,0);l.addFreshEntity(ant);final boolean[] saw={false};
        h.succeedWhen(()->{if(ant.sheltered()){h.assertTrue(c.work()>0&&ant.getY()<p.getY()&&!ant.carrying(),"Rain sends the carried leaf into the garden before sheltering");saw[0]=true;l.setWeatherParameters(10000,0,false,false);}h.assertTrue(saw[0]&&!ant.sheltered()&&ant.getY()>=p.getY(),"Worker physically emerges when weather clears: "+ant.position()+", "+ant.workStatus());l.setWeatherParameters(10000,0,old,false);});
    }
    @GameTest(template="assembly_test",batch="colony_water",timeoutTicks=220)
    public static void submergedAntDrownsQuickly(GameTestHelper h){
        var c=setup(h);var l=h.getLevel();var p=c.getBlockPos().offset(3,0,3);for(int y=0;y<3;y++)l.setBlock(p.above(y),Blocks.WATER.defaultBlockState(),18);var ant=LittleLeafContent.BLACK_ANT.get().create(l);ant.bind(c.getBlockPos(),0,false,false);ant.setNoAi(true);ant.moveTo(p.getX()+.5,p.getY(),p.getZ()+.5,0,0);l.addFreshEntity(ant);
        h.succeedWhen(()->h.assertTrue(!ant.isAlive(),"A submerged insect has a short air reserve rather than surviving underwater indefinitely"));
    }

    @GameTest(template="assembly_test",batch="littleleaf",timeoutTicks=1800)
    public static void workerDeliversThroughActualTallTermiteMound(GameTestHelper h){
        var c=setup(h);var l=h.getLevel();var p=c.getBlockPos();
        // Remove only the test core, preserving its real soil substrate.
        l.setBlock(p,Blocks.AIR.defaultBlockState(),18);
        // A savanna biome isn't required for this geometry regression: recreate
        // the exact tall species footprint and its generated ground doorway.
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)for(int y=0;y<5;y++){
            if(x*x+z*z>(3-y*.45)*(3-y*.45)||x==0&&z==0)continue;
            l.setBlock(p.offset(x,y,z),Blocks.DIRT.defaultBlockState(),18);
        }
        for(int y=-3;y<=1;y++)l.setBlock(p.offset(0,y,1),Blocks.AIR.defaultBlockState(),18);
        for(int z=1;z<=3;z++)for(int y=0;y<=1;y++)l.setBlock(p.offset(0,y,z),Blocks.AIR.defaultBlockState(),18);
        l.setBlock(p,LittleLeafContent.TERMITE_COLONY.get().defaultBlockState(),18);
        var colony=(ColonyCoreBlockEntity)l.getBlockEntity(p);colony.initializeMound(l.getGameTime(),0,3,5);
        l.setBlock(p.offset(1,-3,0),LittleLeafContent.COLONY_FUNGUS.get().defaultBlockState().setValue(ColonyFungusBlock.RIPE,false),18);
        var ant=LittleLeafContent.TERMITE.get().create(l);ant.bind(p,0,false,false);ant.assignJob(ColonyInsectEntity.FORAGER);ant.carry(true);ant.moveTo(p.getX()+.5,p.getY(),p.getZ()+4.5,0,0);l.addFreshEntity(ant);
        h.succeedWhen(()->{
            h.assertTrue(colony.work()>=1&&!ant.carrying()&&!ant.sheltered()&&ant.getY()>=p.getY(),"Load physically enters a tall mound, then emerges: "+ant.position()+", "+ant.workStatus());
            h.assertTrue(l.getBlockState(p.offset(1,0,1)).is(Blocks.DIRT),"Tiny delivery preserves the surrounding wall");
            h.assertTrue(l.getBlockState(p.offset(1,-3,0)).getValue(ColonyFungusBlock.RIPE),"The actual underground fungus receives the leaf");
        });
    }
    @GameTest(template="assembly_test",batch="littleleaf",timeoutTicks=100)
    public static void blockedEntranceAndTinyBuilderDoNotRemoveTerrain(GameTestHelper h){
        var c=setup(h);var l=h.getLevel();var p=c.getBlockPos();
        l.setBlock(p.offset(0,0,1),Blocks.DIRT.defaultBlockState(),18);
        var ant=LittleLeafContent.BLACK_ANT.get().create(l);ant.bind(p,0,false,false);ant.carry(true);
        h.assertTrue(!c.openEntrance(l)&&l.getBlockState(p.offset(0,0,1)).is(Blocks.DIRT),"Returning tiny workers never dig through a blocked door");
        c.invertColony();ant.carry(false);var soil=p.offset(9,-1,0);l.setBlock(soil,Blocks.DIRT.defaultBlockState(),18);ant.moveTo(soil.getX()+.5,soil.getY()+1,soil.getZ()+.5,0,0);
        h.assertTrue(c.buildSite(l,ant)==null&&!c.excavate(l,soil,ant)&&l.getBlockState(soil).is(Blocks.DIRT),"A small insect remains unable to excavate even if its colony is enlarged");
        h.succeed();
    }
    @GameTest(template="assembly_test",batch="littleleaf",timeoutTicks=100)
    public static void colonyReturnFindsRealGroundWithoutFloatingDirt(GameTestHelper h){
        var c=setup(h);var l=h.getLevel();var p=c.getBlockPos();
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int y=0;y<=2;y++)if(x!=0||z!=0)l.setBlock(p.offset(x,y,z),Blocks.DIRT.defaultBlockState(),18);
        var q=ColonyTravel.safeReturn(l,p);
        h.assertTrue(q!=null&&q.getY()<=p.getY()+2&&l.getBlockState(q).getCollisionShape(l,q).isEmpty()&&l.getBlockState(q.below()).isFaceSturdy(l,q.below(),Direction.UP),"Exit searches the existing ground around a sealed mound");
        h.assertTrue(l.getBlockState(p.above(15)).isAir(),"No dirt platform appears above the colony");h.succeed();
    }
}
