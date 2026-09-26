package net.caravidro.wayaround.industrial.ship;

import java.util.List;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class CaravelItem extends Item {
    public CaravelItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!WorldFeatureRuntime.enabled(level, WorldFeature.SHIPS)) {
            return InteractionResultHolder.pass(stack);
        }
        HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }
        Vec3 look = player.getViewVector(1.0F);
        for (Entity entity : level.getEntities(player,
                player.getBoundingBox().expandTowards(look.scale(player.blockInteractionRange())).inflate(1.0),
                EntitySelector.NO_SPECTATORS.and(Entity::isPickable))) {
            if (entity.getBoundingBox().inflate(entity.getPickRadius()).contains(player.getEyePosition())) {
                return InteractionResultHolder.pass(stack);
            }
        }
        CaravelEntity ship = new CaravelEntity(CoalShipContent.CARAVEL_ENTITY.get(), level);
        ship.setPos(hit.getLocation());
        ship.setYRot(player.getYRot());
        if (level instanceof ServerLevel serverLevel) {
            EntityType.<CaravelEntity>createDefaultStackConfig(serverLevel, stack, player).accept(ship);
        }
        if (!level.noCollision(ship, ship.getBoundingBox())) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            if (!level.addFreshEntity(ship)) {
                return InteractionResultHolder.fail(stack);
            }
            level.gameEvent(player, GameEvent.ENTITY_PLACE, hit.getLocation());
            stack.consume(1, player);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.wayaround.caravel.hull").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.wayaround.caravel.controls").withStyle(ChatFormatting.GRAY));
    }
}
