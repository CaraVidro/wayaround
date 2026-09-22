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

public final class PrioriteBottleItem extends Item {

    public PrioriteBottleItem(Properties properties) {
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
         * Se clicou numa poça existente, soma 1 no amount.
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
                                    amount + 1
                            ),
                            3
                    );

                    consumeAndGiveGlassBottle(
                            player,
                            stack,
                            context
                    );

                    level.playSound(
                            null,
                            clickedPos,
                            SoundEvents.BOTTLE_EMPTY,
                            SoundSource.BLOCKS,
                            1.0F,
                            0.75F
                    );
                }

                return InteractionResult.SUCCESS;
            }
        }

        /*
         * Senão, tenta criar uma poça amount=1
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
                            .setValue(PrioriteBlock.AMOUNT, 1),
                    3
            );

            consumeAndGiveGlassBottle(
                    player,
                    stack,
                    context
            );

            level.playSound(
                    null,
                    placePos,
                    SoundEvents.BOTTLE_EMPTY,
                    SoundSource.BLOCKS,
                    1.0F,
                    0.75F
            );
        }

        return InteractionResult.SUCCESS;
    }

    private void consumeAndGiveGlassBottle(
            Player player,
            ItemStack stack,
            UseOnContext context
    ) {

        if (player == null || player.getAbilities().instabuild) {
            return;
        }

        stack.shrink(1);

        ItemStack emptyBottle =
                new ItemStack(Items.GLASS_BOTTLE);

        if (stack.isEmpty()) {
            player.setItemInHand(
                    context.getHand(),
                    emptyBottle
            );
        } else if (!player.addItem(emptyBottle)) {
            player.drop(emptyBottle, false);
        }
    }
}
