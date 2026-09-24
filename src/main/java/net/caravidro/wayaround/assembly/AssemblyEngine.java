package net.caravidro.wayaround.assembly;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class AssemblyEngine {
    public static final int MAX_PARTS = 3;

    private AssemblyEngine() {}

    public static boolean insert(AssemblyState state, AssemblyPart part) {
        if (state.size() >= MAX_PARTS || state.has(part.kind())) return false;
        state.add(part);
        return true;
    }

    public static AssemblyPart hammer(AssemblyState state, RandomSource random) {
        AssemblyPart target = state.weakestPart();
        if (target == null) return null;

        float before = target.assemblyScore();
        target.applyHammer(random);
        state.recordOperation(target.assemblyScore() + 0.001F < before);
        return target;
    }

    public static AssemblyPart rotateLast(AssemblyState state) {
        AssemblyPart target = state.last();
        if (target == null) return null;
        target.rotate90();
        state.recordOperation(false);
        return target;
    }

    public static boolean canFinalizePrimitiveAxe(AssemblyState state) {
        return state.has(AssemblyPart.Kind.HEAD)
                && state.has(AssemblyPart.Kind.HANDLE)
                && state.has(AssemblyPart.Kind.BINDING)
                && state.operations() >= 2
                && state.averageAlignment() >= 0.60F
                && state.averageTension() >= 0.31F;
    }

    public static int primitiveAxeDurability(AssemblyState state) {
        float structural = state.durabilityScore();
        float assembly = state.overallQuality();
        return Mth.clamp(
                Math.round(64.0F + 156.0F * (structural * 0.60F + assembly * 0.40F)),
                64,
                220
        );
    }
}
