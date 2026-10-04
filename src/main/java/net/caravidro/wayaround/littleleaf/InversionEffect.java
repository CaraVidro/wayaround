package net.caravidro.wayaround.littleleaf;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;

/** An instantaneous, reversible size change, stored by vanilla attributes. */
public final class InversionEffect extends MobEffect {
    public static final ResourceLocation SIZE=ResourceLocation.fromNamespaceAndPath("wayaround","size_inversion");
    public InversionEffect(){super(MobEffectCategory.NEUTRAL,0xC4E09C);}
    @Override public boolean isInstantenous(){return true;}
    @Override public void applyInstantenousEffect(Entity source,Entity indirect,LivingEntity target,int amplifier,double strength){if(!target.level().isClientSide)invert(target);}
    @Override public boolean shouldApplyEffectTickThisTick(int ticks,int amplifier){return true;}
    @Override public boolean applyEffectTick(LivingEntity e,int amplifier){if(!e.level().isClientSide)invert(e);return false;}
    public static void invert(LivingEntity e){
        if(e.level().dimension().equals(ColonyTravel.DIMENSION))return;
        var scale=e.getAttribute(Attributes.SCALE);if(scale==null)return;
        if(scale.hasModifier(SIZE)){scale.removeModifier(SIZE);e.refreshDimensions();return;}
        boolean tiny=scale.getValue()<=.2;
        double desired=e instanceof ColonyInsectEntity insect?(scale.getValue()<=.5?ColonyRules.GIANT:ColonyRules.TINY)*(insect.caste()==2?2:insect.caste()==1?1.2:1):tiny?1:ColonyRules.PLAYER_TINY;
        scale.addPermanentModifier(new AttributeModifier(SIZE,desired/scale.getValue()-1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        e.refreshDimensions();
    }
    public static void miniature(LivingEntity e){var a=e.getAttribute(Attributes.SCALE);if(a==null)return;a.removeModifier(SIZE);a.addPermanentModifier(new AttributeModifier(SIZE,ColonyRules.PLAYER_TINY/a.getValue()-1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));e.refreshDimensions();}
}
