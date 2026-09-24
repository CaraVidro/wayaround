package net.caravidro.wayaround.voice.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.caravidro.wayaround.voice.VoiceConstants;

public final class VoiceToneAnalyzer {

    private VoiceToneAnalyzer() {
    }

    private static final int DOWNSAMPLE = 4;
    private static final double FRAME_SECONDS = 0.020;

    public record ToneProfile(
            double activeRms,
            double peakRms,
            double startPitch,
            double endPitch,
            double pitchRange,
            double holdSeconds,
            double intent,
            char punctuation,
            String tone
    ) {
    }

    public static ToneProfile analyze(
            byte[] pcm
    ) {
        if (pcm == null || pcm.length < 4) {
            return neutral();
        }

        short[] samples =
                decodeLittleEndian16(pcm);

        int frameSamples =
                Math.max(
                        64,
                        (int) (
                                VoiceConstants.SAMPLE_RATE
                                        * FRAME_SECONDS
                        )
                );

        List<Frame> frames =
                new ArrayList<>();

        double peakRms = 0.0;

        for (int start = 0;
             start + frameSamples <= samples.length;
             start += frameSamples) {

            double rms =
                    calculateRms(
                            samples,
                            start,
                            frameSamples
                    );

            peakRms =
                    Math.max(
                            peakRms,
                            rms
                    );

            frames.add(
                    new Frame(
                            rms,
                            0.0
                    )
            );
        }

        if (frames.isEmpty()) {
            return neutral();
        }

        List<Double> rmsValues =
                frames.stream()
                        .map(Frame::rms)
                        .sorted()
                        .toList();

        double noiseFloor =
                percentile(
                        rmsValues,
                        0.20
                );

        double activeThreshold =
                Math.max(
                        0.006,
                        noiseFloor * 2.25
                );

        List<Integer> activeIndices =
                new ArrayList<>();

        List<Double> activeRmsValues =
                new ArrayList<>();

        List<Double> activePitches =
                new ArrayList<>();

        for (int index = 0;
             index < frames.size();
             index++) {

            Frame frame =
                    frames.get(index);

            if (frame.rms()
                    < activeThreshold) {

                continue;
            }

            int sampleStart =
                    index * frameSamples;

            double pitch =
                    estimatePitch(
                            samples,
                            sampleStart,
                            frameSamples
                    );

            Frame updated =
                    new Frame(
                            frame.rms(),
                            pitch
                    );

            frames.set(
                    index,
                    updated
            );

            activeIndices.add(index);
            activeRmsValues.add(
                    frame.rms()
            );

            if (pitch > 0.0) {
                activePitches.add(
                        pitch
                );
            }
        }

        if (activeIndices.isEmpty()) {
            return neutral();
        }

        double activeRms =
                activeRmsValues.stream()
                        .mapToDouble(
                                Double::doubleValue
                        )
                        .average()
                        .orElse(0.0);

        List<Double> orderedPitches =
                new ArrayList<>(
                        activePitches
                );

        Collections.sort(
                orderedPitches
        );

        double pitchRange =
                orderedPitches.size() >= 4
                        ? percentile(
                                orderedPitches,
                                0.90
                        )
                        - percentile(
                                orderedPitches,
                                0.10
                        )
                        : 0.0;

        List<Double> sequencePitches =
                new ArrayList<>();

        for (int index : activeIndices) {
            double pitch =
                    frames.get(index)
                            .pitch();

            if (pitch > 0.0) {
                sequencePitches.add(
                        pitch
                );
            }
        }

        double startPitch =
                medianFraction(
                        sequencePitches,
                        0.0,
                        0.30
                );

        double endPitch =
                medianFraction(
                        sequencePitches,
                        0.70,
                        1.0
                );

        double holdSeconds =
                detectInitialHold(
                        frames,
                        activeIndices
                );

        double volumeScore =
                normalize(
                        activeRms,
                        0.025,
                        0.15
                );

        double peakScore =
                normalize(
                        peakRms,
                        0.06,
                        0.24
                );

        double rangeScore =
                normalize(
                        pitchRange,
                        30.0,
                        150.0
                );

        double endingDelta =
                startPitch > 0.0
                        && endPitch > 0.0
                        ? Math.abs(
                                endPitch
                                        - startPitch
                        )
                        : 0.0;

        double endingScore =
                normalize(
                        endingDelta,
                        10.0,
                        80.0
                );

        double intent =
                clamp01(
                        volumeScore * 0.30
                                + peakScore * 0.25
                                + rangeScore * 0.25
                                + endingScore * 0.20
                );

        boolean risingQuestion =
                startPitch > 0.0
                        && endPitch
                        > startPitch
                        + Math.max(
                                18.0,
                                startPitch * 0.12
                        );

        char punctuation;
        String tone;

        if (risingQuestion) {
            punctuation = '?';
            tone = "PERGUNTA";
            intent =
                    Math.max(
                            intent,
                            0.72
                    );

        } else if (intent >= 0.56
                || peakRms >= 0.17) {

            punctuation = '!';
            tone = "ENFATICO";

        } else {
            punctuation = '.';
            tone = "NEUTRO";
        }

        return new ToneProfile(
                activeRms,
                peakRms,
                startPitch,
                endPitch,
                pitchRange,
                holdSeconds,
                intent,
                punctuation,
                tone
        );
    }

