package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.accessory.AccessoryCustomizationData;
import net.caravidro.wayaround.accessory.AccessoryItem;
import net.caravidro.wayaround.accessory.AccessoryKind;
import net.caravidro.wayaround.content.OddityContent;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AccessoryWorkshopApplyC2SPayload(
        long blockPos,
        int hand,
        String kind,
        int material,
        int size,
        int extras,
        int woolColor
) implements CustomPacketPayload {

    public static final Type<AccessoryWorkshopApplyC2SPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "accessory_workshop_apply_c2s"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            AccessoryWorkshopApplyC2SPayload
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
                    buf -> new AccessoryWorkshopApplyC2SPayload(
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
            AccessoryWorkshopApplyC2SPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> {
                    if (!(context.player()
                            instanceof ServerPlayer player)) {
                        return;
                    }

                    BlockPos pos =
                            BlockPos.of(
                                    payload.blockPos()
                            );

                    if (!WorldFeatureRuntime.serverEnabled(
                            WorldFeature.ACCESSORIES
                    )
                            || player.position()
                            .distanceToSqr(
                                    Vec3.atCenterOf(
                                            pos
                                    )
                            ) > 64.0
                            || !player.level()
                            .getBlockState(
                                    pos
                            )
                            .is(
                                    OddityContent.ACCESSORY_WORKSHOP.get()
                            )) {
                        return;
                    }

                    InteractionHand hand =
                            payload.hand() == 1
                                    ? InteractionHand.OFF_HAND
                                    : InteractionHand.MAIN_HAND;

                    ItemStack stack =
                            player.getItemInHand(
                                    hand
                            );

                    if (!(stack.getItem()
                            instanceof AccessoryItem accessory)) {
                        return;
                    }

                    AccessoryKind kind =
                            accessory.kind();

                    if (!AccessoryCustomizationData.supported(
                            kind
                    )
                            || !kind.path()
                            .equals(
                                    payload.kind()
                            )) {
                        return;
                    }

                    AccessoryCustomizationData.Config safe =
                            AccessoryCustomizationData.sanitize(
                                    kind,
                                    new AccessoryCustomizationData.Config(
                                            payload.material(),
                                            payload.size(),
                                            payload.extras(),
                                            payload.woolColor()
                                    )
                            );

                    AccessoryCustomizationData.write(
                            stack,
                            kind,
                            safe
                    );

                    player.getInventory()
                            .setChanged();

                    player.displayClientMessage(
                            Component.translatable(
                                    "accessory.workshop.applied"
                            ),
                            true
                    );
                }
        );
    }
}
