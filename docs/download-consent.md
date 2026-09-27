# Download consent

Way Around does not initiate optional file downloads without informed user consent.

## Vosk speech model

The optional Portuguese Vosk model is used only for local/offline speech recognition.

- It is **not** downloaded during startup, world join, voice-chat startup, warmup, or recognition.
- If the model is absent, speech recognition reports that the model is not installed.
- The player must open **Way Around Voice** and select **Install speech model (~31 MB)**.
- Before any network request is made, Minecraft shows a confirmation dialog containing:
  - the approximate download size;
  - the source domain (`alphacephei.com`);
  - the purpose (offline Portuguese speech recognition);
  - the fact that no download occurs unless the player chooses **Yes**.
- The network download method is private and is only reached from
  `VoskSpeechRecognizer.installWithUserConsentAsync()`.

## In-game VHS / TV recordings

Recordings may be transferred from the current Minecraft server so a VHS/TV recording can be played locally.

This transfer also requires consent:

1. The client requests **metadata only**.
2. The server replies with the recording ID and exact byte size.
3. The client shows a confirmation dialog explaining:
   - the exact size;
   - that the source is the current Minecraft server;
   - that the file will be stored in `wayaround-recordings`;
   - that it is needed to play the requested in-game media.
4. Only after the player chooses **Yes** does the client send
   `MediaRecordingApproveC2SPayload`.
5. The server starts sending file chunks only after that approval.
6. The client additionally rejects all incoming recording chunks unless the
   exact recording ID and exact advertised size are present in its local
   approved-download map.

Declining a recording does not create a file.
