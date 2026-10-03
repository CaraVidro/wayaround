package net.caravidro.wayaround.nature;

import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("wayaround_nature")
@PrefixGameTestTemplate(false)
public final class SurvivalRecipeGameTests {
    @GameTest(template="assembly_test",batch="recipes",timeoutTicks=80)
    public static void vanillaTableAndNewRecipesAreUsable(GameTestHelper h) {
        var level=h.getLevel();var manager=level.getRecipeManager();
        var table=manager.getRecipeFor(RecipeType.CRAFTING,CraftingInput.of(2,2,List.of(
                new ItemStack(Items.OAK_PLANKS),new ItemStack(Items.OAK_PLANKS),new ItemStack(Items.OAK_PLANKS),new ItemStack(Items.OAK_PLANKS))),level);
        h.assertTrue(table.isPresent() && table.get().value().getResultItem(level.registryAccess()).is(Items.CRAFTING_TABLE),"Vanilla 2x2 crafting remains usable");
        var tag=TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("wayaround","new_craftable_survival"));
        var targets=BuiltInRegistries.ITEM.getTag(tag).orElseThrow();
        int checked=0;
        for(var item:targets) {
            Item target=item.value();var matches=manager.getAllRecipesFor(RecipeType.CRAFTING).stream().filter(r->r.value().getResultItem(level.registryAccess()).is(target)).toList();
            h.assertTrue(!matches.isEmpty(),"Missing craft recipe for "+item.unwrapKey());
            for(var holder:matches) {
                h.assertTrue(holder.value() instanceof ShapedRecipe,"Expected distinct shaped recipe");
                var recipe=(ShapedRecipe)holder.value();var grid=new ArrayList<ItemStack>();
                for(var ingredient:recipe.getIngredients()) {
                    var options=ingredient.getItems();grid.add(options.length==0?ItemStack.EMPTY:options[0].copy());
                }
                var input=CraftingInput.of(recipe.getWidth(),recipe.getHeight(),grid);
                var compatible=manager.getRecipesFor(RecipeType.CRAFTING,input,level);
                h.assertTrue(compatible.size()==1 && compatible.getFirst().value().getResultItem(level.registryAccess()).is(target),"Ambiguous or unusable recipe for "+item.unwrapKey()+": "+compatible.stream().map(r->r.id().toString()).toList());
                checked++;
            }
        }
        h.assertTrue(checked==56,"Every new survival recipe was validated");
        var outputs=new HashSet<Item>();for(var recipe:manager.getRecipes())outputs.add(recipe.value().getResultItem(level.registryAccess()).getItem());
        for(var item:BuiltInRegistries.ITEM) {
            var key=BuiltInRegistries.ITEM.getKey(item);
            if(key.getNamespace().equals("wayaround") && !outputs.contains(item) && !(item instanceof SpawnEggItem))
                System.out.println("RECIPE_ROUTE_AUDIT "+key+" "+item.getClass().getSimpleName());
        }
        h.succeed();
    }
}
