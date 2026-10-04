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
        InversionEffect.invert(ant);long before=c.work();h.assertTrue(c.acceptLoad(ant)&&c.work()==before&&!ant.carrying(),"An enlarged outsider cannot feed the small inner garden");
        h.assertTrue(l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(leaf).inflate(4)).stream().anyMatch(i->i.getItem().is(LittleLeafContent.LEAF_FRAGMENT.get())&&i.getItem().getCount()==4),"Larger real load left outside");
        h.assertTrue(!c.acceptLoad(ant),"The same load cannot be deposited twice");h.succeed();
    }
    @GameTest(template="assembly_test",batch="littleleaf",timeoutTicks=100)
    public static void colonyDoseChangesBirthScaleAndLargeDelivery(GameTestHelper h){
        var c=setup(h);var l=h.getLevel();var ant=LittleLeafContent.TERMITE.get().create(l);
        ColonyEvents.dose(l,Vec3.atCenterOf(c.getBlockPos()));h.assertTrue(c.giant(),"Splash at the core changes colony inversion");
        ant.bind(c.getBlockPos(),0,false,c.giant());ant.moveTo(c.getBlockPos().getX()+3,c.getBlockPos().getY(),c.getBlockPos().getZ(),0,0);ant.carry(true);
        long before=c.work();h.assertTrue(ant.getScale()>=3&&c.acceptLoad(ant)&&c.work()==before+8,"New giant inhabitants feed the enhanced colony with larger loads");
        ColonyEvents.dose(l,Vec3.atCenterOf(c.getBlockPos()));h.assertTrue(!c.giant(),"Second colony splash reverses future births");h.succeed();
    }
    @GameTest(template="assembly_test",batch="littleleaf",timeoutTicks=180)
    public static void aWorkerActuallyEntersAndReturnsFromTheGarden(GameTestHelper h){
        var c=setup(h);var l=h.getLevel();var p=c.getBlockPos();var ant=LittleLeafContent.BLACK_ANT.get().create(l);ant.bind(p,0,false,false);ant.carry(true);ant.moveTo(p.getX()+.5,p.getY(),p.getZ()+1.5,0,0);l.addFreshEntity(ant);
        h.succeedWhen(()->h.assertTrue(c.work()>=1&&!ant.carrying()&&ant.getY()>=p.getY(),"Worker entered the physical chamber, delivered once and emerged"));
    }
    @GameTest(template="assembly_test",batch="littleleaf",timeoutTicks=100)
    public static void attackingPersistsColonyHostilityAndQueenDeath(GameTestHelper h){
        var c=setup(h);var l=h.getLevel();var p=c.getBlockPos();var soldier=LittleLeafContent.RED_ANT.get().create(l);soldier.bind(p,1,false,false);
        var player=h.makeMockPlayer(GameType.SURVIVAL);soldier.hurt(l.damageSources().playerAttack(player),1);
        h.assertTrue(c.hostile(player),"Direct attack is shared with the colony");var saved=c.saveWithFullMetadata(l.registryAccess());c.loadWithComponents(saved,l.registryAccess());h.assertTrue(c.hostile(player),"Colony remembers the attacker after reload");
        c.initialize(l.getGameTime(),0,true);var queen=LittleLeafContent.RED_ANT.get().create(l);queen.bind(p,2,false,false);queen.die(l.damageSources().generic());c.advance(l.getGameTime()+24000);h.assertTrue(c.work()==0,"Dead queen stops aggregate growth");h.succeed();
    }
}
