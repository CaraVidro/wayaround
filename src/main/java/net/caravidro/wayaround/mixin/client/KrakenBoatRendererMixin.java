package net.caravidro.wayaround.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.caravidro.wayaround.ecology.client.KrakenSceneRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.world.entity.vehicle.Boat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BoatRenderer.class)
public abstract class KrakenBoatRendererMixin {
    @Inject(method="render(Lnet/minecraft/world/entity/vehicle/Boat;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",shift=At.Shift.AFTER))
    private void wayaround$krakenSwell(Boat boat,float yaw,float partial,PoseStack poses,
                                       MultiBufferSource buffers,int light,CallbackInfo ci) {
        float roll=KrakenSceneRenderer.boatRoll(boat.position(),partial);
        poses.mulPose(Axis.ZP.rotationDegrees(roll));
        poses.mulPose(Axis.XP.rotationDegrees(roll*.45F));
    }
}
