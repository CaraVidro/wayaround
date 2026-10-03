package net.caravidro.wayaround.nature;

import java.util.EnumSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** A stolen bite is real held equipment, saved by Mob and recoverable on death. */
public final class BirdFoodTheftGoal extends Goal {
    private final PathfinderMob bird;
    private Player target;
    private int until, cooldown;
    private Vec3 escape;
    public BirdFoodTheftGoal(PathfinderMob bird) {
        this.bird = bird;
        setFlags(EnumSet.of(Flag.MOVE));
    }
    public static boolean offersFood(Player player) {
        return !player.isCreative() && !player.isSpectator()
                && (player.getMainHandItem().has(DataComponents.FOOD) || player.getOffhandItem().has(DataComponents.FOOD));
    }
    public static boolean stealOne(PathfinderMob bird, Player player) {
        if (!bird.getMainHandItem().isEmpty() || !offersFood(player)) return false;
        ItemStack held = player.getMainHandItem().has(DataComponents.FOOD) ? player.getMainHandItem() : player.getOffhandItem();
        bird.setItemSlot(EquipmentSlot.MAINHAND, held.split(1));
        bird.setDropChance(EquipmentSlot.MAINHAND, 1f);
        bird.setPersistenceRequired();
        player.getInventory().setChanged();
        return true;
    }
    @Override public boolean canUse() {
        if (bird.tickCount < cooldown || bird.tickCount % 20 != 0 || !bird.getMainHandItem().isEmpty()) return false;
        target = bird.level().getEntitiesOfClass(Player.class, bird.getBoundingBox().inflate(12), BirdFoodTheftGoal::offersFood)
                .stream().limit(8).min(java.util.Comparator.comparingDouble(bird::distanceToSqr)).orElse(null);
        return target != null;
    }
    @Override public void start() { until = bird.tickCount + 160; escape = null; }
    @Override public boolean canContinueToUse() { return bird.tickCount < until && (escape != null || target != null && target.isAlive() && offersFood(target)); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void tick() {
        if (escape == null && target != null) {
            if (bird.distanceToSqr(target) < 2.25 && bird.hasLineOfSight(target) && stealOne(bird, target)) {
                Vec3 away = bird.position().subtract(target.position()).multiply(1, 0, 1).normalize();
                if (away.lengthSqr() < .1) away = new Vec3(1, 0, 0);
                escape = bird.position().add(away.scale(12)).add(0, 5, 0);
                until = bird.tickCount + 120;
            } else if (bird.tickCount % 10 == 0) bird.getNavigation().moveTo(target.getX(), target.getY() + 1.2, target.getZ(), 1.15);
        }
        if (escape != null && bird.tickCount % 20 == 0)
            bird.getNavigation().moveTo(escape.x, escape.y, escape.z, 1.35);
    }
    @Override public void stop() { target = null; escape = null; cooldown = bird.tickCount + 600; bird.getNavigation().stop(); }
}
