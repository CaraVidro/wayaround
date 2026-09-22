package net.caravidro.wayaround.mixin;

import net.caravidro.wayaround.worldgen.weather.AntarcticWaterFreezing;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class AntarcticWaterFreezingMixin {
    @Inject(method = "tickChunk", at = @At("TAIL"))
    private void wayaround$freezeAntarcticWater(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        AntarcticWaterFreezing.tickChunk((ServerLevel) (Object) this, chunk, randomTickSpeed);
    }
}
