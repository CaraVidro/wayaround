package net.caravidro.wayaround.media.broadcast;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.media.MediaContent;
import net.caravidro.wayaround.media.PlacedCameraBlockEntity;
import net.caravidro.wayaround.media.TelevisionBlockEntity;
import net.caravidro.wayaround.network.BroadcastAudioS2CPayload;
import net.caravidro.wayaround.network.BroadcastImageS2CPayload;
import net.caravidro.wayaround.network.BroadcastWorldSoundS2CPayload;
import net.caravidro.wayaround.sounds.WayAroundSounds;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = WayAround.MODID)
public final class BroadcastManager {
    private static final int DEVICE_TTL = 100;
    private static final int MAX_TOPOLOGY = 768;

    /*
     * Protect the normal Minecraft connection from TV/voice fan-out.
     * Budgets are per receiver and reset every server tick.
     */
    private static final int MAX_IMAGE_PACKETS_PER_PLAYER_PER_TICK =
            4;

    private static final int MAX_AUDIO_PACKETS_PER_PLAYER_PER_TICK =
            6;

    private static int realtimeBudgetTick =
            Integer.MIN_VALUE;

    private static final Map<UUID, Integer> IMAGE_PACKET_BUDGET =
            new HashMap<>();

    private static final Map<UUID, Integer> AUDIO_PACKET_BUDGET =
            new HashMap<>();

    private static final Map<ResourceKey<Level>, DimensionState> STATES = new HashMap<>();
    private static final Set<UUID> HANDHELD_CAMERAS = ConcurrentHashMap.newKeySet();

    private BroadcastManager() {}

    public static void heartbeatMicrophone(ServerLevel level, BlockPos pos) {
        state(level).microphones.put(pos.asLong(), level.getGameTime());
    }

    public static void heartbeatAntenna(ServerLevel level, BlockPos pos) {
        state(level).antennas.put(pos.asLong(), level.getGameTime());
    }

    public static void heartbeatRadio(ServerLevel level, BlockPos pos) {
        state(level).radios.put(pos.asLong(), level.getGameTime());
    }

    public static void heartbeatTelevision(ServerLevel level, BlockPos pos) {
        state(level).televisions.put(pos.asLong(), level.getGameTime());
    }

    public static void setHandheldCamera(ServerPlayer player, boolean active) {
        if (active) HANDHELD_CAMERAS.add(player.getUUID());
        else HANDHELD_CAMERAS.remove(player.getUUID());
    }

    public static void captureVoice(ServerPlayer sender, byte[] pcm) {
        if (!WorldFeatureRuntime.serverEnabled(WorldFeature.MEDIA)
                || pcm == null
                || pcm.length == 0) {
            return;
        }

        ServerLevel level = sender.serverLevel();
        DimensionState state = state(level);
        Set<Long> emitted = new HashSet<>();

        if (holdingHandheldMicrophone(sender)) {
            BlockPos uplink =
                    nearestAntenna(
                            level,
                            sender.position(),
                            128.0
                    );

            if (uplink != null) {
                emitVoiceFromAntenna(
                        level,
                        uplink,
                        pcm,
                        emitted
                );
            }
        }

        for (long microphoneLong : fresh(state.microphones, level.getGameTime())) {
            BlockPos microphone = BlockPos.of(microphoneLong);
            if (microphone.distSqr(sender.blockPosition()) > 12.0 * 12.0) continue;

            Topology topology = topology(level, microphone);
            if (topology.mode == BroadcastMode.OFF_AIR
                    || topology.mode == BroadcastMode.INTERMISSION) {
                continue;
            }

            for (BlockPos antenna : topology.antennas) {
                if (emitted.add(antenna.asLong())) {
                    emitAudio(level, antenna, pcm, topology.effect);
                }
            }
        }
    }

