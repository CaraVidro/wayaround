# Adaptive storage and bounded water work

Base: `feature/organic-tree-architecture-v1-5`, commit `169cb202`.
No release version bump. Minecraft 1.21.1 / NeoForge 21.1.250.

## Implemented

### Packaging

- `build` / `assemble` now run `compactJar`: lossless DEFLATE level 9,
  byte-length/CRC verification, atomic replacement, and keep the original if
  recompression would make it larger. All normal resource and class paths remain.
- Exclude the unused `*.local.ogg` backup from processed resources. The existing
  `in_my_way.local.ogg` is byte-identical to `in_my_way.ogg` (4,713,136 bytes each).
  The tracked source backup is retained; no sound or image quality is reduced.
- `compactSourceArchive` creates a portable ZIP of source, resources, build files,
  wrapper, documentation and tools under `build/distributions`. Local audio
  backups and datagen caches are excluded. The editable checkout is untouched.
- Direct `jar` remains the normal Gradle JAR task; use `build`, `assemble` or
  `compactJar` for the additional verification/recompression pass.

A controlled local ZIP measurement of **src/main/resources only**, excluding
compiled classes, generated audio and jar-in-jar dependencies:

| Variant | Bytes |
| --- | ---: |
| Existing resources, DEFLATE 6 | 15,568,132 |
| Without duplicate local audio | 10,873,591 |

Reduction: 4,694,541 bytes (30.15%). Stronger recompression did not improve this
particular ZIP, so the optimizer correctly retained the smaller input. This is
not a measured final-mod-JAR percentage and not an FPS measurement.

### Voice model hibernation

The first adapter for `ColdDirectoryArchive` is the existing Vosk speech model.
It is an optional, immutable local cache with a real extracted disk footprint.

- A connected world with voice disabled no longer repeatedly tries to start
  microphone capture or prewarm STT. Local voice preferences are still respected.
- While STT is required, keep the model ready. After at least 2 minutes without
  demand **or decoder use**, release native model RAM if no decoder holds a lease.
- After 30 minutes idle, archive the extracted model once at DEFLATE 9. Maintenance
  runs on a daemon worker at most once per 1,200 client ticks, never in a render
  or server tick callback. Times count inactivity during this application session;
  they are not an offline calendar schedule and never force an active decoder out.
- Verify every archive entry with SHA-256 before atomically retiring the directory.
  Keep the original when savings are less than 4 KiB. A failed pack leaves the
  original or a verified committed archive available.
- Restore only when STT is requested: stage, bound extracted bytes/entry count,
  check checksums and expected model structure, then atomically commit. No network
  request is used to wake a cold model. The existing explicit download-consent
  path remains the only model download path.
- Native recognizers lease the model for their entire lifetime. A worker cannot
  close or archive it while a streaming or batch decoder is using it.
- Status text distinguishes a cold model from an uninstalled one. First use after
  hibernation can take longer, especially on a hard disk.
- Cold archives persist across restarts. Staging copies are never used as models;
  an interrupted retirement is cleaned after a successful restoration.

### Runtime work

- Surface discovery: at most **2,048 square-grid candidates per game tick**,
  traversed from the player outward. A radius-128 square previously traversed
  66,049 candidates synchronously (51,433 inside the circle). Discovery now takes
  at most 33 ticks under a stationary camera. Publish a complete replacement cache;
  keep the prior cache during refresh. Cosmetic water may appear after this delay.
- Turbulence discovery: at most **128 candidates per tick**, instead of scanning
  the full radius-22 grid at once. Complete traversal takes 16 ticks, followed by
  the existing refresh interval. Search also invalidates on vertical movement.
- Both scans skip unloaded chunks, discard stale work after large movement, clear
  on world changes/logout/feature disable, and release retained backing arrays.
  These budgets bound candidate count, not a guaranteed millisecond frame budget.
- The cosmetic surface overlay performs no discovery or drawing underwater.
  Vanilla water remains rendered. Bubbles and local turbulence still function.
- Hot feature checks read a precomputed effective bitmask. Dependency resolution
  happens when settings change. Raw selections, world NBT and network payloads
  remain unchanged; client/server snapshots stay independent.

## Scope and limits

There are no separate “realistic water” and “stylized water” asset modules in
this base: there is one `WATER_DYNAMICS` switch, with procedural geometry.
Procedural water caches are cheaper to discard and reconstruct than save to disk.

Registered blocks/items/entities and shared textures remain available for saved
worlds, creative inventory, recipes and resource packs. This change does not
unregister content or rewrite a loaded mod JAR. It does not compress world saves,
media recordings or arbitrary user files. Future cache adapters must explicitly
own their directory and exclude readers/writers for the entire archive operation.
Adding the utility alone does not automatically hibernate another system.

## Verification

- `storageRegressionTest`: lossless freeze/thaw, incompressible data, packing and
  extraction byte limits, validation rejection, interrupted retirement recovery,
  checksum corruption, path traversal, symlink rejection, and complete center-out
  scans for radii 0, 1, 2, 22, 24 and 128.
- Local tests use the actual Java implementation, without Minecraft or stubs.
- Full Gradle compilation was attempted but this environment cannot reach
  `services.gradle.org` to fetch the configured Gradle 9.7.1 wrapper. Java 21 and
  Minecraft dependencies are not installed locally. GitHub Actions build
  36910091424 subsequently passed the full build and checks with Java 21.
  Client/FPS/native Vosk integration still require a real game run; no performance
  gain in FPS is claimed.

Manual runtime scenarios: move/teleport/dive with water on, change dimensions,
reconnect with water off then on; compare visual continuity and frame-time spikes.
For voice, toggle voice off on a server, allow 2/30 minutes idle, then return to an
STT-enabled world. Check storage logs, exact restored model bytes, streaming and
batch recognition, and absence of unsolicited downloads. Test on Windows as well
as Linux because native model handles must close before renaming the directory.
