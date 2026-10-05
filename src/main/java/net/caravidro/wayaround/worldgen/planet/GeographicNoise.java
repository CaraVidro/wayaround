package net.caravidro.wayaround.worldgen.planet;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.*;

/** Transform immediate noise leaves only; cache/interpolation markers remain in the vanilla graph. */
public record GeographicNoise(DensityFunction input,int scale,boolean periodic,boolean temperature) implements DensityFunction {
    public static final MapCodec<GeographicNoise> DATA_CODEC=RecordCodecBuilder.mapCodec(i->i.group(
        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(GeographicNoise::input),
        com.mojang.serialization.Codec.INT.fieldOf("scale").forGetter(GeographicNoise::scale),
        com.mojang.serialization.Codec.BOOL.fieldOf("periodic").forGetter(GeographicNoise::periodic),
        com.mojang.serialization.Codec.BOOL.fieldOf("temperature").forGetter(GeographicNoise::temperature)
    ).apply(i,GeographicNoise::new));
    public static final KeyDispatchDataCodec<GeographicNoise> CODEC=KeyDispatchDataCodec.of(DATA_CODEC);
    private static final ThreadLocal<Context> CONTEXT=ThreadLocal.withInitial(Context::new);
    private static final class Context implements FunctionContext {
        int x,y,z;public int blockX(){return x;}public int blockY(){return y;}public int blockZ(){return z;}
    }
    @Override public double compute(FunctionContext c) {
        int x=periodic?PlanetMath.wrap(c.blockX()):c.blockX(),z=periodic?PlanetMath.wrap(c.blockZ()):c.blockZ();
        double wx=periodic?.5*PlanetMath.smooth((Math.abs((long)x)-(PlanetMath.HALF-1024.0))/1024):0;
        double wz=periodic?.5*PlanetMath.smooth((Math.abs((long)z)-(PlanetMath.HALF-1024.0))/1024):0;
        double value=sample(x,c.blockY(),z);
        int ax=x<0?x+PlanetMath.SIZE:x-PlanetMath.SIZE,az=z<0?z+PlanetMath.SIZE:z-PlanetMath.SIZE;
        if(wx>0)value=value*(1-wx)+sample(ax,c.blockY(),z)*wx;
        if(wz>0) {
            double alternate=sample(x,c.blockY(),az);
            if(wx>0)alternate=alternate*(1-wx)+sample(ax,c.blockY(),az)*wx;
            value=value*(1-wz)+alternate*wz;
        }
        if(temperature&&periodic) { double latitude=PlanetMath.latitude(z);value=Math.max(-1,Math.min(1,value*.8+.18-.9*latitude*latitude)); }
        return value;
    }
    private double sample(int x,int y,int z) {
        Context s=CONTEXT.get();int px=s.x,py=s.y,pz=s.z;
        s.x=Math.floorDiv(x,scale);s.y=y;s.z=Math.floorDiv(z,scale);
        try{return input.compute(s);}finally{s.x=px;s.y=py;s.z=pz;}
    }
    @Override public void fillArray(double[] a,ContextProvider p){p.fillAllDirectly(a,this);}
    @Override public DensityFunction mapAll(Visitor v){return v.apply(new GeographicNoise(input.mapAll(v),scale,periodic,temperature));}
    @Override public double minValue(){return temperature?-1:input.minValue();}
    @Override public double maxValue(){return temperature?1:input.maxValue();}
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec(){return CODEC;}
    public static NoiseRouter transform(NoiseRouter router,boolean large,boolean finite) {
        return router.mapAll(new Visitor(){public DensityFunction apply(DensityFunction f){
            if(!(f instanceof net.caravidro.wayaround.mixin.ContinentalNoiseAccessor leaf))return f;
            String key=leaf.wayaround$noise().noiseData().unwrapKey().map(k->k.location().getPath()).orElse("");
            int scale=large?(key.startsWith("continentalness")?6:key.startsWith("temperature")||key.startsWith("vegetation")?3:key.startsWith("erosion")?4:key.startsWith("ridge")?3:1):1;
            return scale>1||finite?new GeographicNoise(f,scale,finite,key.startsWith("temperature")):f;
        }});
    }
}
