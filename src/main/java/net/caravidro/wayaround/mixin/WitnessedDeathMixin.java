package net.caravidro.wayaround.mixin;

import net.caravidro.wayaround.worldgen.planet.DeathWitnessService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(ServerPlayer.class)
public abstract class WitnessedDeathMixin {
    @Redirect(method="die",at=@At(value="INVOKE",target="Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void wayaround$localDeath(PlayerList list,Component message,boolean overlay) {
        if(!DeathWitnessService.enabled())list.broadcastSystemMessage(message,overlay);
        else DeathWitnessService.send((ServerPlayer)(Object)this,message,p->true);
    }
    @Redirect(method="die",at=@At(value="INVOKE",target="Lnet/minecraft/server/players/PlayerList;broadcastSystemToTeam(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/network/chat/Component;)V"))
    private void wayaround$teamDeath(PlayerList list,Player source,Component message) {
        if(!DeathWitnessService.enabled())list.broadcastSystemToTeam(source,message);
        else DeathWitnessService.send((ServerPlayer)(Object)this,message,p->source.getTeam()!=null&&source.getTeam().equals(p.getTeam()));
    }
    @Redirect(method="die",at=@At(value="INVOKE",target="Lnet/minecraft/server/players/PlayerList;broadcastSystemToAllExceptTeam(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/network/chat/Component;)V"))
    private void wayaround$otherTeamDeath(PlayerList list,Player source,Component message) {
        if(!DeathWitnessService.enabled())list.broadcastSystemToAllExceptTeam(source,message);
        else DeathWitnessService.send((ServerPlayer)(Object)this,message,p->source.getTeam()==null||!source.getTeam().equals(p.getTeam()));
    }
}
