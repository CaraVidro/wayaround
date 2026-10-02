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
        h.assertTrue(pipe.amount()==760&&pipe.outletAmount()==240,"First 240 mB leaves the transport tank but remains conserved at the outlet");
        h.assertTrue(h.getBlockState(new BlockPos(5,2,4)).isAir(),"A partial bucket does not spawn a source block yet");
        var saved=pipe.saveWithoutMetadata(h.getLevel().registryAccess());pipe.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(pipe.amount()==760&&pipe.outletAmount()==240&&pipe.hasValve()&&pipe.open(),"Transport and outlet buffers, valve and orientation survive saves");
        for(int i=0;i<4;i++)PipeFlow.pump(h.getLevel(),pipe);
        h.assertTrue(pipe.amount()==0&&pipe.outletAmount()==0&&h.getBlockState(new BlockPos(5,2,4)).is(Blocks.WATER),"Five 240 mB-bounded transfers conserve one full source without deleting partial flow");
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
    public static void pressurePulseFollowsPumpDischargeRoute(GameTestHelper h){
        var root=pipe(h,5,2,5,PipeworkContent.SMALL_COPPER_PIPE.get());
        var next=pipe(h,5,2,4,PipeworkContent.SMALL_COPPER_PIPE.get());
        PipeFlow.applyPressurePulse(h.getLevel(),root,Direction.NORTH,5.5F);
        h.assertTrue(root.hydraulicPressureBar()>=5.4F,
                "Pump pressure reaches the discharge root even before useful flow");
        h.assertTrue(next.hydraulicPressureBar()>4.6F,
                "Pressure propagates through the bounded directional liquid route");
        h.succeed();
    }

    @GameTest(template="assembly_test",batch="mechanics",timeoutTicks=100)
    public static void narrowRotaryLiftIsVisualOnly(GameTestHelper h){
        BlockPos headLocal=new BlockPos(5,3,5);
        BlockPos crankLocal=new BlockPos(6,3,5);
        BlockPos sourceLocal=headLocal.below();
        BlockPos routeLocal=headLocal.relative(Direction.NORTH);

        h.setBlock(sourceLocal,Blocks.WATER);
        h.setBlock(
                headLocal,
                PipeworkContent.ROTARY_SMALL_COPPER_LIFT.get()
                        .defaultBlockState()
                        .setValue(RotaryLiftPipeBlock.FACING,Direction.NORTH)
        );
        h.setBlock(routeLocal,PipeworkContent.SMALL_COPPER_PIPE.get());
        h.setBlock(
                crankLocal,
                PowerContent.MANUAL_CRANK.get()
                        .defaultBlockState()
                        .setValue(ManualCrankBlock.FACING,Direction.WEST)
        );

        var head=(PipeBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(headLocal));
        var route=(PipeBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(routeLocal));
        var crank=(ManualCrankBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(crankLocal));
        crank.crank(h.makeMockPlayer(GameType.SURVIVAL));

        for(int tick=0;tick<18;tick++){
            ManualCrankBlockEntity.serverTick(h.getLevel(),crank.getBlockPos(),crank.getBlockState(),crank);
            PipeBlockEntity.tick(h.getLevel(),head.getBlockPos(),head.getBlockState(),head);
        }

        h.assertTrue(
                h.getBlockState(sourceLocal).is(Blocks.WATER),
                "Narrow rotary lift must never remove its reference water source"
        );
        h.assertTrue(
                route.wet()&&route.amount()==0,
                "Narrow lift propagates visible water through the route without creating stored volume"
        );
        h.succeed();
    }

    @GameTest(template="assembly_test",batch="mechanics",timeoutTicks=120)
    public static void largeRotaryLiftMovesOneRealSource(GameTestHelper h){
        BlockPos headLocal=new BlockPos(5,3,5);
        BlockPos crankLocal=new BlockPos(6,3,5);
        BlockPos sourceLocal=headLocal.below();
        BlockPos routeLocal=headLocal.relative(Direction.NORTH);
        BlockPos outletLocal=routeLocal.relative(Direction.NORTH);

        h.setBlock(sourceLocal,Blocks.WATER);
        h.setBlock(
                headLocal,
                PipeworkContent.ROTARY_LARGE_WATER_LIFT.get()
                        .defaultBlockState()
                        .setValue(RotaryLiftPipeBlock.FACING,Direction.NORTH)
        );
        h.setBlock(routeLocal,PipeworkContent.LARGE_WATER_MAIN.get());
        h.setBlock(
                crankLocal,
                PowerContent.MANUAL_CRANK.get()
                        .defaultBlockState()
                        .setValue(ManualCrankBlock.FACING,Direction.WEST)
        );

        var head=(PipeBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(headLocal));
        var crank=(ManualCrankBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(crankLocal));
        crank.crank(h.makeMockPlayer(GameType.SURVIVAL));

        for(int tick=0;tick<24;tick++){
            ManualCrankBlockEntity.serverTick(h.getLevel(),crank.getBlockPos(),crank.getBlockState(),crank);
            PipeBlockEntity.tick(h.getLevel(),head.getBlockPos(),head.getBlockState(),head);
        }

        h.assertTrue(
                h.getBlockState(sourceLocal).isAir(),
                "Large rotary lift removes the real source bucket at the intake"
        );
        h.assertTrue(
                h.getBlockState(outletLocal).is(Blocks.WATER),
                "Large rotary lift recreates the conserved source bucket at the terminal mouth"
        );
        h.assertTrue(
                head.amount()==0,
                "Successful large transfer leaves no duplicated hidden bucket in the intake head"
        );
        h.succeed();
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


    @GameTest(template="assembly_test",batch="mechanics",timeoutTicks=100)
    public static void crankActuallyDrivesMechanicalPump(GameTestHelper h){
        BlockPos pumpLocal=new BlockPos(5,2,5);
        BlockPos crankLocal=new BlockPos(6,2,5);
        BlockPos sourceLocal=new BlockPos(5,2,6);
        BlockPos dischargeLocal=new BlockPos(5,2,4);

        h.setBlock(pumpLocal.below(),Blocks.STONE);
        h.setBlock(
                pumpLocal,
                PipeworkContent.MECHANICAL_PUMP.get()
                        .defaultBlockState()
                        .setValue(MechanicalPumpBlock.FACING,Direction.NORTH)
        );
        h.setBlock(
                crankLocal,
                PowerContent.MANUAL_CRANK.get()
                        .defaultBlockState()
                        .setValue(ManualCrankBlock.FACING,Direction.WEST)
        );
        h.setBlock(sourceLocal,Blocks.WATER);
        pipe(h,5,2,4,PipeworkContent.SMALL_COPPER_PIPE.get());

        var pump=(MechanicalPumpBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pumpLocal));
        var crank=(ManualCrankBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(crankLocal));
        var player=h.makeMockPlayer(GameType.SURVIVAL);

        var cartridge=new ItemStack(PipeworkContent.PUMP_IMPELLER.get());
        pump.installImpeller(player,cartridge);
        crank.crank(player);

        float peakFlow=0;

        for(int tick=0;tick<18;tick++){
            ManualCrankBlockEntity.serverTick(
                    h.getLevel(),
                    crank.getBlockPos(),
                    crank.getBlockState(),
                    crank
            );
            MechanicalPumpBlockEntity.serverTick(
                    h.getLevel(),
                    pump.getBlockPos(),
                    pump.getBlockState(),
                    pump
            );
            peakFlow=Math.max(peakFlow,pump.flowPerTick());
        }

        h.assertTrue(
                h.getBlockState(sourceLocal).isAir(),
                "A powered pump must pull the real source block behind it"
        );
        h.assertTrue(
                pump.rpm()>8&&peakFlow>0,
                "Crank power must produce pump RPM and bounded discharge flow"
        );
        h.assertTrue(
                pump.bufferAmount()<1000,
                "Transferred liquid must leave the pump buffer instead of duplicating"
        );
        h.succeed();
    }

    @GameTest(template="assembly_test",batch="mechanics",timeoutTicks=80)
    public static void sharedMechanicalLoadSeparatesPowerTorqueAndSpeed(GameTestHelper h){
        IRotationalPower weakFast=new IRotationalPower(){
            public float rpm(){return 72F;}
            public float torque(){return 1.25F;}
            public float power(){return 2F;}
            public Direction.Axis axis(){return Direction.Axis.X;}
            public int rotationDirection(){return 1;}
        };

        MechanicalLoad.Demand demand=MechanicalLoad.sample(weakFast,4F,3F,48F);
        h.assertTrue(demand.powerStarved(),"A source with too little available power is detected as power-starved");
        h.assertTrue(demand.torqueStarved(),"High RPM cannot substitute missing torque for heavy work");
        h.assertTrue(demand.overspeed()>.45F,"The same shared model exposes overspeed relative to a machine-safe RPM");
        h.assertTrue(Math.abs(MechanicalLoad.fulfillment(4F,2F)-.5F)<.001F,
                "Granted power is represented as bounded demand fulfillment");

        float normal=MechanicalLoad.failureStress(.55F,0F,.08F,.18F,.95F);
        float abused=MechanicalLoad.failureStress(1.6F,.8F,.7F,1.2F,.2F);
        h.assertTrue(abused>normal&&abused>1F,
                "Combined load, speed, heat, vibration and poor condition produce higher failure stress");
        h.succeed();
    }


}
