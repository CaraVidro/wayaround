package net.caravidro.wayaround.industrial.crushing;

import java.util.*;
import net.caravidro.wayaround.industrial.assembly.*;
import net.caravidro.wayaround.industrial.mechanical.*;
import net.caravidro.wayaround.worldconfig.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

public final class CrusherBlockEntity extends BlockEntity implements AssemblyMachine {
    private static final int MAX_STACKS = 16, MAX_OUTPUT = 512;
    private final MachineParts parts;
    private final List<ItemStack> input = new ArrayList<>(), output = new ArrayList<>();
    private float rpm, angle, progress, heat, vibration, load;
    private boolean stalled, supported;
    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(CrusherContent.ENTITY.get(),pos,state);
        parts = new MachineParts(size().id());
    }
    public CrusherSize size() { return ((CrusherBlock)getBlockState().getBlock()).size(); }
    public MachineParts parts() { return parts; }
    public float rpm() { return rpm; }
    public float angle() { return angle; }
    public float vibration() { return vibration; }
    public boolean working() { return !input.isEmpty() && Math.abs(rpm) > 7 && !stalled; }
    public int inputCapacity() { return size().inputCapacity(parts.feedMultiplier()); }
    public int inputCount() { return input.stream().mapToInt(ItemStack::getCount).sum(); }
    public int outputCount() { return output.stream().mapToInt(ItemStack::getCount).sum(); }
    public static void serverTick(Level level, BlockPos pos, BlockState state, CrusherBlockEntity crusher) { crusher.tick((ServerLevel)level); }
    private void tick(ServerLevel level) {
        if (!WorldFeatureRuntime.enabled(level,WorldFeature.INDUSTRIAL_MACHINES)
                || !WorldFeatureRuntime.enabled(level,WorldFeature.POWER_NETWORKS)
                || !WorldFeatureRuntime.enabled(level,WorldFeature.ASSEMBLY)) { if (rpm != 0) { rpm = 0; load = 0; sync(); } return; }
        float oldRpm = rpm; boolean oldSupported = supported, oldStalled = stalled;
        supported = supportsValid();
        if (level.getGameTime() % 5 == 0 && parts.complete() && supported) {
            // Only actual dropped items in the intake are accepted; no remote mining.
            for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class,
                    new AABB(worldPosition).inflate(.02,.0,.02).move(0,.5,0))) {
                int admitted = offer(entity.getItem());
                if (admitted > 0) {
                    ItemStack left = entity.getItem().copy(); left.shrink(admitted);
                    if (left.isEmpty()) entity.discard(); else entity.setItem(left);
                }
            }
        }
        CrushingRecipe recipe = input.isEmpty() ? null : CrushingRecipe.find(input.get(0));
        float strength = parts.condition(MachinePartSpec.Role.TOOL);
        float requiredTorque = recipe == null ? 0 : size().power * recipe.hardness() * .35F;
        float target = 0, granted = 0;
        float request = size().power * parts.driveCost() * (recipe == null ? .15F : 1);
        IRotationalPower source = bestSource();
        stalled = !parts.complete() || !supported || strength < .12F
            || parts.condition(MachinePartSpec.Role.DRIVE) < .12F || parts.condition(MachinePartSpec.Role.BEARING) < .12F
            || heat > 1 || (recipe != null && recipe.hardness() > size().hardness * strength * 2.6F);
        if (source != null && source.active() && parts.complete() && supported) {
            granted = source.consumePower(request);
            load = granted / Math.max(.01F,size().power);
            if (recipe != null && (Math.abs(source.torque()) < requiredTorque || granted > supportCapacity())) stalled = true;
            if (!stalled) target = source.rpm() * Mth.clamp(granted / request,0,1);
        } else load = 0;
        rpm += (target-rpm) * (parts.has(MachinePartSpec.Role.DRIVE) && parts.spec(MachinePartSpec.Role.DRIVE).heavy() ? .06F : .18F);
        if (Math.abs(rpm)<.01F) rpm=0;
        angle = (angle + rpm * .3F) % 360;
        vibration = recipe == null ? 0 : Mth.clamp(load * (1-parts.condition(MachinePartSpec.Role.BEARING))
            + Math.abs(rpm)/100 * (1-parts.condition(MachinePartSpec.Role.DRIVE)),0,1);
        heat = Math.max(0,heat-.0015F) + (recipe != null && granted > 0 ? .00035F * load * parts.driveCost() : 0);
        if (working() && recipe != null && granted > .01F) {
            int batch = size().boundedBatch(input.get(0).getCount(), MAX_OUTPUT-outputCount(), recipe.count(), parts.feedMultiplier());
            if (canOutput(recipe,batch)) {
                progress += Math.min(1.6F,Math.abs(rpm)/30) * parts.speed() / (size().cycleTicks * recipe.hardness());
                if (progress >= 1) {
                    progress -= 1;
                    appendOutput(new ItemStack(recipe.output(),recipe.count()*batch));
                    input.get(0).shrink(batch); if(input.get(0).isEmpty()) input.remove(0);
                    parts.wear(.0007F * batch * recipe.hardness(),Math.max(1,load));
                    level.playSound(null,worldPosition,SoundEvents.STONE_BREAK,SoundSource.BLOCKS,.4F,.65F);
                    sync();
                }
                if (level.getGameTime()%10==0) level.sendParticles(ParticleTypes.POOF,
                    worldPosition.getX()+.5,worldPosition.getY()+.9,worldPosition.getZ()+.5,2,.15,.05,.15,.015);
            }
        } else if (recipe == null) progress=0;
        // Frame and bearing stress still exist when a powered tool stalls.
        if (stalled && granted > 0) parts.wear(.00003F,load);
        if (level.getGameTime()%5==0 && (Math.abs(oldRpm-rpm)>.02F || oldSupported!=supported || oldStalled!=stalled)) sync(); else setChanged();
    }
    private IRotationalPower bestSource() {
        IRotationalPower best=null; float score=-1;
        Direction back = getBlockState().getValue(CrusherBlock.FACING).getOpposite();
        for(Direction direction:new Direction[]{back, back.getOpposite()}) { var candidate=MechanicalTransmission.findSource(level,worldPosition,direction);
            float next=MechanicalTransmission.sourceScore(candidate); if(next>score){score=next;best=candidate;} }
        return best;
    }
    public List<BlockPos> supportPositions() {
        Direction right=getBlockState().getValue(CrusherBlock.FACING).getClockWise();
        if(size()==CrusherSize.SMALL)return List.of();
        if(size()==CrusherSize.MEDIUM)return List.of(worldPosition.relative(right),worldPosition.relative(right.getOpposite()));
        Direction front=getBlockState().getValue(CrusherBlock.FACING);
        return List.of(worldPosition.relative(right).relative(front),worldPosition.relative(right).relative(front.getOpposite()),
            worldPosition.relative(right.getOpposite()).relative(front),worldPosition.relative(right.getOpposite()).relative(front.getOpposite()));
    }
    private float supportCapacity() {
        if (size()==CrusherSize.SMALL) return 6;
        float capacity=0;
        for (BlockPos pos:supportPositions()) if (level.getBlockState(pos).getBlock() instanceof MachineSupportBlock support)
            capacity += support.reinforced()?14:5;
        return capacity;
    }
    public boolean supportsValid() {
        if(level==null || !level.getBlockState(worldPosition.below()).isFaceSturdy(level,worldPosition.below(),Direction.UP))return false;
        for(BlockPos pos:supportPositions()) {
            if(!level.hasChunkAt(pos)||!level.hasChunkAt(pos.below()))return false;
            if(!(level.getBlockState(pos).getBlock() instanceof MachineSupportBlock support))return false;
            if(size()==CrusherSize.LARGE && !support.reinforced())return false;
            if(!level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP))return false;
        } return true;
    }
    public int offer(ItemStack offered) {
        if(level==null||level.isClientSide||!parts.complete()||!supportsValid()
            || !WorldFeatureRuntime.enabled(level, WorldFeature.ASSEMBLY)
            || !WorldFeatureRuntime.enabled(level, WorldFeature.INDUSTRIAL_MACHINES) || CrushingRecipe.find(offered)==null)return 0;
        int accepted=Math.min(offered.getCount(),inputCapacity()-inputCount());
        if(accepted<=0)return 0;
        for(ItemStack stored:input) if(ItemStack.isSameItemSameComponents(stored,offered)) {
            int add=Math.min(accepted,stored.getMaxStackSize()-stored.getCount());
            if(add>0){stored.grow(add);sync();return add;}
        }
        if(input.size()>=MAX_STACKS)return 0;
        accepted=Math.min(accepted,offered.getMaxStackSize());
        input.add(offered.copyWithCount(accepted));sync();return accepted;
    }
    /** Future connected-block transport calls this only after physically delivering a payload.
     * Atomic: rejection leaves caller stacks and machine inventory untouched. Never reads/removes world blocks. */
    public boolean offerConnectedBatch(List<ItemStack> delivered) {
        if(size()!=CrusherSize.LARGE||level==null||level.isClientSide||!parts.complete()||!supportsValid()
                || delivered.isEmpty()||delivered.size()>MAX_STACKS)return false;
        int count=0;
        Set<ItemStack> unique = Collections.newSetFromMap(new IdentityHashMap<>());
        for(ItemStack stack:delivered){if(stack == null || !unique.add(stack) || stack.isEmpty()||stack.getCount()>stack.getMaxStackSize()||CrushingRecipe.find(stack)==null)return false;count+=stack.getCount();}
        if(count+inputCount()>inputCapacity()||input.size()+delivered.size()>MAX_STACKS)return false;
        for(ItemStack stack:delivered)input.add(stack.copy());
        for(ItemStack stack:delivered)stack.setCount(0);
        sync();return true;
    }
    private boolean canOutput(CrushingRecipe recipe,int batch) {
        if (batch <= 0) return false;
        int wanted=recipe.count()*batch;
        if(wanted+outputCount()>MAX_OUTPUT)return false;
        int free=(MAX_STACKS-output.size())*recipe.output().getDefaultMaxStackSize();
        for(ItemStack stack:output)if(stack.is(recipe.output()))free+=stack.getMaxStackSize()-stack.getCount();
        return free>=wanted;
    }
    private void appendOutput(ItemStack produced) {
        for(ItemStack stack:output)if(ItemStack.isSameItemSameComponents(stack,produced)){
            int add=Math.min(produced.getCount(),stack.getMaxStackSize()-stack.getCount());stack.grow(add);produced.shrink(add);
        }
        while(!produced.isEmpty()){int count=Math.min(produced.getCount(),produced.getMaxStackSize());output.add(produced.copyWithCount(count));produced.shrink(count);}
    }
    public void install(Player player,ItemStack stack) {
        if (level == null || level.isClientSide || !WorldFeatureRuntime.enabled(level, WorldFeature.ASSEMBLY)) return;
        if(Math.abs(rpm)>.5F||!parts.install(player,stack)){describe(player);return;} sync();
    }
    public void removePart(Player player,BlockHitResult hit) {
        if (level == null || level.isClientSide) return;
        if(Math.abs(rpm)>.5F){player.displayClientMessage(Component.translatable("message.wayaround.machine.stop_first"),true);return;}
        double y=hit.getLocation().y-worldPosition.getY();
        var role=y>.78?MachinePartSpec.Role.FEED:y>.45?MachinePartSpec.Role.TOOL:y>.22?MachinePartSpec.Role.DRIVE:MachinePartSpec.Role.BEARING;
        ItemStack removed=parts.remove(role); if(removed.isEmpty())removed=parts.removeLast();
        if(!removed.isEmpty()){if(!player.getInventory().add(removed))player.drop(removed,false);sync();}
    }
    public boolean collectOutput(Player player) {
        if(output.isEmpty())return false;
        for(ItemStack stack:output)if(!player.getInventory().add(stack))player.drop(stack,false);
        output.clear();sync();return true;
    }
    public void describe(Player player) {
        player.displayClientMessage(Component.translatable("message.wayaround.crusher.status",parts.nodes(true).size(),4,
            inputCount(),inputCapacity(),Math.round(rpm),supported?Component.translatable("message.wayaround.crusher.supported"):
            Component.translatable("message.wayaround.crusher.support_missing"),stalled?Component.translatable("message.wayaround.crusher.stalled"):
            Component.translatable("message.wayaround.crusher.ready")),true);
    }
    public void dropContents() {
        parts.drop(level,worldPosition);
        for(ItemStack stack:input)Block.popResource(level,worldPosition,stack);
        for(ItemStack stack:output)Block.popResource(level,worldPosition,stack);
        input.clear();output.clear();
    }
    private void sync(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);parts.save(tag,registries);
        ListTag in=new ListTag(),out=new ListTag();for(ItemStack stack:input)in.add(stack.save(registries));for(ItemStack stack:output)out.add(stack.save(registries));
        tag.put("Input",in);tag.put("Output",out);tag.putFloat("Rpm",rpm);tag.putFloat("Angle",angle);tag.putFloat("Progress",progress);
        tag.putFloat("Heat",heat);tag.putFloat("Vibration",vibration);tag.putFloat("Load",load);tag.putBoolean("Stalled",stalled);tag.putBoolean("Supported",supported);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);parts.load(tag,registries);input.clear();output.clear();
        loadInventory(tag.getList("Input",Tag.TAG_COMPOUND),registries,input,size().capacity*2);
        loadInventory(tag.getList("Output",Tag.TAG_COMPOUND),registries,output,MAX_OUTPUT);
        rpm=finite(tag.getFloat("Rpm"),-200,200);angle=finite(tag.getFloat("Angle"),-360,360);progress=finite(tag.getFloat("Progress"),0,1);
        heat=finite(tag.getFloat("Heat"),0,2);vibration=finite(tag.getFloat("Vibration"),0,1);load=finite(tag.getFloat("Load"),0,10);
        stalled=tag.getBoolean("Stalled");supported=tag.getBoolean("Supported");
    }
    private void loadInventory(ListTag tags,HolderLookup.Provider registries,List<ItemStack> target,int budget){
        for(int i=0;i<Math.min(MAX_STACKS,tags.size());i++){
            ItemStack stack=ItemStack.parseOptional(registries,tags.getCompound(i));
            if(stack.isEmpty())continue;int count=Math.min(budget,Math.min(stack.getCount(),stack.getMaxStackSize()));
            if(count>0){target.add(stack.copyWithCount(count));budget-=count;}
        }
    }
    private static float finite(float value,float min,float max){return Float.isFinite(value)?Mth.clamp(value,min,max):0;}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){var tag=new CompoundTag();saveAdditional(tag,registries);return tag;}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public ResourceLocation assemblyType(){return ResourceLocation.fromNamespaceAndPath("wayaround",size().id()+"_crusher");}
    @Override public BlockPos assemblyAnchor(){return worldPosition;}
    @Override public Collection<AssemblyPartNode> assemblyParts(){
        var nodes=new ArrayList<>(parts.nodes(supportsValid()));
        nodes.add(LegacyMachineAssembly.part("frame","chassis",AssemblyPartProfile.Kind.FRAME,AssemblyPartProfile.Material.IRON,
            BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()),0,supportsValid(),1));
        for(BlockPos pos:supportPositions())if(level!=null&&level.hasChunkAt(pos)&&level.getBlockState(pos).getBlock() instanceof MachineSupportBlock support)
            nodes.add(LegacyMachineAssembly.part("support_"+pos.asLong(),"support",AssemblyPartProfile.Kind.FRAME,
                support.reinforced()?AssemblyPartProfile.Material.IRON:AssemblyPartProfile.Material.WOOD,
                BuiltInRegistries.BLOCK.getKey(support),0,level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP),1));
        return nodes;
    }
    @Override public Collection<AssemblyConnection> assemblyConnections(){
        var connections=new ArrayList<>(parts.connections());
        for(var node:assemblyParts())if(node.id().startsWith("support_"))connections.add(new AssemblyConnection("frame",node.id(),AssemblyConnection.Type.SUPPORT,.9F,0));
        return connections;
    }
    @Override public float currentAssemblyLoad(){return load;}
    @Override public void applyAssemblyWear(float fraction){parts.wear(fraction,Math.max(1,load));sync();}
}
