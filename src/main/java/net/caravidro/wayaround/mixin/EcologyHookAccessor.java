package net.caravidro.wayaround.mixin;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(FishingHook.class)
public interface EcologyHookAccessor {
 @Invoker("setHookedEntity") void wayaround$setHookedEntity(Entity entity);
}
