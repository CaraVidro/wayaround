package net.caravidro.wayaround.industrial.pipework;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

public final class IndustrialPipeBlock extends PipeBlock {

    public static final MapCodec<IndustrialPipeBlock> CODEC =
            MapCodec.unit(() -> new IndustrialPipeBlock(
                    PipeworkContent.SMALL_COPPER_SPEC,
                    Properties.of().noOcclusion()
            ));

    private final PipeSpec spec;

    public IndustrialPipeBlock(
            PipeSpec spec,
            Properties properties
    ) {
        super(spec.radius(), properties);
        this.spec = spec;

        BlockState initial =
                stateDefinition.any();

        for (Direction direction : Direction.values()) {
            initial = initial.setValue(
                    PROPERTY_BY_DIRECTION.get(direction),
                    false
            );
        }

        registerDefaultState(initial);
    }

    public PipeSpec spec() {
        return spec;
    }

    @Override
    protected MapCodec<? extends PipeBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        BlockState state =
                defaultBlockState();

        for (Direction direction : Direction.values()) {
            state =
                    state.setValue(
                            PROPERTY_BY_DIRECTION.get(direction),
                            connects(
                                    context.getLevel(),
                                    context.getClickedPos().relative(direction)
                            )
                    );
        }

        return state;
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction side,
            BlockState neighbor,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        return state.setValue(
                PROPERTY_BY_DIRECTION.get(side),
                connects(level, neighborPos)
        );
    }

    private boolean connects(
            LevelAccessor level,
            BlockPos pos
    ) {
        if (!level.hasChunkAt(pos)) {
            return false;
        }

        Block other =
                level.getBlockState(pos)
                        .getBlock();

        return other instanceof IndustrialPipeBlock pipe
                && spec.compatible(pipe.spec());
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (!level.isClientSide) {
            PipeNetwork.NetworkInfo info =
                    PipeNetwork.inspect(
                            level,
                            pos
                    );

            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.pipework.status",
                            Component.translatable(
                                    "pipe.wayaround."
                                            + spec.id()
                            ),
                            spec.flowPerTick(),
                            spec.maxPressureBar(),
                            spec.maxTemperatureC(),
                            info.pipeCount(),
                            info.bottleneckFlowPerTick(),
                            info.bottleneckPressureBar()
                    ),
                    false
            );
        }

        return InteractionResult.sidedSuccess(
                level.isClientSide
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                NORTH,
                EAST,
                SOUTH,
                WEST,
                UP,
                DOWN
        );
    }
}
