package net.caravidro.wayaround.mixin.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.caravidro.wayaround.daybreak.client.DaysBreakClient;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Remove moon/sky/gamma illumination at night, preserving real block lights. */
@Mixin(value=LightTexture.class,priority=900)
public abstract class DaysBreakLightMixin {
    @Shadow @Final private NativeImage lightPixels;
    @Inject(method="updateLightTexture",at=@At(value="INVOKE",
            target="Lnet/minecraft/client/renderer/texture/DynamicTexture;upload()V",shift=At.Shift.BEFORE))
    private void wayaround$daysBreakNight(float partial,CallbackInfo ci) {
        if(!DaysBreakClient.active()||DaysBreakClient.day())return;
        for(int block=0;block<16;block++) {
            int original=lightPixels.getPixelRGBA(block,0);
            float brightness=block/15f;brightness*=brightness;
            int r=Math.round((original&255)*brightness),g=Math.round(((original>>>8)&255)*brightness),b=Math.round(((original>>>16)&255)*brightness);
            int color=0xFF000000|(b<<16)|(g<<8)|r;
            for(int sky=0;sky<16;sky++)lightPixels.setPixelRGBA(block,sky,color);
        }
    }
}
