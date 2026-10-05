package net.caravidro.wayaround.industrial.pipework;

import java.util.*;
import net.caravidro.wayaround.environment.EnvironmentalFields;
import net.caravidro.wayaround.flow.FlowState;
import net.caravidro.wayaround.flow.UniversalFlow;
import net.caravidro.wayaround.industrial.assembly.*;
import net.caravidro.wayaround.interaction.*;
import net.caravidro.wayaround.physical.FluidMatterResolver;
import net.caravidro.wayaround.pressure.NaturalPressure;
import net.caravidro.wayaround.pressure.PressureMath;
import net.caravidro.wayaround.worldconfig.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Single persistent fluid budget per valve, bounded routing, real paid shell sections. */
public final class PipeBlockEntity extends BlockEntity implements StructuralReceiver {
    private BlockPos owner;
    private int sections;
    private final List<ItemStack> installedSections=new ArrayList<>();
    private ItemStack valve=ItemStack.EMPTY;
    private ItemStack body=ItemStack.EMPTY;
    private Direction flow=Direction.NORTH;
    private boolean open,removing;
    private FluidStack tank=FluidStack.EMPTY;
    /*
     * Keep discharge accumulation separate from the transport/source tank.
     * This is the v1.5 Ore Washer/partial-output conservation fix.
     */
    private FluidStack outletBuffer=FluidStack.EMPTY;
    private FluidStack visible=FluidStack.EMPTY;
    private long wetUntil;
    private float integrity=1;
    private float hydraulicPressureBar;
    private float peakHydraulicPressureBar;
    private float recentFlowMbPerTick;
    private float rotaryLiftRpm;
    private float rotaryLiftAngle;
    private float rotaryLiftLoad;
    private boolean rotaryLiftTorqueStarved;
    private int outletCursor;
    public PipeBlockEntity(BlockPos pos,BlockState state){super(PipeworkContent.PIPE_ENTITY.get(),pos,state);}
    public void restoreBody(ItemStack stack){body=section(stack);sync();}
    public BlockPos owner(){return owner;}
    public int sections(){return sections;}
    public boolean hasValve(){return !valve.isEmpty();}
    public boolean open(){return open;}
    public Direction flow(){return flow;}
    public boolean wet(){return !tank.isEmpty()||(level!=null&&level.getGameTime()<wetUntil);}
    public FluidStack visualFluid(){return !tank.isEmpty()?tank:visible;}
    public boolean complete(){return !(getBlockState().getBlock() instanceof LargePipeBlock b)||sections==b.required();}
    public int capacity(){return getBlockState().getBlock() instanceof LargePipeBlock b?(b.colossal()?64000:16000):2000;}
    public int amount(){return tank.getAmount();}
    public float hydraulicPressureBar(){return hydraulicPressureBar;}
    public float hydraulicPressureKPa(){return (float)PressureMath.barToKPa(hydraulicPressureBar);}
    public float peakHydraulicPressureBar(){return peakHydraulicPressureBar;}
    public float recentFlowMbPerTick(){return recentFlowMbPerTick;}
    public float rotaryLiftRpm(){return rotaryLiftRpm;}
    public float rotaryLiftAngle(){return rotaryLiftAngle;}
    public float rotaryLiftLoad(){return rotaryLiftLoad;}
    public boolean rotaryLiftTorqueStarved(){return rotaryLiftTorqueStarved;}
    public float rotaryLiftVibration(){
        return Math.clamp(
                (rotaryLiftTorqueStarved?.42F:0F)
                        +Math.max(0,rotaryLiftLoad-1F)*.28F
                        +(1F-integrity)*.25F,
                0F,1.5F);
    }
    public float rotaryLiftCondition(){
        float bodyCondition=AssemblyItemData.materialMechanicalIntegrity(body);
        var profile=AssemblyItemData.readPart(body);
        if(profile!=null)bodyCondition*=profile.durabilityScore()*profile.performanceFactor();
        return Math.clamp(bodyCondition*integrity,.08F,1F);
    }
    public void updateRotaryLift(float rpm,float load,boolean torqueStarved){
        float previous=rotaryLiftRpm;
        rotaryLiftRpm=finite(rpm,-180F,180F);
        rotaryLiftLoad=finite(load,0F,4F);
        rotaryLiftTorqueStarved=torqueStarved;
        rotaryLiftAngle=(rotaryLiftAngle+rotaryLiftRpm*.30F)%360F;
        setChanged();
        if(level!=null&&!level.isClientSide
                &&Math.floorMod(level.getGameTime()+worldPosition.asLong(),5)==0
                &&(Math.abs(previous-rotaryLiftRpm)>.02F||Math.abs(rotaryLiftRpm)>.01F))sync();
    }
    public PipeBlockEntity controller(){return owner==null?this:level!=null&&level.hasChunkAt(owner)&&level.getBlockEntity(owner) instanceof PipeBlockEntity pipe?pipe:null;}
    public static List<BlockPos> shellPositions(BlockPos center,BlockState state){
        if(!(state.getBlock() instanceof LargePipeBlock b))return List.of();
        ArrayList<BlockPos> positions=new ArrayList<>();int r=b.radius();Direction.Axis axis=state.getValue(LargePipeBlock.AXIS);
        for(int along=-1;along<=1;along++)for(int a=-r;a<=r;a++)for(int c=-r;c<=r;c++){
            if(Math.abs(a)!=r&&Math.abs(c)!=r)continue;
            positions.add(switch(axis){case X->center.offset(along,a,c);case Y->center.offset(a,along,c);case Z->center.offset(a,c,along);});
        }return positions;
    }
    public static boolean roomFor(Level level,BlockPos center,BlockState state){
        if(!level.isInWorldBounds(center))return false;
        for(BlockPos p:shellPositions(center,state)){
            if(!level.isInWorldBounds(p)||!level.hasChunkAt(p))return false;
            BlockState existing=level.getBlockState(p);
            if(level.getBlockEntity(p) instanceof PipeBlockEntity part&&center.equals(part.owner))continue;
            if(!existing.canBeReplaced())return false;
        }return true;
    }
    private ItemStack section(ItemStack stack){
        ItemStack piece=stack.copyWithCount(1);
        if(level instanceof ServerLevel server)AssemblyItemData.ensurePart(piece,AssemblyPartProfile.Kind.FRAME,0,server.random);
        return piece;
    }
    public void firstSection(){firstSection(new ItemStack(getBlockState().getBlock().asItem()));}
    public void firstSection(ItemStack stack){if(sections>0)return;installedSections.add(section(stack));sections=1;flow=Direction.get(Direction.AxisDirection.POSITIVE,getBlockState().getValue(LargePipeBlock.AXIS));buildShell();sync();}
    public boolean assemble(ItemStack stack,Player player){
        if(!(getBlockState().getBlock() instanceof LargePipeBlock b)||sections>=b.required()||!stack.is(b.asItem())||!roomFor(level,worldPosition,getBlockState()))return false;
        installedSections.add(section(stack));sections++;stack.consume(1,player);buildShell();
        level.playSound(null,worldPosition,SoundEvents.ANVIL_PLACE,SoundSource.BLOCKS,.35F,.8F+sections/(float)b.required()*.4F);sync();return true;
    }
    private void buildShell(){
        if(level==null||level.isClientSide||!(getBlockState().getBlock() instanceof LargePipeBlock b))return;
        List<BlockPos> positions=shellPositions(worldPosition,getBlockState());int count=(positions.size()*sections+b.required()-1)/b.required();
        for(int i=0;i<count;i++){BlockPos p=positions.get(i);
            if(level.getBlockEntity(p) instanceof PipeBlockEntity existing&&worldPosition.equals(existing.owner))continue;
            BlockState existing=level.getBlockState(p);
            if(!existing.canBeReplaced())continue;
            BlockState shellState=getBlockState().setValue(LargePipeBlock.SHELL,true)
                    .setValue(LargePipeBlock.WATERLOGGED,level.getFluidState(p).getType()==net.minecraft.world.level.material.Fluids.WATER);
            level.setBlock(p,shellState,3);
            if(level.getBlockEntity(p) instanceof PipeBlockEntity shell){shell.owner=worldPosition;shell.sync();}
        }
    }
    public boolean installValve(ItemStack stack,Player player,Direction direction){
        if(getBlockState().getBlock() instanceof RotaryLiftPipeBlock)return false;
        if(hasValve()||!complete()||owner!=null)return false;
        if(getBlockState().getBlock() instanceof LargePipeBlock&&direction.getAxis()!=getBlockState().getValue(LargePipeBlock.AXIS))return false;
        valve=stack.copyWithCount(1);if(level instanceof ServerLevel server)AssemblyItemData.ensurePart(valve,AssemblyPartProfile.Kind.GEARBOX,0,server.random);
        stack.consume(1,player);flow=direction;
        if(getBlockState().getBlock() instanceof IndustrialPipeBlock){
            BlockState state=getBlockState().setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction),true)
                    .setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction.getOpposite()),true);
            level.setBlock(worldPosition,state,3);
        }
        sync();return true;
    }
    public void turn(Player player){
        if(level==null||level.isClientSide||!hasValve())return;
        if(player.isShiftKeyDown()){flow=flow.getOpposite();open=false;}else open=!open;
        level.playSound(null,worldPosition,SoundEvents.LEVER_CLICK,SoundSource.BLOCKS,.7F,open?.7F:.5F);sync();
    }
    public void dropParts(){
        if(level==null||level.isClientSide)return;
        if(!valve.isEmpty()){Block.popResource(level,worldPosition,valve);valve=ItemStack.EMPTY;}
        if(getBlockState().getBlock() instanceof IndustrialPipeBlock){Block.popResource(level,worldPosition,body.isEmpty()?new ItemStack(getBlockState().getBlock().asItem()):body);body=ItemStack.EMPTY;}
    }
    public void dismantle(){
        if(removing||level==null||level.isClientSide)return;
        if(owner!=null){PipeBlockEntity root=controller();if(root!=null&&!root.removing){root.dismantle();level.removeBlock(root.worldPosition,false);}return;}
        removing=true;open=false;
        for(BlockPos p:shellPositions(worldPosition,getBlockState()))if(level.hasChunkAt(p)&&level.getBlockEntity(p) instanceof PipeBlockEntity shell&&worldPosition.equals(shell.owner)){shell.removing=true;level.removeBlock(p,false);}
        dropParts();sections=0;
        for(ItemStack piece:installedSections)Block.popResource(level,worldPosition,piece);
        installedSections.clear();
        // Remaining transported liquid becomes a spill only if a real full bucket can be placed.
        if(!tank.isEmpty())PipeFlow.spill((ServerLevel)level,worldPosition.relative(flow,2),tank,false);
    }
    public static void tick(Level level,BlockPos pos,BlockState state,PipeBlockEntity pipe){
        if(pipe.owner!=null||!pipe.complete()||!WorldFeatureRuntime.enabled(level,WorldFeature.POWER_NETWORKS))return;
        if(level.isClientSide)return;
        if(!(level instanceof ServerLevel server))return;
        if(state.getBlock() instanceof RotaryLiftPipeBlock lift)RotaryLiftPipeFlow.tick(server,pipe,lift);
        if(pipe.open&&pipe.hasValve()&&!(state.getBlock() instanceof RotaryLiftPipeBlock)
                &&Math.floorMod(level.getGameTime()+pos.asLong(),10)==0)PipeFlow.pump(server,pipe);
        pipe.hydraulicPressureBar*=.94F;
        if(pipe.hydraulicPressureBar<.01F)pipe.hydraulicPressureBar=0;
        pipe.recentFlowMbPerTick*=.82F;
        if(pipe.recentFlowMbPerTick<.05F)pipe.recentFlowMbPerTick=0;

        if(!pipe.body.isEmpty()
                &&Math.floorMod(level.getGameTime()+pos.asLong(),200)==0){
            var profile=AssemblyItemData.readPart(pipe.body);
            if(profile!=null){
                var environment=EnvironmentalFields.sample(server,pos);
                float wetness=Math.clamp(
                        Math.max(0F,(environment.humidity()-.55F)*.55F)
                                +(server.isRainingAt(pos.above())?.55F:0F)
                                +(pipe.wet()?.18F:0F),
                        0F,1F);
                if(wetness>.01F)AssemblyItemData.exposeMaterialWet(
                        pipe.body,profile.material(),level.getGameTime(),
                        wetness,NaturalPressure.isOcean(server,pos));
            }
        }
        if(pipe.wet()&&pipe.integrity<.65F&&Math.floorMod(level.getGameTime()+pos.asLong(),20)==0){
            FluidStack liquid=pipe.visualFluid();
            server.sendParticles(PipeFlow.drip(liquid),pos.getX()+.5,pos.getY()+.05,pos.getZ()+.5,1,.25,0,.25,0);
            if(!pipe.tank.isEmpty()){pipe.tank.shrink(Math.min(10,pipe.tank.getAmount()));pipe.sync();}
        }
    }
    public FluidStack stored(){return tank;}
    public FluidStack outletStored(){return outletBuffer;}
    public int outletAmount(){return outletBuffer.getAmount();}
    public int outletRoom(){return Math.max(0,1000-outletBuffer.getAmount());}
    public void receiveOutlet(FluidStack fluid){
        if(fluid.isEmpty())return;
        if(outletBuffer.isEmpty())outletBuffer=fluid.copy();
        else if(FluidStack.isSameFluidSameComponents(outletBuffer,fluid))outletBuffer.grow(fluid.getAmount());
        sync();
    }
    public void usedOutlet(int amount){outletBuffer.shrink(Math.max(0,amount));sync();}
    public void receive(FluidStack fluid){if(tank.isEmpty())tank=fluid.copy();else tank.grow(fluid.getAmount());sync();}
    public void used(int amount){tank.shrink(amount);sync();}
    public void markFlow(FluidStack fluid,Direction direction){
        markFlow(fluid,direction,0.0F);
    }

    public void markFlow(FluidStack fluid,Direction direction,float milliBucketsPerTick){
        int temperature=fluid.getFluid().getFluidType().getTemperature()-273;
        int tolerance=getBlockState().getBlock() instanceof IndustrialPipeBlock pipe?pipe.spec().maxTemperatureC():800;
        if(temperature>tolerance)damage(Math.min(.025F,(temperature-tolerance)*.00001F));

        if(level!=null&&!body.isEmpty()
                &&Math.floorMod(level.getGameTime()+worldPosition.asLong(),20)==0){
            var profile=AssemblyItemData.readPart(body);
            if(profile!=null)AssemblyItemData.observeMaterialTemperature(
                    body,profile.material(),level.getGameTime(),temperature);
        }

        recentFlowMbPerTick=Math.max(
                recentFlowMbPerTick,
                Float.isFinite(milliBucketsPerTick)?Math.max(0,milliBucketsPerTick):0);
        flow=hasValve()?flow:direction;visible=fluid.copyWithAmount(1);wetUntil=level.getGameTime()+30;sync();}

    public FlowState universalFlowState(){
        FluidStack fluid=visualFluid();
        double crossSection=internalCrossSectionM2();
        Vec3 direction=new Vec3(flow.getStepX(),flow.getStepY(),flow.getStepZ());
        double absolutePressure=PressureMath.STANDARD_ATMOSPHERE_KPA+hydraulicPressureKPa();
        double turbulence=Math.clamp(
                (pressureRatingBar()<=.001F?0:hydraulicPressureBar/pressureRatingBar())*.30
                        +(1.0F-integrity)*.45,
                0.0,
                1.0);

        return UniversalFlow.minecraftLiquidConduit(
                FluidMatterResolver.resolve(fluid),
                direction,
                absolutePressure,
                recentFlowMbPerTick,
                crossSection,
                turbulence);
    }

    private double internalCrossSectionM2(){
        BlockState state=getBlockState();
        if(state.getBlock() instanceof IndustrialPipeBlock pipe){
            return Math.max(1.0E-5,pipe.spec().internalCrossSectionM2());
        }
        if(state.getBlock() instanceof LargePipeBlock pipe){
            double radius=Math.max(.20,pipe.radius()-.15);
            return Math.PI*radius*radius;
        }
        return 1.0;
    }

    public void applyHydraulicPressure(float pressureBar){
        applyHydraulicPressureKPa(
                PressureMath.barToKPa(
                        pressureBar
                )
        );
    }

    public void applyHydraulicPressureKPa(double pressureKPa){
        if(owner!=null||!complete())return;

        float actual=(float)PressureMath.kPaToBar(
                Double.isFinite(pressureKPa)?Math.max(0,pressureKPa):0);
        hydraulicPressureBar=Math.max(hydraulicPressureBar,actual);
        peakHydraulicPressureBar=Math.max(peakHydraulicPressureBar,actual);

        float rating=pressureRatingBar();
        float pressureDamage=(float)PressureMath.overloadDamage(
                hydraulicPressureKPa(),
                PressureMath.barToKPa(rating),
                integrity);

        boolean wearTick=
                level==null
                        || Math.floorMod(
                        level.getGameTime()+worldPosition.asLong(),
                        20
                )==0;

        if(pressureDamage>0&&wearTick){
            damage(Math.min(.08F,pressureDamage*4F));
        }

        float loadRatio=rating<=.001F?0:actual/rating;
        if(!body.isEmpty()){
            var profile=AssemblyItemData.readPart(body);
            if(profile!=null)AssemblyItemData.observeMaterialUse(
                    body,profile.material(),level==null?0:level.getGameTime(),
                    Math.min(2.5F,loadRatio),Math.max(0,loadRatio-1F)*.35F,0);
        }

        if(pressureDamage>0&&level instanceof ServerLevel server&&Math.floorMod(server.getGameTime()+worldPosition.asLong(),20)==0){
            server.playSound(null,worldPosition,SoundEvents.IRON_TRAPDOOR_CLOSE,SoundSource.BLOCKS,
                    .18F+Math.min(.32F,pressureDamage*8F),.72F);
            if(wet())server.sendParticles(ParticleTypes.SPLASH,
                    worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,
                    2,.18,.18,.18,.02);
        }

        setChanged();
    }

    private float pressureRatingBar(){
        BlockState state=getBlockState();
        if(state.getBlock() instanceof IndustrialPipeBlock pipe)return pipe.spec().maxPressureBar();
        if(state.getBlock() instanceof LargePipeBlock duct)return duct.colossal()?14F:8F;
        return 4F;
    }
    public void sync(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){
        CompoundTag tag=new CompoundTag();if(owner!=null)tag.putLong("Owner",owner.asLong());tag.putInt("Sections",sections);
        if(!valve.isEmpty())tag.put("Valve",new ItemStack(valve.getItem()).save(registries));
        tag.putInt("Flow",flow.ordinal());tag.putBoolean("Open",open);
        FluidStack liquid=visualFluid();if(!liquid.isEmpty())tag.put("Visible",liquid.copyWithAmount(1).save(registries));
        tag.putLong("WetUntil",wet()?Math.max(wetUntil,(level==null?0:level.getGameTime())+30):0);tag.putFloat("Integrity",integrity);tag.putFloat("HydraulicPressure",hydraulicPressureBar);
        tag.putFloat("RotaryLiftRpm",rotaryLiftRpm);tag.putFloat("RotaryLiftAngle",rotaryLiftAngle);
        tag.putFloat("RotaryLiftLoad",rotaryLiftLoad);tag.putBoolean("RotaryLiftTorqueStarved",rotaryLiftTorqueStarved);
        // Disk owns all real stacks and fluid volume. Rendering needs only the visible state.
        return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);if(!body.isEmpty())tag.put("Body",body.save(registries));ListTag parts=new ListTag();for(ItemStack stack:installedSections)parts.add(stack.save(registries));tag.put("InstalledSections",parts);if(owner!=null)tag.putLong("Owner",owner.asLong());tag.putInt("Sections",sections);
        if(!valve.isEmpty())tag.put("Valve",valve.save(registries));tag.putInt("Flow",flow.ordinal());tag.putBoolean("Open",open);
        if(!tank.isEmpty())tag.put("Fluid",tank.save(registries));if(!outletBuffer.isEmpty())tag.put("OutletFluid",outletBuffer.save(registries));if(!visible.isEmpty())tag.put("Visible",visible.save(registries));
        tag.putLong("WetUntil",wetUntil);tag.putFloat("Integrity",integrity);tag.putFloat("HydraulicPressure",hydraulicPressureBar);tag.putFloat("PeakHydraulicPressure",peakHydraulicPressureBar);
        tag.putFloat("RotaryLiftRpm",rotaryLiftRpm);tag.putFloat("RotaryLiftAngle",rotaryLiftAngle);
        tag.putFloat("RotaryLiftLoad",rotaryLiftLoad);tag.putBoolean("RotaryLiftTorqueStarved",rotaryLiftTorqueStarved);
        tag.putInt("OutletCursor",outletCursor);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);owner=tag.contains("Owner")?BlockPos.of(tag.getLong("Owner")):null;
        sections=Math.max(0,Math.min(getBlockState().getBlock() instanceof LargePipeBlock b?b.required():0,tag.getInt("Sections")));
        body=ItemStack.parseOptional(registries,tag.getCompound("Body"));
        installedSections.clear();
        ListTag parts=tag.getList("InstalledSections",Tag.TAG_COMPOUND);
        for(int i=0;i<Math.min(sections,parts.size());i++){ItemStack stack=ItemStack.parseOptional(registries,parts.getCompound(i));if(!stack.isEmpty())installedSections.add(stack.copyWithCount(1));}
        if(level!=null&&!level.isClientSide&&getBlockState().getBlock() instanceof LargePipeBlock){
            // Compatibility with the first staged-duct revision: its count represented paid plain sections.
            while(installedSections.size()<sections)installedSections.add(section(new ItemStack(getBlockState().getBlock().asItem())));
        }
        valve=ItemStack.parseOptional(registries,tag.getCompound("Valve"));flow=Direction.values()[Math.floorMod(tag.getInt("Flow"),6)];open=tag.getBoolean("Open");
        tank=FluidStack.parseOptional(registries,tag.getCompound("Fluid"));if(tank.getAmount()>capacity())tank.setAmount(capacity());
        outletBuffer=FluidStack.parseOptional(registries,tag.getCompound("OutletFluid"));if(outletBuffer.getAmount()>1000)outletBuffer.setAmount(1000);
        visible=FluidStack.parseOptional(registries,tag.getCompound("Visible"));wetUntil=tag.getLong("WetUntil");float value=tag.getFloat("Integrity");integrity=Float.isFinite(value)?Math.clamp(value,0,1):0;
        float pressure=tag.getFloat("HydraulicPressure");hydraulicPressureBar=Float.isFinite(pressure)?Math.max(0,pressure):0;
        float peakPressure=tag.getFloat("PeakHydraulicPressure");peakHydraulicPressureBar=Float.isFinite(peakPressure)?Math.max(hydraulicPressureBar,peakPressure):hydraulicPressureBar;
        rotaryLiftRpm=finite(tag.getFloat("RotaryLiftRpm"),-180F,180F);
        rotaryLiftAngle=finite(tag.getFloat("RotaryLiftAngle"),-360F,360F);
        rotaryLiftLoad=finite(tag.getFloat("RotaryLiftLoad"),0F,4F);
        rotaryLiftTorqueStarved=tag.getBoolean("RotaryLiftTorqueStarved");
        outletCursor=tag.getInt("OutletCursor");
    }
    public int nextOutlet(int count){int chosen=Math.floorMod(outletCursor,count);outletCursor=chosen+1;return chosen;}
    @Override public BlockPos structuralPosition(){return worldPosition;}
    @Override public float structuralIntegrity(){return integrity;}
    @Override public void receiveWorldForce(WorldForce force,float magnitude){damage(Math.max(0,magnitude)*.001F);}
    @Override public void receiveStructuralDamage(StructuralDamage damage){damage(Math.max(0,damage.amount())*.02F);}
    private void damage(float amount){
        integrity=Math.max(0,integrity-amount);
        var main=AssemblyItemData.readPart(body);if(main!=null){main.applyWear(amount);AssemblyItemData.writePart(body,main);}
        for(ItemStack piece:installedSections){var profile=AssemblyItemData.readPart(piece);if(profile!=null){profile.applyWear(amount);AssemblyItemData.writePart(piece,profile);}}
        sync();
    }
    private static float finite(float value,float min,float max){
        return Float.isFinite(value)?Math.clamp(value,min,max):0;
    }
}
