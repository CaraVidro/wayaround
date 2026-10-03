package net.caravidro.wayaround.nexus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class NexustorBaseBlockEntity
        extends BlockEntity {

    private int bodies;
    private int fingers;
    private boolean head;
    private boolean core;

    private boolean eventActive;
    private boolean complete;
    private boolean portalEnabled;

    private int wave;
    private int killsRemaining;
    private int health = 100;
    private float progress;
    private long startedAt;
    private long nextWaveAt;

    /*
     * V1.1.1 initially built the reactor from real world blocks. The special
     * renderer replaces that architecture; this flag cleans those generated
     * blocks once when an old test-world reactor is loaded.
     */
    private boolean visualMigrated;

    public NexustorBaseBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                NexusContent.NEXUSTOR_BASE_ENTITY.get(),
                pos,
                state
        );
    }

    public static void serverTick(
            net.minecraft.world.level.Level level,
            BlockPos pos,
            BlockState state,
            NexustorBaseBlockEntity reactor
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!reactor.visualMigrated) {
            NexustorStructure.cleanupLegacyGeometry(
                    serverLevel,
                    pos
            );

            reactor.visualMigrated =
                    true;

            reactor.sync();
        }

        if (reactor.eventActive
                || reactor.complete) {
            NexusEventManager.ensure(
                    serverLevel,
                    pos
            );
        }
    }

    public void installBody(
            Player player,
            ItemStack stack
    ) {
        if (bodies >= 3) {
            tell(player, "message.wayaround.nexustor.body_complete");
            return;
        }

        if (!NexustorStructure.canBuildBodyLayer(
                level,
                worldPosition,
                bodies + 1
        )) {
            tell(player, "message.wayaround.nexustor.blocked");
            return;
        }

        bodies++;
        consume(player, stack);
        tell(player, "message.wayaround.nexustor.body_added", bodies, 3);
        sync();
    }

    public void installFinger(
            Player player,
            ItemStack stack
    ) {
        if (bodies < 3) {
            tell(player, "message.wayaround.nexustor.need_body");
            return;
        }

        if (fingers >= 4) {
            tell(player, "message.wayaround.nexustor.fingers_complete");
            return;
        }

        if (!NexustorStructure.canBuildFinger(
                level,
                worldPosition,
                fingers
        )) {
            tell(player, "message.wayaround.nexustor.blocked");
            return;
        }

        fingers++;
        consume(player, stack);
        tell(player, "message.wayaround.nexustor.finger_added", fingers, 4);
        sync();
    }

    public void installHead(
            Player player,
            ItemStack stack
    ) {
        if (fingers < 4) {
            tell(player, "message.wayaround.nexustor.need_fingers");
            return;
        }

        if (head) {
            tell(player, "message.wayaround.nexustor.head_complete");
            return;
        }

        if (!NexustorStructure.canBuildHead(
                level,
                worldPosition
        )) {
            tell(player, "message.wayaround.nexustor.blocked");
            return;
        }

        head = true;
        consume(player, stack);
        tell(player, "message.wayaround.nexustor.head_added");
        sync();
    }

    public void installCore(
            Player player,
            ItemStack stack
    ) {
        if (!head) {
            tell(player, "message.wayaround.nexustor.need_head");
            return;
        }

        if (core) {
            tell(player, "message.wayaround.nexustor.core_present");
            return;
        }

        if (!NexustorStructure.canInsertCore(
                level,
                worldPosition
        )) {
            tell(player, "message.wayaround.nexustor.blocked");
            return;
        }

        core = true;
        consume(player, stack);

        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            NexusEventManager.start(
                    serverPlayer.serverLevel(),
                    worldPosition,
                    this,
                    serverPlayer
            );
        }

        sync();
    }

    private static void consume(
            Player player,
            ItemStack stack
    ) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    private static void tell(
            Player player,
            String key,
            Object... args
    ) {
        player.displayClientMessage(
                Component.translatable(
                        key,
                        args
                ),
                true
        );
    }

    public Component status() {
        if (complete) {
            return Component.translatable(
                    "message.wayaround.nexustor.status_complete"
            );
        }

        if (eventActive) {
            return Component.translatable(
                    "message.wayaround.nexustor.status_active",
                    Math.round(progress * 100.0F),
                    wave,
                    health
            );
        }

        return Component.translatable(
                "message.wayaround.nexustor.status_build",
                bodies,
                fingers,
                head ? 1 : 0,
                core ? 1 : 0
        );
    }

    public void beginEvent(
            long gameTime
    ) {
        eventActive = true;
        complete = false;
        wave = 0;
        killsRemaining = 0;
        health = 100;
        progress = 0.0F;
        startedAt = gameTime;
        nextWaveAt = gameTime + NexusEventManager.FIRST_WAVE_DELAY;
        sync();
    }

    public void finishEvent() {
        eventActive = false;
        complete = true;
        portalEnabled = true;
        wave = 5;
        killsRemaining = 0;
        health = 100;
        progress = 1.0F;
        sync();
    }

    public void failEvent() {
        eventActive = false;
        complete = false;
        portalEnabled = false;
        core = false;
        killsRemaining = 0;
        progress = 0.0F;
        sync();
    }

    public boolean damageCore(
            int amount
    ) {
        health = Math.max(0, health - Math.max(0, amount));
        sync();
        return health <= 0;
    }

    public void startWave(
            int wave,
            int count,
            long nextWaveAt
    ) {
        this.wave = wave;
        this.killsRemaining += Math.max(0, count);
        this.nextWaveAt = nextWaveAt;
        sync();
    }

    public void infectedKilled(
            long ignoredNextWave
    ) {
        /*
         * Killing every infected is no longer required to advance the event.
         * Old waves can overlap with new ones and keep pressuring the core.
         */
        killsRemaining =
                Math.max(
                        0,
                        killsRemaining - 1
                );

        sync();
    }

    public void setProgress(
            float value
    ) {
        float clamped =
                net.minecraft.util.Mth.clamp(
                        value,
                        0.0F,
                        1.0F
                );

        if (Math.abs(
                clamped - progress
        ) < 0.0025F) {
            return;
        }

        progress =
                clamped;

        sync();
    }

    public void setNextWaveAt(
            long value
    ) {
        nextWaveAt = value;
        sync();
    }

    public int bodies() { return bodies; }
    public int fingers() { return fingers; }
    public boolean headInstalled() { return head; }
    public boolean coreInstalled() { return core; }

    public boolean eventActive() { return eventActive; }
    public boolean complete() { return complete; }
    public boolean portalEnabled() { return portalEnabled; }

    public void setPortalVisual(boolean enabled) {
        if(portalEnabled!=enabled){portalEnabled=enabled;sync();}
    }

    public boolean togglePortal() {
        if (!complete) {
            return false;
        }

        portalEnabled =
                !portalEnabled;

        sync();

        return portalEnabled;
    }
    public int wave() { return wave; }
    public int killsRemaining() { return killsRemaining; }
    public int health() { return health; }
    public float progress() { return progress; }
    public long startedAt() { return startedAt; }
    public long nextWaveAt() { return nextWaveAt; }

    private void sync() {
        setChanged();

        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(
                    worldPosition,
                    state,
                    state,
                    3
            );
        }
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(tag, registries);
        tag.putInt("Bodies", bodies);
        tag.putInt("Fingers", fingers);
        tag.putBoolean("Head", head);
        tag.putBoolean("Core", core);
        tag.putBoolean("EventActive", eventActive);
        tag.putBoolean("Complete", complete);
        tag.putBoolean("PortalEnabled", portalEnabled);
        tag.putInt("Wave", wave);
        tag.putInt("KillsRemaining", killsRemaining);
        tag.putInt("Health", health);
        tag.putFloat("Progress", progress);
        tag.putLong("StartedAt", startedAt);
        tag.putLong("NextWaveAt", nextWaveAt);
        tag.putBoolean("VisualMigrated", visualMigrated);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(tag, registries);
        bodies = tag.getInt("Bodies");
        fingers = tag.getInt("Fingers");
        head = tag.getBoolean("Head");
        core = tag.getBoolean("Core");
        eventActive = tag.getBoolean("EventActive");
        complete = tag.getBoolean("Complete");
        portalEnabled =
                tag.contains("PortalEnabled")
                        ? tag.getBoolean("PortalEnabled")
                        : complete;
        wave = tag.getInt("Wave");
        killsRemaining = tag.getInt("KillsRemaining");
        health = tag.contains("Health") ? tag.getInt("Health") : 100;
        progress = tag.getFloat("Progress");
        startedAt = tag.getLong("StartedAt");
        nextWaveAt = tag.getLong("NextWaveAt");
        visualMigrated = tag.getBoolean("VisualMigrated");
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries
    ) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
