package net.caravidro.wayaround.nature;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;

public final class AppleTreePlacement {
    private AppleTreePlacement() {}
    public static boolean place(LevelAccessor level,BlockPos root,Predicate<BlockPos> writable) {
        if(!level.getBlockState(root.below()).is(BlockTags.DIRT)&&!level.getBlockState(root.below()).is(Blocks.FARMLAND))return false;
        for(int x=-2;x<=2;x++)for(int y=0;y<=5;y++)for(int z=-2;z<=2;z++){
            BlockPos p=root.offset(x,y,z);if(!writable.test(p))return false;
            var b=level.getBlockState(p);
            if(p.equals(root)&&!b.is(NatureContent.APPLE_SAPLING.get())&&!b.isAir()&&!b.canBeReplaced())return false;
            if(!p.equals(root)&&!b.isAir()&&!b.is(BlockTags.LEAVES)&&!b.canBeReplaced())return false;
            if(level.getBlockEntity(p)!=null&&!p.equals(root))return false;
        }
        for(int y=1;y<=3;y++)level.setBlock(root.above(y),Blocks.OAK_LOG.defaultBlockState(),2);
        for(int y=3;y<=5;y++)for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
            if(x==0&&z==0&&y==3||Math.abs(x)==2&&Math.abs(z)==2||y==5&&(Math.abs(x)>1||Math.abs(z)>1))continue;
            level.setBlock(root.offset(x,y,z),NatureContent.APPLE_LEAVES.get().defaultBlockState().setValue(LeavesBlock.DISTANCE,1),2);
        }
        level.setBlock(root,NatureContent.APPLE_LOG.get().defaultBlockState(),3);
        return true;
    }
}
