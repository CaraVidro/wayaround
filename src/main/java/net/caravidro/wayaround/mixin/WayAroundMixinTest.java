package net.caravidro.wayaround.mixin;

import net.caravidro.wayaround.WayAround;
import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class WayAroundMixinTest {

    @Inject(method = "runServer", at = @At("HEAD"))
    private void wayaround$mixinTest(CallbackInfo ci) {
        WayAround.LOGGER.info("=================================");
        WayAround.LOGGER.info("WAYAROUND MIXIN ESTÁ VIVO!");
        WayAround.LOGGER.info("=================================");
    }
}