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
                                                                                    "Eventos: door, footsteps, tunnel, pyramid, sabotage, arson, theft, appear, all"
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

                    case "appear" -> {
                        spawnApparition(
                                target.serverLevel(),
                                target.blockPosition(),
                                0,
                                ""
                        );
                        yield true;
                    }

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

        if (blame.isBlank()) {
            for (ServerPlayer other :
                    victim.server
                            .getPlayerList()
                            .getPlayers()) {
                if (other != victim) {
                    blame =
                            other.getGameProfile()
                                    .getName();
                    break;
                }
            }
        }

        if (blame.isBlank()) {
            blame =
                    "???";
        }

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

        if (level.getEntitiesOfClass(
                HerobrineEntity.class,
                victim.getBoundingBox()
                        .inflate(
                                128.0
                        )
        ).size() > 0) {
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
        )
                || playerNear(
                level,
                base,
                28.0
        )) {
            return;
        }

        String blame =
                strongestAssociation(
                        victim.server,
                        victim.getUUID()
                );

        if (blame.isBlank()) {
            return;
        }

        JusticeSenseData justice =
                JusticeSenseData.get(
                        victim.server
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
                justice.chestPositions(
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

        if (chest != null
                && !playerNear(
                level,
                chest,
                24.0
        )) {
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

        HerobrineEntity entity =
                OldFriendContent.HEROBRINE.get()
                        .create(
                                level
                        );

        if (entity == null) {
            return;
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
                blame
        );

        data.putInt(
                PHASE,
                0
        );

        data.putLong(
                ACTION_AT,
                level.getGameTime()
        );

        level.addFreshEntity(
                entity
        );
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

        if (!data.contains(
                TARGET
        )) {
            entity.discard();
            return;
        }

        BlockPos target =
                BlockPos.of(
                        data.getLong(
                                TARGET
                        )
                );

        int phase =
                data.getInt(
                        PHASE
                );

        long age =
                level.getGameTime()
                        - data.getLong(
                        ACTION_AT
                );

        if (age > 20L * 35L) {
            entity.discard();
            return;
        }

        if (phase == 0) {
            entity.getNavigation()
                    .moveTo(
                            target.getX()
                                    + 0.5,
                            target.getY(),
                            target.getZ()
                                    + 0.5,
                            1.18
                    );

            if (entity.distanceToSqr(
                    Vec3.atCenterOf(
                            target
                    )
            ) <= 3.2 * 3.2
                    || age > 20L * 14L) {
                performAction(
                        level,
                        entity,
                        target,
                        data.getInt(
                                ACTION
                        ),
                        data.getString(
                                BLAME
                        )
                );

                data.putInt(
                        PHASE,
                        1
                );

                data.putLong(
                        ACTION_AT,
                        level.getGameTime()
                );
            }

            return;
        }

        Vec3 away =
                entity.position()
                        .subtract(
                                Vec3.atCenterOf(
                                        target
                                )
                        )
                        .multiply(
                                1.0,
                                0.0,
                                1.0
                        );

        if (away.lengthSqr()
                < 0.01) {
            away =
                    new Vec3(
                            1.0,
                            0.0,
                            0.0
                    );
        }

        Vec3 escape =
                entity.position()
                        .add(
                                away.normalize()
                                        .scale(
                                                16.0
                                        )
                        );

        entity.getNavigation()
                .moveTo(
                        escape.x,
                        escape.y,
                        escape.z,
                        1.46
                );

        if (age > 20L * 5L
                || entity.distanceToSqr(
                escape
        ) < 3.0 * 3.0) {
            entity.discard();
        }
    }

    private static void performAction(
            ServerLevel level,
            HerobrineEntity entity,
            BlockPos target,
            int action,
            String blame
    ) {
        if (action == ACTION_ARSON) {
            ignite(
                    level,
                    target
            );

            placeSignature(
                    level,
                    target,
                    "- " + blame
            );

            return;
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

            placeSignature(
                    level,
                    target,
                    ":) - " + blame
            );
        }
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

    private static void placeSignature(
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

                return;
            }
        }
    }
}
