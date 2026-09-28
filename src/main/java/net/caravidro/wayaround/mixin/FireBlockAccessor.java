package net.caravidro.wayaround.mixin;

import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Reuses vanilla's own fire table so enhanced spread respects the same fuels
 * as ordinary FireBlock, including blocks registered as burnable by mods.
 */
@Mixin(FireBlock.class)
public interface FireBlockAccessor {

    @Invoker("canBurn")
    boolean wayaround$canBurn(
            BlockState state
    );
}