    public static void captureAmbient(
            ServerPlayer reporter,
            ResourceLocation sound,
            SoundSource source,
            float volume,
            float pitch,
            Vec3 soundPosition
    ) {
        if (!WorldFeatureRuntime.serverEnabled(WorldFeature.MEDIA)
                || reporter.position().distanceToSqr(soundPosition) > 64.0 * 64.0) {
            return;
        }

        ServerLevel level = reporter.serverLevel();
        DimensionState dimension = state(level);

        long signature = sound.hashCode();
        signature = signature * 31L + BlockPos.containing(soundPosition).asLong();
        signature = signature * 31L + (level.getGameTime() / 2L);

        Long previous = dimension.recentSounds.put(signature, level.getGameTime());
        if (previous != null && level.getGameTime() - previous <= 2L) return;

        Set<Long> emitted = new HashSet<>();

        if (holdingHandheldMicrophone(reporter)
                && reporter.position().distanceToSqr(soundPosition) <= 14.0 * 14.0) {
            BlockPos uplink =
                    nearestAntenna(
                            level,
                            reporter.position(),
                            128.0
                    );

            if (uplink != null) {
                emitSoundFromAntenna(
                        level,
                        uplink,
                        sound,
                        source,
                        volume,
                        pitch,
                        emitted
                );
            }
        }

        for (long microphoneLong : fresh(dimension.microphones, level.getGameTime())) {
            BlockPos microphone = BlockPos.of(microphoneLong);
            if (Vec3.atCenterOf(microphone).distanceToSqr(soundPosition) > 12.0 * 12.0) continue;

            Topology topology = topology(level, microphone);
            if (topology.mode != BroadcastMode.ON_AIR) continue;

            for (BlockPos antenna : topology.antennas) {
                if (emitted.add(antenna.asLong())) {
                    emitWorldSound(level, antenna, sound, source, volume, pitch, topology.effect);
                }
            }
        }
    }

    public static void broadcastPlacedCamera(
            ServerLevel level,
            PlacedCameraBlockEntity camera
    ) {
        if (!WorldFeatureRuntime.serverEnabled(WorldFeature.MEDIA)) return;

        byte[] image = null;

        if (camera.hasIntegratedAntenna()) {
            Topology topology = topology(level, camera.getBlockPos());
            if (topology.mode != BroadcastMode.ON_AIR) return;

            image = BroadcastCameraSampler.capture(level, camera.getBlockPos(), camera.facing());
            emitImage(
                    level,
                    camera.getBlockPos(),
                    camera.frequencyKHz(),
                    camera.integratedRangeBlocks(),
                    image,
                    topology.effect
            );
            return;
        }

        Topology topology = topology(level, camera.getBlockPos());
        if (topology.mode != BroadcastMode.ON_AIR || topology.antennas.isEmpty()) return;

        image = BroadcastCameraSampler.capture(level, camera.getBlockPos(), camera.facing());

        for (BlockPos antenna : topology.antennas) {
            BroadcastAntennaBlockEntity be = antenna(level, antenna);
            if (be != null) {
                emitImage(level, antenna, be.frequencyKHz(), be.rangeBlocks(), image, topology.effect);
            }
        }
    }

    public static void tickAntenna(ServerLevel level, BroadcastAntennaBlockEntity antenna) {
        Topology topology = topology(level, antenna.getBlockPos());
        if (topology.mode != BroadcastMode.INTERMISSION) return;

        int frequency = antenna.frequencyKHz();
        long tick = level.getGameTime();

        DimensionState state = state(level);

        for (long radioLong : fresh(state.radios, tick)) {
            BlockPos pos = BlockPos.of(radioLong);
            if (!(level.getBlockEntity(pos) instanceof RadioBlockEntity radio)
                    || radio.frequencyKHz() != frequency) {
                continue;
            }

            float quality = signalQuality(level, antenna.getBlockPos(), pos, antenna.rangeBlocks());
            if (quality <= 0.03F) continue;

            radio.markSignal(tick);

            if (tick % 160L == Math.floorMod(pos.asLong(), 160L)) {
                level.playSound(
                        null,
                        pos,
                        WayAroundSounds.INTERMISSION.get(),
                        SoundSource.RECORDS,
                        radio.volume() * quality,
                        1.0F
                );
            }
        }

        for (long tvLong : fresh(state.televisions, tick)) {
            BlockPos tvPos = BlockPos.of(tvLong);
            if (!(level.getBlockEntity(tvPos) instanceof TelevisionBlockEntity television)
                    || television.hasTapeLoaded()
                    || television.frequencyKHz() != frequency) {
                continue;
            }

            BlockPos receiver = receiveAntenna(level, tvPos);
            if (receiver == null) continue;

            float quality = signalQuality(level, antenna.getBlockPos(), receiver, antenna.rangeBlocks());
            if (quality <= 0.03F) continue;

            sendImageAt(
                    level,
                    tvPos,
                    BroadcastCameraSampler.intermissionFrame(),
                    quality,
                    BroadcastEffect.CLEAN
            );

            if (tick % 160L == Math.floorMod(tvPos.asLong(), 160L)) {
                level.playSound(
                        null,
                        tvPos,
                        WayAroundSounds.INTERMISSION.get(),
                        SoundSource.RECORDS,
                        0.70F * quality,
                        1.0F
                );
            }
        }
    }

