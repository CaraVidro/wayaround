package net.caravidro.wayaround.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EnergyVisionS2CPayload(boolean active, List<Entry> entries)
        implements CustomPacketPayload {

    public record Entry(UUID player, byte kind) {}

    public static final byte NORMAL_JUJUTSU = 1;
    public static final byte SPECTRUM = 2;

    public static final Type<EnergyVisionS2CPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WayAround.MODID, "energy_vision_s2c"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnergyVisionS2CPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> {
                buf.writeBoolean(payload.active());
                buf.writeVarInt(payload.entries().size());
                for (Entry entry : payload.entries()) {
                    buf.writeUUID(entry.player());
                    buf.writeByte(entry.kind());
                }
            }, buf -> {
                boolean active = buf.readBoolean();
                int size = buf.readVarInt();
                if (size < 0 || size > 512) {
                    throw new IllegalArgumentException("Invalid energy vision signature count: " + size);
                }
                List<Entry> entries = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    entries.add(new Entry(buf.readUUID(), buf.readByte()));
                }
                return new EnergyVisionS2CPayload(active, List.copyOf(entries));
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            EnergyVisionS2CPayload payload,
            IPayloadContext context
    ) {
        ClientPayloadBridge.handleEnergyVision(
                payload,
                context
        );
    }
}
