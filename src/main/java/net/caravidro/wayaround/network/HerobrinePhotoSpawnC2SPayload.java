package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.oldfriend.OldFriendManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record HerobrinePhotoSpawnC2SPayload(
        int x,
        int y,
        int z
) implements CustomPacketPayload {

    public static final Type<HerobrinePhotoSpawnC2SPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "herobrine_photo_spawn_c2s"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            HerobrinePhotoSpawnC2SPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeInt(
                                payload.x()
                        );
                        buf.writeInt(
                                payload.y()
                        );
                        buf.writeInt(
                                payload.z()
                        );
                    },
                    buf ->
                            new HerobrinePhotoSpawnC2SPayload(
                                    buf.readInt(),
                                    buf.readInt(),
                                    buf.readInt()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            HerobrinePhotoSpawnC2SPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> {
                    if (!(context.player()
                            instanceof ServerPlayer player)) {
                        return;
                    }

                    OldFriendManager.spawnPhotoApparition(
                            player,
                            new BlockPos(
                                    payload.x(),
                                    payload.y(),
                                    payload.z()
                            )
                    );
                }
        );
    }
}
