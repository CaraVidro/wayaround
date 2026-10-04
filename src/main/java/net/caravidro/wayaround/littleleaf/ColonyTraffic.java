package net.caravidro.wayaround.littleleaf;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.*;

/** Local right-of-way, using bodies and safe retreat space rather than pushing nestmates. */
public final class ColonyTraffic {
    public static int priority(ColonyInsectEntity e){return e.emergency()?100:e.carryingBody()?90:e.caste()==1&&e.getTarget()!=null?80:e.carryingMaterial()?70:e.carrying()?60:e.getDeltaMovement().y<-.02?50:e.climbing()?40:20;}
    public static boolean precedes(ColonyInsectEntity a,ColonyInsectEntity b){int ap=priority(a),bp=priority(b);return ap>bp||(ap==bp&&a.getId()<b.getId());}
    public static boolean permit(ColonyInsectEntity e,Vec3 next){
        if(!(e.level() instanceof ServerLevel l)||!ColonyBudget.search(l))return true;
        var delta=next.subtract(e.position());if(delta.lengthSqr()<1e-6)return true;
        double look=e.enlarged()?3:.35;var step=delta.normalize().scale(Math.min(look,delta.length()));var body=e.getBoundingBox().move(step).inflate(e.enlarged()?.12:.025);int checked=0;
        for(var other:l.getEntitiesOfClass(ColonyInsectEntity.class,e.getBoundingBox().expandTowards(step).inflate(look),a->a!=e&&a.activeAdult()&&a.species()==e.species()&&java.util.Objects.equals(a.home(),e.home()))){
            if(checked++>=16)break;if(!body.intersects(other.getBoundingBox())||precedes(e,other))continue;
            var backwards=delta.normalize().scale(e.enlarged()?-.16:-.025);if(e.climbing()&&delta.y>0)backwards=new Vec3(backwards.x,-.10,backwards.z);
            var retreat=e.position().add(backwards);var floor=net.minecraft.core.BlockPos.containing(retreat).below();
            if(ColonyCoreBlockEntity.loaded(l,floor)&&l.getFluidState(floor).isEmpty()&&!l.getBlockState(floor).getCollisionShape(l,floor).isEmpty()&&l.noCollision(e,e.getBoundingBox().move(backwards)))e.setDeltaMovement(backwards);
            else e.setDeltaMovement(0,Math.min(0,e.getDeltaMovement().y),0);
            e.yieldFor(10);return false;
        }return true;
    }
    private ColonyTraffic(){}
}