    public static void tickRadio(ServerLevel level, RadioBlockEntity radio) {
        long tick = level.getGameTime();

        if (tick - radio.lastSignalTick() <= 35L) return;

        if (tick % 60L != Math.floorMod(radio.getBlockPos().asLong(), 60L)) return;

        float staticVolume = radio.volume() * 0.28F;
        if (staticVolume <= 0.01F) return;

        level.playSound(
                null,
                radio.getBlockPos(),
                WayAroundSounds.RADIO_STATIC.get(),
                SoundSource.RECORDS,
                staticVolume,
                0.96F + level.random.nextFloat() * 0.08F
        );
    }

    private static BlockPos nearestAntenna(
            ServerLevel level,
            Vec3 source,
            double maxDistance
    ) {
        DimensionState dimension =
                state(
                        level
                );

        BlockPos best =
                null;

        double bestDistance =
                maxDistance
                        * maxDistance;

        for (long antennaLong :
                fresh(
                        dimension.antennas,
                        level.getGameTime()
                )) {
            BlockPos candidate =
                    BlockPos.of(
                            antennaLong
                    );

            double distance =
                    Vec3.atCenterOf(
                            candidate
                    )
                            .distanceToSqr(
                                    source
                            );

            if (distance <= bestDistance) {
                bestDistance =
                        distance;

                best =
                        candidate;
            }
        }

        return best;
    }

    public static float televisionSignalQuality(
            ServerLevel level,
            BlockPos television,
            int frequencyKHz
    ) {
        BlockPos receiver =
                receiveAntenna(
                        level,
                        television
                );

        if (receiver == null) {
            return 0.0F;
        }

        return bestSignalQuality(
                level,
                receiver,
                frequencyKHz
        );
    }

    public static float bestSignalQuality(
            ServerLevel level,
            BlockPos receiver,
            int frequencyKHz
    ) {
        DimensionState dimension =
                state(
                        level
                );

        float best =
                0.0F;

        for (long antennaLong :
                fresh(
                        dimension.antennas,
                        level.getGameTime()
                )) {

            BlockPos antennaPos =
                    BlockPos.of(
                            antennaLong
                    );

            BroadcastAntennaBlockEntity antenna =
                    antenna(
                            level,
                            antennaPos
                    );

            if (antenna == null
                    || antenna.frequencyKHz()
                            != frequencyKHz) {
                continue;
            }

            best =
                    Math.max(
                            best,
                            signalQuality(
                                    level,
                                    antennaPos,
                                    receiver,
                                    antenna.rangeBlocks()
                            )
                    );
        }

        return best;
    }

    public static boolean holdingHandheldMicrophone(Player player) {
        return isHandheldMicrophone(player.getMainHandItem())
                || isHandheldMicrophone(player.getOffhandItem());
    }

    private static boolean isHandheldMicrophone(ItemStack stack) {
        return !stack.isEmpty() && stack.is(MediaContent.HANDHELD_MICROPHONE.get());
    }

    private static void emitVoiceFromAntenna(
            ServerLevel level,
            BlockPos antennaPos,
            byte[] pcm,
            Set<Long> emitted
    ) {
        if (!emitted.add(antennaPos.asLong())) return;

        Topology topology = topology(level, antennaPos);
        if (topology.mode != BroadcastMode.ON_AIR) return;

        emitAudio(level, antennaPos, pcm, topology.effect);
    }

