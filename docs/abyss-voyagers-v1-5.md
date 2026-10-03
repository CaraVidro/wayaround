# Abyss voyages

Implemented:
- Both craft obey water boundaries, fall in air and interpolate network movement. The submarine has inertia for thrust/steering.
- Exposure below 85 blocks of depth (submarine), or 70 (capsule), accumulates while loaded. Warning creaks/beeps/shakes start at 90 seconds; critical strain at 150 seconds; implosion at 180 seconds kills occupants and drops the submarine cargo once. Ascending to safe depth relieves exposure. Exposure survives save/reload; unloaded vehicles are not simulated.
- Real underwater dynamic light on both craft, smooth lamp pitch and two-tick aim updates. Old light sources persist eight ticks to allow propagation, and overlapping craft share leases. No decorative submarine lamps.
- Inventory key while aboard or Shift-right-click opens a persisted 27-slot cargo inventory. Shift-right-click with a stick packs an empty, unoccupied submarine; loaded cargo or pressure strain cannot be erased by packing.
- Motor/ambient hum, pressure warnings, bottom proximity beeps, collision sound, sediment and camera impulse. Side windows and a surface-waterline camera fix; upward abyss visibility is limited by black fog.
- Wreck chests settle onto the rebuilt floor preserving loot tables and inventory. Legacy repair only moves unsupported unopened vanilla shipwreck/ruin containers, not player storage.
- Sparse seabed skulls and settled physical fish carcasses in newly generated trenches.
- Giant jellyfish growth retains its morph's minimum scale; no client scale correction loop.
- Player corpses float in water and lava. After ten seconds in lava, the model becomes a skeleton without deleting inventory. Synchronized owner UUID and saved signed texture properties let the client use the owner's actual skin even after disconnect/save/reload. Vanilla defaults are used while texture loading is pending or a profile has no skin data.
- Kraken movement/reveal sounds. `/kraken eyes` opens/blinks/retreats a pair of eyes at diving depth. `/kraken submarine` shakes a occupied submarine. Both can occur naturally in haunted abyss waters.
- Continental noise uses fourfold horizontal sampling for larger connected oceans and continents. The same input feeds climate and terrain; the existing abyss basin feature remains. Applies to newly generated terrain with living vegetation enabled, not retroactive terraforming.

Validation: pressure duration/recovery contracts; dedicated-server cargo/pressure persistence, dry capsule gravity, container conservation, corpse ownership/skeleton persistence; existing nature and Kraken animation tests; client startup/resources. Gameplay visuals, sound mix, waterline and geography require manual in-world inspection.

Additional work: complete entry notice followed by optional voice settings, gradual regional ambient/fog transitions, accessory sockets outside vanilla crafting/storage, mushroom texture references repaired, water contact along 16 tentacle sections, and command-only remote deep-biome search without generating chunks. Survival recipe coverage is documented in survival-recipe-audit-v1-5.md.
