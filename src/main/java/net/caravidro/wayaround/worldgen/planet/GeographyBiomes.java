package net.caravidro.wayaround.worldgen.planet;

import java.util.*;
import net.caravidro.wayaround.worldconfig.*;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;

/** Source-scoped lookup: preserve arbitrary biome additions; only adjust vanilla high elevation surfaces. */
public final class GeographyBiomes {
    private static final Map<BiomeSource,Map<String,Holder<Biome>>> SOURCES=Collections.synchronizedMap(new WeakHashMap<>());
    private static Map<String,Holder<Biome>> biomes(BiomeSource s) {
        return SOURCES.computeIfAbsent(s,source->{var map=new HashMap<String,Holder<Biome>>();for(var b:source.possibleBiomes())b.unwrapKey().ifPresent(k->map.put(k.location().toString(),b));return map;});
    }
    public static boolean overworld(BiomeSource source) { var m=biomes(source);return m.containsKey("minecraft:plains")||m.containsKey("minecraft:forest"); }
    public static Holder<Biome> elevation(BiomeSource source,Holder<Biome> original,int x,int y,int z,Climate.Sampler sampler) {
        if(!WorldFeatureRuntime.serverEnabled(WorldFeature.LARGE_GEOGRAPHY)||!overworld(source)||y<16)return original;
        var key=original.unwrapKey();if(key.isEmpty()||!key.get().location().getNamespace().equals("minecraft"))return original;
        var climate=sampler.sample(x,y,z);
        double h=PlanetMath.height(Climate.unquantizeCoord(climate.continentalness()),Climate.unquantizeCoord(climate.erosion()),Climate.unquantizeCoord(climate.weirdness()));
        double t=Climate.unquantizeCoord(climate.temperature());
        String name=h>220?(t<.2?"frozen_peaks":"stony_peaks"):h>165?(t<.1?"snowy_slopes":"meadow"):null;
        return name==null?original:biomes(source).getOrDefault("minecraft:"+name,original);
    }
    private GeographyBiomes() {}
}
