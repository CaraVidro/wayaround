package net.caravidro.wayaround.network;

import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.client.TukunaMarkRenderer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TukunaMarkS2CPayload(
        UUID player,
        boolean active,
        int transitionTicks
) implements CustomPacketPayload {

    public static final Type<TukunaMarkS2CPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    WayAround.MODID,
                    "tukuna_mark_s2c"
            ));

    public static final StreamCodec<RegistryFriendlyByteBuf, TukunaMarkS2CPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(payload.player());
                        buf.writeBoolean(payload.active());
                        buf.writeVarInt(payload.transitionTicks());
                    },
                    buf -> new TukunaMarkS2CPayload(
                            buf.readUUID(),
                            buf.readBoolean(),
                            buf.readVarInt()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            TukunaMarkS2CPayload payload,
            IPayloadContext context
    ) {
        if (!FMLEnvironment.dist.isClient()) return;
        context.enqueueWork(() -> TukunaMarkRenderer.receive(payload));
    }
}
