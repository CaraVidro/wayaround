package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.media.MediaContent;
import net.caravidro.wayaround.media.MediaInventory;
import net.caravidro.wayaround.media.broadcast.BroadcastManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StartRecordingC2SPayload()
        implements CustomPacketPayload {

    public static final Type<StartRecordingC2SPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "start_recording_c2s"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            StartRecordingC2SPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                    },
                    buf ->
                            new StartRecordingC2SPayload()
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            StartRecordingC2SPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> {
                    if (!(context.player()
                            instanceof ServerPlayer player)) {

                        return;
                    }

                    boolean holdingCamera =
                            player.getMainHandItem()
                                    .is(
                                            MediaContent.CAMERA.get()
                                    )
                                    || player.getOffhandItem()
                                    .is(
                                            MediaContent.CAMERA.get()
                                    );

                    if (!holdingCamera && !net.caravidro.wayaround.war.outpost.OutpostRemote.cameraActive(player)) {
                        PacketDistributor.sendToPlayer(
                                player,
                                new RecordingStartResultS2CPayload(
                                        false,
                                        ""
                                )
                        );
                        return;
                    }

                    if (!MediaInventory.consumeOne(
                            player,
                            MediaContent.FILM_ROLL.get()
                    )) {

                        PacketDistributor.sendToPlayer(
                                player,
                                new RecordingStartResultS2CPayload(
                                        false,
                                        "message.wayaround.media.need_film_roll"
                                )
                        );
                        return;
                    }

                    BroadcastManager.setHandheldCamera(
                            player,
                            true
                    );

                    PacketDistributor.sendToPlayer(
                            player,
                            new RecordingStartResultS2CPayload(
                                    true,
                                    ""
                            )
                    );
                }
        );
    }
}
