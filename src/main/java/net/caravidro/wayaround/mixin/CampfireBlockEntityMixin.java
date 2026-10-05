package net.caravidro.wayaround.mixin;

import net.caravidro.wayaround.physical.VanillaMatterInteractions;
import net.caravidro.wayaround.thermal.EnvironmentalTemperature;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets ordinary lit Minecraft campfires participate in the physical foundation
 * without replacing the vanilla block or its cooking logic.
 */
@Mixin(CampfireBlockEntity.class)
public abstract class CampfireBlockEntityMixin {

    @Inject(
            method = "cookTick",
            at = @At("TAIL")
    )
    private static void wayaround$physicalCampfireTick(
            Level level,
            BlockPos pos,
            BlockState state,
            CampfireBlockEntity campfire,
            CallbackInfo ci
    ) {
        if (level instanceof ServerLevel serverLevel) {
            VanillaMatterInteractions.tickLitCampfire(
                    serverLevel,
                    pos,
                    state
            );

            /*
             * Campfires are reliable thermal sources rather than relying on
             * opportunistic nearby-player sampling. UniversalTemperature
             * de-duplicates repeated applications within the same second.
             */
            if (Math.floorMod(
                    serverLevel.getGameTime()
                            + pos.asLong(),
                    20L
            ) == 0L) {
                EnvironmentalTemperature.applyBlockSource(
                        serverLevel,
                        pos,
                        state
                );
            }
        }
    }
}
