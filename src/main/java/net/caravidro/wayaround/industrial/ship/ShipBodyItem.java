package net.caravidro.wayaround.industrial.ship;

import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Places the first hull module. Further modules snap through ship interaction. */
public final class ShipBodyItem
        extends Item {

    public ShipBodyItem(
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

        if (!WorldFeatureRuntime.enabled(
                level,
                WorldFeature.SHIPS
        )) {
            return InteractionResultHolder.pass(
                    stack
            );
        }

        HitResult hit =
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

        BlockHitResult blockHit =
                (BlockHitResult) hit;

        Vec3 normal =
                Vec3.atLowerCornerOf(
                        blockHit.getDirection()
                                .getNormal()
                );

        Vec3 location =
                hit.getLocation()
                        .add(
                                normal.scale(
                                        0.56
                                )
                        );

        AssemblyShipEntity ship =
                new AssemblyShipEntity(
                        ExperimentalShipContent.ASSEMBLY_SHIP.get(),
                        level
                );

        ship.setPos(
                location.x,
                location.y + 0.20,
                location.z
        );

        /*
         * Quantize the initial frame. Snapped modules are local to this frame,
         * so construction remains deterministic instead of accumulating tiny
         * floating point angle errors.
         */
        float yaw =
                Math.round(
                        player.getYRot()
                                / 90.0F
                )
                        * 90.0F;

        ship.setYRot(
                yaw
        );

        ship.initializeFirstBody();

        if (level
                instanceof ServerLevel server) {
            EntityType.<AssemblyShipEntity>createDefaultStackConfig(
                    server,
                    stack,
                    player
            ).accept(
                    ship
            );
        }

        if (!level.isClientSide) {
            if (!level.addFreshEntity(
                    ship
            )) {
                return InteractionResultHolder.fail(
                        stack
                );
            }

            if (!player.getAbilities()
                    .instabuild) {
                stack.shrink(
                        1
                );
            }

            level.gameEvent(
                    player,
                    GameEvent.ENTITY_PLACE,
                    BlockPos.containing(
                            location
                    )
            );
        }

        player.awardStat(
                Stats.ITEM_USED.get(
                        this
                )
        );

        return InteractionResultHolder.sidedSuccess(
                stack,
                level.isClientSide
        );
    }
}
