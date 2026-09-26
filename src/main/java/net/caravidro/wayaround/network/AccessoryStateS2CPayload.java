package net.caravidro.wayaround.network;

import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.client.AccessoryClientState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AccessoryStateS2CPayload(
        UUID player,
        String head,
        String hands,
        String torso,
        String feet,
        int glassesMode
) implements CustomPacketPayload {

    public static final Type<AccessoryStateS2CPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "accessory_state_s2c"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            AccessoryStateS2CPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(
                                payload.player()
                        );
                        buf.writeUtf(
                                payload.head()
                        );
                        buf.writeUtf(
                                payload.hands()
                        );
                        buf.writeUtf(
                                payload.torso()
                        );
                        buf.writeUtf(
                                payload.feet()
                        );
                        buf.writeVarInt(
                                payload.glassesMode()
                        );
                    },
                    buf ->
                            new AccessoryStateS2CPayload(
                                    buf.readUUID(),
                                    buf.readUtf(),
                                    buf.readUtf(),
                                    buf.readUtf(),
                                    buf.readUtf(),
                                    buf.readVarInt()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            AccessoryStateS2CPayload payload,
            IPayloadContext context
    ) {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }

        context.enqueueWork(
                () ->
                        AccessoryClientState.receive(
                                payload
                        )
        );
    }
}
