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

    private int microphoneIndex;

    private Button enabledButton;
    private Button debugButton;
    private Button microphoneButton;

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

        this.addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        "Atualizar microfones"
                                ),
                                button -> {
                                    reloadMicrophones();

                                    microphoneButton
                                            .setMessage(
                                                    microphoneLabel()
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
                                        "Concluido"
                                ),
                                button -> onClose()
                        )
                        .bounds(
                                x,
                                y + 120,
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

    private Component debugLabel() {
        return Component.literal(
                "Debug de fala: "
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
                25,
                0xFFFFFF
        );

        graphics.drawCenteredString(
                this.font,
                Component.literal(
                        "Segure V para falar"
                ),
                this.width / 2,
                43,
                0xA0A0A0
        );

        graphics.drawCenteredString(
                this.font,
                Component.literal(
                        "Debug: texto local + volume + pitch + alongamento de vogal"
                ),
                this.width / 2,
                55,
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
                67,
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
