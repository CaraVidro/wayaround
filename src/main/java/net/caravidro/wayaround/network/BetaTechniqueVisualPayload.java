package net.caravidro.wayaround.network;

import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.client.BetaTechniqueClientEffects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BetaTechniqueVisualPayload(
        UUID owner,
        byte mode,
        double x,
        double y,
        double z,
        float power,
        float progress
) implements CustomPacketPayload {

    public static final byte RED = 1;
    public static final byte FUSION = 2;
    public static final byte BLAST = 3;
    public static final byte AFTERMATH = 4;

    public static final Type<BetaTechniqueVisualPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "beta_technique_visual"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            BetaTechniqueVisualPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(
                                payload.owner()
                        );
                        buf.writeByte(
                                payload.mode()
                        );
                        buf.writeDouble(
                                payload.x()
                        );
                        buf.writeDouble(
                                payload.y()
                        );
                        buf.writeDouble(
                                payload.z()
                        );
                        buf.writeFloat(
                                payload.power()
                        );
                        buf.writeFloat(
                                payload.progress()
                        );
                    },
                    buf ->
                            new BetaTechniqueVisualPayload(
                                    buf.readUUID(),
                                    buf.readByte(),
                                    buf.readDouble(),
                                    buf.readDouble(),
                                    buf.readDouble(),
                                    buf.readFloat(),
                                    buf.readFloat()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            BetaTechniqueVisualPayload payload,
            IPayloadContext context
    ) {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }

        context.enqueueWork(
                () -> BetaTechniqueClientEffects.receive(
                        payload
                )
        );
    }
}
