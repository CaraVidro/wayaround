package net.caravidro.wayaround.ecology;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Cleinton.
 *
 * He has no natural spawn table on purpose. Warfare Outpost currently contains
 * exactly one thing and it is this fish.
 */
public final class CleintonEntity extends AguaWorldFishEntity {

    public CleintonEntity(
            EntityType<? extends CleintonEntity> type,
            Level level
    ) {
        super(type, level);
    }
}
