package net.caravidro.wayaround.nexus.client;

import java.util.ArrayList;
import java.util.List;

/** Static celestial directions, independent of world position and chunk/render distance. */
public final class NexusSkyGeometry {
    public static final double DISTANCE = 40;
    public static final int SEGMENTS = 256;
    private static final double GAP = .062;
    private static final Color IVORY = new Color(255, 247, 232, 255);
    private static final Color RIM = new Color(189, 148, 139, 255);
    private static final Color RED = new Color(103, 4, 21, 255);
    private static final Color VOID = new Color(8, 1, 5, 255);
    public record Color(int r, int g, int b, int a) {}
    public record Vertex(float x, float y, float z, Color color) {}

    public static double center(double angle) {
        return .38 + .012 * Math.sin(angle * 3) + .004 * Math.sin(angle * 7);
    }
    public static double halfWidth(double angle) {
        return .074 * (.85 + .15 * (.5 + .5 * Math.cos(angle)));
    }
    private static double edgeNoise(double angle) {
        return .0012 * Math.sin(angle * 41) + .0006 * Math.sin(angle * 93);
    }
    public static List<Vertex> create() {
        var mesh = new ArrayList<Vertex>(18000);
        // Smooth burgundy horizon, muted lower hemisphere and almost black zenith.
        for (int ring = 0; ring < 32; ring++) for (int sector = 0; sector < 64; sector++) {
            double a = -Math.PI / 2 + ring * Math.PI / 32, b = a + Math.PI / 32;
            double t = sector * Math.PI / 32, u = t + Math.PI / 32;
            sky(mesh, a, t); sky(mesh, a, u); sky(mesh, b, u); sky(mesh, b, t);
        }
        // All layers wrap through 360 degrees; the only interruption is the intentional rupture.
        strip(mesh, -Math.PI, Math.PI, -2.8, -1.5, new Color(125, 2, 26, 0), new Color(151, 5, 31, 35));
        strip(mesh, -Math.PI, Math.PI, 1.5, 2.8, new Color(151, 5, 31, 35), new Color(125, 2, 26, 0));
        strip(mesh, -Math.PI, Math.PI, -1.5, 0, new Color(52, 1, 12, 255), RED);
        strip(mesh, -Math.PI, Math.PI, 0, 1.5, RED, new Color(61, 1, 16, 255));
        for (int side : new int[]{-1, 1}) {
            double start = side < 0 ? -Math.PI : GAP, end = side < 0 ? -GAP : Math.PI;
            strip(mesh, start, end, -1.33, -1.02, new Color(255, 189, 145, 0), new Color(255, 211, 178, 56));
            strip(mesh, start, end, 1.02, 1.33, new Color(255, 211, 178, 56), new Color(255, 189, 145, 0));
            strip(mesh, start, end, -1.02, -.77, RIM, IVORY);
            strip(mesh, start, end, -.77, .77, IVORY, IVORY);
            strip(mesh, start, end, .77, 1.02, IVORY, RIM);
        }
        // A dark, asymmetric cleft splits the luminous core; glowing broken rims continue beyond it.
        double[] main = { .026,-.60, -.012,-.43, .034,-.24, -.022,-.08,
                .014,.10, -.010,.28, .018,.43, -.028,.60, .013,.78, -.012,1.04 };
        fracture(mesh, main, .050, true);
        fracture(mesh, new double[]{-.017,.57, -.18,.68, -.31,.64, -.44,.76, -.57,.79}, .014, false);
        fracture(mesh, new double[]{.017,.73, .16,.85, .28,.90, .36,1.05}, .010, false);
        fracture(mesh, new double[]{.015,.10, .20,.01, .32,.08, .46,-.04, .61,-.10}, .016, false);
        fracture(mesh, new double[]{.032,-.25, -.13,-.32, -.24,-.28, -.37,-.41}, .011, false);
        return List.copyOf(mesh);
    }
    private static void strip(List<Vertex> mesh, double start, double end, double lower, double upper, Color bottom, Color top) {
        int pieces = Math.max(1, (int)Math.ceil((end - start) / (Math.PI * 2) * SEGMENTS));
        for (int i = 0; i < pieces; i++) {
            double a = start + (end - start) * i / pieces, b = start + (end - start) * (i + 1) / pieces;
            band(mesh, a, lower, bottom); band(mesh, b, lower, bottom);
            band(mesh, b, upper, top); band(mesh, a, upper, top);
        }
    }
    private static void band(List<Vertex> mesh, double angle, double across, Color color) {
        if(Math.abs(Math.abs(angle)-GAP)<1e-9)angle+=Math.copySign(.007*Math.sin(across*3)+across*.008,angle);
        direction(mesh, angle, center(angle) + halfWidth(angle) * across + edgeNoise(angle), DISTANCE, color, true);
    }
    private record Stroke(double[] path, double[] x, double[] y) {}
    private static void fracture(List<Vertex> mesh, double[] path, double width, boolean split) {
        Stroke joined=join(path);
        Color outer=new Color(226, 92, 76, 0), inner=new Color(249, 182, 147, 88);
        stroke(mesh, joined, -width*2.8, -width, outer, inner);
        stroke(mesh, joined, width, width*2.8, inner, outer);
        stroke(mesh, joined, -width, width, split ? IVORY : new Color(246,228,208,255), split ? IVORY : new Color(246,228,208,255));
        if(split)stroke(mesh, joined, -width*.72, width*.72, VOID, VOID);
    }
    private static Stroke join(double[] path) {
        int points=path.length/2;double[] x=new double[points],y=new double[points];
        for(int i=0;i<points;i++) {
            int before=Math.max(0,i-1),after=Math.min(points-1,i+1);
            double ax=path[i*2]-path[before*2],ay=path[i*2+1]-path[before*2+1];
            double bx=path[after*2]-path[i*2],by=path[after*2+1]-path[i*2+1];
            if(i==0){ax=bx;ay=by;}if(i==points-1){bx=ax;by=ay;}
            double al=Math.hypot(ax,ay),bl=Math.hypot(bx,by);
            double nx=-ay/al-by/bl,ny=ax/al+bx/bl,length=Math.hypot(nx,ny);
            nx/=length;ny/=length;
            double miter=Math.min(1.65,1/Math.max(.15,nx*(-by/bl)+ny*(bx/bl)));
            x[i]=nx*miter;y[i]=ny*miter;
        }
        return new Stroke(path,x,y);
    }
    private static void stroke(List<Vertex> mesh, Stroke stroke, double lower, double upper, Color lowColor, Color highColor) {
        int points=stroke.path.length/2;
        for(int i=0;i<points-1;i++) {
            strokePoint(mesh,stroke,i,lower,lowColor);strokePoint(mesh,stroke,i+1,lower,lowColor);
            strokePoint(mesh,stroke,i+1,upper,highColor);strokePoint(mesh,stroke,i,upper,highColor);
        }
    }
    private static void strokePoint(List<Vertex> mesh,Stroke stroke,int i,double side,Color color) {
        double width=side*taper(i,stroke.path.length/2);
        direction(mesh,stroke.path[i*2]+stroke.x[i]*width,stroke.path[i*2+1]+stroke.y[i]*width,DISTANCE,color,true);
    }
    private static double taper(int i, int points) {
        // Near the band the cleft is broad; the distant tips become hairline fractures.
        double middle = 1 - Math.abs(i / (double)(points - 1) * 2 - 1);
        return .06 + .94 * Math.sin(middle * Math.PI / 2);
    }
    private static void sky(List<Vertex> mesh, double latitude, double angle) {
        double y = Math.sin(latitude), horizon = Math.exp(-Math.pow((y + .10) / .49, 2));
        double lower = Math.max(0, -y);
        Color color = new Color((int)(4 + horizon * 105 + lower * 29), (int)(2 + horizon * 3), (int)(8 + horizon * 10 + lower * 5), 255);
        direction(mesh, angle, latitude, 48, color, false);
    }
    private static void direction(List<Vertex> mesh, double angle, double latitude, double radius, Color color, boolean tilted) {
        double horizontal = radius * Math.cos(latitude);
        double x = horizontal * Math.sin(angle), y = radius * Math.sin(latitude), z = -horizontal * Math.cos(angle);
        if (tilted) {
            double rx = x * Math.cos(.18) - y * Math.sin(.18), ry = x * Math.sin(.18) + y * Math.cos(.18);
            x = rx * Math.cos(.55) + z * Math.sin(.55); y = ry; z = -rx * Math.sin(.55) + z * Math.cos(.55);
        }
        mesh.add(new Vertex((float)x, (float)y, (float)z, color));
    }
    private NexusSkyGeometry() {}
}