    public static String applyExpression(
            String transcript,
            ToneProfile profile
    ) {
        if (transcript == null) {
            return "";
        }

        String cleaned =
                transcript.trim()
                        .replaceAll(
                                "[.!?]+$",
                                ""
                        );

        if (cleaned.isBlank()) {
            return "";
        }

        if (profile.holdSeconds()
                >= 0.36) {

            cleaned =
                    stretchFirstVowel(
                            cleaned,
                            profile.holdSeconds()
                    );
        }

        return cleaned
                + profile.punctuation();
    }

    private static String stretchFirstVowel(
            String text,
            double holdSeconds
    ) {
        int repeats =
                (int) Math.round(
                        holdSeconds / 0.10
                );

        repeats =
                Math.max(
                        3,
                        Math.min(
                                14,
                                repeats
                        )
                );

        for (int index = 0;
             index < text.length();
             index++) {

            char character =
                    text.charAt(index);

            if (!isVowel(character)) {
                continue;
            }

            StringBuilder builder =
                    new StringBuilder(
                            text.length()
                                    + repeats
                    );

            builder.append(
                    text,
                    0,
                    index
            );

            for (int i = 0;
                 i < repeats;
                 i++) {

                builder.append(
                        character
                );
            }

            builder.append(
                    text.substring(
                            index + 1
                    )
            );

            return builder.toString();
        }

        return text;
    }

    private static boolean isVowel(
            char character
    ) {
        return "aeiouAEIOU"
                .indexOf(character) >= 0
                || "áàâãäéèêëíìîïóòôõöúùûüÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜ"
                .indexOf(character) >= 0;
    }

    private static double detectInitialHold(
            List<Frame> frames,
            List<Integer> activeIndices
    ) {
        if (activeIndices.isEmpty()) {
            return 0.0;
        }

        int first =
                activeIndices.get(0);

        double baselinePitch = 0.0;
        int stableFrames = 0;
        int toleratedBreaks = 0;

        for (int index = first;
             index < frames.size();
             index++) {

            Frame frame =
                    frames.get(index);

            if (frame.pitch() <= 0.0) {
                if (stableFrames == 0) {
                    continue;
                }

                toleratedBreaks++;

                if (toleratedBreaks > 1) {
                    break;
                }

                continue;
            }

            if (baselinePitch <= 0.0) {
                baselinePitch =
                        frame.pitch();

                stableFrames++;
                continue;
            }

            double relativeDifference =
                    Math.abs(
                            frame.pitch()
                                    - baselinePitch
                    )
                            / baselinePitch;

            if (relativeDifference > 0.18) {
                toleratedBreaks++;

                if (toleratedBreaks > 1) {
                    break;
                }

                continue;
            }

            toleratedBreaks = 0;

            baselinePitch =
                    baselinePitch * 0.85
                            + frame.pitch() * 0.15;

            stableFrames++;
        }

        return stableFrames
                * FRAME_SECONDS;
    }

