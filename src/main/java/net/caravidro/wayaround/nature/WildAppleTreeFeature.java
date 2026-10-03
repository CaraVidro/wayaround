package net.caravidro.wayaround.nature;

import net.caravidro.wayaround.ecology.EcologicalHistory;
import net.caravidro.wayaround.worldconfig.*;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Wild trees have an existing fruit history; planted saplings still start young. */
public final class WildAppleTreeFeature extends Feature<NoneFeatureConfiguration> {
    public WildAppleTreeFeature(){super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c){
        if(!WorldFeatureRuntime.serverEnabled(WorldFeature.LIVING_VEGETATION))return false;
        var l=c.level();var p=c.origin();
        if(!l.ensureCanWrite(p)||!NatureContent.APPLE_SAPLING.get().defaultBlockState().canSurvive(l,p))return false;
        long hash=EcologicalHistory.mix(l.getSeed()^p.asLong());
        if(Math.floorMod(hash,8)==0){
            if(!l.getBlockState(p).canBeReplaced())return false;
            return l.setBlock(p,NatureContent.APPLE_SAPLING.get().defaultBlockState(),2);
        }
        if(!AppleTreePlacement.place(l,p,l::ensureCanWrite))return false;
        if(l.getBlockEntity(p) instanceof AppleTreeBlockEntity tree)tree.initializeWild(l,l.getLevel().getGameTime(),l.getSeed());
        return true;
    }
}
