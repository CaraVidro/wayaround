package net.caravidro.wayaround.worldgen.planet;

public final class PlanetMathTest {
    static int checks;
    static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    public static void main(String[] args) {
        check(PlanetMath.wrap(32768)==-32768,"east goes west");check(PlanetMath.wrap(-32769)==32767,"west goes east");
        check(PlanetMath.wrap(32768.375)==-32767.625,"sub-block overshoot retained");
        check(PlanetMath.delta(32760,-32760)==16,"short distance across seam");
        check(PlanetMath.antarctica(0,22528)>.99,"polar continent center");
        check(PlanetMath.antarctica(0,32000)==0&&PlanetMath.antarctica(22000,22528)==0,"Antarctica has finite coasts");
        for(int x=-90000;x<90000;x+=23) {
            int folded=PlanetMath.wrap(x);check(folded>=-32768&&folded<32768,"finite domain");
            check(folded==PlanetMath.wrap(x+65536)&&folded==PlanetMath.wrap(x-65536),"same location after lap");
            check(Math.abs(PlanetMath.southernOcean(x,32000)-PlanetMath.southernOcean(x+65536,32000))<1e-10,"periodic polar sea");
            check(Math.abs(PlanetMath.antarctica(x,22528)-PlanetMath.antarctica(x,22528+65536))<1e-10,"periodic polar continent");
        }
        double previous=PlanetMath.height(-1,0,.4);int oceans=0,lowlands=0,peaks=0;
        for(int c=-100;c<=100;c++)for(int e=-100;e<=100;e+=5)for(int r=-100;r<=100;r+=5) {
            double height=PlanetMath.height(c/100.0,e/100.0,r/100.0);
            check(Double.isFinite(height)&&height>=-54&&height<=286,"height bounds");
            if(height<50)oceans++;if(height>=64&&height<120)lowlands++;if(height>230)peaks++;
        }
        for(int i=-1000;i<1000;i++) {
            double next=PlanetMath.height(i/1000.0,.1,.5);check(Math.abs(next-previous)<2||i==-1000,"continuous coasts");previous=next;
        }
        check(oceans>10000&&lowlands>10000&&peaks>1000,"variety: open sea, buildable plains and high mountains");
        System.out.println("Planet geography: "+checks+" checks passed (both axes, overshoot, finite poles, periodic geography, smooth coasts, height variety)");
    }
}
