package net.caravidro.wayaround.media;

import com.mojang.serialization.MapCodec;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Deliberately dumb voice assistant.
 *
 * It understands a tiny phrase set, pulses redstone on itself when it accepts
 * a command, and has one special party trick: remotely toggling redstone lamps
 * in a 30-block sphere.
 */
public final class AlexaBlock extends Block {

    public static final MapCodec<AlexaBlock> CODEC =
            simpleCodec(AlexaBlock::new);

    public static final BooleanProperty POWERED =
            BlockStateProperties.POWERED;

    public static final int LISTEN_RANGE =
            16;

    public static final int LAMP_RANGE =
            30;

    private static final VoxelShape SHAPE =
            Block.box(
                    3.0,
                    0.0,
                    3.0,
                    13.0,
                    10.0,
                    13.0
            );

    public AlexaBlock(
            Properties properties
    ) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(
                                POWERED,
                                false
                        )
        );
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                POWERED
        );
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    protected boolean isSignalSource(
            BlockState state
    ) {
        return true;
    }

    @Override
    protected int getSignal(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction
    ) {
        return state.getValue(
                POWERED
        )
                ? 15
                : 0;
    }

    @Override
    protected int getDirectSignal(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction
    ) {
        return getSignal(
                state,
                level,
                pos,
                direction
        );
    }

    @Override
    protected void tick(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random
    ) {
        if (!state.getValue(
                POWERED
        )) {
            return;
        }

        level.setBlock(
                pos,
                state.setValue(
                        POWERED,
                        false
                ),
                Block.UPDATE_ALL
        );
    }

    public static void handleVoice(
            ServerPlayer player,
            String transcript
    ) {
        if (!WorldFeatureRuntime.enabled(
                player.level(),
                WorldFeature.MEDIA
        )
                || transcript == null
                || transcript.isBlank()) {
            return;
        }

        BlockPos alexa =
                nearestAlexa(
                        player
                );

        if (alexa == null) {
            return;
        }

        ServerLevel level =
                player.serverLevel();

        String text =
                normalize(
                        transcript
                );

        boolean lamps =
                text.contains(
                        "lampad"
                )
                        || text.contains(
                        "luz"
                )
                        || text.contains(
                        "light"
                );

        boolean turnOn =
                text.contains(
                        "acend"
                )
                        || text.contains(
                        "ascend"
                )
                        || text.contains(
                        "liga"
                )
                        || text.contains(
                        "ativ"
                )
                        || text.contains(
                        "on"
                );

        boolean turnOff =
                text.contains(
                        "apag"
                )
                        || text.contains(
                        "desliga"
                )
                        || text.contains(
                        "desativ"
                )
                        || text.contains(
                        "off"
                );

        boolean all =
                text.contains(
                        "todas"
                )
                        || text.contains(
                        "todos"
                )
                        || text.contains(
                        "tudo"
                )
                        || text.contains(
                        "all"
                )
                        || text.contains(
                        "every"
                );

        boolean understood =
                false;

        if (lamps
                && (turnOn
                || turnOff)) {

            int changed =
                    operateLamps(
                            level,
                            alexa,
                            turnOn
                                    && !turnOff,
                            all
                    );

            understood =
                    changed > 0;
        } else if (
                text.contains(
                        "redstone"
                )
                        || text.contains(
                        "pulso"
                )
                        || text.contains(
                        "pulse"
                )
        ) {
            understood =
                    true;
        }

        if (!understood) {
            return;
        }

        pulse(
                level,
                alexa
        );
    }

    private static BlockPos nearestAlexa(
            ServerPlayer player
    ) {
        ServerLevel level =
                player.serverLevel();

        BlockPos origin =
                player.blockPosition();

        BlockPos.MutableBlockPos cursor =
                new BlockPos.MutableBlockPos();

        BlockPos best =
                null;

        double bestDistance =
                Double.MAX_VALUE;

        int range =
                LISTEN_RANGE;

        for (int dx = -range;
             dx <= range;
             dx++) {
            for (int dy = -range;
                 dy <= range;
                 dy++) {
                for (int dz = -range;
                     dz <= range;
                     dz++) {

                    int distanceSq =
                            dx * dx
                                    + dy * dy
                                    + dz * dz;

                    if (distanceSq
                            > range * range) {
                        continue;
                    }

                    cursor.set(
                            origin.getX() + dx,
                            origin.getY() + dy,
                            origin.getZ() + dz
                    );

                    if (!level.getBlockState(
                            cursor
                    ).is(
                            MediaContent.ALEXA.get()
                    )) {
                        continue;
                    }

                    if (distanceSq
                            < bestDistance) {
                        bestDistance =
                                distanceSq;

                        best =
                                cursor.immutable();
                    }
                }
            }
        }

        return best;
    }

    private static int operateLamps(
            ServerLevel level,
            BlockPos center,
            boolean on,
            boolean all
    ) {
        List<BlockPos> lamps =
                new ArrayList<>();

        BlockPos.MutableBlockPos cursor =
                new BlockPos.MutableBlockPos();

        int range =
                LAMP_RANGE;

        for (int dx = -range;
             dx <= range;
             dx++) {
            for (int dy = -range;
                 dy <= range;
                 dy++) {
                for (int dz = -range;
                     dz <= range;
                     dz++) {

                    if (dx * dx
                            + dy * dy
                            + dz * dz
                            > range * range) {
                        continue;
                    }

                    cursor.set(
                            center.getX() + dx,
                            center.getY() + dy,
                            center.getZ() + dz
                    );

                    BlockState state =
                            level.getBlockState(
                                    cursor
                            );

                    if (state.is(
                            Blocks.REDSTONE_LAMP
                    )) {
                        lamps.add(
                                cursor.immutable()
                        );
                    }
                }
            }
        }

        if (lamps.isEmpty()) {
            return 0;
        }

        if (!all) {
            BlockPos chosen =
                    lamps.get(
                            level.random.nextInt(
                                    lamps.size()
                            )
                    );

            setLamp(
                    level,
                    chosen,
                    on
            );

            return 1;
        }

        int changed =
                0;

        for (BlockPos lamp :
                lamps) {
            setLamp(
                    level,
                    lamp,
                    on
            );

            changed++;
        }

        return changed;
    }

    private static void setLamp(
            ServerLevel level,
            BlockPos pos,
            boolean on
    ) {
        BlockState state =
                level.getBlockState(
                        pos
                );

        if (!state.is(
                Blocks.REDSTONE_LAMP
        )) {
            return;
        }

        level.setBlock(
                pos,
                state.setValue(
                        RedstoneLampBlock.LIT,
                        on
                ),
                Block.UPDATE_CLIENTS
        );
    }

    private static void pulse(
            ServerLevel level,
            BlockPos pos
    ) {
        BlockState state =
                level.getBlockState(
                        pos
                );

        if (!state.is(
                MediaContent.ALEXA.get()
        )) {
            return;
        }

        level.setBlock(
                pos,
                state.setValue(
                        POWERED,
                        true
                ),
                Block.UPDATE_ALL
        );

        level.scheduleTick(
                pos,
                MediaContent.ALEXA.get(),
                12
        );

        level.playSound(
                null,
                pos,
                SoundEvents.NOTE_BLOCK_PLING.value(),
                SoundSource.BLOCKS,
                0.72F,
                1.65F
        );
    }

    private static String normalize(
            String input
    ) {
        String decomposed =
                Normalizer.normalize(
                        input,
                        Normalizer.Form.NFD
                );

        return decomposed
                .replaceAll(
                        "\\p{M}+",
                        ""
                )
                .toLowerCase(
                        Locale.ROOT
                )
                .trim();
    }
}
