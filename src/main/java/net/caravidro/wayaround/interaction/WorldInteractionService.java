package net.caravidro.wayaround.interaction;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Common physical interaction bus.
 *
 * <p>Emitters do not import machine classes. Receivers opt in through
 * StructuralReceiver. V1 intentionally targets block entities/assembled
 * structures first; ordinary terrain destruction remains owned by each
 * gameplay system.</p>
 */
public final class WorldInteractionService {

    private static final double MAX_RADIUS =
            16.0;

    private static final int MAX_RECEIVERS_PER_EMISSION =
            128;

    private WorldInteractionService() {
    }

    public static int applyForce(
            ServerLevel level,
            WorldForce force
    ) {
        double radius =
                Math.min(
                        MAX_RADIUS,
                        Math.max(
                                0.5,
                                force.radius()
                        )
                );

        BlockPos min =
                BlockPos.containing(
                        force.origin()
                                .subtract(
                                        radius,
                                        radius,
                                        radius
                                )
                );

        BlockPos max =
                BlockPos.containing(
                        force.origin()
                                .add(
                                        radius,
                                        radius,
                                        radius
                                )
                );

        Set<BlockEntity> visited =
                new HashSet<>();

        int receivers =
                0;

        for (BlockPos pos :
                BlockPos.betweenClosed(
                        min,
                        max
                )) {
            if (receivers
                    >= MAX_RECEIVERS_PER_EMISSION) {
                break;
            }

            if (!level.hasChunkAt(
                    pos
            )) {
                continue;
            }

            BlockEntity blockEntity =
                    level.getBlockEntity(
                            pos
                    );

            if (!(blockEntity
                    instanceof StructuralReceiver receiver)
                    || !visited.add(
                    blockEntity
            )) {
                continue;
            }

            Vec3 target =
                    Vec3.atCenterOf(
                            receiver.structuralPosition()
                    );

            float magnitude =
                    force.magnitudeAt(
                            target
                    );

            if (magnitude <= 0.001F) {
                continue;
            }

            receiver.receiveWorldForce(
                    force,
                    magnitude
            );

            receivers++;
        }

        return receivers;
    }

    public static void damage(
            StructuralReceiver receiver,
            StructuralDamage damage
    ) {
        receiver.receiveStructuralDamage(
                damage
        );
    }
}
