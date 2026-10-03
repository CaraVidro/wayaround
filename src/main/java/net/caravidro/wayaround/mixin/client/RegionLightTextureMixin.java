package net.caravidro.wayaround.mixin.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ease ambient regional lighting without delaying strong artificial sources. */
@Mixin(value=LightTexture.class,priority=800)
public abstract class RegionLightTextureMixin {
    @Shadow @Final private NativeImage lightPixels;
    @Unique private final int[] wayaround$previous=new int[256];
    @Unique private long wayaround$last;
    @Unique private Object wayaround$level;
    @Inject(method="updateLightTexture",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/texture/DynamicTexture;upload()V",shift=At.Shift.BEFORE))
    private void wayaround$adapt(float partial,CallbackInfo ci) {
        var level=Minecraft.getInstance().level;if(level==null)return;
        long now=Util.getMillis();boolean reset=wayaround$level!=level || wayaround$last==0 || net.caravidro.wayaround.daybreak.client.DaysBreakClient.active();
        float factor=(float)(1-Math.exp(-Math.min(250,now-wayaround$last)/750.0));wayaround$last=now;wayaround$level=level;
        for(int sky=0;sky<16;sky++)for(int block=0;block<16;block++) {
            int index=sky*16+block,target=lightPixels.getPixelRGBA(block,sky),previous=wayaround$previous[index];
            int color=target;
            if(!reset && block<8) {
                color=target&0xFF000000;
                for(int shift=0;shift<24;shift+=8) {
                    int t=(target>>>shift)&255,p=(previous>>>shift)&255;
                    int v=Math.abs(t-p)<3?t:Math.round(p+(t-p)*factor);
                    if(v==p && t!=p && factor>0)v+=Integer.signum(t-p);color|=v<<shift;
                }
                lightPixels.setPixelRGBA(block,sky,color);
            }
            wayaround$previous[index]=color;
        }
    }
}
