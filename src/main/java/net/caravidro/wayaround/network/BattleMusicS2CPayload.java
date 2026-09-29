package net.caravidro.wayaround.network;

import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.client.sound.BattleThemeSound;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Synchronizes the battle theme.
 *
 * Participants receive a direct, non-positional mix. Nearby observers receive
 * one spatial source per fighter. STOP terminates every instance for the
 * battle immediately.
 */
public record BattleMusicS2CPayload(
        UUID battle,
        UUID source,
        byte mode
) implements CustomPacketPayload {

    public static final byte START_DIRECT = 0;
    public static final byte START_SPATIAL = 1;
    public static final byte STOP = 2;

    public static final Type<BattleMusicS2CPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "battle_music"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            BattleMusicS2CPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(payload.battle());
                        buf.writeUUID(payload.source());
                        buf.writeByte(payload.mode());
                    },
                    buf -> new BattleMusicS2CPayload(
                            buf.readUUID(),
                            buf.readUUID(),
                            buf.readByte()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            BattleMusicS2CPayload payload,
            IPayloadContext context
    ) {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }

        context.enqueueWork(
                () -> {
                    if (payload.mode()
                            == STOP) {
                        BattleThemeSound.stopBattle(
                                payload.battle()
                        );
                        return;
                    }

                    BattleThemeSound.play(
                            payload.battle(),
                            payload.source(),
                            payload.mode()
                                    == START_DIRECT
                    );
                }
        );
    }
}
