package net.caravidro.wayaround.nature;

import java.util.*;
import net.caravidro.wayaround.time.TimeAgingEngine;
import net.caravidro.wayaround.worldconfig.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** One clock per tree, twelve fruit sites, no ticking entities for apples. */
public final class AppleTreeBlockEntity extends BlockEntity {
    private static final Map<ServerLevel,Set<AppleTreeBlockEntity>> ACTIVE=new WeakHashMap<>();
    private static final int[][] FRUIT={{2,3,-1},{2,3,0},{2,3,1},{-2,3,-1},{-2,3,0},{-2,3,1},{-1,3,2},{0,3,2},{1,3,2},{-1,3,-2},{0,3,-2},{1,3,-2}};
    private long lastSample=-1,saplingAge;
    private long[] ages=new long[12];
    private int[] stages=new int[12];
    public AppleTreeBlockEntity(BlockPos p,BlockState s){super(NatureContent.APPLE_TREE_ENTITY.get(),p,s);}
    @Override public void onLoad(){super.onLoad();if(level instanceof ServerLevel s)ACTIVE.computeIfAbsent(s,k->new HashSet<>()).add(this);}
    @Override public void setRemoved(){if(level instanceof ServerLevel s){var set=ACTIVE.get(s);if(set!=null)set.remove(this);}super.setRemoved();}
    public static void tick(Level l,BlockPos p,BlockState state,AppleTreeBlockEntity tree){
        if(l.getGameTime()%80!=Math.floorMod(p.asLong(),80))return;
        long now=l.getGameTime(),dt=tree.lastSample<0?0:Math.max(0,now-tree.lastSample);tree.lastSample=now;
        if(state.is(NatureContent.APPLE_SAPLING.get())&&!state.canSurvive(l,p)){l.destroyBlock(p,true);return;}
        if(WorldFeatureRuntime.serverEnabled(WorldFeature.LIVING_VEGETATION))tree.advance(dt);
    }
    public void advance(long dt){
        if(!(level instanceof ServerLevel s)||isRemoved())return;
        TimeAgingEngine.sampleWorldSurface(s,worldPosition,false);
        if(getBlockState().is(NatureContent.APPLE_SAPLING.get())){
            saplingAge+=Math.max(0,Math.min(dt,24000));
            if(saplingAge>=6000)grow();
        }else{
            for(int i=0;i<FRUIT.length;i++){
                BlockPos p=worldPosition.offset(FRUIT[i][0],FRUIT[i][1],FRUIT[i][2]);
                if(!s.hasChunkAt(p))continue;
                BlockState leaf=s.getBlockState(p);
                if(!leaf.is(NatureContent.APPLE_LEAVES.get()))continue;
                int visible=leaf.getValue(AppleLeavesBlock.FRUIT);
                if(stages[i]==3&&visible==0){ages[i]=-4000;stages[i]=0;continue;}
                ages[i]=NatureMath.fruitAge(ages[i],dt);
                int stage=NatureMath.fruitStage(ages[i]);stages[i]=stage;
                if(visible!=stage)s.setBlock(p,leaf.setValue(AppleLeavesBlock.FRUIT,stage),3);
            }
        }
        setChanged();
    }
    public boolean grow(){
        if(!(level instanceof ServerLevel s))return false;
        // Validate the entire footprint before placing anything; no chunk loads.
        for(int x=-2;x<=2;x++)for(int y=0;y<=5;y++)for(int z=-2;z<=2;z++){
            BlockPos p=worldPosition.offset(x,y,z);
            if(!s.hasChunkAt(p))return false;
            BlockState b=s.getBlockState(p);
            if(!p.equals(worldPosition)&&!b.isAir()&&!b.is(BlockTags.LEAVES)&&!b.canBeReplaced())return false;
        }
        for(int y=1;y<=3;y++)s.setBlock(worldPosition.above(y),Blocks.OAK_LOG.defaultBlockState(),3);
        for(int y=3;y<=5;y++)for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
            if(x==0&&z==0&&y==3||Math.abs(x)==2&&Math.abs(z)==2||y==5&&(Math.abs(x)>1||Math.abs(z)>1))continue;
            s.setBlock(worldPosition.offset(x,y,z),NatureContent.APPLE_LEAVES.get().defaultBlockState().setValue(LeavesBlock.DISTANCE,1),3);
        }
        s.setBlock(worldPosition,NatureContent.APPLE_LOG.get().defaultBlockState(),3);
        if(s.getBlockEntity(worldPosition) instanceof AppleTreeBlockEntity grown){grown.lastSample=s.getGameTime();grown.advance(0);}
        return true;
    }
    public static void debugPulse(ServerLevel l,long dt){
        var set=ACTIVE.get(l);if(set==null)return;int budget=128;
        for(var tree:new ArrayList<>(set)){
            if(budget--<=0)break;
            if(!tree.isRemoved()&&l.getNearestPlayer(tree.worldPosition.getX(),tree.worldPosition.getY(),tree.worldPosition.getZ(),64,false)!=null)tree.advance(dt);
        }
    }
    public static void clear(){ACTIVE.clear();}
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);t.putLong("LastSample",lastSample);t.putLong("SaplingAge",saplingAge);t.putLongArray("FruitAges",ages);t.putIntArray("FruitStages",stages);}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);lastSample=t.contains("LastSample")?t.getLong("LastSample"):-1;saplingAge=Math.max(0,t.getLong("SaplingAge"));long[] a=t.getLongArray("FruitAges");int[] b=t.getIntArray("FruitStages");for(int i=0;i<12;i++){ages[i]=i<a.length?Math.max(-4000,Math.min(12000,a[i])):0;stages[i]=i<b.length?Math.max(0,Math.min(3,b[i])):0;}}
}
