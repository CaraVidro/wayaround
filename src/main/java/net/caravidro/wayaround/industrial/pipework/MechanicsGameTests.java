package net.caravidro.wayaround.industrial.pipework;
import net.caravidro.wayaround.industrial.mechanical.*;
import net.caravidro.wayaround.industrial.power.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.fluids.FluidStack;

@GameTestHolder("wayaround_crushing")
@PrefixGameTestTemplate(false)
public final class MechanicsGameTests {
    private static PipeBlockEntity pipe(GameTestHelper h,int x,int y,int z,Block block){h.setBlock(x,y,z,block);return (PipeBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(x,y,z)));}
    @GameTest(template="assembly_test",batch="mechanics",timeoutTicks=80)
    public static void worldFluidConservationAndPersistence(GameTestHelper h){
        var pipe=pipe(h,5,2,5,PipeworkContent.SMALL_COPPER_PIPE.get());
        h.setBlock(5,2,6,Blocks.WATER);
        var player=h.makeMockPlayer(GameType.SURVIVAL);var valve=new ItemStack(PipeworkContent.VALVE.get(),2);
        h.assertTrue(pipe.installValve(valve,player,Direction.NORTH)&&valve.getCount()==1,"Valve consumes one actual item");
        pipe.turn(player);PipeFlow.pump(h.getLevel(),pipe);
        h.assertTrue(h.getBlockState(new BlockPos(5,2,6)).isAir(),"Intake removes the actual source block");
        h.assertTrue(pipe.amount()==760,"A 1000-unit bucket loses only the 240-unit outlet spray");
        h.assertTrue(h.getBlockState(new BlockPos(5,2,4)).isAir(),"Small outlet sprays rather than spawning a source block");
        var saved=pipe.saveWithoutMetadata(h.getLevel().registryAccess());pipe.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(pipe.amount()==760&&pipe.hasValve()&&pipe.open(),"Fluid budget, valve and orientation survive saves");
        h.succeed();
    }
    @GameTest(template="assembly_test",batch="mechanics",timeoutTicks=80)
    public static void assembledLargeTransfersExactlyOneSource(GameTestHelper h){
        var pipe=pipe(h,5,4,5,PipeworkContent.GIANT.get());pipe.firstSection();
        var player=h.makeMockPlayer(GameType.SURVIVAL);var sections=new ItemStack(PipeworkContent.GIANT_ITEM.get(),20);
        for(int i=1;i<15;i++)h.assertTrue(pipe.assemble(sections,player),"Every stage requires a paid section");
        h.assertTrue(sections.getCount()==6&&pipe.complete(),"Giant has exactly fifteen physical sections");
        h.assertTrue(!pipe.assemble(sections,player)&&sections.getCount()==6,"Complete duct does not eat more sections");
        h.assertTrue(pipe.getBlockState().getCollisionShape(h.getLevel(),pipe.getBlockPos()).isEmpty(),"Player corridor must remain hollow");
        for(BlockPos p:PipeBlockEntity.shellPositions(pipe.getBlockPos(),pipe.getBlockState()))h.assertTrue(h.getLevel().getBlockEntity(p) instanceof PipeBlockEntity shell&&pipe.getBlockPos().equals(shell.owner()),"Shell collision belongs to controller");
        h.setBlock(5,4,3,Blocks.WATER);pipe.installValve(new ItemStack(PipeworkContent.VALVE.get()),player,Direction.SOUTH);pipe.turn(player);PipeFlow.pump(h.getLevel(),pipe);
        h.assertTrue(h.getBlockState(new BlockPos(5,4,3)).isAir()&&h.getBlockState(new BlockPos(5,4,7)).is(Blocks.WATER)&&pipe.amount()==0,"Source moves to large mouth without duplication");h.succeed();
    }
    @GameTest(template="assembly_test",batch="mechanics",timeoutTicks=80)
    public static void obstructedAssemblyKeepsItems(GameTestHelper h){
        var pipe=pipe(h,5,4,5,PipeworkContent.COLOSSAL.get());pipe.firstSection();
        h.setBlock(7,4,6,Blocks.STONE);var stack=new ItemStack(PipeworkContent.COLOSSAL_ITEM.get(),49);
        h.assertTrue(!pipe.assemble(stack,h.makeMockPlayer(GameType.SURVIVAL))&&stack.getCount()==49&&pipe.sections()==1,"Obstructions cannot consume parts or overwrite blocks");h.succeed();
    }
    @GameTest(template="assembly_test",batch="mechanics",timeoutTicks=80)
    public static void closedValveStopsFlow(GameTestHelper h){
        var root=pipe(h,5,2,5,PipeworkContent.IRON_WATER_PIPE.get());var last=pipe(h,5,2,4,PipeworkContent.IRON_WATER_PIPE.get());
        var player=h.makeMockPlayer(GameType.SURVIVAL);root.installValve(new ItemStack(PipeworkContent.VALVE.get()),player,Direction.NORTH);root.turn(player);
        last.installValve(new ItemStack(PipeworkContent.VALVE.get()),player,Direction.NORTH);h.setBlock(5,2,6,Blocks.WATER);
        PipeFlow.pump(h.getLevel(),root);h.assertTrue(h.getBlockState(new BlockPos(5,2,6)).is(Blocks.WATER),"Closed downstream valve stops intake before draining anything");
        last.turn(player);PipeFlow.pump(h.getLevel(),root);h.assertTrue(h.getBlockState(new BlockPos(5,2,6)).isAir()&&root.amount()==280,"Open route transfers exactly one bounded outlet amount");h.succeed();
    }
    @GameTest(template="assembly_test",batch="mechanics",timeoutTicks=80)
    public static void exposedGearRatiosAndBevel(GameTestHelper h){
        h.setBlock(5,2,5,GearContent.LARGE.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.Z));
        h.setBlock(6,2,5,GearContent.SMALL.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.Z));
        h.setBlock(5,2,6,PowerContent.MANUAL_CRANK.get());var crank=(ManualCrankBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(5,2,6)));
        crank.crank(h.makeMockPlayer(GameType.SURVIVAL));ManualCrankBlockEntity.serverTick(h.getLevel(),crank.getBlockPos(),crank.getBlockState(),crank);
        var large=MechanicalTransmission.forNode(h.getLevel(),h.absolutePos(new BlockPos(5,2,5)));var small=MechanicalTransmission.forNode(h.getLevel(),h.absolutePos(new BlockPos(6,2,5)));
        h.assertTrue(large!=null&&small!=null&&Math.abs(small.rpm()+large.rpm()*2)<.01,"Half the teeth doubles RPM and reverses direction");
        h.assertTrue(small.torque()<large.torque()&&small.power()<=large.power(),"Higher speed sacrifices torque and never creates power");
        h.setBlock(6,2,5,GearContent.SMALL.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X));
        var bevel=MechanicalTransmission.forNode(h.getLevel(),h.absolutePos(new BlockPos(6,2,5)));h.assertTrue(bevel!=null&&bevel.axis()==Direction.Axis.X,"Perpendicular exposed gears turn the output axis");h.succeed();
    }
    @GameTest(template="assembly_test",batch="mechanics",timeoutTicks=80)
    public static void diagonalSmallGearAndCombinedBudgets(GameTestHelper h){
        h.setBlock(5,2,5,GearContent.LARGE.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.Z));
        h.setBlock(6,3,5,GearContent.SMALL.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.Z));
        h.setBlock(5,2,6,PowerContent.MANUAL_CRANK.get());h.setBlock(5,2,4,PowerContent.MANUAL_CRANK.get());
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        for(int z:new int[]{4,6}){var crank=(ManualCrankBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(5,2,z)));crank.crank(player);ManualCrankBlockEntity.serverTick(h.getLevel(),crank.getBlockPos(),crank.getBlockState(),crank);}
        var combined=MechanicalTransmission.forNode(h.getLevel(),h.absolutePos(new BlockPos(6,3,5)));
        h.assertTrue(combined!=null&&combined.power()>0,"Small diagonal gear receives both independent source budgets");
        float first=combined.consumePower(10000),second=combined.consumePower(10000);
        h.assertTrue(first>0&&second==0,"Combined source budgets cannot be spent twice in a tick");h.succeed();
    }
    @GameTest(template="assembly_test",batch="mechanics",timeoutTicks=80)
    public static void twoWaterWheelsShareAnAxle(GameTestHelper h){
        h.setBlock(5,2,5,PowerContent.MECHANICAL_SHAFT.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.Z));
        for(int z:new int[]{4,6}){
            h.setBlock(5,2,z,PowerContent.WATER_WHEEL_HUB.get());
            var wheel=(WaterWheelHubBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(5,2,z)));
            var saved=wheel.getUpdateTag(h.getLevel().registryAccess());saved.putFloat("Rpm",20);saved.putFloat("Torque",5);saved.putFloat("MechanicalPower",10);
            wheel.loadWithComponents(saved,h.getLevel().registryAccess());
        }
        var combined=MechanicalTransmission.forNode(h.getLevel(),h.absolutePos(new BlockPos(5,2,5)));
        h.assertTrue(combined!=null&&combined.power()>10&&combined.torque()>5,"Two distinct water wheels sum available force on their shared axle");
        h.assertTrue(Math.abs(combined.rpm()-20)<.01,"Equal wheel speeds do not magically double unloaded RPM");
        var second=(WaterWheelHubBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(5,2,6)));
        var saved=second.getUpdateTag(h.getLevel().registryAccess());saved.putFloat("Rpm",12);second.loadWithComponents(saved,h.getLevel().registryAccess());
        var unequal=MechanicalTransmission.forNode(h.getLevel(),h.absolutePos(new BlockPos(5,2,5)));
        h.assertTrue(unequal!=null&&unequal.power()>10&&Math.abs(unequal.rpm()-16)<.01,"Slower same-direction wheels still contribute their real power");
        saved.putFloat("Rpm",-20);second.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(MechanicalTransmission.forNode(h.getLevel(),h.absolutePos(new BlockPos(5,2,5)))==null,"Opposing wheels cannot magically clutch themselves out of a rigid axle");h.succeed();
    }
    @GameTest(template="assembly_test",batch="mechanics",timeoutTicks=80)
    public static void fiftySectionsAndLavaRemainPhysical(GameTestHelper h){
        var pipe=pipe(h,5,4,5,PipeworkContent.COLOSSAL.get());pipe.firstSection();
        var player=h.makeMockPlayer(GameType.SURVIVAL);var parts=new ItemStack(PipeworkContent.COLOSSAL_ITEM.get(),49);
        for(int i=1;i<50;i++)h.assertTrue(pipe.assemble(parts,player),"Colossal stages must accept their real section");
        h.assertTrue(parts.isEmpty()&&pipe.sections()==50&&pipe.complete(),"Exactly fifty sections finish the colossal duct");
        h.setBlock(5,4,3,Blocks.LAVA);pipe.installValve(new ItemStack(PipeworkContent.VALVE.get()),player,Direction.SOUTH);pipe.turn(player);PipeFlow.pump(h.getLevel(),pipe);
        h.assertTrue(h.getBlockState(new BlockPos(5,4,3)).isAir()&&h.getBlockState(new BlockPos(5,4,7)).is(Blocks.LAVA),"Liquid transport also moves real lava without replacing it with water");
        var saved=pipe.saveWithoutMetadata(h.getLevel().registryAccess());pipe.loadWithComponents(saved,h.getLevel().registryAccess());h.assertTrue(pipe.sections()==50,"Assembly count persists");
        pipe.dismantle();
        for(BlockPos pos:PipeBlockEntity.shellPositions(pipe.getBlockPos(),pipe.getBlockState()))h.assertTrue(!h.getLevel().getBlockState(pos).is(PipeworkContent.COLOSSAL.get()),"Dismantling removes every owned shell block");
        int dropped=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pipe.getBlockPos()).inflate(2)).stream().filter(e->e.getItem().is(PipeworkContent.COLOSSAL_ITEM.get())).mapToInt(e->e.getItem().getCount()).sum();
        h.assertTrue(dropped==50,"All fifty paid pieces are recovered exactly once");h.succeed();
    }

    @GameTest(template="assembly_test",batch="mechanics",timeoutTicks=80)
    public static void damagedFilledPipeLeaksItsStoredLiquid(GameTestHelper h){
        var pipe=pipe(h,5,2,5,PipeworkContent.IRON_WATER_PIPE.get());
        pipe.receive(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000));
        pipe.receiveStructuralDamage(new net.caravidro.wayaround.interaction.StructuralDamage(net.minecraft.world.phys.Vec3.atCenterOf(pipe.getBlockPos()),20,0,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("wayaround","test"),null));
        h.assertTrue(pipe.structuralIntegrity()<.65F,"Physical structural damage weakens the filled pipe");
        h.runAfterDelay(25,()->{h.assertTrue(pipe.amount()==990||pipe.amount()==980,"Leaking consumes buffered liquid even with its valve closed");h.succeed();});
    }


    @GameTest(template="assembly_test",batch="mechanics",timeoutTicks=80)
    public static void mechanicalPumpUsesOneInstalledModule(GameTestHelper h){
        BlockPos local=new BlockPos(5,2,5);
        h.setBlock(local.below(),Blocks.STONE);
        h.setBlock(local,PipeworkContent.MECHANICAL_PUMP.get());
        var pump=(MechanicalPumpBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(local));
        h.assertTrue(pump!=null&&!pump.hasImpeller()&&pump.assemblyParts().size()==1,
                "New pump is a housing, not a magically complete machine");
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var cartridge=new ItemStack(PipeworkContent.PUMP_IMPELLER.get(),2);
        pump.installImpeller(player,cartridge);
        h.assertTrue(cartridge.getCount()==1&&pump.hasImpeller()&&pump.assemblyParts().size()==2,
                "Exactly one physical impeller cartridge completes the pump internals");
        h.succeed();
    }

    @GameTest(template="assembly_test",batch="mechanics",timeoutTicks=80)
    public static void machinePullAndPushConserveFluid(GameTestHelper h){
        BlockPos sourceLocal=new BlockPos(5,2,6);
        h.setBlock(sourceLocal,Blocks.WATER);
        FluidStack pulled=PipeFlow.pullForMachine(
                h.getLevel(),
                h.absolutePos(sourceLocal),
                Direction.NORTH,
                1000,
                FluidStack.EMPTY
        );
        h.assertTrue(pulled.getAmount()==1000&&h.getBlockState(sourceLocal).isAir(),
                "Machine suction removes exactly one real source bucket");

        var outlet=pipe(h,5,2,5,PipeworkContent.SMALL_COPPER_PIPE.get());
        int pushed=PipeFlow.pushFromMachine(
                h.getLevel(),
                outlet,
                Direction.NORTH,
                pulled,
                1000
        );
        h.assertTrue(pushed==240,
                "Pump discharge is capped by the attached copper-pipe bottleneck");
        h.assertTrue(pulled.getAmount()==1000,
                "PipeFlow reports consumed amount; caller still owns and shrinks its buffer exactly once");
        h.succeed();
    }

}