    private static void emitSoundFromAntenna(
            ServerLevel level,
            BlockPos antennaPos,
            ResourceLocation sound,
            SoundSource source,
            float volume,
            float pitch,
            Set<Long> emitted
    ) {
        if (!emitted.add(antennaPos.asLong())) return;

        Topology topology = topology(level, antennaPos);
        if (topology.mode != BroadcastMode.ON_AIR) return;

        emitWorldSound(level, antennaPos, sound, source, volume, pitch, topology.effect);
    }

    private static void emitAudio(
            ServerLevel level,
            BlockPos antennaPos,
            byte[] pcm,
            BroadcastEffect effect
    ) {
        BroadcastAntennaBlockEntity antenna = antenna(level, antennaPos);
        if (antenna == null) return;

        DimensionState state = state(level);
        long tick = level.getGameTime();

        for (long radioLong : fresh(state.radios, tick)) {
            BlockPos pos = BlockPos.of(radioLong);
            if (!(level.getBlockEntity(pos) instanceof RadioBlockEntity radio)
                    || radio.frequencyKHz() != antenna.frequencyKHz()) {
                continue;
            }

            float quality = signalQuality(level, antennaPos, pos, antenna.rangeBlocks());
            if (quality <= 0.02F) continue;

            radio.markSignal(tick);
            sendAudioAt(level, pos, pcm, quality, radio.volume(), effect, 26.0);
        }

        for (long tvLong : fresh(state.televisions, tick)) {
            BlockPos tvPos = BlockPos.of(tvLong);
            if (!(level.getBlockEntity(tvPos) instanceof TelevisionBlockEntity television)
                    || television.hasTapeLoaded()
                    || television.frequencyKHz() != antenna.frequencyKHz()) {
                continue;
            }

            BlockPos receiver = receiveAntenna(level, tvPos);
            if (receiver == null) continue;

            float quality = signalQuality(level, antennaPos, receiver, antenna.rangeBlocks());
            if (quality <= 0.02F) continue;

            sendAudioAt(level, tvPos, pcm, quality, 0.82F, effect, 30.0);
        }
    }

    private static void emitWorldSound(
            ServerLevel level,
            BlockPos antennaPos,
            ResourceLocation sound,
            SoundSource source,
            float volume,
            float pitch,
            BroadcastEffect effect
    ) {
        BroadcastAntennaBlockEntity antenna = antenna(level, antennaPos);
        if (antenna == null) return;

        DimensionState state = state(level);
        long tick = level.getGameTime();

        for (long radioLong : fresh(state.radios, tick)) {
            BlockPos pos = BlockPos.of(radioLong);
            if (!(level.getBlockEntity(pos) instanceof RadioBlockEntity radio)
                    || radio.frequencyKHz() != antenna.frequencyKHz()) continue;

            float quality = signalQuality(level, antennaPos, pos, antenna.rangeBlocks());
            if (quality <= 0.04F) continue;

            radio.markSignal(tick);
            sendWorldSoundAt(level, pos, sound, source, volume * radio.volume(), pitch, quality, effect, 26.0);
        }

        for (long tvLong : fresh(state.televisions, tick)) {
            BlockPos tvPos = BlockPos.of(tvLong);
            if (!(level.getBlockEntity(tvPos) instanceof TelevisionBlockEntity television)
                    || television.hasTapeLoaded()
                    || television.frequencyKHz() != antenna.frequencyKHz()) continue;

            BlockPos receiver = receiveAntenna(level, tvPos);
            if (receiver == null) continue;

            float quality = signalQuality(level, antennaPos, receiver, antenna.rangeBlocks());
            if (quality <= 0.04F) continue;

            sendWorldSoundAt(level, tvPos, sound, source, volume * 0.82F, pitch, quality, effect, 30.0);
        }
    }

