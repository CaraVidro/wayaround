package net.caravidro.wayaround.ecology;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Tiny warm-ocean reef resident that prefers coral-rich water. */
public final class ClownfishEntity
        extends AguaWorldFishEntity {

    public ClownfishEntity(
            EntityType<? extends ClownfishEntity> type,
            Level level
    ) {
        super(type, level);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 10;
    }
}
