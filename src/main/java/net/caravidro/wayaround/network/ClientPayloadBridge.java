package net.caravidro.wayaround.network;

import java.util.Objects;
import java.util.function.Consumer;

import net.caravidro.wayaround.dream.DreamPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Ponte common-safe para payloads que só produzem efeitos no cliente.
 *
 * <p>Esta classe não pode importar nenhuma classe de {@code net.minecraft.client}
 * nem classes de renderização/tela do Way Around. No dedicated server os
 * handlers permanecem no-op; no cliente, {@code WayAroundClient} instala as
 * implementações reais durante a inicialização física do cliente.</p>
 */
public final class ClientPayloadBridge {

    private static final Consumer<FrostPayload> NOOP_FROST = payload -> {};
    private static final Consumer<BlizzardStatePayload> NOOP_BLIZZARD = payload -> {};
    private static final Consumer<CalvingNetwork.CalvingShakePayload> NOOP_CALVING = payload -> {};

    private static volatile Consumer<FrostPayload> frostHandler = NOOP_FROST;
    private static volatile Consumer<BlizzardStatePayload> blizzardHandler = NOOP_BLIZZARD;
    private static volatile Consumer<CalvingNetwork.CalvingShakePayload> calvingHandler = NOOP_CALVING;

    public static void visibilityChallenge(long nonce) { realtimeHandlers.visibilityChallenge(nonce); }
    public static void xrayChallenge(long nonce) { realtimeHandlers.xrayChallenge(nonce); }

    public interface RealtimeClientHandlers {
        default void broadcastImage(
                BroadcastImageS2CPayload payload
        ) {}

        default void broadcastAudio(
                BroadcastAudioS2CPayload payload
        ) {}

        default void broadcastWorldSound(
                BroadcastWorldSoundS2CPayload payload
        ) {}

        default void mediaChunk(
                MediaRecordingChunkS2CPayload payload
        ) {}

        default void mediaOffer(
                MediaRecordingOfferS2CPayload payload
        ) {}

        default void placedCameraStart(
                PlacedCameraStartS2CPayload payload
        ) {}

        default void placedCameraPickup(
                PlacedCameraPickupS2CPayload payload
        ) {}

        default void recordingStartResult(
                RecordingStartResultS2CPayload payload
        ) {}

        default void recordingReady(
                RecordingReadyS2CPayload payload
        ) {}

        default void voiceFrame(
                VoiceFrameS2CPayload payload
        ) {}

        default void herobrinePhotoMode(
                HerobrinePhotoModeS2CPayload payload
        ) {}

        default void nexusState(
                NexusStateS2CPayload payload
        ) {}

        default void topHatState(
                TopHatStateS2CPayload payload
        ) {}

        default void cloudStorm(CloudStormS2CPayload payload) {}
        default void daysBreak(net.caravidro.wayaround.daybreak.DaysBreakPayload payload) {}
        default void natureAmbient(net.caravidro.wayaround.nature.NatureAmbientPayload payload) {}

        default void fireFrame(FireFrameS2CPayload payload) {}
        default void entitySpectate(int target) {}
        default void outpostView(int target,long mount) {}
        default void xrayChallenge(long nonce) {}
        default void visibilityChallenge(long nonce) {}

        default void krakenScene(KrakenSceneS2CPayload payload) {}

        default void krakenShake(
                KrakenShakeS2CPayload payload
        ) {}

        default void accessoryState(
                AccessoryStateS2CPayload payload
        ) {}

        default void trouserPocketAnimation(
                TrouserPocketAnimationS2CPayload payload
        ) {}

        default void accessoryWorkshopOpen(
                AccessoryWorkshopOpenS2CPayload payload
        ) {}

        default void blueprintLabelOpen(
                BlueprintLabelOpenS2CPayload payload
        ) {}

        default void thermalGlow(
                ThermalGlowPayload payload
        ) {}

        default void fugaArrow(
                FugaArrowPayload payload
        ) {}

        default void structuralCollapse(
                StructuralCollapseS2CPayload payload
        ) {}

        default void blueVisual(
                BlueVisualPayload payload
        ) {}

