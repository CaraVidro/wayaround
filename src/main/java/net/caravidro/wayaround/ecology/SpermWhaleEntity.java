package net.caravidro.wayaround.ecology;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Giant sperm-whale profile. It intentionally reuses WhaleEntity ecology so
 * feeding, surface breathing, carcasses and scavengers all stay modular.
 */
public final class SpermWhaleEntity
        extends WhaleEntity {

    public SpermWhaleEntity(
            EntityType<? extends SpermWhaleEntity> type,
            Level level
    ) {
        super(type, level);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 1;
    }
}
