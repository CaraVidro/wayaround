package net.caravidro.wayaround.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PolarBear.class)
public abstract class PolarBearSpawnMixin {
    @Inject(method = "checkPolarBearSpawnRules", at = @At("HEAD"), cancellable = true)
    private static void wayaround$restrictNaturalSpawnToArctic(
            EntityType<PolarBear> type, LevelAccessor level, MobSpawnType reason,
            BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> cir
    ) {
        if (reason != MobSpawnType.NATURAL && reason != MobSpawnType.CHUNK_GENERATION) return;
        // Negative Z is north. Keep vanilla biome, ground and light checks there.
        if (!(level instanceof ServerLevelAccessor serverLevel)
                || !serverLevel.getLevel().dimension().equals(Level.OVERWORLD)
                || pos.getZ() > -12000) {
            cir.setReturnValue(false);
        }
    }
}
