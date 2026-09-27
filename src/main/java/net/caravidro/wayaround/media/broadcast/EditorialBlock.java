package net.caravidro.wayaround.media.broadcast;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.caravidro.wayaround.media.MediaContent;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;

public final class EditorialBlock extends BaseEntityBlock {
    public static final MapCodec<EditorialBlock> CODEC = simpleCodec(EditorialBlock::new);

    public EditorialBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<EditorialBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EditorialBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!WorldFeatureRuntime.enabled(level, WorldFeature.MEDIA)) return InteractionResult.PASS;
        if (level.getBlockEntity(pos) instanceof EditorialBlockEntity editorial) {
            if (!level.isClientSide) {
                if (player.isShiftKeyDown()) {
                    editorial.nextEffect();
                } else {
                    editorial.nextMode();
                }
                player.displayClientMessage(
                        Component.literal("Editoria: " + editorial.mode().name()
                                + " | efeito " + editorial.effect().name()),
                        true
                );
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
