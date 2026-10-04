package net.caravidro.wayaround.security;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record XrayChallengePayload(long nonce) implements CustomPacketPayload {
    public static final Type<XrayChallengePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("wayaround", "xray_challenge"));
    public static final StreamCodec<RegistryFriendlyByteBuf, XrayChallengePayload> STREAM_CODEC = StreamCodec.of((b,p) -> b.writeLong(p.nonce), b -> new XrayChallengePayload(b.readLong()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
