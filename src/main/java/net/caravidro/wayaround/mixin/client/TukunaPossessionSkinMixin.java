package net.caravidro.wayaround.mixin.client;

import java.util.UUID;

import net.caravidro.wayaround.client.TukunaPossessionClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Possession changes visual identity, not the physical controller entity.
 *
 * Tukuna stays the single moving ServerPlayer so input/movement remain native
 * and latency-free. While linked, its client player resolves the receptacle's
 * PlayerSkin (texture + slim/wide model metadata).
 */
@Mixin(AbstractClientPlayer.class)
public abstract class TukunaPossessionSkinMixin {

    @Inject(
            method = "getSkin",
            at = @At("HEAD"),
            cancellable = true
    )
    private void wayaround$receptacleSkin(
            CallbackInfoReturnable<PlayerSkin> cir
    ) {
        AbstractClientPlayer self =
                (AbstractClientPlayer) (Object) this;

        UUID body =
                TukunaPossessionClient.bodyForController(
                        self.getUUID()
                );

        if (body == null
                || body.equals(self.getUUID())) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.getConnection() == null) {
            return;
        }

        PlayerInfo info =
                minecraft.getConnection()
                        .getPlayerInfo(
                                body
                        );

        if (info != null) {
            cir.setReturnValue(
                    info.getSkin()
            );
        }
    }
}
