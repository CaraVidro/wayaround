package net.caravidro.wayaround.ecology;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Surface fish capable of short gliding bursts above the water. */
public final class FlyingFishEntity
        extends AguaWorldFishEntity {

    public FlyingFishEntity(
            EntityType<? extends FlyingFishEntity> type,
            Level level
    ) {
        super(type, level);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 9;
    }
}
