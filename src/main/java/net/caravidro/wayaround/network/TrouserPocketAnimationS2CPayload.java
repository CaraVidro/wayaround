package net.caravidro.wayaround.network;

import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.client.TrouserPocketClientState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TrouserPocketAnimationS2CPayload(
        UUID player,
        int durationTicks
) implements CustomPacketPayload {

    public static final Type<TrouserPocketAnimationS2CPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "trouser_pocket_animation_s2c"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            TrouserPocketAnimationS2CPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(
                                payload.player()
                        );
                        buf.writeVarInt(
                                payload.durationTicks()
                        );
                    },
                    buf ->
                            new TrouserPocketAnimationS2CPayload(
                                    buf.readUUID(),
                                    buf.readVarInt()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            TrouserPocketAnimationS2CPayload payload,
            IPayloadContext context
    ) {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }

        context.enqueueWork(
                () ->
                        TrouserPocketClientState.receive(
                                payload
                        )
        );
    }
}
