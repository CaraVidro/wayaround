package net.caravidro.wayaround.world.calving;

import java.util.List;
import net.caravidro.wayaround.worldgen.geography.AntarcticField;
import net.caravidro.wayaround.worldgen.terrain.AntarcticTerrain;
import net.caravidro.wayaround.worldgen.terrain.IceCliffField;

/** Standalone geometry checks, independent of the Minecraft server and rendering. */
public final class CalvingGeometryTest {
    public static void main(String[] args) {
        net.caravidro.wayaround.worldconfig.WorldFeatureRuntime.applyServer(net.caravidro.wayaround.worldconfig.WorldFeatureSettings.legacy());
        var slab = List.of(new CalvingFall.Cell(-1, 100, 0), new CalvingFall.Cell(-1, 101, 0),
                new CalvingFall.Cell(0, 100, 0));
        check(CalvingFall.drop(slab, 7, 0, -64, cell -> cell.y() <= 40) == 59,
                "Slab must land directly above the seabed, without the old 15-block limit");
        check(CalvingFall.drop(slab, 7, 0, -64,
                cell -> cell.y() <= (cell.x() == 7 ? 80 : 40)) == 19,
                "Highest support under the footprint must stop a rigid slab");
        check(CalvingFall.drop(slab, 0, 0, -64, cell -> false) == 164, "Respect world bottom");
        check(CalvingFall.drop(slab, 0, 0, -64, cell -> cell.y() <= 99) == 0, "Already supported");
        check(CalvingFall.drop(List.of(), 0, 0, -64, cell -> false) == 0, "Empty snapshot");

        int walls = 0;
        int coasts = 0;
        boolean overhang = false;
        for (int x = -12000; x <= 12000; x += 32) {
            int z = coastline(x);
            coasts++;
            if (IceCliffField.coastalStrength(x, z) > 0.99) {
                walls++;
                double low = IceCliffField.coastBlend(x, 0, z - 3, AntarcticField.sample(x, z - 3));
                double high = IceCliffField.coastBlend(x, 0, z + 3, AntarcticField.sample(x, z + 3));
                check(high - low > 0.9, "Strong coastal walls must rise within six blocks");
                if (IceCliffField.hasOverhang(x, z - 4)) {
                    double top = AntarcticTerrain.getSurfaceHeight(x, z - 4);
                    double latitude = AntarcticField.sample(x, z - 4);
                    double cap = IceCliffField.coastBlend(x, (int) top - 5, z - 4, latitude);
                    double base = IceCliffField.coastBlend(x, (int) top - 20, z - 4, latitude);
                    overhang |= cap - base > 0.8;
                }
            }
        }
        check(walls > coasts * 0.3 && walls < coasts * 0.8, "Common walls, with gentle coast remaining");
        check(overhang, "Some walls must have a projecting cap");

        int raised = 0;
        int inlandSamples = 0;
        boolean undercut = false;
        for (int x = -12000; x < 12000; x += 16) {
            for (int z = 34000; z < 42000; z += 16) {
                var inland = IceCliffField.inland(x, z);
                inlandSamples++;
                if (inland.lift() > 0) raised++;
                if (inland.lift() > 15 && inland.edge() > 1 && inland.edge() < 5
                        && IceCliffField.hasOverhang(x, z)) {
                    int top = (int) AntarcticTerrain.getSurfaceHeight(x, z);
                    check(AntarcticTerrain.sampleDensity(x, top - 5, z) > 0, "Solid inland cap");
                    check(AntarcticTerrain.sampleDensity(x, top - 15, z) < 0, "Open space under cap");
                    undercut = true;
                }
            }
        }
        check(raised > 0 && raised < inlandSamples * 0.02, "Inland cliffs must remain rare");
        check(undercut, "Rare inland overhangs exist");
        check(IceCliffField.inland(0, 0).lift() == 0, "No inland cliffs outside Antarctica");
        finiteCoasts();
        System.out.println("Calving geometry passed: coastal walls=" + walls + "/" + coasts
                + ", inland raised samples=" + raised + "/" + inlandSamples);
    }

    private static void finiteCoasts() {
        net.caravidro.wayaround.worldconfig.WorldFeatureRuntime.applyServer(net.caravidro.wayaround.worldconfig.WorldFeatureSettings.defaults());
        int walls=0,coasts=0;
        for(int x=-12000;x<=12000;x+=64)for(int direction:new int[]{-1,1}) {
            int inside=22528;
            check(AntarcticField.sample(x,inside)>.5022,"Closed continent has an interior");
            int z=inside;
            while(AntarcticField.sample(x,z)>=.5022&&Math.abs(z-inside)<8000)z+=direction;
            coasts++;
            check(Math.abs(z-inside)<8000,"Both ends of the continent have a finite coast");
            check(AntarcticField.sample(x,z+direction*4000)<.01,"Ocean exists beyond both Antarctic shores");
            if(IceCliffField.coastalStrength(x,z)>.99) {
                walls++;
                double outer=IceCliffField.coastBlend(x,0,z+direction*3,AntarcticField.sample(x,z+direction*3));
                double inner=IceCliffField.coastBlend(x,0,z-direction*3,AntarcticField.sample(x,z-direction*3));
                check(inner-outer>.9,"Finite northern and southern coasts retain steep walls");
            }
        }
        check(walls>coasts*.3&&walls<coasts*.8,"Finite coastline retains both cliffs and gentle shore");
        System.out.println("Finite calving coasts passed: walls="+walls+"/"+coasts);
    }

    private static int coastline(int x) {
        int low = 24000, high = 28000;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (AntarcticField.sample(x, middle) < 0.5022) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
