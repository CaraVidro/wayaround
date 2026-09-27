package net.caravidro.wayaround.network;

import java.util.Objects;
import java.util.function.Consumer;

import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Ponte common-safe entre os payloads e os efeitos exclusivos do cliente.
 *
 * <p>NUNCA importe classes de net.minecraft.client, Screen, renderizadores,
 * audio client-side ou GUIs aqui. O dedicated server carrega esta classe.</p>
 */
public final class ClientPayloadBridge {

    private static volatile Consumer<FrostPayload> frost = payload -> {};
    private static volatile Consumer<BlizzardStatePayload> blizzard = payload -> {};
    private static volatile Consumer<BlueVisualPayload> blueVisual = payload -> {};
    private static volatile Consumer<InfinityVisualPayload> infinityVisual = payload -> {};
    private static volatile Consumer<BlueGestureS2CPayload> blueGesture = payload -> {};
    private static volatile Consumer<BetaTechniqueVisualPayload> betaTechnique = payload -> {};
    private static volatile Consumer<ImmortalWheelVisualPayload> immortalWheel = payload -> {};
    private static volatile Consumer<VoiceFrameS2CPayload> voiceFrame = payload -> {};
    private static volatile Consumer<RecordingStartResultS2CPayload> recordingStart = payload -> {};
    private static volatile Consumer<RecordingReadyS2CPayload> recordingReady = payload -> {};
    private static volatile Consumer<PlacedCameraStartS2CPayload> placedCameraStart = payload -> {};
    private static volatile Consumer<PlacedCameraPickupS2CPayload> placedCameraPickup = payload -> {};
    private static volatile Consumer<MediaRecordingChunkS2CPayload> mediaChunk = payload -> {};
    private static volatile Consumer<CalvingNetwork.CalvingShakePayload> calving = payload -> {};

    private ClientPayloadBridge() {
    }

    public static void installFrost(Consumer<FrostPayload> handler) {
        frost = Objects.requireNonNull(handler);
    }

    public static void installBlizzard(Consumer<BlizzardStatePayload> handler) {
        blizzard = Objects.requireNonNull(handler);
    }

    public static void installBlueVisual(Consumer<BlueVisualPayload> handler) {
        blueVisual = Objects.requireNonNull(handler);
    }

    public static void installInfinityVisual(Consumer<InfinityVisualPayload> handler) {
        infinityVisual = Objects.requireNonNull(handler);
    }

    public static void installBlueGesture(Consumer<BlueGestureS2CPayload> handler) {
        blueGesture = Objects.requireNonNull(handler);
    }

    public static void installBetaTechnique(Consumer<BetaTechniqueVisualPayload> handler) {
        betaTechnique = Objects.requireNonNull(handler);
    }

    public static void installImmortalWheel(Consumer<ImmortalWheelVisualPayload> handler) {
        immortalWheel = Objects.requireNonNull(handler);
    }

    public static void installVoiceFrame(Consumer<VoiceFrameS2CPayload> handler) {
        voiceFrame = Objects.requireNonNull(handler);
    }

    public static void installRecordingStart(Consumer<RecordingStartResultS2CPayload> handler) {
        recordingStart = Objects.requireNonNull(handler);
    }

    public static void installRecordingReady(Consumer<RecordingReadyS2CPayload> handler) {
        recordingReady = Objects.requireNonNull(handler);
    }

    public static void installPlacedCameraStart(Consumer<PlacedCameraStartS2CPayload> handler) {
        placedCameraStart = Objects.requireNonNull(handler);
    }

    public static void installPlacedCameraPickup(Consumer<PlacedCameraPickupS2CPayload> handler) {
        placedCameraPickup = Objects.requireNonNull(handler);
    }

    public static void installMediaChunk(Consumer<MediaRecordingChunkS2CPayload> handler) {
        mediaChunk = Objects.requireNonNull(handler);
    }

    public static void installCalving(Consumer<CalvingNetwork.CalvingShakePayload> handler) {
        calving = Objects.requireNonNull(handler);
    }

    private static <T> void onClientMainThread(
            Consumer<T> handler,
            T payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> handler.accept(payload));
    }

    public static void handleFrost(FrostPayload payload, IPayloadContext context) {
        onClientMainThread(frost, payload, context);
    }

    public static void handleBlizzard(BlizzardStatePayload payload, IPayloadContext context) {
        onClientMainThread(blizzard, payload, context);
    }

    public static void handleBlueVisual(BlueVisualPayload payload, IPayloadContext context) {
        onClientMainThread(blueVisual, payload, context);
    }

    public static void handleInfinityVisual(InfinityVisualPayload payload, IPayloadContext context) {
        onClientMainThread(infinityVisual, payload, context);
    }

    public static void handleBlueGesture(BlueGestureS2CPayload payload, IPayloadContext context) {
        onClientMainThread(blueGesture, payload, context);
    }

    public static void handleBetaTechnique(BetaTechniqueVisualPayload payload, IPayloadContext context) {
        onClientMainThread(betaTechnique, payload, context);
    }

    public static void handleImmortalWheel(ImmortalWheelVisualPayload payload, IPayloadContext context) {
        onClientMainThread(immortalWheel, payload, context);
    }

    public static void handleVoiceFrame(VoiceFrameS2CPayload payload, IPayloadContext context) {
        onClientMainThread(voiceFrame, payload, context);
    }

    public static void handleRecordingStart(RecordingStartResultS2CPayload payload, IPayloadContext context) {
        onClientMainThread(recordingStart, payload, context);
    }

    public static void handleRecordingReady(RecordingReadyS2CPayload payload, IPayloadContext context) {
        onClientMainThread(recordingReady, payload, context);
    }

    public static void handlePlacedCameraStart(PlacedCameraStartS2CPayload payload, IPayloadContext context) {
        onClientMainThread(placedCameraStart, payload, context);
    }

    public static void handlePlacedCameraPickup(PlacedCameraPickupS2CPayload payload, IPayloadContext context) {
        onClientMainThread(placedCameraPickup, payload, context);
    }

    public static void handleMediaChunk(MediaRecordingChunkS2CPayload payload, IPayloadContext context) {
        onClientMainThread(mediaChunk, payload, context);
    }

    public static void handleCalving(CalvingNetwork.CalvingShakePayload payload, IPayloadContext context) {
        onClientMainThread(calving, payload, context);
    }
}
