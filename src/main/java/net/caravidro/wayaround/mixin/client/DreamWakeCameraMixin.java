package net.caravidro.wayaround.mixin.client;
import net.caravidro.wayaround.dream.client.DreamClientState;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Camera.class)
public abstract class DreamWakeCameraMixin {
    @Shadow protected abstract void move(float zoom,float dy,float dx);
    @Inject(method="setup",at=@At("TAIL"))
    private void wayaround$wake(BlockGetter level,Entity entity,boolean detached,boolean reverse,float partialTick,CallbackInfo ci){
        if(!detached)move(0,DreamClientState.wakeOffset(partialTick),0);
    }
}
