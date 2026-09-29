package net.caravidro.wayaround.network;

import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Small generic synchronization packet for player cinematics.
 *
 * The animation ID describes the body pose/reconstruction sequence. Camera
 * locking and shake are orthogonal so future abilities can reuse the same
 * system without inventing a second camera protocol.
 */
public record PlayerCinematicPayload(
        UUID player,
        byte animation,
        int durationTicks,
        boolean lockCamera,
        float shakeStrength
) implements CustomPacketPayload {

    public static final byte CLEAR = 0;
    public static final byte FUGA_CHARGE = 1;
    public static final byte FUGA_RELEASE = 2;
    public static final byte IMMORTAL_REBUILD = 3;
    public static final byte DESMARTELAR_CHARGE = 4;
    public static final byte DESMARTELAR_RELEASE = 5;
    public static final byte BLUE_CLAP = 6;
    public static final byte RED_HOLD = 7;
    public static final byte RED_RELEASE = 8;
    public static final byte PURPLE_FUSION = 9;
    public static final byte PURPLE_RELEASE = 10;
    public static final byte TUKUNA_TAKEOVER = 11;
    public static final byte TUKUNA_RETURN = 12;
    public static final byte DESMARTELAR_FIRE_CHARGE = 13;
    public static final byte TUKUNA_DOMAIN_PREVIEW = 14;
    public static final byte TUKUNA_FINGER_REACTION = 15;
    public static final byte TUKUNA_FORCE_FEED = 16;
    public static final byte TUKUNA_FORCED_EAT = 17;
    public static final byte MELEE_PUNCH = 18;
    public static final byte MELEE_BLOCK = 19;
    public static final byte MELEE_CATCH_ATTACKER = 20;
    public static final byte MELEE_CATCH_DEFENDER = 21;
    public static final byte MELEE_LAUNCH = 22;
    public static final byte MELEE_DOWNSLAM = 23;
    public static final byte MELEE_UPPERCUT = 24;
    public static final byte BLACK_FLASH_HEAVY = 25;
    public static final byte BLACK_FLASH_ULTIMATE = 26;
    public static final byte VOID_BATTLE_STANCE = 27;
    public static final byte TUKUNA_BATTLE_STANCE = 28;

    public static final Type<PlayerCinematicPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "player_cinematic"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            PlayerCinematicPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(payload.player());
                        buf.writeByte(payload.animation());
                        buf.writeVarInt(payload.durationTicks());
                        buf.writeBoolean(payload.lockCamera());
                        buf.writeFloat(payload.shakeStrength());
                    },
                    buf -> new PlayerCinematicPayload(
                            buf.readUUID(),
                            buf.readByte(),
                            buf.readVarInt(),
                            buf.readBoolean(),
                            buf.readFloat()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            PlayerCinematicPayload payload,
            IPayloadContext context
    ) {
        ClientPayloadBridge.handlePlayerCinematic(
                payload,
                context
        );
    }
}

