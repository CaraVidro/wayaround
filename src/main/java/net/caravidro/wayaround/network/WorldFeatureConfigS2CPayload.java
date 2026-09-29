package net.caravidro.wayaround.network;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.worldconfig.WaveMode;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.caravidro.wayaround.worldconfig.WorldFeatureSettings;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WorldFeatureConfigS2CPayload(
        long enabledMask,
        int waveMode
) implements CustomPacketPayload {

    public static final Type<WorldFeatureConfigS2CPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "world_feature_config_s2c"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            WorldFeatureConfigS2CPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeLong(
                                payload.enabledMask()
                        );

                        buf.writeVarInt(
                                payload.waveMode()
                        );
                    },
                    buf ->
                            new WorldFeatureConfigS2CPayload(
                                    buf.readLong(),
                                    buf.readVarInt()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            WorldFeatureConfigS2CPayload payload,
            IPayloadContext context
    ) {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }

        context.enqueueWork(
                () -> {
                    WorldFeatureSettings settings =
                            WorldFeatureSettings.fromMask(
                                    payload.enabledMask()
                            );

                    settings.setWaveMode(
                            WaveMode.fromNetwork(
                                    payload.waveMode()
                            )
                    );

                    WorldFeatureRuntime.applyClient(
                            settings
                    );
                }
        );
    }
}
