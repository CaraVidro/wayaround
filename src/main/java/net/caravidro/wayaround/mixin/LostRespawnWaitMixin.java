package net.caravidro.wayaround.mixin;

import net.caravidro.wayaround.worldgen.planet.LostRespawnService;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class LostRespawnWaitMixin {
    @Shadow public ServerPlayer player;
    @Inject(method="handleClientCommand",at=@At("HEAD"),cancellable=true)
    private void wayaround$prepareLand(ServerboundClientCommandPacket packet,CallbackInfo ci) {
        // Network handlers must execute on the server thread before modifying travel state.
        if(!player.server.isSameThread())return;
        if(packet.getAction()==ServerboundClientCommandPacket.Action.PERFORM_RESPAWN&&LostRespawnService.waitForDestination(player))ci.cancel();
    }
}
