import net.caravidro.wayaround.daybreak.DaysBreakMath;
public final class DaysBreakMathTest {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args){
        check(DaysBreakMath.day(0)&&DaysBreakMath.day(11999)&&!DaysBreakMath.day(12000)&&!DaysBreakMath.day(23999)&&DaysBreakMath.day(24000)&&!DaysBreakMath.day(-1),"Calendar boundaries");
        check(DaysBreakMath.sunScale(0)==1&&DaysBreakMath.sunScale(1200)==2&&DaysBreakMath.sunScale(2400)==4,"Exponential growth");
        check(DaysBreakMath.sunScale(Long.MAX_VALUE)==12&&DaysBreakMath.sunScale(-10)==1,"Finite safe geometry");
        for(int i=1;i<=20;i++)check(DaysBreakMath.slowLevel(i)>=DaysBreakMath.slowLevel(i-1)&&DaysBreakMath.damage(i)>DaysBreakMath.damage(i-1),"Cumulative exposure");
        check(DaysBreakMath.slowLevel(999)==4&&DaysBreakMath.damage(999)==8,"Bounded effects");
        System.out.println("Daybreak calendar, growth and exposure contracts passed");
    }
}
