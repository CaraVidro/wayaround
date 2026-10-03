package net.caravidro.wayaround.ecology;

/** Seeded regional history can be evaluated without loading or ticking a chunk. */
public final class EcologicalHistory {
    private EcologicalHistory() {}
    public static long mix(long v) {
        v=(v^(v>>>30))*0xBF58476D1CE4E5B9L;
        v=(v^(v>>>27))*0x94D049BB133111EBL;
        return v^(v>>>31);
    }
    public static long phase(long seed,long address,long gameTime,long period) {
        if(period<=0)throw new IllegalArgumentException("period");
        return (Math.floorMod(gameTime,period)+Math.floorMod(mix(seed^address),period))%period;
    }
    public static long elapsed(long now,long previous,long cap) {
        return previous<0||now<=previous?0:Math.min(cap,now-previous);
    }
}
