package net.caravidro.wayaround.war.outpost;

import java.util.UUID;
import net.caravidro.wayaround.war.*;
import net.caravidro.wayaround.worldconfig.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** One device BE. Contact sensors sleep until touched; alarm scans have a shared ceiling. */
public final class FieldDeviceBlockEntity extends BlockEntity {
    public static final String SEAT_TAG="wayaround_outpost_seat";
    private UUID owner,victim,seatId;
    private Vec3 trigger=Vec3.ZERO;
    private int slot=-1,ammo,heat;
    private float aimYaw,aimPitch;
    private ItemStack held=ItemStack.EMPTY,offhand=ItemStack.EMPTY;
    private long readyAt,fireAt,alarmUntil;
    private boolean detonating;
    private BlockState cover;
    public FieldDeviceBlockEntity(BlockPos p,BlockState s){super(OutpostContent.DEVICE.get(),p,s);}
    public FieldDeviceBlock.Kind kind(){return ((FieldDeviceBlock)getBlockState().getBlock()).kind;}
    public boolean enabled(){return level!=null&&WorldFeatureRuntime.enabled(level,WorldFeature.WAR_WITHOUT_REASON);}
    public float aimYaw(){return aimYaw;}public float aimPitch(){return aimPitch;}
    public int ammo(){return ammo;}public int heat(){return heat;}public UUID owner(){return owner;}public UUID victim(){return victim;}public BlockState cover(){return cover;}
    public void placed(LivingEntity e){owner=e==null?null:e.getUUID();readyAt=level.getGameTime()+30;setChanged();}
    private void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    private void active(boolean a){if(getBlockState().getValue(FieldDeviceBlock.ACTIVE)!=a)level.setBlock(worldPosition,getBlockState().setValue(FieldDeviceBlock.ACTIVE,a),3);}
    public boolean camouflage(){if(cover!=null||level==null)return false;var below=level.getBlockState(worldPosition.below());cover=below.isAir()||below.getRenderShape()!=RenderShape.MODEL?Blocks.GRASS_BLOCK.defaultBlockState():below;changed();return true;}
    public void touch(Entity e){
        if(!enabled()||level.isClientSide||level.getGameTime()<readyAt||!(e instanceof LivingEntity living)||!living.isAlive()||e.isSpectator()||net.caravidro.wayaround.observation.EntitySpectate.ghost(e))return;
        switch(kind()){
            case CONTACT->explode(3);
            case STILL->{if(victim==null){victim=e.getUUID();trigger=e.position();held=living.getMainHandItem().copy();offhand=living.getOffhandItem().copy();slot=e instanceof Player p?p.getInventory().selected:-1;active(true);sound(SoundEvents.LEVER_CLICK,.7F,.5F);if(e instanceof Player p)p.displayClientMessage(Component.translatable("message.wayaround.mine.still"),true);changed();}}
            case SPIKES->{if(e.tickCount%10==0){living.hurt(level.damageSources().cactus(),3);e.setDeltaMovement(e.getDeltaMovement().multiply(.45,1,.45));}}
            default->{}
        }
    }
    public static boolean disturbed(LivingEntity e,Vec3 at,int selected,ItemStack main,ItemStack off){return e.position().distanceToSqr(at)>.0016||(e instanceof Player p&&p.getInventory().selected!=selected)||!ItemStack.isSameItemSameComponents(e.getMainHandItem(),main)||!ItemStack.isSameItemSameComponents(e.getOffhandItem(),off);}
    public void explode(float power){if(detonating||!(level instanceof ServerLevel s)||!enabled())return;detonating=true;victim=null;cover=null;var cause=owner==null?null:s.getEntity(owner);level.removeBlock(worldPosition,false);level.explode(cause,worldPosition.getX()+.5,worldPosition.getY()+.15,worldPosition.getZ()+.5,power,Level.ExplosionInteraction.TNT);}
    private void sound(net.minecraft.sounds.SoundEvent sound,float volume,float pitch){level.playSound(null,worldPosition,sound,SoundSource.BLOCKS,volume,pitch);}
    public void interact(Player p,InteractionHand hand){
        if(!enabled())return;var stack=p.getItemInHand(hand);
        if(stack.is(Items.SHEARS)&&cover!=null){cover=null;give(p,new ItemStack(OutpostContent.CAMO.get()));changed();sound(SoundEvents.SHEEP_SHEAR,.7F,1);return;}
        if(kind()==FieldDeviceBlock.Kind.CHARGE&&stack.is(OutpostContent.CONTROLLER.get())){if(owner!=null&&!owner.equals(p.getUUID()))return;owner=p.getUUID();RemoteControllerItem.bindCharge(stack,worldPosition,level);p.displayClientMessage(Component.translatable("message.wayaround.remote.charge"),true);setChanged();return;}
        if(kind()!=FieldDeviceBlock.Kind.GUN)return;
        if(stack.is(OutpostContent.GUN_BARREL.get())&&!getBlockState().getValue(FieldDeviceBlock.BARREL)){level.setBlock(worldPosition,getBlockState().setValue(FieldDeviceBlock.BARREL,true),3);if(!p.getAbilities().instabuild)stack.shrink(1);sound(SoundEvents.IRON_TRAPDOOR_CLOSE,.7F,1);return;}
        if(stack.is(Items.IRON_NUGGET)){int add=Math.min(256-ammo,stack.getCount());if(add>0){ammo+=add;if(!p.getAbilities().instabuild)stack.shrink(add);changed();}return;}
        if(p.isShiftKeyDown()&&stack.isEmpty()&&seat()==null){if(getBlockState().getValue(FieldDeviceBlock.BARREL)){level.setBlock(worldPosition,getBlockState().setValue(FieldDeviceBlock.BARREL,false),3);give(p,new ItemStack(OutpostContent.GUN_BARREL.get()));}while(ammo>0){int n=Math.min(64,ammo);give(p,new ItemStack(Items.IRON_NUGGET,n));ammo-=n;}changed();return;}
        if(!getBlockState().getValue(FieldDeviceBlock.BARREL)){p.displayClientMessage(Component.translatable("message.wayaround.outpost.barrel"),true);return;}
        if(p instanceof ServerPlayer sp)mount(sp);
    }
    private static void give(Player p,ItemStack s){if(!p.addItem(s))p.drop(s,false);}
    private ArmorStand seat(){return level instanceof ServerLevel s&&seatId!=null&&s.getEntity(seatId) instanceof ArmorStand a?a:null;}
    public boolean operating(ServerPlayer p){return p.getVehicle() instanceof ArmorStand a&&a.getUUID().equals(seatId)&&a.getTags().contains(SEAT_TAG);}
    private void mount(ServerPlayer p){
        if(p.isPassenger()||p.distanceToSqr(Vec3.atCenterOf(worldPosition))>16)return;var current=seat();if(current!=null&&!current.getPassengers().isEmpty())return;
        Vec3 back=Vec3.atLowerCornerOf(getBlockState().getValue(FieldDeviceBlock.FACING).getNormal()).scale(-.9);Vec3 at=Vec3.atCenterOf(worldPosition).add(back).add(0,-1.35,0);
        if(!level.hasChunkAt(BlockPos.containing(at))||!level.noCollision(p,p.getBoundingBox().move(at.add(0,1.5,0).subtract(p.position()))))return;
        if(current==null){current=new ArmorStand(level,at.x,at.y,at.z);current.setInvisible(true);current.setNoGravity(true);current.setInvulnerable(true);current.setSilent(true);current.addTag(SEAT_TAG);current.getPersistentData().putLong("OutpostAnchor",worldPosition.asLong());level.addFreshEntity(current);seatId=current.getUUID();setChanged();}
        if(!p.startRiding(current,true))return;aimYaw=getBlockState().getValue(FieldDeviceBlock.FACING).toYRot();aimPitch=0;p.setYRot(aimYaw);p.setXRot(0);net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p,new OutpostViewPayload(-2,worldPosition.asLong()));p.displayClientMessage(Component.translatable("message.wayaround.outpost.mounted"),true);
    }
    public boolean fire(ServerPlayer p){
        if(!enabled()||!operating(p)||!getBlockState().getValue(FieldDeviceBlock.BARREL)||ammo<=0||heat>=80||level.getGameTime()<fireAt)return false;
        var s=(ServerLevel)level;if(!OutpostBudget.projectile(s,worldPosition))return false;
        float base=getBlockState().getValue(FieldDeviceBlock.FACING).toYRot();float yaw=base+Mth.clamp(Mth.wrapDegrees(p.getYRot()-base),-65,65),pitch=Mth.clamp(p.getXRot(),-35,35);
        Vec3 aim=Vec3.directionFromRotation(pitch,yaw).add(s.random.nextGaussian()*.012,s.random.nextGaussian()*.012,s.random.nextGaussian()*.012).normalize();Vec3 muzzle=Vec3.atCenterOf(worldPosition).add(0,.4,0).add(aim.scale(.95));
        var round=new WarProjectileEntity(WarContent.WAR_PROJECTILE.get(),s);round.setPos(muzzle);round.configure(p,WarGunItem.Kind.MACHINE_GUN,aim.scale(8));s.addFreshEntity(round);
        ammo--;heat+=7;fireAt=level.getGameTime()+2;setChanged();sound(SoundEvents.FIREWORK_ROCKET_BLAST,.8F,.65F);s.sendParticles(ParticleTypes.FLAME,muzzle.x,muzzle.y,muzzle.z,2,.02,.02,.02,.02);return true;
    }
    public static void tick(Level l,BlockPos pos,BlockState state,FieldDeviceBlockEntity d){
        if(!(l instanceof ServerLevel s))return;if(d.heat>0){d.heat--;if(l.getGameTime()%10==0)d.changed();}
        if(d.kind()==FieldDeviceBlock.Kind.GUN&&l.getGameTime()%2==0){var a=d.seat();if(a!=null&&a.getFirstPassenger() instanceof Player p){float base=state.getValue(FieldDeviceBlock.FACING).toYRot();d.aimYaw=base+Mth.clamp(Mth.wrapDegrees(p.getYRot()-base),-65,65);d.aimPitch=Mth.clamp(p.getXRot(),-35,35);if(l.getGameTime()%10==0)d.changed();}}
        if(d.kind()==FieldDeviceBlock.Kind.GUN&&l.getGameTime()%20==0){var seat=d.seat();if(seat!=null&&seat.getPassengers().isEmpty()){seat.discard();d.seatId=null;d.setChanged();}}
        if(!d.enabled())return;
        if(d.victim!=null){var e=s.getEntity(d.victim);if(e instanceof LivingEntity living&&disturbed(living,d.trigger,d.slot,d.held,d.offhand))d.explode(3.5F);}
        if(d.kind()==FieldDeviceBlock.Kind.ALARM&&l.getGameTime()%5==Math.floorMod(pos.asLong(),5)&&OutpostBudget.sensor(s)){
            Vec3 dir=Vec3.atLowerCornerOf(state.getValue(FieldDeviceBlock.FACING).getNormal());Vec3 from=Vec3.atCenterOf(pos).add(dir.scale(.55));Vec3 end=from;for(int step=1;step<=8;step++){var next=from.add(dir.scale(step));if(!l.hasChunkAt(BlockPos.containing(next)))break;end=next;}
            var clip=l.clip(new ClipContext(from,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,(Entity)null));end=clip.getLocation();var area=new AABB(from,end).inflate(.16,.5,.16);
            if(!l.getEntitiesOfClass(LivingEntity.class,area,e->e.isAlive()&&!e.isSpectator()&&!e.getUUID().equals(d.owner)&&!net.caravidro.wayaround.observation.EntitySpectate.ghost(e)).isEmpty()){d.alarmUntil=l.getGameTime()+40;d.active(true);if(l.getGameTime()%20==0)d.sound(SoundEvents.NOTE_BLOCK_BELL.value(),1.5F,.6F);d.setChanged();}
            else if(l.getGameTime()>=d.alarmUntil)d.active(false);
            if(state.getValue(FieldDeviceBlock.ACTIVE))s.sendParticles(ParticleTypes.ELECTRIC_SPARK,end.x,end.y,end.z,1,.05,.05,.05,0);
        }
    }
    public void removed(){var a=seat();if(a!=null){a.ejectPassengers();a.discard();}if(level==null)return;if(cover!=null)Block.popResource(level,worldPosition,new ItemStack(OutpostContent.CAMO.get()));if(getBlockState().getValue(FieldDeviceBlock.BARREL))Block.popResource(level,worldPosition,new ItemStack(OutpostContent.GUN_BARREL.get()));while(ammo>0){int n=Math.min(64,ammo);Block.popResource(level,worldPosition,new ItemStack(Items.IRON_NUGGET,n));ammo-=n;}}
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);if(owner!=null)t.putUUID("Owner",owner);if(victim!=null)t.putUUID("Victim",victim);if(seatId!=null)t.putUUID("Seat",seatId);t.putDouble("X",trigger.x);t.putDouble("Y",trigger.y);t.putDouble("Z",trigger.z);t.putFloat("AimYaw",aimYaw);t.putFloat("AimPitch",aimPitch);t.putInt("Slot",slot);t.putInt("Ammo",ammo);t.putInt("Heat",heat);t.putLong("Ready",readyAt);t.putLong("AlarmUntil",alarmUntil);if(!held.isEmpty())t.put("Held",held.save(r));if(!offhand.isEmpty())t.put("Offhand",offhand.save(r));if(cover!=null)t.put("Cover",NbtUtils.writeBlockState(cover));}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);owner=t.hasUUID("Owner")?t.getUUID("Owner"):null;victim=t.hasUUID("Victim")?t.getUUID("Victim"):null;seatId=t.hasUUID("Seat")?t.getUUID("Seat"):null;trigger=new Vec3(t.getDouble("X"),t.getDouble("Y"),t.getDouble("Z"));aimYaw=t.getFloat("AimYaw");aimPitch=t.getFloat("AimPitch");slot=t.getInt("Slot");ammo=Mth.clamp(t.getInt("Ammo"),0,256);heat=Mth.clamp(t.getInt("Heat"),0,90);readyAt=t.getLong("Ready");alarmUntil=t.getLong("AlarmUntil");held=ItemStack.parseOptional(r,t.getCompound("Held"));offhand=ItemStack.parseOptional(r,t.getCompound("Offhand"));cover=t.contains("Cover")?NbtUtils.readBlockState(r.lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK),t.getCompound("Cover")):null;}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
