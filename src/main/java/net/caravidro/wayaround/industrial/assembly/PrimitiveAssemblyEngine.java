package net.caravidro.wayaround.industrial.assembly;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class PrimitiveAssemblyEngine {
    public static final int MAX_PARTS = 3;

    private PrimitiveAssemblyEngine() {}

    public static boolean insert(PrimitiveAssemblyState state, AssemblyPartProfile part) {
        if (state.size() >= MAX_PARTS || state.has(part.kind())) return false;
        state.add(part);
        return true;
    }

    public static AssemblyPartProfile hammer(PrimitiveAssemblyState state, RandomSource random) {
        AssemblyPartProfile target = state.weakestPart();
        if (target == null) return null;

        float before = target.assemblyScore();
        target.applyHammer(random);
        state.recordOperation(target.assemblyScore() + 0.001F < before);
        return target;
    }

    public static AssemblyPartProfile rotateLast(PrimitiveAssemblyState state) {
        AssemblyPartProfile target = state.last();
        if (target == null) return null;
        target.rotate90();
        state.recordOperation(false);
        return target;
    }

    public static boolean canFinalizePrimitiveAxe(PrimitiveAssemblyState state) {
        return state.has(AssemblyPartProfile.Kind.HEAD)
                && state.has(AssemblyPartProfile.Kind.HANDLE)
                && state.has(AssemblyPartProfile.Kind.BINDING)
                && state.operations() >= 2
                && state.averageAlignment() >= 0.60F
                && state.averageTension() >= 0.31F;
    }

    public static int primitiveAxeDurability(PrimitiveAssemblyState state) {
        float structural = state.durabilityScore();
        float assembly = state.overallQuality();
        return Mth.clamp(
                Math.round(64.0F + 156.0F * (structural * 0.60F + assembly * 0.40F)),
                64,
                220
        );
    }
}
