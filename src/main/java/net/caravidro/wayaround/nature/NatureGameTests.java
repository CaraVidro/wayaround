package net.caravidro.wayaround.nature;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("wayaround_nature")
@PrefixGameTestTemplate(false)
public final class NatureGameTests {
    @GameTest(template="wayaround_crushing:assembly_test",batch="nature",timeoutTicks=80)
    public static void orchardGrowthHarvestAndReload(GameTestHelper h) {
        BlockPos root=h.absolutePos(new BlockPos(5,2,5));
        var level=h.getLevel();
        for(int x=-2;x<=2;x++)for(int y=0;y<=5;y++)for(int z=-2;z<=2;z++)level.setBlock(root.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);
        level.setBlock(root.below(),Blocks.DIRT.defaultBlockState(),3);
        level.setBlock(root,NatureContent.APPLE_SAPLING.get().defaultBlockState(),3);
        ((AppleTreeBlockEntity)level.getBlockEntity(root)).advance(6000);
        h.assertTrue(level.getBlockState(root).is(NatureContent.APPLE_LOG.get()),"Sapling becomes trunk");
        var tree=(AppleTreeBlockEntity)level.getBlockEntity(root);
        BlockPos fruit=root.offset(2,3,0);
        h.assertTrue(level.getBlockState(fruit).getValue(AppleLeavesBlock.FRUIT)==1,"Physical green fruit starts on canopy");
        tree.advance(8000);
        h.assertTrue(level.getBlockState(fruit).getValue(AppleLeavesBlock.FRUIT)==3,"Ecological clock ripens fruit");
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var state=level.getBlockState(fruit);
        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(fruit),net.minecraft.core.Direction.DOWN,fruit,false);
        NatureContent.APPLE_LEAVES.get().useWithoutItem(state,level,fruit,player,hit);
        h.assertTrue(player.getInventory().countItem(net.minecraft.world.item.Items.APPLE)==1,"Right click gives exactly one apple");
        NatureContent.APPLE_LEAVES.get().useWithoutItem(level.getBlockState(fruit),level,fruit,player,hit);
        h.assertTrue(player.getInventory().countItem(net.minecraft.world.item.Items.APPLE)==1,"Second click cannot duplicate fruit");
        tree.advance(0);
        var saved=tree.saveWithFullMetadata(level.registryAccess());
        tree.loadWithComponents(saved,level.registryAccess());
        tree.advance(3999);
        h.assertTrue(level.getBlockState(fruit).getValue(AppleLeavesBlock.FRUIT)==0,"Harvest cooldown survives reload");
        tree.advance(8001);
        h.assertTrue(level.getBlockState(fruit).getValue(AppleLeavesBlock.FRUIT)==3,"Apples regrow without replacing canopy");
        h.succeed();
    }
    @GameTest(template="wayaround_crushing:assembly_test",batch="nature",timeoutTicks=80)
    public static void blockedTreeAndBirdHealth(GameTestHelper h) {
        BlockPos root=h.absolutePos(new BlockPos(5,2,5));var l=h.getLevel();
        l.setBlock(root,NatureContent.APPLE_SAPLING.get().defaultBlockState(),3);
        l.setBlock(root.above(2),Blocks.GOLD_BLOCK.defaultBlockState(),3);
        var tree=(AppleTreeBlockEntity)l.getBlockEntity(root);
        h.assertTrue(!tree.grow()&&l.getBlockState(root.above(2)).is(Blocks.GOLD_BLOCK),"Growth never overwrites a building");
        var tiny=NatureContent.HUMMINGBIRD.get().create(l);var small=NatureContent.THRUSH.get().create(l);var medium=NatureContent.PARROT.get().create(l);
        h.assertTrue(tiny.getMaxHealth()==4&&small.getMaxHealth()==8&&medium.getMaxHealth()==14,"Distinct registered health");
        h.assertTrue(tiny.getBbWidth()<small.getBbWidth()&&small.getBbWidth()<medium.getBbWidth(),"Three distinct sizes");
        h.succeed();
    }
}
