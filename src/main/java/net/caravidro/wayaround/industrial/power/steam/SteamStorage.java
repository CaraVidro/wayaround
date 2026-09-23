package net.caravidro.wayaround.industrial.power.steam;

public final class SteamStorage {
    private final int capacity;
    private int stored;

    public SteamStorage(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be positive");
        this.capacity = capacity;
    }

    public int stored() { return stored; }
    public int capacity() { return capacity; }
    public int room() { return capacity - stored; }

    public double pressureBar() {
        return SteamUnits.MAX_PRESSURE_BAR * stored / (double) capacity;
    }

    public int receive(int amount, boolean simulate) {
        int accepted = Math.min(room(), Math.max(0, amount));
        if (!simulate) stored += accepted;
        return accepted;
    }

    public int extract(int amount, boolean simulate) {
        int extracted = Math.min(stored, Math.max(0, amount));
        if (!simulate) stored -= extracted;
        return extracted;
    }

    public void load(int saved) {
        stored = Math.max(0, Math.min(capacity, saved));
    }
}
