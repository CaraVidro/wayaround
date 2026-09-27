package net.caravidro.wayaround.oldfriend;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.justice.JusticeSenseData;
import net.caravidro.wayaround.network.HerobrinePhotoModeS2CPayload;
import net.caravidro.wayaround.worldstate.WorldStateService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * "Seu Velho Amigo" does not run as a conventional hostile-mob AI.
 *
 * The shrine arms persistent world state. Incidents then escalate over real
 * play time and reuse Justice Sense ownership data to understand where players
 * actually build and store things.
 */
@EventBusSubscriber(
        modid = WayAround.MODID
)
public final class OldFriendManager {

    public static final ResourceLocation STATE_KEY =
            ResourceLocation.fromNamespaceAndPath(
                    WayAround.MODID,
                    "old_friend"
            );

    private static final String ARMED =
            "Armed";

    private static final String AWAKENED_AT =
            "AwakenedAt";

    private static final String PHOTO_MODE =
            "PhotoMode";

    private static final String FRIENDS =
            "Friends";

    private static final String ACTION =
            "OldFriendAction";

    private static final String TARGET =
            "OldFriendTarget";

    private static final String BLAME =
            "OldFriendBlame";

    private static final String PHASE =
            "OldFriendPhase";

    private static final String ACTION_AT =
            "OldFriendActionAt";

    private static final int ACTION_ARSON =
            1;

    private static final int ACTION_THEFT =
            2;

    private static final int ACTION_WATCH =
            3;

    private static final int ACTION_BEHIND =
            4;

    private static final int PHASE_APPROACH =
            0;

    private static final int PHASE_INSPECT =
            1;

    private static final int PHASE_AFTER_ACTION =
            2;

    private static final int PHASE_LEAVE =
            3;

    private static final int PHASE_FLEE =
            4;

    private static final String SPAWNED_AT =
            "OldFriendSpawnedAt";

    private static final String TARGET_PLAYER =
            "OldFriendTargetPlayer";

    private static final String NEXT_DECISION =
            "OldFriendNextDecision";

    private static final String PAUSE_UNTIL =
            "OldFriendPauseUntil";

    private static final String NOTICE_TICKS =
            "OldFriendNoticeTicks";

    private static final String SIGN_TARGET =
            "OldFriendSignTarget";

    private static final String CHEST_OPENED =
            "OldFriendChestOpened";

    private OldFriendManager() {
    }

    @SubscribeEvent
    public static void onRightClick(
            PlayerInteractEvent.RightClickBlock event
    ) {
        if (!(event.getEntity()
                instanceof ServerPlayer player)
                || !event.getItemStack()
                .is(
                        Items.FLINT_AND_STEEL
                )) {
            return;
        }

        ServerLevel level =
                player.serverLevel();

        BlockPos firePos =
                event.getPos()
                        .relative(
                                event.getFace()
                        );

        if (!isOldShrine(
                level,
                firePos
        )) {
            return;
        }

        CompoundTag state =
                WorldStateService.component(
                        player.server,
                        STATE_KEY
                );

        if (state.getBoolean(
                ARMED
        )) {
            return;
        }

        /*
         * Deliberately silent. Vanilla still handles the flint-and-steel
         * interaction; no advancement, message, particle or custom sound is
         * emitted here.
         */
        WorldStateService.updateComponent(
                player.server,
                STATE_KEY,
                tag -> {
                    tag.putBoolean(
                            ARMED,
                            true
                    );

                    tag.putLong(
                            AWAKENED_AT,
                            level.getGameTime()
                    );
                }
        );
    }

    @SubscribeEvent
    public static void tick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        long tick =
                server.getTickCount();

        if (tick % 200L == 0L) {
            learnAssociations(
                    server
            );
        }

        if (tick % 20L != 0L) {
            return;
        }

        CompoundTag state =
                WorldStateService.component(
                        server,
                        STATE_KEY
                );

        if (!state.getBoolean(
                ARMED
        )) {
            return;
        }

        long awakenedAt =
                state.getLong(
                        AWAKENED_AT
                );

