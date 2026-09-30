package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.industrial.engineering.EngineeringBlueprintData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BlueprintLabelC2SPayload(
        String blueprintId,
        String title,
        boolean showCoordinates,
        boolean showDateTime
) implements CustomPacketPayload {

    public static final Type<BlueprintLabelC2SPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "blueprint_label_c2s"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            BlueprintLabelC2SPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUtf(
                                payload.blueprintId(),
                                64
                        );
                        buf.writeUtf(
                                payload.title(),
                                64
                        );
                        buf.writeBoolean(
                                payload.showCoordinates()
                        );
                        buf.writeBoolean(
                                payload.showDateTime()
                        );
                    },
                    buf -> new BlueprintLabelC2SPayload(
                            buf.readUtf(64),
                            buf.readUtf(64),
                            buf.readBoolean(),
                            buf.readBoolean()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            BlueprintLabelC2SPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> {
                    if (!(context.player()
                            instanceof ServerPlayer player)) {
                        return;
                    }

                    for (int slot =
                                 0;
                         slot < player.getInventory()
                                 .getContainerSize();
                         slot++) {

                        ItemStack stack =
                                player.getInventory()
                                        .getItem(
                                                slot
                                        );

                        EngineeringBlueprintData.Info info =
                                EngineeringBlueprintData.read(
                                        stack
                                ).orElse(null);

                        if (info == null
                                || !info.blueprintId()
                                .equals(
                                        payload.blueprintId()
                                )) {
                            continue;
                        }

                        EngineeringBlueprintData.setPresentation(
                                stack,
                                payload.title(),
                                payload.showCoordinates(),
                                payload.showDateTime()
                        );

                        player.getInventory()
                                .setChanged();

                        return;
                    }
                }
        );
    }
}
