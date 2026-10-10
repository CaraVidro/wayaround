# Differential check against the pre-optimization field, using registry-free stubs.
# This checks field math/search equivalence with the deliberate 176-block altitude lift,
# not actual biome terrain or runtime FPS.
from pathlib import Path
import tempfile,subprocess
root=Path(__file__).resolve().parents[2]
work=Path(tempfile.mkdtemp(prefix='way-weather-'))
def put(path,text):
 p=work/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text)
package='net/caravidro/wayaround/worldgen/weather/local/'
put(package+'LocalWeatherField.java',(root/('src/main/java/'+package+'LocalWeatherField.java')).read_text())
baseline=subprocess.check_output(['git','show','e95b30e731c40af60673315e43b7c253385505bc:src/main/java/'+package+'LocalWeatherField.java'],cwd=root,text=True)
put(package+'LegacyWeatherField.java',baseline.replace('LocalWeatherField','LegacyWeatherField'))
put('net/minecraft/util/Mth.java','package net.minecraft.util; public class Mth { public static float clamp(float x,float a,float b){return Math.max(a,Math.min(b,x));} public static double clamp(double x,double a,double b){return Math.max(a,Math.min(b,x));} }')
put('net/minecraft/world/level/Level.java','package net.minecraft.world.level; public class Level {}')
put('net/caravidro/wayaround/performance/PerformanceProfiler.java','package net.caravidro.wayaround.performance; public class PerformanceProfiler {public enum Section{LOCAL_WEATHER} public static long begin(Section s){return 0;} public static void end(Section s,long t){} }')
put(package+'WindTestManager.java','package net.caravidro.wayaround.worldgen.weather.local; public class WindTestManager { public static float strengthAt(double x,double z,long t){return 0;} }')
# No overrides are active in the equivalence fixture. Stub the new runtime hook
# so baseline and optimized field math remain comparable without NeoForge.
put(package+'CloudRainOverrides.java','package net.caravidro.wayaround.worldgen.weather.local; import net.minecraft.world.level.Level; public class CloudRainOverrides { public static LocalWeatherField.CloudCell apply(Level l,LocalWeatherField.CloudCell c,long t){return c;} }')
put(package+'RegionalCloudClimate.java','''package net.caravidro.wayaround.worldgen.weather.local;
import net.minecraft.world.level.Level;
public class RegionalCloudClimate {
 public static long calls;
 public static LocalWeatherField.CloudCell adapt(Level l,LocalWeatherField.CloudCell c){calls++;if(l==null)return c;if((c.id()&3)==0)return null;return new LocalWeatherField.CloudCell(c.id(),c.x(),c.z(),c.y(),c.radius()*1.04,c.storm()*.85f);}
 public static LegacyWeatherField.CloudCell adapt(Level l,LegacyWeatherField.CloudCell c){calls++;if(l==null)return c;if((c.id()&3)==0)return null;return new LegacyWeatherField.CloudCell(c.id(),c.x(),c.z(),c.y(),c.radius()*1.04,c.storm()*.85f);}
}''')
put('WeatherDifferential.java','''import java.util.*; import net.caravidro.wayaround.worldgen.weather.local.*; import net.minecraft.world.level.Level;
public class WeatherDifferential {
 static void eq(float a,float b){if(Float.floatToIntBits(a)!=Float.floatToIntBits(b))throw new AssertionError(a+" != "+b);}
 public static void main(String[] args){ Random r=new Random(1956);long oldCalls=0,newCalls=0;
 for(int i=0;i<3000;i++){
 double x=r.nextDouble()*60000000-30000000,z=r.nextDouble()*60000000-30000000; long t=Math.abs(r.nextLong()%1000000000); Level l=(i&1)==0?null:new Level(); double range=r.nextInt(800);
 RegionalCloudClimate.calls=0;var old=LegacyWeatherField.sample(l,x,z,t);oldCalls+=RegionalCloudClimate.calls;
 RegionalCloudClimate.calls=0;var now=LocalWeatherField.sample(l,x,z,t);newCalls+=RegionalCloudClimate.calls;
 eq(old.cloud(),now.cloud());eq(old.rain(),now.rain());eq(old.warning(),now.warning());eq(old.windX(),now.windX());eq(old.windZ(),now.windZ());
 var a=LegacyWeatherField.nearbyCells(l,x,z,t,range);var b=LocalWeatherField.nearbyCells(l,x,z,t,range);
 if(a.size()!=b.size())throw new AssertionError("cell count");
 for(int j=0;j<a.size();j++){var c=a.get(j);var d=b.get(j);if(c.id()!=d.id()||c.x()!=d.x()||c.z()!=d.z()||Math.abs(c.y()+176.0-d.y())>1e-9||d.y()<324.0||c.radius()!=d.radius()||c.storm()!=d.storm())throw new AssertionError("cell mismatch");}
 }
 System.out.println("3000 weather queries unchanged; cloud centers raised 176 blocks with floor 324; climate adaptations "+oldCalls+" -> "+newCalls);
 }
}''')
files=[str(p) for p in work.rglob('*.java')]
subprocess.run(['java','com.sun.tools.javac.Main','-d',str(work/'classes'),*files],check=True)
subprocess.run(['java','-cp',str(work/'classes'),'WeatherDifferential'],check=True)
