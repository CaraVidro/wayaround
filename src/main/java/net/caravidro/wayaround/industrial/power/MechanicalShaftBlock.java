package net.caravidro.wayaround.industrial.power;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class MechanicalShaftBlock
        extends RotatedPillarBlock
        implements EntityBlock {

    private static final VoxelShape SHAFT_Y =
            box(
                    5.0,
                    0.0,
                    5.0,
                    11.0,
                    16.0,
                    11.0
            );

    private static final VoxelShape SHAFT_X =
            box(
                    0.0,
                    5.0,
                    5.0,
                    16.0,
                    11.0,
                    11.0
            );

    private static final VoxelShape SHAFT_Z =
            box(
                    5.0,
                    5.0,
                    0.0,
                    11.0,
                    11.0,
                    16.0
            );

    public MechanicalShaftBlock(
            Properties properties
    ) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new MechanicalTransmissionBlockEntity(
                pos,
                state
        );
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack
    ) {
        super.setPlacedBy(
                level,
                pos,
                state,
                placer,
                stack
        );

        if (!level.isClientSide
                && level.getBlockEntity(
                pos
        ) instanceof MechanicalTransmissionBlockEntity transmission) {

            transmission.restoreFromItem(
                    stack
            );
        }
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState replacement,
            boolean moving
    ) {
        if (!state.is(
                replacement.getBlock()
        )
                && !level.isClientSide
                && level.getBlockEntity(
                pos
        ) instanceof MechanicalTransmissionBlockEntity transmission) {

            transmission.dropAssembly();
        }

        super.onRemove(
                state,
                level,
                pos,
                replacement,
                moving
        );
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        Direction.Axis axis =
                state.getValue(
                        AXIS
                );

        return axis == Direction.Axis.X
                ? SHAFT_X
                : axis == Direction.Axis.Z
                        ? SHAFT_Z
                        : SHAFT_Y;
    }
}
