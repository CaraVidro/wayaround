package net.caravidro.wayaround.ecology;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Rare ribbon-like deep-ocean giant. */
public final class OarfishEntity extends AguaWorldFishEntity {

    public OarfishEntity(
            EntityType<? extends OarfishEntity> type,
            Level level
    ) {
        super(type, level);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 2;
    }
}
