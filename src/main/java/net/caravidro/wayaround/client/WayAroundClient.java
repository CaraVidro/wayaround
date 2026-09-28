package net.caravidro.wayaround.client;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.client.calving.ClientCalvingEffects;
import net.caravidro.wayaround.media.broadcast.BroadcastEffect;
import net.caravidro.wayaround.media.client.BroadcastAudioClient;
import net.caravidro.wayaround.media.client.BroadcastClientState;
import net.caravidro.wayaround.media.client.MediaRecorder;
import net.caravidro.wayaround.media.client.MediaTransferClient;
import net.caravidro.wayaround.media.client.RecordedWorldSound;
import net.caravidro.wayaround.media.client.TapeLabelScreen;
import net.caravidro.wayaround.nexus.client.NexusClientState;
import net.caravidro.wayaround.oldfriend.client.HerobrinePhotoState;
import net.caravidro.wayaround.network.BroadcastAudioS2CPayload;
import net.caravidro.wayaround.network.BroadcastImageS2CPayload;
import net.caravidro.wayaround.network.BroadcastWorldSoundS2CPayload;
import net.caravidro.wayaround.network.ClientPayloadBridge;
import net.caravidro.wayaround.network.HerobrinePhotoModeS2CPayload;
import net.caravidro.wayaround.network.MediaRecordingChunkS2CPayload;
import net.caravidro.wayaround.network.NexusStateS2CPayload;
import net.caravidro.wayaround.network.MediaRecordingOfferS2CPayload;
import net.caravidro.wayaround.network.PlacedCameraPickupS2CPayload;
import net.caravidro.wayaround.network.PlacedCameraStartS2CPayload;
import net.caravidro.wayaround.network.RecordingReadyS2CPayload;
import net.caravidro.wayaround.network.RecordingStartResultS2CPayload;
import net.caravidro.wayaround.network.VoiceFrameS2CPayload;
import net.caravidro.wayaround.voice.client.VoicePlayback;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Bootstrap carregado exclusivamente no cliente físico.
 *
 * <p>Toda ligação entre networking common e classes de renderização/GUI deve
 * nascer aqui (ou em outra classe client-only), nunca em classes carregadas
 * pelo dedicated server.</p>
 */
@Mod(value = WayAround.MODID, dist = Dist.CLIENT)
public final class WayAroundClient {

    public WayAroundClient(IEventBus modEventBus) {
        ClientPayloadBridge.install(
                FrostRenderer::receive,
                payload -> ClientBlizzardState.receive(payload.intensity()),
                ClientCalvingEffects::receive
        );

        ClientPayloadBridge.installRealtime(
                new ClientPayloadBridge.RealtimeClientHandlers() {

                    @Override
                    public void broadcastImage(
                            BroadcastImageS2CPayload payload
                    ) {
                        BroadcastClientState.receiveImage(
                                payload.television(),
                                payload.width(),
                                payload.height(),
                                payload.rgb(),
                                payload.quality(),
                                payload.effect()
                        );
                    }

                    @Override
                    public void broadcastAudio(
                            BroadcastAudioS2CPayload payload
                    ) {
                        BroadcastAudioClient.enqueue(
                                payload.pcm(),
                                payload.quality(),
                                payload.volume(),
                                payload.effect()
                        );
                    }

                    @Override
                    public void broadcastWorldSound(
                            BroadcastWorldSoundS2CPayload payload
                    ) {
                        ResourceLocation sound =
                                ResourceLocation.tryParse(
                                        payload.sound()
                                );

                        if (sound == null) {
                            return;
                        }

                        SoundSource source;

                        try {
                            source =
                                    SoundSource.valueOf(
                                            payload.source()
                                    );
                        } catch (Exception ignored) {
                            source =
                                    SoundSource.BLOCKS;
                        }

                        float quality =
                                Math.max(
                                        0.0F,
                                        Math.min(
                                                1.0F,
                                                payload.quality()
                                        )
                                );

                        float volume =
                                payload.volume()
                                        * (
                                        0.35F
                                                + quality
                                                * 0.65F
                                );

                        float pitch =
                                payload.pitch();

                        BroadcastEffect[] effects =
                                BroadcastEffect.values();

                        BroadcastEffect effect =
                                effects[
                                        Math.max(
                                                0,
                                                Math.min(
                                                        effects.length - 1,
                                                        payload.effect()
                                                )
                                        )
                                        ];

                        if (effect
                                == BroadcastEffect.DISTANT) {
                            pitch *=
                                    0.92F;
                        }

                        if (effect
                                == BroadcastEffect.VHS) {
                            pitch *=
                                    0.985F;
                        }

                        if (effect
                                == BroadcastEffect.GLITCH
                                && (
                                System.nanoTime()
                                        & 3L
                        ) == 0L) {
                            return;
                        }

                        Minecraft.getInstance()
                                .getSoundManager()
                                .play(
                                        new RecordedWorldSound(
                                                sound,
                                                source,
                                                volume,
                                                pitch,
                                                payload.receiver()
                                                        .getX()
                                                        + 0.5,
                                                payload.receiver()
                                                        .getY()
                                                        + 0.5,
                                                payload.receiver()
                                                        .getZ()
                                                        + 0.5
                                        )
                                );
                    }

                    @Override
                    public void mediaChunk(
                            MediaRecordingChunkS2CPayload payload
                    ) {
                        MediaTransferClient.acceptChunk(
                                payload.recordingId(),
                                payload.totalLength(),
                                payload.offset(),
                                payload.data()
                        );
                    }

                    @Override
                    public void mediaOffer(
                            MediaRecordingOfferS2CPayload payload
                    ) {
                        MediaTransferClient.offer(
                                payload.recordingId(),
                                payload.totalLength()
                        );
                    }

                    @Override
                    public void placedCameraStart(
                            PlacedCameraStartS2CPayload payload
                    ) {
                        MediaRecorder.startPlaced(
                                payload.position(),
                                payload.facing()
                        );
                    }

                    @Override
                    public void placedCameraPickup(
                            PlacedCameraPickupS2CPayload payload
                    ) {
                        MediaRecorder.resumeFromPlacedCamera();
                    }

                    @Override
                    public void recordingStartResult(
                            RecordingStartResultS2CPayload payload
                    ) {
                        MediaRecorder.onStartResult(
                                payload.allowed(),
                                payload.messageKey()
                        );
                    }

                    @Override
                    public void recordingReady(
                            RecordingReadyS2CPayload payload
                    ) {
                        Minecraft.getInstance()
                                .setScreen(
                                        new TapeLabelScreen(
                                                payload.recordingId(),
                                                payload.defaultTitle(),
                                                payload.vhs()
                                        )
                                );
                    }

                    @Override
                    public void voiceFrame(
                            VoiceFrameS2CPayload payload
                    ) {
                        VoicePlayback.enqueue(
                                payload.pcm()
                        );
                    }

                    @Override
                    public void herobrinePhotoMode(
                            HerobrinePhotoModeS2CPayload payload
                    ) {
                        HerobrinePhotoState.setEnabled(
                                payload.enabled()
                        );
                    }

                    @Override
                    public void nexusState(
                            NexusStateS2CPayload payload
                    ) {
                        NexusClientState.receive(
                                payload
                        );
                    }
                }
        );

        WayAround.LOGGER.info(
                "Way Around client inicializado. Protocolo de rede {}.",
                net.caravidro.wayaround.network.WayAroundNetwork.PROTOCOL_VERSION
        );
    }
}
