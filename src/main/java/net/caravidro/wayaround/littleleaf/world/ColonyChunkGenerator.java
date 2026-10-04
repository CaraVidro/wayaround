package net.caravidro.wayaround.littleleaf.world;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.caravidro.wayaround.littleleaf.LittleLeafContent;
import net.minecraft.core.*;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;

public final class ColonyChunkGenerator extends ChunkGenerator {
    public static final MapCodec<ColonyChunkGenerator> CODEC=RecordCodecBuilder.mapCodec(i->i.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter(ColonyChunkGenerator::getBiomeSource)).apply(i,ColonyChunkGenerator::new));
    public ColonyChunkGenerator(BiomeSource source){super(source);}
    @Override protected MapCodec<? extends ChunkGenerator> codec(){return CODEC;}
    public static BlockState state(ColonyLayout.Material m){return switch(m){case AIR->Blocks.AIR.defaultBlockState();case BEDROCK->Blocks.BEDROCK.defaultBlockState();case SOIL->Blocks.ROOTED_DIRT.defaultBlockState();case ROOT->Blocks.OAK_WOOD.defaultBlockState();case STEM->Blocks.MUSHROOM_STEM.defaultBlockState();case CAP->Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState();case LEAF->Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true);case FUNGUS->LittleLeafContent.COLONY_FUNGUS.get().defaultBlockState();case LIGHT->Blocks.SHROOMLIGHT.defaultBlockState();case EXIT->LittleLeafContent.COLONY_EXIT.get().defaultBlockState();};}
    @Override public CompletableFuture<ChunkAccess> fillFromNoise(Blender b,RandomState r,StructureManager s,ChunkAccess chunk){
        var p=new BlockPos.MutableBlockPos();
        for(int x=chunk.getPos().getMinBlockX();x<=chunk.getPos().getMaxBlockX();x++)for(int z=chunk.getPos().getMinBlockZ();z<=chunk.getPos().getMaxBlockZ();z++)for(int y=0;y<ColonyLayout.HEIGHT;y++){
            var material=ColonyLayout.sample(x,y,z);if(material==ColonyLayout.Material.AIR)continue;p.set(x,y,z);chunk.setBlockState(p,state(material),false);
        }
        Heightmap.primeHeightmaps(chunk,java.util.EnumSet.of(Heightmap.Types.WORLD_SURFACE_WG,Heightmap.Types.OCEAN_FLOOR_WG));return CompletableFuture.completedFuture(chunk);
    }
    @Override public void applyCarvers(WorldGenRegion l,long seed,RandomState r,BiomeManager b,StructureManager s,ChunkAccess c,GenerationStep.Carving step){}
    @Override public void buildSurface(WorldGenRegion l,StructureManager s,RandomState r,ChunkAccess c){}
    @Override public void spawnOriginalMobs(WorldGenRegion l){}
    @Override public int getGenDepth(){return ColonyLayout.HEIGHT;}
    @Override public int getSeaLevel(){return 0;}
    @Override public int getMinY(){return 0;}
    @Override public int getBaseHeight(int x,int z,Heightmap.Types t,LevelHeightAccessor h,RandomState r){return ColonyLayout.HEIGHT;}
    @Override public NoiseColumn getBaseColumn(int x,int z,LevelHeightAccessor h,RandomState r){BlockState[] states=new BlockState[h.getHeight()];for(int i=0;i<states.length;i++)states[i]=state(ColonyLayout.sample(x,i+h.getMinBuildHeight(),z));return new NoiseColumn(h.getMinBuildHeight(),states);}
    @Override public void addDebugScreenInfo(List<String> l,RandomState r,BlockPos p){l.add("Little Leaf World: subterranean colony");}
}
