package net.caravidro.wayaround.nexus;

import java.util.Set;

import net.caravidro.wayaround.WayAround;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

@EventBusSubscriber(modid = WayAround.MODID)
public final class NexusPortalManager {

    public static final ResourceKey<Level> NEXUS =
            ResourceKey.create(
                    Registries.DIMENSION,
                    ResourceLocation.fromNamespaceAndPath(
                            WayAround.MODID,
                            "nexus"
                    )
            );

    private static final String RETURN_DIMENSION =
            "WayAroundNexusReturnDimension";

    private static final String RETURN_BASE =
            "WayAroundNexusReturnBase";

    private static final String COOLDOWN =
            "WayAroundNexusPortalCooldown";

    private static final String WARN_COOLDOWN =
            "WayAroundNexusPortalWarnCooldown";

    private static final String INSIDE =
            "WayAroundNexusInside";

    private NexusPortalManager() {
    }

    public static void sync(
            ServerLevel source,
            BlockPos base,
            NexustorBaseBlockEntity reactor
    ) {
        boolean open =
                reactor.complete()
                        && reactor.portalEnabled();

        BlockPos sourceAnchor =
                sourceAnchor(
                        base
                );

        if (open) {
            placePortalPlane(
                    source,
                    sourceAnchor
            );
        } else {
            removePortalPlane(
                    source,
                    sourceAnchor
            );
        }

        ServerLevel nexus =
                source.getServer()
                        .getLevel(
                                NEXUS
                        );

        if (nexus == null) {
            return;
        }

        BlockPos destination =
                destinationAnchor(
                        base
                );

        /*
         * Do not force-load an empty Nexus every 40 ticks just to maintain a
         * portal nobody can see. Entering the source portal explicitly creates
         * the destination chamber; when the Nexus chunk is already loaded we
         * keep both sides synchronized.
         */
        if (!nexus.hasChunkAt(
                destination
        )) {
            return;
        }

        if (open) {
            ensureDestinationChamber(
                    nexus,
                    destination
            );

            placePortalPlane(
                    nexus,
                    destination
            );
        } else {
            removePortalPlane(
                    nexus,
                    destination
            );
        }
    }

    public static void touch(
            Level level,
            BlockPos pos,
            Entity entity
    ) {
        if (level.isClientSide
                || !(level instanceof ServerLevel server)
                || !(entity instanceof ServerPlayer player)
                || player.isPassenger()) {
            return;
        }

        long now =
                player.server
                        .getTickCount();

        CompoundTag data =
                player.getPersistentData();

        if (now < data.getLong(
                COOLDOWN
        )) {
            return;
        }

        if (server.dimension()
                .equals(
                        NEXUS
                )) {
            leaveNexus(
                    player,
                    data,
                    now
            );
        } else {
            enterNexus(
                    server,
                    pos,
                    player,
                    data,
                    now
            );
        }
    }

    private static void enterNexus(
            ServerLevel source,
            BlockPos touched,
            ServerPlayer player,
            CompoundTag data,
            long now
    ) {
        BlockPos base =
                NexusEventManager.findNearbyBase(
                        source,
                        touched,
                        8
                );

        if (base == null
                || !(source.getBlockEntity(
                base
        ) instanceof NexustorBaseBlockEntity reactor)
                || !reactor.complete()
                || !reactor.portalEnabled()) {
            return;
        }

        ServerLevel nexus =
                source.getServer()
                        .getLevel(
                                NEXUS
                        );

        if (nexus == null) {
            return;
        }

        BlockPos destination =
                destinationAnchor(
                        base
                );

        ensureDestinationChamber(
                nexus,
                destination
        );

        placePortalPlane(
                nexus,
                destination
        );

        data.putString(
                RETURN_DIMENSION,
                source.dimension()
                        .location()
                        .toString()
        );

        data.putLong(
                RETURN_BASE,
                base.asLong()
        );

        data.putLong(
                COOLDOWN,
                now + 50L
        );

        data.putBoolean(
                INSIDE,
                true
        );

        BlockPos arrival =
                destination.offset(
                        1,
                        0,
                        -3
                );

        player.teleportTo(
                nexus,
                arrival.getX() + 0.5,
                arrival.getY(),
                arrival.getZ() + 0.5,
                Set.of(),
                player.getYRot(),
                player.getXRot()
        );

        NexusAdvancements.theNexus(
                player
        );

        nexus.playSound(
                null,
                arrival,
                SoundEvents.PORTAL_TRAVEL,
                SoundSource.PLAYERS,
                1.0F,
                0.62F
        );
    }