        for (ServerPlayer player :
                server.getPlayerList()
                        .getPlayers()) {
            int stage =
                    stage(
                            player.serverLevel()
                                    .getGameTime()
                                    - awakenedAt
                    );

            subtleIncidents(
                    player,
                    stage
            );

            if (stage >= 2
                    && player.getRandom()
                    .nextInt(
                            1_200
                    ) == 0) {
                maybeCarveTunnel(
                        player
                );
            }

            if (stage >= 2
                    && player.getRandom()
                    .nextInt(
                            6_000
                    ) == 0) {
                buildPyramidNear(
                        player
                );
            }

            if (stage >= 3
                    && player.getRandom()
                    .nextInt(
                            2_400
                    ) == 0) {
                maybeSabotage(
                        player
                );
            }

            if (stage >= 3
                    && player.getRandom()
                    .nextInt(
                            1_800
                    ) == 0) {
                spawnWatcher(
                        player
                );
            }

            /*
             * Around three in-game days after the shrine, the "behind you"
             * apparition enters the pool. It is intentionally rare.
             */
            if (stage >= 4
                    && player.getRandom()
                    .nextInt(
                            1_400
                    ) == 0) {
                spawnBehind(
                        player
                );
            }
        }
    }

    @SubscribeEvent
    public static void commands(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher()
                .register(
                        Commands.literal(
                                        "herobrine"
                                )
                                .requires(
                                        source ->
                                                source.hasPermission(
                                                        2
                                                )
                                )
                                .then(
                                        Commands.literal(
                                                        "photo"
                                                )
                                                .then(
                                                        Commands.argument(
                                                                        "enabled",
                                                                        BoolArgumentType.bool()
                                                                )
                                                                .executes(
                                                                        context -> {
                                                                            boolean enabled =
                                                                                    BoolArgumentType.getBool(
                                                                                            context,
                                                                                            "enabled"
                                                                                    );

                                                                            MinecraftServer server =
                                                                                    context.getSource()
                                                                                            .getServer();

                                                                            WorldStateService.updateComponent(
                                                                                    server,
                                                                                    STATE_KEY,
                                                                                    tag -> tag.putBoolean(
                                                                                            PHOTO_MODE,
                                                                                            enabled
                                                                                    )
                                                                            );

                                                                            for (ServerPlayer player :
                                                                                    server.getPlayerList()
                                                                                            .getPlayers()) {
                                                                                PacketDistributor.sendToPlayer(
                                                                                        player,
                                                                                        new HerobrinePhotoModeS2CPayload(
                                                                                                enabled
                                                                                        )
                                                                                );
                                                                            }

                                                                            context.getSource()
                                                                                    .sendSuccess(
                                                                                            () -> Component.literal(
                                                                                                    "Herobrine photo mode: "
                                                                                                            + enabled
                                                                                            ),
                                                                                            false
                                                                                    );

                                                                            return 1;
                                                                        }
                                                                )
                                                )
                                )
                                .then(
                                        Commands.literal(
                                                        "event"
                                                )
                                                .executes(
                                                        context -> {
                                                            context.getSource()
                                                                    .sendSuccess(
                                                                            () -> Component.literal(
                                                                                    "Eventos: door, footsteps, tunnel, pyramid, sabotage, arson, theft, watch, behind, appear, all"
                                                                            ),
                                                                            false
                                                                    );

                                                            return 1;
                                                        }
                                                )
                                                .then(
                                                        eventCommand(
                                                                "door",
                                                                "door"
                                                        )
                                                )
                                                .then(
                                                        eventCommand(
                                                                "footsteps",
                                                                "footsteps"
                                                        )
                                                )
                                                .then(
                                                        eventCommand(
                                                                "tunnel",
                                                                "tunnel"
                                                        )
                                                )
                                                .then(
                                                        eventCommand(
                                                                "pyramid",
                                                                "pyramid"
                                                        )
                                                )
                                                .then(
                                                        eventCommand(
                                                                "sabotage",
                                                                "sabotage"
                                                        )
                                                )
                                                .then(
                                                        eventCommand(
                                                                "arson",
                                                                "arson"
                                                        )
                                                )
                                                .then(
                                                        eventCommand(
                                                                "theft",
                                                                "theft"
                                                        )
                                                )
                                                .then(
                                                        eventCommand(
                                                                "watch",
                                                                "watch"
                                                        )
                                                )
                                                .then(
                                                        eventCommand(
                                                                "behind",
                                                                "behind"
                                                        )
                                                )
                                                .then(
                                                        eventCommand(
                                                                "appear",
                                                                "appear"
                                                        )
                                                )
                                                .then(
                                                        eventCommand(
                                                                "all",
                                                                "all"
                                                        )
                                                )
                                )
                );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> eventCommand(
            String literal,
            String eventName
    ) {
        return Commands.literal(
                        literal
                )
                .executes(
                        context ->
                                triggerEvent(
                                        context.getSource()
                                                .getPlayerOrException(),
                                        eventName
                                )
                )
                .then(
                        Commands.argument(
                                        "target",
                                        EntityArgument.player()
                                )
                                .executes(
                                        context ->
                                                triggerEvent(
                                                        EntityArgument.getPlayer(
                                                                context,
                                                                "target"
                                                        ),
                                                        eventName
                                                )
                                )
                );
    }

    private static int triggerEvent(
            ServerPlayer target,
            String eventName
    ) {
        boolean success =
                switch (eventName) {
                    case "door" -> {
                        openDoorBehind(
                                target
                        );
                        yield true;
                    }

                    case "footsteps" -> {
                        caveFootsteps(
                                target,
                                3
                        );
                        yield true;
                    }

                    case "tunnel" -> {
                        forceCarveTunnel(
                                target
                        );
                        yield true;
                    }

                    case "pyramid" ->
                            buildPyramidNear(
                                    target
                            );

                    case "sabotage" ->
                            forceSabotage(
                                    target,
                                    0
                            );

                    case "arson" ->
                            forceSabotage(
                                    target,
                                    ACTION_ARSON
                            );

                    case "theft" ->
                            forceSabotage(
                                    target,
                                    ACTION_THEFT
                            );

                    case "watch",
                         "appear" ->
                            spawnWatcher(
                                    target
                            );

                    case "behind" ->
                            spawnBehind(
                                    target
                            );

                    case "all" -> {
                        openDoorBehind(
                                target
                        );

                        caveFootsteps(
                                target,
                                3
                        );

                        forceCarveTunnel(
                                target
                        );

                        buildPyramidNear(
                                target
                        );

                        forceSabotage(
                                target,
                                0
                        );

                        spawnWatcher(
                                target
                        );

                        spawnBehind(
                                target
                        );

                        yield true;
                    }

                    default ->
                            false;
                };

        String result =
                success
                        ? "ativado"
                        : "sem alvo valido";

        target.server
                .getPlayerList()
                .getPlayers()
                .stream()
                .filter(
                        player ->
                                player.hasPermissions(
                                        2
                                )
                )
                .forEach(
                        operator ->
                                operator.sendSystemMessage(
                                        Component.literal(
                                                "[Herobrine] "
                                                        + eventName
                                                        + " -> "
                                                        + target.getGameProfile()
                                                        .getName()
                                                        + " ("
                                                        + result
                                                        + ")"
                                        )
                                )
                );

        return success
                ? 1
                : 0;
    }

    @SubscribeEvent
    public static void login(
            PlayerEvent.PlayerLoggedInEvent event
    ) {
        if (!(event.getEntity()
                instanceof ServerPlayer player)) {
            return;
        }

        PacketDistributor.sendToPlayer(
                player,
                new HerobrinePhotoModeS2CPayload(
                        photoMode(
                                player.server
                        )
                )
        );
    }

    public static boolean photoMode(
            MinecraftServer server
    ) {
        return WorldStateService.component(
                        server,
                        STATE_KEY
                )
                .getBoolean(
                        PHOTO_MODE
                );
    }

    private static boolean isOldShrine(
            ServerLevel level,
            BlockPos firePos
    ) {
        return level.getBlockState(
                        firePos.below()
                )
                .is(
                        OldFriendContent.HEROBRINE_TOTEM.get()
                )
                && level.getBlockState(
                firePos.below(2)
        ).is(
                Blocks.NETHERRACK
        )
                && level.getBlockState(
                firePos.below(3)
        ).is(
                Blocks.GOLD_BLOCK
        )
                && level.getBlockState(
                firePos.below(4)
        ).is(
                Blocks.GOLD_BLOCK
        );
    }

    private static int stage(
            long elapsed
    ) {
        // Three Minecraft days after activation: direct behind-you sightings.
        if (elapsed >= 3L * 24_000L) {
            return 4;
        }

        if (elapsed >= 30L * 60L * 20L) {
            return 3;
        }

        if (elapsed >= 15L * 60L * 20L) {
            return 2;
        }

        if (elapsed >= 5L * 60L * 20L) {
            return 1;
        }

        return 0;
    }

    private static void subtleIncidents(
            ServerPlayer player,
            int stage
    ) {
        int doorChance =
                switch (stage) {
                    case 0 -> 360;
                    case 1 -> 240;
                    case 2 -> 160;
                    default -> 120;
                };

        int stepChance =
                switch (stage) {
                    case 0 -> 180;
                    case 1 -> 120;
                    case 2 -> 80;
                    default -> 60;
                };

        if (player.getRandom()
                .nextInt(
                        doorChance
                ) == 0) {
            openDoorBehind(
                    player
            );
        }

        if (!player.serverLevel()
                .canSeeSky(
                        player.blockPosition()
                )
                && player.getY()
                < 64.0
                && player.getRandom()
                .nextInt(
                        stepChance
                ) == 0) {
            caveFootsteps(
                    player,
                    stage
            );
        }
    }

    private static void openDoorBehind(
            ServerPlayer player
    ) {
        ServerLevel level =
                player.serverLevel();

        Vec3 look =
                player.getLookAngle()
                        .multiply(
                                1.0,
                                0.0,
                                1.0
                        );

        if (look.lengthSqr()
                < 0.001) {
            return;
        }

        look =
                look.normalize();

        BlockPos origin =
                player.blockPosition();

        ArrayList<BlockPos> candidates =
                new ArrayList<>();

        for (BlockPos pos :
                BlockPos.betweenClosed(
                        origin.offset(
                                -7,
                                -2,
                                -7
                        ),
                        origin.offset(
                                7,
                                2,
                                7
                        )
                )) {
            BlockState state =
                    level.getBlockState(
                            pos
                    );

            if (!(state.getBlock()
                    instanceof DoorBlock)
                    || !state.hasProperty(
                    BlockStateProperties.OPEN
            )
                    || state.getValue(
                    BlockStateProperties.OPEN
            )) {
                continue;
            }

            Vec3 toDoor =
                    Vec3.atCenterOf(
                                    pos
                            )
                            .subtract(
                                    player.position()
                            )
                            .multiply(
                                    1.0,
                                    0.0,
                                    1.0
                            );

            if (toDoor.lengthSqr()
                    < 2.0) {
                continue;
            }

            if (toDoor.normalize()
                    .dot(
                            look
                    )
                    < -0.28) {
                candidates.add(
                        pos.immutable()
                );
            }
        }

        if (candidates.isEmpty()) {
            return;
        }

        BlockPos chosen =
                candidates.get(
                        player.getRandom()
                                .nextInt(
                                        candidates.size()
                                )
                );

        BlockState state =
                level.getBlockState(
                        chosen
                );

        level.setBlock(
                chosen,
                state.setValue(
                        BlockStateProperties.OPEN,
                        true
                ),
                3
        );

        level.playSound(
                null,
                chosen,
                SoundEvents.WOODEN_DOOR_OPEN,
                SoundSource.BLOCKS,
                0.75F,
                0.88F
                        + player.getRandom()
                                .nextFloat()
                                * 0.12F
        );
    }

    private static void caveFootsteps(
            ServerPlayer player,
            int stage
    ) {
        ServerLevel level =
                player.serverLevel();

        Vec3 look =
                player.getLookAngle()
                        .multiply(
                                1.0,
                                0.0,
                                1.0
                        );

        if (look.lengthSqr()
                < 0.001) {
            look =
                    new Vec3(
                            1.0,
                            0.0,
                            0.0
                    );
        } else {
            look =
                    look.normalize();
        }

        Vec3 right =
                new Vec3(
                        -look.z,
                        0.0,
                        look.x
                );

        Vec3 source =
                player.position()
                        .subtract(
                                look.scale(
                                        5.0
                                                + player.getRandom()
                                                .nextDouble()
                                                * 5.0
                                )
                        )
                        .add(
                                right.scale(
                                        (
                                                player.getRandom()
                                                        .nextDouble()
                                                        - 0.5
                                        )
                                                * 5.0
                                )
                        );

        int count =
                2
                        + player.getRandom()
                                .nextInt(
                                        2 + stage
                                );

        for (int i = 0;
             i < count;
             i++) {
            BlockPos soundPos =
                    BlockPos.containing(
                            source.add(
                                    look.scale(
                                            i * 0.85
                                    )
                            )
                    );

            level.playSound(
                    null,
                    soundPos,
                    SoundEvents.STONE_STEP,
                    SoundSource.AMBIENT,
                    0.38F
                            + stage * 0.06F,
                    0.72F
                            + player.getRandom()
                            .nextFloat()
                                    * 0.16F
            );
        }
    }

    private static void learnAssociations(
            MinecraftServer server
    ) {
        List<ServerPlayer> players =
                server.getPlayerList()
                        .getPlayers();

        if (players.size()
                < 2) {
            return;
        }

        WorldStateService.updateComponent(
                server,
                STATE_KEY,
                state -> {
                    CompoundTag all =
                            state.getCompound(
                                    FRIENDS
                            );

                    for (int i = 0;
                         i < players.size();
                         i++) {
                        ServerPlayer a =
                                players.get(
                                        i
                                );

                        for (int j = i + 1;
                             j < players.size();
                             j++) {
                            ServerPlayer b =
                                    players.get(
                                            j
                                    );

                            if (a.level()
                                    != b.level()
                                    || a.distanceToSqr(
                                    b
                            ) > 32.0 * 32.0) {
                                continue;
                            }

                            rememberAssociation(
                                    all,
                                    a,
                                    b
                            );

                            rememberAssociation(
                                    all,
                                    b,
                                    a
                            );
                        }
                    }

                    state.put(
                            FRIENDS,
                            all
                    );
                }
        );
    }

    private static void rememberAssociation(
            CompoundTag all,
            ServerPlayer owner,
            ServerPlayer friend
    ) {
        String ownerKey =
                owner.getUUID()
                        .toString();

        CompoundTag ownerFriends =
                all.getCompound(
                        ownerKey
                );

        String friendKey =
                friend.getUUID()
                        .toString();

        CompoundTag row =
                ownerFriends.getCompound(
                        friendKey
                );

        row.putInt(
                "Score",
                Math.min(
                        10_000,
                        row.getInt(
                                "Score"
                        )
                                + 1
                )
        );

        row.putString(
                "Name",
                friend.getGameProfile()
                        .getName()
        );

        ownerFriends.put(
                friendKey,
                row
        );

        all.put(
                ownerKey,
                ownerFriends
        );
    }

    private static String strongestAssociation(
            MinecraftServer server,
            UUID victim
    ) {
        CompoundTag all =
                WorldStateService.component(
                                server,
                                STATE_KEY
                        )
                        .getCompound(
                                FRIENDS
                        );

        CompoundTag owner =
                all.getCompound(
                        victim.toString()
                );

        String bestName =
                "";

        int bestScore =
                0;

        for (String key :
                owner.getAllKeys()) {
            CompoundTag row =
                    owner.getCompound(
                            key
                    );

            int score =
                    row.getInt(
                            "Score"
                    );

            if (score > bestScore
                    && !row.getString(
                    "Name"
            ).isBlank()) {
                bestScore =
                        score;

                bestName =
                        row.getString(
                                "Name"
                        );
            }
        }

        return bestScore >= 3
                ? bestName
                : "";
    }

    private static void forceCarveTunnel(
            ServerPlayer victim
    ) {
        ServerLevel level =
                victim.serverLevel();

        BlockPos base =
                baseCenter(
                        level,
                        victim.getUUID()
                );

        if (base == null
                || !level.hasChunkAt(
                base
        )) {
            base =
                    victim.blockPosition();
        }

        int y =
                Math.max(
                        level.getMinBuildHeight()
                                + 8,
                        base.getY()
                                - 7
                                - victim.getRandom()
                                .nextInt(
                                        6
                                )
                );

        double angle =
                victim.getRandom()
                        .nextDouble()
                        * Math.PI
                        * 2.0;

        int distance =
                10
                        + victim.getRandom()
                        .nextInt(
                                10
                        );

        BlockPos start =
                new BlockPos(
                        base.getX()
                                + (int) Math.round(
                                Math.cos(
                                        angle
                                )
                                        * distance
                        ),
                        y,
                        base.getZ()
                                + (int) Math.round(
                                Math.sin(
                                        angle
                                )
                                        * distance
                        )
                );

        carveTunnel(
                level,
                start,
                new BlockPos(
                        base.getX(),
                        y,
                        base.getZ()
                ),
                victim.getRandom()
                        .nextInt(
                                1_000_000
                        )
        );
    }

    private static boolean buildPyramidNear(
            ServerPlayer target
    ) {
        ServerLevel level =
                target.serverLevel();

        BlockPos origin =
                target.blockPosition();

        for (int attempt = 0;
             attempt < 28;
             attempt++) {
            double angle =
                    target.getRandom()
                            .nextDouble()
                            * Math.PI
                            * 2.0;

            int radius =
                    12
                            + target.getRandom()
                            .nextInt(
                                    18
                            );

            int x =
                    origin.getX()
                            + (int) Math.round(
                            Math.cos(
                                    angle
                            )
                                    * radius
                    );

            int z =
                    origin.getZ()
                            + (int) Math.round(
                            Math.sin(
                                    angle
                            )
                                    * radius
                    );

            int y =
                    level.getHeight(
                            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            x,
                            z
                    );

            BlockPos center =
                    new BlockPos(
                            x,
                            y,
                            z
                    );

            if (!level.hasChunkAt(
                    center
            )
                    || !validPyramidSite(
                    level,
                    center
            )) {
                continue;
            }

            for (int layer = 0;
                 layer < 4;
                 layer++) {
                int half =
                        3 - layer;

                int layerY =
                        center.getY()
                                + layer;

                for (int dx = -half;
                     dx <= half;
                     dx++) {
                    for (int dz = -half;
                         dz <= half;
                         dz++) {
                        BlockPos pos =
                                new BlockPos(
                                        center.getX()
                                                + dx,
                                        layerY,
                                        center.getZ()
                                                + dz
                                );

                        if (level.getBlockState(
                                pos
                        ).isAir()) {
                            level.setBlock(
                                    pos,
                                    Blocks.SAND
                                            .defaultBlockState(),
                                    3
                            );
                        }
                    }
                }
            }

            return true;
        }

        return false;
    }

    private static boolean validPyramidSite(
            ServerLevel level,
            BlockPos center
    ) {
        for (int dx = -3;
             dx <= 3;
             dx++) {
            for (int dz = -3;
                 dz <= 3;
                 dz++) {
                BlockPos base =
                        center.offset(
                                dx,
                                0,
                                dz
                        );

                if (!level.getBlockState(
                        base
                ).isAir()
                        || !level.getFluidState(
                        base
                ).isEmpty()
                        || !level.getBlockState(
                        base.below()
                ).isFaceSturdy(
                        level,
                        base.below(),
                        Direction.UP
                )) {
                    return false;
                }
            }
        }

        return true;
    }

    private static void maybeCarveTunnel(
            ServerPlayer victim
    ) {
        ServerLevel level =
                victim.serverLevel();

        BlockPos base =
                baseCenter(
                        level,
                        victim.getUUID()
                );

        if (base == null
                || !level.hasChunkAt(
                base
        )) {
            return;
        }

        int y =
                Math.max(
                        level.getMinBuildHeight()
                                + 8,
                        base.getY()
                                - 8
                                - victim.getRandom()
                                .nextInt(
                                        7
                                )
                );

        double angle =
                victim.getRandom()
                        .nextDouble()
                        * Math.PI
                        * 2.0;

        int distance =
                12
                        + victim.getRandom()
                        .nextInt(
                                10
                        );

        BlockPos start =
                new BlockPos(
                        base.getX()
                                + (int) Math.round(
                                Math.cos(
                                        angle
                                )
                                        * distance
                        ),
                        y,
                        base.getZ()
                                + (int) Math.round(
                                Math.sin(
                                        angle
                                )
                                        * distance
                        )
                );

        carveTunnel(
                level,
                start,
                new BlockPos(
                        base.getX(),
                        y,
                        base.getZ()
                ),
                victim.getRandom()
                        .nextInt(
                                1_000_000
                        )
        );
    }

    private static void carveTunnel(
            ServerLevel level,
            BlockPos start,
            BlockPos goal,
            int seed
    ) {
        java.util.Random random =
                new java.util.Random(
                        seed
                );

        Vec3 delta =
                Vec3.atCenterOf(
                                goal
                        )
                        .subtract(
                                Vec3.atCenterOf(
                                        start
                                )
                        );

        if (delta.lengthSqr()
                < 4.0) {
            return;
        }

        Vec3 direction =
                delta.normalize();

        int length =
                14
                        + random.nextInt(
                                12
                        );

        Vec3 cursor =
                Vec3.atCenterOf(
                        start
                );

        for (int step = 0;
             step < length;
             step++) {
            if (step > 0
                    && step % 5 == 0) {
                direction =
                        direction.add(
                                        (
                                                random.nextDouble()
                                                        - 0.5
                                        )
                                                * 0.22,
                                        (
                                                random.nextDouble()
                                                        - 0.5
                                        )
                                                * 0.08,
                                        (
                                                random.nextDouble()
                                                        - 0.5
                                        )
                                                * 0.22
                                )
                                .normalize();
            }

            cursor =
                    cursor.add(
                            direction
                    );

            BlockPos floor =
                    BlockPos.containing(
                            cursor
                    );

            carveNatural(
                    level,
                    floor
            );

            carveNatural(
                    level,
                    floor.above()
            );

            if (step == length / 2) {
                Direction branch =
                        random.nextBoolean()
                                ? Direction.EAST
                                : Direction.WEST;

                for (int side = 1;
                     side <= 4;
                     side++) {
                    BlockPos branchPos =
                            floor.relative(
                                    branch,
                                    side
                            );

                    carveNatural(
                            level,
                            branchPos
                    );

                    carveNatural(
                            level,
                            branchPos.above()
                    );
                }
            }
        }
    }

    private static void carveNatural(
            ServerLevel level,
            BlockPos pos
    ) {
        if (level.getBlockEntity(
                pos
        ) != null) {
            return;
        }

        BlockState state =
                level.getBlockState(
                        pos
                );

        if (state.is(
                Blocks.STONE
        )
                || state.is(
                Blocks.DEEPSLATE
        )
                || state.is(
                Blocks.DIRT
        )
                || state.is(
                Blocks.GRAVEL
        )
                || state.is(
                Blocks.ANDESITE
        )
                || state.is(
                Blocks.DIORITE
        )
                || state.is(
                Blocks.GRANITE
        )
                || state.is(
                Blocks.TUFF
        )) {
            level.setBlock(
                    pos,
                    Blocks.AIR
                            .defaultBlockState(),
                    3
            );
        }
    }

    private static boolean forceSabotage(
            ServerPlayer victim,
            int requestedAction
    ) {
        ServerLevel level =
                victim.serverLevel();

        BlockPos base =
                baseCenter(
                        level,
                        victim.getUUID()
                );

        if (base == null
                || !level.hasChunkAt(
                base
        )) {
            base =
                    victim.blockPosition();
        }

        String blame =
                strongestAssociation(
                        victim.server,
                        victim.getUUID()
                );

        if (requestedAction == 0
                || requestedAction == ACTION_ARSON) {
            BlockPos wood =
                    findWoodTarget(
                            level,
                            base
                    );

            if (wood != null) {
                spawnApparition(
                        level,
                        wood,
                        ACTION_ARSON,
                        blame
                );

                return true;
            }

            if (requestedAction == ACTION_ARSON) {
                return false;
            }
        }

        if (requestedAction == 0
                || requestedAction == ACTION_THEFT) {
            BlockPos chest =
                    findOwnedOrNearbyChest(
                            level,
                            victim,
                            base
                    );

            if (chest != null) {
                spawnApparition(
                        level,
                        chest,
                        ACTION_THEFT,
                        blame
                );

                return true;
            }

            if (requestedAction == ACTION_THEFT) {
                return false;
            }
        }

        spawnApparition(
                level,
                base,
                0,
                blame
        );

        return true;
    }

    private static BlockPos findOwnedOrNearbyChest(
            ServerLevel level,
            ServerPlayer victim,
            BlockPos base
    ) {
        List<BlockPos> remembered =
                JusticeSenseData.get(
                                victim.server
                        )
                        .chestPositions(
                                level,
                                victim.getUUID(),
                                96
                        );

        BlockPos known =
                remembered.stream()
                        .filter(
                                level::hasChunkAt
                        )
                        .min(
                                Comparator.comparingDouble(
                                        pos ->
                                                pos.distSqr(
                                                        base
                                                )
                                )
                        )
                        .orElse(null);

        if (known != null) {
            return known;
        }

        for (BlockPos pos :
                BlockPos.betweenClosed(
                        base.offset(
                                -10,
                                -4,
                                -10
                        ),
                        base.offset(
                                10,
                                5,
                                10
                        )
                )) {
            if (level.getBlockEntity(
                    pos
            ) instanceof ChestBlockEntity) {
                return pos.immutable();
            }
        }

        return null;
    }

    private static void maybeSabotage(
            ServerPlayer victim
    ) {
        ServerLevel level =
                victim.serverLevel();

        if (!level.getEntitiesOfClass(
                HerobrineEntity.class,
                new AABB(
                        victim.getX() - 128.0,
                        victim.getY() - 64.0,
                        victim.getZ() - 128.0,
                        victim.getX() + 128.0,
                        victim.getY() + 64.0,
                        victim.getZ() + 128.0
                )
        ).isEmpty()) {
            return;
        }

        BlockPos base =
                baseCenter(
                        level,
                        victim.getUUID()
                );

        if (base == null
                || !level.hasChunkAt(
                base
        )) {
            return;
        }

        /*
         * Only the owner has to be away. Other players may be at the base and
         * can genuinely witness what happens.
         */
        if (victim.distanceToSqr(
                Vec3.atCenterOf(
                        base
                )
        ) <= 48.0 * 48.0) {
            return;
        }

        String blame =
                strongestAssociation(
                        victim.server,
                        victim.getUUID()
                );

        boolean wooden =
                isWoodenBase(
                        level,
                        base
                );

        if (wooden
                && victim.getRandom()
                .nextFloat()
                < 0.58F) {
            BlockPos target =
                    findWoodTarget(
                            level,
                            base
                    );

            if (target != null) {
                spawnApparition(
                        level,
                        target,
                        ACTION_ARSON,
                        blame
                );

                return;
            }
        }

        List<BlockPos> chests =
                JusticeSenseData.get(
                                victim.server
                        )
                        .chestPositions(
                                level,
                                victim.getUUID(),
                                96
                        );

        BlockPos chest =
                chests.stream()
                        .filter(
                                level::hasChunkAt
                        )
                        .min(
                                Comparator.comparingDouble(
                                        pos ->
                                                pos.distSqr(
                                                        base
                                                )
                                )
                        )
                        .orElse(null);

        if (chest != null) {
            spawnApparition(
                    level,
                    chest,
                    ACTION_THEFT,
                    blame
            );
        }
    }

    private static boolean playerNear(
            ServerLevel level,
            BlockPos pos,
            double radius
    ) {
        double radiusSq =
                radius * radius;

        for (ServerPlayer player :
                level.players()) {
            if (player.distanceToSqr(
                    Vec3.atCenterOf(
                            pos
                    )
            ) <= radiusSq) {
                return true;
            }
        }

        return false;
    }

    private static BlockPos baseCenter(
            ServerLevel level,
            UUID owner
    ) {
        List<BlockPos> claims =
                JusticeSenseData.get(
                                level.getServer()
                        )
                        .structurePositions(
                                level,
                                owner,
                                160
                        );

        if (claims.isEmpty()) {
            return null;
        }

        BlockPos bestSeed =
                null;

        int bestCount =
                0;

        for (BlockPos seed :
                claims) {
            int count =
                    0;

            for (BlockPos other :
                    claims) {
                if (seed.distSqr(
                        other
                ) <= 14.0 * 14.0) {
                    count++;
                }
            }

            if (count > bestCount) {
                bestCount =
                        count;

                bestSeed =
                        seed;
            }
        }

        if (bestSeed == null) {
            return null;
        }

        long x =
                0;

        long y =
                0;

        long z =
                0;

        int count =
                0;

        for (BlockPos pos :
                claims) {
            if (bestSeed.distSqr(
                    pos
            ) > 14.0 * 14.0) {
                continue;
            }

            x +=
                    pos.getX();

            y +=
                    pos.getY();

            z +=
                    pos.getZ();

            count++;
        }

        if (count == 0) {
            return bestSeed;
        }

        return new BlockPos(
                (int) (x / count),
                (int) (y / count),
                (int) (z / count)
        );
    }

    private static boolean isWoodenBase(
            ServerLevel level,
            BlockPos center
    ) {
        int wood =
                0;

        int solid =
                0;

        for (BlockPos pos :
                BlockPos.betweenClosed(
                        center.offset(
                                -7,
                                -3,
                                -7
                        ),
                        center.offset(
                                7,
                                5,
                                7
                        )
                )) {
            BlockState state =
                    level.getBlockState(
                            pos
                    );

            if (state.isAir()) {
                continue;
            }

            solid++;

            if (state.is(
                    BlockTags.PLANKS
            )
                    || state.is(
                    BlockTags.LOGS
            )) {
                wood++;
            }
        }

        return wood >= 12
                && wood
                >= Math.max(
                1,
                solid / 5
        );
    }

    private static BlockPos findWoodTarget(
            ServerLevel level,
            BlockPos center
    ) {
        ArrayList<BlockPos> candidates =
                new ArrayList<>();

        for (BlockPos pos :
                BlockPos.betweenClosed(
                        center.offset(
                                -7,
                                -3,
                                -7
                        ),
                        center.offset(
                                7,
                                5,
                                7
                        )
                )) {
            BlockState state =
                    level.getBlockState(
                            pos
                    );

            if (state.is(
                    BlockTags.PLANKS
            )
                    || state.is(
                    BlockTags.LOGS
            )) {
                for (Direction direction :
                        Direction.values()) {
                    BlockPos fire =
                            pos.relative(
                                    direction
                            );

                    if (level.getBlockState(
                            fire
                    ).isAir()) {
                        candidates.add(
                                pos.immutable()
                        );
                        break;
                    }
                }
            }
        }

        return candidates.isEmpty()
                ? null
                : candidates.get(
                level.random.nextInt(
                        candidates.size()
                )
        );
    }

    private static void spawnApparition(
            ServerLevel level,
            BlockPos target,
            int action,
            String blame
    ) {
        BlockPos spawn =
                findSpawn(
                        level,
                        target
                );

        if (spawn == null) {
            return;
        }

        spawnAt(
                level,
                spawn,
                target,
                action,
                blame,
                null
        );
    }

    private static boolean spawnAt(
            ServerLevel level,
            BlockPos spawn,
            BlockPos target,
            int action,
            String blame,
            UUID targetPlayer
    ) {
        HerobrineEntity entity =
                OldFriendContent.HEROBRINE.get()
                        .create(
                                level
                        );

        if (entity == null) {
            return false;
        }

        entity.moveTo(
                spawn.getX()
                        + 0.5,
                spawn.getY(),
                spawn.getZ()
                        + 0.5,
                level.random.nextFloat()
                        * 360.0F,
                0.0F
        );

        CompoundTag data =
                entity.getPersistentData();

        data.putInt(
                ACTION,
                action
        );

        data.putLong(
                TARGET,
                target.asLong()
        );

        data.putString(
                BLAME,
                blame == null
                        ? ""
                        : blame
        );

        data.putInt(
                PHASE,
                PHASE_APPROACH
        );

        data.putLong(
                ACTION_AT,
                level.getGameTime()
        );

        data.putLong(
                SPAWNED_AT,
                level.getGameTime()
        );

        if (targetPlayer != null) {
            data.putString(
                    TARGET_PLAYER,
                    targetPlayer.toString()
            );
        }

        if (action == ACTION_ARSON) {
            entity.setItemInHand(
                    net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.item.ItemStack(
                            Items.FLINT_AND_STEEL
                    )
            );
        }

        level.addFreshEntity(
                entity
        );

        return true;
    }

    private static boolean spawnWatcher(
            ServerPlayer target
    ) {
        ServerLevel level =
                target.serverLevel();

        if (nearbyHerobrine(
                level,
                target.position(),
                40.0
        )) {
            return false;
        }

        BlockPos spawn =
                findObservationSpot(
                        target
                );

        if (spawn == null) {
            return false;
        }

        return spawnAt(
                level,
                spawn,
                target.blockPosition(),
                ACTION_WATCH,
                "",
                target.getUUID()
        );
    }

    private static boolean spawnBehind(
            ServerPlayer target
    ) {
        ServerLevel level =
                target.serverLevel();

        if (nearbyHerobrine(
                level,
                target.position(),
                32.0
        )) {
            return false;
        }

        Vec3 look =
                target.getLookAngle()
                        .multiply(
                                1.0,
                                0.0,
                                1.0
                        );

        if (look.lengthSqr()
                < 0.001) {
            return false;
        }

        look =
                look.normalize();

        Vec3 right =
                new Vec3(
                        -look.z,
                        0.0,
                        look.x
                );

        Vec3 desired =
                target.position()
                        .subtract(
                                look.scale(
                                        4.5
                                                + target.getRandom()
                                                .nextDouble()
                                                * 2.0
                                )
                        )
                        .add(
                                right.scale(
                                        (
                                                target.getRandom()
                                                        .nextDouble()
                                                        - 0.5
                                        )
                                                * 1.6
                                )
                        );

        BlockPos spawn =
                groundAt(
                        level,
                        BlockPos.containing(
                                desired
                        ),
                        4
                );

        if (spawn == null
                || !validStandingSpot(
                level,
                spawn
        )) {
            return false;
        }

        return spawnAt(
                level,
                spawn,
                target.blockPosition(),
                ACTION_BEHIND,
                "",
                target.getUUID()
        );
    }

    private static BlockPos findObservationSpot(
            ServerPlayer target
    ) {
        ServerLevel level =
                target.serverLevel();

        BlockPos origin =
                target.blockPosition();

        for (int attempt = 0;
             attempt < 36;
             attempt++) {
            double angle =
                    target.getRandom()
                            .nextDouble()
                            * Math.PI
                            * 2.0;

            int radius =
                    8
                            + target.getRandom()
                            .nextInt(
                                    11
                            );

            BlockPos rough =
                    new BlockPos(
                            origin.getX()
                                    + (int) Math.round(
                                    Math.cos(
                                            angle
                                    )
                                            * radius
                            ),
                            origin.getY(),
                            origin.getZ()
                                    + (int) Math.round(
                                    Math.sin(
                                            angle
                                    )
                                            * radius
                            )
                    );

            BlockPos candidate =
                    groundAt(
                            level,
                            rough,
                            8
                    );

            if (candidate != null
                    && validStandingSpot(
                    level,
                    candidate
            )
                    && hasNearbyCover(
                    level,
                    candidate
            )) {
                return candidate;
            }
        }

        return null;
    }

    private static BlockPos groundAt(
            ServerLevel level,
            BlockPos around,
            int verticalSearch
    ) {
        for (int dy = verticalSearch;
             dy >= -verticalSearch;
             dy--) {
            BlockPos pos =
                    around.offset(
                            0,
                            dy,
                            0
                    );

            if (validStandingSpot(
                    level,
                    pos
            )) {
                return pos;
            }
        }

        return null;
    }

    private static boolean validStandingSpot(
            ServerLevel level,
            BlockPos pos
    ) {
        return level.hasChunkAt(
                pos
        )
                && level.getBlockState(
                pos
        ).isAir()
                && level.getBlockState(
                pos.above()
        ).isAir()
                && level.getBlockState(
                pos.below()
        ).isFaceSturdy(
                level,
                pos.below(),
                Direction.UP
        )
                && level.getFluidState(
                pos
        ).isEmpty();
    }

    private static boolean hasNearbyCover(
            ServerLevel level,
            BlockPos pos
    ) {
        for (BlockPos nearby :
                BlockPos.betweenClosed(
                        pos.offset(
                                -2,
                                -1,
                                -2
                        ),
                        pos.offset(
                                2,
                                3,
                                2
                        )
                )) {
            if (nearby.equals(
                    pos
            )
                    || nearby.equals(
                    pos.above()
            )) {
                continue;
            }

            BlockState state =
                    level.getBlockState(
                            nearby
                    );

            if (state.is(
                    BlockTags.LOGS
            )
                    || (
                    !state.isAir()
                            && state.isCollisionShapeFullBlock(
                            level,
                            nearby
                    )
            )) {
                return true;
            }
        }

        return false;
    }

    private static boolean nearbyHerobrine(
            ServerLevel level,
            Vec3 position,
            double radius
    ) {
        AABB area =
                new AABB(
                        position.x - radius,
                        position.y - radius,
                        position.z - radius,
                        position.x + radius,
                        position.y + radius,
                        position.z + radius
                );

        return !level.getEntitiesOfClass(
                HerobrineEntity.class,
                area
        ).isEmpty();
    }

    private static BlockPos findSpawn(
            ServerLevel level,
            BlockPos target
    ) {
        for (int attempt = 0;
             attempt < 18;
             attempt++) {
            double angle =
                    level.random.nextDouble()
                            * Math.PI
                            * 2.0;

            int radius =
                    8
                            + level.random.nextInt(
                                    8
                            );

            int x =
                    target.getX()
                            + (int) Math.round(
                            Math.cos(
                                    angle
                            )
                                    * radius
                    );

            int z =
                    target.getZ()
                            + (int) Math.round(
                            Math.sin(
                                    angle
                            )
                                    * radius
                    );

            int y =
                    level.getHeight(
                            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            x,
                            z
                    );

            BlockPos pos =
                    new BlockPos(
                            x,
                            y,
                            z
                    );

            if (level.hasChunkAt(
                    pos
            )
                    && level.getBlockState(
                    pos
            ).isAir()
                    && level.getBlockState(
                    pos.above()
            ).isAir()) {
                return pos;
            }
        }

        return null;
    }

    public static void tickHerobrine(
            HerobrineEntity entity
    ) {
        if (!(entity.level()
                instanceof ServerLevel level)) {
            return;
        }

        CompoundTag data =
                entity.getPersistentData();

        int action =
                data.getInt(
                        ACTION
                );

        if (action == ACTION_BEHIND) {
            tickBehindApparition(
                    level,
                    entity,
                    data
            );
            return;
        }

        if (action == ACTION_WATCH) {
            tickWatcher(
                    level,
                    entity,
                    data
            );
            return;
        }

        if (!data.contains(
                TARGET
        )) {
            entity.discard();
            return;
        }

        long now =
                level.getGameTime();

        long spawnedAt =
                data.contains(
                        SPAWNED_AT
                )
                        ? data.getLong(
                        SPAWNED_AT
                )
                        : now;

        if (now - spawnedAt
                > 20L * 120L) {
            entity.discard();
            return;
        }

        int phase =
                data.getInt(
                        PHASE
                );

        ServerPlayer observer =
                observingPlayer(
                        level,
                        entity
                );

        if (observer != null
                && phase != PHASE_FLEE
                && phase != PHASE_LEAVE) {
            enterPhase(
                    data,
                    PHASE_FLEE,
                    now
            );

            phase =
                    PHASE_FLEE;
        }

        BlockPos target =
                BlockPos.of(
                        data.getLong(
                                TARGET
                        )
                );

        long phaseAge =
                now
                        - data.getLong(
                        ACTION_AT
                );

        if (phase == PHASE_FLEE) {
            fleeLikePlayer(
                    level,
                    entity,
                    observer,
                    phaseAge
            );
            return;
        }

        if (phase == PHASE_LEAVE) {
            leaveLikePlayer(
                    level,
                    entity,
                    target,
                    phaseAge
            );
            return;
        }

        if (phase == PHASE_APPROACH) {
            walkLikePlayerToward(
                    level,
                    entity,
                    target,
                    data,
                    0.98
            );

            if (entity.distanceToSqr(
                    Vec3.atCenterOf(
                            target
                    )
            ) <= 3.0 * 3.0) {
                entity.getNavigation()
                        .stop();

                enterPhase(
                        data,
                        PHASE_INSPECT,
                        now
                );
            }

            return;
        }

        int actionType =
                data.getInt(
                        ACTION
                );

        if (phase == PHASE_INSPECT) {
            entity.getNavigation()
                    .stop();

            entity.lookNaturallyAt(
                    target.getX()
                            + 0.5,
                    target.getY()
                            + 0.65,
                    target.getZ()
                            + 0.5
            );

            if (actionType == ACTION_THEFT
                    && !data.getBoolean(
                    CHEST_OPENED
            )
                    && level.getBlockEntity(
                    target
            ) instanceof ChestBlockEntity) {
                BlockState chestState =
                        level.getBlockState(
                                target
                        );

                level.blockEvent(
                        target,
                        chestState.getBlock(),
                        1,
                        1
                );

                data.putBoolean(
                        CHEST_OPENED,
                        true
                );
            }

            if (phaseAge < (
                    actionType == ACTION_THEFT
                            ? 75L
                            : 48L
            )) {
                return;
            }

            BlockPos sign =
                    performSabotageAction(
                            level,
                            entity,
                            target,
                            actionType,
                            data.getString(
                                    BLAME
                            )
                    );

            if (sign != null) {
                data.putLong(
                        SIGN_TARGET,
                        sign.asLong()
                );
            }

            enterPhase(
                    data,
                    PHASE_AFTER_ACTION,
                    now
            );

            return;
        }

        if (phase == PHASE_AFTER_ACTION) {
            entity.getNavigation()
                    .stop();

            if (data.contains(
                    SIGN_TARGET
            )) {
                BlockPos sign =
                        BlockPos.of(
                                data.getLong(
                                        SIGN_TARGET
                                )
                        );

                entity.lookNaturallyAt(
                        sign.getX()
                                + 0.5,
                        sign.getY()
                                + 0.75,
                        sign.getZ()
                                + 0.5
                );
            } else {
                entity.lookNaturallyAt(
                        target.getX()
                                + 0.5,
                        target.getY()
                                + 0.7,
                        target.getZ()
                                + 0.5
                );
            }

            if (phaseAge >= 55L) {
                enterPhase(
                        data,
                        PHASE_LEAVE,
                        now
                );
            }
        }
    }

    private static void tickWatcher(
            ServerLevel level,
            HerobrineEntity entity,
            CompoundTag data
    ) {
        ServerPlayer target =
                targetPlayer(
                        level,
                        data
                );

        if (target == null) {
            entity.discard();
            return;
        }

        long now =
                level.getGameTime();

        long phaseAge =
                now
                        - data.getLong(
                        ACTION_AT
                );

        int phase =
                data.getInt(
                        PHASE
                );

        if (phase == PHASE_FLEE
                || phase == PHASE_LEAVE) {
            leaveLikePlayer(
                    level,
                    entity,
                    target.blockPosition(),
                    phaseAge
            );
            return;
        }

        entity.getNavigation()
                .stop();

        entity.lookNaturallyAt(
                target.getX(),
                target.getEyeY(),
                target.getZ()
        );

        ServerPlayer observer =
                observingPlayer(
                        level,
                        entity
                );

        if (observer != null
                || phaseAge
                > 20L * (
                7L
                        + level.random.nextInt(
                                5
                        )
        )) {
            /*
             * He does not pop out. He turns away and leaves like a player who
             * realized he was caught.
             */
            enterPhase(
                    data,
                    PHASE_LEAVE,
                    now
            );
        }
    }

    private static void tickBehindApparition(
            ServerLevel level,
            HerobrineEntity entity,
            CompoundTag data
    ) {
        ServerPlayer target =
                targetPlayer(
                        level,
                        data
                );

        if (target == null) {
            entity.discard();
            return;
        }

        entity.getNavigation()
                .stop();

        entity.lookNaturallyAt(
                target.getX(),
                target.getEyeY(),
                target.getZ()
        );

        long age =
                level.getGameTime()
                        - data.getLong(
                        SPAWNED_AT
                );

        if (isLookingAt(
                target,
                entity,
                0.84
        )) {
            int seenTicks =
                    data.getInt(
                            NOTICE_TICKS
                    );

            if (seenTicks >= 1) {
                entity.discard();
                return;
            }

            /*
             * One server tick of grace: on a normal client this leaves roughly
             * one visual frame/interpolation beat after the player turns.
             */
            data.putInt(
                    NOTICE_TICKS,
                    seenTicks + 1
            );

            return;
        }

        data.putInt(
                NOTICE_TICKS,
                0
        );

        if (age > 20L * 6L) {
            entity.discard();
        }
    }

    private static ServerPlayer targetPlayer(
            ServerLevel level,
            CompoundTag data
    ) {
        if (!data.contains(
                TARGET_PLAYER
        )) {
            return null;
        }

        try {
            UUID id =
                    UUID.fromString(
                            data.getString(
                                    TARGET_PLAYER
                            )
                    );

            return level.getServer()
                    .getPlayerList()
                    .getPlayer(
                            id
                    );
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void walkLikePlayerToward(
            ServerLevel level,
            HerobrineEntity entity,
            BlockPos target,
            CompoundTag data,
            double speed
    ) {
        long now =
                level.getGameTime();

        if (now < data.getLong(
                PAUSE_UNTIL
        )) {
            entity.getNavigation()
                    .stop();

            entity.lookNaturallyAt(
                    target.getX()
                            + 0.5
                            + (
                            level.random.nextDouble()
                                    - 0.5
                    )
                            * 2.0,
                    target.getY()
                            + 0.7,
                    target.getZ()
                            + 0.5
                            + (
                            level.random.nextDouble()
                                    - 0.5
                    )
                            * 2.0
            );

            return;
        }

        if (now >= data.getLong(
                NEXT_DECISION
        )) {
            data.putLong(
                    NEXT_DECISION,
                    now
                            + 14L
                            + level.random.nextInt(
                            24
                    )
            );

            if (level.random.nextFloat()
                    < 0.22F) {
                data.putLong(
                        PAUSE_UNTIL,
                        now
                                + 10L
                                + level.random.nextInt(
                                28
                        )
                );

                entity.getNavigation()
                        .stop();

                return;
            }

            Vec3 toTarget =
                    Vec3.atCenterOf(
                                    target
                            )
                            .subtract(
                                    entity.position()
                            )
                            .multiply(
                                    1.0,
                                    0.0,
                                    1.0
                            );

            Vec3 side =
                    toTarget.lengthSqr()
                            < 0.001
                            ? Vec3.ZERO
                            : new Vec3(
                            -toTarget.z,
                            0.0,
                            toTarget.x
                    )
                            .normalize()
                            .scale(
                                    (
                                            level.random.nextDouble()
                                                    - 0.5
                                    )
                                            * 2.6
                            );

            Vec3 waypoint =
                    Vec3.atCenterOf(
                                    target
                            )
                            .add(
                                    side
                            );

            entity.getNavigation()
                    .moveTo(
                            waypoint.x,
                            waypoint.y,
                            waypoint.z,
                            speed
                                    * (
                                    0.92
                                            + level.random.nextDouble()
                                            * 0.14
                            )
                    );

            entity.lookNaturallyAt(
                    waypoint.x,
                    waypoint.y
                            + 0.9,
                    waypoint.z
            );

            if (entity.onGround()
                    && level.random.nextFloat()
                    < 0.055F) {
                entity.getJumpControl()
                        .jump();
            }
        }
    }

    private static void leaveLikePlayer(
            ServerLevel level,
            HerobrineEntity entity,
            BlockPos from,
            long phaseAge
    ) {
        ServerPlayer observer =
                observingPlayer(
                        level,
                        entity
                );

        Vec3 away =
                entity.position()
                        .subtract(
                                observer != null
                                        ? observer.position()
                                        : Vec3.atCenterOf(
                                        from
                                )
                        )
                        .multiply(
                                1.0,
                                0.0,
                                1.0
                        );

        if (away.lengthSqr()
                < 0.001) {
            away =
                    new Vec3(
                            1.0,
                            0.0,
                            0.0
                    );
        } else {
            away =
                    away.normalize();
        }

        Vec3 target =
                entity.position()
                        .add(
                                away.scale(
                                        14.0
                                )
                        );

        if (entity.tickCount % 16 == 0
                || entity.getNavigation()
                .isDone()) {
            entity.getNavigation()
                    .moveTo(
                            target.x,
                            target.y,
                            target.z,
                            observer == null
                                    ? 1.08
                                    : 1.32
                    );

            entity.lookNaturallyAt(
                    target.x,
                    target.y
                            + 0.7,
                    target.z
            );
        }

        if (entity.onGround()
                && level.random.nextFloat()
                < 0.035F) {
            entity.getJumpControl()
                    .jump();
        }

        if (observer != null
                && entity.tickCount % 24 == 0) {
            // Brief look back over the shoulder while walking away.
            entity.lookNaturallyAt(
                    observer.getX(),
                    observer.getEyeY(),
                    observer.getZ()
            );
        }

        if (observer == null
                && phaseAge > 35L
                && nearestPlayerDistanceSqr(
                level,
                entity
        ) > 18.0 * 18.0) {
            entity.discard();
        }
    }

    private static void fleeLikePlayer(
            ServerLevel level,
            HerobrineEntity entity,
            ServerPlayer observer,
            long phaseAge
    ) {
        ServerPlayer threat =
                observer != null
                        ? observer
                        : nearestPlayer(
                        level,
                        entity
                );

        BlockPos from =
                threat != null
                        ? threat.blockPosition()
                        : entity.blockPosition()
                        .offset(
                                -4,
                                0,
                                0
                        );

        leaveLikePlayer(
                level,
                entity,
                from,
                phaseAge
        );
    }

    private static ServerPlayer observingPlayer(
            ServerLevel level,
            HerobrineEntity entity
    ) {
        ServerPlayer best =
                null;

        double bestDistance =
                Double.MAX_VALUE;

        for (ServerPlayer player :
                level.players()) {
            if (player.isSpectator()) {
                continue;
            }

            double distance =
                    player.distanceToSqr(
                            entity
                    );

            if (distance > 48.0 * 48.0
                    || !isLookingAt(
                    player,
                    entity,
                    0.80
            )) {
                continue;
            }

            if (distance < bestDistance) {
                bestDistance =
                        distance;
                best =
                        player;
            }
        }

        return best;
    }

    private static boolean isLookingAt(
            ServerPlayer player,
            Entity entity,
            double minDot
    ) {
        Vec3 to =
                entity.getEyePosition()
                        .subtract(
                                player.getEyePosition()
                        );

        if (to.lengthSqr()
                < 0.001) {
            return true;
        }

        return player.getLookAngle()
                .dot(
                        to.normalize()
                )
                >= minDot
                && player.hasLineOfSight(
                entity
        );
    }

    private static ServerPlayer nearestPlayer(
            ServerLevel level,
            HerobrineEntity entity
    ) {
        return level.players()
                .stream()
                .min(
                        Comparator.comparingDouble(
                                entity::distanceToSqr
                        )
                )
                .orElse(null);
    }

    private static double nearestPlayerDistanceSqr(
            ServerLevel level,
            HerobrineEntity entity
    ) {
        ServerPlayer nearest =
                nearestPlayer(
                        level,
                        entity
                );

        return nearest == null
                ? Double.MAX_VALUE
                : nearest.distanceToSqr(
                entity
        );
    }

    private static void enterPhase(
            CompoundTag data,
            int phase,
            long now
    ) {
        data.putInt(
                PHASE,
                phase
        );

        data.putLong(
                ACTION_AT,
                now
        );

        data.remove(
                PAUSE_UNTIL
        );

        data.remove(
                NEXT_DECISION
        );
    }

    private static BlockPos performSabotageAction(
            ServerLevel level,
            HerobrineEntity entity,
            BlockPos target,
            int action,
            String blame
    ) {
        if (action == ACTION_ARSON) {
            entity.swing(
                    net.minecraft.world.InteractionHand.MAIN_HAND
            );

            ignite(
                    level,
                    target
            );

            if (!blame.isBlank()) {
                return placeSignature(
                        level,
                        target,
                        "- " + blame
                );
            }

            return null;
        }

        if (action == ACTION_THEFT
                && level.getBlockEntity(
                target
        ) instanceof ChestBlockEntity chest) {
            ArrayList<Integer> slots =
                    new ArrayList<>();

            for (int slot = 0;
                 slot < chest.getContainerSize();
                 slot++) {
                if (!chest.getItem(
                        slot
                ).isEmpty()) {
                    slots.add(
                            slot
                    );
                }
            }

            if (!slots.isEmpty()) {
                int slot =
                        slots.get(
                                level.random.nextInt(
                                        slots.size()
                                )
                        );

                chest.getItem(
                                slot
                        )
                        .shrink(
                                1
                        );

                chest.setChanged();
            }

            BlockState chestState =
                    level.getBlockState(
                            target
                    );

            level.blockEvent(
                    target,
                    chestState.getBlock(),
                    1,
                    0
            );

            if (!blame.isBlank()) {
                return placeSignature(
                        level,
                        target,
                        ":) - " + blame
                );
            }
        }

        return null;
    }

    private static void ignite(
            ServerLevel level,
            BlockPos wood
    ) {
        for (Direction direction :
                Direction.values()) {
            BlockPos firePos =
                    wood.relative(
                            direction
                    );

            if (!level.getBlockState(
                    firePos
            ).isAir()) {
                continue;
            }

            level.setBlock(
                    firePos,
                    Blocks.FIRE
                            .defaultBlockState(),
                    3
            );

            level.playSound(
                    null,
                    firePos,
                    SoundEvents.FLINTANDSTEEL_USE,
                    SoundSource.BLOCKS,
                    0.8F,
                    0.94F
            );

            break;
        }
    }

    private static BlockPos placeSignature(
            ServerLevel level,
            BlockPos target,
            String message
    ) {
        for (int radius = 1;
             radius <= 4;
             radius++) {
            for (Direction direction :
                    Direction.Plane.HORIZONTAL) {
                BlockPos candidate =
                        target.relative(
                                direction,
                                radius
                        );

                if (!level.getBlockState(
                        candidate
                ).isAir()
                        || !level.getBlockState(
                        candidate.below()
                ).isFaceSturdy(
                        level,
                        candidate.below(),
                        Direction.UP
                )) {
                    continue;
                }

                level.setBlock(
                        candidate,
                        Blocks.OAK_SIGN
                                .defaultBlockState(),
                        3
                );

                if (level.getBlockEntity(
                        candidate
                ) instanceof SignBlockEntity sign) {
                    SignText text =
                            sign.getFrontText()
                                    .setMessage(
                                            0,
                                            Component.literal(
                                                    message
                                            )
                                    );

                    sign.setText(
                            text,
                            true
                    );

                    sign.setChanged();

                    level.sendBlockUpdated(
                            candidate,
                            sign.getBlockState(),
                            sign.getBlockState(),
                            3
                    );
                }

                return candidate;
            }
        }

        return null;
    }
}
