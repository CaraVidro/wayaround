package net.caravidro.wayaround.ecology;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Huge filter-feeding ocean animal.
 *
 * It stays inside the common Agua World fish ecology so currents, growth and
 * population systems can see it, but putting a whale into a bucket is firmly
 * rejected.
 */
public class WhaleEntity
        extends AguaWorldFishEntity {

    public WhaleEntity(
            EntityType<? extends WhaleEntity> type,
            Level level
    ) {
        super(type, level);
    }

    @Override
    protected InteractionResult mobInteract(
            Player player,
            InteractionHand hand
    ) {
        return InteractionResult.PASS;
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 2;
    }
}
