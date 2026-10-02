package net.caravidro.wayaround.client;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import net.caravidro.wayaround.WayAround;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Visual block light only: four sources, 8-tick sampling, bounded section rebuilds. */
@EventBusSubscriber(modid=WayAround.MODID,value=Dist.CLIENT)
public final class SimpleDynamicLights {
    private record Source(Vec3 pos,int level) {}
    // Chunk builders read this immutable snapshot on worker threads.
    private static volatile List<Source> sources=List.of();
    private static final LinkedHashSet<BlockPos> dirtySections=new LinkedHashSet<>();
    private static ClientLevel owner;
    private SimpleDynamicLights() {}
    private static int emission(ItemStack stack){
        if(stack.isEmpty())return 0;
        if(stack.is(Items.LAVA_BUCKET))return 15;
        if(stack.is(Items.GLOW_INK_SAC)||stack.is(Items.GLOW_BERRIES))return 8;
        if(stack.getItem() instanceof BlockItem item)return item.getBlock().defaultBlockState().getLightEmission();
        return 0;
    }
    private static Source source(Entity e){
        int light=e.isOnFire()?15:0;
        if(e instanceof LivingEntity living)light=Math.max(light,Math.max(emission(living.getMainHandItem()),emission(living.getOffhandItem())));
        if(e instanceof ItemEntity item)light=Math.max(light,emission(item.getItem()));
        return light>0?new Source(e.position().add(0,e.getBbHeight()*.65,0),light):null;
    }
    private static void dirty(Source source){
        int radius=source.level;
        int minX=(int)Math.floor((source.pos.x-radius)/16),maxX=(int)Math.floor((source.pos.x+radius)/16);
        int minY=(int)Math.floor((source.pos.y-radius)/16),maxY=(int)Math.floor((source.pos.y+radius)/16);
        int minZ=(int)Math.floor((source.pos.z-radius)/16),maxZ=(int)Math.floor((source.pos.z+radius)/16);
        for(int x=minX;x<=maxX;x++)for(int y=minY;y<=maxY;y++)for(int z=minZ;z<=maxZ;z++){
            if(dirtySections.size()>=216)return;
            dirtySections.add(new BlockPos(x,y,z));
        }
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        var mc=Minecraft.getInstance();
        if(owner!=mc.level){sources=List.of();dirtySections.clear();owner=mc.level;}
        if(owner==null||mc.player==null)return;
        if(owner.getGameTime()%8==0){
            var next=new ArrayList<Source>(4);
            Source player=source(mc.player);if(player!=null)next.add(player);
            int inspected=0;
            for(Entity entity:owner.entitiesForRendering()){
                if(++inspected>128||next.size()>=4)break;
                if(entity==mc.player||entity.distanceToSqr(mc.player)>24*24)continue;
                Source s=source(entity);if(s!=null)next.add(s);
            }
            List<Source> updated=List.copyOf(next);
            if(!sources.equals(updated)){
                for(Source old:sources)dirty(old);
                for(Source current:updated)dirty(current);
                sources=updated;
            }
        }
        var it=dirtySections.iterator();int budget=12;
        while(it.hasNext()&&budget-->0){
            var section=it.next();it.remove();
            int x=section.getX()*16,y=section.getY()*16,z=section.getZ()*16;
            if(owner.hasChunkAt(new BlockPos(x,y,z)))mc.levelRenderer.setBlocksDirty(x,y,z,x+15,y+15,z+15);
        }
    }
    public static int light(BlockPos pos,int packed){
        int value=(packed>>4)&15;
        List<Source> snapshot=sources;
        if(value==15||snapshot.isEmpty())return packed;
        for(Source s:snapshot){
            if(s.level<=value)continue;
            double dx=pos.getX()+.5-s.pos.x,dy=pos.getY()+.5-s.pos.y,dz=pos.getZ()+.5-s.pos.z;
            double squared=dx*dx+dy*dy+dz*dz;
            if(squared<(s.level-value)*(s.level-value))value=Math.max(value,(int)Math.max(0,s.level-Math.sqrt(squared)));
        }
        return (packed&~0xF0)|(value<<4);
    }
}