        default void infinityVisual(
                InfinityVisualPayload payload
        ) {}

        default void blueGesture(
                BlueGestureS2CPayload payload
        ) {}

        default void betaTechniqueVisual(
                BetaTechniqueVisualPayload payload
        ) {}

        default void immortalWheelVisual(
                ImmortalWheelVisualPayload payload
        ) {}

        default void immortalWheelReactivation(
                ImmortalWheelReactivationPayload payload
        ) {}

        default void tukunaPossession(
                TukunaPossessionS2CPayload payload
        ) {}

        default void tukunaView(
                TukunaViewS2CPayload payload
        ) {}

        default void tukunaMark(
                TukunaMarkS2CPayload payload
        ) {}

        default void tukunaSpeechVisual(
                TukunaSpeechVisualS2CPayload payload
        ) {}

        default void tukunaPossessionVisual(
                TukunaPossessionVisualS2CPayload payload
        ) {}

        default void energyVision(
                EnergyVisionS2CPayload payload
        ) {}

        default void tukunaFugaVisual(
                TukunaFugaVisualPayload payload
        ) {}

        default void playerCinematic(
                PlayerCinematicPayload payload
        ) {}

        default void battleMusic(
                BattleMusicS2CPayload payload
        ) {}

        default void voidDomainVisual(
                VoidDomainVisualPayload payload
        ) {}

        default void justiceDomainVisual(
                JusticeDomainVisualPayload payload
        ) {}

        default void domainIntro(
                DomainIntroS2CPayload payload
        ) {}

        default void dream(
                DreamPayload payload
        ) {}
    }

    private static final RealtimeClientHandlers NOOP_REALTIME =
            new RealtimeClientHandlers() {};

    private static volatile RealtimeClientHandlers realtimeHandlers =
            NOOP_REALTIME;

    private ClientPayloadBridge() {
    }

    public static void install(
            Consumer<FrostPayload> frost,
            Consumer<BlizzardStatePayload> blizzard,
            Consumer<CalvingNetwork.CalvingShakePayload> calving
    ) {
        frostHandler = Objects.requireNonNull(frost, "frost");
        blizzardHandler = Objects.requireNonNull(blizzard, "blizzard");
        calvingHandler = Objects.requireNonNull(calving, "calving");
    }

    public static void handleFrost(FrostPayload payload, IPayloadContext context) {
        frostHandler.accept(payload);
    }

    public static void handleBlizzard(BlizzardStatePayload payload, IPayloadContext context) {
        blizzardHandler.accept(payload);
    }

    public static void handleCalving(
            CalvingNetwork.CalvingShakePayload payload,
            IPayloadContext context
    ) {
        calvingHandler.accept(payload);
    }

    public static void installRealtime(
            RealtimeClientHandlers handlers
    ) {
        realtimeHandlers =
                Objects.requireNonNull(
                        handlers,
                        "handlers"
                );
    }

    public static void handleBroadcastImage(
            BroadcastImageS2CPayload payload,
            IPayloadContext context
    ) {
        if (payload.width()
                != MediaNetworkLimits.BROADCAST_WIDTH
                || payload.height()
                != MediaNetworkLimits.BROADCAST_HEIGHT
                || payload.rgb() == null
                || payload.rgb().length
                != MediaNetworkLimits.BROADCAST_RGB_BYTES
                || !Float.isFinite(
                payload.quality()
        )) {
            return;
        }

        context.enqueueWork(
                () -> realtimeHandlers.broadcastImage(
                        payload
                )
        );
    }

    public static void handleBroadcastAudio(
            BroadcastAudioS2CPayload payload,
            IPayloadContext context
    ) {
        if (payload.pcm() == null
                || payload.pcm().length == 0
                || payload.pcm().length
                > net.caravidro.wayaround.voice.VoiceConstants.MAX_PACKET_BYTES
                || !Float.isFinite(
                payload.quality()
        )
                || !Float.isFinite(
                payload.volume()
        )) {
            return;
        }

        context.enqueueWork(
                () -> realtimeHandlers.broadcastAudio(
                        payload
                )
        );
    }

