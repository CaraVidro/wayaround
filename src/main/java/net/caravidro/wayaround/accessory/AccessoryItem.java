package net.caravidro.wayaround.accessory;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class AccessoryItem
        extends Item {

    private final AccessoryKind kind;

    public AccessoryItem(
            AccessoryKind kind,
            Properties properties
    ) {
        super(properties);
        this.kind = kind;
    }

    public AccessoryKind kind() {
        return kind;
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

        if (player
                instanceof ServerPlayer serverPlayer) {

            /*
             * Shift + use with the engineer trousers in one hand and an item
             * in the other loads exactly one item into the physical pocket.
             * Normal use keeps its old behaviour and equips the trousers.
             */
            if (kind == AccessoryKind.ENGINEER_TROUSERS
                    && player.isShiftKeyDown()) {
                InteractionHand sourceHand =
                        hand == InteractionHand.MAIN_HAND
                                ? InteractionHand.OFF_HAND
                                : InteractionHand.MAIN_HAND;

                ItemStack source =
                        player.getItemInHand(
                                sourceHand
                        );

                ItemStack pocket =
                        TrouserPocketData.read(
                                stack,
                                serverPlayer.registryAccess()
                        );

                if (!source.isEmpty()) {
                    if (pocket.isEmpty()) {
                        ItemStack single =
                                source.copy();

                        single.setCount(
                                1
                        );

                        TrouserPocketData.write(
                                stack,
                                single,
                                serverPlayer.registryAccess()
                        );

                        if (!serverPlayer.getAbilities()
                                .instabuild) {
                            source.shrink(
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

            AccessoryManager.equipFromHand(
                    serverPlayer,
                    hand,
                    this
            );
        }

        return InteractionResultHolder.sidedSuccess(
                stack,
                level.isClientSide
        );
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        int stage =
                AccessoryWear.stage(
                        stack,
                        kind
                );

        tooltip.add(
                Component.translatable(
                        kind.kit()
                                .translationKey()
                ).withStyle(
                        ChatFormatting.DARK_AQUA
                )
        );

        tooltip.add(
                Component.translatable(
                        switch (stage) {
                            case 1 ->
                                    "accessory.wear.worn";
                            case 2 ->
                                    "accessory.wear.ruined";
                            default ->
                                    "accessory.wear.intact";
                        }
                ).withStyle(
                        stage == 0
                                ? ChatFormatting.GRAY
                                : stage == 1
                                ? ChatFormatting.YELLOW
                                : ChatFormatting.RED
                )
        );

        if (kind.windLoose()) {
            tooltip.add(
                    Component.translatable(
                            "accessory.wind_loose"
                    ).withStyle(
                            ChatFormatting.GRAY
                    )
            );
        }

        String optics =
                switch (kind) {
                    case RAILWAY_GOGGLES -> "accessory.optics.railway";
                    case MINER_GOGGLES -> "accessory.optics.miner";
                    case STORM_VISOR -> "accessory.optics.storm";
                    case ARCTIC_GOGGLES -> "accessory.optics.arctic";
                    default -> null;
                };

        if (optics != null) {
            tooltip.add(
                    Component.translatable(
                            optics
                    ).withStyle(
                            ChatFormatting.DARK_AQUA
                    )
            );
        }

        if (kind.breakableGlass()) {
            int glass =
                    AccessoryWear.glassState(
                            stack
                    );

            tooltip.add(
                    Component.translatable(
                            switch (glass) {
                                case 1 ->
                                        "accessory.glass.cracked";
                                case 2 ->
                                        "accessory.glass.broken";
                                default ->
                                        "accessory.glass.intact";
                            }
                    ).withStyle(
                            glass == 0
                                    ? ChatFormatting.AQUA
                                    : glass == 1
                                    ? ChatFormatting.GOLD
                                    : ChatFormatting.DARK_RED
                    )
            );
        }

        if (kind.motion()
                != AccessoryMotion.NONE) {
            tooltip.add(
                    Component.translatable(
                            "accessory.secondary_motion"
                    ).withStyle(
                            ChatFormatting.DARK_GRAY
                    )
            );
        }

        if (kind.ratHost()) {
            tooltip.add(
                    Component.translatable(
                            "accessory.rat_ready"
                    ).withStyle(
                            ChatFormatting.DARK_GRAY
                    )
            );
        }

        if (AccessoryCustomizationData.supported(
                kind
        )) {
            AccessoryCustomizationData.Config custom =
                    AccessoryCustomizationData.read(
                            stack,
                            kind
                    );

            if (AccessoryCustomizationData.colorOnly(
                    kind
            )) {
                tooltip.add(
                        Component.translatable(
                                "accessory.workshop.tooltip.color",
                                Component.translatable(
                                        AccessoryCustomizationData.woolNameKey(
                                                custom.woolColor()
                                        )
                                )
                        ).withStyle(
                                ChatFormatting.GOLD
                        )
                );
            } else {
                tooltip.add(
                        Component.translatable(
                                "accessory.workshop.tooltip.material",
                                Component.translatable(
                                        AccessoryCustomizationData.materialNameKey(
                                                custom.material()
                                        )
                                )
                        ).withStyle(
                                ChatFormatting.GOLD
                        )
                );

                tooltip.add(
                        Component.translatable(
                                kind == AccessoryKind.ENGINEER_CAP
                                        ? "accessory.workshop.tooltip.height"
                                        : kind == AccessoryKind.SOMBRERO
                                        ? "accessory.workshop.tooltip.brim"
                                        : "accessory.workshop.tooltip.size",
                                custom.size() + 1
                        ).withStyle(
                                ChatFormatting.GRAY
                        )
                );
            }

            if (kind == AccessoryKind.ENGINEER_CAP) {
                tooltip.add(
                        Component.translatable(
                                "accessory.workshop.tooltip.extras",
                                custom.gears()
                                        ? Component.translatable(
                                        "accessory.workshop.extra.gears"
                                )
                                        : Component.translatable(
                                        "accessory.workshop.extra.none"
                                ),
                                custom.clock()
                                        ? Component.translatable(
                                        "accessory.workshop.extra.clock"
                                )
                                        : Component.translatable(
                                        "accessory.workshop.extra.none"
                                )
                        ).withStyle(
                                ChatFormatting.DARK_GRAY
                        )
                );
            }

            if (kind == AccessoryKind.SOMBRERO) {
                tooltip.add(
                        Component.translatable(
                                "accessory.workshop.tooltip.wool",
                                Component.translatable(
                                        AccessoryCustomizationData.woolNameKey(
                                                custom.woolColor()
                                        )
                                )
                        ).withStyle(
                                ChatFormatting.DARK_GRAY
                        )
                );
            }
        }

        if (kind == AccessoryKind.WATCHING_EYE) {
            tooltip.add(
                    Component.translatable(
                            "tooltip.wayaround.watching_eye"
                    ).withStyle(
                            ChatFormatting.DARK_PURPLE
                    )
            );
        }

        if (kind == AccessoryKind.CARDBOARD_BOX) {
            tooltip.add(
                    Component.translatable(
                            "tooltip.wayaround.cardboard_box"
                    ).withStyle(
                            ChatFormatting.GRAY
                    )
            );
        }

        if (kind == AccessoryKind.GAS_MASK) {
            tooltip.add(
                    Component.translatable(
                            "tooltip.wayaround.gas_mask"
                    ).withStyle(
                            ChatFormatting.DARK_GRAY
                    )
            );
        }

        if (kind == AccessoryKind.ENGINEER_TROUSERS
                && context.registries() != null) {
            ItemStack pocket =
                    TrouserPocketData.read(
                            stack,
                            context.registries()
                    );

            tooltip.add(
                    Component.translatable(
                            pocket.isEmpty()
                                    ? "accessory.trouser_pocket.empty"
                                    : "accessory.trouser_pocket.item",
                            pocket.isEmpty()
                                    ? ""
                                    : pocket.getHoverName()
                    ).withStyle(
                            pocket.isEmpty()
                                    ? ChatFormatting.DARK_GRAY
                                    : ChatFormatting.GOLD
                    )
            );

            tooltip.add(
                    Component.translatable(
                            "accessory.trouser_pocket.load_hint"
                    ).withStyle(
                            ChatFormatting.DARK_GRAY
                    )
            );
        }
    }
}
