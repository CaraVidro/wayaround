package net.caravidro.wayaround.industrial.pipework;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
/** First click pays the first section; later clicks use the very same item. */
public final class LargePipeItem extends BlockItem {
    public LargePipeItem(LargePipeBlock block,Properties p){super(block,p);}
    @Override protected boolean canPlace(BlockPlaceContext context,BlockState state){
        return super.canPlace(context,state)&&PipeBlockEntity.roomFor(context.getLevel(),context.getClickedPos(),state);
    }
    @Override protected boolean placeBlock(BlockPlaceContext context,BlockState state){
        if(!super.placeBlock(context,state))return false;
        if(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof PipeBlockEntity pipe)pipe.firstSection(context.getItemInHand());
        return true;
    }
}
