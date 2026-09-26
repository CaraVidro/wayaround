package net.caravidro.wayaround.network;

import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.client.TukunaPossessionClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Visual-only possession link.
 *
 * The controller remains the one real moving ServerPlayer for input/gameplay.
 * Clients hide the frozen receptacle entity and resolve the controller with
 * the receptacle's visual identity. Refresh packets also make late observers
 * discover an already-running possession.
 */
public record TukunaPossessionVisualS2CPayload(
        UUID controller,
        UUID body,
        boolean active
) implements CustomPacketPayload {

    public static final Type<TukunaPossessionVisualS2CPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    WayAround.MODID,
                    "tukuna_possession_visual_s2c"
            ));

    public static final StreamCodec<RegistryFriendlyByteBuf, TukunaPossessionVisualS2CPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(payload.controller());
                        buf.writeUUID(payload.body());
                        buf.writeBoolean(payload.active());
                    },
                    buf -> new TukunaPossessionVisualS2CPayload(
                            buf.readUUID(),
                            buf.readUUID(),
                            buf.readBoolean()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            TukunaPossessionVisualS2CPayload payload,
            IPayloadContext context
    ) {
        if (!FMLEnvironment.dist.isClient()) return;
        context.enqueueWork(() -> TukunaPossessionClient.setVisualLink(
                payload.controller(),
                payload.body(),
                payload.active()
        ));
    }
}
