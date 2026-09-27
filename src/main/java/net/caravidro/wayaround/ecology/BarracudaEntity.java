package net.caravidro.wayaround.ecology;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Fast mid-sized predator that shares the modular aquatic hunting system. */
public final class BarracudaEntity
        extends AguaWorldFishEntity
        implements AquaticPredator {

    public BarracudaEntity(
            EntityType<? extends BarracudaEntity> type,
            Level level
    ) {
        super(type, level);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 5;
    }

    @Override
    public double huntSpeed() {
        return 1.78;
    }

    @Override
    public float biteBaseDamage() {
        return 3.5F;
    }
}
