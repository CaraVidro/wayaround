package net.caravidro.wayaround.ecology;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Ecology adapter for a drifting jellyfish. It uses the shared aquatic mob
 * foundation so it participates in WayAround population simulation.
 */
public final class JellyfishEntity extends AguaWorldFishEntity {

    public JellyfishEntity(
            EntityType<? extends JellyfishEntity> type,
            Level level
    ) {
        super(type, level);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 16;
    }
}
