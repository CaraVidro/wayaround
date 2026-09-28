package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.client.AccessoryWorkshopScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AccessoryWorkshopOpenS2CPayload(
        long blockPos,
        int hand,
        String kind,
        int material,
        int size,
        int extras,
        int woolColor
) implements CustomPacketPayload {

    public static final Type<AccessoryWorkshopOpenS2CPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "accessory_workshop_open_s2c"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            AccessoryWorkshopOpenS2CPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeLong(payload.blockPos());
                        buf.writeVarInt(payload.hand());
                        buf.writeUtf(payload.kind());
                        buf.writeVarInt(payload.material());
                        buf.writeVarInt(payload.size());
                        buf.writeVarInt(payload.extras());
                        buf.writeVarInt(payload.woolColor());
                    },
                    buf -> new AccessoryWorkshopOpenS2CPayload(
                            buf.readLong(),
                            buf.readVarInt(),
                            buf.readUtf(),
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readVarInt()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            AccessoryWorkshopOpenS2CPayload payload,
            IPayloadContext context
    ) {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }

        context.enqueueWork(
                () -> Minecraft.getInstance()
                        .setScreen(
                                new AccessoryWorkshopScreen(
                                        payload
                                )
                        )
        );
    }
}
