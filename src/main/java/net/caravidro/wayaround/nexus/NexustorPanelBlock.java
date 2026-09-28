package net.caravidro.wayaround.nexus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class NexustorPanelBlock extends Block {

    public static final IntegerProperty LEVEL =
            IntegerProperty.create(
                    "level",
                    0,
                    5
            );

    public NexustorPanelBlock(
            Properties properties
    ) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(
                                LEVEL,
                                0
                        )
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
                && player instanceof ServerPlayer serverPlayer) {
            BlockPos base =
                    NexusEventManager.findNearbyBase(
                            (ServerLevel) level,
                            pos,
                            8
                    );

            if (base != null
                    && level.getBlockEntity(base)
                    instanceof NexustorBaseBlockEntity reactor) {
                serverPlayer.displayClientMessage(
                        reactor.status(),
                        false
                );
            } else {
                serverPlayer.displayClientMessage(
                        Component.translatable(
                                "message.wayaround.nexustor.panel_offline"
                        ),
                        true
                );
            }
        }

        return InteractionResult.sidedSuccess(
                level.isClientSide
        );
    }

    public static BlockState withProgress(
            BlockState state,
            float progress
    ) {
        return state.setValue(
                LEVEL,
                Mth.clamp(
                        Math.round(progress * 5.0F),
                        0,
                        5
                )
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(LEVEL);
    }
}
