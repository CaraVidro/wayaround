package net.caravidro.wayaround.mixin;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Read-only access to the two vanilla continental noise leaf records. */
@Mixin(targets={"net.minecraft.world.level.levelgen.DensityFunctions$Noise","net.minecraft.world.level.levelgen.DensityFunctions$ShiftedNoise"})
public interface ContinentalNoiseAccessor {
    @Accessor("noise") DensityFunction.NoiseHolder wayaround$noise();
}
