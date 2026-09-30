package net.caravidro.wayaround.industrial.crushing;

import java.util.List;
import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.industrial.assembly.AssemblyItemData;
import net.caravidro.wayaround.industrial.power.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("wayaround_crushing")
@PrefixGameTestTemplate(false)
public final class CrusherGameTests {
    private static CrusherBlockEntity crusher(GameTestHelper helper,CrusherSize size,boolean assembled){
        BlockPos local=new BlockPos(5,2,5);
        helper.setBlock(local.below(),Blocks.STONE);
        helper.setBlock(local,CrusherContent.FRAMES.get(size).get());
        var entity=(CrusherBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(local));
        for(BlockPos pos:entity.supportPositions()){
            helper.getLevel().setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(pos,CrusherContent.IRON_SUPPORT.get().defaultBlockState());
        }
        if(assembled){
            Player player=helper.makeMockPlayer(GameType.SURVIVAL);
            for(String id:new String[]{"light_machine_shaft","plain_machine_bearing",size.id()+"_crushing_tool","narrow_machine_hopper"}){
                ItemStack stack=new ItemStack(CrusherContent.part(id),2);entity.install(player,stack);
                helper.assertTrue(stack.getCount()==1,"Installing consumes exactly one physical component");
            }
        }return entity;
    }
    @GameTest(template="assembly_test",batch="crushing",timeoutTicks=80)
    public static void assemblyAndSmallAdmission(GameTestHelper helper){
        var crusher=crusher(helper,CrusherSize.SMALL,false);
        helper.assertTrue(crusher.offer(new ItemStack(Items.RAW_IRON,64))==0,"Empty frame must not admit material");
        helper.assertTrue(crusher.assemblyParts().size()==1,"Empty frame cannot contain ghost tools");
        Player player=helper.makeMockPlayer(GameType.SURVIVAL);
        for(String id:new String[]{"light_machine_shaft","plain_machine_bearing","small_crushing_tool","wide_machine_hopper"})
            crusher.install(player,new ItemStack(CrusherContent.part(id)));
        helper.assertTrue(crusher.offer(new ItemStack(Items.RAW_IRON,64))==1,"Wide feed must not turn small crusher into a bulk processor");
        helper.assertTrue(crusher.offer(new ItemStack(Items.RAW_IRON))==0,"Full small crusher must reject another item");
        helper.assertTrue(crusher.parts().nodes(true).size()==4,"All four installed components appear in the Assembly graph");
        helper.succeed();
    }
    @GameTest(template="assembly_test",batch="crushing",timeoutTicks=80)
    public static void mediumNeedsPhysicalSupports(GameTestHelper helper){
        var crusher=crusher(helper,CrusherSize.MEDIUM,true);
        helper.assertTrue(crusher.offer(new ItemStack(Items.IRON_ORE,64))==64,"Medium admits a stack");
        helper.getLevel().setBlockAndUpdate(crusher.supportPositions().get(0),Blocks.AIR.defaultBlockState());
        helper.assertTrue(!crusher.supportsValid(),"Removing a placed beam must invalidate structural support");
        helper.assertTrue(crusher.offer(new ItemStack(Items.IRON_ORE))==0,"Unsupported intake must not silently eat items");
        helper.succeed();
    }
    @GameTest(template="assembly_test",batch="crushing",timeoutTicks=80)
    public static void largeAtomicPayloadAndSave(GameTestHelper helper){
        var crusher=crusher(helper,CrusherSize.LARGE,true);
        var valid=new ItemStack(Items.IRON_ORE,32);var invalid=new ItemStack(Items.DIRT);
        helper.assertTrue(!crusher.offerConnectedBatch(List.of(valid,invalid)),"Unsupported payload must be rejected atomically");
        helper.assertTrue(valid.getCount()==32&&crusher.inputCount()==0,"Failed delivery must preserve caller and machine inventories");
        helper.assertTrue(!crusher.offerConnectedBatch(List.of(valid,valid)),"The same stack reference cannot be delivered twice");
        helper.assertTrue(valid.getCount()==32&&crusher.inputCount()==0,"Aliased batch rejection must be atomic");
        var second=new ItemStack(Items.DEEPSLATE_IRON_ORE,32);
        helper.assertTrue(crusher.offerConnectedBatch(List.of(valid,second)),"Large intake accepts a physically delivered valid payload");
        helper.assertTrue(valid.isEmpty()&&second.isEmpty()&&crusher.inputCount()==64,"Successful delivery must transfer ownership without duplication");
        crusher.parts().wear(.08F,1);
        var registries=helper.getLevel().registryAccess();
        var restored=new CrusherBlockEntity(crusher.getBlockPos(),crusher.getBlockState());
        restored.loadWithComponents(crusher.getUpdateTag(registries),registries);
        helper.assertTrue(restored.inputCount()==64&&restored.parts().complete(),"Inventory and installed parts must survive reload");
        helper.assertTrue(AssemblyItemData.readPart(restored.parts().stack(MachinePartSpec.Role.TOOL)).wear()>0,"Actual component wear must survive reload");
        helper.succeed();
    }
    @GameTest(template="assembly_test",batch="crushing",timeoutTicks=80)
    public static void manualPowerProcessesIron(GameTestHelper helper){
        var crusher=crusher(helper,CrusherSize.SMALL,true);
        helper.setBlock(5,2,6,PowerContent.MANUAL_CRANK.get());
        var crank=(ManualCrankBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(5,2,6)));
        Player player=helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(crusher.offer(new ItemStack(Items.RAW_IRON))==1,"Assembled small intake accepts raw iron");
        for(int tick=0;tick<420;tick++){
            if(tick%20==0)crank.crank(player);
            ManualCrankBlockEntity.serverTick(helper.getLevel(),crank.getBlockPos(),crank.getBlockState(),crank);
            CrusherBlockEntity.serverTick(helper.getLevel(),crusher.getBlockPos(),crusher.getBlockState(),crusher);
        }
        helper.assertTrue(crusher.inputCount()==0&&crusher.outputCount()==1,"Real crank power must turn one raw iron into one crushed iron");
        helper.assertTrue(AssemblyItemData.readPart(crusher.parts().stack(MachinePartSpec.Role.TOOL)).wear()>0,"Work must wear the actual installed tool");
        helper.succeed();
    }
    @GameTest(template="assembly_test",batch="crushing",timeoutTicks=80)
    public static void millFrameAndLegacyMigration(GameTestHelper helper){
        helper.setBlock(5,2,5,PowerContent.MECHANICAL_MILL.get());
        var mill=(MechanicalMillBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(5,2,5)));
        helper.assertTrue(!mill.parts().complete()&&mill.assemblyParts().size()==1,"New mill must be an empty chassis");
        var legacy=new net.minecraft.nbt.CompoundTag();legacy.putInt("WheatInput",7);legacy.putInt("FlourOutput",4);
        mill.loadWithComponents(legacy,helper.getLevel().registryAccess());
        helper.assertTrue(mill.parts().complete()&&mill.flourOutput()==4,"Old mills must retain output and recover their prior physical components");
        var saved=mill.getUpdateTag(helper.getLevel().registryAccess());
        mill.parts().remove(MachinePartSpec.Role.DRIVE);
        saved=mill.getUpdateTag(helper.getLevel().registryAccess());
        mill.loadWithComponents(saved,helper.getLevel().registryAccess());
        helper.assertTrue(!mill.parts().complete(),"Reloading an incomplete new mill must not regenerate missing parts");
        helper.succeed();
    }
}
