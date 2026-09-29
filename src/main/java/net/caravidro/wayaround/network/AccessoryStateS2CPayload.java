package net.caravidro.wayaround.network;

import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AccessoryStateS2CPayload(
        UUID player,
        String[] kinds,
        int[] wear,
        int[] glass,
        int glassesMode,
        ItemStack trouserPocket,
        int headMaterial,
        int headSize,
        int headExtras,
        int headWoolColor,
        int[] customColors
) implements CustomPacketPayload {

    public static final Type<AccessoryStateS2CPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "accessory_state_s2c"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            AccessoryStateS2CPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(
                                payload.player()
                        );

                        int count =
                                Math.min(
                                        payload.kinds().length,
                                        Math.min(
                                                payload.wear().length,
                                                payload.glass().length
                                        )
                                );

                        buf.writeVarInt(
                                count
                        );

                        for (int i = 0;
                             i < count;
                             i++) {
                            buf.writeUtf(
                                    payload.kinds()[i]
                            );

                            buf.writeVarInt(
                                    Math.max(
                                            0,
                                            payload.wear()[i]
                                    )
                            );

                            buf.writeByte(
                                    payload.glass()[i]
                            );
                        }

                        buf.writeVarInt(
                                payload.glassesMode()
                        );

                        boolean hasPocket =
                                payload.trouserPocket() != null
                                        && !payload.trouserPocket()
                                        .isEmpty();

                        buf.writeBoolean(
                                hasPocket
                        );

                        if (hasPocket) {
                            ItemStack.STREAM_CODEC.encode(
                                    buf,
                                    payload.trouserPocket()
                            );
                        }

                        buf.writeVarInt(
                                payload.headMaterial()
                        );
                        buf.writeVarInt(
                                payload.headSize()
                        );
                        buf.writeVarInt(
                                payload.headExtras()
                        );
                        buf.writeVarInt(
                                payload.headWoolColor()
                        );

                        buf.writeVarInt(
                                payload.customColors().length
                        );

                        for (int color :
                                payload.customColors()) {
                            buf.writeVarInt(
                                    color
                            );
                        }
                    },
                    buf -> {
                        UUID player =
                                buf.readUUID();

                        int count =
                                Math.min(
                                        32,
                                        Math.max(
                                                0,
                                                buf.readVarInt()
                                        )
                                );

                        String[] kinds =
                                new String[
                                        count
                                        ];

                        int[] wear =
                                new int[
                                        count
                                        ];

                        int[] glass =
                                new int[
                                        count
                                        ];

                        for (int i = 0;
                             i < count;
                             i++) {
                            kinds[i] =
                                    buf.readUtf();

                            wear[i] =
                                    buf.readVarInt();

                            glass[i] =
                                    buf.readByte();
                        }

                        int glassesMode =
                                buf.readVarInt();

                        ItemStack pocket =
                                buf.readBoolean()
                                        ? ItemStack.STREAM_CODEC.decode(
                                        buf
                                )
                                        : ItemStack.EMPTY;

                        int headMaterial =
                                buf.readVarInt();

                        int headSize =
                                buf.readVarInt();

                        int headExtras =
                                buf.readVarInt();

                        int headWoolColor =
                                buf.readVarInt();

                        int colorCount =
                                Math.min(
                                        32,
                                        Math.max(
                                                0,
                                                buf.readVarInt()
                                        )
                                );

                        int[] customColors =
                                new int[
                                        colorCount
                                        ];

                        for (int i = 0;
                             i < colorCount;
                             i++) {
                            customColors[i] =
                                    buf.readVarInt();
                        }

                        return new AccessoryStateS2CPayload(
                                player,
                                kinds,
                                wear,
                                glass,
                                glassesMode,
                                pocket,
                                headMaterial,
                                headSize,
                                headExtras,
                                headWoolColor,
                                customColors
                        );
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            AccessoryStateS2CPayload payload,
            IPayloadContext context
    ) {
        ClientPayloadBridge.handleAccessoryState(
                payload,
                context
        );
    }
}
