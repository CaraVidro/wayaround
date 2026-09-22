package net.caravidro.wayaround.industrial.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;

final class CoalShipDispenser extends DefaultDispenseItemBehavior {
    private final DefaultDispenseItemBehavior fallback = new DefaultDispenseItemBehavior();

    @Override
    protected ItemStack execute(BlockSource source, ItemStack stack) {
        Direction facing = source.state().getValue(DispenserBlock.FACING);
        BlockPos front = source.pos().relative(facing);
        double offset;
        if (source.level().getFluidState(front).is(FluidTags.WATER)) {
            offset = 1.0;
        } else if (source.level().getBlockState(front).isAir()
                && source.level().getFluidState(front.below()).is(FluidTags.WATER)) {
            offset = 0.0;
        } else {
            return fallback.dispense(source, stack);
        }
        CoalShipEntity ship = new CoalShipEntity(CoalShipContent.COAL_SHIP_ENTITY.get(), source.level());
        Vec3 center = source.center();
        double distance = 0.5625 + ship.getBbWidth() / 2.0;
        ship.setPos(center.x + facing.getStepX() * distance,
                center.y + facing.getStepY() * 1.125 + offset,
                center.z + facing.getStepZ() * distance);
        ship.setYRot(facing.toYRot());
        EntityType.<CoalShipEntity>createDefaultStackConfig(source.level(), stack, null).accept(ship);
        if (source.level().noCollision(ship, ship.getBoundingBox()) && source.level().addFreshEntity(ship)) {
            stack.shrink(1);
        }
        return stack;
    }
}
