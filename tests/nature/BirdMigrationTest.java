import net.caravidro.wayaround.nature.BirdMigrationMath;
public final class BirdMigrationTest {
    public static void main(String[] args) {
        double x=BirdMigrationMath.x(0,1),z=BirdMigrationMath.z(0,1);
        check(Math.abs(Math.hypot(x,z)-1)<1e-12,"Forward frame is a unit horizontal vector");
        check(Math.abs(x*.16-z*.48)<1e-12,"Beak points along the migration trajectory");
        check(Math.abs(BirdMigrationMath.x(1,0)*x+BirdMigrationMath.z(1,0)*z)<1e-12,"Wingspan is perpendicular to travel");
        for(int i=0;i<36;i++)for(int age=0;age<900;age++)check(Math.abs(BirdMigrationMath.flap(age,i))<=.4,"Wing flaps never rotate the whole bird vertically");
        System.out.println("Migration: horizontal body, heading-aligned beak and symmetric flaps passed");
    }
    private static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
}
