package net.caravidro.wayaround.accessory;

import net.caravidro.wayaround.WayAround;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * One shared bridge between world glass breakage and wearable lenses.
 *
 * Future glass-breaking mechanics should call notifyGlassBroken instead of
 * directly knowing about goggles. Player block breaking is hooked here too.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class AccessoryGlassDamage {

    private AccessoryGlassDamage() {
    }

    public static void notifyGlassBroken(
            ServerLevel level,
            BlockPos pos,
            double radius,
            int severity
    ) {
        double radiusSq =
                radius * radius;

        Vec3 center =
                Vec3.atCenterOf(
                        pos
                );

        for (ServerPlayer player :
                level.players()) {
            double distanceSq =
                    player.position()
                            .distanceToSqr(
                                    center
                            );

            if (distanceSq > radiusSq) {
                continue;
            }

            /*
             * Close shards are much more dangerous. This is deterministic
             * enough to feel causal while still allowing glasses to survive a
             * distant pane shattering nearby.
             */
            double proximity =
                    1.0
                            - Math.sqrt(
                            distanceSq
                    ) / Math.max(
                            0.01,
                            radius
                    );

            double chance =
                    Math.min(
                            1.0,
                            0.30
                                    + proximity
                                    * 0.58
                                    + Math.max(
                                    0,
                                    severity - 1
                            ) * 0.20
                    );

            if (level.random.nextDouble()
                    > chance) {
                continue;
            }

            if (AccessoryManager.damageBreakableGlass(
                    player,
                    Math.max(
                            1,
                            severity
                    )
            )) {
                level.playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.GLASS_BREAK,
                        SoundSource.PLAYERS,
                        0.58F,
                        1.18F
                                + level.random.nextFloat()
                                * 0.28F
                );
            }
        }
    }

    public static boolean isGlass(
            BlockState state
    ) {
        String path =
                BuiltInRegistries.BLOCK
                        .getKey(
                                state.getBlock()
                        )
                        .getPath();

        return path.contains(
                "glass"
        );
    }

    @SubscribeEvent
    public static void playerBreak(
            BlockEvent.BreakEvent event
    ) {
        if (!(event.getLevel()
                instanceof ServerLevel level)
                || !isGlass(
                event.getState()
        )) {
            return;
        }

        notifyGlassBroken(
                level,
                event.getPos(),
                3.25,
                1
        );
    }
}
