package net.caravidro.wayaround.worldgen.planet;

import net.caravidro.wayaround.worldconfig.*;
import net.caravidro.wayaround.worldgen.terrain.AntarcticTerrain;

/** Periodic blend of the existing pure polar terrain, including iceberg noise at the north/south seam. */
public final class PeriodicPolarTerrain {
    public static double ice(int x,int y,int z){return sample(x,y,z,false);}
    public static double ocean(int x,int y,int z){return sample(x,y,z,true);}
    private static double base(int x,int y,int z,boolean ocean){return ocean?AntarcticTerrain.sampleIcebergOceanDensity(x,y,z):AntarcticTerrain.sampleDensity(x,y,z);}
    private static double sample(int x,int y,int z,boolean ocean) {
        if(!WorldFeatureRuntime.serverEnabled(WorldFeature.FINITE_WORLD))return base(x,y,z,ocean);
        x=PlanetMath.wrap(x);z=PlanetMath.wrap(z);
        double wx=.5*PlanetMath.smooth((Math.abs((long)x)-(PlanetMath.HALF-1024.0))/1024),wz=.5*PlanetMath.smooth((Math.abs((long)z)-(PlanetMath.HALF-1024.0))/1024);
        int ax=x<0?x+PlanetMath.SIZE:x-PlanetMath.SIZE,az=z<0?z+PlanetMath.SIZE:z-PlanetMath.SIZE;
        double a=base(x,y,z,ocean);if(wx>0)a=a*(1-wx)+base(ax,y,z,ocean)*wx;
        if(wz>0){double b=base(x,y,az,ocean);if(wx>0)b=b*(1-wx)+base(ax,y,az,ocean)*wx;a=a*(1-wz)+b*wz;}
        return a;
    }
    private PeriodicPolarTerrain() {}
}
