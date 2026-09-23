package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.industrial.assembly.AssemblyInteractionEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AssemblyEmptyHandPayload()
        implements CustomPacketPayload {

    public static final Type<AssemblyEmptyHandPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "assembly_empty_hand"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            AssemblyEmptyHandPayload
    > STREAM_CODEC =
            StreamCodec.unit(
                    new AssemblyEmptyHandPayload()
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            AssemblyEmptyHandPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> {
                    if (context.player()
                            instanceof ServerPlayer player) {
                        AssemblyInteractionEvents.handleEmptyHand(
                                player
                        );
                    }
                }
        );
    }
}
