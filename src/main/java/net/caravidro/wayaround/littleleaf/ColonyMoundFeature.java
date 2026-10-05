package net.caravidro.wayaround.littleleaf;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Small physical mound and diggable fungus chamber; writes stay within the decoration region. */
public final class ColonyMoundFeature extends Feature<NoneFeatureConfiguration> {
    public ColonyMoundFeature(){super(NoneFeatureConfiguration.CODEC);}
    private static int preferredSpecies(
            int z,
            boolean dry,
            net.minecraft.util.RandomSource random
    ) {
        String[] ids={
                "wayaround:black_ant",
                "wayaround:red_ant",
                "wayaround:honey_ant",
                "wayaround:termite"
        };

        double[] weights=new double[ids.length];
        double total=0;

        for(int i=0;i<ids.length;i++){
            double suitability=net.caravidro.wayaround.ecology.AnimalClimateProfile.suitability(ids[i],z);
            double biome=i==3?(dry?2.8:.24):(dry?.72:1.0);
            weights[i]=Math.max(.001,suitability*biome);
            total+=weights[i];
        }

        double roll=random.nextDouble()*total;
        for(int i=0;i<weights.length;i++){
            roll-=weights[i];
            if(roll<=0)return i;
        }
        return 0;
    }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c){
        if(!net.caravidro.wayaround.worldconfig.WorldFeatureRuntime.serverEnabled(net.caravidro.wayaround.worldconfig.WorldFeature.LITTLE_LEAF_WORLD))return false;
        var l=c.level();var p=c.origin();if(!l.ensureCanWrite(p)||!l.getBlockState(p.below()).is(BlockTags.DIRT)||!l.getBlockState(p).canBeReplaced())return false;
        boolean dry=l.getBiome(p).is(BiomeTags.IS_SAVANNA);
        int species=preferredSpecies(p.getZ(),dry,c.random());
        int radius=species==3?3:2,height=species==3?5:2;
        // Validate the complete small footprint before touching terrain.
        for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)for(int y=-4;y<=height;y++){
            var q=p.offset(x,y,z);if(!l.ensureCanWrite(q)||!l.getFluidState(q).isEmpty())return false;
            if(y>=0&&!l.getBlockState(q).canBeReplaced())return false;
        }
        for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)for(int y=0;y<height;y++){
            if(x*x+z*z>(radius-y*.45)*(radius-y*.45)||x==0&&z==0)continue;
            l.setBlock(p.offset(x,y,z),Blocks.DIRT.defaultBlockState(),2);
        }
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=-3;y<=-2;y++)l.setBlock(p.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);
        for(int y=-2;y<=1;y++)l.setBlock(p.offset(0,y,1),Blocks.AIR.defaultBlockState(),2);
        // A real ground-level door connects the chamber to the outside even
        // under the five-block termite roof. Small workers never dig this later.
        for(int z=1;z<=radius;z++)for(int y=0;y<=1;y++)l.setBlock(p.offset(0,y,z),Blocks.AIR.defaultBlockState(),2);
        l.setBlock(p.offset(1,-3,0),LittleLeafContent.COLONY_FUNGUS.get().defaultBlockState(),2);
        l.setBlock(p,LittleLeafContent.core(species).defaultBlockState(),2);
        if(l.getBlockEntity(p) instanceof ColonyCoreBlockEntity core)core.initializeMound(l.getLevel().getGameTime(),c.random().nextInt(40),radius,height);
        return true;
    }
}
