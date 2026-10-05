package net.caravidro.wayaround.worldgen.planet;

import java.util.function.Predicate;
import net.caravidro.wayaround.worldconfig.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;

/** Never cancel death or its private death screen: filter only vanilla chat recipients. */
public final class DeathWitnessService {
    public static boolean sees(ServerPlayer observer,ServerPlayer victim) {
        if(observer==victim||observer.level()!=victim.level()||!observer.isAlive()||observer.isSleeping()
                ||observer.getCamera()!=observer||net.caravidro.wayaround.observation.EntitySpectate.active(observer))return false;
        Vec3 eye=observer.getEyePosition(),target=victim.position().add(0,victim.getBbHeight()*.65,0),ray=target.subtract(eye);
        double distance=ray.length();if(distance>48||distance<.05)return false;
        if(observer.getLookAngle().dot(ray.scale(1/distance))<.8191520443)return false;
        // Clip only within the two already-visible entities' loaded area. Fluids do not act as stone walls.
        for(int i=0;i<=Math.ceil(distance/8);i++)if(!observer.serverLevel().hasChunkAt(net.minecraft.core.BlockPos.containing(eye.add(ray.scale(i/Math.max(1,Math.ceil(distance/8)))))))return false;
        return observer.level().clip(new ClipContext(eye,target,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,observer)).getType()==HitResult.Type.MISS;
    }
    public static void send(ServerPlayer victim,Component message,Predicate<ServerPlayer> teamRule) {
        for(ServerPlayer observer:victim.server.getPlayerList().getPlayers())if(teamRule.test(observer)&&sees(observer,victim))observer.sendSystemMessage(message);
    }
    public static boolean enabled(){return WorldFeatureRuntime.serverEnabled(WorldFeature.WITNESSED_DEATHS);}
    private DeathWitnessService() {}
}
