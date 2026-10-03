package net.caravidro.wayaround.voice;

/** Distance falloff for signed 16-bit little-endian microphone frames. */
public final class VoiceDistanceMath {
    private VoiceDistanceMath() {}

    public static double gain(double distanceSquared) {
        if (!Double.isFinite(distanceSquared) || distanceSquared < 0) return 0;
        double distance = Math.sqrt(distanceSquared);
        if (distance <= 3) return 1;
        double t = Math.min(1, (distance - 3) / (VoiceConstants.HEARING_RANGE_BLOCKS - 3));
        return 1 - t * t * (3 - 2 * t);
    }

    public static byte[] attenuate(byte[] pcm, double gain) {
        double volume = Double.isFinite(gain) ? Math.max(0, Math.min(1, gain)) : 0;
        if (volume == 1) return pcm;
        byte[] output = new byte[pcm.length];
        for (int i = 0; i + 1 < pcm.length; i += 2) {
            int sample = (short)((pcm[i] & 255) | (pcm[i + 1] << 8));
            int quiet = (int)Math.round(sample * volume);
            output[i] = (byte)quiet;
            output[i + 1] = (byte)(quiet >> 8);
        }
        return output;
    }
}
