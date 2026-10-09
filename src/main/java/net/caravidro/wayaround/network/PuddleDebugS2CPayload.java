package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.appearance.PuddleDebugClientCache;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** One bounded, opt-in visual test puddle; duration=0 removes it immediately. */
public record PuddleDebugS2CPayload(long position, int durationTicks)
        implements CustomPacketPayload {
    public static final Type<PuddleDebugS2CPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(WayAround.MODID, "puddle_debug_s2c"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PuddleDebugS2CPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeLong(p.position());
                        buf.writeVarInt(p.durationTicks());
                    },
                    buf -> new PuddleDebugS2CPayload(buf.readLong(), buf.readVarInt()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PuddleDebugS2CPayload payload, IPayloadContext context) {
        if (payload.durationTicks < 0 || payload.durationTicks > 2400) return;
        context.enqueueWork(() -> PuddleDebugClientCache.receive(
                payload.position(), payload.durationTicks()));
    }
}
