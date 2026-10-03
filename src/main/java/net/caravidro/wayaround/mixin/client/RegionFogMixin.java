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
        boolean reset=wayaround$level.get()!=level || wayaround$time==0 || fluid==net.minecraft.world.level.material.FogType.LAVA
                || fluid==net.minecraft.world.level.material.FogType.POWDER_SNOW || net.caravidro.wayaround.daybreak.client.DaysBreakClient.active();
        float factor=reset?1F:(float)(1-Math.exp(-Math.min(100,now-wayaround$time)/850.0));
        wayaround$time=now;if(wayaround$level.get()!=level)wayaround$level=new java.lang.ref.WeakReference<>(level);
        wayaround$r+=factor*(fogRed-wayaround$r);wayaround$g+=factor*(fogGreen-wayaround$g);wayaround$b+=factor*(fogBlue-wayaround$b);
        fogRed=wayaround$r;fogGreen=wayaround$g;fogBlue=wayaround$b;
        RenderSystem.setShaderFogColor(fogRed,fogGreen,fogBlue,1);RenderSystem.clearColor(fogRed,fogGreen,fogBlue,0);
    }
}
