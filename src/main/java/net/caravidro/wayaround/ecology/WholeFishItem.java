package net.caravidro.wayaround.ecology;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Generic whole-fish item. Species behavior lives in FishProcessingProfile, so
 * future processable fish do not need a bespoke carry item class.
 */
public final class WholeFishItem
        extends Item {

    private final FishProcessingProfile profile;
    private final boolean cooked;
    private final boolean large;

    public WholeFishItem(
            FishProcessingProfile profile,
            boolean cooked,
            boolean large,
            Properties properties
    ) {
        super(properties);
        this.profile = profile;
        this.cooked = cooked;
        this.large = large;
    }

    public FishProcessingProfile profile() {
        return profile;
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
        ItemStack stack =
                player.getItemInHand(
                        hand
                );

        if (!(level
                instanceof ServerLevel server)) {
            return InteractionResultHolder.success(
                    stack
            );
        }

        FishCarcassEntity carcass =
                EcologyContent.FISH_CARCASS.get()
                        .create(
                                server
                        );

        if (carcass == null) {
            return InteractionResultHolder.fail(
                    stack
            );
        }

        Vec3 look =
                player.getLookAngle()
                        .normalize();

        Vec3 drop =
                player.getEyePosition()
                        .add(
                                look.scale(
                                        1.15
                                )
                        )
                        .add(
                                0.0,
                                -0.60,
                                0.0
                        );

        carcass.moveTo(
                drop.x,
                drop.y,
                drop.z,
                player.getYRot(),
                0.0F
        );

        carcass.initialize(
                profile,
                profile.carryScale(
                        large
                ),
                cooked,
                large
        );

        server.addFreshEntity(
                carcass
        );

        if (!player.getAbilities()
                .instabuild) {
            stack.shrink(
                    1
            );
        }

        return InteractionResultHolder.success(
                stack
        );
    }
}
