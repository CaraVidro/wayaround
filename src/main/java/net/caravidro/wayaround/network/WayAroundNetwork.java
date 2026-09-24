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

        registrar.playToServer(
                RecordingFinishedC2SPayload.TYPE,
                RecordingFinishedC2SPayload.STREAM_CODEC,
                RecordingFinishedC2SPayload::handle
        );

        registrar.playToServer(
                StartRecordingC2SPayload.TYPE,
                StartRecordingC2SPayload.STREAM_CODEC,
                StartRecordingC2SPayload::handle
        );

        registrar.playToClient(
                RecordingStartResultS2CPayload.TYPE,
                RecordingStartResultS2CPayload.STREAM_CODEC,
                RecordingStartResultS2CPayload::handle
        );

        registrar.playToServer(
                PhotoTakenC2SPayload.TYPE,
                PhotoTakenC2SPayload.STREAM_CODEC,
                PhotoTakenC2SPayload::handle
        );

        registrar.playToClient(
                RecordingReadyS2CPayload.TYPE,
                RecordingReadyS2CPayload.STREAM_CODEC,
                RecordingReadyS2CPayload::handle
        );

        registrar.playToServer(
                LabelRecordingC2SPayload.TYPE,
                LabelRecordingC2SPayload.STREAM_CODEC,
                LabelRecordingC2SPayload::handle
        );

        registrar.playToClient(
                PlacedCameraStartS2CPayload.TYPE,
                PlacedCameraStartS2CPayload.STREAM_CODEC,
                PlacedCameraStartS2CPayload::handle
        );

        registrar.playToClient(
                PlacedCameraPickupS2CPayload.TYPE,
                PlacedCameraPickupS2CPayload.STREAM_CODEC,
                PlacedCameraPickupS2CPayload::handle
        );
    }
}
