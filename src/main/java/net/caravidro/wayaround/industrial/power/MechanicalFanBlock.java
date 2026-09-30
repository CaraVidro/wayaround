package net.caravidro.wayaround.industrial.power;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class MechanicalFanBlock extends BaseEntityBlock {

    public static final MapCodec<MechanicalFanBlock> CODEC =
            simpleCodec(
                    MechanicalFanBlock::new
            );

    public static final DirectionProperty FACING =
            BlockStateProperties.FACING;

    public MechanicalFanBlock(
            Properties properties
    ) {
        super(
                properties
        );

        registerDefaultState(
                stateDefinition.any()
                        .setValue(
                                FACING,
                                Direction.NORTH
                        )
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
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
                        context.getNearestLookingDirection()
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
        return new MechanicalFanBlockEntity(
                pos,
                state
        );
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return level.isClientSide
                ? null
                : createTickerHelper(
                        type,
                        PowerContent.MECHANICAL_FAN_ENTITY.get(),
                        MechanicalFanBlockEntity::serverTick
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
        if (!level.isClientSide
                && level.getBlockEntity(
                pos
        ) instanceof MechanicalFanBlockEntity fan) {

            player.displayClientMessage(
                    Component.translatable(
                            "message.wayaround.mechanical_fan.status",
                            String.format(
                                    java.util.Locale.ROOT,
                                    "%.1f",
                                    fan.rpm()
                            ),
                            Math.round(
                                    fan.airflow()
                                            * 100.0F
                            ),
                            Math.round(
                                    fan.pressure()
                                            * 100.0F
                            ),
                            fan.blocked()
                                    ? Component.translatable(
                                    "message.wayaround.mechanical_fan.blocked"
                            ).getString()
                                    : Component.translatable(
                                    "message.wayaround.mechanical_fan.clear"
                            ).getString()
                    ),
                    true
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
                FACING
        );
    }
}
