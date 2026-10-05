package net.caravidro.wayaround.worldgen.planet;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.*;

/** Coherent preliminary/final surfaces. Deep vanilla noise caves and ore routing remain intact. */
public record RegionalTerrainDensity(DensityFunction input,DensityFunction continent,DensityFunction erosion,DensityFunction ridge,boolean preliminary) implements DensityFunction {
    public static final MapCodec<RegionalTerrainDensity> DATA_CODEC=RecordCodecBuilder.mapCodec(i->i.group(
        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(RegionalTerrainDensity::input),
        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("continent").forGetter(RegionalTerrainDensity::continent),
        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("erosion").forGetter(RegionalTerrainDensity::erosion),
        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("ridge").forGetter(RegionalTerrainDensity::ridge),
        com.mojang.serialization.Codec.BOOL.fieldOf("preliminary").forGetter(RegionalTerrainDensity::preliminary)
    ).apply(i,RegionalTerrainDensity::new));
    public static final KeyDispatchDataCodec<RegionalTerrainDensity> CODEC=KeyDispatchDataCodec.of(DATA_CODEC);
    // Per-worker, per-function cache, bounded to 64 columns; no world/seed global mutable cache.
    private static final ThreadLocal<Columns[]> COLUMNS=ThreadLocal.withInitial(()->new Columns[]{new Columns(),new Columns()});
    private static final class Point implements FunctionContext {int x,y,z;public int blockX(){return x;}public int blockY(){return y;}public int blockZ(){return z;}}
    private static final class Columns { final Point point=new Point();RegionalTerrainDensity owner;long[] keys=new long[64];double[] heights=new double[64];boolean[] valid=new boolean[64]; }
    @Override public double compute(FunctionContext c) {
        Columns cache=COLUMNS.get()[preliminary?1:0];if(cache.owner!=this){cache.owner=this;java.util.Arrays.fill(cache.valid,false);}
        long key=((long)c.blockX()<<32)^(c.blockZ()&0xffffffffL);int index=(int)(key^(key>>>32)*31)&63;
        double height;
        if(cache.valid[index]&&cache.keys[index]==key)height=cache.heights[index];
        else {height=surface(c,cache.point);cache.valid[index]=true;cache.keys[index]=key;cache.heights[index]=height;}
        double terrain=(height-c.blockY())/48;
        if(preliminary)return terrain;
        if(c.blockY()>=54)return terrain;
        double vanilla=input.compute(c);
        // Keep noise caves below Y=30; interpolate back into new landforms through Y=54.
        double cave=Math.min(terrain,vanilla),blend=PlanetMath.smooth((c.blockY()-30)/24.0);
        return cave+(terrain-cave)*blend;
    }
    private double surface(FunctionContext c,Point p) {
        // Sample outside NoiseChunk's quart-nearest cache, then interpolate four climate columns.
        // The scalar noise inputs stay seeded; no nested NoiseInterpolator or remote chunk access.
        int x=Math.floorDiv(c.blockX(),4)*4,z=Math.floorDiv(c.blockZ(),4)*4;double tx=(c.blockX()-x)/4.0,tz=(c.blockZ()-z)/4.0;
        p.y=64;p.x=x;p.z=z;double a=height(p);p.x=x+4;double b=height(p);p.z=z+4;double d=height(p);p.x=x;double e=height(p);
        return (a+(b-a)*tx)*(1-tz)+(e+(d-e)*tx)*tz;
    }
    private double height(FunctionContext p){return PlanetMath.height(continent.compute(p),erosion.compute(p),ridge.compute(p));}
    @Override public void fillArray(double[] a,ContextProvider p){p.fillAllDirectly(a,this);}
    @Override public DensityFunction mapAll(Visitor v){return v.apply(new RegionalTerrainDensity(input.mapAll(v),continent.mapAll(v),erosion.mapAll(v),ridge.mapAll(v),preliminary));}
    @Override public double minValue(){return Double.NEGATIVE_INFINITY;}
    @Override public double maxValue(){return Double.POSITIVE_INFINITY;}
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec(){return CODEC;}
}