    private static double estimatePitch(
            short[] samples,
            int start,
            int length
    ) {
        int downsampledLength =
                length / DOWNSAMPLE;

        if (downsampledLength < 64) {
            return 0.0;
        }

        double[] values =
                new double[
                        downsampledLength
                        ];

        double mean = 0.0;

        for (int index = 0;
             index < downsampledLength;
             index++) {

            double value =
                    samples[
                            start
                                    + index
                                    * DOWNSAMPLE
                            ];

            values[index] = value;
            mean += value;
        }

        mean /=
                downsampledLength;

        double energy = 0.0;

        for (int index = 0;
             index < downsampledLength;
             index++) {

            values[index] -= mean;

            energy +=
                    values[index]
                            * values[index];
        }

        if (energy < 1.0) {
            return 0.0;
        }

        double sampleRate =
                VoiceConstants.SAMPLE_RATE
                        / DOWNSAMPLE;

        int minLag =
                Math.max(
                        1,
                        (int) (
                                sampleRate / 400.0
                        )
                );

        int maxLag =
                Math.min(
                        downsampledLength - 2,
                        (int) (
                                sampleRate / 75.0
                        )
                );

        double bestCorrelation =
                Double.NEGATIVE_INFINITY;

        int bestLag = -1;

        for (int lag = minLag;
             lag <= maxLag;
             lag++) {

            double dot = 0.0;
            double leftEnergy = 0.0;
            double rightEnergy = 0.0;

            for (int index = 0;
                 index + lag
                         < downsampledLength;
                 index++) {

                double left =
                        values[index];

                double right =
                        values[index + lag];

                dot += left * right;
                leftEnergy +=
                        left * left;
                rightEnergy +=
                        right * right;
            }

            double denominator =
                    Math.sqrt(
                            leftEnergy
                                    * rightEnergy
                    );

            if (denominator <= 0.0) {
                continue;
            }

            double correlation =
                    dot / denominator;

            if (correlation
                    > bestCorrelation) {

                bestCorrelation =
                        correlation;

                bestLag = lag;
            }
        }

        if (bestLag <= 0
                || bestCorrelation < 0.34) {

            return 0.0;
        }

        return sampleRate
                / bestLag;
    }

    private static short[] decodeLittleEndian16(
            byte[] pcm
    ) {
        short[] result =
                new short[
                        pcm.length / 2
                        ];

        for (int index = 0;
             index < result.length;
             index++) {

            int low =
                    pcm[index * 2]
                            & 0xFF;

            int high =
                    pcm[index * 2 + 1];

            result[index] =
                    (short) (
                            low
                                    | (high << 8)
                    );
        }

        return result;
    }

    private static double calculateRms(
            short[] samples,
            int start,
            int length
    ) {
        double sum = 0.0;

        for (int index = start;
             index < start + length;
             index++) {

            double normalized =
                    samples[index]
                            / 32768.0;

            sum +=
                    normalized
                            * normalized;
        }

        return Math.sqrt(
                sum / length
        );
    }

    private static double medianFraction(
            List<Double> values,
            double startFraction,
            double endFraction
    ) {
        if (values.isEmpty()) {
            return 0.0;
        }

        int from =
                Math.max(
                        0,
                        Math.min(
                                values.size() - 1,
                                (int) Math.floor(
                                        values.size()
                                                * startFraction
                                )
                        )
                );

        int to =
                Math.max(
                        from + 1,
                        Math.min(
                                values.size(),
                                (int) Math.ceil(
                                        values.size()
                                                * endFraction
                                )
                        )
                );

        List<Double> slice =
                new ArrayList<>(
                        values.subList(
                                from,
                                to
                        )
                );

        Collections.sort(
                slice
        );

        return percentile(
                slice,
                0.5
        );
    }

    private static double percentile(
            List<Double> sorted,
            double percentile
    ) {
        if (sorted.isEmpty()) {
            return 0.0;
        }

        double position =
                clamp01(percentile)
                        * (sorted.size() - 1);

        int lower =
                (int) Math.floor(
                        position
                );

        int upper =
                (int) Math.ceil(
                        position
                );

        if (lower == upper) {
            return sorted.get(lower);
        }

        double mix =
                position - lower;

        return sorted.get(lower)
                * (1.0 - mix)
                + sorted.get(upper)
                * mix;
    }

    private static double normalize(
            double value,
            double minimum,
            double maximum
    ) {
        if (maximum <= minimum) {
            return 0.0;
        }

        return clamp01(
                (value - minimum)
                        / (maximum - minimum)
        );
    }

    private static double clamp01(
            double value
    ) {
        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }

    private static ToneProfile neutral() {
        return new ToneProfile(
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                '.',
                "NEUTRO"
        );
    }

    private record Frame(
            double rms,
            double pitch
    ) {
    }
}
