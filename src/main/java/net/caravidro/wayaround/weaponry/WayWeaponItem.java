package net.caravidro.wayaround.weaponry;

import java.util.List;

import net.caravidro.wayaround.WayAround;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public final class WayWeaponItem
        extends SwordItem {

    private final WeaponFamily family;
    private final String materialId;

    public WayWeaponItem(
            Tier tier,
            WeaponFamily family,
            String materialId,
            Properties properties
    ) {
        super(
                tier,
                properties
        );

        this.family =
                family;

        this.materialId =
                materialId;
    }

    public WeaponFamily family() {
        return family;
    }

    public String materialId() {
        return materialId;
    }

    public static ItemAttributeModifiers attributes(
            Tier tier,
            WeaponFamily family
    ) {
        ItemAttributeModifiers.Builder builder =
                ItemAttributeModifiers.builder()
                        .add(
                                Attributes.ATTACK_DAMAGE,
                                new AttributeModifier(
                                        Item.BASE_ATTACK_DAMAGE_ID,
                                        family.attackDamage()
                                                + tier.getAttackDamageBonus(),
                                        AttributeModifier.Operation.ADD_VALUE
                                ),
                                EquipmentSlotGroup.MAINHAND
                        )
                        .add(
                                Attributes.ATTACK_SPEED,
                                new AttributeModifier(
                                        Item.BASE_ATTACK_SPEED_ID,
                                        family.attackSpeed(),
                                        AttributeModifier.Operation.ADD_VALUE
                                ),
                                EquipmentSlotGroup.MAINHAND
                        );

        if (Math.abs(
                family.reach()
        ) > 1.0E-6) {
            builder.add(
                    Attributes.ENTITY_INTERACTION_RANGE,
                    new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath(
                                    WayAround.MODID,
                                    family.id()
                                            + "_reach"
                            ),
                            family.reach(),
                            AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
            );
        }

        return builder.build();
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        switch (family) {
            case DAGGER -> {
                tooltip.add(
                        Component.translatable(
                                "tooltip.wayaround.weaponry.dagger"
                        ).withStyle(
                                ChatFormatting.GRAY
                        )
                );

                tooltip.add(
                        Component.translatable(
                                "tooltip.wayaround.weaponry.dagger_dual"
                        ).withStyle(
                                ChatFormatting.DARK_GRAY
                        )
                );
            }

            case KATANA ->
                    tooltip.add(
                            Component.translatable(
                                    "tooltip.wayaround.weaponry.katana"
                            ).withStyle(
                                    ChatFormatting.GRAY
                            )
                    );

            case SCYTHE ->
                    tooltip.add(
                            Component.translatable(
                                    "tooltip.wayaround.weaponry.scythe"
                            ).withStyle(
                                    ChatFormatting.GRAY
                            )
                    );
        }
    }
}
