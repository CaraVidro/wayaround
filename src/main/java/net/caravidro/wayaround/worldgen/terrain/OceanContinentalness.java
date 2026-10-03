package net.caravidro.wayaround.worldgen.terrain;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.*;

/** Fourfold horizontal continental scale; leaf sampling retains density caches/interpolation. */
public record OceanContinentalness(DensityFunction input) implements DensityFunction {
    public static final MapCodec<OceanContinentalness> DATA_CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(OceanContinentalness::input)).apply(i,OceanContinentalness::new));
    public static final KeyDispatchDataCodec<OceanContinentalness> CODEC=KeyDispatchDataCodec.of(DATA_CODEC);
    @Override public double compute(FunctionContext c) { return input.compute(new SinglePointContext(Math.floorDiv(c.blockX(),4),c.blockY(),Math.floorDiv(c.blockZ(),4))); }
    @Override public void fillArray(double[] values,ContextProvider provider) { provider.fillAllDirectly(values,this); }
    @Override public DensityFunction mapAll(Visitor visitor) { return visitor.apply(new OceanContinentalness(input.mapAll(visitor))); }
    @Override public double minValue() { return input.minValue(); }
    @Override public double maxValue() { return input.maxValue(); }
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }
    public static NoiseRouter scale(NoiseRouter router) {
        return router.mapAll(new Visitor() {
            @Override public DensityFunction apply(DensityFunction f) {
                DensityFunction.NoiseHolder noise=null;
                if(f instanceof DensityFunctions.Noise n) noise=n.noise();
                else if(f instanceof DensityFunctions.ShiftedNoise n) noise=n.noise();
                if(noise!=null && noise.noiseData().unwrapKey().map(k->k.location().getPath().startsWith("continentalness")).orElse(false)) return new OceanContinentalness(f);
                return f;
            }
        });
    }
}
