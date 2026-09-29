package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.jujutsu.JujutsuManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Mirrors the shared Shift+T ability-panel state to the authoritative server.
 * Black Flash and future combat mechanics can therefore require the player to
 * have actually awakened Jujutsu and intentionally entered ability mode.
 */
public record JujutsuMenuStateC2SPayload(
        boolean open
) implements CustomPacketPayload {

    public static final Type<JujutsuMenuStateC2SPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "jujutsu_menu_state"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            JujutsuMenuStateC2SPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) ->
                            buf.writeBoolean(
                                    payload.open()
                            ),
                    buf ->
                            new JujutsuMenuStateC2SPayload(
                                    buf.readBoolean()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            JujutsuMenuStateC2SPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> {
                    if (context.player()
                            instanceof ServerPlayer player) {
                        JujutsuManager.setAbilityPanelOpen(
                                player,
                                payload.open()
                        );
                    }
                }
        );
    }
}
