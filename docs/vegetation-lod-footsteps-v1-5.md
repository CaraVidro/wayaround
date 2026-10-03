# Vegetation, Kraken interpolation and material steps

## Implemented

- Native section frustum/occlusion culling still excludes invisible chunk meshes. Small roots, thin horizontal branch segments, ecological plants and aquatic floor organisms fully enclosed by six opaque neighbours are omitted when building the mesh.
- Section-centre distance chooses three detail bands: initially full detail below 48 blocks, core leaf cubes above 48, and no small plants above 80. Roots/thin branches disappear in the middle band. Main trunks, thick branches, normal leaf volume, apple leaves/fruit, collision and ecology persist.
- Hysteresis (40/56 and 72/88 blocks) prevents oscillating section rebuilds. Each client tick examines at most 64 tracked sections and refreshes at most 8. Tracking is bounded to 8192 entries; old worlds and distant sections are discarded. Camera rotation never causes rebuilds. Reapproaching restores details asynchronously through the same budget.
- Eight dense plant models retain at most six representative elements, reducing redundant blades while retaining their 3D silhouette. All 24 wood-segment collision shapes are reused instead of allocated per query.
- Kraken packets still arrive at the existing cadence. A client animation clock interpolates every frame, converges at at most 0.15 additional ticks per tick, and never rewinds the current scene. A new event resets interpolation. The near tentacle has 96 curve segments (formerly 64), with suction cup sampling kept near its original count; distant geometry stays at 36.
- Player footsteps retain the actual NeoForge material SoundType, cadence and sound settings. Walking gains 2.6x vanilla volume, sprinting 3.2x with slightly lower pitch, and crouching 1.25x. Per-step pitch varies by +/-3%; final volume caps at 0.85. No duplicate footstep sound or new sound entity is spawned.
- Client texture-bake warnings exposed invalid references: meadow sedge now uses hay_block_side; related missing salmon, quartz and clock item references use actual 1.21.1 texture names.

## Validation and limits

Standalone tests exercise delayed and forward packet corrections, monotonic per-frame movement, event reset and LOD boundary hysteresis. CI boots the client, requires both new mixins to be applied, rejects missing textures in all mod models and runs the existing geometry tests. The nature dedicated-server GameTests cover common mixin loading and retain ecological/mining regression coverage.

Actual camera visibility, appearance with third-party resource packs/shaders, perceived footstep volume and frame-rate improvement require in-world inspection. The foliage wrapper is restricted to the eight vanilla leaf blocks whose models this mod extends. Alternative renderers that bypass ModelBlockRenderer require their own compatibility bridge; no FPS gain is claimed without profiling.

These changes build on ecological history/open-pit fixes. Off-screen ecology is reconstructed on loading or advances in already loaded chunks, never by forcing the whole world to stay loaded.
