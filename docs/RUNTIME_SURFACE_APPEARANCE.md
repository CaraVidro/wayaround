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
  `/wayappearance rust 80` (requires permission level 2). The operator change is pushed to nearby viewers immediately, including zero to clear old rust; the existing coarse sync remains for natural aging.

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
respective visuals. Local precipitation uses the procedural cloud weather field even when vanilla global rain is clear.

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
then run `/wayappearance rust 80`; expect visible brown rust patches on exposed faces immediately. Repeat with `rust 0` to remove them.
For global vanilla rain, use `/weather rain` over supported stone/concrete to observe puddles and `/weather clear` to see them evaporate. For local weather, aim at any visible WayAround cloud and use `/raincloud` (or `/cloudstorm rain`). The chosen cloud turns into a storm for 5 minutes and produces localized raindrop particles and puddles under its footprint; `/raincloud clear` removes its temporary override. The same field is used on dedicated servers and synced to clients. Both `Procedural Clouds` and `Living Weather` must be enabled.

Disable `Procedural Clouds` in your world configuration to see vanilla clouds again. The old Overworld cloud mixin cancelled vanilla rendering unconditionally; it now cancels only when the replacement renderer is actually enabled. Disable `Time & Aging` to hide all rust overlays; the rust command now explicitly warns when this toggle is off. Test Tukuna possession and swimming /
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

## Diagnostics and boundaries

- If `/wayappearance rust 80` does not change the iron block, check that `Time & Aging` is ON, cheats/op permission is level 2, the block is within eight blocks, and you are running this feature branch (not the older main build).
- The original implementation sent rust at most once per 200 ticks, had tiny sparsely scattered pixels, and the sky mixin did not honor the clouds toggle. All three were corrected in this follow-up.
- These are rendered decals over the source block, **not mutations of the PNG atlas**. Rust is persisted; raincloud overrides and puddles are temporary; water/metal block types remain unchanged.
- Worldgen changes already baked into saved chunks generally cannot be reverted by toggling a feature off; the vanilla fallback rule here covers live rendering and local weather behavior.


## Pixel-art puddle revamp (2026-10-09)

- Puddles are now rendered as deterministic **32-column, 24-row painted silhouettes** rather than two rectangles: a staggered nonlinear outline, a darker rim, layered translucent water, occasional small reflective streaks and subtle animated rain rings. Interior spans form one connected puddle; no chunky individual pixel sprites.
- Rain sampling now checks a 7×7 area around the player every 8 client ticks, the directly looked-at surface and 16 additional bounded random columns. Before this, 24 attempts in a 45×45 area could miss a small floor for a long time.
- Full grass/dirt, sand, gravel, bricks, mud, deepslate and other solid floors now qualify, in addition to iron and paving.
- `/wayappearance puddle`: look at an uncovered eligible solid floor within 8 blocks, then run the operator command to force a 30-second test puddle even in clear weather. `/wayappearance puddle clear` removes the test puddle under your crosshair. Requires **Living Weather** enabled, but not `/weather rain`. This test is intentionally visual-only and client-local after server authorization.
- Both natural and debug puddles are transient: they never place water blocks or modify collision. Runtime appearance is one bounded client buffer pass, with no new textures uploaded.


## World-space rotated puddles — multi-block continuation

This replaces the earlier 32-column single-block stamped puddles with a proper
world-space **implicit field**, sampled by adjacent block faces. Each puddle
anchor has its own full-circle angle, dimensions, local offset, and two
deterministic octaves of warped value noise, inspired by Blender's Noise Texture
and randomized coordinates. The **same absolute world X/Z** is evaluated on
both sides of a block edge, so the decal is continuous across that edge.

The field uses signed threshold contours, and the renderer clips each cell at
the interpolated wet/dry boundary instead of painting one rectangular tile per
block. Two staggered depth offsets produce a dark edge and semitransparent
water; sparse tinted highlights are confined to the interior. Decal growth
depends on the puddle's wetness.

**Solid-neighbor rule:** may extend onto immediate cardinal and diagonal
neighbors *only* when the ground remains a solid supported block at the same
Y, with air above. Diagonal spill additionally requires both side supports.
No decal extends into a missing/air block, climbs walls, or triggers unloaded
chunks. Geometry does not edit vanilla models or texture atlases.

To protect performance there are at most 88 active anchors and 28 puddles
drawn per frame, with sparse deterministic natural anchors. Debug puddles
get rendering priority.

Test: use `/wayappearance puddle` aiming at one of the ground blocks in a
3×3 level paved area, then inspect whether the silhouette rotates, changes
size and continues over adjacent blocks. `/wayappearance puddle clear` erases
the forced anchor. Confirm that creating a hole in a neighbor stops extension
onto that cell. Also test rain spawning and evaporation. The geometry is
client-local and collision-free.

Research:
- Blender 4.5 Noise Texture (fBm, distortion, texture-coordinate rotation):
  https://docs.blender.org/manual/en/4.5/compositing/types/texture/noise.html
- NVIDIA GPU Gems world-space terrain texture coordinate continuity:
  https://developer.nvidia.com/gpugems/gpugems3/part-i-geometry/chapter-1-generating-complex-procedural-terrains-using-gpu
- Minecraft Puddles & Floods, connected multi-block puddles:
  https://github.com/PigCart/puddles-floods
- NeoForge 1.21.1 render stage: AFTER_PARTICLES handles translucency
  more safely than AFTER_TRANSLUCENT_BLOCKS with fabulous target:
  https://lexxie.dev/neoforge/1.21.1/net/neoforged/neoforge/client/event/RenderLevelStageEvent.Stage.html
