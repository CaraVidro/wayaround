package net.caravidro.wayaround.network;

import java.util.ArrayList;
import java.util.List;
import net.caravidro.wayaround.client.FrostRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FrostPayload(ResourceLocation dimension, long chunk, boolean replace, List<Entry> entries)
        implements CustomPacketPayload {
    public record Entry(BlockPos pos, int state, int faces) {}
    public static final Type<FrostPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("wayaround", "frost"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FrostPayload> STREAM_CODEC = StreamCodec.of((buf, payload) -> {
        buf.writeResourceLocation(payload.dimension());
        buf.writeLong(payload.chunk());
        buf.writeBoolean(payload.replace());
        buf.writeVarInt(payload.entries().size());
        for (Entry entry : payload.entries()) {
            buf.writeBlockPos(entry.pos());
            buf.writeVarInt(entry.state());
            buf.writeVarInt(entry.faces());
        }
    }, buf -> {
        ResourceLocation dimension = buf.readResourceLocation();
        long chunk = buf.readLong();
        boolean replace = buf.readBoolean();
        int size = buf.readVarInt();
        if (size < 0 || size > 512) throw new IllegalArgumentException("Invalid frost batch size");
        List<Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) entries.add(new Entry(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt()));
        return new FrostPayload(dimension, chunk, replace, List.copyOf(entries));
    });

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(FrostPayload payload, IPayloadContext context) {
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            context.enqueueWork(() -> FrostRenderer.receive(payload));
        }
    }
}
