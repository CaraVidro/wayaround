package net.caravidro.wayaround.media;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class ChairBlockEntity
        extends BlockEntity {

    public static final String SEAT_TAG =
            "wayaround_chair_seat";

    public ChairBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                MediaContent.CHAIR_ENTITY.get(),
                pos,
                state
        );
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            ChairBlockEntity chair
    ) {
        if (level.getGameTime()
                % 20L != 0L) {

            return;
        }

        AABB area =
                new AABB(pos)
                        .inflate(
                                0.40,
                                0.75,
                                0.40
                        );

        List<ArmorStand> seats =
                level.getEntitiesOfClass(
                        ArmorStand.class,
                        area,
                        entity ->
                                entity.getTags()
                                        .contains(
                                                SEAT_TAG
                                        )
                );

        for (ArmorStand seat : seats) {
            if (seat.getPassengers()
                    .isEmpty()) {

                seat.discard();
            }
        }
    }

    public static void removeSeat(
            Level level,
            BlockPos pos
    ) {
        AABB area =
                new AABB(pos)
                        .inflate(
                                0.45,
                                0.80,
                                0.45
                        );

        for (ArmorStand seat :
                level.getEntitiesOfClass(
                        ArmorStand.class,
                        area,
                        entity ->
                                entity.getTags()
                                        .contains(
                                                SEAT_TAG
                                        )
                )) {

            seat.ejectPassengers();
            seat.discard();
        }
    }
}
