package net.caravidro.wayaround.industrial.pipework;

import java.util.*;
import net.caravidro.wayaround.industrial.assembly.*;
import net.caravidro.wayaround.interaction.*;
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
    private ItemStack valve=ItemStack.EMPTY;
    private Direction flow=Direction.NORTH;
    private boolean open,removing;
    private FluidStack tank=FluidStack.EMPTY;
    private FluidStack visible=FluidStack.EMPTY;
    private long wetUntil;
    private float integrity=1;
    private int outletCursor;
    public PipeBlockEntity(BlockPos pos,BlockState state){super(PipeworkContent.PIPE_ENTITY.get(),pos,state);}
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
            if(!existing.canBeReplaced()||!existing.getFluidState().isEmpty())return false;
        }return true;
    }
    public void firstSection(){sections=1;flow=Direction.get(Direction.AxisDirection.POSITIVE,getBlockState().getValue(LargePipeBlock.AXIS));buildShell();sync();}
    public boolean assemble(ItemStack stack,Player player){
        if(!(getBlockState().getBlock() instanceof LargePipeBlock b)||sections>=b.required()||!stack.is(b.asItem())||!roomFor(level,worldPosition,getBlockState()))return false;
        sections++;stack.consume(1,player);buildShell();
        level.playSound(null,worldPosition,SoundEvents.ANVIL_PLACE,SoundSource.BLOCKS,.35F,.8F+sections/(float)b.required()*.4F);sync();return true;
    }
    private void buildShell(){
        if(level==null||level.isClientSide||!(getBlockState().getBlock() instanceof LargePipeBlock b))return;
        List<BlockPos> positions=shellPositions(worldPosition,getBlockState());int count=(positions.size()*sections+b.required()-1)/b.required();
        for(int i=0;i<count;i++){BlockPos p=positions.get(i);
            if(level.getBlockEntity(p) instanceof PipeBlockEntity existing&&worldPosition.equals(existing.owner))continue;
            if(!level.getBlockState(p).canBeReplaced())continue;
            level.setBlock(p,getBlockState().setValue(LargePipeBlock.SHELL,true),3);
            if(level.getBlockEntity(p) instanceof PipeBlockEntity shell){shell.owner=worldPosition;shell.sync();}
        }
    }
    public boolean installValve(ItemStack stack,Player player,Direction direction){
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
    public void dropParts(){if(level!=null&&!level.isClientSide&&!valve.isEmpty()){Block.popResource(level,worldPosition,valve);valve=ItemStack.EMPTY;}}
    public void dismantle(){
        if(removing||level==null||level.isClientSide)return;
        if(owner!=null){PipeBlockEntity root=controller();if(root!=null&&!root.removing){root.dismantle();level.removeBlock(root.worldPosition,false);}return;}
        removing=true;open=false;
        for(BlockPos p:shellPositions(worldPosition,getBlockState()))if(level.hasChunkAt(p)&&level.getBlockEntity(p) instanceof PipeBlockEntity shell&&worldPosition.equals(shell.owner)){shell.removing=true;level.removeBlock(p,false);}
        dropParts();int left=sections;sections=0;
        while(left>0){int n=Math.min(64,left);Block.popResource(level,worldPosition,new ItemStack(getBlockState().getBlock().asItem(),n));left-=n;}
        // Remaining transported liquid becomes a spill only if a real full bucket can be placed.
        if(!tank.isEmpty())PipeFlow.spill((ServerLevel)level,worldPosition.relative(flow,2),tank,false);
    }
    public static void tick(Level level,BlockPos pos,BlockState state,PipeBlockEntity pipe){
        if(pipe.owner!=null||!pipe.complete()||!WorldFeatureRuntime.enabled(level,WorldFeature.POWER_NETWORKS))return;
        if(level.isClientSide)return;
        if(!(level instanceof ServerLevel server))return;
        if(pipe.open&&pipe.hasValve()&&Math.floorMod(level.getGameTime()+pos.asLong(),10)==0)PipeFlow.pump(server,pipe);
        if(pipe.wet()&&pipe.integrity<.65F&&Math.floorMod(level.getGameTime()+pos.asLong(),20)==0){
            FluidStack liquid=pipe.visualFluid();
            server.sendParticles(PipeFlow.drip(liquid),pos.getX()+.5,pos.getY()+.05,pos.getZ()+.5,1,.25,0,.25,0);
            if(!pipe.tank.isEmpty()){pipe.tank.shrink(Math.min(10,pipe.tank.getAmount()));pipe.sync();}
        }
    }
    public FluidStack stored(){return tank;}
    public void receive(FluidStack fluid){if(tank.isEmpty())tank=fluid.copy();else tank.grow(fluid.getAmount());sync();}
    public void used(int amount){tank.shrink(amount);sync();}
    public void markFlow(FluidStack fluid,Direction direction){flow=hasValve()?flow:direction;visible=fluid.copyWithAmount(1);wetUntil=level.getGameTime()+30;sync();}
    public void sync(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);if(owner!=null)tag.putLong("Owner",owner.asLong());tag.putInt("Sections",sections);
        if(!valve.isEmpty())tag.put("Valve",valve.save(registries));tag.putInt("Flow",flow.ordinal());tag.putBoolean("Open",open);
        if(!tank.isEmpty())tag.put("Fluid",tank.save(registries));if(!visible.isEmpty())tag.put("Visible",visible.save(registries));
        tag.putLong("WetUntil",wetUntil);tag.putFloat("Integrity",integrity);tag.putInt("OutletCursor",outletCursor);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);owner=tag.contains("Owner")?BlockPos.of(tag.getLong("Owner")):null;
        sections=Math.max(0,Math.min(getBlockState().getBlock() instanceof LargePipeBlock b?b.required():0,tag.getInt("Sections")));
        valve=ItemStack.parseOptional(registries,tag.getCompound("Valve"));flow=Direction.values()[Math.floorMod(tag.getInt("Flow"),6)];open=tag.getBoolean("Open");
        tank=FluidStack.parseOptional(registries,tag.getCompound("Fluid"));if(tank.getAmount()>capacity())tank.setAmount(capacity());
        visible=FluidStack.parseOptional(registries,tag.getCompound("Visible"));wetUntil=tag.getLong("WetUntil");float value=tag.getFloat("Integrity");integrity=Float.isFinite(value)?Math.clamp(value,0,1):0;
        outletCursor=tag.getInt("OutletCursor");
    }
    public int nextOutlet(int count){int chosen=Math.floorMod(outletCursor,count);outletCursor=chosen+1;return chosen;}
    @Override public BlockPos structuralPosition(){return worldPosition;}
    @Override public float structuralIntegrity(){return integrity;}
    @Override public void receiveWorldForce(WorldForce force,float magnitude){damage(Math.max(0,magnitude)*.001F);}
    @Override public void receiveStructuralDamage(StructuralDamage damage){damage(Math.max(0,damage.amount())*.02F);}
    private void damage(float amount){integrity=Math.max(0,integrity-amount);sync();}
}
