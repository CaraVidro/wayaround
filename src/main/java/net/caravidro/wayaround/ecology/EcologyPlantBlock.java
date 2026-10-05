package net.caravidro.wayaround.ecology;

import com.mojang.serialization.MapCodec;
import net.caravidro.wayaround.environment.EnvironmentalFields;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class EcologyPlantBlock extends BushBlock {

    public static final MapCodec<EcologyPlantBlock> CODEC =
            simpleCodec(EcologyPlantBlock::new);

    public EcologyPlantBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        return state.isSolidRender(level, pos)
                || super.mayPlaceOn(state, level, pos);
    }
    @Override
    public int getFlammability(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            net.minecraft.core.Direction face
    ) {
        if (!state.getFluidState().isEmpty()) {
            return 0;
        }

        float moisture =
                level instanceof ServerLevel server
                        ? EnvironmentalFields.soilMoisture(
                        server,
                        pos
                )
                        : 0.35F;

        return Math.max(
                8,
                Math.round(
                        100.0F
                                * (
                                1.0F
                                        - moisture
                                                * 0.78F
                        )
                )
        );
    }

    @Override
    public int getFireSpreadSpeed(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            net.minecraft.core.Direction face
    ) {
        if (!state.getFluidState().isEmpty()) {
            return 0;
        }

        float moisture =
                level instanceof ServerLevel server
                        ? EnvironmentalFields.soilMoisture(
                        server,
                        pos
                )
                        : 0.35F;

        return Math.max(
                4,
                Math.round(
                        60.0F
                                * (
                                1.0F
                                        - moisture
                                                * 0.72F
                        )
                )
        );
    }
}
