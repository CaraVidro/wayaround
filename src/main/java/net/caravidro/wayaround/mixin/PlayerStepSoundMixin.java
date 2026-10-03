package net.caravidro.wayaround.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Reuses vanilla material selection and step cadence; no additional sound emission. */
@Mixin(Entity.class)
public abstract class PlayerStepSoundMixin {
    @ModifyArgs(method = {"playStepSound", "playCombinationStepSounds", "playMuffledStepSound"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"))
    private void wayaround$audibleMaterialStep(Args args) {
        if (!((Object)this instanceof Player player)) return;
        float volume = args.get(1), pitch = args.get(2);
        float gain = player.isCrouching() ? 1.25f : player.isSprinting() ? 3.2f : 2.6f;
        args.set(1, Math.min(.85f, volume * gain));
        args.set(2, pitch * (player.isSprinting() ? .94f : 1f) * (.97f + player.getRandom().nextFloat() * .06f));
    }
}
