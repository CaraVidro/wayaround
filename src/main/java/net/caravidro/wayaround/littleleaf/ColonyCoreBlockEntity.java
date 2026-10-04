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
    private long last=-1,work,workClock;private boolean habitat=true,interior,queenBorn,gardenPrepared,queenDead;private UUID queenId;
    private GlobalPos source;private int decayCursor,soilCursor;private final Map<UUID,BlockPos> assignments=new HashMap<>();
    private int buildStage=-1,buildCursor,clearCursor;private boolean clearing;private final ArrayList<Long> owned=new ArrayList<>();
    private final LinkedHashSet<UUID> enemies=new LinkedHashSet<>();private BlockPos food,foodCandidate,climbSource,climbPoint;private int foodColumn,foodY=Integer.MIN_VALUE;
    private static final List<BlockPos> FOOD_COLUMNS=new ArrayList<>(),SOIL_COLUMNS=new ArrayList<>();
    static {for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++)FOOD_COLUMNS.add(new BlockPos(x,0,z));FOOD_COLUMNS.sort(Comparator.comparingInt(p->p.getX()*p.getX()+p.getZ()*p.getZ()));}
    static {for(int x=-24;x<=24;x++)for(int z=-24;z<=24;z++)SOIL_COLUMNS.add(new BlockPos(x,0,z));SOIL_COLUMNS.sort(Comparator.comparingInt(p->p.getX()*p.getX()+p.getZ()*p.getZ()));}
    public ColonyCoreBlockEntity(BlockPos p,BlockState s){super(LittleLeafContent.CORE_ENTITY.get(),p,s);}
    public int species(){return getBlockState().getValue(ColonyCoreBlock.SPECIES);}
    public boolean giant(){return getBlockState().getValue(ColonyCoreBlock.GIANT);}
    public int stage(){return getBlockState().getValue(ColonyCoreBlock.STAGE);}
    public long work(){return work;}
    public boolean interior(){return interior;}
    public boolean abandoned(){return queenDead;}
    public void linkSource(GlobalPos p){source=p;setChanged();syncAbandonment();}
    public GlobalPos identity(){return interior&&source!=null?source:GlobalPos.of(level.dimension(),worldPosition);}
    private void syncAbandonment(){if(level instanceof ServerLevel l&&ColonyTransitData.get(l.getServer()).abandoned(identity())&&!queenDead){queenDead=true;decayCursor=0;setChanged();var q=queen(l);if(q!=null)q.kill();}}

    @Override public void onLoad(){super.onLoad();if(level instanceof ServerLevel l){ACTIVE.computeIfAbsent(l,k->new LinkedHashSet<>()).add(this);if(last<0)last=l.getGameTime();advance(l.getGameTime());syncAbandonment();}}
    @Override public void setRemoved(){if(level instanceof ServerLevel l){var a=ACTIVE.get(l);if(a!=null)a.remove(this);}super.setRemoved();}
    public static void debugPulse(ServerLevel l,long dt){var set=ACTIVE.get(l);if(set==null)return;int n=0;for(var c:set){if(n++>=128)break;if(!c.isRemoved()){c.workClock+=Math.max(0,Math.min(dt,24000));c.advance(l.getGameTime());}}}
    public static void clear(){ACTIVE.clear();}
    public void initialize(long now,long oldWork,boolean available){last=now;work=Math.max(0,oldWork);habitat=available;setChanged();}
    public void initializeMound(long now,long oldWork,int radius,int height){
        initialize(now,oldWork,true);gardenPrepared=true;
        for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)for(int y=0;y<height;y++){
            if(x*x+z*z>(radius-y*.45)*(radius-y*.45)||x==0&&z==0||x==0&&z==1&&y<2)continue;
            owned.add(worldPosition.offset(x,y,z).asLong());
        }
        setChanged();
    }
    public void makeInterior(int species,int stage){interior=true;work=ColonyRules.THRESHOLDS[stage];level.setBlock(worldPosition,getBlockState().setValue(ColonyCoreBlock.SPECIES,species).setValue(ColonyCoreBlock.GIANT,true).setValue(ColonyCoreBlock.STAGE,stage),3);setChanged();}
    public static void tick(Level l,BlockPos p,BlockState state,ColonyCoreBlockEntity c){
        if(!(l instanceof ServerLevel s)||l.getGameTime()%20!=Math.floorMod(p.asLong(),20))return;
        if(!net.caravidro.wayaround.worldconfig.WorldFeatureRuntime.serverEnabled(net.caravidro.wayaround.worldconfig.WorldFeature.LITTLE_LEAF_WORLD))return;
        c.syncAbandonment();c.advance(s.getGameTime());
        if(c.queenDead){c.decayGarden(s);return;}
        if(!c.interior)c.prepareGarden(s);
        else ColonyTravel.releaseInvaders(s,c);
        if(s.getNearestPlayer(p.getX(),p.getY(),p.getZ(),c.interior?96:48,false)==null)return;
        if((s.getGameTime()/20)%6==0)c.birth(s);
    }
    public void advance(long now){
        long dt=last<0?0:Math.max(0,now-last);last=now;
        if(!interior&&habitat&&!queenDead){workClock+=Math.min(dt,24000L*30);work=ColonyRules.advanceWork(work,workClock,true);workClock%=1200;}
        int target=ColonyRules.stage(work,giant());
        if(!interior&&level!=null&&stage()!=target)level.setBlock(worldPosition,getBlockState().setValue(ColonyCoreBlock.STAGE,target),3);
        if(dt>0)setChanged();
    }
    public void invertColony(){if(interior)return;level.setBlock(worldPosition,getBlockState().setValue(ColonyCoreBlock.GIANT,!giant()).setValue(ColonyCoreBlock.STAGE,ColonyRules.stage(work,!giant())),3);buildStage=-1;setChanged();}
    public boolean acceptLoad(ColonyInsectEntity insect){
        if(queenDead||!(level instanceof ServerLevel l)||!insect.carrying()||!worldPosition.equals(insect.home()))return false;
        if(insect.enlarged()&&!giant()&&!interior){
            if(l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,insect.getBoundingBox().inflate(3)).size()<4)insect.spawnAtLocation(new ItemStack(LittleLeafContent.LEAF_FRAGMENT.get(),4));
        }else {
            delivered(insect.enlarged());
            var f=interior?worldPosition.offset(26,0,7):worldPosition.offset(1,-3,0);
            if(loaded(l,f)&&l.getBlockState(f).is(LittleLeafContent.COLONY_FUNGUS.get()))l.setBlock(f,l.getBlockState(f).setValue(ColonyFungusBlock.RIPE,true),3);
            if(species()==2&&l.random.nextInt(16)==0)insect.spawnAtLocation(LittleLeafContent.HONEYDEW.get());
        }
        insect.carry(false);ColonyEffects.work(l,insect.blockPosition(),Blocks.OAK_LEAVES.defaultBlockState(),net.minecraft.sounds.SoundEvents.COMPOSTER_FILL_SUCCESS,enlargedVolume(insect));return true;
    }
    public void delivered(boolean large){if(queenDead)return;work=Math.min(4096,work+(large?8:1));setChanged();}
    public void remember(UUID id){if(enemies.contains(id))return;if(enemies.size()>=16)enemies.remove(enemies.iterator().next());enemies.add(id);setChanged();}
    public void queenDied(){
        if(queenDead)return;queenDead=true;decayCursor=0;assignments.clear();setChanged();
        if(level instanceof ServerLevel l){ColonyTransitData.get(l.getServer()).abandon(identity());l.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.SILVERFISH_DEATH,net.minecraft.sounds.SoundSource.NEUTRAL,.35F,.55F);decayGarden(l);}
    }
    private void decayGarden(ServerLevel l){
        var center=interior?worldPosition.offset(28,0,7):worldPosition.offset(1,-3,0);int total=interior?75:1;
        for(int i=0;i<16&&decayCursor<total&&ColonyBudget.build(l);i++){
            int n=decayCursor;var p=interior?center.offset(n%5-2,n/25,(n/5)%5-2):center;if(!loaded(l,p))return;decayCursor++;
            var state=l.getBlockState(p);if(state.is(LittleLeafContent.COLONY_FUNGUS.get())){l.setBlock(p,state.setValue(ColonyFungusBlock.ALIVE,false).setValue(ColonyFungusBlock.RIPE,false),3);ColonyEffects.work(l,p,state,net.minecraft.sounds.SoundEvents.COMPOSTER_EMPTY,.08F);}
        }setChanged();
    }
    public ColonyInsectEntity queen(ServerLevel l){return queenId!=null&&l.getEntity(queenId) instanceof ColonyInsectEntity q&&q.isAlive()?q:null;}
    public boolean hostile(Player p){return enemies.contains(p.getUUID());}
    public BlockPos food(ServerLevel l){
        if(queenDead)return null;
        if(interior)return worldPosition.offset(28,0,4);
        if(food!=null&&loaded(l,food)&&edible(l.getBlockState(food)))return food;
        food=null;
        for(int i=0;i<48&&ColonyBudget.search(l);i++){
            var offset=FOOD_COLUMNS.get(foodColumn);var column=worldPosition.offset(offset);
            if(!loaded(l,column)){nextFoodColumn();continue;}
            if(foodY==Integer.MIN_VALUE){foodY=Math.min(worldPosition.getY()+32,l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,column.getX(),column.getZ())-1);continue;}
            if(foodY<worldPosition.getY()-2){if(foodCandidate!=null){food=foodCandidate;nextFoodColumn();break;}nextFoodColumn();continue;}
            var p=new BlockPos(column.getX(),foodY--,column.getZ());
            if(edible(l.getBlockState(p)))foodCandidate=p;
            else if(foodCandidate!=null){food=foodCandidate;nextFoodColumn();break;}
        }
        return food;
    }
    public BlockPos approach(ServerLevel l,BlockPos source,ColonyInsectEntity insect){
        if(interior||insect.enlarged()||source.getY()-insect.getY()<1.1)return source.above();
        if(!source.equals(climbSource)){
            climbSource=source.immutable();climbPoint=null;double best=Double.MAX_VALUE;
            // Shared climb route: a short, budgeted scan for the supporting trunk near the lower canopy.
            for(int y=1;y<=3;y++)for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){
                if(!ColonyBudget.search(l)){if(climbPoint==null)climbSource=null;return climbPoint!=null?climbPoint:source.above();}var p=source.offset(x,-y,z);
                if(!loaded(l,p)||!l.getBlockState(p).is(BlockTags.LOGS))continue;
                double d=x*x+z*z+y*.2;if(d<best){best=d;climbPoint=p.above(y+1);}
            }
        }
        return climbPoint!=null?climbPoint:source.above();
    }
    private void nextFoodColumn(){foodColumn=(foodColumn+1)%FOOD_COLUMNS.size();foodY=Integer.MIN_VALUE;foodCandidate=null;}
    public void rejectFood(BlockPos p){if(p!=null&&p.equals(food)){food=null;nextFoodColumn();}}
    private boolean edible(BlockState s){return s.is(BlockTags.LEAVES)||(species()==3&&s.is(BlockTags.LOGS));}
    public boolean cut(ServerLevel l,BlockPos p,ColonyInsectEntity insect){
        if(queenDead||insect.carrying()||!loaded(l,p)||!edible(l.getBlockState(p))||!insect.getBoundingBox().inflate(insect.enlarged()?.65:.20).intersects(new AABB(p)))return false;
        insect.carry(true); // A cut is a fragment, not a whole disappearing leaf or trunk.
        ColonyEffects.work(l,p,l.getBlockState(p),net.minecraft.sounds.SoundEvents.GRASS_BREAK,enlargedVolume(insect));return true;
    }
    void birth(ServerLevel l){
        if(queenDead||!ColonyBudget.birth(l))return;
        var nearby=l.getEntitiesOfClass(ColonyInsectEntity.class,new AABB(worldPosition).inflate(interior?112:48));
        long own=nearby.stream().filter(e->worldPosition.equals(e.home())).count();
        if(own>=ColonyRules.population(stage(),interior)||nearby.size()>=64)return;
        int role=!queenBorn?2:own%4==0?1:0;
        var e=LittleLeafContent.type(species()).create(l);if(e==null)return;
        e.bind(worldPosition,role,interior,giant());
        var preferred=interior?(role==2?worldPosition.offset(64,0,4):worldPosition.offset(8+(int)(own%3)*3,0,4)):worldPosition.above();
        if(!placeAtEntrance(l,e,preferred))return;
        if(l.addFreshEntity(e)){if(role==2){queenBorn=true;queenId=e.getUUID();}setChanged();}
    }
    /** Births require a real supporting surface and a clear body, never an arbitrary air coordinate. */
    boolean placeAtEntrance(ServerLevel l,ColonyInsectEntity e,BlockPos preferred){
        int radius=e.enlarged()?6:2;
        for(int r=0;r<=radius;r++)for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++){
            if(Math.max(Math.abs(x),Math.abs(z))!=r)continue;
            for(int dy=0;dy>=-3;dy--){var q=preferred.offset(x,dy,z);
                if(!loaded(l,q)||!loaded(l,q.below())||!l.getFluidState(q).isEmpty()||!l.getBlockState(q.below()).isFaceSturdy(l,q.below(),Direction.UP))continue;
                e.moveTo(q.getX()+.5,q.getY(),q.getZ()+.5,l.random.nextFloat()*360,0);
                if(l.noCollision(e))return true;
            }
        }
        return false;
    }
    private void prepareGarden(ServerLevel l){
        if(queenDead||gardenPrepared||!ColonyBudget.reserveBuild(l,38))return;
        // A crafted core only excavates soil, with the same small chamber as natural mounds.
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=-3;y<=-2;y++){
            var p=worldPosition.offset(x,y,z);if(!loaded(l,p))return;var s=l.getBlockState(p);
            if(!l.getFluidState(p).isEmpty()||!(s.isAir()||s.is(BlockTags.DIRT)||s.is(LittleLeafContent.COLONY_FUNGUS.get())))return;
        }
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=-3;y<=-2;y++){
            var p=worldPosition.offset(x,y,z);if(l.getBlockState(p).is(BlockTags.DIRT))l.setBlock(p,Blocks.AIR.defaultBlockState(),18);
        }
        var f=worldPosition.offset(1,-3,0);if(l.getBlockState(f).isAir())l.setBlock(f,LittleLeafContent.COLONY_FUNGUS.get().defaultBlockState(),18);
        gardenPrepared=true;setChanged();
    }
    private float enlargedVolume(ColonyInsectEntity insect){return insect.enlarged()?.32F:.06F;}
    /** Blueprints request work; only a carrying insect is allowed to place a block. */
    public BlockPos buildSite(ServerLevel l,ColonyInsectEntity insect){
        if(queenDead||interior||!giant()||!l.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING))return null;
        var assigned=assignments.get(insect.getUUID());if(assigned!=null&&loaded(l,assigned)&&l.getBlockState(assigned).isAir())return assigned;assignments.remove(insect.getUUID());
        assignments.entrySet().removeIf(e->l.getEntity(e.getKey())==null);
        if(buildStage!=stage()){buildStage=stage();buildCursor=0;setChanged();}
        int r=ColonyRules.radius(stage(),species()==3),h=ColonyRules.height(stage(),species()==3),extent=r+3,side=extent*2+1,total=side*side*(h+1);
        for(int n=0;n<32&&ColonyBudget.build(l);n++){
            if(buildCursor>=total){buildCursor=0;break;}
            int index=buildCursor++,y=index/(side*side),x=index%side-extent,z=(index/side)%side-extent;setChanged();var p=worldPosition.offset(x,y,z);if(!loaded(l,p))continue;
            boolean doorway=(Math.abs(x)<=1||Math.abs(z)<=1)&&y<4;double radius=r*(1-y/(double)(h+2)),d=Math.sqrt(x*x+z*z);
            boolean shell=!doorway&&d<=radius+1&&d>=Math.max(1,radius-1.3);
            boolean wall=stage()>=2&&Math.max(Math.abs(x),Math.abs(z))==r+2&&y<4&&(!doorway||y==3);
            boolean battlement=stage()>=4&&Math.max(Math.abs(x),Math.abs(z))==r+2&&y==4&&(x+z)%2==0;
            boolean tower=stage()>=4&&Math.abs(x)>=r&&Math.abs(z)>=r&&y<Math.min(h,8);
            if(p.equals(worldPosition)||(x==0&&z==1&&y<2)||!(shell||wall||battlement||tower)||!l.getBlockState(p).isAir()||!l.getFluidState(p).isEmpty()||assignments.containsValue(p))continue;
            // Avoid unsupported floating pieces; walls grow from their foundation upwards.
            if(l.getBlockState(p.below()).getCollisionShape(l,p.below()).isEmpty())continue;
            assignments.put(insect.getUUID(),p);return p;
        }return null;
    }
    public BlockPos soil(ServerLevel l){
        int exclusion=ColonyRules.radius(stage(),species()==3)+4;
        for(int n=0;n<16&&ColonyBudget.search(l);n++){
            var offset=SOIL_COLUMNS.get(soilCursor++%SOIL_COLUMNS.size());int x=offset.getX(),z=offset.getZ();if(Math.max(Math.abs(x),Math.abs(z))<exclusion)continue;
            var column=worldPosition.offset(x,0,z);if(!loaded(l,column))continue;
            var p=new BlockPos(column.getX(),Math.min(worldPosition.getY()+8,l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,column.getX(),column.getZ())-1),column.getZ());
            for(int y=0;y<12;y++){var q=p.below(y);if(Math.abs(q.getY()-worldPosition.getY())<=12&&material(l,q))return q;}
        }return null;
    }
    private boolean material(ServerLevel l,BlockPos p){return loaded(l,p)&&l.getFluidState(p).isEmpty()&&l.getBlockEntity(p)==null&&!owned.contains(p.asLong())&&(l.getBlockState(p).is(BlockTags.DIRT)||l.getBlockState(p).is(Blocks.CLAY));}
    public boolean excavate(ServerLevel l,BlockPos p,ColonyInsectEntity insect){
        if(queenDead||!giant()||!insect.enlarged()||insect.carryingMaterial()||insect.carrying()||!l.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)||!material(l,p)||worldPosition.distSqr(p)<Math.pow(ColonyRules.radius(stage(),species()==3)+3,2)||!insect.getBoundingBox().inflate(.75).intersects(new AABB(p))||!ColonyBudget.build(l))return false;
        var state=l.getBlockState(p);if(!l.setBlock(p,Blocks.AIR.defaultBlockState(),3))return false;
        insect.material(state.is(Blocks.GRASS_BLOCK)?Blocks.DIRT.defaultBlockState():state);ColonyEffects.work(l,p,state,net.minecraft.sounds.SoundEvents.GRAVEL_BREAK,.3F);return true;
    }
    public boolean placeMaterial(ServerLevel l,BlockPos p,ColonyInsectEntity insect){
        if(queenDead||!giant()||!insect.carryingMaterial()||!l.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)||!loaded(l,p)||!l.getBlockState(p).isAir()||!l.getFluidState(p).isEmpty()||!insect.getBoundingBox().inflate(.85).intersects(new AABB(p))||insect.getBoundingBox().intersects(new AABB(p))||!ColonyBudget.build(l))return false;
        var expected=assignments.get(insect.getUUID());if(expected!=null&&!expected.equals(p))return false;
        if(p.distSqr(worldPosition)>Math.pow(ColonyRules.radius(stage(),species()==3)+8,2))return false;
        var state=insect.material();if(!l.setBlock(p,state,3))return false;
        insect.material(null);assignments.remove(insect.getUUID());if(owned.size()<8192)owned.add(p.asLong());setChanged();ColonyEffects.work(l,p,state,net.minecraft.sounds.SoundEvents.GRAVEL_PLACE,.35F);return true;
    }
    public ColonyCoreBlockEntity rivalColony(ServerLevel l){
        ColonyCoreBlockEntity best=null;double distance=48*48;int checked=0;
        for(int x=(worldPosition.getX()>>4)-3;x<=(worldPosition.getX()>>4)+3;x++)for(int z=(worldPosition.getZ()>>4)-3;z<=(worldPosition.getZ()>>4)+3;z++){
            if(!ColonyBudget.search(l))return best;var chunk=l.getChunkSource().getChunkNow(x,z);if(chunk==null)continue;
            for(var be:chunk.getBlockEntities().values()){if(++checked>128)return best;if(be instanceof ColonyCoreBlockEntity c&&c!=this&&!c.abandoned()&&c.species()!=species()&&!c.interior()){double d=worldPosition.distSqr(c.worldPosition);if(d<distance){best=c;distance=d;}}}
        }return best;
    }
    public static boolean loaded(ServerLevel l,BlockPos p){return l.getChunkSource().getChunkNow(p.getX()>>4,p.getZ()>>4)!=null;}
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);t.putLong("Clock",last);t.putInt("DecayCursor",decayCursor);if(source!=null){t.putString("SourceDimension",source.dimension().location().toString());t.putLong("SourcePos",source.pos().asLong());}t.putLong("Work",work);t.putLong("WorkClock",workClock);t.putBoolean("Habitat",habitat);t.putBoolean("Interior",interior);t.putBoolean("QueenBorn",queenBorn);t.putBoolean("QueenDead",queenDead);t.putBoolean("GardenPrepared",gardenPrepared);if(queenId!=null)t.putUUID("QueenId",queenId);t.putInt("BuildStage",buildStage);t.putInt("BuildCursor",buildCursor);t.putInt("ClearCursor",clearCursor);t.putBoolean("Clearing",clearing);t.putLongArray("Owned",owned.stream().mapToLong(Long::longValue).toArray());var list=new ListTag();for(var id:enemies){var q=new CompoundTag();q.putUUID("Id",id);list.add(q);}t.put("Enemies",list);}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);decayCursor=Math.max(0,t.getInt("DecayCursor"));var sourceId=net.minecraft.resources.ResourceLocation.tryParse(t.getString("SourceDimension"));source=sourceId==null?null:GlobalPos.of(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,sourceId),BlockPos.of(t.getLong("SourcePos")));last=t.contains("Clock")?t.getLong("Clock"):-1;work=Math.max(0,Math.min(4096,t.getLong("Work")));workClock=Math.floorMod(t.getLong("WorkClock"),1200);habitat=!t.contains("Habitat")||t.getBoolean("Habitat");interior=t.getBoolean("Interior");queenBorn=t.getBoolean("QueenBorn");queenDead=t.getBoolean("QueenDead");gardenPrepared=t.getBoolean("GardenPrepared");queenId=t.hasUUID("QueenId")?t.getUUID("QueenId"):null;buildStage=t.contains("BuildStage")?t.getInt("BuildStage"):-1;buildCursor=Math.max(0,t.getInt("BuildCursor"));clearCursor=Math.max(0,t.getInt("ClearCursor"));clearing=t.getBoolean("Clearing");owned.clear();for(long p:t.getLongArray("Owned")){if(owned.size()>=8192)break;owned.add(p);}clearCursor=Math.min(clearCursor,owned.size());enemies.clear();for(var q:t.getList("Enemies",Tag.TAG_COMPOUND)){var c=(CompoundTag)q;if(c.hasUUID("Id")&&enemies.size()<16)enemies.add(c.getUUID("Id"));}}
}
