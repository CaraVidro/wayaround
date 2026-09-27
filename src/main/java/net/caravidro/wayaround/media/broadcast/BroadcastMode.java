package net.caravidro.wayaround.media.broadcast;

public enum BroadcastMode {
    ON_AIR,
    OFF_AIR,
    INTERMISSION;

    public BroadcastMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
