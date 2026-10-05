import net.caravidro.wayaround.nexus.client.NexusSkyGeometry;

public final class NexusSkyGeometryTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args) {
        var mesh=NexusSkyGeometry.create();
        check(mesh.size()%4==0&&mesh.size()<20000,"Bounded static quad mesh");
        check(Math.abs(NexusSkyGeometry.center(-Math.PI)-NexusSkyGeometry.center(Math.PI))<1e-12,"Continuous rear seam");
        check(Math.abs(NexusSkyGeometry.halfWidth(-Math.PI)-NexusSkyGeometry.halfWidth(Math.PI))<1e-12,"No width jump behind the observer");
        check(NexusSkyGeometry.halfWidth(Math.PI)/NexusSkyGeometry.halfWidth(0)>.84,"Gentle taper rather than a vanishing endpoint");
        boolean[] covered=new boolean[72];int translucent=0;
        for(var v:mesh) {
            double distance=Math.sqrt(v.x()*v.x()+v.y()*v.y()+v.z()*v.z());
            check(Double.isFinite(distance)&&distance>39.9&&distance<48.1,"Every vertex remains safely inside a short far plane");
            var color=v.color();check(color.a()>=0&&color.a()<=255&&color.r()>=0&&color.r()<=255,"Finite RGBA");
            if(color.a()>0&&color.a()<255)translucent++;
            if(color.r()>220&&color.g()>200&&color.a()==255) {
                int sector=(int)Math.floor((Math.atan2(v.x(),-v.z())+Math.PI)/(Math.PI*2)*72)%72;
                covered[sector]=true;
            }
        }
        int sectors=0;for(boolean value:covered)if(value)sectors++;
        check(sectors>=70,"Bright ribbon covers the full sky, apart from its intentional rupture");
        check(translucent>1000,"Soft halo gradients are present");
        check(mesh.equals(NexusSkyGeometry.create()),"Static deterministic backdrop, with no position/time dependence");
        System.out.println("Nexus sky geometry passed: "+checks+" checks, "+mesh.size()+" vertices, "+sectors+" bright azimuth sectors");
    }
}