    private static void leaveNexus(
            ServerPlayer player,
            CompoundTag data,
            long now
    ) {
        String rawDimension =
                data.getString(
                        RETURN_DIMENSION
                );

        if (rawDimension.isBlank()
                || !data.contains(
                RETURN_BASE
        )) {
            warnTrapped(
                    player,
                    data,
                    now
            );
            return;
        }

        ResourceLocation id =
                ResourceLocation.tryParse(
                        rawDimension
                );

        if (id == null) {
            warnTrapped(
                    player,
                    data,
                    now
            );
            return;
        }

        ResourceKey<Level> key =
                ResourceKey.create(
                        Registries.DIMENSION,
                        id
                );

        ServerLevel source =
                player.server
                        .getLevel(
                                key
                        );

        if (source == null) {
            warnTrapped(
                    player,
                    data,
                    now
            );
            return;
        }

        BlockPos base =
                BlockPos.of(
                        data.getLong(
                                RETURN_BASE
                        )
                );

        /*
         * The source chunk may have naturally unloaded while everybody was in
         * the Nexus. Load just that chunk so "portal off" is an actual machine
         * state, not an accidental chunk-loading trap.
         */
        source.getChunkAt(
                base
        );

        if (!(source.getBlockEntity(
                base
        ) instanceof NexustorBaseBlockEntity reactor)
                || !reactor.complete()
                || !reactor.portalEnabled()) {
            warnTrapped(
                    player,
                    data,
                    now
            );
            return;
        }

        data.putLong(
                COOLDOWN,
                now + 50L
        );

        data.putBoolean(
                INSIDE,
                false
        );

        BlockPos arrival =
                base.offset(
                        0,
                        1,
                        2
                );

        player.teleportTo(
                source,
                arrival.getX() + 0.5,
                arrival.getY(),
                arrival.getZ() + 0.5,
                Set.of(),
                player.getYRot(),
                player.getXRot()
        );

        source.playSound(
                null,
                arrival,
                SoundEvents.PORTAL_TRAVEL,
                SoundSource.PLAYERS,
                1.0F,
                0.78F
        );
    }

    private static void warnTrapped(
            ServerPlayer player,
            CompoundTag data,
            long now
    ) {
        if (now < data.getLong(
                WARN_COOLDOWN
        )) {
            return;
        }

        data.putLong(
                WARN_COOLDOWN,
                now + 40L
        );

        player.displayClientMessage(
                Component.translatable(
                        "message.wayaround.nexus.no_return_signal"
                ),
                true
        );
    }

    @SubscribeEvent
    public static void clone(
            PlayerEvent.Clone event
    ) {
        if (!(event.getOriginal()
                instanceof ServerPlayer original)
                || !(event.getEntity()
                instanceof ServerPlayer replacement)) {
            return;
        }

        CompoundTag source =
                original.getPersistentData();

        CompoundTag target =
                replacement.getPersistentData();

        if (source.contains(
                RETURN_DIMENSION
        )) {
            target.putString(
                    RETURN_DIMENSION,
                    source.getString(
                            RETURN_DIMENSION
                    )
            );
        }

        if (source.contains(
                RETURN_BASE
        )) {
            target.putLong(
                    RETURN_BASE,
                    source.getLong(
                            RETURN_BASE
                    )
            );
        }

        target.putBoolean(
                INSIDE,
                source.getBoolean(
                        INSIDE
                )
        );
    }

    @SubscribeEvent
    public static void respawn(
            PlayerEvent.PlayerRespawnEvent event
    ) {
        if (!(event.getEntity()
                instanceof ServerPlayer player)
                || !player.getPersistentData()
                .getBoolean(
                        INSIDE
                )) {
            return;
        }

        ServerLevel nexus =
                player.server
                        .getLevel(
                                NEXUS
                        );

        if (nexus == null) {
            return;
        }

        BlockPos base =
                BlockPos.of(
                        player.getPersistentData()
                                .getLong(
                                        RETURN_BASE
                                )
                );

        BlockPos destination =
                destinationAnchor(
                        base
                );

        ensureDestinationChamber(
                nexus,
                destination
        );

        BlockPos arrival =
                destination.offset(
                        1,
                        0,
                        -3
                );

        /*
         * Death is not an emergency exit. If you died inside, your respawn is
         * routed back into the Nexus. The linked Nexustor remains the only way
         * out.
         */
        player.teleportTo(
                nexus,
                arrival.getX() + 0.5,
                arrival.getY(),
                arrival.getZ() + 0.5,
                Set.of(),
                player.getYRot(),
                player.getXRot()
        );
    }

    public static BlockPos sourceAnchor(
            BlockPos base
    ) {
        return base.offset(
                -1,
                1,
                4
        );
    }

    public static BlockPos destinationAnchor(
            BlockPos sourceBase
    ) {
        return new BlockPos(
                sourceBase.getX() - 1,
                80,
                sourceBase.getZ()
        );
    }

