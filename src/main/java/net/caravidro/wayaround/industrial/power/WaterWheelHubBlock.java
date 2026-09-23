package net.caravidro.wayaround.industrial.power;

import com.mojang.serialization.MapCodec;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class WaterWheelHubBlock
        extends BaseEntityBlock {

    public static final MapCodec<WaterWheelHubBlock> CODEC =
            simpleCodec(WaterWheelHubBlock::new);

    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING =
            HorizontalDirectionalBlock.FACING;

    public static final BooleanProperty DOUBLE =
            BooleanProperty.create(
                    "double"
            );

    public WaterWheelHubBlock(
            Properties properties
    ) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(
                                FACING,
                                Direction.NORTH
                        )
                        .setValue(
                                DOUBLE,
                                false
                        )
        );
    }

    @Override
    protected MapCodec<WaterWheelHubBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        BlockState state =
                defaultBlockState()
                        .setValue(
                                FACING,
                                context.getHorizontalDirection()
                                        .getOpposite()
                        )
                        .setValue(
                                DOUBLE,
                                false
                        );

        if (!hasSupport(
                context.getLevel(),
                context.getClickedPos(),
                state
        )) {
            return null;
        }

        if (!hasWheelClearance(
                context.getLevel(),
                context.getClickedPos(),
                state
        )) {
            return null;
        }

        return state;
    }

    @Override
    protected RenderShape getRenderShape(
            BlockState state
    ) {
        /*
         * The physical block remains the interaction/collision anchor, but all
         * visible geometry is rendered by WaterWheelHubRenderer. This removes
         * the old static cube from the center.
         */
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new WaterWheelHubBlockEntity(
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
                PowerContent.WATER_WHEEL_HUB_ENTITY.get(),
                WaterWheelHubBlockEntity::serverTick
        );
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (stack.is(
                PowerContent.WATER_WHEEL_HUB_ITEM.get()
        )) {
            if (state.getValue(DOUBLE)) {
                return ItemInteractionResult.CONSUME;
            }

            if (!level.isClientSide) {
                level.setBlock(
                        pos,
                        state.setValue(
                                DOUBLE,
                                true
                        ),
                        Block.UPDATE_ALL
                );

                stack.consume(
                        1,
                        player
                );

                if (level.getBlockEntity(pos)
                        instanceof WaterWheelHubBlockEntity hub) {
                    hub.configurationChanged();
                }

                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(
                                "message.wayaround.water_wheel.double_body"
                        ),
                        true
                );
            }

            return ItemInteractionResult.sidedSuccess(
                    level.isClientSide
            );
        }

        /*
         * The old blade BlockItem is now used as a construction plate when
         * clicked on a wheel body. It can still exist as a legacy block, but
         * wheel construction itself is stored inside this block entity.
         */
        if (stack.is(
                PowerContent.WATER_WHEEL_BLADE_ITEM.get()
        )) {
            if (!level.isClientSide
                    && level.getBlockEntity(pos)
                    instanceof WaterWheelHubBlockEntity hub) {

                if (hub.addPlate()) {
                    stack.consume(
                            1,
                            player
                    );

                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable(
                                    "message.wayaround.water_wheel.plate_added",
                                    hub.plateCount()
                            ),
                            true
                    );
                } else {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable(
                                    "message.wayaround.water_wheel.plate_limit",
                                    WaterWheelHubBlockEntity.MAX_PLATES
                            ),
                            true
                    );
                }
            }

            return ItemInteractionResult.sidedSuccess(
                    level.isClientSide
            );
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (!level.isClientSide
                && level.getBlockEntity(pos)
                instanceof WaterWheelHubBlockEntity hub) {

            if (hub.plateCount() > 0) {
                int index =
                        hub.adjustSelectedPlate(
                                player.isShiftKeyDown()
                        );

                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(
                                "message.wayaround.water_wheel.plate_angle",
                                index + 1,
                                Math.round(
                                        hub.plateTiltDegrees(
                                                index
                                        )
                                )
                        ),
                        true
                );
            } else {
                player.displayClientMessage(
                        hub.status(),
                        true
                );
            }
        }

        return InteractionResult.sidedSuccess(
                level.isClientSide
        );
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        return super.updateShape(
                state,
                direction,
                neighborState,
                level,
                pos,
                neighborPos
        );
    }

    static boolean hasSupport(
            LevelReader level,
            BlockPos pos,
            BlockState state
    ) {
        /*
         * Dedicated supports can hold the hub from any neighboring side,
         * including a vertical post directly below it.
         */
        for (Direction direction :
                Direction.values()) {

            if (level.getBlockState(
                    pos.relative(direction)
            ).getBlock()
                    instanceof WaterWheelSupportBlock) {
                return true;
            }
        }

        /*
         * Ordinary construction only counts when it supports the axle itself:
         * a wall on either side of the shaft.
         */
        Direction.Axis axis =
                state.getValue(FACING)
                        .getAxis();

        Direction first =
                axis == Direction.Axis.X
                        ? Direction.EAST
                        : Direction.NORTH;

        Direction second =
                first.getOpposite();

        return sturdyAxleSide(
                level,
                pos,
                first
        )
                || sturdyAxleSide(
                        level,
                        pos,
                        second
                );
    }

    private static boolean sturdyAxleSide(
            LevelReader level,
            BlockPos pos,
            Direction direction
    ) {
        BlockPos neighbor =
                pos.relative(direction);

        BlockState neighborState =
                level.getBlockState(neighbor);

        return neighborState.isFaceSturdy(
                level,
                neighbor,
                direction.getOpposite()
        );
    }

    private static boolean hasWheelClearance(
            Level level,
            BlockPos pos,
            BlockState state
    ) {
        Direction.Axis axle =
                state.getValue(FACING)
                        .getAxis();

        for (int vertical = -3;
                vertical <= 3;
                vertical++) {

            for (int lateral = -3;
                    lateral <= 3;
                    lateral++) {

                if (vertical == 0
                        && lateral == 0) {
                    continue;
                }

                double radius =
                        Math.sqrt(
                                vertical * vertical
                                + lateral * lateral
                        );

                if (radius > 3.05) {
                    continue;
                }

                BlockPos check =
                        axle == Direction.Axis.X
                                ? pos.offset(
                                        0,
                                        vertical,
                                        lateral
                                )
                                : pos.offset(
                                        lateral,
                                        vertical,
                                        0
                                );

                BlockState candidate =
                        level.getBlockState(check);

                if (candidate.getBlock()
                        instanceof WaterWheelSupportBlock) {
                    continue;
                }

                if (!candidate.getFluidState()
                        .isEmpty()) {
                    continue;
                }

                if (candidate.getCollisionShape(
                        level,
                        check
                ).isEmpty()) {
                    continue;
                }

                return false;
            }
        }

        return true;
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
                && !level.isClientSide
                && level.getBlockEntity(pos)
                instanceof WaterWheelHubBlockEntity hub) {

            /*
             * The block's normal loot returns the first body. The second
             * consumed body and all internally installed plates are returned
             * here so configuration never becomes a resource black hole.
             */
            if (state.getValue(DOUBLE)) {
                popResource(
                        level,
                        pos,
                        new ItemStack(
                                PowerContent.WATER_WHEEL_HUB_ITEM.get()
                        )
                );
            }

            if (hub.plateCount() > 0) {
                popResource(
                        level,
                        pos,
                        new ItemStack(
                                PowerContent.WATER_WHEEL_BLADE_ITEM.get(),
                                hub.plateCount()
                        )
                );
            }
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
                FACING,
                DOUBLE
        );
    }
}
