package net.caravidro.wayaround.mixin.client;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Camera.class)
public abstract class EntitySpectateCameraMixin {
 @Shadow private boolean detached;
 @Shadow protected abstract void setRotation(float yaw,float pitch);
 @Shadow protected abstract void setPosition(Vec3 p);
 @Shadow protected abstract void move(float x,float y,float z);
 @Shadow private float getMaxZoom(float zoom){return 0;}
 @Inject(method="setup",at=@At("TAIL")) private void orbit(BlockGetter level,Entity entity,boolean detached,boolean reverse,float partial,CallbackInfo ci){
  var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
  Entity observed=mc.level.getEntity(net.caravidro.wayaround.client.FieldInteractionClient.target);if(observed==null||!observed.isAlive())return;
  this.detached=true;
  setPosition(observed.getPosition(partial).add(0,observed.getBbHeight()*.5,0));setRotation(mc.player.getYRot(),mc.player.getXRot());move(-getMaxZoom(Math.max(3,Math.min(16,observed.getBbWidth()*2+2))),0,0);
 }
}
