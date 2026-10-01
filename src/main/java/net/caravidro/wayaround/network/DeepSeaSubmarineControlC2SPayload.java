package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.ecology.DeepSeaSubmarineEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DeepSeaSubmarineControlC2SPayload(
        byte throttle,
        byte steering,
        byte vertical
) implements CustomPacketPayload {

    public static final Type<DeepSeaSubmarineControlC2SPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "deep_sea_submarine_control_c2s"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            DeepSeaSubmarineControlC2SPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeByte(
                                payload.throttle()
                        );

                        buf.writeByte(
                                payload.steering()
                        );

                        buf.writeByte(
                                payload.vertical()
                        );
                    },
                    buf ->
                            new DeepSeaSubmarineControlC2SPayload(
                                    buf.readByte(),
                                    buf.readByte(),
                                    buf.readByte()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            DeepSeaSubmarineControlC2SPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> {
                    if (!(context.player()
                            instanceof ServerPlayer player)
                            || !(player.getVehicle()
                            instanceof DeepSeaSubmarineEntity submarine)) {
                        return;
                    }

                    submarine.setControls(
                            clamp(
                                    payload.throttle()
                            ),
                            clamp(
                                    payload.steering()
                            ),
                            clamp(
                                    payload.vertical()
                            )
                    );
                }
        );
    }

    private static float clamp(
            byte value
    ) {
        return Math.max(
                -1.0F,
                Math.min(
                        1.0F,
                        value
                )
        );
    }
}
