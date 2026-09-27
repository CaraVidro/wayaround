package net.caravidro.wayaround.media.broadcast;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class TvAntennaBlock extends Block {
    public static final MapCodec<TvAntennaBlock> CODEC = simpleCodec(TvAntennaBlock::new);

    public TvAntennaBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }
}
