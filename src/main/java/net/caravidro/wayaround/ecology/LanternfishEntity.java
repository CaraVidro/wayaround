package net.caravidro.wayaround.ecology;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Small deep-water fish with strong bioluminescent organs. */
public final class LanternfishEntity
        extends AguaWorldFishEntity {

    public LanternfishEntity(
            EntityType<? extends LanternfishEntity> type,
            Level level
    ) {
        super(type, level);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 14;
    }
}
