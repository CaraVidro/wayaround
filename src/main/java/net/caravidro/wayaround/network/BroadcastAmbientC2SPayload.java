package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.media.broadcast.BroadcastManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BroadcastAmbientC2SPayload(
        String sound,
        String source,
        float volume,
        float pitch,
        double x,
        double y,
        double z
) implements CustomPacketPayload {
    public static final Type<BroadcastAmbientC2SPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(WayAround.MODID, "broadcast_ambient_c2s"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BroadcastAmbientC2SPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUtf(payload.sound(), 160);
                        buf.writeUtf(payload.source(), 24);
                        buf.writeFloat(payload.volume());
                        buf.writeFloat(payload.pitch());
                        buf.writeDouble(payload.x());
                        buf.writeDouble(payload.y());
                        buf.writeDouble(payload.z());
                    },
                    buf -> new BroadcastAmbientC2SPayload(
                            buf.readUtf(160),
                            buf.readUtf(24),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readDouble()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(BroadcastAmbientC2SPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            ResourceLocation sound = ResourceLocation.tryParse(payload.sound());
            if (sound == null) return;

            SoundSource source;
            try {
                source = SoundSource.valueOf(payload.source());
            } catch (Exception exception) {
                source = SoundSource.BLOCKS;
            }

            BroadcastManager.captureAmbient(
                    player,
                    sound,
                    source,
                    payload.volume(),
                    payload.pitch(),
                    new Vec3(payload.x(), payload.y(), payload.z())
            );
        });
    }
}
