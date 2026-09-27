package net.caravidro.wayaround.media.blackbox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.media.MediaContent;
import net.caravidro.wayaround.network.VoiceFrameS2CPayload;
import net.caravidro.wayaround.worldstate.WorldEvent;
import net.caravidro.wayaround.worldstate.WorldStateService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Live capture and playback coordinator for Black Boxes.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class BlackBoxManager {

    public static final double RANGE =
            48.0;

    private static final double FULL_VOLUME_RANGE =
            8.0;

    private static final Map<UUID, ActiveBox> ACTIVE =
            new HashMap<>();

    private static final Map<UUID, PlaybackSession> PLAYBACKS =
            new HashMap<>();

    private BlackBoxManager() {
    }

    public static void register(
            BlackBoxBlockEntity box
    ) {
        if (!(box.getLevel()
                instanceof ServerLevel level)
                || box.recordingId() == null
                || !box.isRecording()) {
            return;
        }

        try {
            BlackBoxRecordingStore.ensureOpen(
                    level.getServer(),
                    box.recordingId(),
                    level.dimension()
                            .location(),
                    box.getBlockPos(),
                    box.startGameTime(),
                    box.startedAtMillis()
            );

            ACTIVE.put(
                    box.recordingId(),
                    new ActiveBox(
                            box.recordingId(),
                            level.dimension(),
                            box.getBlockPos()
                                    .immutable()
                    )
            );

        } catch (Exception exception) {
            WayAround.LOGGER.warn(
                    "[BlackBox] could not open {}: {}",
                    box.recordingId(),
                    exception.getMessage()
            );
        }
    }

    public static void seal(
            BlackBoxBlockEntity box
    ) {
        UUID id =
                box.recordingId();

        if (id == null) {
            return;
        }

        if (box.getLevel()
                instanceof ServerLevel level) {
            BlackBoxRecordingStore.marker(
                    id,
                    level.getGameTime(),
                    "RECORDER SEALED"
            );
        }

        ACTIVE.remove(
                id
        );

        BlackBoxRecordingStore.seal(
                id
        );
    }

    public static void captureVoice(
            ServerLevel level,
            Vec3 source,
            UUID speaker,
            String speakerName,
            byte[] pcm
    ) {
        if (pcm == null
                || pcm.length < 2) {
            return;
        }

        for (ActiveBox active :
                List.copyOf(
                        ACTIVE.values()
                )) {
            if (!active.dimension()
                    .equals(
                            level.dimension()
                    )) {
                continue;
            }

            if (!(level.getBlockEntity(
                    active.position()
            )
                    instanceof BlackBoxBlockEntity box)
                    || !box.isCaptureEnabled()) {
                continue;
            }

            double distance =
                    source.distanceTo(
                            Vec3.atCenterOf(
                                    active.position()
                            )
                    );

            if (distance > RANGE) {
                continue;
            }

            float gain =
                    gain(
                            distance
                    );

            byte[] filtered =
                    distantFilter(
                            pcm,
                            gain,
                            distance
                    );

            BlackBoxRecordingStore.voice(
                    active.recordingId(),
                    level.getGameTime(),
                    speaker,
                    speakerName,
                    gain,
                    filtered
            );
        }
    }

    @SubscribeEvent
    public static void onSoundAtPosition(
            PlayLevelSoundEvent.AtPosition event
    ) {
        if (!(event.getLevel()
                instanceof ServerLevel level)
                || event.getSound() == null) {
            return;
        }

        captureSound(
                level,
                event.getPosition(),
                event.getSound()
                        .value()
                        .location()
                        .toString(),
                event.getSource(),
                event.getNewVolume(),
                event.getNewPitch()
        );
    }

    @SubscribeEvent
    public static void onSoundAtEntity(
            PlayLevelSoundEvent.AtEntity event
    ) {
        if (!(event.getLevel()
                instanceof ServerLevel level)
                || event.getSound() == null) {
            return;
        }

        captureSound(
                level,
                event.getEntity()
                        .position(),
                event.getSound()
                        .value()
                        .location()
                        .toString(),
                event.getSource(),
                event.getNewVolume(),
                event.getNewPitch()
        );
    }

    private static void captureSound(
            ServerLevel level,
            Vec3 sourcePosition,
            String soundId,
            SoundSource source,
            float sourceVolume,
            float pitch
    ) {
        for (ActiveBox active :
                List.copyOf(
                        ACTIVE.values()
                )) {
            if (!active.dimension()
                    .equals(
                            level.dimension()
                    )) {
                continue;
            }

            if (!(level.getBlockEntity(
                    active.position()
            )
                    instanceof BlackBoxBlockEntity box)
                    || !box.isCaptureEnabled()) {
                continue;
            }

            double distance =
                    sourcePosition.distanceTo(
                            Vec3.atCenterOf(
                                    active.position()
                            )
                    );

            if (distance > RANGE) {
                continue;
            }

            float fadedVolume =
                    Math.max(
                            0.01F,
                            sourceVolume
                                    * gain(
                                    distance
                            )
                    );

            BlackBoxRecordingStore.sound(
                    active.recordingId(),
                    level.getGameTime(),
                    soundId,
                    source.name(),
                    fadedVolume,
                    pitch
            );
        }
    }

    @SubscribeEvent
    public static void onChat(
            ServerChatEvent event
    ) {
        ServerPlayer speaker =
                event.getPlayer();

        ServerLevel level =
                speaker.serverLevel();

        Vec3 source =
                speaker.position();

        for (ActiveBox active :
                List.copyOf(
                        ACTIVE.values()
                )) {
            if (!active.dimension()
                    .equals(
                            level.dimension()
                    )) {
                continue;
            }

            if (!(level.getBlockEntity(
                    active.position()
            )
                    instanceof BlackBoxBlockEntity box)
                    || !box.isCaptureEnabled()) {
                continue;
            }

            double distance =
                    source.distanceTo(
                            Vec3.atCenterOf(
                                    active.position()
                            )
                    );

            if (distance > RANGE) {
                continue;
            }

            BlackBoxRecordingStore.chat(
                    active.recordingId(),
                    level.getGameTime(),
                    speaker.getGameProfile()
                            .getName(),
                    event.getRawText()
            );
        }
    }

    public static void captureWorldState(
            BlackBoxBlockEntity box
    ) {
        if (!(box.getLevel()
                instanceof ServerLevel level)
                || !box.isCaptureEnabled()
                || box.recordingId() == null) {
            return;
        }

        List<WorldEvent> events =
                new ArrayList<>(
                        WorldStateService.nearby(
                                level,
                                box.getBlockPos(),
                                RANGE,
                                128
                        )
                );

        events.sort(
                Comparator.comparingLong(
                        WorldEvent::sequence
                )
        );

        long newest =
                box.lastWorldSequence();

        for (WorldEvent event :
                events) {
            if (event.sequence()
                    <= box.lastWorldSequence()) {
                continue;
            }

            BlackBoxRecordingStore.worldEvent(
                    box.recordingId(),
                    level.getGameTime(),
                    event
            );

            newest =
                    Math.max(
                            newest,
                            event.sequence()
                    );
        }

        if (newest
                != box.lastWorldSequence()) {
            box.setLastWorldSequence(
                    newest
            );
        }
    }

    public static void rewind(
            ServerPlayer player,
            UUID recordingId
    ) {
        PlaybackSession old =
                PLAYBACKS.remove(
                        player.getUUID()
                );

        if (old != null) {
            old.close();
        }

        if (!BlackBoxRecordingStore.exists(
                player.server,
                recordingId
        )) {
            player.displayClientMessage(
                    Component.literal(
                                    "A Black Box está selada, mas o arquivo não existe neste mundo."
                            )
                            .withStyle(
                                    ChatFormatting.DARK_RED
                            ),
                    true
            );

            return;
        }

        try {
            BlackBoxRecordingStore.Reader reader =
                    BlackBoxRecordingStore.openReader(
                            player.server,
                            recordingId
                    );

            PLAYBACKS.put(
                    player.getUUID(),
                    new PlaybackSession(
                            player.getUUID(),
                            player.server
                                    .getTickCount(),
                            reader,
                            reader.next()
                    )
            );

            player.sendSystemMessage(
                    Component.literal(
                                    "[BLACK BOX] Rebobinando do início..."
                            )
                            .withStyle(
                                    ChatFormatting.DARK_GRAY
                            )
            );

        } catch (Exception exception) {
            player.sendSystemMessage(
                    Component.literal(
                                    "[BLACK BOX] Não foi possível ler a gravação."
                            )
                            .withStyle(
                                    ChatFormatting.DARK_RED
                            )
            );

            WayAround.LOGGER.warn(
                    "[BlackBox] playback {} failed: {}",
                    recordingId,
                    exception.getMessage()
            );
        }
    }

    @SubscribeEvent
    public static void tick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        Iterator<Map.Entry<UUID, PlaybackSession>> iterator =
                PLAYBACKS.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            PlaybackSession session =
                    iterator.next()
                            .getValue();

            ServerPlayer player =
                    server.getPlayerList()
                            .getPlayer(
                                    session.playerId
                            );

            if (player == null) {
                session.close();
                iterator.remove();
                continue;
            }

            int elapsed =
                    (int) Math.max(
                            0L,
                            server.getTickCount()
                                    - session.startedAtServerTick
                    );

            try {
                int budget =
                        12;

                while (session.next != null
                        && session.next.tick()
                        <= elapsed
                        && budget-- > 0) {
                    dispatch(
                            player,
                            session.next
                    );

                    session.next =
                            session.reader.next();
                }

                if (session.next == null) {
                    player.sendSystemMessage(
                            Component.literal(
                                            "[BLACK BOX] Fim da gravação."
                                    )
                                    .withStyle(
                                            ChatFormatting.DARK_GRAY
                                    )
                    );

                    session.close();
                    iterator.remove();
                }

            } catch (Exception exception) {
                session.close();
                iterator.remove();

                WayAround.LOGGER.warn(
                        "[BlackBox] playback stream failed: {}",
                        exception.getMessage()
                );
            }
        }
    }

    private static void dispatch(
            ServerPlayer player,
            BlackBoxRecordingStore.Entry entry
    ) {
        if (entry
                instanceof BlackBoxRecordingStore.VoiceEntry voice) {
            PacketDistributor.sendToPlayer(
                    player,
                    new VoiceFrameS2CPayload(
                            voice.pcm()
                    )
            );

            return;
        }

        if (entry
                instanceof BlackBoxRecordingStore.TextEntry text) {
            player.sendSystemMessage(
                    Component.literal(
                                    "[BLACK BOX · rádio] "
                                            + text.speaker()
                                            + ": "
                                            + text.message()
                            )
                            .withStyle(
                                    ChatFormatting.GRAY
                            )
            );

            return;
        }

        if (entry
                instanceof BlackBoxRecordingStore.WorldEntry world) {
            player.sendSystemMessage(
                    Component.literal(
                                    "[BLACK BOX · evento #"
                                            + world.sequence()
                                            + "] "
                                            + world.type()
                            )
                            .withStyle(
                                    ChatFormatting.DARK_GRAY
                            )
            );

            return;
        }

        if (entry
                instanceof BlackBoxRecordingStore.SoundEntry sound) {
            try {
                ResourceLocation id =
                        ResourceLocation.parse(
                                sound.soundId()
                        );

                SoundEvent event =
                        SoundEvent.createVariableRangeEvent(
                                id
                        );

                SoundSource source =
                        SoundSource.valueOf(
                                sound.source()
                        );

                player.connection.send(
                        new ClientboundSoundPacket(
                                Holder.direct(
                                        event
                                ),
                                source,
                                player.getX(),
                                player.getY(),
                                player.getZ(),
                                Math.max(
                                        0.01F,
                                        sound.volume()
                                ),
                                sound.pitch(),
                                player.getRandom()
                                        .nextLong()
                        )
                );
            } catch (Exception ignored) {
            }

            return;
        }

        if (entry
                instanceof BlackBoxRecordingStore.MarkerEntry marker) {
            player.sendSystemMessage(
                    Component.literal(
                                    "[BLACK BOX] "
                                            + marker.marker()
                            )
                            .withStyle(
                                    ChatFormatting.DARK_GRAY
                            )
            );
        }
    }

    @SubscribeEvent
    public static void onToss(
            ItemTossEvent event
    ) {
        if (event.getEntity()
                .getItem()
                .is(
                        MediaContent.BLACK_BOX_ITEM.get()
                )) {
            event.getEntity()
                    .setUnlimitedLifetime();
        }
    }

    @SubscribeEvent
    public static void onExpire(
            ItemExpireEvent event
    ) {
        if (event.getEntity()
                .getItem()
                .is(
                        MediaContent.BLACK_BOX_ITEM.get()
                )) {
            event.getEntity()
                    .setUnlimitedLifetime();
        }
    }

    @SubscribeEvent
    public static void stop(
            ServerStoppedEvent event
    ) {
        ACTIVE.clear();

        for (PlaybackSession playback :
                PLAYBACKS.values()) {
            playback.close();
        }

        PLAYBACKS.clear();
        BlackBoxRecordingStore.closeAll();
    }

    private static float gain(
            double distance
    ) {
        if (distance <= FULL_VOLUME_RANGE) {
            return 1.0F;
        }

        return (float) Math.max(
                0.05,
                1.0
                        - (distance - FULL_VOLUME_RANGE)
                        / (RANGE - FULL_VOLUME_RANGE)
        );
    }

    /**
     * Distance is represented twice: quieter signal and a cheap low-pass
     * effect. Far voices lose high-frequency detail instead of merely becoming
     * quieter, which reads much more like a remote recorder.
     */
    private static byte[] distantFilter(
            byte[] pcm,
            float gain,
            double distance
    ) {
        byte[] output =
                new byte[
                        pcm.length
                ];

        double muffling =
                Math.max(
                        0.0,
                        Math.min(
                                0.90,
                                (distance - FULL_VOLUME_RANGE)
                                        / (RANGE - FULL_VOLUME_RANGE)
                                        * 0.86
                        )
                );

        int previous =
                0;

        for (int index = 0;
             index + 1 < pcm.length;
             index += 2) {
            int sample =
                    (short) (
                            (pcm[index] & 0xFF)
                                    | (pcm[index + 1] << 8)
                    );

            int filtered =
                    (int) Math.round(
                            sample * (1.0 - muffling)
                                    + previous * muffling
                    );

            previous =
                    filtered;

            int scaled =
                    Math.max(
                            Short.MIN_VALUE,
                            Math.min(
                                    Short.MAX_VALUE,
                                    Math.round(
                                            filtered * gain
                                    )
                            )
                    );

            output[index] =
                    (byte) (
                            scaled
                                    & 0xFF
                    );

            output[index + 1] =
                    (byte) (
                            (scaled >> 8)
                                    & 0xFF
                    );
        }

        return output;
    }

    private record ActiveBox(
            UUID recordingId,
            ResourceKey<Level> dimension,
            BlockPos position
    ) {
    }

    private static final class PlaybackSession {
        private final UUID playerId;
        private final long startedAtServerTick;
        private final BlackBoxRecordingStore.Reader reader;
        private BlackBoxRecordingStore.Entry next;

        private PlaybackSession(
                UUID playerId,
                long startedAtServerTick,
                BlackBoxRecordingStore.Reader reader,
                BlackBoxRecordingStore.Entry next
        ) {
            this.playerId = playerId;
            this.startedAtServerTick = startedAtServerTick;
            this.reader = reader;
            this.next = next;
        }

        private void close() {
            reader.close();
        }
    }
}