    public static void handleBroadcastWorldSound(
            BroadcastWorldSoundS2CPayload payload,
            IPayloadContext context
    ) {
        if (payload.sound() == null
                || payload.sound().isBlank()
                || payload.source() == null
                || !Float.isFinite(
                payload.volume()
        )
                || !Float.isFinite(
                payload.pitch()
        )
                || !Float.isFinite(
                payload.quality()
        )) {
            return;
        }

        context.enqueueWork(
                () -> realtimeHandlers.broadcastWorldSound(
                        payload
                )
        );
    }

    public static void handleMediaChunk(
            MediaRecordingChunkS2CPayload payload,
            IPayloadContext context
    ) {
        if (payload.data() == null
                || payload.data().length == 0
                || payload.data().length
                > MediaNetworkLimits.DOWNLOAD_CHUNK
                || payload.totalLength() <= 0L
                || payload.totalLength()
                > MediaNetworkLimits.MAX_RECORDING_BYTES
                || payload.offerToken() == 0L
                || payload.offset() < 0L
                || payload.offset()
                + payload.data().length
                > payload.totalLength()) {
            return;
        }

        context.enqueueWork(
                () -> realtimeHandlers.mediaChunk(
                        payload
                )
        );
    }

    public static void handleMediaOffer(
            MediaRecordingOfferS2CPayload payload,
            IPayloadContext context
    ) {
        if (payload.totalLength() <= 0L
                || payload.totalLength()
                > MediaNetworkLimits.MAX_RECORDING_BYTES
                || payload.offerToken() == 0L) {
            return;
        }

        context.enqueueWork(
                () -> realtimeHandlers.mediaOffer(
                        payload
                )
        );
    }

