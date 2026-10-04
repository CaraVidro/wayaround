package net.caravidro.wayaround.littleleaf;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** One persistent colony clock; only a capped sample of its population is materialized. */
public final class ColonyCoreBlockEntity extends BlockEntity {
    private static final Map<ServerLevel,Set<ColonyCoreBlockEntity>> ACTIVE=new WeakHashMap<>();
    private long last=-1,work,workClock;private boolean habitat=true,interior,queenBorn;
    private int buildStage=-1,buildCursor,clearCursor;private boolean clearing;private final ArrayList<Long> owned=new ArrayList<>();
    private final LinkedHashSet<UUID> enemies=new LinkedHashSet<>();private BlockPos food;
    public ColonyCoreBlockEntity(BlockPos p,BlockState s){super(LittleLeafContent.CORE_ENTITY.get(),p,s);}
    public int species(){return getBlockState().getValue(ColonyCoreBlock.SPECIES);}
    public boolean giant(){return getBlockState().getValue(ColonyCoreBlock.GIANT);}
    public int stage(){return getBlockState().getValue(ColonyCoreBlock.STAGE);}
    public long work(){return work;}
    public boolean interior(){return interior;}
    @Override public void onLoad(){super.onLoad();if(level instanceof ServerLevel l){ACTIVE.computeIfAbsent(l,k->new LinkedHashSet<>()).add(this);if(last<0)last=l.getGameTime();advance(l.getGameTime());}}
    @Override public void setRemoved(){if(level instanceof ServerLevel l){var a=ACTIVE.get(l);if(a!=null)a.remove(this);}super.setRemoved();}
    public static void debugPulse(ServerLevel l,long dt){var set=ACTIVE.get(l);if(set==null)return;int n=0;for(var c:List.copyOf(set)){if(n++>=128)break;if(!c.isRemoved()){c.workClock+=Math.max(0,Math.min(dt,24000));c.advance(l.getGameTime());}}}
    public static void clear(){ACTIVE.clear();}
    public void initialize(long now,long oldWork,boolean available){last=now;work=Math.max(0,oldWork);habitat=available;setChanged();}
    public void makeInterior(int species,int stage){interior=true;work=ColonyRules.THRESHOLDS[stage];level.setBlock(worldPosition,getBlockState().setValue(ColonyCoreBlock.SPECIES,species).setValue(ColonyCoreBlock.GIANT,true).setValue(ColonyCoreBlock.STAGE,stage),3);setChanged();}
    public static void tick(Level l,BlockPos p,BlockState state,ColonyCoreBlockEntity c){
        if(!(l instanceof ServerLevel s)||l.getGameTime()%20!=Math.floorMod(p.asLong(),20))return;
        if(!net.caravidro.wayaround.worldconfig.WorldFeatureRuntime.serverEnabled(net.caravidro.wayaround.worldconfig.WorldFeature.LITTLE_LEAF_WORLD))return;
        c.advance(s.getGameTime());
        if(!c.interior)c.build(s);
        if(s.getNearestPlayer(p.getX(),p.getY(),p.getZ(),c.interior?96:48,false)==null)return;
        if((s.getGameTime()/20)%6==0)c.birth(s);
    }
    public void advance(long now){
        long dt=last<0?0:Math.max(0,now-last);last=now;
        if(!interior&&habitat){workClock+=Math.min(dt,24000L*30);work=ColonyRules.advanceWork(work,workClock,true);workClock%=1200;}
        int target=ColonyRules.stage(work,giant());
        if(!interior&&level!=null&&stage()!=target)level.setBlock(worldPosition,getBlockState().setValue(ColonyCoreBlock.STAGE,target),3);
        if(dt>0)setChanged();
    }
    public void invertColony(){if(interior)return;level.setBlock(worldPosition,getBlockState().setValue(ColonyCoreBlock.GIANT,!giant()).setValue(ColonyCoreBlock.STAGE,ColonyRules.stage(work,!giant())),3);buildStage=-1;setChanged();}
    public void delivered(boolean large){work=Math.min(4096,work+(large?8:1));food=null;setChanged();}
    public void remember(UUID id){if(enemies.contains(id))return;if(enemies.size()>=16)enemies.remove(enemies.iterator().next());enemies.add(id);setChanged();}
    public boolean hostile(Player p){return enemies.contains(p.getUUID());}
    public BlockPos food(ServerLevel l){
        if(interior)return worldPosition.offset(28,0,4);
        if(food!=null&&loaded(l,food)&&edible(l.getBlockState(food)))return food;
        food=null;
        for(int i=0;i<48&&ColonyBudget.search(l);i++){
            var p=worldPosition.offset(l.random.nextInt(25)-12,l.random.nextInt(13),l.random.nextInt(25)-12);
            if(loaded(l,p)&&edible(l.getBlockState(p))){food=p.immutable();break;}
        }
        return food;
    }
    private boolean edible(BlockState s){return s.is(BlockTags.LEAVES)||(species()==3&&s.is(BlockTags.LOGS));}
    public void cut(ServerLevel l,BlockPos p,ColonyInsectEntity insect){
        if(!loaded(l,p)||(!interior&&!edible(l.getBlockState(p))))return;
        insect.carry(true); // A cut is a fragment, not a whole disappearing leaf or trunk.
        l.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,l.getBlockState(p)),p.getX()+.5,p.getY()+.1,p.getZ()+.5,2,.1,.1,.1,.01);
    }
    private void birth(ServerLevel l){
        if(!ColonyBudget.birth(l))return;
        var nearby=l.getEntitiesOfClass(ColonyInsectEntity.class,new AABB(worldPosition).inflate(interior?112:48));
        long own=nearby.stream().filter(e->worldPosition.equals(e.home())).count();
        if(own>=ColonyRules.population(stage(),interior)||nearby.size()>=64)return;
        int role=!queenBorn?2:own%4==0?1:0;
        var e=LittleLeafContent.type(species()).create(l);if(e==null)return;
        var spawn=interior?(role==2?worldPosition.offset(64,0,4):worldPosition.offset(8+(int)(own%3)*3,0,4)):
            role==2?worldPosition.offset(0,-3,1):worldPosition.offset(0,0,ColonyRules.radius(stage(),species()==3)+2);
        if(!loaded(l,spawn))return;
        e.bind(worldPosition,role,interior,giant());e.moveTo(spawn.getX()+.5,spawn.getY(),spawn.getZ()+.5,l.random.nextFloat()*360,0);
        if(!l.noCollision(e))return;
        if(l.addFreshEntity(e)){if(role==2)queenBorn=true;setChanged();}
    }
    /** Incremental shell/wall/tower construction; respects existing blocks and chunk boundaries. */
    private void build(ServerLevel l){
        if(buildStage!=stage()){buildStage=stage();buildCursor=0;clearCursor=0;clearing=!owned.isEmpty();setChanged();}
        int r=ColonyRules.radius(stage(),species()==3),h=ColonyRules.height(stage(),species()==3),extent=r+3,side=extent*2+1,total=side*side*(h+1);
        for(int n=0;n<32&&ColonyBudget.build(l);n++){
            if(clearing&&clearCursor<owned.size()){
                var p=BlockPos.of(owned.get(clearCursor));if(!loaded(l,p))break;
                if(l.getBlockState(p).is(Blocks.DIRT))l.setBlock(p,Blocks.AIR.defaultBlockState(),18);
                clearCursor++;if(clearCursor==owned.size()){owned.clear();clearCursor=0;clearing=false;}setChanged();continue;
            }
            if(buildCursor>=total)break;
            int index=buildCursor,y=index/(side*side),x=index%side-extent,z=(index/side)%side-extent;
            var p=worldPosition.offset(x,y,z);if(!loaded(l,p))break;buildCursor++;setChanged();
            boolean doorway=(Math.abs(x)<=1||Math.abs(z)<=1)&&y<4;
            double radius=r*(1-y/(double)(h+2));double d=Math.sqrt(x*x+z*z);
            boolean shell=!doorway&&y>0&&d<=radius+1&&d>=Math.max(1,radius-1.3);
            boolean wall=stage()>=2&&Math.max(Math.abs(x),Math.abs(z))==r+2&&y<4&&(!doorway||y==3);
            boolean battlement=stage()>=4&&Math.max(Math.abs(x),Math.abs(z))==r+2&&y==4&&(x+z)%2==0;
            boolean tower=stage()>=4&&Math.abs(x)>=r&&Math.abs(z)>=r&&y<Math.min(h,8);
            if(p.equals(worldPosition)||(x==0&&z==1&&y<2)||!(shell||wall||battlement||tower))continue;
            var old=l.getBlockState(p);if(!old.canBeReplaced()||!l.getFluidState(p).isEmpty())continue;
            if(l.setBlock(p,Blocks.DIRT.defaultBlockState(),18)&&owned.size()<8192)owned.add(p.asLong());
        }
    }
    public static boolean loaded(ServerLevel l,BlockPos p){return l.getChunkSource().getChunkNow(p.getX()>>4,p.getZ()>>4)!=null;}
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);t.putLong("Clock",last);t.putLong("Work",work);t.putLong("WorkClock",workClock);t.putBoolean("Habitat",habitat);t.putBoolean("Interior",interior);t.putBoolean("QueenBorn",queenBorn);t.putInt("BuildStage",buildStage);t.putInt("BuildCursor",buildCursor);t.putInt("ClearCursor",clearCursor);t.putBoolean("Clearing",clearing);t.putLongArray("Owned",owned.stream().mapToLong(Long::longValue).toArray());var list=new ListTag();for(var id:enemies){var q=new CompoundTag();q.putUUID("Id",id);list.add(q);}t.put("Enemies",list);}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);last=t.contains("Clock")?t.getLong("Clock"):-1;work=Math.max(0,Math.min(4096,t.getLong("Work")));workClock=Math.floorMod(t.getLong("WorkClock"),1200);habitat=!t.contains("Habitat")||t.getBoolean("Habitat");interior=t.getBoolean("Interior");queenBorn=t.getBoolean("QueenBorn");buildStage=t.contains("BuildStage")?t.getInt("BuildStage"):-1;buildCursor=Math.max(0,t.getInt("BuildCursor"));clearCursor=Math.max(0,t.getInt("ClearCursor"));clearing=t.getBoolean("Clearing");owned.clear();for(long p:t.getLongArray("Owned")){if(owned.size()>=8192)break;owned.add(p);}clearCursor=Math.min(clearCursor,owned.size());enemies.clear();for(var q:t.getList("Enemies",Tag.TAG_COMPOUND)){var c=(CompoundTag)q;if(c.hasUUID("Id")&&enemies.size()<16)enemies.add(c.getUUID("Id"));}}
}
