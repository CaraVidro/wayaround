package net.caravidro.wayaround.industrial.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("wayaround_nature")
@PrefixGameTestTemplate(false)
public final class MiningHistoryGameTests {
    @GameTest(template="assembly_test",batch="history",timeoutTicks=80)
    public static void pitClearsNaturalRoofAndRepairsOldCursor(GameTestHelper h){
        var l=h.getLevel();BlockPos root=h.absolutePos(new BlockPos(20,1,8));
        for(int y=1;y<=16;y++)l.setBlock(root.above(y),Blocks.STONE.defaultBlockState(),3);
        l.setBlock(root.above(16),Blocks.GRASS_BLOCK.defaultBlockState(),3);
        l.setBlock(root.above(17),Blocks.OAK_LOG.defaultBlockState(),3);
        l.setBlock(root.above(18),Blocks.OAK_LEAVES.defaultBlockState(),3);
        l.setBlock(root,MiningContent.REGION_ANCHOR.get().defaultBlockState(),3);
        var anchor=(MiningRegionAnchorBlockEntity)l.getBlockEntity(root);anchor.configure(ComplexOreKind.IRON,true,12);
        anchor.cursor(12*25+12);
        var region=new MiningRegionRules.Region(0,0,root.getX(),root.getZ(),root.getX(),root.getZ(),ComplexOreKind.IRON,true,12,0);
        DeferredMiningManager.structureStep(l,root,anchor,region);
        h.assertTrue(l.getBlockState(root.above(16)).isAir()&&l.getBlockState(root.above(18)).isAir()&&l.canSeeSky(root.above(8)),"Grass and tree cap no longer roof an open pit");
        h.assertTrue(!DeferredMiningManager.pitCarvable(Blocks.COBBLESTONE.defaultBlockState())&&!DeferredMiningManager.pitCarvable(Blocks.OAK_PLANKS.defaultBlockState()),"Construction materials remain protected");
        var old=anchor.saveWithFullMetadata(l.registryAccess());old.remove("GeometryVersion");old.putInt("Stage",MiningRegionAnchorBlockEntity.Stage.COMPLETE.ordinal());
        anchor.loadWithComponents(old,l.registryAccess());
        h.assertTrue(anchor.roofRepairOnly()&&anchor.stage()==MiningRegionAnchorBlockEntity.Stage.STRUCTURE,"Old completed pits repair roof without rerolling ore, props or mobs");
        var saved=anchor.saveWithFullMetadata(l.registryAccess());anchor.loadWithComponents(saved,l.registryAccess());
        h.assertTrue(anchor.roofRepairOnly(),"Repair cursor survives reload");h.succeed();
    }
}
