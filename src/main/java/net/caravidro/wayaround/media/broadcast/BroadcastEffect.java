package net.caravidro.wayaround.media.broadcast;

public enum BroadcastEffect {
    CLEAN,
    DISTANT,
    VHS,
    GLITCH;

    public BroadcastEffect next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
