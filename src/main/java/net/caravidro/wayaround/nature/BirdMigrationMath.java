package net.caravidro.wayaround.nature;

/** Local X is the wingspan; local Z is the forward direction, always horizontal. */
public final class BirdMigrationMath {
    public static final double VELOCITY_X=.48,VELOCITY_Z=.16;
    private static final double SPEED=Math.hypot(VELOCITY_X,VELOCITY_Z);
    public static final double FORWARD_X=VELOCITY_X/SPEED,FORWARD_Z=VELOCITY_Z/SPEED;
    private BirdMigrationMath() {}
    public static double x(double lateral,double forward){return -FORWARD_Z*lateral+FORWARD_X*forward;}
    public static double z(double lateral,double forward){return FORWARD_X*lateral+FORWARD_Z*forward;}
    public static double flap(double age,int bird){return Math.sin(age*.65+bird)*.40;}
}
