package net.caravidro.wayaround.ecology;
import net.minecraft.util.RandomSource;
public final class AbyssSoundSchedule {
 public static long delay(RandomSource random){return 2400L+random.nextInt(4801)+random.nextInt(4801);}
 private AbyssSoundSchedule(){}
}
