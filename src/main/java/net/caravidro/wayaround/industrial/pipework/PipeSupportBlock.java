package net.caravidro.wayaround.industrial.pipework;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.Block;

public final class PipeSupportBlock
        extends Block {

    public static final MapCodec<PipeSupportBlock> CODEC =
            simpleCodec(
                    PipeSupportBlock::new
            );

    public PipeSupportBlock(
            Properties properties
    ) {
        super(
                properties
        );
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }
}
