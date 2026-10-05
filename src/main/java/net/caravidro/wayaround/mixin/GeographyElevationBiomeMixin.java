package net.caravidro.wayaround.mixin;

import net.caravidro.wayaround.worldgen.planet.GeographyBiomes;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiNoiseBiomeSource.class)
public abstract class GeographyElevationBiomeMixin {
    @Inject(method="getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",at=@At("RETURN"),cancellable=true)
    private void wayaround$elevation(int x,int y,int z,Climate.Sampler sampler,CallbackInfoReturnable<Holder<Biome>> cir) {
        cir.setReturnValue(GeographyBiomes.elevation((MultiNoiseBiomeSource)(Object)this,cir.getReturnValue(),x,y,z,sampler));
    }
}
