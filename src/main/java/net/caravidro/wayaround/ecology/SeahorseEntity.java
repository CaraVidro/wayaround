package net.caravidro.wayaround.ecology;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Tiny reef fish; intentionally stays small even after long survival. */
public final class SeahorseEntity extends AguaWorldFishEntity {

    public SeahorseEntity(
            EntityType<? extends SeahorseEntity> type,
            Level level
    ) {
        super(type, level);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 12;
    }
}