    public static void handlePlacedCameraStart(
            PlacedCameraStartS2CPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> realtimeHandlers.placedCameraStart(
                        payload
                )
        );
    }

    public static void handlePlacedCameraPickup(
            PlacedCameraPickupS2CPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> realtimeHandlers.placedCameraPickup(
                        payload
                )
        );
    }

    public static void handleRecordingStartResult(
            RecordingStartResultS2CPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> realtimeHandlers.recordingStartResult(
                        payload
                )
        );
    }

    public static void handleRecordingReady(
            RecordingReadyS2CPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> realtimeHandlers.recordingReady(
                        payload
                )
        );
    }

    public static void handleTopHatState(
            TopHatStateS2CPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> realtimeHandlers.topHatState(
                        payload
                )
        );
    }

    public static void handleNatureAmbient(net.caravidro.wayaround.nature.NatureAmbientPayload p,IPayloadContext context){if(p.sane())context.enqueueWork(()->realtimeHandlers.natureAmbient(p));}

    public static void handleCloudStorm(CloudStormS2CPayload payload,IPayloadContext context){
        if(payload.isSane())context.enqueueWork(()->realtimeHandlers.cloudStorm(payload));
    }

    public static void handleFireFrame(FireFrameS2CPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> realtimeHandlers.fireFrame(payload));
    }

    public static void handleKrakenScene(KrakenSceneS2CPayload payload, IPayloadContext context) {
        if (payload.isSane()) context.enqueueWork(() -> realtimeHandlers.krakenScene(payload));
    }

    public static void handleKrakenShake(
            KrakenShakeS2CPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> realtimeHandlers.krakenShake(
                        payload
                )
        );
    }

    public static void daysBreak(net.caravidro.wayaround.daybreak.DaysBreakPayload payload){realtimeHandlers.daysBreak(payload);}

    public static void handleNexusState(
            NexusStateS2CPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> realtimeHandlers.nexusState(
                        payload
                )
        );
    }

    public static void handleHerobrinePhotoMode(
            HerobrinePhotoModeS2CPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> realtimeHandlers.herobrinePhotoMode(
                        payload
                )
        );
    }

    public static void handleVoiceFrame(
            VoiceFrameS2CPayload payload,
            IPayloadContext context
    ) {
        if (payload.pcm() == null
                || payload.pcm().length == 0
                || payload.pcm().length
                > net.caravidro.wayaround.voice.VoiceConstants.MAX_PACKET_BYTES) {
            return;
        }

        context.enqueueWork(
                () -> realtimeHandlers.voiceFrame(
                        payload
                )
        );
    }


    public static void handleAccessoryState(
            AccessoryStateS2CPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> realtimeHandlers.accessoryState(payload));
    }

    public static void handleTrouserPocketAnimation(
            TrouserPocketAnimationS2CPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> realtimeHandlers.trouserPocketAnimation(payload));
    }

    public static void handleAccessoryWorkshopOpen(
            AccessoryWorkshopOpenS2CPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> realtimeHandlers.accessoryWorkshopOpen(payload));
    }

    public static void handleBlueprintLabelOpen(
            BlueprintLabelOpenS2CPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> realtimeHandlers.blueprintLabelOpen(payload));
    }

    public static void handleThermalGlow(
            ThermalGlowPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> realtimeHandlers.thermalGlow(payload));
    }

    public static void handleFugaArrow(
            FugaArrowPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> realtimeHandlers.fugaArrow(payload));
    }

    public static void handleStructuralCollapse(
            StructuralCollapseS2CPayload payload,
            IPayloadContext context
    ) {
        if (payload == null
                || !payload.isSane()) {
            return;
        }

        context.enqueueWork(
                () -> realtimeHandlers.structuralCollapse(
                        payload
                )
        );
    }

    public static void handleBlueVisual(
            BlueVisualPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> realtimeHandlers.blueVisual(payload));
    }

    public static void handleInfinityVisual(
            InfinityVisualPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> realtimeHandlers.infinityVisual(payload));
    }

    public static void handleBlueGesture(
            BlueGestureS2CPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> realtimeHandlers.blueGesture(payload));
    }

    public static void handleBetaTechniqueVisual(
            BetaTechniqueVisualPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> realtimeHandlers.betaTechniqueVisual(payload));
    }

    public static void handleImmortalWheelVisual(
            ImmortalWheelVisualPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> realtimeHandlers.immortalWheelVisual(payload));
    }

    public static void handleImmortalWheelReactivation(
            ImmortalWheelReactivationPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> realtimeHandlers.immortalWheelReactivation(payload));
    }

    public static void handleTukunaPossession(
            TukunaPossessionS2CPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> realtimeHandlers.tukunaPossession(payload));
    }

    public static void handleTukunaView(
            TukunaViewS2CPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> realtimeHandlers.tukunaView(payload));
    }

    public static void handleTukunaMark(
            TukunaMarkS2CPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> realtimeHandlers.tukunaMark(payload));
    }

    public static void handleTukunaSpeechVisual(
            TukunaSpeechVisualS2CPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> realtimeHandlers.tukunaSpeechVisual(payload));
    }

    public static void handleTukunaPossessionVisual(
            TukunaPossessionVisualS2CPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> realtimeHandlers.tukunaPossessionVisual(payload));
    }

    public static void handleEnergyVision(
            EnergyVisionS2CPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> realtimeHandlers.energyVision(payload));
    }

    public static void handleTukunaFugaVisual(
            TukunaFugaVisualPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> realtimeHandlers.tukunaFugaVisual(payload));
    }

    public static void handlePlayerCinematic(
            PlayerCinematicPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> realtimeHandlers.playerCinematic(payload));
    }

    public static void handleBattleMusic(
            BattleMusicS2CPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> realtimeHandlers.battleMusic(payload));
    }

    public static void handleVoidDomainVisual(
            VoidDomainVisualPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> realtimeHandlers.voidDomainVisual(payload));
    }

    public static void handleJusticeDomainVisual(
            JusticeDomainVisualPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> realtimeHandlers.justiceDomainVisual(payload));
    }

    public static void handleDomainIntro(
            DomainIntroS2CPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> realtimeHandlers.domainIntro(payload));
    }

    public static void handleDream(
            DreamPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> realtimeHandlers.dream(
                        payload
                )
        );
    }
    public static void outpostView(int target,long mount){realtimeHandlers.outpostView(target,mount);}
    public static void entitySpectate(int target){realtimeHandlers.entitySpectate(target);}
}
