package net.caravidro.wayaround.mixin.client;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Smooth the final regional fog color after abyss/polar/Nexus effects. */
@Mixin(value=FogRenderer.class,priority=800)
public abstract class RegionFogMixin {
    @Shadow private static float fogRed,fogGreen,fogBlue;
    @Unique private static float wayaround$r,wayaround$g,wayaround$b;
    @Unique private static long wayaround$time;
    @Unique private static java.lang.ref.WeakReference<ClientLevel> wayaround$level=new java.lang.ref.WeakReference<>(null);
    @Inject(method="setupColor",at=@At("TAIL"))
    private static void wayaround$fade(Camera camera,float partial,ClientLevel level,int distance,float boss,CallbackInfo ci) {
        long now=Util.getMillis();var fluid=camera.getFluidInCamera();

        if(fluid==net.minecraft.world.level.material.FogType.NONE){
            var p=camera.getPosition();
            var environment=net.caravidro.wayaround.environment.EnvironmentalFieldClientCache.get(p.x,p.z);
            if(environment!=null){
                float smoke=Math.clamp(environment.smoke(),0F,1F);
                float pollution=Math.clamp(environment.pollution(),0F,1F);
                float humidity=Math.clamp(environment.humidity(),0F,1F);
                float cloudWater=Math.clamp(environment.cloudWater(),0F,1F);
                float haze=Math.clamp(smoke*.55F+pollution*.32F,0F,.58F);
                float mist=Math.clamp((humidity-.78F)*1.55F+cloudWater*.12F,0F,.28F)
                        *(1F-haze*.65F);
                float targetR=.34F+pollution*.08F;
                float targetG=.33F+pollution*.02F;
                float targetB=.32F-smoke*.04F;
                fogRed+=(targetR-fogRed)*haze;
                fogGreen+=(targetG-fogGreen)*haze;
                fogBlue+=(targetB-fogBlue)*haze;
                fogRed+=(.72F-fogRed)*mist;
                fogGreen+=(.76F-fogGreen)*mist;
                fogBlue+=(.80F-fogBlue)*mist;
            }
        }

        boolean reset=wayaround$level.get()!=level || wayaround$time==0 || fluid==net.minecraft.world.level.material.FogType.LAVA
                || fluid==net.minecraft.world.level.material.FogType.POWDER_SNOW || net.caravidro.wayaround.daybreak.client.DaysBreakClient.active();
        float factor=reset?1F:(float)(1-Math.exp(-Math.min(100,now-wayaround$time)/850.0));
        wayaround$time=now;if(wayaround$level.get()!=level)wayaround$level=new java.lang.ref.WeakReference<>(level);
        wayaround$r+=factor*(fogRed-wayaround$r);wayaround$g+=factor*(fogGreen-wayaround$g);wayaround$b+=factor*(fogBlue-wayaround$b);
        fogRed=wayaround$r;fogGreen=wayaround$g;fogBlue=wayaround$b;
        RenderSystem.setShaderFogColor(fogRed,fogGreen,fogBlue,1);RenderSystem.clearColor(fogRed,fogGreen,fogBlue,0);
    }
}
