package net.caravidro.wayaround.industrial.assembly;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.industrial.IndustrialContent;
import net.caravidro.wayaround.industrial.material.IndustrialMetalGrade;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Assembly history is attached to ordinary vanilla items as well, so a generic
 * tooltip event is required: a mechanically sawn oak plank is still an oak
 * plank, but its workmanship/history does not disappear.
 */
@EventBusSubscriber(
        modid = WayAround.MODID,
        value = Dist.CLIENT
)
public final class AssemblyPartTooltipEvents {

    private AssemblyPartTooltipEvents() {
    }

    @SubscribeEvent
    public static void onTooltip(
            ItemTooltipEvent event
    ) {
        if (event.getItemStack().is(
                Items.IRON_INGOT
        )
                || event.getItemStack().is(
                IndustrialContent.REFINED_IRON_INGOT.get()
        )
                || event.getItemStack().is(
                IndustrialContent.PRECISION_IRON_INGOT.get()
        )) {

            IndustrialMetalGrade grade =
                    IndustrialMetalGrade.of(
                            event.getItemStack()
                    );

            event.getToolTip()
                    .add(
                            Component.translatable(
                                    "tooltip.wayaround.iron_grade",
                                    Component.translatable(
                                            switch (grade) {
                                                case LOW ->
                                                        "tooltip.wayaround.iron_grade.low";

                                                case MEDIUM ->
                                                        "tooltip.wayaround.iron_grade.medium";

                                                case HIGH ->
                                                        "tooltip.wayaround.iron_grade.high";
                                            }
                                    )
                            ).withStyle(
                                    grade == IndustrialMetalGrade.HIGH
                                            ? ChatFormatting.AQUA
                                            : grade == IndustrialMetalGrade.MEDIUM
                                            ? ChatFormatting.GREEN
                                            : ChatFormatting.GRAY
                            )
                    );
        }

        AssemblyPartProfile profile =
                AssemblyItemData.readPart(
                        event.getItemStack()
                );

        if (profile == null) {
            return;
        }

        event.getToolTip()
                .add(
                        Component.translatable(
                                "tooltip.wayaround.part.kind",
                                profile.kind()
                                        .name()
                        )
                                .withStyle(
                                        ChatFormatting.DARK_GRAY
                                )
                );

        event.getToolTip()
                .add(
                        Component.translatable(
                                "tooltip.wayaround.assembly.quality",
                                percent(
                                        profile.assemblyScore()
                                )
                        )
                                .withStyle(
                                        ChatFormatting.GRAY
                                )
                );

        event.getToolTip()
                .add(
                        Component.translatable(
                                "tooltip.wayaround.assembly.durability",
                                percent(
                                        profile.durabilityScore()
                                )
                        )
                                .withStyle(
                                        profile.durabilityScore()
                                                < 0.18F
                                                ? ChatFormatting.RED
                                                : ChatFormatting.GRAY
                                )
                );

        event.getToolTip()
                .add(
                        Component.translatable(
                                "tooltip.wayaround.assembly.fatigue",
                                percent(
                                        profile.fatigue()
                                )
                        )
                                .withStyle(
                                        ChatFormatting.DARK_GRAY
                                )
                );

        CompoundTag process =
                AssemblyItemData.readProcessStamp(
                        event.getItemStack()
                );

        if (!process.isEmpty()) {
            event.getToolTip()
                    .add(
                            Component.translatable(
                                    "tooltip.wayaround.part.process",
                                    process.getString(
                                            "Process"
                                    ),
                                    Math.round(
                                            process.getFloat(
                                                    "Quality"
                                            )
                                                    * 100.0F
                                    )
                            )
                                    .withStyle(
                                            ChatFormatting.DARK_AQUA
                                    )
                    );
        }
    }

    private static int percent(
            float value
    ) {
        return Math.round(
                value
                        * 100.0F
        );
    }
}
