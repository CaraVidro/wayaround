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
            long before=saplingAge,elapsed=Math.max(0,Math.min(dt,24000));
            saplingAge+=elapsed;
            if(saplingAge>=6000&&grow()&&s.getBlockEntity(worldPosition) instanceof AppleTreeBlockEntity grown)
                grown.advance(Math.max(0,elapsed-Math.max(0,6000-before)));
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
        if(!AppleTreePlacement.place(s,worldPosition,s::hasChunkAt))return false;
        if(s.getBlockEntity(worldPosition) instanceof AppleTreeBlockEntity grown){grown.lastSample=s.getGameTime();grown.advance(0);}
        return true;
    }
    public void initializeWild(LevelAccessor world,long now,long seed){
        lastSample=now;
        for(int i=0;i<FRUIT.length;i++){
            BlockPos p=worldPosition.offset(FRUIT[i][0],FRUIT[i][1],FRUIT[i][2]);
            ages[i]=Math.floorMod(net.caravidro.wayaround.ecology.EcologicalHistory.mix(seed^p.asLong()),16000);
            if(i%4==0)ages[i]=8000+ages[i]%4001;
            ages[i]=Math.min(12000,ages[i]);stages[i]=NatureMath.fruitStage(ages[i]);
            var leaf=world.getBlockState(p);
            if(leaf.is(NatureContent.APPLE_LEAVES.get()))world.setBlock(p,leaf.setValue(AppleLeavesBlock.FRUIT,stages[i]),2);
        }
        setChanged();
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
