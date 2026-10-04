package net.caravidro.wayaround.security;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record XrayReportPayload(long nonce, ResourceEvidence evidence) implements CustomPacketPayload {
    public static final Type<XrayReportPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("wayaround", "xray_report"));
    public static final StreamCodec<RegistryFriendlyByteBuf, XrayReportPayload> STREAM_CODEC = StreamCodec.of((b,p) -> {
        b.writeLong(p.nonce); b.writeInt(p.evidence.transparent()); b.writeInt(p.evidence.hiddenModels());
        b.writeInt(p.evidence.opaqueOres()); b.writeInt(p.evidence.inspected()); b.writeUtf(p.evidence.fingerprint(),64);
    }, b -> new XrayReportPayload(b.readLong(),new ResourceEvidence(b.readInt(),b.readInt(),b.readInt(),b.readInt(),b.readUtf(64))));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(XrayReportPayload p, IPayloadContext context) {
        context.enqueueWork(() -> { if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) AntiXrayService.receive(player,p); });
    }
}
