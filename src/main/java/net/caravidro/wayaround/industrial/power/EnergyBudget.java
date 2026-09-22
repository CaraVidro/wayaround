package net.caravidro.wayaround.industrial.power;

import java.util.function.IntUnaryOperator;

/** A small buffer that only spends the energy a receiver actually accepted. */
public final class EnergyBudget {
    private final int capacity;
    private int stored;

    public EnergyBudget(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be positive");
        this.capacity = capacity;
    }

    public int stored() { return stored; }
    public int capacity() { return capacity; }

    public void load(int savedEnergy) {
        stored = Math.max(0, Math.min(capacity, savedEnergy));
    }

    public int add(int generated) {
        int accepted = Math.min(capacity - stored, Math.max(0, generated));
        stored += accepted;
        return accepted;
    }

    public int extract(int requested, boolean simulate) {
        int extracted = Math.min(stored, Math.max(0, requested));
        if (!simulate) stored -= extracted;
        return extracted;
    }

    public int transferTo(int limit, IntUnaryOperator receiver) {
        int offered = Math.min(stored, Math.max(0, limit));
        if (offered == 0) return 0;
        int accepted = Math.max(0, Math.min(offered, receiver.applyAsInt(offered)));
        stored -= accepted;
        return accepted;
    }
}
