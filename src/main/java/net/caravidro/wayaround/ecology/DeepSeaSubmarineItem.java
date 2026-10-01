package net.caravidro.wayaround.ecology;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Places the powered exploration submarine into water. */
public final class DeepSeaSubmarineItem extends Item {

    public DeepSeaSubmarineItem(
            Properties properties
    ) {
        super(
                properties
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack =
                player.getItemInHand(
                        hand
                );

        BlockHitResult hit =
                getPlayerPOVHitResult(
                        level,
                        player,
                        ClipContext.Fluid.ANY
                );

        if (hit.getType()
                != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(
                    stack
            );
        }

        BlockPos pos =
                hit.getBlockPos();

        if (!level.getFluidState(
                pos
        ).is(
                FluidTags.WATER
        )) {
            if (level.getFluidState(
                    pos.above()
            ).is(
                    FluidTags.WATER
            )) {
                pos =
                        pos.above();

            } else {
                return InteractionResultHolder.pass(
                        stack
                );
            }
        }

        if (!level.isClientSide) {
            DeepSeaSubmarineEntity submarine =
                    EcologyContent.DEEP_SEA_SUBMARINE.get()
                            .create(
                                    level
                            );

            if (submarine == null) {
                return InteractionResultHolder.fail(
                        stack
                );
            }

            submarine.setPos(
                    pos.getX()
                            + 0.5,
                    pos.getY()
                            + 0.12,
                    pos.getZ()
                            + 0.5
            );

            submarine.setYRot(
                    player.getYRot()
            );

            if (!level.noCollision(
                    submarine
            )) {
                return InteractionResultHolder.fail(
                        stack
                );
            }

            level.addFreshEntity(
                    submarine
            );

            player.startRiding(
                    submarine
            );

            if (!player.getAbilities()
                    .instabuild) {
                stack.shrink(
                        1
                );
            }
        }

        return InteractionResultHolder.sidedSuccess(
                stack,
                level.isClientSide
        );
    }
}
