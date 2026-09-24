package net.caravidro.wayaround.voice.client;

import java.util.List;

import net.caravidro.wayaround.voice.VoiceConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class VoiceSettingsScreen
        extends Screen {

    private final Screen parent;

    private List<VoiceDevices.InputDevice>
            microphones;

    private List<VoiceDevices.OutputDevice>
            outputs;

    private int microphoneIndex;
    private int outputIndex;

    private Button enabledButton;
    private Button modeButton;
    private Button debugButton;
    private Button microphoneButton;
    private Button outputButton;

    public VoiceSettingsScreen(
            Screen parent
    ) {
        super(
                Component.literal(
                        "Way Around Voice"
                )
        );

        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        reloadMicrophones();
        reloadOutputs();

        int buttonWidth = 260;
        int buttonHeight = 20;
        int x =
                (this.width - buttonWidth) / 2;
        int y =
                this.height / 2 - 68;

        enabledButton =
                this.addRenderableWidget(
                        Button.builder(
                                        enabledLabel(),
                                        button -> {
                                            VoiceConfig.setEnabled(
                                                    !VoiceConfig.isEnabled()
                                            );

                                            if (!VoiceConfig.isEnabled()) {
                                                VoiceCapture.stop();
                                            }

                                            button.setMessage(
                                                    enabledLabel()
                                            );
                                        }
                                )
                                .bounds(
                                        x,
                                        y,
                                        buttonWidth,
                                        buttonHeight
                                )
                                .build()
                );

        modeButton =
                this.addRenderableWidget(
                        Button.builder(
                                        modeLabel(),
                                        button -> {
                                            VoiceConfig.setActivationMode(
                                                    VoiceConfig
                                                            .getActivationMode()
                                                            .next()
                                            );

                                            VoiceCapture.stop();

                                            button.setMessage(
                                                    modeLabel()
                                            );
                                        }
                                )
                                .bounds(
                                        x,
                                        y + 104,
                                        buttonWidth,
                                        buttonHeight
                                )
                                .build()
                );

        debugButton =
                this.addRenderableWidget(
                        Button.builder(
                                        debugLabel(),
                                        button -> {
                                            VoiceConfig
                                                    .setDebugSpeechEnabled(
                                                            !VoiceConfig
                                                                    .isDebugSpeechEnabled()
                                                    );

                                            button.setMessage(
                                                    debugLabel()
                                            );
                                        }
                                )
                                .bounds(
                                        x,
                                        y + 26,
                                        buttonWidth,
                                        buttonHeight
                                )
                                .build()
                );

        microphoneButton =
                this.addRenderableWidget(
                        Button.builder(
                                        microphoneLabel(),
                                        button -> {
                                            if (microphones.isEmpty()) {
                                                return;
                                            }

                                            microphoneIndex =
                                                    (microphoneIndex + 1)
                                                            % microphones.size();

                                            VoiceDevices.InputDevice
                                                    device =
                                                    microphones.get(
                                                            microphoneIndex
                                                    );

                                            VoiceConfig.setMicrophoneId(
                                                    device.id()
                                            );

                                            VoiceCapture.stop();

                                            button.setMessage(
                                                    microphoneLabel()
                                            );
                                        }
                                )
                                .bounds(
                                        x,
                                        y + 52,
                                        buttonWidth,
                                        buttonHeight
                                )
                                .build()
                );

        outputButton =
                this.addRenderableWidget(
                        Button.builder(
                                        outputLabel(),
                                        button -> {
                                            if (outputs.isEmpty()) {
                                                return;
                                            }

                                            outputIndex =
                                                    (outputIndex + 1)
                                                            % outputs.size();

                                            VoiceDevices.OutputDevice device =
                                                    outputs.get(
                                                            outputIndex
                                                    );

                                            VoiceConfig.setSpeakerId(
                                                    device.id()
                                            );

                                            VoicePlayback.restartOutput();

                                            button.setMessage(
                                                    outputLabel()
                                            );
                                        }
                                )
                                .bounds(
                                        x,
                                        y + 78,
                                        buttonWidth,
                                        buttonHeight
                                )
                                .build()
                );

        this.addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        "Atualizar dispositivos"
                                ),
                                button -> {
                                    reloadMicrophones();
                                    reloadOutputs();

                                    microphoneButton
                                            .setMessage(
                                                    microphoneLabel()
                                            );

                                    outputButton
                                            .setMessage(
                                                    outputLabel()
                                            );
                                }
                        )
                        .bounds(
                                x,
                                y + 130,
                                buttonWidth,
                                buttonHeight
                        )
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        "Concluido"
                                ),
                                button -> onClose()
                        )
                        .bounds(
                                x,
                                y + 164,
                                buttonWidth,
                                buttonHeight
                        )
                        .build()
        );
    }

    private void reloadMicrophones() {
        microphones =
                VoiceDevices.listInputs();

        microphoneIndex =
                VoiceDevices.indexOfSelected(
                        microphones
                );
    }

    private void reloadOutputs() {
        outputs =
                VoiceDevices.listOutputs();

        outputIndex =
                VoiceDevices.indexOfSelectedOutput(
                        outputs
                );
    }

    private Component enabledLabel() {
        return Component.literal(
                "Voice Chat: "
                        + (
                        VoiceConfig.isEnabled()
                                ? "LIGADO"
                                : "DESLIGADO"
                )
        );
    }

    private Component modeLabel() {
        return Component.literal(
                "Modo: "
                        + (
                        VoiceConfig.isVoiceActivation()
                                ? "VOICE ACTIVATION"
                                : "PUSH TO TALK"
                )
        );
    }

    private Component debugLabel() {
        return Component.literal(
                "Debug de fala local: "
                        + (
                        VoiceConfig.isDebugSpeechEnabled()
                                ? "LIGADO"
                                : "DESLIGADO"
                )
        );
    }

    private Component microphoneLabel() {
        if (microphones == null
                || microphones.isEmpty()) {

            return Component.literal(
                    "Microfone: nenhum encontrado"
            );
        }

        String name =
                microphones.get(
                        microphoneIndex
                ).displayName();

        if (name.length() > 34) {
            name =
                    name.substring(
                            0,
                            31
                    )
                            + "...";
        }

        return Component.literal(
                "Microfone: " + name
        );
    }

    private Component outputLabel() {
        if (outputs == null
                || outputs.isEmpty()) {

            return Component.literal(
                    "Saida: nenhuma encontrada"
            );
        }

        String name =
                outputs.get(
                        outputIndex
                ).displayName();

        if (name.length() > 34) {
            name =
                    name.substring(
                            0,
                            31
                    )
                            + "...";
        }

        return Component.literal(
                "Saida: " + name
        );
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        this.renderBackground(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );

        graphics.drawCenteredString(
                this.font,
                this.title,
                this.width / 2,
                20,
                0xFFFFFF
        );

        graphics.drawCenteredString(
                this.font,
                Component.literal(
                        VoiceConfig.isVoiceActivation()
                                ? "Voice Activation: fala detectada automaticamente"
                                : "Push to Talk: segure V para falar"
                ),
                this.width / 2,
                38,
                0xA0A0A0
        );

        graphics.drawCenteredString(
                this.font,
                Component.literal(
                        VoskSpeechRecognizer
                                .statusText()
                ),
                this.width / 2,
                50,
                VoskSpeechRecognizer
                        .isModelInstalled()
                        ? 0x55FF55
                        : 0xFFAA55
        );

        graphics.drawCenteredString(
                this.font,
                Component.literal(
                        "Vosk offline: palavras | Way Around: pitch, intensidade e alongamento"
                ),
                this.width / 2,
                62,
                0x777777
        );

        graphics.drawCenteredString(
                this.font,
                Component.literal(
                        "V0 - mono "
                                + (int) VoiceConstants.SAMPLE_RATE
                                + " Hz - alcance de "
                                + (int) VoiceConstants.HEARING_RANGE_BLOCKS
                                + " blocos"
                ),
                this.width / 2,
                74,
                0x777777
        );

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );
    }
}
