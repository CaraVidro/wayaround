package net.caravidro.wayaround.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class WayAroundNetwork {

    /**
     * Bump this whenever the payload set/codec contract changes.
     *
     * Keeping this at "1" across many development builds allowed old and new
     * WayAround jars to claim network compatibility and then decode different
     * packet layouts after login.
     */
    public static final String PROTOCOL_VERSION =
            "11";

    private WayAroundNetwork() {
    }

    public static void register(
            RegisterPayloadHandlersEvent event
    ) {

        PayloadRegistrar registrar =
                event.registrar(PROTOCOL_VERSION);

        registrar.playToServer(SpectrumInputPayload.TYPE, SpectrumInputPayload.STREAM_CODEC, SpectrumInputPayload::handle);
        registrar.playToServer(
                SpectrumMeleeInputPayload.TYPE,
                SpectrumMeleeInputPayload.STREAM_CODEC,
                SpectrumMeleeInputPayload::handle
        );

        registrar.playToServer(
                AccessoryActionC2SPayload.TYPE,
                AccessoryActionC2SPayload.STREAM_CODEC,
                AccessoryActionC2SPayload::handle
        );

        registrar.playToServer(
                TopHatAdjustC2SPayload.TYPE,
                TopHatAdjustC2SPayload.STREAM_CODEC,
                TopHatAdjustC2SPayload::handle
        );

        registrar.playToServer(
                TrouserPocketRetrieveC2SPayload.TYPE,
                TrouserPocketRetrieveC2SPayload.STREAM_CODEC,
                TrouserPocketRetrieveC2SPayload::handle
        );

        registrar.playToClient(
                AccessoryStateS2CPayload.TYPE,
                AccessoryStateS2CPayload.STREAM_CODEC,
                AccessoryStateS2CPayload::handle
        );

        registrar.playToClient(
                TopHatStateS2CPayload.TYPE,
                TopHatStateS2CPayload.STREAM_CODEC,
                TopHatStateS2CPayload::handle
        );

        registrar.playToClient(
                TrouserPocketAnimationS2CPayload.TYPE,
                TrouserPocketAnimationS2CPayload.STREAM_CODEC,
                TrouserPocketAnimationS2CPayload::handle
        );

        registrar.playToClient(
                WindTestStateS2CPayload.TYPE,
                WindTestStateS2CPayload.STREAM_CODEC,
                WindTestStateS2CPayload::handle
        );

        registrar.playToClient(
                WorldFeatureConfigS2CPayload.TYPE,
                WorldFeatureConfigS2CPayload.STREAM_CODEC,
                WorldFeatureConfigS2CPayload::handle
        );
        registrar.playToClient(ThermalGlowPayload.TYPE, ThermalGlowPayload.STREAM_CODEC, ThermalGlowPayload::handle);
        registrar.playToClient(FugaArrowPayload.TYPE, FugaArrowPayload.STREAM_CODEC, FugaArrowPayload::handle);
        registrar.playToClient(FrostPayload.TYPE, FrostPayload.STREAM_CODEC, ClientPayloadBridge::handleFrost);

        registrar.playToClient(
                BlizzardStatePayload.TYPE,
                BlizzardStatePayload.STREAM_CODEC,
                ClientPayloadBridge::handleBlizzard
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

        registrar.playToServer(
                DomainPreludeC2SPayload.TYPE,
                DomainPreludeC2SPayload.STREAM_CODEC,
                DomainPreludeC2SPayload::handle
        );

        registrar.playToServer(
                TechniqueGestureC2SPayload.TYPE,
                TechniqueGestureC2SPayload.STREAM_CODEC,
                TechniqueGestureC2SPayload::handle
        );

        registrar.playToClient(
                TukunaPossessionS2CPayload.TYPE,
                TukunaPossessionS2CPayload.STREAM_CODEC,
                TukunaPossessionS2CPayload::handle
        );

        registrar.playToClient(
                TukunaViewS2CPayload.TYPE,
                TukunaViewS2CPayload.STREAM_CODEC,
                TukunaViewS2CPayload::handle
        );

        registrar.playToClient(
                TukunaMarkS2CPayload.TYPE,
                TukunaMarkS2CPayload.STREAM_CODEC,
                TukunaMarkS2CPayload::handle
        );

        registrar.playToClient(
                TukunaSpeechVisualS2CPayload.TYPE,
                TukunaSpeechVisualS2CPayload.STREAM_CODEC,
                TukunaSpeechVisualS2CPayload::handle
        );

        registrar.playToClient(
                TukunaPossessionVisualS2CPayload.TYPE,
                TukunaPossessionVisualS2CPayload.STREAM_CODEC,
                TukunaPossessionVisualS2CPayload::handle
        );

        registrar.playToClient(
                SpectrumUnlockS2CPayload.TYPE,
                SpectrumUnlockS2CPayload.STREAM_CODEC,
                SpectrumUnlockS2CPayload::handle
        );

        registrar.playToServer(
                JujutsuCastC2SPayload.TYPE,
                JujutsuCastC2SPayload.STREAM_CODEC,
                JujutsuCastC2SPayload::handle
        );

        registrar.playToClient(
                EnergyVisionS2CPayload.TYPE,
                EnergyVisionS2CPayload.STREAM_CODEC,
                EnergyVisionS2CPayload::handle
        );

        registrar.playToClient(
                TukunaFugaVisualPayload.TYPE,
                TukunaFugaVisualPayload.STREAM_CODEC,
                TukunaFugaVisualPayload::handle
        );

        registrar.playToClient(
                PlayerCinematicPayload.TYPE,
                PlayerCinematicPayload.STREAM_CODEC,
                PlayerCinematicPayload::handle
        );

        registrar.playToClient(
                VoidDomainVisualPayload.TYPE,
                VoidDomainVisualPayload.STREAM_CODEC,
                VoidDomainVisualPayload::handle
        );

        registrar.playToClient(
                JusticeDomainVisualPayload.TYPE,
                JusticeDomainVisualPayload.STREAM_CODEC,
                JusticeDomainVisualPayload::handle
        );

        registrar.playToServer(
                JusticeVoiceStatementC2SPayload.TYPE,
                JusticeVoiceStatementC2SPayload.STREAM_CODEC,
                JusticeVoiceStatementC2SPayload::handle
        );

        registrar.playToServer(
                TukunaVoiceStatementC2SPayload.TYPE,
                TukunaVoiceStatementC2SPayload.STREAM_CODEC,
                TukunaVoiceStatementC2SPayload::handle
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
                HerobrinePhotoSpawnC2SPayload.TYPE,
                HerobrinePhotoSpawnC2SPayload.STREAM_CODEC,
                HerobrinePhotoSpawnC2SPayload::handle
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
                MediaRecordingOfferS2CPayload.TYPE,
                MediaRecordingOfferS2CPayload.STREAM_CODEC,
                MediaRecordingOfferS2CPayload::handle
        );

        registrar.playToServer(
                MediaRecordingApproveC2SPayload.TYPE,
                MediaRecordingApproveC2SPayload.STREAM_CODEC,
                MediaRecordingApproveC2SPayload::handle
        );

        registrar.playToClient(
                MediaRecordingChunkS2CPayload.TYPE,
                MediaRecordingChunkS2CPayload.STREAM_CODEC,
                MediaRecordingChunkS2CPayload::handle
        );

        registrar.playToServer(
                BroadcastAmbientC2SPayload.TYPE,
                BroadcastAmbientC2SPayload.STREAM_CODEC,
                BroadcastAmbientC2SPayload::handle
        );

        registrar.playToClient(
                BroadcastAudioS2CPayload.TYPE,
                BroadcastAudioS2CPayload.STREAM_CODEC,
                BroadcastAudioS2CPayload::handle
        );

        registrar.playToClient(
                BroadcastImageS2CPayload.TYPE,
                BroadcastImageS2CPayload.STREAM_CODEC,
                BroadcastImageS2CPayload::handle
        );

        registrar.playToClient(
                BroadcastWorldSoundS2CPayload.TYPE,
                BroadcastWorldSoundS2CPayload.STREAM_CODEC,
                BroadcastWorldSoundS2CPayload::handle
        );

        registrar.playToClient(
                HerobrinePhotoModeS2CPayload.TYPE,
                HerobrinePhotoModeS2CPayload.STREAM_CODEC,
                HerobrinePhotoModeS2CPayload::handle
        );

        registrar.playToClient(
                NexusStateS2CPayload.TYPE,
                NexusStateS2CPayload.STREAM_CODEC,
                NexusStateS2CPayload::handle
        );

        registrar.playToClient(
                DomainIntroS2CPayload.TYPE,
                DomainIntroS2CPayload.STREAM_CODEC,
                DomainIntroS2CPayload::handle
        );
    }
}

