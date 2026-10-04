package net.caravidro.wayaround.littleleaf;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Small physical mound and diggable fungus chamber; writes stay within the decoration region. */
public final class ColonyMoundFeature extends Feature<NoneFeatureConfiguration> {
    public ColonyMoundFeature(){super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c){
        if(!net.caravidro.wayaround.worldconfig.WorldFeatureRuntime.serverEnabled(net.caravidro.wayaround.worldconfig.WorldFeature.LITTLE_LEAF_WORLD))return false;
        var l=c.level();var p=c.origin();if(!l.ensureCanWrite(p)||!l.getBlockState(p.below()).is(BlockTags.DIRT)||!l.getBlockState(p).canBeReplaced())return false;
        boolean dry=l.getBiome(p).is(BiomeTags.IS_SAVANNA);int species=dry?3:c.random().nextInt(3),radius=species==3?3:2,height=species==3?5:2;
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
        l.setBlock(p.offset(1,-3,0),LittleLeafContent.COLONY_FUNGUS.get().defaultBlockState(),2);
        l.setBlock(p,LittleLeafContent.COLONY_CORE.get().defaultBlockState().setValue(ColonyCoreBlock.SPECIES,species),2);
        if(l.getBlockEntity(p) instanceof ColonyCoreBlockEntity core)core.initializeMound(l.getLevel().getGameTime(),c.random().nextInt(40),radius,height);
        return true;
    }
}
