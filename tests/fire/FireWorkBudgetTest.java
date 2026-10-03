import net.caravidro.wayaround.worldgen.weather.fire.FireWorkBudget;
public final class FireWorkBudgetTest {
    public static void main(String[] args) {
        var probes=new FireWorkBudget(4,256);
        int allowed=0;for(int tick=0;tick<4;tick++)for(int i=0;i<10000;i++)if(probes.tryUse(tick))allowed++;
        check(allowed==256,"Failed searches cannot multiply work within a four-tick window");
        check(probes.tryUse(4),"Next window replenishes work");
        var ticks=new FireWorkBudget(1,64);
        for(int i=0;i<64;i++)check(ticks.tryUse(8),"Up to 64 native fire ticks are processed");
        check(!ticks.tryUse(8)&&ticks.tryUse(9),"Burst overload is deferred, not unbounded");
        probes.clear();check(probes.tryUse(0),"New server/world resets stale budgets");
        System.out.println("Fire budgets: bounded failed probes, native tick bursts and lifecycle reset passed");
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
