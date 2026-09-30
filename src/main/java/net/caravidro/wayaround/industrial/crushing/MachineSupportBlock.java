package net.caravidro.wayaround.industrial.crushing;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Standalone supporting beams: removable world blocks, not a ghost multiblock. */
public final class MachineSupportBlock extends Block {
    public static final MapCodec<MachineSupportBlock> CODEC = simpleCodec(p -> new MachineSupportBlock(false, p));
    private static final VoxelShape SHAPE = Shapes.or(box(0,0,0,16,3,16), box(2,3,2,5,15,5),
        box(11,3,11,14,15,14), box(1,13,1,15,16,15));
    private final boolean reinforced;
    public MachineSupportBlock(boolean reinforced, Properties properties) { super(properties); this.reinforced = reinforced; }
    public boolean reinforced() { return reinforced; }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
}
