# Calving, chef clothing and lenses — implemented

Antarctic slab detachment now calls the structural demolition manager's connected,
bounded rigid partition and uses its client renderer. It no longer creates up to
180 FallingBlockEntity shell entities. Original block states, including the full
ice slab, move outward with shared analytical gravity and settle on the actual
terrain. Warning snow chips remain real individual falling blocks. Buildings
settle as their original blocks instead of becoming a flood of item drops;
container contents and block-entity NBT retain the existing one-owner capture,
empty-before-removal and restore-once path. Edits during the warning abort the
snapshot; source/destination chunk readiness still gates the event. Ordinary
server shutdown settles detached calving before saving; this does not add crash
recovery for transient in-progress events. No distant chunk is loaded by scans.

The chef set keeps its existing items/recipes and player bone attachment. The
shared worn/flying toque now has an open band, twelve pleats and eight crown
lobes. The double-breasted jacket adds side panels, placket, collar, neckerchief
tails and breast pocket. Dark gray trousers contrast with the white uniform;
the moving apron has a bib, shoulder straps, rear ties, pockets and stitched hem.
Existing wear stages remain visible.

Exposed nearby explosions (within ten blocks, in the explosion's affected entity
list) crack/shatter worn lenses and knock off the chef toque. Two short collision
rays allow solid walls to protect the equipment. Strong non-explosion hits and
occasional severe outdoor gusts can also detach the chef hat. Detached hats reuse
the flying top-hat entity, preserve the complete real stack and become one
recoverable item after landing; old engineer-hat saves retain a migration path.
Equipped accessories also preserve full stack components through transfer and
player cloning, so names/other metadata are not rebuilt away.

First-person lenses tint the scene in the same blue/violet colors as their
models. A cracked right lens appears at damage stage 1; both lenses have cracks
at stage 2. The fixed small crack mesh persists while worn, even after relog or
unequip/re-equip because damage belongs to the stack. Removing lenses clears
tint/cracks; lifting spectral glasses onto the head clears them too. The overlay
is drawn before HUD elements and does not obscure menus or third-person views.

Protocol **34** carries calving drift/motion in the shared collapse payload.
Server and clients need the same mod build. `/calving here` and
`/wayaroundcollapse test` retain their existing commands.

Validation: `tests/equipment/RigidFallMotionTest.java` checks every drop from 0
through 512 blocks against discrete gravity and fractional interpolation. CI
starts a real dedicated server with `-Dwayaround.validateCalvingEquipment=true`
for snapshot edits, exact block/chest conservation, bounded packet roundtrip,
exposed/sheltered impacts, hat save/recovery and durable lens state. An opt-in
client (`-Dwayaround.validateEquipmentVisuals=true`) renders the real overlay and
shared chef mesh into a GPU framebuffer and exports captures. Multiplayer
latency, shader packs and the full worn outfit still merit in-game visual QA.
