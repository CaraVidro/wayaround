package net.caravidro.wayaround.nature;

import net.caravidro.wayaround.network.FireFrameS2CPayload;
import net.caravidro.wayaround.worldgen.weather.fire.FireTickLimiter;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("wayaround_nature")
@PrefixGameTestTemplate(false)
public final class FireRuntimeGameTests {
    private static BlockPos denseFire(GameTestHelper h,boolean permanent) {
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(4,2,4));
        int x=p.getX() & ~7,z=p.getZ() & ~7;
        for(int dx=0;dx<8;dx++)for(int dz=0;dz<8;dz++) {
            var cell=new BlockPos(x+dx,p.getY(),z+dz);
            l.setBlock(cell.below(),(permanent?Blocks.NETHERRACK:Blocks.STONE).defaultBlockState(),18);
            l.setBlock(cell,Blocks.FIRE.defaultBlockState(),18);
        }
        return new BlockPos(x+3,p.getY(),z+3);
    }
    @GameTest(template="assembly_test",batch="fire",timeoutTicks=80)
    public static void disabledFireTickNeverThinsExistingFire(GameTestHelper h) {
        var l=h.getLevel();var p=denseFire(h,false);var rule=l.getGameRules().getRule(GameRules.RULE_DOFIRETICK);boolean old=rule.get();
        try {
            rule.set(false,l.getServer());l.getBlockState(p).tick(l,p,net.minecraft.util.RandomSource.create(1));
            h.assertTrue(l.getBlockState(p).is(Blocks.FIRE),"doFireTick=false preserves even a crowded fire cell");
        } finally {rule.set(old,l.getServer());}
        h.succeed();
    }
    @GameTest(template="assembly_test",batch="fire",timeoutTicks=80)
    public static void denseFireThinsWithoutExtinguishingPermanentSources(GameTestHelper h) {
        var l=h.getLevel();var p=denseFire(h,true);
        h.assertTrue(!FireTickLimiter.shouldThin(l,p),"Dimension infiniburn support preserves permanent flames");
        l.setBlock(p.below(),Blocks.STONE.defaultBlockState(),18);
        h.assertTrue(FireTickLimiter.shouldThin(l,p),"Crowded ordinary flames are bounded by the cached cell count");
        h.succeed();
    }
    @GameTest(template="assembly_test",batch="fire",timeoutTicks=80)
    public static void flameFrameRoundTripAndAllocationLimits(GameTestHelper h) {
        var flames=new java.util.ArrayList<FireFrameS2CPayload.Flame>();
        for(int i=0;i<128;i++)flames.add(new FireFrameS2CPayload.Flame(new BlockPos(-100+i,64,20).asLong(),.25F,.03F,.72F,2.1F));
        var payload=new FireFrameS2CPayload(flames);
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try {
            FireFrameS2CPayload.STREAM_CODEC.encode(buffer,payload);
            h.assertTrue(payload.equals(FireFrameS2CPayload.STREAM_CODEC.decode(buffer)),"Block identity, off-center anchors and flame size survive batching");
            buffer.clear();buffer.writeVarInt(129);boolean rejected=false;
            try{FireFrameS2CPayload.STREAM_CODEC.decode(buffer);}catch(IllegalArgumentException expected){rejected=true;}
            h.assertTrue(rejected,"Oversized wire frames are rejected before allocating entries");
            rejected=false;
            try{new FireFrameS2CPayload(java.util.List.of(new FireFrameS2CPayload.Flame(0,Float.NaN,0,0,1)));}catch(IllegalArgumentException expected){rejected=true;}
            h.assertTrue(rejected,"Non-finite render coordinates are rejected");
        } finally {buffer.release();}
        h.succeed();
    }
}