    private static void emitImage(
            ServerLevel level,
            BlockPos transmitter,
            int frequency,
            double range,
            byte[] image,
            BroadcastEffect effect
    ) {
        DimensionState state = state(level);
        long tick = level.getGameTime();

        for (long tvLong : fresh(state.televisions, tick)) {
            BlockPos tvPos = BlockPos.of(tvLong);
            if (!(level.getBlockEntity(tvPos) instanceof TelevisionBlockEntity television)
                    || television.hasTapeLoaded()
                    || television.frequencyKHz() != frequency) continue;

            BlockPos receiver = receiveAntenna(level, tvPos);
            if (receiver == null) continue;

            float quality = signalQuality(level, transmitter, receiver, range);
            if (quality <= 0.02F) continue;

            sendImageAt(level, tvPos, image, quality, effect);
        }
    }

    private static void sendAudioAt(
            ServerLevel level,
            BlockPos receiver,
            byte[] pcm,
            float quality,
            float deviceVolume,
            BroadcastEffect effect,
            double audibleRadius
    ) {
        Vec3 center = Vec3.atCenterOf(receiver);

        for (ServerPlayer player : level.players()) {
            double distance = player.position().distanceTo(center);
            if (distance > audibleRadius) continue;

            float falloff = (float) Math.max(0.0, 1.0 - distance / audibleRadius);
            float volume = Mth.clamp(deviceVolume * falloff, 0.0F, 1.0F);
            if (volume <= 0.01F) continue;
            if (!allowRealtimePacket(
                    level,
                    player,
                    AUDIO_PACKET_BUDGET,
                    MAX_AUDIO_PACKETS_PER_PLAYER_PER_TICK
            )) {
                continue;
            }

            PacketDistributor.sendToPlayer(
                    player,
                    new BroadcastAudioS2CPayload(
                            receiver,
                            pcm,
                            quality,
                            volume,
                            effect.ordinal()
                    )
            );
        }
    }

    private static void sendWorldSoundAt(
            ServerLevel level,
            BlockPos receiver,
            ResourceLocation sound,
            SoundSource source,
            float volume,
            float pitch,
            float quality,
            BroadcastEffect effect,
            double audibleRadius
    ) {
        Vec3 center = Vec3.atCenterOf(receiver);

        for (ServerPlayer player : level.players()) {
            double distance = player.position().distanceTo(center);
            if (distance > audibleRadius) continue;

            float falloff = (float) Math.max(0.0, 1.0 - distance / audibleRadius);

            PacketDistributor.sendToPlayer(
                    player,
                    new BroadcastWorldSoundS2CPayload(
                            receiver,
                            sound.toString(),
                            source.name(),
                            Mth.clamp(volume * falloff, 0.0F, 4.0F),
                            pitch,
                            quality,
                            effect.ordinal()
                    )
            );
        }
    }

    private static void sendImageAt(
            ServerLevel level,
            BlockPos television,
            byte[] image,
            float quality,
            BroadcastEffect effect
    ) {
        Vec3 center = Vec3.atCenterOf(television);

        for (ServerPlayer player : level.players()) {
            if (player.position().distanceToSqr(center) > 48.0 * 48.0) continue;
            if (!allowRealtimePacket(
                    level,
                    player,
                    IMAGE_PACKET_BUDGET,
                    MAX_IMAGE_PACKETS_PER_PLAYER_PER_TICK
            )) {
                continue;
            }

            PacketDistributor.sendToPlayer(
                    player,
                    new BroadcastImageS2CPayload(
                            television,
                            BroadcastCameraSampler.WIDTH,
                            BroadcastCameraSampler.HEIGHT,
                            image,
                            quality,
                            effect.ordinal()
                    )
            );
        }
    }

    private static boolean allowRealtimePacket(
            ServerLevel level,
            ServerPlayer player,
            Map<UUID, Integer> budget,
            int limit
    ) {
        int tick =
                level.getServer()
                        .getTickCount();

        if (tick != realtimeBudgetTick) {
            realtimeBudgetTick =
                    tick;

            IMAGE_PACKET_BUDGET.clear();
            AUDIO_PACKET_BUDGET.clear();
        }

        UUID playerId =
                player.getUUID();

        int used =
                budget.getOrDefault(
                        playerId,
                        0
                );

        if (used >= limit) {
            return false;
        }

        budget.put(
                playerId,
                used + 1
        );

        return true;
    }

    private static Topology topology(ServerLevel level, BlockPos start) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        List<BlockPos> antennas = new ArrayList<>();

