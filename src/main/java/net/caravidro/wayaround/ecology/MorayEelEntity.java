package net.caravidro.wayaround.ecology;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Reef predator: slower pursuit than a barracuda, but a hard close bite. */
public final class MorayEelEntity
        extends AguaWorldFishEntity
        implements AquaticPredator {

    public MorayEelEntity(
            EntityType<? extends MorayEelEntity> type,
            Level level
    ) {
        super(type, level);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 3;
    }

    @Override
    public double huntRadius() {
        return 12.0;
    }

    @Override
    public double huntVerticalRadius() {
        return 5.0;
    }

    @Override
    public double huntSpeed() {
        return 1.32;
    }

    @Override
    public double biteReach() {
        return 1.28;
    }

    @Override
    public float biteBaseDamage() {
        return 5.0F;
    }

    @Override
    public float biteScaleDamage() {
        return 1.2F;
    }
}
