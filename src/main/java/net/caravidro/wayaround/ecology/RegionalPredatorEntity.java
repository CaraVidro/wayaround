package net.caravidro.wayaround.ecology;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** The shared predator engine handles bites, feeding and prey reactions. */
public final class RegionalPredatorEntity extends RegionalFishEntity implements AquaticPredator {
    public RegionalPredatorEntity(EntityType<? extends RegionalFishEntity> type, Level level, RegionalFishSpecies species) {
        super(type, level, species);
    }
    @Override public double huntRadius() { return species() == RegionalFishSpecies.ANGLERFISH ? 5 : 12; }
    @Override public double huntSpeed() { return species() == RegionalFishSpecies.ANGLERFISH ? .85 : 1.3; }
    @Override public float biteBaseDamage() { return species() == RegionalFishSpecies.ANGLERFISH ? 2.5F : 4; }
}
