package net.caravidro.wayaround.network;

import net.caravidro.wayaround.dream.DreamPayload;
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
            "34";

    private WayAroundNetwork() {
    }

    public static void register(
            RegisterPayloadHandlersEvent event
    ) {

        /*
         * Keep one tiny channel mandatory so old/new WayAround jars still fail
         * fast on a real protocol mismatch. Feature channels are optional at
         * negotiation time: a missing feature registration must not strand a
         * remote client in CONFIGURATION until Minecraft's timeout expires.
         */
        PayloadRegistrar requiredRegistrar =
                event.registrar(PROTOCOL_VERSION);

        requiredRegistrar.playBidirectional(
                WayAroundProtocolPayload.TYPE,
                WayAroundProtocolPayload.STREAM_CODEC,
                WayAroundProtocolPayload::handle
        );

        requiredRegistrar.playToServer(net.caravidro.wayaround.security.XrayReportPayload.TYPE, net.caravidro.wayaround.security.XrayReportPayload.STREAM_CODEC, net.caravidro.wayaround.security.XrayReportPayload::handle);
        requiredRegistrar.playToClient(net.caravidro.wayaround.security.XrayChallengePayload.TYPE, net.caravidro.wayaround.security.XrayChallengePayload.STREAM_CODEC, (p,c) -> c.enqueueWork(() -> ClientPayloadBridge.xrayChallenge(p.nonce())));

        requiredRegistrar.playToServer(net.caravidro.wayaround.security.VisibilityReportPayload.TYPE, net.caravidro.wayaround.security.VisibilityReportPayload.STREAM_CODEC, net.caravidro.wayaround.security.VisibilityReportPayload::handle);
        requiredRegistrar.playToClient(net.caravidro.wayaround.security.VisibilityChallengePayload.TYPE, net.caravidro.wayaround.security.VisibilityChallengePayload.STREAM_CODEC, (p,c) -> c.enqueueWork(() -> ClientPayloadBridge.visibilityChallenge(p.nonce())));

        PayloadRegistrar registrar =
                requiredRegistrar.optional();

        registrar.playToServer(net.caravidro.wayaround.war.outpost.OutpostControlPayload.TYPE,net.caravidro.wayaround.war.outpost.OutpostControlPayload.STREAM_CODEC,net.caravidro.wayaround.war.outpost.OutpostControlPayload::handle);
        registrar.playToClient(net.caravidro.wayaround.war.outpost.OutpostViewPayload.TYPE,net.caravidro.wayaround.war.outpost.OutpostViewPayload.STREAM_CODEC,(p,c)->c.enqueueWork(()->ClientPayloadBridge.outpostView(p.target(),p.mount())));
        registrar.playToServer(FieldControlPayload.TYPE,FieldControlPayload.STREAM_CODEC,FieldControlPayload::handle);
        registrar.playToClient(EntitySpectateStatePayload.TYPE,EntitySpectateStatePayload.STREAM_CODEC,(p,c)->c.enqueueWork(()->ClientPayloadBridge.entitySpectate(p.target())));
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
                DeepSeaCapsuleControlC2SPayload.TYPE,
                DeepSeaCapsuleControlC2SPayload.STREAM_CODEC,
                DeepSeaCapsuleControlC2SPayload::handle
        );

        registrar.playToServer(
                DeepSeaSubmarineControlC2SPayload.TYPE,
                DeepSeaSubmarineControlC2SPayload.STREAM_CODEC,
                DeepSeaSubmarineControlC2SPayload::handle
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

        registrar.playToServer(
                AccessoryWorkshopApplyC2SPayload.TYPE,
                AccessoryWorkshopApplyC2SPayload.STREAM_CODEC,
                AccessoryWorkshopApplyC2SPayload::handle
        );

        registrar.playToClient(net.caravidro.wayaround.nature.NatureAmbientPayload.TYPE,net.caravidro.wayaround.nature.NatureAmbientPayload.STREAM_CODEC,ClientPayloadBridge::handleNatureAmbient);
        registrar.playToClient(CloudStormS2CPayload.TYPE,CloudStormS2CPayload.STREAM_CODEC,ClientPayloadBridge::handleCloudStorm);

        registrar.playToClient(FireFrameS2CPayload.TYPE, FireFrameS2CPayload.STREAM_CODEC, ClientPayloadBridge::handleFireFrame);

        registrar.playToClient(KrakenSceneS2CPayload.TYPE, KrakenSceneS2CPayload.STREAM_CODEC,
                ClientPayloadBridge::handleKrakenScene);

        registrar.playToClient(
                AccessoryStateS2CPayload.TYPE,
                AccessoryStateS2CPayload.STREAM_CODEC,
                ClientPayloadBridge::handleAccessoryState
        );

        registrar.playToClient(
                TopHatStateS2CPayload.TYPE,
                TopHatStateS2CPayload.STREAM_CODEC,
                TopHatStateS2CPayload::handle
        );

        registrar.playToClient(
                KrakenShakeS2CPayload.TYPE,
                KrakenShakeS2CPayload.STREAM_CODEC,
                KrakenShakeS2CPayload::handle
        );

        registrar.playToClient(
                TrouserPocketAnimationS2CPayload.TYPE,
                TrouserPocketAnimationS2CPayload.STREAM_CODEC,
                ClientPayloadBridge::handleTrouserPocketAnimation
        );

        registrar.playToClient(
                AccessoryWorkshopOpenS2CPayload.TYPE,
                AccessoryWorkshopOpenS2CPayload.STREAM_CODEC,
                ClientPayloadBridge::handleAccessoryWorkshopOpen
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
        registrar.playToClient(ThermalGlowPayload.TYPE, ThermalGlowPayload.STREAM_CODEC, ClientPayloadBridge::handleThermalGlow);
        registrar.playToClient(FugaArrowPayload.TYPE, FugaArrowPayload.STREAM_CODEC, ClientPayloadBridge::handleFugaArrow);
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
                ClientPayloadBridge::handleBlueVisual
        );

        registrar.playToClient(
                StructuralCollapseS2CPayload.TYPE,
                StructuralCollapseS2CPayload.STREAM_CODEC,
                ClientPayloadBridge::handleStructuralCollapse
        );

        registrar.playToClient(
                InfinityVisualPayload.TYPE,
                InfinityVisualPayload.STREAM_CODEC,
                ClientPayloadBridge::handleInfinityVisual
        );

        registrar.playToClient(
                BlueGestureS2CPayload.TYPE,
                BlueGestureS2CPayload.STREAM_CODEC,
                ClientPayloadBridge::handleBlueGesture
        );

        registrar.playToClient(
                BetaTechniqueVisualPayload.TYPE,
                BetaTechniqueVisualPayload.STREAM_CODEC,
                ClientPayloadBridge::handleBetaTechniqueVisual
        );

        registrar.playToClient(
                ImmortalWheelVisualPayload.TYPE,
                ImmortalWheelVisualPayload.STREAM_CODEC,
                ClientPayloadBridge::handleImmortalWheelVisual
        );

        registrar.playToClient(
                ImmortalWheelReactivationPayload.TYPE,
                ImmortalWheelReactivationPayload.STREAM_CODEC,
                ClientPayloadBridge::handleImmortalWheelReactivation
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
                AlexaVoiceCommandC2SPayload.TYPE,
                AlexaVoiceCommandC2SPayload.STREAM_CODEC,
                AlexaVoiceCommandC2SPayload::handle
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
                ClientPayloadBridge::handleTukunaPossession
        );

        registrar.playToClient(
                TukunaViewS2CPayload.TYPE,
                TukunaViewS2CPayload.STREAM_CODEC,
                ClientPayloadBridge::handleTukunaView
        );

        registrar.playToClient(
                TukunaMarkS2CPayload.TYPE,
                TukunaMarkS2CPayload.STREAM_CODEC,
                ClientPayloadBridge::handleTukunaMark
        );

        registrar.playToClient(
                TukunaSpeechVisualS2CPayload.TYPE,
                TukunaSpeechVisualS2CPayload.STREAM_CODEC,
                ClientPayloadBridge::handleTukunaSpeechVisual
        );

        registrar.playToClient(
                TukunaPossessionVisualS2CPayload.TYPE,
                TukunaPossessionVisualS2CPayload.STREAM_CODEC,
                ClientPayloadBridge::handleTukunaPossessionVisual
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

        registrar.playToServer(
                JujutsuMenuStateC2SPayload.TYPE,
                JujutsuMenuStateC2SPayload.STREAM_CODEC,
                JujutsuMenuStateC2SPayload::handle
        );

        registrar.playToClient(
                EnergyVisionS2CPayload.TYPE,
                EnergyVisionS2CPayload.STREAM_CODEC,
                ClientPayloadBridge::handleEnergyVision
        );

        registrar.playToClient(
                TukunaFugaVisualPayload.TYPE,
                TukunaFugaVisualPayload.STREAM_CODEC,
                ClientPayloadBridge::handleTukunaFugaVisual
        );

        registrar.playToClient(
                PlayerCinematicPayload.TYPE,
                PlayerCinematicPayload.STREAM_CODEC,
                ClientPayloadBridge::handlePlayerCinematic
        );

        registrar.playToClient(
                BattleMusicS2CPayload.TYPE,
                BattleMusicS2CPayload.STREAM_CODEC,
                ClientPayloadBridge::handleBattleMusic
        );

        registrar.playToClient(
                VoidDomainVisualPayload.TYPE,
                VoidDomainVisualPayload.STREAM_CODEC,
                ClientPayloadBridge::handleVoidDomainVisual
        );

        registrar.playToClient(
                JusticeDomainVisualPayload.TYPE,
                JusticeDomainVisualPayload.STREAM_CODEC,
                ClientPayloadBridge::handleJusticeDomainVisual
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

        registrar.playToServer(
                EngineeringBlueprintSaveC2SPayload.TYPE,
                EngineeringBlueprintSaveC2SPayload.STREAM_CODEC,
                EngineeringBlueprintSaveC2SPayload::handle
        );

        registrar.playToServer(
                CircuitWorkbenchActionC2SPayload.TYPE,
                CircuitWorkbenchActionC2SPayload.STREAM_CODEC,
                CircuitWorkbenchActionC2SPayload::handle
        );

        registrar.playToServer(
                BlueprintLabelC2SPayload.TYPE,
                BlueprintLabelC2SPayload.STREAM_CODEC,
                BlueprintLabelC2SPayload::handle
        );

        registrar.playToClient(
                BlueprintLabelOpenS2CPayload.TYPE,
                BlueprintLabelOpenS2CPayload.STREAM_CODEC,
                BlueprintLabelOpenS2CPayload::handle
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

        registrar.playToClient(net.caravidro.wayaround.daybreak.DaysBreakPayload.TYPE,
                net.caravidro.wayaround.daybreak.DaysBreakPayload.STREAM_CODEC,
                net.caravidro.wayaround.daybreak.DaysBreakPayload::handle);

        registrar.playToClient(
                NexusStateS2CPayload.TYPE,
                NexusStateS2CPayload.STREAM_CODEC,
                NexusStateS2CPayload::handle
        );

        registrar.playToClient(
                DomainIntroS2CPayload.TYPE,
                DomainIntroS2CPayload.STREAM_CODEC,
                ClientPayloadBridge::handleDomainIntro
        );

        registrar.playToClient(
                DreamPayload.TYPE,
                DreamPayload.CODEC,
                ClientPayloadBridge::handleDream
        );
    }
}

