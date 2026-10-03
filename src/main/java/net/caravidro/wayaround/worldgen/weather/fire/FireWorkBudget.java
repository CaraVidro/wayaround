package net.caravidro.wayaround.worldgen.weather.fire;

/** Hard work limit for a tick window, including unsuccessful searches. */
public final class FireWorkBudget {
    private final int period, capacity;
    private long window=Long.MIN_VALUE;
    private int remaining;
    public FireWorkBudget(int period,int capacity) {
        if(period<1 || capacity<1)throw new IllegalArgumentException("Positive budget required");
        this.period=period;this.capacity=capacity;
    }
    public boolean tryUse(long tick) {
        long current=Math.floorDiv(tick,period);
        if(current!=window){window=current;remaining=capacity;}
        if(remaining==0)return false;
        remaining--;return true;
    }
    public void clear(){window=Long.MIN_VALUE;remaining=0;}
}
