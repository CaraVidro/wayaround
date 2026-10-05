package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.environment.EnvironmentalFieldClientCache;
import net.caravidro.wayaround.environment.EnvironmentalFields;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Quantized sparse environmental cells around one player.
 */
public record EnvironmentalFieldS2CPayload(
        long[] keys,
        byte[] fields
) implements CustomPacketPayload {

    private static final int FIELD_COUNT =
            7;

    private static final int MAX_CELLS =
            49;

    public static final Type<EnvironmentalFieldS2CPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "environmental_fields"
                    )
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, EnvironmentalFieldS2CPayload> STREAM_CODEC =
            StreamCodec.of(
                    EnvironmentalFieldS2CPayload::write,
                    EnvironmentalFieldS2CPayload::read
            );

    private static void write(
            RegistryFriendlyByteBuf buffer,
            EnvironmentalFieldS2CPayload payload
    ) {
        int count =
                Math.min(
                        MAX_CELLS,
                        payload.keys == null
                                ? 0
                                : payload.keys.length
                );

        buffer.writeVarInt(
                count
        );

        for (int index = 0;
             index < count;
             index++) {

            buffer.writeLong(
                    payload.keys[index]
            );

            int offset =
                    index
                            * FIELD_COUNT;

            for (int field = 0;
                 field < FIELD_COUNT;
                 field++) {
                buffer.writeByte(
                        payload.fields[
                                offset + field
                        ]
                );
            }
        }
    }

    private static EnvironmentalFieldS2CPayload read(
            RegistryFriendlyByteBuf buffer
    ) {
        int count =
                Math.clamp(
                        buffer.readVarInt(),
                        0,
                        MAX_CELLS
                );

        long[] keys =
                new long[
                        count
                ];

        byte[] fields =
                new byte[
                        count
                                * FIELD_COUNT
                ];

        for (int index = 0;
             index < count;
             index++) {

            keys[index] =
                    buffer.readLong();

            int offset =
                    index
                            * FIELD_COUNT;

            for (int field = 0;
                 field < FIELD_COUNT;
                 field++) {
                fields[
                        offset + field
                ] =
                        buffer.readByte();
            }
        }

        return new EnvironmentalFieldS2CPayload(
                keys,
                fields
        );
    }

    public static EnvironmentalFieldS2CPayload single(
            long key,
            EnvironmentalFields.Snapshot snapshot
    ) {
        return batch(
                new long[] {
                        key
                },
                new EnvironmentalFields.Snapshot[] {
                        snapshot
                }
        );
    }

    public static EnvironmentalFieldS2CPayload batch(
            long[] keys,
            EnvironmentalFields.Snapshot[] snapshots
    ) {
        int count =
                Math.min(
                        MAX_CELLS,
                        Math.min(
                                keys == null
                                        ? 0
                                        : keys.length,
                                snapshots == null
                                        ? 0
                                        : snapshots.length
                        )
                );

        long[] safeKeys =
                new long[
                        count
                ];

        byte[] fields =
                new byte[
                        count
                                * FIELD_COUNT
                ];

        for (int index = 0;
             index < count;
             index++) {

            safeKeys[index] =
                    keys[index];

            EnvironmentalFields.Snapshot state =
                    snapshots[index];

            int offset =
                    index
                            * FIELD_COUNT;

            fields[offset] =
                    encode(
                            state.humidity()
                    );

            fields[offset + 1] =
                    encode(
                            state.cloudWater()
                    );

            fields[offset + 2] =
                    encode(
                            state.soilMoisture()
                    );

            fields[offset + 3] =
                    encode(
                            state.snowBudget()
                    );

            fields[offset + 4] =
                    encode(
                            state.smoke()
                    );

            fields[offset + 5] =
                    encode(
                            state.pollution()
                    );

            fields[offset + 6] =
                    encode(
                            state.waterAvailability()
                    );
        }

        return new EnvironmentalFieldS2CPayload(
                safeKeys,
                fields
        );
    }

    public boolean sane() {
        return keys != null
                && fields != null
                && keys.length <= MAX_CELLS
                && fields.length
                == keys.length
                        * FIELD_COUNT;
    }

    public static void handle(
            EnvironmentalFieldS2CPayload payload,
            IPayloadContext context
    ) {
        if (!payload.sane()) {
            return;
        }

        context.enqueueWork(
                () -> {
                    for (int index = 0;
                         index < payload.keys.length;
                         index++) {

                        int offset =
                                index
                                        * FIELD_COUNT;

                        EnvironmentalFieldClientCache.receive(
                                payload.keys[index],
                                new EnvironmentalFields.Snapshot(
                                        decode(
                                                payload.fields[offset]
                                        ),
                                        decode(
                                                payload.fields[offset + 1]
                                        ),
                                        decode(
                                                payload.fields[offset + 2]
                                        ),
                                        decode(
                                                payload.fields[offset + 3]
                                        ),
                                        decode(
                                                payload.fields[offset + 4]
                                        ),
                                        decode(
                                                payload.fields[offset + 5]
                                        ),
                                        decode(
                                                payload.fields[offset + 6]
                                        )
                                )
                        );
                    }
                }
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static byte encode(
            float value
    ) {
        int quantized =
                Math.round(
                        Math.clamp(
                                value,
                                0.0F,
                                1.0F
                        )
                                * 255.0F
                );

        return (byte) (
                quantized
                        & 0xff
        );
    }

    private static float decode(
            byte value
    ) {
        return (
                value
                        & 0xff
        )
                / 255.0F;
    }
}
