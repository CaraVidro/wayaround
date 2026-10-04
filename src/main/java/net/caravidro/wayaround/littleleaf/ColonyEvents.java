package net.caravidro.wayaround.littleleaf;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid=WayAround.MODID)
public final class ColonyEvents {
    @SubscribeEvent public static void brewing(RegisterBrewingRecipesEvent e){e.getBuilder().addMix(Potions.AWKWARD,LittleLeafContent.FUNGUS_ITEM.get(),LittleLeafContent.INVERSION_POTION);}
    @SubscribeEvent public static void splash(ProjectileImpactEvent e){
        if(!(e.getProjectile() instanceof ThrownPotion potion)||!(potion.level() instanceof ServerLevel l)||!potion.getItem().is(Items.SPLASH_POTION))return;
        var contents=potion.getItem().getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY);
        if(java.util.stream.StreamSupport.stream(contents.getAllEffects().spliterator(),false).noneMatch(m->m.getEffect().is(LittleLeafContent.INVERSION.getKey())))return;
        var center=BlockPos.containing(e.getRayTraceResult().getLocation());
        // One impact, at most 147 already-loaded block probes; lingering clouds do not repeatedly toggle a nest.
        for(var p:BlockPos.betweenClosed(center.offset(-3,-1,-3),center.offset(3,1,3)))if(ColonyCoreBlockEntity.loaded(l,p)&&l.getBlockEntity(p) instanceof ColonyCoreBlockEntity core)core.invertColony();
    }
    @SubscribeEvent public static void eggOnCore(PlayerInteractEvent.RightClickBlock e){
        if(!(e.getLevel() instanceof ServerLevel l)||!(l.getBlockEntity(e.getPos()) instanceof ColonyCoreBlockEntity core))return;
        var item=e.getItemStack().getItem();int species=item==LittleLeafContent.RED_EGG.get()?1:item==LittleLeafContent.HONEY_EGG.get()?2:item==LittleLeafContent.TERMITE_EGG.get()?3:item==LittleLeafContent.BLACK_EGG.get()?0:-1;
        if(species<0)return;l.setBlock(e.getPos(),core.getBlockState().setValue(ColonyCoreBlock.SPECIES,species),3);if(!e.getEntity().isCreative())e.getItemStack().shrink(1);e.setCanceled(true);e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
    }
}
