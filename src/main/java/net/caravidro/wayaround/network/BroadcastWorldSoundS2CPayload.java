package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.media.broadcast.BroadcastEffect;
import net.caravidro.wayaround.media.client.RecordedWorldSound;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BroadcastWorldSoundS2CPayload(
        BlockPos receiver,
        String sound,
        String source,
        float volume,
        float pitch,
        float quality,
        int effect
) implements CustomPacketPayload {
    public static final Type<BroadcastWorldSoundS2CPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(WayAround.MODID, "broadcast_world_sound_s2c"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BroadcastWorldSoundS2CPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeBlockPos(payload.receiver());
                        buf.writeUtf(payload.sound(), 160);
                        buf.writeUtf(payload.source(), 24);
                        buf.writeFloat(payload.volume());
                        buf.writeFloat(payload.pitch());
                        buf.writeFloat(payload.quality());
                        buf.writeVarInt(payload.effect());
                    },
                    buf -> new BroadcastWorldSoundS2CPayload(
                            buf.readBlockPos(),
                            buf.readUtf(160),
                            buf.readUtf(24),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readVarInt()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(BroadcastWorldSoundS2CPayload payload, IPayloadContext context) {
        if (!FMLEnvironment.dist.isClient()) return;

        context.enqueueWork(() -> {
            ResourceLocation sound = ResourceLocation.tryParse(payload.sound());
            if (sound == null) return;

            SoundSource source;
            try { source = SoundSource.valueOf(payload.source()); }
            catch (Exception ignored) { source = SoundSource.BLOCKS; }

            float quality = Math.max(0.0F, Math.min(1.0F, payload.quality()));
            float volume = payload.volume() * (0.35F + quality * 0.65F);
            float pitch = payload.pitch();

            BroadcastEffect[] effects = BroadcastEffect.values();
            BroadcastEffect effect = effects[Math.max(0, Math.min(effects.length - 1, payload.effect()))];

            if (effect == BroadcastEffect.DISTANT) pitch *= 0.92F;
            if (effect == BroadcastEffect.VHS) pitch *= 0.985F;
            if (effect == BroadcastEffect.GLITCH && (System.nanoTime() & 3L) == 0L) return;

            Minecraft.getInstance().getSoundManager().play(
                    new RecordedWorldSound(
                            sound,
                            source,
                            volume,
                            pitch,
                            payload.receiver().getX() + 0.5,
                            payload.receiver().getY() + 0.5,
                            payload.receiver().getZ() + 0.5
                    )
            );
        });
    }
}
