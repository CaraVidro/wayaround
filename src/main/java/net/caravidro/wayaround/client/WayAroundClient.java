package net.caravidro.wayaround.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.client.calving.ClientCalvingEffects;
import net.caravidro.wayaround.media.client.MediaRecorder;
import net.caravidro.wayaround.media.client.MediaTransferClient;
import net.caravidro.wayaround.media.client.TapeLabelScreen;
import net.caravidro.wayaround.network.ClientPayloadBridge;
import net.caravidro.wayaround.voice.client.VoicePlayback;

import net.minecraft.client.Minecraft;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Bootstrap carregado somente no cliente físico.
 *
 * <p>É aqui que networking common ganha acesso aos renderizadores, áudio,
 * câmera e telas do cliente. O dedicated server nunca carrega esta classe.</p>
 */
@Mod(value = WayAround.MODID, dist = Dist.CLIENT)
public final class WayAroundClient {

    public WayAroundClient(IEventBus modEventBus) {
        ClientPayloadBridge.installFrost(FrostRenderer::receive);

        ClientPayloadBridge.installBlizzard(
                payload -> ClientBlizzardState.receive(payload.intensity())
        );

        ClientPayloadBridge.installBlueVisual(BlueClientEffects::receive);
        ClientPayloadBridge.installInfinityVisual(InfinityClientEffects::receive);

        ClientPayloadBridge.installBlueGesture(
                payload -> BlueClientEffects.playGesture(payload.gesture())
        );

        ClientPayloadBridge.installBetaTechnique(BetaTechniqueClientEffects::receive);
        ClientPayloadBridge.installImmortalWheel(ImmortalWheelClientEffects::receive);

        ClientPayloadBridge.installVoiceFrame(
                payload -> VoicePlayback.enqueue(payload.pcm())
        );

        ClientPayloadBridge.installRecordingStart(
                payload -> MediaRecorder.onStartResult(
                        payload.allowed(),
                        payload.messageKey()
                )
        );

        ClientPayloadBridge.installRecordingReady(
                payload -> Minecraft.getInstance().setScreen(
                        new TapeLabelScreen(
                                payload.recordingId(),
                                payload.defaultTitle(),
                                payload.vhs()
                        )
                )
        );

        ClientPayloadBridge.installPlacedCameraStart(
                payload -> MediaRecorder.startPlaced(
                        payload.position(),
                        payload.facing()
                )
        );

        ClientPayloadBridge.installPlacedCameraPickup(
                payload -> MediaRecorder.resumeFromPlacedCamera()
        );

        ClientPayloadBridge.installMediaChunk(
                payload -> MediaTransferClient.acceptChunk(
                        payload.recordingId(),
                        payload.totalLength(),
                        payload.offset(),
                        payload.data()
                )
        );

        ClientPayloadBridge.installCalving(ClientCalvingEffects::receive);

        WayAround.LOGGER.info("Way Around client payload handlers instalados.");
    }
}
