# Runtime surface appearance — WayAround 1.6

## What this adds

WayAround already has `TemporalState` and `TemporalAgingData`. This patch **extends**
that state to drive live visual overlays, rather than introducing a second aging
database or rewriting resource-pack PNGs mid-game.

- **Tukuna:** the existing per-pixel tattoo and speech-mouth renderer now runs
  in a `RenderLayer` attached to the animated vanilla player bones. The
  receptacle/possession skin system and authoritative Tukuna packets are reused.
- **Iron and raw iron blocks:** persistent temporal corrosion is quantized to one
  byte and sent only for previously sampled blocks within 32 blocks of a player.
  The client draws deterministic, pixel-like rust flecks on visible block faces.
- **Rain puddles:** sampled solid ground receives local translucent patches with
  gradual fade after the rain. Puddles are visual-only (no fluid or collision).
- **Operator tool:** look at an iron/raw-iron block and enter
  `/wayappearance rust 80` (requires permission level 2). The new persistent
  strength reaches viewers on the next coarse aging sync (~10 seconds).

## Architecture

`TemporalAgingData` (server SavedData)
→ `TimeAgingEngine` (existing coarse sampler)
→ `SurfaceAppearanceS2CPayload` (at most 96 [pos, byte] entries per player, every 200 ticks)
→ `SurfaceAppearanceClientCache` (1024-entry bounded mirror, common-safe)
→ `SurfaceAppearanceRenderer` (one batched POSITION_COLOR draw).

The client does not modify original texture files, force block-model rebuilds,
spawn decal entities, or load a missing chunk. Marks use coordinate hashes so
they do not flicker between frames. Only close-by samples are rendered.

Temporary rain puddles are client-local; persistent rust is server-owned.
The `Time & Aging` and `Living Weather` world feature toggles control the
respective visuals.

## How to test on Windows 11 / VS Code

Use **JDK 21** and an up-to-date checkout of the feature branch. In the integrated
PowerShell terminal:

```powershell
git fetch origin
git switch feature/runtime-surface-appearance-v1-6
git pull --ff-only origin feature/runtime-surface-appearance-v1-6
.\gradlew.bat runClient
```

In a creative singleplayer world with cheats, place an iron block, aim at it,
then run `/wayappearance rust 80`; expect irregular brown flecks on exposed
faces after the next aging-sync pass. Repeat with `rust 0` to remove them.
Use `/weather rain` over supported stone/concrete to observe puddles and
`/weather clear` to see them evaporate. Test Tukuna possession and swimming /
fall-flying to check bone-attached marks.

For the dedicated-server boundary and compile smoke:

```powershell
.\gradlew.bat compileJava verifyDedicatedCommonBoundary verifyDedicatedSafeNetwork
```

## Scope / future work

This is a foundational **render overlay system**, not arbitrary pixel mutation
of downloadable resource-pack textures. Present materials are intentionally
limited to common iron and several full-block paved surfaces. Future adapters
can sample `MaterialMemory` of industrial parts and provide damaged metal,
scratches, soot, moss, painted decals and user-authored texture masks.

Puddles are not persisted, because only their short-lived appearance is
simulated; they do not currently change friction or pressure. Third-person
Tukuna tattoos are bone attached. Separate first-person tattoo rendering still
requires an arm-specific hook before it can be called complete.

Keep per-frame CPU and GPU limits in mind when extending this: do not allocate
new `DynamicTexture` objects per block or update the entire texture atlas every
world tick.
