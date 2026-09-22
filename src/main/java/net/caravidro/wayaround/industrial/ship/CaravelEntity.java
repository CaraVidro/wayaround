package net.caravidro.wayaround.industrial.ship;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A broad, shallow hull: mast height must not affect vanilla boat buoyancy. */
public final class CaravelEntity extends SailingShipEntity {
    public static final double MAX_SPEED = 0.18;

    public CaravelEntity(EntityType<? extends Boat> type, Level level) {
        super(type, level);
        setVariant(Boat.Type.SPRUCE);
    }

    @Override public Item getDropItem() { return CoalShipContent.CARAVEL_ITEM.get(); }

    @Override public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, amount * 0.25F);
    }

    @Override public void setDeltaMovement(Vec3 velocity) {
        double speed = velocity.horizontalDistance();
        if (speed > MAX_SPEED) {
            double scale = MAX_SPEED / speed;
            velocity = new Vec3(velocity.x * scale, velocity.y, velocity.z * scale);
        }
        super.setDeltaMovement(velocity);
    }

    @Override protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        // Helm is behind the main mast, above the raised stern deck.
        return new Vec3(0.0, 0.95, -1.35).yRot(-getYRot() * (float) Math.PI / 180.0F);
    }
}
