package net.caravidro.wayaround.media.client;

public record RecordedAmbientSound(
        long timeMillis,
        String soundId,
        String source,
        float volume,
        float pitch
) {
}
