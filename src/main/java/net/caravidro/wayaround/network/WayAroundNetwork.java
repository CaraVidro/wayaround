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

        registrar.playToClient(
                InfinityVisualPayload.TYPE,
                InfinityVisualPayload.STREAM_CODEC,
                InfinityVisualPayload::handle
        );

        registrar.playToClient(
                BlueGestureS2CPayload.TYPE,
                BlueGestureS2CPayload.STREAM_CODEC,
                BlueGestureS2CPayload::handle
        );

        registrar.playToClient(
                BetaTechniqueVisualPayload.TYPE,
                BetaTechniqueVisualPayload.STREAM_CODEC,
                BetaTechniqueVisualPayload::handle
        );

        registrar.playToClient(
                ImmortalWheelVisualPayload.TYPE,
                ImmortalWheelVisualPayload.STREAM_CODEC,
                ImmortalWheelVisualPayload::handle
        );

        registrar.playToClient(
                ImmortalWheelReactivationPayload.TYPE,
                ImmortalWheelReactivationPayload.STREAM_CODEC,
                ImmortalWheelReactivationPayload::handle
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
                VoiceIntentC2SPayload.TYPE,
                VoiceIntentC2SPayload.STREAM_CODEC,
                VoiceIntentC2SPayload::handle
        );

        registrar.playToClient(
                TukunaPossessionS2CPayload.TYPE,
                TukunaPossessionS2CPayload.STREAM_CODEC,
                TukunaPossessionS2CPayload::handle
        );

        registrar.playToClient(
                VoidDomainVisualPayload.TYPE,
                VoidDomainVisualPayload.STREAM_CODEC,
                VoidDomainVisualPayload::handle
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

        registrar.playToServer(
                MediaRecordingUploadC2SPayload.TYPE,
                MediaRecordingUploadC2SPayload.STREAM_CODEC,
                MediaRecordingUploadC2SPayload::handle
        );

        registrar.playToServer(
                MediaRecordingRequestC2SPayload.TYPE,
                MediaRecordingRequestC2SPayload.STREAM_CODEC,
                MediaRecordingRequestC2SPayload::handle
        );

        registrar.playToClient(
                MediaRecordingChunkS2CPayload.TYPE,
                MediaRecordingChunkS2CPayload.STREAM_CODEC,
                MediaRecordingChunkS2CPayload::handle
        );
    }
}
