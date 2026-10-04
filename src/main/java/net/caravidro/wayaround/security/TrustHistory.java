package net.caravidro.wayaround.security;

/** Pure server policy. Only time between consecutive strong samples earns evidence. */
public final class TrustHistory {
    public enum Action { NONE, PRIVATE_WARNING, PUBLIC_WARNING, KICK, BAN }
    public double evidenceSeconds, roundSeconds, lifetimeSeconds;
    public double privateRound, publicRound;
    public int kicks, stage;
    public boolean banned;
    public long updatedAt;
    public int oreBreaks, enclosedOreBreaks;

    public Action observe(ResourceEvidence evidence, double seconds, boolean approved, int privateAt, int publicAt, int kickAt) {
        return observeConfirmed(evidence.strong(),seconds,approved,privateAt,publicAt,kickAt);
    }
    public Action observeConfirmed(boolean confirmed, double seconds, boolean approved, int privateAt, int publicAt, int kickAt) {
        // Caller authenticates the session/nonce. Unknown, clean, approved and mining-only never add evidence.
        if (approved || !confirmed) return Action.NONE;
        double elapsed = Double.isFinite(seconds) ? Math.max(0, Math.min(5, seconds)) : 0;
        evidenceSeconds = Math.min(86400, evidenceSeconds + elapsed);
        lifetimeSeconds = Math.min(31536000, lifetimeSeconds + elapsed);
        roundSeconds = Math.min(86400, roundSeconds + elapsed);
        if (stage == 0 && evidenceSeconds >= privateAt) { stage = 1; privateRound=roundSeconds; return Action.PRIVATE_WARNING; }
        if (stage == 1 && roundSeconds >= Math.max(publicAt,privateRound+20)) { stage = 2; publicRound=roundSeconds; return Action.PUBLIC_WARNING; }
        if (stage == 2 && roundSeconds >= Math.max(kickAt,publicRound+20)) return Action.KICK;
        return Action.NONE;
    }

    /** Called only after an actual disconnect is being applied; audit-only verdicts do not count. */
    public Action expelled() {
        kicks++;
        roundSeconds = 0;
        stage = 0;
        privateRound=0;publicRound=0;
        if (kicks >= 3) { banned = true; return Action.BAN; }
        return Action.KICK;
    }

    /** /pardon remains authoritative. Historical totals and audit records are preserved. */
    public void pardoned() { kicks = 0; stage = 0; banned = false; evidenceSeconds = 0; roundSeconds = 0; privateRound=0;publicRound=0; }
}
