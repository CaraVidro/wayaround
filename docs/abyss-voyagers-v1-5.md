# Abyss voyages

Implemented:
- Both craft obey water boundaries, fall in air and interpolate network movement. The submarine has inertia for thrust/steering.
- Exposure below 85 blocks of depth (submarine), or 70 (capsule), accumulates while loaded. Warning creaks/beeps/shakes start at 90 seconds; critical strain at 150 seconds; implosion at 180 seconds kills occupants and drops the submarine cargo once. Ascending to safe depth relieves exposure. Exposure survives save/reload; unloaded vehicles are not simulated.
- Real underwater dynamic light on both craft, smooth lamp pitch and two-tick aim updates. Old light sources persist eight ticks to allow propagation, and overlapping craft share leases. No decorative submarine lamps.
- Inventory key while aboard or Shift-right-click opens a persisted 27-slot cargo inventory. Shift-right-click with a stick packs an empty, unoccupied submarine; loaded cargo or pressure strain cannot be erased by packing.
- Motor/ambient hum, pressure warnings, bottom proximity beeps, collision sound, sediment and camera impulse. Side windows and a surface-waterline camera fix; upward abyss visibility is limited by black fog.
- The submarine's front viewport uses the same clear cutout glass and cabin lighting as its side ports. Kraken skin/eyes render before translucent water and vehicle panes, with depth testing retained for terrain and hull occlusion; foam and surface shadows retain their later pass. The capsule has no decorative glass floodlight cone; its articulated supports, lamps and real dynamic lighting remain.
- Kraken vertices subtract the interpolated camera position once and use the render event's explicit view matrix. The shader model-view is temporarily identity, then restored, avoiding camera-following geometry and duplicate camera rotation in either mesh pass.
- Wreck chests settle onto the rebuilt floor preserving loot tables and inventory. Legacy repair only moves unsupported unopened vanilla shipwreck/ruin containers, not player storage.
- Sparse seabed skulls and settled physical fish carcasses in newly generated trenches.
- Giant jellyfish growth retains its morph's minimum scale; no client scale correction loop.
- Player corpses float in water and lava. After ten seconds in lava, the model becomes a skeleton without deleting inventory. Synchronized owner UUID and saved signed texture properties let the client use the owner's actual skin even after disconnect/save/reload. Vanilla defaults are used while texture loading is pending or a profile has no skin data.
- Kraken movement/reveal sounds. `/kraken eyes` opens/blinks/retreats a pair of eyes at diving depth. `/kraken submarine` shakes a occupied submarine. Both can occur naturally in haunted abyss waters.
- Continental noise uses fourfold horizontal sampling for larger connected oceans and continents. The same input feeds climate and terrain; the existing abyss basin feature remains. Applies to newly generated terrain with living vegetation enabled, not retroactive terraforming.

Validation: pressure duration/recovery contracts; dedicated-server cargo/pressure persistence, dry capsule gravity, container conservation, corpse ownership/skeleton persistence; existing nature and Kraken animation tests; client startup/resources. Gameplay visuals, sound mix, waterline and geography require manual in-world inspection.

Viewport visual checks: compare the submarine's front and side windows from inside and outside, both submerged and at the waterline; view `/kraken eyes` while diving and a head/tentacle event at the surface; confirm hull/terrain still occlude the creature. In the capsule, confirm the twin supports and lamps remain, with illuminated blocks ahead and no blue glass beam. Repeat with Fast, Fancy and Fabulous graphics; these GPU composition checks are not covered by the startup smoke test.

During Kraken checks, turn the camera through a full circle and move sideways while the event runs. Eyes, tentacles, foam and shadows must keep their world anchors and animate there rather than rotate with the view.

Additional work: complete entry notice followed by optional voice settings, gradual regional ambient/fog transitions, accessory sockets outside vanilla crafting/storage, mushroom texture references repaired, water contact along 16 tentacle sections, and command-only remote deep-biome search without generating chunks. Survival recipe coverage is documented in survival-recipe-audit-v1-5.md.
