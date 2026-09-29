package net.caravidro.wayaround.ecology;

import net.caravidro.wayaround.ecology.ai.LivingFaunaManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A physical whole sardine. Furnace/smoker/campfire recipes cook the fish item;
 * right-clicking air places the carcass back into the world.
 */
public final class WholeSardineItem extends Item {

    private final boolean cooked;
    private final boolean large;

    public WholeSardineItem(
            boolean cooked,
            boolean large,
            Properties properties
    ) {
        super(properties);
        this.cooked = cooked;
        this.large = large;
    }

    public boolean cooked() {
        return cooked;
    }

    public boolean large() {
        return large;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (!(level instanceof ServerLevel server)) {
            return InteractionResultHolder.success(stack);
        }

        SardineEntity sardine = EcologyContent.SARDINE.get().create(server);

        if (sardine == null) {
            return InteractionResultHolder.fail(stack);
        }

        Vec3 look = player.getLookAngle().normalize();
        Vec3 drop =
                player.getEyePosition()
                        .add(look.scale(1.15))
                        .add(0.0, -0.60, 0.0);

        sardine.moveTo(
                drop.x,
                drop.y,
                drop.z,
                player.getYRot(),
                0.0F
        );

        LivingFaunaManager.setFishSize(
                sardine,
                large ? 0.64F : 0.31F
        );

        sardine.restoreAsCarcass(cooked, large);
        server.addFreshEntity(sardine);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResultHolder.success(stack);
    }
}
