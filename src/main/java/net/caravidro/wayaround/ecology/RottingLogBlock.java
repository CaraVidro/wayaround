package net.caravidro.wayaround.ecology;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class RottingLogBlock extends RotatedPillarBlock {

    public static final IntegerProperty ROT =
            IntegerProperty.create("rot", 0, 3);

    public static final MapCodec<RottingLogBlock> CODEC =
            simpleCodec(RottingLogBlock::new);

    public RottingLogBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(
                defaultBlockState()
                        .setValue(AXIS, Direction.Axis.Y)
                        .setValue(ROT, 0)
        );
    }

    @Override
    public MapCodec<? extends RotatedPillarBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(AXIS, ROT);
    }

    @Override
    protected void randomTick(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random
    ) {
        float moisture = moisture(level, pos);
        int rot = state.getValue(ROT);

        if (moisture > 0.28F
                && rot < 3
                && random.nextFloat() < 0.16F + moisture * 0.24F) {
            level.setBlockAndUpdate(
                    pos,
                    state.setValue(ROT, rot + 1)
            );
            rot++;
        }

        BlockPos above = pos.above();

        if (rot >= 1
                && level.getBlockState(above).isAir()
                && random.nextFloat() < 0.12F * moisture) {

            BlockState growth;

            if (rot >= 2
                    && level.getMaxLocalRawBrightness(above) <= 9
                    && random.nextFloat() < 0.28F) {
                growth =
                        random.nextBoolean()
                                ? Blocks.BROWN_MUSHROOM.defaultBlockState()
                                : Blocks.RED_MUSHROOM.defaultBlockState();

            } else {
                growth =
                        rot >= 3 && random.nextBoolean()
                                ? EcologyContent.DAMP_FERN.get().defaultBlockState()
                                : EcologyContent.WOODLAND_SORREL.get().defaultBlockState();
            }

            if (growth.canSurvive(level, above)) {
                level.setBlockAndUpdate(above, growth);
            }
        }

        if (state.getValue(AXIS)
                == Direction.Axis.Y
                && rot >= 1
                && random.nextFloat() < 0.018F) {
            TreeLifecycleManager.tryFallStandingDead(
                    level,
                    pos,
                    random
            );
        }

        if (rot >= 3
                && moisture > 0.40F
                && random.nextFloat() < 0.045F) {
            level.setBlockAndUpdate(
                    pos,
                    random.nextFloat() < 0.62F
                            ? Blocks.ROOTED_DIRT.defaultBlockState()
                            : Blocks.MOSS_BLOCK.defaultBlockState()
            );
        }
    }

    private static float moisture(ServerLevel level, BlockPos pos) {
        float moisture = level.isRainingAt(pos.above()) ? 0.72F : 0.0F;

        for (Direction direction : Direction.values()) {
            if (level.getFluidState(pos.relative(direction)).is(FluidTags.WATER)) {
                moisture = Math.max(moisture, 1.0F);
            }
        }

        return moisture;
    }
}
