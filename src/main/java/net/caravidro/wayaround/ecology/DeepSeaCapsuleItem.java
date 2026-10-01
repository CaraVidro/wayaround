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

/** Places the small one-person abyss capsule directly into water. */
public final class DeepSeaCapsuleItem extends Item {
    public DeepSeaCapsuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);

        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }

        BlockPos pos = hit.getBlockPos();
        if (!level.getFluidState(pos).is(FluidTags.WATER)) {
            if (level.getFluidState(pos.above()).is(FluidTags.WATER)) {
                pos = pos.above();
            } else {
                return InteractionResultHolder.pass(stack);
            }
        }

        if (!level.isClientSide) {
            DeepSeaCapsuleEntity capsule = EcologyContent.DEEP_SEA_CAPSULE.get().create(level);
            if (capsule == null) {
                return InteractionResultHolder.fail(stack);
            }

            capsule.setPos(pos.getX() + 0.5, pos.getY() + 0.15, pos.getZ() + 0.5);
            capsule.setYRot(player.getYRot());

            if (!level.noCollision(capsule)) {
                return InteractionResultHolder.fail(stack);
            }

            level.addFreshEntity(capsule);
            player.startRiding(capsule);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
