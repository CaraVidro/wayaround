package net.caravidro.wayaround.industrial.crushing;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Conservative yields: crushing opens processing, it is not free ore multiplication. */
public record CrushingRecipe(Item output, int count, float hardness) {
    public static CrushingRecipe find(ItemStack input) {
        var id = BuiltInRegistries.ITEM.getKey(input.getItem());
        if (!id.getNamespace().equals("minecraft")) return null;
        return switch (id.getPath()) {
            case "raw_iron", "iron_ore" -> new CrushingRecipe(CrusherContent.CRUSHED_IRON.get(), 1, 1.4F);
            case "deepslate_iron_ore" -> new CrushingRecipe(CrusherContent.CRUSHED_IRON.get(), 1, 2.3F);
            case "raw_iron_block" -> new CrushingRecipe(CrusherContent.CRUSHED_IRON.get(), 9, 3.2F);
            case "raw_copper", "copper_ore" -> new CrushingRecipe(CrusherContent.CRUSHED_COPPER.get(), 1, 1.25F);
            case "deepslate_copper_ore" -> new CrushingRecipe(CrusherContent.CRUSHED_COPPER.get(), 1, 2.15F);
            case "raw_copper_block" -> new CrushingRecipe(CrusherContent.CRUSHED_COPPER.get(), 9, 3.0F);
            case "raw_gold", "gold_ore" -> new CrushingRecipe(CrusherContent.CRUSHED_GOLD.get(), 1, 1.3F);
            case "deepslate_gold_ore" -> new CrushingRecipe(CrusherContent.CRUSHED_GOLD.get(), 1, 2.2F);
            case "raw_gold_block" -> new CrushingRecipe(CrusherContent.CRUSHED_GOLD.get(), 9, 3.1F);
            case "coal_ore" -> new CrushingRecipe(Items.COAL, 1, 1.15F);
            case "deepslate_coal_ore" -> new CrushingRecipe(Items.COAL, 1, 2.0F);
            case "stone", "cobblestone" -> new CrushingRecipe(Items.GRAVEL, 1, 1.25F);
            case "deepslate", "cobbled_deepslate" -> new CrushingRecipe(Items.GRAVEL, 1, 2.3F);
            case "gravel" -> new CrushingRecipe(Items.SAND, 1, .6F);
            default -> null;
        };
    }
}
