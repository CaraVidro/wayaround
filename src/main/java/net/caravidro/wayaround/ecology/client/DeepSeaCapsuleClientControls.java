package net.caravidro.wayaround.ecology.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.ecology.DeepSeaCapsuleEntity;
import net.caravidro.wayaround.network.DeepSeaCapsuleControlC2SPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Maps ordinary forward/back keys to vertical capsule thrust. */
@EventBusSubscriber(modid = WayAround.MODID, value = Dist.CLIENT)
public final class DeepSeaCapsuleClientControls {
    private static byte lastInput;
    private static int resend;

    private DeepSeaCapsuleClientControls() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || !(minecraft.player.getVehicle() instanceof DeepSeaCapsuleEntity)
                || minecraft.screen != null) {
            if (lastInput != 0) {
                lastInput = 0;
                PacketDistributor.sendToServer(new DeepSeaCapsuleControlC2SPayload((byte) 0));
            }
            return;
        }

        byte input = 0;
        if (minecraft.options.keyUp.isDown()) input++;
        if (minecraft.options.keyDown.isDown()) input--;

        if (input != lastInput || resend-- <= 0) {
            lastInput = input;
            resend = 3;
            PacketDistributor.sendToServer(new DeepSeaCapsuleControlC2SPayload(input));
        }
    }
}
