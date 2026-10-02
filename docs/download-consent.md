# Download consent

Way Around does not initiate optional file downloads without informed user consent.

This is enforced at runtime boundaries rather than only documented as a caller convention.

## Vosk speech model

The optional Portuguese Vosk model is used only for local/offline speech recognition.

- It is **not** downloaded during startup, world join, voice-chat startup, warmup, recognition, or cold-storage restore.
- If the model is absent, speech recognition reports that the model is not installed.
- The player must open **Way Around Voice** and select the speech-model install control.
- The only public install entry point opens the consent screen itself. The method that starts HTTP is private.
- Before any network request is made, Minecraft shows:
  - what will be downloaded: the Portuguese Vosk model;
  - approximate size: about 31 MB;
  - exact source: `https://alphacephei.com`;
  - purpose: offline Portuguese speech recognition;
  - destination: `config/wayaround/voice-models/vosk-model-small-pt-0.3`;
  - an explicit statement that choosing **No** starts no network request.
- The HTTP client does not follow redirects. A different host cannot silently replace the disclosed source.
- The response body is streamed through a byte limit before disk growth. `BodyHandlers.ofFile` is intentionally not used.
- ZIP extraction keeps path traversal and expanded-size limits.
- Cold-storage hibernation/restoration is entirely local and never re-downloads the model.

## In-game VHS / TV recordings

Recordings may be transferred from the current Minecraft server so a VHS/TV recording can be played locally.

The transfer is a two-phase consent protocol:

1. The client requests **metadata only** for a recording it actually wants to play.
2. The server creates a short-lived one-time offer containing:
   - recording ID;
   - exact byte size;
   - random non-zero offer token.
3. The client ignores unsolicited offers that do not correspond to a recent local request.
4. Minecraft shows a confirmation dialog explaining:
   - the exact size;
   - that the source is the current Minecraft server;
   - that the destination is `wayaround-recordings`;
   - that the purpose is playback of the requested VHS/TV media;
   - that no file data is downloaded unless the player chooses **Yes**.
5. On **Yes**, the client creates a short-lived exact consent grant and echoes the same ID, size and token to the server.
6. The server consumes the pending offer exactly once and starts transfer only if ID, size, token and expiry all match and the file still has the approved size.
7. Every data chunk carries the same offer token.
8. The client rejects every chunk unless ID, size, token and local approval expiry still match.
9. Only after those checks can a temporary `.download` file be created/written.
10. Declining creates no file.

This prevents a stale approval from being reused for another recording, another size, or another transfer attempt.

## Mod updates

Way Around does not contain an automatic mod-JAR updater.

The NeoForge metadata keeps `updateJSONURL` disabled. The in-game **UPDATE LOG** is local release text only and does not contact a server or download files.

If an updater is introduced later, it must use a new informed-consent flow before any update metadata/file transfer is enabled.

## CI safety tripwire

`downloadConsentRegressionTest` is part of `check`.

It verifies the exact consent-grant contract and scans runtime Java sources for direct network/download APIs such as:

- `HttpClient` / `HttpRequest`;
- `java.net.URL` / `URLConnection`;
- `openConnection` / `openStream`;
- direct file body handlers.

The reviewed Vosk class is currently the only permitted direct Internet download sink. The test also verifies:

- its public API is the consent-screen entry point;
- its HTTP worker is private;
- redirects are disabled;
- downloads are bounded before disk writes;
- NeoForge automatic update metadata remains disabled.

A newly-added runtime downloader therefore fails CI until it receives an explicit reviewed consent path.
