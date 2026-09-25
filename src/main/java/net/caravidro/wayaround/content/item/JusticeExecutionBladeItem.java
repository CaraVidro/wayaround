package net.caravidro.wayaround.content.item;

import java.util.List;

import net.caravidro.wayaround.justice.JusticeRewardManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public final class JusticeExecutionBladeItem
        extends SwordItem {

    public JusticeExecutionBladeItem(
            Tier tier,
            Properties properties
    ) {
        super(
                tier,
                properties
        );
    }

    @Override
    public boolean isFoil(
            ItemStack stack
    ) {
        return true;
    }

    @Override
    public boolean hurtEnemy(
            ItemStack stack,
            LivingEntity target,
            LivingEntity attacker
    ) {
        boolean base =
                super.hurtEnemy(
                        stack,
                        target,
                        attacker
                );

        if (attacker
                instanceof ServerPlayer owner
                && JusticeRewardManager.isAuthorized(
                owner.getUUID(),
                target.getUUID()
        )) {

            target.hurt(
                    owner.damageSources()
                            .playerAttack(
                                    owner
                            ),
                    14.0F
            );

            owner.serverLevel()
                    .playSound(
                            null,
                            target.blockPosition(),
                            SoundEvents.PLAYER_ATTACK_CRIT,
                            SoundSource.PLAYERS,
                            1.25F,
                            0.72F
                    );
        }

        return base;
    }

    @Override
    public void inventoryTick(
            ItemStack stack,
            Level level,
            Entity entity,
            int slotId,
            boolean isSelected
    ) {
        super.inventoryTick(
                stack,
                level,
                entity,
                slotId,
                isSelected
        );

        if (!level.isClientSide
                && entity
                instanceof ServerPlayer holder
                && !JusticeRewardManager.bladeValid(
                holder.getUUID(),
                stack
        )) {

            stack.setCount(
                    0
            );
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        CompoundTag tag =
                stack.getOrDefault(
                        DataComponents.CUSTOM_DATA,
                        CustomData.EMPTY
                ).copyTag();

        tooltip.add(
                Component.translatable(
                        "tooltip.wayaround.justice_blade"
                ).withStyle(
                        ChatFormatting.GOLD
                )
        );

        String target =
                tag.getString(
                        JusticeRewardManager.TARGET_NAME_KEY
                );

        if (!target.isBlank()) {
            tooltip.add(
                    Component.translatable(
                            "tooltip.wayaround.justice_blade.target",
                            target
                    ).withStyle(
                            ChatFormatting.GRAY
                    )
            );
        }
    }
}
