package net.caravidro.wayaround.nature;

import net.caravidro.wayaround.ecology.EcologyContent;
import net.caravidro.wayaround.ecology.DeepOceanSurfaceRepair;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("wayaround_nature")
@PrefixGameTestTemplate(false)
public final class BirdBehaviorGameTests {
    @GameTest(template="assembly_test",batch="birds",timeoutTicks=80)
    public static void stolenFoodConservationAndReload(GameTestHelper h) {
        var l=h.getLevel();var crow=NatureContent.CROW.get().create(l);
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BREAD,4));
        h.assertTrue(BirdFoodTheftGoal.stealOne(crow,player),"Crow steals a held bite");
        h.assertTrue(player.getMainHandItem().getCount()==3&&crow.getMainHandItem().getCount()==1,"Exactly one real item transfers");
        h.assertTrue(!BirdFoodTheftGoal.stealOne(crow,player)&&player.getMainHandItem().getCount()==3,"A carrying crow cannot duplicate another bite");
        var saved=crow.saveWithoutId(new CompoundTag());var reload=NatureContent.CROW.get().create(l);reload.load(saved);
        h.assertTrue(reload.getMainHandItem().is(Items.BREAD)&&reload.getMainHandItem().getCount()==1,"Stolen stack persists");
        var gull=EcologyContent.SEAGULL.get().create(l);
        h.assertTrue(BirdFoodTheftGoal.stealOne(gull,player)&&player.getMainHandItem().getCount()==2,"Gull shares conservative theft behavior");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STONE));
        h.assertTrue(!BirdFoodTheftGoal.offersFood(player),"Tools and blocks are not food");
        h.assertTrue(WoodlandBirdEntity.nectarOffer(new ItemStack(Items.DANDELION))&&WoodlandBirdEntity.nectarOffer(new ItemStack(Items.SUGAR)),"Flower and sugar tempt hummingbirds");
        h.succeed();
    }
    @GameTest(template="assembly_test",batch="birds",timeoutTicks=80)
    public static void birdRoutesAroundSolidWall(GameTestHelper h) {
        var l=h.getLevel();
        for(int x=2;x<=12;x++)for(int y=1;y<=8;y++)for(int z=2;z<=12;z++)l.setBlock(h.absolutePos(new BlockPos(x,y,z)),Blocks.AIR.defaultBlockState(),3);
        for(int x=2;x<=12;x++)for(int z=2;z<=12;z++)l.setBlock(h.absolutePos(new BlockPos(x,0,z)),Blocks.DIRT.defaultBlockState(),3);
        for(int y=1;y<=6;y++)for(int z=4;z<=9;z++)l.setBlock(h.absolutePos(new BlockPos(7,y,z)),Blocks.STONE.defaultBlockState(),3);
        var bird=NatureContent.HUMMINGBIRD.get().create(l);BlockPos start=h.absolutePos(new BlockPos(4,3,6));bird.moveTo(start.getX()+.5,start.getY(),start.getZ()+.5,0,0);l.addFreshEntity(bird);
        BlockPos target=h.absolutePos(new BlockPos(10,3,6));var path=bird.getNavigation().createPath(target,1);
        System.out.println("Bird follow range="+bird.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE)+", start="+start+", target="+target+", box="+bird.getBoundingBox());
        if(path!=null)for(int i=0;i<path.getNodeCount();i++){var n=path.getNode(i);System.out.println("Bird node="+n+", type="+n.type+", malus="+n.costMalus);}
        if(path!=null)System.out.println("Bird route: nodes="+path.getNodeCount()+", reachable="+path.canReach()+", distance="+path.getDistToTarget());
        h.assertTrue(path!=null&&path.canReach(),"Flight navigation reaches the far side of the wall");
        for(int i=0;i<path.getNodeCount();i++){
            var n=path.getNode(i);h.assertTrue(!l.getBlockState(new BlockPos(n.x,n.y,n.z)).is(Blocks.STONE),"Route never walks through wall cells");
        }
        bird.discard();h.succeed();
    }
    @GameTest(template="assembly_test",batch="birds",timeoutTicks=80)
    public static void legacyRaisedOceanLayerDetection(GameTestHelper h) {
        var l=h.getLevel();BlockPos p=new BlockPos(h.absolutePos(new BlockPos(5,2,5)).getX(),l.getSeaLevel(),h.absolutePos(new BlockPos(5,2,5)).getZ());
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            for(int y=-3;y<=0;y++)l.setBlock(p.offset(x,y,z),Blocks.WATER.defaultBlockState(),2);
            l.setBlock(p.offset(x,1,z),Blocks.AIR.defaultBlockState(),2);
        }
        h.assertTrue(DeepOceanSurfaceRepair.raisedLayer(l,p),"Legacy sheet is exactly one block above the sea");
        l.setBlock(p.above(),Blocks.STONE.defaultBlockState(),2);
        h.assertTrue(!DeepOceanSurfaceRepair.raisedLayer(l,p),"Covered water is protected");
        h.assertTrue(!DeepOceanSurfaceRepair.raisedLayer(l,p.below()),"Normal sea height is never removed");
        h.succeed();
    }
}
