package net.caravidro.wayaround.network;

import java.util.Objects;
import java.util.function.Consumer;

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
                > MediaNetworkLimits.MAX_RECORDING_BYTES) {
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
}