        BroadcastMode mode = BroadcastMode.ON_AIR;
        BroadcastEffect effect = BroadcastEffect.CLEAN;
        boolean editorSeen = false;

        queue.add(start.immutable());

        while (!queue.isEmpty() && visited.size() < MAX_TOPOLOGY) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos.asLong())) continue;

            BlockState state = level.getBlockState(pos);

            if (!pos.equals(start) && !isTransmitNetworkBlock(state)) continue;

            if (state.is(MediaContent.BROADCAST_ANTENNA.get())
                    && level.getBlockEntity(
                    pos
            ) instanceof BroadcastAntennaBlockEntity antenna
                    && antenna.isTowerController()) {

                antennas.add(
                        pos.immutable()
                );
            }

            if (!editorSeen && level.getBlockEntity(pos) instanceof EditorialBlockEntity editorial) {
                mode = editorial.mode();
                effect = editorial.effect();
                editorSeen = true;
            }

            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!visited.contains(next.asLong()) && isTransmitNetworkBlock(level.getBlockState(next))) {
                    queue.addLast(next.immutable());
                }
            }
        }

        return new Topology(List.copyOf(antennas), mode, effect);
    }

    private static boolean isTransmitNetworkBlock(BlockState state) {
        return state.is(MediaContent.BROADCAST_CABLE.get())
                || state.is(MediaContent.BROADCAST_ANTENNA.get())
                || state.is(MediaContent.EDITORIAL.get())
                || state.is(MediaContent.BROADCAST_MICROPHONE.get())
                || state.is(MediaContent.PLACED_CAMERA.get());
    }

    private static BlockPos receiveAntenna(ServerLevel level, BlockPos television) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        queue.add(television.immutable());

        while (!queue.isEmpty() && visited.size() < MAX_TOPOLOGY) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos.asLong())) continue;

            BlockState state = level.getBlockState(pos);

            if (state.is(MediaContent.TV_ANTENNA.get())) {
                return pos.immutable();
            }

            if (!pos.equals(television)
                    && !state.is(MediaContent.BROADCAST_CABLE.get())
                    && !state.is(MediaContent.TV_ANTENNA.get())
                    && !state.is(MediaContent.TELEVISION.get())) {
                continue;
            }

            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                BlockState nextState = level.getBlockState(next);
                if (nextState.is(MediaContent.BROADCAST_CABLE.get())
                        || nextState.is(MediaContent.TV_ANTENNA.get())
                        || nextState.is(MediaContent.TELEVISION.get())) {
                    queue.addLast(next.immutable());
                }
            }
        }

        return null;
    }

    private static BroadcastAntennaBlockEntity antenna(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BroadcastAntennaBlockEntity antenna
                ? antenna
                : null;
    }

    private static float signalQuality(
            ServerLevel level,
            BlockPos transmitter,
            BlockPos receiver,
            double range
    ) {
        DimensionState state = state(level);
        SignalKey key = new SignalKey(transmitter.asLong(), receiver.asLong());

        CachedSignal cached = state.signalCache.get(key);
        if (cached != null && level.getGameTime() - cached.tick <= 20L) {
            return cached.quality;
        }

        Vec3 from = Vec3.atCenterOf(transmitter);
        Vec3 to = Vec3.atCenterOf(receiver);
        double distance = from.distanceTo(to);

        if (distance >= range) {
            state.signalCache.put(key, new CachedSignal(level.getGameTime(), 0.0F));
            return 0.0F;
        }

        double quality = 1.0 - distance / range;

        int samples = Math.min(160, Math.max(1, (int) Math.ceil(distance / 7.0)));
        int blockers = 0;

        for (int i = 1; i < samples; i++) {
            double t = i / (double) samples;
            Vec3 point = from.lerp(to, t);
            BlockPos pos = BlockPos.containing(point);

            if (level.getBlockState(pos).isSolidRender(level, pos)) {
                blockers++;
                if (blockers >= 32) break;
            }
        }

        quality *= Math.pow(0.94, blockers);
        float result = Mth.clamp((float) quality, 0.0F, 1.0F);

        state.signalCache.put(key, new CachedSignal(level.getGameTime(), result));
        return result;
    }

    private static List<Long> fresh(Map<Long, Long> devices, long tick) {
        devices.entrySet().removeIf(entry -> tick - entry.getValue() > DEVICE_TTL);
        return List.copyOf(devices.keySet());
    }

    private static DimensionState state(ServerLevel level) {
        return STATES.computeIfAbsent(level.dimension(), ignored -> new DimensionState());
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (!WorldFeatureRuntime.serverEnabled(WorldFeature.MEDIA)) return;

        /*
         * Live video used to update once per second. With the still-lightweight
         * 64x36 sampler we can afford two updates per second without turning
         * every TV into a raycast benchmark.
         */
        if (event.getServer().getTickCount() % 10 != 0) return;

        for (UUID id : List.copyOf(HANDHELD_CAMERAS)) {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(id);

            if (player == null
                    || (!player.getMainHandItem().is(MediaContent.CAMERA.get())
                    && !player.getOffhandItem().is(MediaContent.CAMERA.get()))) {
                HANDHELD_CAMERAS.remove(id);
                continue;
            }

            ServerLevel level = player.serverLevel();
            DimensionState state = state(level);

            ItemStack heldCamera =
                    player.getMainHandItem()
                            .is(
                                    MediaContent.CAMERA.get()
                            )
                            ? player.getMainHandItem()
                            : player.getOffhandItem();

            byte[] image = null;

            if (heldCamera.is(
                    MediaContent.CAMERA.get()
            )
                    && BroadcastCameraData.hasIntegratedAntenna(
                    heldCamera
            )) {

                image =
                        BroadcastCameraSampler.capture(
                                level,
                                player.getEyePosition(),
                                player.getLookAngle()
                        );

                emitImage(
                        level,
                        player.blockPosition(),
                        BroadcastCameraData.frequencyKHz(
                                heldCamera
                        ),
                        960.0,
                        image,
                        BroadcastEffect.CLEAN
                );
            }

            if (!BroadcastCameraData.hasIntegratedAntenna(
                    heldCamera
            )) {
                BlockPos uplink =
                        nearestAntenna(
                                level,
                                player.position(),
                                128.0
                        );

                if (uplink != null) {
                    Topology topology =
                            topology(
                                    level,
                                    uplink
                            );

                    BroadcastAntennaBlockEntity antenna =
                            antenna(
                                    level,
                                    uplink
                            );

                    if (topology.mode
                            == BroadcastMode.ON_AIR
                            && antenna != null) {

                        if (image == null) {
                            image =
                                    BroadcastCameraSampler.capture(
                                            level,
                                            player.getEyePosition(),
                                            player.getLookAngle()
                                    );
                        }

                        emitImage(
                                level,
                                uplink,
                                antenna.frequencyKHz(),
                                antenna.rangeBlocks(),
                                image,
                                topology.effect
                        );
                    }
                }
            }
        }

        for (ServerLevel level :
                event.getServer().getAllLevels()) {

            DimensionState state =
                    STATES.get(
                            level.dimension()
                    );

            if (state == null) {
                continue;
            }

            long gameTime =
                    level.getGameTime();

            state.recentSounds
                    .entrySet()
                    .removeIf(
                            entry ->
                                    gameTime
                                            - entry.getValue()
                                            > 200L
                    );

            state.signalCache
                    .entrySet()
                    .removeIf(
                            entry ->
                                    gameTime
                                            - entry.getValue()
                                            .tick
                                            > 400L
                    );
        }
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) {
        STATES.clear();
        HANDHELD_CAMERAS.clear();
    }

    private record Topology(List<BlockPos> antennas, BroadcastMode mode, BroadcastEffect effect) {}
    private record SignalKey(long transmitter, long receiver) {}
    private record CachedSignal(long tick, float quality) {}

    private static final class DimensionState {
        private final Map<Long, Long> microphones = new HashMap<>();
        private final Map<Long, Long> antennas = new HashMap<>();
        private final Map<Long, Long> radios = new HashMap<>();
        private final Map<Long, Long> televisions = new HashMap<>();
        private final Map<Long, Long> recentSounds = new HashMap<>();
        private final Map<SignalKey, CachedSignal> signalCache = new HashMap<>();
    }
}
