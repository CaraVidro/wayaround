package net.caravidro.wayaround.media.client;

import net.caravidro.wayaround.network.BroadcastAmbientC2SPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;

public final class BroadcastAmbientCapture {
    private static long second = -1L;
    private static int sentThisSecond;

    private BroadcastAmbientCapture() {}

    public static void maybeForward(
            SoundInstance sound,
            SoundSource source,
            float volume,
            float pitch
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.getConnection() == null || sound.isRelative()) return;

        double dx = sound.getX() - minecraft.player.getX();
        double dy = sound.getY() - minecraft.player.getY();
        double dz = sound.getZ() - minecraft.player.getZ();

        if (dx * dx + dy * dy + dz * dz > 48.0 * 48.0) return;

        long nowSecond = System.currentTimeMillis() / 1000L;
        if (nowSecond != second) {
            second = nowSecond;
            sentThisSecond = 0;
        }

        if (++sentThisSecond > 24) return;

        PacketDistributor.sendToServer(
                new BroadcastAmbientC2SPayload(
                        sound.getLocation().toString(),
                        source.name(),
                        volume,
                        pitch,
                        sound.getX(),
                        sound.getY(),
                        sound.getZ()
                )
        );
    }
}