    private static void ensureDestinationChamber(
            ServerLevel nexus,
            BlockPos portalAnchor
    ) {
        nexus.getChunkAt(
                portalAnchor
        );

        BlockPos center =
                portalAnchor.offset(
                        1,
                        0,
                        -2
                );

        BlockPos marker =
                center.offset(
                        0,
                        -2,
                        0
                );

        /*
         * Carve the safe arrival cavern only once. The rest of the dimension
         * stays fully procedural through minecraft:caves.
         */
        if (nexus.getBlockState(
                marker
        ).is(
                Blocks.CHISELED_POLISHED_BLACKSTONE
        )) {
            return;
        }

        for (int x = -6;
             x <= 6;
             x++) {
            for (int z = -7;
                 z <= 7;
                 z++) {

                double nx =
                        x / 6.0;

                double nz =
                        z / 7.0;

                double radial =
                        nx * nx
                                + nz * nz;

                if (radial > 1.12) {
                    continue;
                }

                int localHeight =
                        12
                                + (int) Math.round(
                                (1.0
                                        - Math.min(
                                        1.0,
                                        radial
                                ))
                                        * 12.0
                        );

                for (int y = 0;
                     y <= localHeight;
                     y++) {
                    BlockPos p =
                            center.offset(
                                    x,
                                    y,
                                    z
                            );

                    nexus.setBlock(
                            p,
                            Blocks.AIR.defaultBlockState(),
                            Block.UPDATE_CLIENTS
                    );
                }

                BlockPos floor =
                        center.offset(
                                x,
                                -1,
                                z
                        );

                nexus.setBlock(
                        floor,
                        (
                                Math.floorMod(
                                        x * 3
                                                + z,
                                        7
                                ) == 0
                                        ? Blocks.POLISHED_BLACKSTONE
                                        : Blocks.DEEPSLATE
                        ).defaultBlockState(),
                        Block.UPDATE_CLIENTS
                );
            }
        }

        /*
         * Four rough tunnels make the arrival room visibly connect into the
         * procedural cave network instead of looking like a sealed arena.
         */
        for (Direction direction : new Direction[]{
                Direction.NORTH,
                Direction.SOUTH,
                Direction.EAST,
                Direction.WEST
        }) {
            for (int distance = 5;
                 distance <= 18;
                 distance++) {
                BlockPos tunnelCenter =
                        center.relative(
                                direction,
                                distance
                        );

                for (int side = -2;
                     side <= 2;
                     side++) {
                    for (int y = 0;
                         y <= 6;
                         y++) {
                        BlockPos p =
                                direction.getAxis()
                                        == Direction.Axis.X
                                        ? tunnelCenter.offset(
                                        0,
                                        y,
                                        side
                                )
                                        : tunnelCenter.offset(
                                        side,
                                        y,
                                        0
                                );

                        nexus.setBlock(
                                p,
                                Blocks.AIR.defaultBlockState(),
                                Block.UPDATE_CLIENTS
                        );
                    }
                }
            }
        }

        nexus.setBlock(
                marker,
                Blocks.CHISELED_POLISHED_BLACKSTONE
                        .defaultBlockState(),
                Block.UPDATE_CLIENTS
        );
    }

    private static void placePortalPlane(
            ServerLevel level,
            BlockPos anchor
    ) {
        for (int x = 0;
             x < 3;
             x++) {
            for (int y = 0;
                 y < 3;
                 y++) {
                BlockPos p =
                        anchor.offset(
                                x,
                                y,
                                0
                        );

                if (level.getBlockState(
                        p
                ).is(
                        NexusContent.NEXUS_PORTAL.get()
                )) {
                    continue;
                }

                if (level.getBlockState(
                        p
                ).canBeReplaced()
                        || level.getBlockState(
                        p
                ).isAir()) {
                    level.setBlock(
                            p,
                            NexusContent.NEXUS_PORTAL
                                    .get()
                                    .defaultBlockState(),
                            Block.UPDATE_ALL
                    );
                }
            }
        }
    }

    private static void removePortalPlane(
            ServerLevel level,
            BlockPos anchor
    ) {
        for (int x = 0;
             x < 3;
             x++) {
            for (int y = 0;
                 y < 3;
                 y++) {
                BlockPos p =
                        anchor.offset(
                                x,
                                y,
                                0
                        );

                if (level.getBlockState(
                        p
                ).is(
                        NexusContent.NEXUS_PORTAL.get()
                )) {
                    level.setBlock(
                            p,
                            Blocks.AIR.defaultBlockState(),
                            Block.UPDATE_ALL
                    );
                }
            }
        }
    }
}
