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
    private final LinkedHashSet<Long> connections=new LinkedHashSet<>();private final ArrayDeque<BlockPos> accessSites=new ArrayDeque<>();private BlockPos satelliteSite;
    private final LinkedHashSet<Long> pendingInteriorChunks=new LinkedHashSet<>(),protectedInterior=new LinkedHashSet<>();private long repairChunk=Long.MIN_VALUE;private int repairCursor;private boolean layoutStopped;
    private int interiorProfile,layoutCursor,foundersBorn;private GlobalPos source;private int decayCursor,soilCursor;private final Map<UUID,BlockPos> assignments=new HashMap<>();
    private int buildStage=-1,buildCursor,clearCursor;private boolean clearing;private final ArrayList<Long> owned=new ArrayList<>();
    private final LinkedHashMap<BlockPos,Long> rejectedFood=new LinkedHashMap<>();
    private final LinkedHashSet<UUID> enemies=new LinkedHashSet<>();private BlockPos food,foodCandidate;private int foodColumn,foodY=Integer.MIN_VALUE;
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
            if(x*x+z*z>(radius-y*.45)*(radius-y*.45)||x==0&&z==0||x==0&&z>=1&&z<=radius&&y<2)continue;
            owned.add(worldPosition.offset(x,y,z).asLong());
        }
        setChanged();
    }
    public void makeInterior(int species,int stage){interior=true;work=ColonyRules.THRESHOLDS[stage];level.setBlock(worldPosition,getBlockState().setValue(ColonyCoreBlock.SPECIES,species).setValue(ColonyCoreBlock.GIANT,true).setValue(ColonyCoreBlock.STAGE,stage),3);setChanged();}
    public static void tick(Level l,BlockPos p,BlockState state,ColonyCoreBlockEntity c){
        if(!(l instanceof ServerLevel s))return;if(c.interior)c.expandInterior(s);if(l.getGameTime()%20!=Math.floorMod(p.asLong(),20))return;
        if(!net.caravidro.wayaround.worldconfig.WorldFeatureRuntime.serverEnabled(net.caravidro.wayaround.worldconfig.WorldFeature.LITTLE_LEAF_WORLD))return;
        c.syncAbandonment();c.advance(s.getGameTime());if(c.connections.removeIf(q->loaded(s,BlockPos.of(q))&&!(s.getBlockEntity(BlockPos.of(q)) instanceof ColonyConnectionBlockEntity)))c.setChanged();
        if(c.queenDead){c.decayGarden(s);return;}
        if(!c.interior)c.prepareGarden(s);
        else {ColonyTravel.releaseInvaders(s,c);ColonyTravel.releaseBodies(s,c);}
        if(s.getNearestPlayer(p.getX(),p.getY(),p.getZ(),c.interior?96:48,false)==null)return;
        if((s.getGameTime()/20)%6==0)c.birth(s);
    }
    public void configureInterior(int profile){profile=Math.clamp(profile,0,4);if(profile>interiorProfile){interiorProfile=profile;layoutCursor=0;setChanged();}}
    public int interiorProfile(){return interiorProfile;}
    /** Only observed, loaded gallery cells expand, sharing the ordinary block budget. */
    public void protectInterior(BlockPos p){if(!interior)return;if(protectedInterior.size()>=8192){layoutStopped=true;}else protectedInterior.add(p.asLong());setChanged();}
    private boolean carveInterior(ServerLevel l,BlockPos p){
        if(protectedInterior.contains(p.asLong()))return true;
        var target=net.caravidro.wayaround.littleleaf.world.ColonyLayout.sample(p.getX(),p.getY(),p.getZ(),interiorProfile);
        if(target!=net.caravidro.wayaround.littleleaf.world.ColonyLayout.Material.AIR)return true;
        if(!loaded(l,p)){if(pendingInteriorChunks.size()<64)pendingInteriorChunks.add(net.minecraft.world.level.ChunkPos.asLong(p.getX()>>4,p.getZ()>>4));return true;}
        var state=l.getBlockState(p);if(state.is(Blocks.ROOTED_DIRT)||state.is(Blocks.OAK_WOOD)){if(!ColonyBudget.build(l))return false;l.setBlock(p,Blocks.AIR.defaultBlockState(),18);}return true;
    }
    public void expandInterior(ServerLevel l){
        if(!interior||interiorProfile==0||queenDead||layoutStopped||l.getNearestPlayer(worldPosition.getX(),worldPosition.getY(),worldPosition.getZ(),112,false)==null)return;
        int total=128*128*28,cellX=Math.floorDiv(worldPosition.getX(),128)*128,cellZ=Math.floorDiv(worldPosition.getZ(),128)*128;
        if(layoutCursor<total){
            for(int n=0;n<256&&layoutCursor<total;n++){int i=layoutCursor;var p=new BlockPos(cellX+i%128,32+i/(128*128),cellZ+(i/128)%128);if(!carveInterior(l,p))return;layoutCursor++;}setChanged();return;
        }
        if(repairChunk==Long.MIN_VALUE){for(long key:pendingInteriorChunks){var pos=new net.minecraft.world.level.ChunkPos(key);if(l.getChunkSource().getChunkNow(pos.x,pos.z)!=null){repairChunk=key;repairCursor=0;break;}}}
        if(repairChunk==Long.MIN_VALUE)return;var pos=new net.minecraft.world.level.ChunkPos(repairChunk);if(l.getChunkSource().getChunkNow(pos.x,pos.z)==null)return;
        for(int n=0;n<256&&repairCursor<16*16*28;n++){int i=repairCursor;var p=new BlockPos(pos.getMinBlockX()+i%16,32+i/256,pos.getMinBlockZ()+(i/16)%16);if(!carveInterior(l,p))return;repairCursor++;}
        if(repairCursor>=16*16*28){pendingInteriorChunks.remove(repairChunk);repairChunk=Long.MIN_VALUE;repairCursor=0;}setChanged();
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
        insect.carry(false);if(!insect.enlarged()||giant()||interior)ColonyTransitData.get(l.getServer()).feed(identity(),insect.enlarged()?8:1);ColonyEffects.work(l,insect.blockPosition(),Blocks.OAK_LEAVES.defaultBlockState(),net.minecraft.sounds.SoundEvents.COMPOSTER_FILL_SUCCESS,enlargedVolume(insect));return true;
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
        if(food!=null&&availableFood(l,food))return food;
        food=null;
        for(int i=0;i<48&&ColonyBudget.search(l);i++){
            var offset=FOOD_COLUMNS.get(foodColumn);var column=worldPosition.offset(offset);
            if(!loaded(l,column)){nextFoodColumn();continue;}
            if(foodY==Integer.MIN_VALUE){foodY=Math.min(worldPosition.getY()+32,l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,column.getX(),column.getZ())-1);continue;}
            if(foodY<worldPosition.getY()-2){if(foodCandidate!=null){food=foodCandidate;nextFoodColumn();break;}nextFoodColumn();continue;}
            var p=new BlockPos(column.getX(),foodY--,column.getZ());
            if(availableFood(l,p))foodCandidate=p;
            else if(foodCandidate!=null){food=foodCandidate;nextFoodColumn();break;}
        }
        return food;
    }
    /** Near-ground forage gives a blocked canopy a real, reachable alternative. */
    public BlockPos food(ServerLevel l,ColonyInsectEntity insect){
        var origin=insect.blockPosition();
        for(int n=0;n<12&&ColonyBudget.search(l);n++){
            int ring=n/4+1;var d=Direction.from2DDataValue((n+insect.getId())%4);var q=origin.relative(d,ring);
            for(int y=0;y>=-1;y--){var p=q.offset(0,y,0);if(availableFood(l,p))return p;}
        }
        return food(l);
    }
    private void nextFoodColumn(){foodColumn=(foodColumn+1)%FOOD_COLUMNS.size();foodY=Integer.MIN_VALUE;foodCandidate=null;}
    public void rejectFood(BlockPos p){if(p==null)return;if(rejectedFood.size()>=24)rejectedFood.remove(rejectedFood.keySet().iterator().next());rejectedFood.put(p,level.getGameTime()+1200);if(p.equals(food)){food=null;nextFoodColumn();}}
    private boolean edible(BlockState s){return s.is(BlockTags.LEAVES)||s.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("wayaround","colony_forage")))||(species()==3&&s.is(BlockTags.LOGS));}
    private boolean availableFood(ServerLevel l,BlockPos p){
        if(!loaded(l,p)||!edible(l.getBlockState(p))||!l.getFluidState(p).isEmpty())return false;
        Long until=rejectedFood.get(p);if(until!=null){if(until>l.getGameTime())return false;rejectedFood.remove(p);}
        if(l.getBlockState(p).is(Blocks.GRASS_BLOCK)){var above=p.above();return loaded(l,above)&&l.getFluidState(above).isEmpty()&&l.getBlockState(above).getCollisionShape(l,above).isEmpty();}return true;
    }
    public boolean wetWeather(ServerLevel l){return !interior&&(l.isRaining()||net.caravidro.wayaround.industrial.ship.ShipWind.sample(l,Vec3.atCenterOf(worldPosition)).length()>.036||net.caravidro.wayaround.worldgen.weather.local.WindTestManager.strengthAt(worldPosition.getX(),worldPosition.getZ(),l.getGameTime())>.6)&&l.canSeeSky(worldPosition.above(Math.max(16,ColonyRules.height(stage(),species()==3)+2)));}
    public BlockPos gardenEntry(){return worldPosition.offset(0,-3,1);}
    public BlockPos gardenExit(){return worldPosition.offset(0,0,2);}
    /** Workers use an existing passage; tiny traffic never excavates surrounding soil. */
    public boolean openEntrance(ServerLevel l){
        for(int y=-3;y<=0;y++){
            var p=worldPosition.offset(0,y,1);
            if(!loaded(l,p)||!l.getFluidState(p).isEmpty()||!l.getBlockState(p).getCollisionShape(l,p).isEmpty())return false;
        }
        return gardenPrepared;
    }
    public boolean cut(ServerLevel l,BlockPos p,ColonyInsectEntity insect){
        if(queenDead||insect.carrying()||!loaded(l,p)||!availableFood(l,p)||!insect.getBoundingBox().inflate(insect.enlarged()?.65:.20).intersects(new AABB(p)))return false;
        insect.carry(true); // A cut is a fragment, not a whole disappearing leaf or trunk.
        ColonyEffects.work(l,p,l.getBlockState(p),net.minecraft.sounds.SoundEvents.GRASS_BREAK,enlargedVolume(insect));return true;
    }
    void birth(ServerLevel l){birthAt(l,null);}
    void birthAt(ServerLevel l,BlockPos entrance){
        if(queenDead||!ColonyBudget.birth(l))return;
        var nearby=l.getEntitiesOfClass(ColonyInsectEntity.class,new AABB(worldPosition).inflate(interior?112:48));
        long own=nearby.stream().filter(e->worldPosition.equals(e.home())&&!e.corpse()).count();
        if(own>=capacity()||nearby.stream().filter(e->!e.corpse()).count()>=64)return;
        int role=!queenBorn?2:own%4==0?1:0;
        var e=LittleLeafContent.type(species()).create(l);if(e==null)return;
        e.bind(worldPosition,role,interior,giant());
        var preferred=interior?(role==2?worldPosition.offset(64,0,4):worldPosition.offset(8+(int)(own%3)*3,0,4)):(role==2||giant()?worldPosition.above():gardenExit());
        if(entrance!=null&&role!=2)preferred=entrance;
        boolean founder=role!=2&&foundersBorn<3;
        if(role!=2){if(founder)e.assignJob(foundersBorn==0?ColonyInsectEntity.FORAGER:foundersBorn==1?ColonyInsectEntity.NURSE:giant()?ColonyInsectEntity.BUILDER:ColonyInsectEntity.UNDERTAKER);else {e.makeLarva();e.assignJob((int)(own%4));}if(interior)preferred=nursery().offset((int)own%5-2,0,(int)(own/5)%5-2);}
        if(!placeAtEntrance(l,e,preferred))return;
        if(l.addFreshEntity(e)){if(founder)foundersBorn++;if(role==2){queenBorn=true;queenId=e.getUUID();}setChanged();}
    }
    public void releaseWork(ColonyInsectEntity e){assignments.remove(e.getUUID());}
    public int capacity(){return Math.min(48,ColonyRules.population(stage(),interior)+connections.size()*3);}
    public int connectionCount(){return connections.size();}
    public boolean addConnection(BlockPos p){if(!giant()||interior||queenDead||p.distSqr(worldPosition)>48*48||connections.size()>=8&&!connections.contains(p.asLong()))return false;connections.add(p.asLong());setChanged();return true;}
    public int physicalStage(){return giant()?Math.min(4,Math.min(stage(),owned.size()/32+connections.size()/2)):0;}
    public BlockPos nursery(){return interior?worldPosition.offset(28,0,39):worldPosition.offset(0,0,3);}
    public BlockPos cemetery(){return interior?worldPosition.offset(28,0,-25):gardenEntry().west();}
    public ColonyInsectEntity hungryLarva(ServerLevel l){int n=0;for(var e:l.getEntitiesOfClass(ColonyInsectEntity.class,new AABB(worldPosition).inflate(interior?112:48),e->worldPosition.equals(e.home())&&e.larva()&&e.feeds()<3)){if(n++>=64)break;return e;}return null;}
    public boolean nurse(ServerLevel l,ColonyInsectEntity worker,ColonyInsectEntity child){
        if(queenDead||!worldPosition.equals(child.home())||!child.larva()||!worker.getBoundingBox().inflate(worker.enlarged()?1:.25).intersects(child.getBoundingBox())||!ColonyTransitData.get(l.getServer()).takeFood(identity()))return false;
        child.feed();ColonyEffects.work(l,child.blockPosition(),Blocks.OAK_LEAVES.defaultBlockState(),net.minecraft.sounds.SoundEvents.COMPOSTER_FILL_SUCCESS,worker.enlarged()?.2F:.04F);return true;
    }
    public boolean mature(ServerLevel l,ColonyInsectEntity child){
        if(queenDead||!child.larva()||child.feeds()<3||l.getGameTime()-child.born()<400)return false;var previous=child.position();
        child.grow();if(!placeAtEntrance(l,child,interior?nursery():giant()?worldPosition.above():gardenExit())){child.makeLarva();child.setPos(previous);return false;}
        ColonyEffects.work(l,child.blockPosition(),Blocks.ROOTED_DIRT.defaultBlockState(),net.minecraft.sounds.SoundEvents.COMPOSTER_READY,.15F);return true;
    }
    public boolean bury(ServerLevel l,ColonyInsectEntity worker){
        var body=worker.carriedBody();if(body==null||!body.corpse()||queenDead)return false;
        if(interior){body.releaseBody();body.bury();worker.clearBody();return true;}
        var data=ColonyTransitData.get(l.getServer());if(!data.queueBody(identity(),new ColonyTransitData.Body(body.getUUID(),body.species(),body.caste()))){body.releaseBody();body.bury();worker.clearBody();return true;}
        body.discard();worker.clearBody();ColonyEffects.work(l,worker.blockPosition(),Blocks.ROOTED_DIRT.defaultBlockState(),net.minecraft.sounds.SoundEvents.GRAVEL_PLACE,.12F);return true;
    }
    /** Access plans contain supported real placements, not a generated free staircase. */
    public void requestAccess(ServerLevel l,ColonyInsectEntity worker,BlockPos goal){
        if(!giant()||!worker.enlarged()||interior||queenDead||wetWeather(l)||accessSites.size()>96||goal.distSqr(worldPosition)>48*48)return;
        var start=worker.blockPosition();var d=Direction.getNearest(goal.getX()-worker.getX(),0,goal.getZ()-worker.getZ());if(d.getAxis()==Direction.Axis.Y)d=Direction.EAST;int rise=Math.clamp(goal.getY()-start.getY(),0,6),length=Math.min(12,Math.max(rise+2,(int)Math.sqrt(start.distSqr(goal))));
        for(int step=0;step<=length&&ColonyBudget.search(l);step++){
            var column=start.relative(d,step);if(!loaded(l,column)||!loaded(l,column.below()))break;int level=rise>0?Math.min(rise,step/2):0;
            boolean gap=l.getFluidState(column.below()).isEmpty()&&l.getBlockState(column.below()).getCollisionShape(l,column.below()).isEmpty();
            if(rise==0&&!gap&&l.getFluidState(column.below()).isEmpty()&&step>0)continue;
            for(int y=0;y<=level;y++){var p=column.above(y);if(loaded(l,p)&&l.getBlockState(p).isAir()&&l.getFluidState(p).isEmpty()&&!accessSites.contains(p)&&accessSites.size()<128)accessSites.add(p);}
        }setChanged();
    }
    private BlockPos accessSite(ServerLevel l,ColonyInsectEntity insect){
        for(var p:accessSites){if(!loaded(l,p)||!l.getBlockState(p).isAir()||assignments.containsValue(p))continue;
            boolean supported=!l.getBlockState(p.below()).getCollisionShape(l,p.below()).isEmpty();if(!supported)for(var d:Direction.Plane.HORIZONTAL)if(owned.contains(p.relative(d).asLong())){supported=true;break;}
            if(supported){assignments.put(insect.getUUID(),p);return p;}
        }return null;
    }
    private BlockPos connectionSite(ServerLevel l,ColonyInsectEntity insect){
        if(stage()<1||connections.size()>=8||ColonyTransitData.get(l.getServer()).food(identity())<4)return null;
        if(satelliteSite!=null&&loaded(l,satelliteSite)&&l.getBlockState(satelliteSite).isAir()&&!assignments.containsValue(satelliteSite)){assignments.put(insect.getUUID(),satelliteSite);return satelliteSite;}
        int r=Math.min(32,ColonyRules.radius(stage(),species()==3)+5+connections.size()*3);
        for(var d:Direction.Plane.HORIZONTAL){var col=worldPosition.relative(d,r);if(!loaded(l,col))continue;
            for(int dy=4;dy>=-4&&ColonyBudget.search(l);dy--){var p=col.offset(0,dy,0);if(!l.getBlockState(p).isAir()||!l.getFluidState(p).isEmpty()||!l.getBlockState(p.below()).isFaceSturdy(l,p.below(),Direction.UP))continue;
                boolean close=false;for(long other:connections)if(BlockPos.of(other).distSqr(p)<16){close=true;break;}if(!close){satelliteSite=p;assignments.put(insect.getUUID(),p);return p;}
            }
        }return null;
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
        // Initial colony architecture only. No later forager may excavate an entrance.
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=-3;y<=-2;y++){
            var p=worldPosition.offset(x,y,z);if(!loaded(l,p))return;var s=l.getBlockState(p);
            if(!l.getFluidState(p).isEmpty()||!(s.isAir()||s.is(BlockTags.DIRT)||s.is(LittleLeafContent.COLONY_FUNGUS.get())))return;
        }
        var throat=worldPosition.offset(0,-1,1);
        if(!loaded(l,throat)||!l.getFluidState(throat).isEmpty()||!(l.getBlockState(throat).isAir()||l.getBlockState(throat).is(BlockTags.DIRT)))return;
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=-3;y<=-2;y++){
            var p=worldPosition.offset(x,y,z);if(l.getBlockState(p).is(BlockTags.DIRT))l.setBlock(p,Blocks.AIR.defaultBlockState(),18);
        }
        if(l.getBlockState(throat).is(BlockTags.DIRT))l.setBlock(throat,Blocks.AIR.defaultBlockState(),18);
        var f=worldPosition.offset(1,-3,0);if(l.getBlockState(f).isAir())l.setBlock(f,LittleLeafContent.COLONY_FUNGUS.get().defaultBlockState(),18);
        gardenPrepared=true;setChanged();
    }
    private float enlargedVolume(ColonyInsectEntity insect){return insect.enlarged()?.32F:.06F;}
    /** Blueprints request work; only a carrying insect is allowed to place a block. */
    public BlockPos buildSite(ServerLevel l,ColonyInsectEntity insect){
        if(queenDead||interior||!giant()||!insect.enlarged()||!l.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING))return null;
        var assigned=assignments.get(insect.getUUID());if(assigned!=null&&loaded(l,assigned)&&l.getBlockState(assigned).isAir())return assigned;assignments.remove(insect.getUUID());
        assignments.entrySet().removeIf(e->l.getEntity(e.getKey())==null);
        if(buildStage!=stage()){buildStage=stage();buildCursor=0;setChanged();}
        connections.removeIf(p->loaded(l,BlockPos.of(p))&&!(l.getBlockEntity(BlockPos.of(p)) instanceof ColonyConnectionBlockEntity));
        var access=accessSite(l,insect);if(access!=null)return access;var satellite=connectionSite(l,insect);if(satellite!=null)return satellite;
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
        if(queenDead||interior||!giant()||!insect.enlarged()||!insect.carryingMaterial()||!l.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)||!loaded(l,p)||!l.getBlockState(p).isAir()||!l.getFluidState(p).isEmpty()||!insect.getBoundingBox().inflate(.85).intersects(new AABB(p))||insect.getBoundingBox().intersects(new AABB(p))||!ColonyBudget.build(l))return false;
        var expected=assignments.get(insect.getUUID());if(expected!=null&&!expected.equals(p))return false;
        if(p.distSqr(worldPosition)>48*48)return false;
        var state=insect.material();boolean satellite=p.equals(satelliteSite)&&connections.size()<8&&ColonyTransitData.get(l.getServer()).food(identity())>=4;
        if(satellite)state=LittleLeafContent.CONNECTION.get().defaultBlockState().setValue(ColonyCoreBlock.SPECIES,species());if(!l.setBlock(p,state,3))return false;
        accessSites.remove(p);if(satellite&&l.getBlockEntity(p) instanceof ColonyConnectionBlockEntity link){link.bind(this);for(int n=0;n<4;n++)ColonyTransitData.get(l.getServer()).takeFood(identity());satelliteSite=null;}
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
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);t.putLong("Clock",last);t.putInt("InteriorProfile",interiorProfile);t.putInt("FoundersBorn",foundersBorn);t.putLongArray("PendingInterior",pendingInteriorChunks.stream().mapToLong(Long::longValue).toArray());t.putLongArray("ProtectedInterior",protectedInterior.stream().mapToLong(Long::longValue).toArray());t.putLong("RepairChunk",repairChunk);t.putInt("RepairCursor",repairCursor);t.putBoolean("LayoutStopped",layoutStopped);t.putInt("LayoutCursor",layoutCursor);t.putLongArray("Connections",connections.stream().mapToLong(Long::longValue).toArray());t.putLongArray("Access",accessSites.stream().mapToLong(BlockPos::asLong).toArray());t.putInt("DecayCursor",decayCursor);if(source!=null){t.putString("SourceDimension",source.dimension().location().toString());t.putLong("SourcePos",source.pos().asLong());}t.putLong("Work",work);t.putLong("WorkClock",workClock);t.putBoolean("Habitat",habitat);t.putBoolean("Interior",interior);t.putBoolean("QueenBorn",queenBorn);t.putBoolean("QueenDead",queenDead);t.putBoolean("GardenPrepared",gardenPrepared);if(queenId!=null)t.putUUID("QueenId",queenId);t.putInt("BuildStage",buildStage);t.putInt("BuildCursor",buildCursor);t.putInt("ClearCursor",clearCursor);t.putBoolean("Clearing",clearing);t.putLongArray("Owned",owned.stream().mapToLong(Long::longValue).toArray());var list=new ListTag();for(var id:enemies){var q=new CompoundTag();q.putUUID("Id",id);list.add(q);}t.put("Enemies",list);}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);pendingInteriorChunks.clear();for(long q:t.getLongArray("PendingInterior")){if(pendingInteriorChunks.size()>=64)break;pendingInteriorChunks.add(q);}protectedInterior.clear();for(long q:t.getLongArray("ProtectedInterior")){if(protectedInterior.size()>=8192)break;protectedInterior.add(q);}repairChunk=t.contains("RepairChunk")?t.getLong("RepairChunk"):Long.MIN_VALUE;repairCursor=Math.clamp(t.getInt("RepairCursor"),0,16*16*28);layoutStopped=t.getBoolean("LayoutStopped");foundersBorn=Math.clamp(t.getInt("FoundersBorn"),0,3);interiorProfile=Math.clamp(t.getInt("InteriorProfile"),0,4);layoutCursor=Math.clamp(t.getInt("LayoutCursor"),0,128*128*28);connections.clear();for(long p:t.getLongArray("Connections")){if(connections.size()>=8)break;connections.add(p);}accessSites.clear();for(long p:t.getLongArray("Access")){if(accessSites.size()>=128)break;accessSites.add(BlockPos.of(p));}decayCursor=Math.max(0,t.getInt("DecayCursor"));var sourceId=net.minecraft.resources.ResourceLocation.tryParse(t.getString("SourceDimension"));source=sourceId==null?null:GlobalPos.of(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,sourceId),BlockPos.of(t.getLong("SourcePos")));last=t.contains("Clock")?t.getLong("Clock"):-1;work=Math.max(0,Math.min(4096,t.getLong("Work")));workClock=Math.floorMod(t.getLong("WorkClock"),1200);habitat=!t.contains("Habitat")||t.getBoolean("Habitat");interior=t.getBoolean("Interior");queenBorn=t.getBoolean("QueenBorn");queenDead=t.getBoolean("QueenDead");gardenPrepared=t.getBoolean("GardenPrepared");queenId=t.hasUUID("QueenId")?t.getUUID("QueenId"):null;buildStage=t.contains("BuildStage")?t.getInt("BuildStage"):-1;buildCursor=Math.max(0,t.getInt("BuildCursor"));clearCursor=Math.max(0,t.getInt("ClearCursor"));clearing=t.getBoolean("Clearing");owned.clear();for(long p:t.getLongArray("Owned")){if(owned.size()>=8192)break;owned.add(p);}clearCursor=Math.min(clearCursor,owned.size());enemies.clear();for(var q:t.getList("Enemies",Tag.TAG_COMPOUND)){var c=(CompoundTag)q;if(c.hasUUID("Id")&&enemies.size()<16)enemies.add(c.getUUID("Id"));}}
}
