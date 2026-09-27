package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(
        modid = WayAround.MODID
)
public final class CalvingNetwork {

    private CalvingNetwork() {
    }

    @SubscribeEvent
    public static void register(
            RegisterPayloadHandlersEvent event
    ) {

        event.registrar(
                        "1"
                )
                .playToClient(
                        CalvingShakePayload.TYPE,
                        CalvingShakePayload.STREAM_CODEC,
                        ClientPayloadBridge::handleCalving
                );
    }

    public record CalvingShakePayload(
            double x,
            double y,
            double z,
            float intensity,
            int ticks
    ) implements CustomPacketPayload {

        public static final Type<CalvingShakePayload> TYPE =
                new Type<>(
                        ResourceLocation
                                .fromNamespaceAndPath(
                                        WayAround.MODID,
                                        "calving_shake"
                                )
                );

        public static final StreamCodec<
                RegistryFriendlyByteBuf,
                CalvingShakePayload
                > STREAM_CODEC =
                StreamCodec.of(
                        (buffer, payload) -> {
                            buffer.writeDouble(
                                    payload.x()
                            );

                            buffer.writeDouble(
                                    payload.y()
                            );

                            buffer.writeDouble(
                                    payload.z()
                            );

                            buffer.writeFloat(
                                    payload.intensity()
                            );

                            buffer.writeVarInt(
                                    payload.ticks()
                            );
                        },

                        buffer ->
                                new CalvingShakePayload(
                                        buffer.readDouble(),
                                        buffer.readDouble(),
                                        buffer.readDouble(),
                                        buffer.readFloat(),
                                        buffer.readVarInt()
                                )
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
