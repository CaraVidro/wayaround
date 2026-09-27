package net.caravidro.wayaround.debug;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Tiny projectile laboratory.
 *
 * Normal right click: cycle projectile.
 * Sneak + right click: fire.
 *
 * Modes intentionally use vanilla projectile entities so the block stays
 * disposable and useful for testing generic Infinity behavior.
 */
public final class DebugTurretBlock
        extends HorizontalDirectionalBlock {

    public static final MapCodec<DebugTurretBlock> CODEC =
            simpleCodec(
                    DebugTurretBlock::new
            );

    public static final DirectionProperty FACING =
            HorizontalDirectionalBlock.FACING;

    public static final IntegerProperty MODE =
            IntegerProperty.create(
                    "mode",
                    0,
                    3
            );

    public DebugTurretBlock(
            Properties properties
    ) {
        super(
                properties
        );

        registerDefaultState(
                stateDefinition.any()
                        .setValue(
                                FACING,
                                Direction.NORTH
                        )
                        .setValue(
                                MODE,
                                0
                        )
        );
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        return defaultBlockState()
                .setValue(
                        FACING,
                        context.getHorizontalDirection()
                                .getOpposite()
                );
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            fire(
                    serverLevel,
                    pos,
                    state
            );

            player.displayClientMessage(
                    Component.literal(
                            "Debug Turret: FIRE / "
                                    + modeName(
                                    state.getValue(
                                            MODE
                                    )
                            )
                    ),
                    true
            );

            return InteractionResult.CONSUME;
        }

        int nextMode =
                (
                        state.getValue(
                                MODE
                        )
                                + 1
                ) % 4;

        level.setBlock(
                pos,
                state.setValue(
                        MODE,
                        nextMode
                ),
                3
        );

        player.displayClientMessage(
                Component.literal(
                        "Debug Turret: "
                                + modeName(
                                nextMode
                        )
                ),
                true
        );

        level.playSound(
                null,
                pos,
                SoundEvents.UI_BUTTON_CLICK.value(),
                SoundSource.BLOCKS,
                0.45F,
                1.25F
                        + nextMode
                                * 0.08F
        );

        return InteractionResult.CONSUME;
    }

    private static void fire(
            ServerLevel level,
            BlockPos pos,
            BlockState state
    ) {
        int mode =
                state.getValue(
                        MODE
                );

        Direction facing =
                state.getValue(
                        FACING
                );

        Vec3 direction =
                Vec3.atLowerCornerOf(
                                facing.getNormal()
                        )
                        .normalize();

        Vec3 origin =
                Vec3.atCenterOf(
                                pos
                        )
                        .add(
                                direction.scale(
                                        0.72
                                )
                        )
                        .add(
                                0.0,
                                0.10,
                                0.0
                        );

        Entity projectile;

        double speed;

        switch (mode) {
            case 1 -> {
                projectile =
                        EntityType.ARROW.create(
                                level
                        );

                speed =
                        8.5;
            }

            case 2 -> {
                projectile =
                        EntityType.SNOWBALL.create(
                                level
                        );

                speed =
                        1.8;
            }

            case 3 -> {
                projectile =
                        EntityType.SMALL_FIREBALL.create(
                                level
                        );

                speed =
                        2.4;
            }

            default -> {
                projectile =
                        EntityType.ARROW.create(
                                level
                        );

                speed =
                        3.0;
            }
        }

        if (projectile == null) {
            return;
        }

        projectile.setPos(
                origin.x,
                origin.y,
                origin.z
        );

        projectile.setDeltaMovement(
                direction.scale(
                        speed
                )
        );

        /*
         * Mode 1 is the debug "bullet": an arrow entity with extreme speed
         * and no gravity. Infinity only cares that it is a Projectile and how
         * fast it entered the field.
         */
        if (mode == 1) {
            projectile.setNoGravity(
                    true
            );
        }

        level.addFreshEntity(
                projectile
        );

        level.playSound(
                null,
                pos,
                mode == 1
                        ? SoundEvents.FIREWORK_ROCKET_BLAST
                        : SoundEvents.DISPENSER_DISPENSE,
                SoundSource.BLOCKS,
                mode == 1
                        ? 0.85F
                        : 0.55F,
                mode == 1
                        ? 1.65F
                        : 1.0F
        );
    }

    private static String modeName(
            int mode
    ) {
        return switch (mode) {
            case 1 -> "FAST BULLET";
            case 2 -> "SNOWBALL";
            case 3 -> "FIREBALL";
            default -> "ARROW";
        };
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                FACING,
                MODE
        );
    }
}
