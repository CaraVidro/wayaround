package net.caravidro.wayaround.nexus.world;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.caravidro.wayaround.media.*;
import net.caravidro.wayaround.nexus.NexusContent;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;

/** Chunk-local concrete/rock generator. Room decisions depend on coordinates, never generation order. */
public final class NexusChunkGenerator extends ChunkGenerator {
    public static final MapCodec<NexusChunkGenerator> CODEC=RecordCodecBuilder.mapCodec(instance->instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(NexusChunkGenerator::getBiomeSource)).apply(instance,NexusChunkGenerator::new));
    private static final ResourceLocation RANDOM=ResourceLocation.fromNamespaceAndPath("wayaround","nexus_layout");
    public NexusChunkGenerator(BiomeSource source){super(source);}
    public static long layoutSeed(RandomState random){return random.getOrCreateRandomFactory(RANDOM).at(0,0,0).nextLong();}
    @Override protected MapCodec<? extends ChunkGenerator> codec(){return CODEC;}
    @Override public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender,RandomState random,StructureManager structures,ChunkAccess chunk) {
        long seed=layoutSeed(random);var p=new BlockPos.MutableBlockPos();
        for(int x=chunk.getPos().getMinBlockX();x<=chunk.getPos().getMaxBlockX();x++)for(int z=chunk.getPos().getMinBlockZ();z<=chunk.getPos().getMaxBlockZ();z++) {
            var column=NexusLayout.column(seed,x,z);
            for(int y=0;y<=column.top();y++) {
                var material=column.sample(y);if(material==NexusLayout.Material.AIR)continue;
                p.set(x,y,z);BlockState state=state(material);chunk.setBlockState(p,state,false);
                if(state.hasBlockEntity()) {
                    CompoundTag tag=new CompoundTag();tag.putString("id",material==NexusLayout.Material.CHAIR?"wayaround:wooden_chair":"wayaround:television");
                    tag.putInt("x",x);tag.putInt("y",y);tag.putInt("z",z);chunk.setBlockEntityNbt(tag);
                }
            }
        }
        Heightmap.primeHeightmaps(chunk,java.util.EnumSet.of(Heightmap.Types.WORLD_SURFACE_WG,Heightmap.Types.OCEAN_FLOOR_WG));
        return CompletableFuture.completedFuture(chunk);
    }
    public static BlockState state(NexusLayout.Material m){return switch(m){
        case AIR->Blocks.AIR.defaultBlockState();case BEDROCK->Blocks.BEDROCK.defaultBlockState();
        case ROCK->Blocks.STONE.defaultBlockState();case ANDESITE->Blocks.POLISHED_ANDESITE.defaultBlockState();
        case CONCRETE->Blocks.GRAY_CONCRETE.defaultBlockState();case PALE_CONCRETE->Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        case RED_CONCRETE->Blocks.RED_CONCRETE.defaultBlockState();case LIGHT->Blocks.SEA_LANTERN.defaultBlockState();
        case CHAIR->MediaContent.WOODEN_CHAIR.get().defaultBlockState().setValue(WoodenChairBlock.FACING,Direction.NORTH);
        case TV->MediaContent.TELEVISION.get().defaultBlockState().setValue(TelevisionBlock.FACING,Direction.SOUTH);
        case CONTROL->NexusContent.NEXUS_CONTROL.get().defaultBlockState();case LOG->Blocks.OAK_LOG.defaultBlockState();
        case LEAVES->Blocks.OAK_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT,true);
    };}
    @Override public void applyCarvers(WorldGenRegion region,long seed,RandomState random,BiomeManager biomes,StructureManager structures,ChunkAccess chunk,GenerationStep.Carving step) {}
    @Override public void buildSurface(WorldGenRegion region,StructureManager structures,RandomState random,ChunkAccess chunk) {}
    @Override public void spawnOriginalMobs(WorldGenRegion region) {
        var pos=region.getCenter();NaturalSpawner.spawnMobsForChunkGeneration(region,region.getBiome(pos.getWorldPosition().atY(NexusLayout.FLOOR)),pos,
                RandomSource.create(NexusLayout.hash(region.getSeed(),pos.x,pos.z)));
    }
    @Override public int getGenDepth(){return NexusLayout.HEIGHT;}
    @Override public int getSeaLevel(){return 0;}
    @Override public int getMinY(){return 0;}
    @Override public int getBaseHeight(int x,int z,Heightmap.Types type,LevelHeightAccessor height,RandomState random) {
        var c=NexusLayout.column(layoutSeed(random),x,z);
        for(int y=Math.min(c.top(),height.getMaxBuildHeight()-1);y>=height.getMinBuildHeight();y--)
            if(type.isOpaque().test(state(c.sample(y))))return y+1;
        return height.getMinBuildHeight();
    }
    @Override public NoiseColumn getBaseColumn(int x,int z,LevelHeightAccessor height,RandomState random) {
        var c=NexusLayout.column(layoutSeed(random),x,z);BlockState[] states=new BlockState[height.getHeight()];
        for(int i=0;i<states.length;i++)states[i]=state(c.sample(height.getMinBuildHeight()+i));return new NoiseColumn(height.getMinBuildHeight(),states);
    }
    @Override public void addDebugScreenInfo(List<String> lines,RandomState random,BlockPos pos){lines.add("Nexus: fractured concrete complex");}
}
