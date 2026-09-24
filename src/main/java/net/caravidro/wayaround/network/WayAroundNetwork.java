package net.caravidro.wayaround.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class WayAroundNetwork {

    private WayAroundNetwork() {
    }

    public static void register(
            RegisterPayloadHandlersEvent event
    ) {

        PayloadRegistrar registrar =
                event.registrar("1");

        registrar.playToClient(FrostPayload.TYPE, FrostPayload.STREAM_CODEC, FrostPayload::handle);

        registrar.playToClient(
                BlizzardStatePayload.TYPE,
                BlizzardStatePayload.STREAM_CODEC,
                BlizzardStatePayload::handle
        );

        registrar.playToServer(
                AssemblyEmptyHandPayload.TYPE,
                AssemblyEmptyHandPayload.STREAM_CODEC,
                AssemblyEmptyHandPayload::handle
        );

        registrar.playToServer(
                BlueScrollPayload.TYPE,
                BlueScrollPayload.STREAM_CODEC,
                BlueScrollPayload::handle
        );

        registrar.playToClient(
                BlueVisualPayload.TYPE,
                BlueVisualPayload.STREAM_CODEC,
                BlueVisualPayload::handle
        );

        registrar.playToServer(
                VoiceFrameC2SPayload.TYPE,
                VoiceFrameC2SPayload.STREAM_CODEC,
                VoiceFrameC2SPayload::handle
        );

        registrar.playToClient(
                VoiceFrameS2CPayload.TYPE,
                VoiceFrameS2CPayload.STREAM_CODEC,
                VoiceFrameS2CPayload::handle
        );
    }
}
