package net.caravidro.wayaround.ecology;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Large peaceful glider for warm and lukewarm oceans. */
public final class MantaRayEntity extends AguaWorldFishEntity {

    public MantaRayEntity(
            EntityType<? extends MantaRayEntity> type,
            Level level
    ) {
        super(type, level);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 4;
    }
}
