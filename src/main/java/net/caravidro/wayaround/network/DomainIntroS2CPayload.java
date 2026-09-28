package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.client.DomainIntroClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DomainIntroS2CPayload(
        byte style,
        int durationTicks
) implements CustomPacketPayload {

    public static final Type<DomainIntroS2CPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "domain_intro_s2c"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            DomainIntroS2CPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeByte(
                                payload.style()
                        );

                        buf.writeVarInt(
                                payload.durationTicks()
                        );
                    },
                    buf ->
                            new DomainIntroS2CPayload(
                                    buf.readByte(),
                                    buf.readVarInt()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            DomainIntroS2CPayload payload,
            IPayloadContext context
    ) {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }

        context.enqueueWork(
                () ->
                        DomainIntroClient.start(
                                payload.style(),
                                payload.durationTicks()
                        )
        );
    }
}
