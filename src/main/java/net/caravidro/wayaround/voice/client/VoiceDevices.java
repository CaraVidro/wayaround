package net.caravidro.wayaround.voice.client;

import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import net.caravidro.wayaround.voice.VoiceConstants;

public final class VoiceDevices {

    private VoiceDevices() {
    }

    public record InputDevice(
            String id,
            String displayName,
            Mixer.Info mixerInfo
    ) {
    }

    public record OutputDevice(
            String id,
            String displayName,
            Mixer.Info mixerInfo
    ) {
    }

    public static List<InputDevice> listInputs() {
        List<InputDevice> devices =
                new ArrayList<>();

        devices.add(
                new InputDevice(
                        "",
                        "Padrao do sistema",
                        null
                )
        );

        DataLine.Info wanted =
                new DataLine.Info(
                        TargetDataLine.class,
                        VoiceConstants.audioFormat()
                );

        for (Mixer.Info info
                : AudioSystem.getMixerInfo()) {

            try {
                Mixer mixer =
                        AudioSystem.getMixer(info);

                if (!mixer.isLineSupported(wanted)) {
                    continue;
                }

                devices.add(
                        new InputDevice(
                                buildId(info),
                                displayName(
                                        info,
                                        "Microfone sem nome"
                                ),
                                info
                        )
                );

            } catch (Exception ignored) {
            }
        }

        return devices;
    }

    public static List<OutputDevice> listOutputs() {
        List<OutputDevice> devices =
                new ArrayList<>();

        devices.add(
                new OutputDevice(
                        "",
                        "Padrao do sistema",
                        null
                )
        );

        DataLine.Info wanted =
                new DataLine.Info(
                        SourceDataLine.class,
                        VoiceConstants.audioFormat()
                );

        for (Mixer.Info info
                : AudioSystem.getMixerInfo()) {

            try {
                Mixer mixer =
                        AudioSystem.getMixer(info);

                if (!mixer.isLineSupported(wanted)) {
                    continue;
                }

                devices.add(
                        new OutputDevice(
                                buildId(info),
                                displayName(
                                        info,
                                        "Saida sem nome"
                                ),
                                info
                        )
                );

            } catch (Exception ignored) {
            }
        }

        return devices;
    }

    public static InputDevice selectedOrDefault() {
        List<InputDevice> devices =
                listInputs();

        String selectedId =
                VoiceConfig.getMicrophoneId();

        for (InputDevice device : devices) {
            if (device.id()
                    .equals(selectedId)) {

                return device;
            }
        }

        return devices.get(0);
    }

    public static OutputDevice selectedOutputOrDefault() {
        List<OutputDevice> devices =
                listOutputs();

        String selectedId =
                VoiceConfig.getSpeakerId();

        for (OutputDevice device : devices) {
            if (device.id()
                    .equals(selectedId)) {

                return device;
            }
        }

        return devices.get(0);
    }

    public static int indexOfSelected(
            List<InputDevice> devices
    ) {
        String selectedId =
                VoiceConfig.getMicrophoneId();

        for (int index = 0;
             index < devices.size();
             index++) {

            if (devices.get(index)
                    .id()
                    .equals(selectedId)) {

                return index;
            }
        }

        return 0;
    }

    public static int indexOfSelectedOutput(
            List<OutputDevice> devices
    ) {
        String selectedId =
                VoiceConfig.getSpeakerId();

        for (int index = 0;
             index < devices.size();
             index++) {

            if (devices.get(index)
                    .id()
                    .equals(selectedId)) {

                return index;
            }
        }

        return 0;
    }

    private static String buildId(
            Mixer.Info info
    ) {
        return safe(info.getName())
                + "|"
                + safe(info.getVendor())
                + "|"
                + safe(info.getVersion());
    }

    private static String displayName(
            Mixer.Info info,
            String fallback
    ) {
        String name =
                info.getName();

        if (name == null
                || name.isBlank()) {

            return fallback;
        }

        return name;
    }

    private static String safe(
            String text
    ) {
        return text == null
                ? ""
                : text;
    }
}
