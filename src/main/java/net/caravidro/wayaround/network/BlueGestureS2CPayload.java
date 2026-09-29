package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BlueGestureS2CPayload(
        byte gesture
) implements CustomPacketPayload {

    public static final byte SUMMON = 1;
    public static final byte ORBIT = 2;
    public static final byte LAUNCH = 3;
    public static final byte STOP = 4;
    public static final byte HOLD = 5;
    public static final byte FUSION = 6;

    public static final Type<BlueGestureS2CPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "blue_gesture_s2c"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            BlueGestureS2CPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) ->
                            buf.writeByte(
                                    payload.gesture()
                            ),
                    buf ->
                            new BlueGestureS2CPayload(
                                    buf.readByte()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            BlueGestureS2CPayload payload,
            IPayloadContext context
    ) {
        ClientPayloadBridge.handleBlueGesture(
                payload,
                context
        );
    }
}
