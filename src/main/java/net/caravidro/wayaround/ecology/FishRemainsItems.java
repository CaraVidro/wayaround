package net.caravidro.wayaround.ecology;
import java.util.*;
import net.minecraft.world.item.*;
import net.minecraft.world.food.FoodProperties;
import net.neoforged.neoforge.registries.DeferredItem;
public final class FishRemainsItems {
    public static final EnumMap<FishProcessingProfile,DeferredItem<Item>> RAW=new EnumMap<>(FishProcessingProfile.class),COOKED=new EnumMap<>(FishProcessingProfile.class);
    public static final EnumMap<FishProcessingProfile,DeferredItem<WholeFishItem>> WHOLE=new EnumMap<>(FishProcessingProfile.class);
    static {
        for(var p:FishProcessingProfile.values())if(p.networkId()>=2){String id=p.name().toLowerCase(Locale.ROOT);
            if(!existing(p))RAW.put(p,EcologyContent.ITEMS.register("raw_"+id+"_meat",()->new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(.15F).build()))));
            if(regional(p)==null)COOKED.put(p,EcologyContent.ITEMS.register("cooked_"+id+"_meat",()->new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(5).saturationModifier(.6F).build()))));
            else COOKED.put(p,RegionalFishSpecies.COOKED.get(regional(p)));
            WHOLE.put(p,EcologyContent.ITEMS.register("whole_"+id,()->new WholeFishItem(p,false,false,new Item.Properties().stacksTo(1))));
        }
    }
    private static RegionalFishSpecies regional(FishProcessingProfile p){try{return RegionalFishSpecies.valueOf(p.name());}catch(IllegalArgumentException e){return null;}}
    private static boolean existing(FishProcessingProfile p){return regional(p)!=null||p==FishProcessingProfile.COD||p==FishProcessingProfile.TROPICAL||p==FishProcessingProfile.PUFFER||p==FishProcessingProfile.SUNFISH||p==FishProcessingProfile.SHARK;}
    public static Item raw(FishProcessingProfile p){var regional=regional(p);if(regional!=null)return RegionalFishSpecies.MEAT.get(regional).get();return switch(p){case COD->EcologyContent.RAW_COD_MEAT.get();case TROPICAL->EcologyContent.RAW_TROPICAL_FISH_MEAT.get();case PUFFER->EcologyContent.RAW_PUFFERFISH_MEAT.get();case SUNFISH->EcologyContent.RAW_SUNFISH_MEAT.get();case SHARK->EcologyContent.RAW_SHARK_MEAT.get();default->RAW.get(p).get();};}
    public static void bootstrap(){}
}
