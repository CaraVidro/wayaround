package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.industrial.engineering.EngineeringBlueprintData;
import net.caravidro.wayaround.industrial.engineering.EngineeringWorkbenchBlockEntity;
import net.caravidro.wayaround.industrial.power.PowerContent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EngineeringBlueprintSaveC2SPayload(
        long workbenchPos,
        CompoundTag project
) implements CustomPacketPayload {

    public static final Type<EngineeringBlueprintSaveC2SPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "engineering_blueprint_save_c2s"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            EngineeringBlueprintSaveC2SPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeLong(
                                payload.workbenchPos()
                        );
                        buf.writeNbt(
                                payload.project()
                        );
                    },
                    buf -> {
                        long pos =
                                buf.readLong();

                        CompoundTag tag =
                                buf.readNbt();

                        return new EngineeringBlueprintSaveC2SPayload(
                                pos,
                                tag == null
                                        ? new CompoundTag()
                                        : tag
                        );
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            EngineeringBlueprintSaveC2SPayload payload,
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
                                    payload.workbenchPos()
                            );

                    if (player.distanceToSqr(
                            pos.getX() + 0.5,
                            pos.getY() + 0.5,
                            pos.getZ() + 0.5
                    ) > 64.0
                            || !(player.level()
                            .getBlockEntity(
                                    pos
                            ) instanceof EngineeringWorkbenchBlockEntity)) {
                        return;
                    }

                    CompoundTag project =
                            EngineeringBlueprintData.sanitizeProject(
                                    payload.project()
                            );

                    ItemStack target =
                            heldBlueprint(
                                    player
                            );

                    boolean created =
                            target.isEmpty();

                    if (created) {
                        target =
                                new ItemStack(
                                        PowerContent.ENGINEERING_BLUEPRINT.get()
                                );
                    }

                    EngineeringBlueprintData.writeProject(
                            target,
                            project,
                            pos
                    );

                    if (created) {
                        if (!player.getInventory()
                                .add(
                                        target
                                )) {
                            player.drop(
                                    target,
                                    false
                            );
                        }
                    }

                    player.getInventory()
                            .setChanged();

                    player.displayClientMessage(
                            Component.translatable(
                                    created
                                            ? "message.wayaround.engineering_blueprint.created"
                                            : "message.wayaround.engineering_blueprint.updated"
                            ),
                            true
                    );
                }
        );
    }

    private static ItemStack heldBlueprint(
            ServerPlayer player
    ) {
        ItemStack main =
                player.getMainHandItem();

        if (main.is(
                PowerContent.ENGINEERING_BLUEPRINT.get()
        )) {
            return main;
        }

        ItemStack off =
                player.getOffhandItem();

        if (off.is(
                PowerContent.ENGINEERING_BLUEPRINT.get()
        )) {
            return off;
        }

        return ItemStack.EMPTY;
    }
}
