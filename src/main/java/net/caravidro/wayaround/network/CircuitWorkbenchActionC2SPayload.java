package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.industrial.electronics.CircuitBoardData;
import net.caravidro.wayaround.industrial.electronics.ElectronicsWorkbenchBlockEntity;
import net.caravidro.wayaround.industrial.electronics.ElectronicsWorkbenchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CircuitWorkbenchActionC2SPayload(
        long workbenchPos,
        int action,
        int componentOrdinal,
        int a,
        int b
) implements CustomPacketPayload {

    public static final int PLACE = 0;
    public static final int CONNECT = 1;
    public static final int REMOVE = 2;
    public static final int UPGRADE = 3;

    public static final Type<CircuitWorkbenchActionC2SPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "circuit_workbench_action_c2s"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            CircuitWorkbenchActionC2SPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeLong(
                                payload.workbenchPos()
                        );
                        buf.writeVarInt(
                                payload.action()
                        );
                        buf.writeVarInt(
                                payload.componentOrdinal()
                        );
                        buf.writeVarInt(
                                payload.a()
                        );
                        buf.writeVarInt(
                                payload.b()
                        );
                    },
                    buf -> new CircuitWorkbenchActionC2SPayload(
                            buf.readLong(),
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
            CircuitWorkbenchActionC2SPayload payload,
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

                    if (!(player.containerMenu
                            instanceof ElectronicsWorkbenchMenu menu)
                            || !menu.workbenchPos()
                            .equals(
                                    pos
                            )
                            || player.distanceToSqr(
                            pos.getX() + 0.5,
                            pos.getY() + 0.5,
                            pos.getZ() + 0.5
                    ) > 64.0
                            || !(player.level()
                            .getBlockEntity(
                                    pos
                            ) instanceof ElectronicsWorkbenchBlockEntity workbench)) {
                        return;
                    }

                    switch (payload.action()) {
                        case PLACE -> {
                            CircuitBoardData.ComponentType[] values =
                                    CircuitBoardData.ComponentType.values();

                            if (payload.componentOrdinal() <= 0
                                    || payload.componentOrdinal() >= values.length) {
                                return;
                            }

                            workbench.placeComponent(
                                    player,
                                    values[payload.componentOrdinal()],
                                    payload.a()
                            );
                        }

                        case CONNECT -> workbench.connect(
                                player,
                                payload.a(),
                                payload.b()
                        );

                        case REMOVE -> workbench.removeCell(
                                player,
                                payload.a()
                        );

                        case UPGRADE -> workbench.upgradeBoard(
                                player
                        );

                        default -> {
                        }
                    }
                }
        );
    }
}
