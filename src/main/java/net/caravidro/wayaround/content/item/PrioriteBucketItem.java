package net.caravidro.wayaround.content.item;

import net.caravidro.wayaround.content.WayAroundContent;
import net.caravidro.wayaround.block.PrioriteBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class PrioriteBucketItem extends Item {

    public PrioriteBucketItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(
            UseOnContext context
    ) {

        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clickedPos);

        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        /*
         * Se clicou numa poça já existente,
         * enche até 4.
         */
        if (clickedState.is(WayAroundContent.PRIORITE.get())) {

            int amount =
                    clickedState.getValue(
                            PrioriteBlock.AMOUNT
                    );

            if (amount < 4) {

                if (!level.isClientSide) {
                    level.setBlock(
                            clickedPos,
                            clickedState.setValue(
                                    PrioriteBlock.AMOUNT,
                                    4
                            ),
                            3
                    );

                    consumeAndGiveEmptyBucket(
                            player,
                            stack,
                            context
                    );

                    level.playSound(
                            null,
                            clickedPos,
                            SoundEvents.BUCKET_EMPTY_LAVA,
                            SoundSource.BLOCKS,
                            1.0F,
                            0.75F
                    );
                }

                return InteractionResult.SUCCESS;
            }
        }

        /*
         * Se o bloco clicado pode ser substituído,
         * coloca ali.
         * Senão, coloca no bloco adjacente.
         */
        BlockPos placePos =
                clickedState.canBeReplaced()
                        ? clickedPos
                        : clickedPos.relative(
                                context.getClickedFace()
                        );

        BlockState placeState =
                level.getBlockState(placePos);

        if (!placeState.canBeReplaced()) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide) {

            level.setBlock(
                    placePos,
                    WayAroundContent.PRIORITE.get()
                            .defaultBlockState()
                            .setValue(PrioriteBlock.AMOUNT, 4),
                    3
            );

            consumeAndGiveEmptyBucket(
                    player,
                    stack,
                    context
            );

            level.playSound(
                    null,
                    placePos,
                    SoundEvents.BUCKET_EMPTY_LAVA,
                    SoundSource.BLOCKS,
                    1.0F,
                    0.75F
            );
        }

        return InteractionResult.SUCCESS;
    }

    private void consumeAndGiveEmptyBucket(
            Player player,
            ItemStack stack,
            UseOnContext context
    ) {

        if (player == null || player.getAbilities().instabuild) {
            return;
        }

        stack.shrink(1);

        ItemStack emptyBucket =
                new ItemStack(Items.BUCKET);

        if (stack.isEmpty()) {
            player.setItemInHand(
                    context.getHand(),
                    emptyBucket
            );
        } else if (!player.addItem(emptyBucket)) {
            player.drop(emptyBucket, false);
        }
    }
}
