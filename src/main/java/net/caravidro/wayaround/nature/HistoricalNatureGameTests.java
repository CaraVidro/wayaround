package net.caravidro.wayaround.nature;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("wayaround_nature")
@PrefixGameTestTemplate(false)
public final class HistoricalNatureGameTests {
    @GameTest(template="assembly_test",batch="history",timeoutTicks=80)
    public static void wildOrchardAndUnloadedSapling(GameTestHelper h){
        var l=h.getLevel();BlockPos p=h.absolutePos(new BlockPos(5,2,5));
        for(int x=-2;x<=2;x++)for(int y=0;y<=5;y++)for(int z=-2;z<=2;z++)l.setBlock(p.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);
        l.setBlock(p.below(),Blocks.DIRT.defaultBlockState(),3);
        h.assertTrue(AppleTreePlacement.place(l,p,l::hasChunkAt),"Wild tree can generate with no player");
        var tree=(AppleTreeBlockEntity)l.getBlockEntity(p);tree.initializeWild(l,l.getGameTime(),42);
        int ripe=0;
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
            var leaf=l.getBlockState(p.offset(x,3,z));
            if(leaf.is(NatureContent.APPLE_LEAVES.get())&&leaf.getValue(AppleLeavesBlock.FRUIT)==3)ripe++;
        }
        h.assertTrue(ripe>0,"Previously unseen wild tree already carries ripe apples");
        l.setBlock(p,NatureContent.APPLE_SAPLING.get().defaultBlockState(),3);
        for(int x=-2;x<=2;x++)for(int y=1;y<=5;y++)for(int z=-2;z<=2;z++)l.setBlock(p.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);
        ((AppleTreeBlockEntity)l.getBlockEntity(p)).advance(24000);
        h.assertTrue(l.getBlockState(p.offset(2,3,0)).getValue(AppleLeavesBlock.FRUIT)==3,"Unloaded sapling catches up growth AND fruit maturity in one pulse");
        h.succeed();
    }
    @GameTest(template="assembly_test",batch="history",timeoutTicks=80)
    public static void sunfishAlreadyBaskingAtDiscovery(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(7,2,7));
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)for(int y=0;y<=9;y++)l.setBlock(p.offset(x,y,z),y<4?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        var fish=net.caravidro.wayaround.ecology.EcologyContent.SUNFISH.get().create(l);
        fish.moveTo(p.getX()+.5,p.getY()+1,p.getZ()+.5,0,0);
        long offset=net.caravidro.wayaround.ecology.EcologicalHistory.phase(l.getSeed(),fish.getUUID().getMostSignificantBits(),0,8000);
        long start=16000+8000-offset;
        fish.restoreHistoricalBasking(start);
        h.assertTrue(fish.isBasking()&&fish.getY()>p.getY()+3,"First encounter can already be resting at open water surface");
        fish.restoreHistoricalBasking(start+1300);
        h.assertTrue(!fish.isBasking(),"Elapsed offscreen time ends the resting phase");
        h.succeed();
    }
}
