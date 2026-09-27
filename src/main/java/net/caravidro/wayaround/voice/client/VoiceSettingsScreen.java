package net.caravidro.wayaround.voice.client;

import java.util.List;

import net.caravidro.wayaround.voice.VoiceConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
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
    private Button modelButton;

    public VoiceSettingsScreen(
            Screen parent
    ) {
        super(
                Component.literal(
                        "Way Around Voice"
                )
        );

        this.parent =
                parent;
    }

    @Override
    protected void init() {
        super.init();

        reloadMicrophones();
        reloadOutputs();

        int buttonWidth =
                Math.min(
                        300,
                        this.width - 24
                );

        int buttonHeight =
                20;

        int x =
                (this.width - buttonWidth)
                        / 2;

        /*
         * Compact enough for the common 240px GUI height while leaving room
         * for the explicit model-download control.
         */
        int y =
                Math.max(
                        82,
                        this.height / 2 - 42
                );

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
                                        y + 24,
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

                                            VoiceDevices.InputDevice device =
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
                                        y + 48,
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
                                        y + 72,
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
                                        y + 96,
                                        buttonWidth,
                                        buttonHeight
                                )
                                .build()
                );

        modelButton =
                this.addRenderableWidget(
                        Button.builder(
                                        modelLabel(),
                                        button ->
                                                requestModelInstallConsent()
                                )
                                .bounds(
                                        x,
                                        y + 120,
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
                                y + 144,
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
                                button ->
                                        onClose()
                        )
                        .bounds(
                                x,
                                y + 168,
                                buttonWidth,
                                buttonHeight
                        )
                        .build()
        );
    }

    private void requestModelInstallConsent() {
        if (this.minecraft == null
                || VoskSpeechRecognizer.isPreparing()) {
            return;
        }

        if (VoskSpeechRecognizer.isModelInstalled()) {
            VoskSpeechRecognizer.warmUpAsync();
            return;
        }

        /*
         * Informed consent required by Modrinth:
         * - what: Portuguese Vosk speech model
         * - size: about 31 MB
         * - source: alphacephei.com
         * - destination: local Way Around config/model directory
         * - purpose: offline/local speech recognition
         *
         * No network request is issued before the player chooses Yes.
         */
        this.minecraft.setScreen(
                new ConfirmScreen(
                        accepted -> {
                            if (this.minecraft == null) {
                                return;
                            }

                            this.minecraft.setScreen(
                                    this
                            );

                            if (accepted) {
                                VoskSpeechRecognizer
                                        .installWithUserConsentAsync();
                            }
                        },
                        Component.literal(
                                "Download Way Around speech model?"
                        ),
                        Component.literal(
                                "Downloads about 31 MB from alphacephei.com and stores it locally for offline Portuguese speech recognition. No download occurs unless you choose Yes."
                        )
                )
        );
    }

    @Override
    public void tick() {
        super.tick();

        if (modelButton != null) {
            modelButton.setMessage(
                    modelLabel()
            );

            modelButton.active =
                    !VoskSpeechRecognizer.isPreparing();
        }
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

    private Component modelLabel() {
        if (VoskSpeechRecognizer.isPreparing()) {
            return Component.literal(
                    "Modelo de fala: BAIXANDO..."
            );
        }

        if (VoskSpeechRecognizer.isModelInstalled()) {
            return Component.literal(
                    "Modelo de fala: INSTALADO"
            );
        }

        return Component.literal(
                "Instalar modelo de fala (~31 MB)..."
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
                "Microfone: "
                        + name
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
                "Saida: "
                        + name
        );
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(
                    parent
            );
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
                16,
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
                32,
                0xA0A0A0
        );

        graphics.drawCenteredString(
                this.font,
                Component.literal(
                        VoskSpeechRecognizer
                                .statusText()
                ),
                this.width / 2,
                44,
                VoskSpeechRecognizer
                        .isModelInstalled()
                        ? 0x55FF55
                        : 0xFFAA55
        );

        graphics.drawCenteredString(
                this.font,
                Component.literal(
                        "Modelo Vosk opcional: nenhum arquivo e baixado automaticamente"
                ),
                this.width / 2,
                56,
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
                68,
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
