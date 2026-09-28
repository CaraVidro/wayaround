package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldgen.weather.local.WindTestManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Synchronizes the temporary debug wind field. The client evaluates the same
 * ramp locally from gameTime, so the gust stays smooth without a packet every
 * frame.
 */
public record WindTestStateS2CPayload(
        boolean active,
        double centerX,
        double centerZ,
        double radius,
        long startedAt,
        int rampTicks,
        int fadeTicks,
        float limit
) implements CustomPacketPayload {

    public static final Type<WindTestStateS2CPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "wind_test_state_s2c"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            WindTestStateS2CPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeBoolean(payload.active());
                        buf.writeDouble(payload.centerX());
                        buf.writeDouble(payload.centerZ());
                        buf.writeDouble(payload.radius());
                        buf.writeLong(payload.startedAt());
                        buf.writeInt(payload.rampTicks());
                        buf.writeInt(payload.fadeTicks());
                        buf.writeFloat(payload.limit());
                    },
                    buf -> new WindTestStateS2CPayload(
                            buf.readBoolean(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readLong(),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readFloat()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            WindTestStateS2CPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> WindTestManager.acceptRemote(
                        payload.active(),
                        payload.centerX(),
                        payload.centerZ(),
                        payload.radius(),
                        payload.startedAt(),
                        payload.rampTicks(),
                        payload.fadeTicks(),
                        payload.limit()
                )
        );
    }
}
