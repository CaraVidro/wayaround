package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.accessory.AccessoryManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AccessoryActionC2SPayload(
        byte slot,
        byte action
) implements CustomPacketPayload {

    public static final byte EQUIP_OR_UNEQUIP =
            0;

    public static final byte TOGGLE =
            1;

    public static final Type<AccessoryActionC2SPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "accessory_action_c2s"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            AccessoryActionC2SPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeByte(
                                payload.slot()
                        );
                        buf.writeByte(
                                payload.action()
                        );
                    },
                    buf ->
                            new AccessoryActionC2SPayload(
                                    buf.readByte(),
                                    buf.readByte()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            AccessoryActionC2SPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> {
                    if (context.player()
                            instanceof ServerPlayer player) {
                        AccessoryManager.handlePanelAction(
                                player,
                                payload.slot(),
                                payload.action()
                        );
                    }
                }
        );
    }
}
