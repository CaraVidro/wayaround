package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldgen.weather.local.CloudRainOverrides;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Minimal temporary rain-state update for a single procedural cloud. */
public record CloudRainS2CPayload(long cellId, long untilGameTime)
        implements CustomPacketPayload {

    public static final Type<CloudRainS2CPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(WayAround.MODID, "cloud_rain_override_s2c"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CloudRainS2CPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, packet) -> {
                        buf.writeLong(packet.cellId());
                        buf.writeLong(packet.untilGameTime());
                    },
                    buf -> new CloudRainS2CPayload(buf.readLong(), buf.readLong()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CloudRainS2CPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> CloudRainOverrides.receive(
                packet.cellId(), packet.untilGameTime()));
    }
}
