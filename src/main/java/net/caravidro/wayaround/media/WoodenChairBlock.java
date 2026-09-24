package net.caravidro.wayaround.media;

import com.mojang.serialization.MapCodec;

import javax.annotation.Nullable;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

public final class WoodenChairBlock
        extends BaseEntityBlock {

    public static final MapCodec<WoodenChairBlock> CODEC =
            simpleCodec(
                    WoodenChairBlock::new
            );

    public static final net.minecraft.world.level.block.state.properties.DirectionProperty
            FACING =
            HorizontalDirectionalBlock.FACING;

    public WoodenChairBlock(
            Properties properties
    ) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(
                                FACING,
                                Direction.NORTH
                        )
        );
    }

    @Override
    protected MapCodec<WoodenChairBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        return defaultBlockState()
                .setValue(
                        FACING,
                        context.getHorizontalDirection()
                                .getOpposite()
                );
    }

    @Override
    protected RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new ChairBlockEntity(
                pos,
                state
        );
    }

    @Nullable
    @Override
    public <T extends BlockEntity>
            BlockEntityTicker<T> getTicker(
                    Level level,
                    BlockState state,
                    BlockEntityType<T> type
            ) {
        if (level.isClientSide) {
            return null;
        }

        return createTickerHelper(
                type,
                MediaContent.CHAIR_ENTITY.get(),
                ChairBlockEntity::serverTick
        );
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        AABB area =
                new AABB(pos)
                        .inflate(
                                0.35,
                                0.65,
                                0.35
                        );

        List<ArmorStand> seats =
                level.getEntitiesOfClass(
                        ArmorStand.class,
                        area,
                        seat ->
                                seat.getTags()
                                        .contains(
                                                ChairBlockEntity.SEAT_TAG
                                        )
                );

        ArmorStand seat =
                seats.stream()
                        .filter(
                                existing ->
                                        existing.getPassengers()
                                                .isEmpty()
                        )
                        .findFirst()
                        .orElse(null);

        if (seat == null) {
            seat =
                    new ArmorStand(
                            level,
                            pos.getX() + 0.5,
                            pos.getY() - 0.60,
                            pos.getZ() + 0.5
                    );

            seat.setInvisible(
                    true
            );
            seat.setSilent(
                    true
            );
            seat.setNoGravity(
                    true
            );
            seat.setSmall(
                    true
            );
            seat.setMarker(
                    true
            );
            seat.setInvulnerable(
                    true
            );
            seat.addTag(
                    ChairBlockEntity.SEAT_TAG
            );

            level.addFreshEntity(
                    seat
            );
        }

        player.startRiding(
                seat,
                true
        );

        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState replacement,
            boolean moving
    ) {
        if (!state.is(
                replacement.getBlock()
        )
                && !level.isClientSide) {

            ChairBlockEntity.removeSeat(
                    level,
                    pos
            );
        }

        super.onRemove(
                state,
                level,
                pos,
                replacement,
                moving
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                FACING
        );
    }
}
