# v1.2.1 — THE WORLD CONNECTS

Status: **development preview**

Current published/dev baseline remains **v1.2.0 — Grande Novo Mundo** until this release is explicitly finished.

## Release identity

**THE WORLD CONNECTS** is the update where previously separate WayAround systems begin behaving as one world.

The release should emphasize connections rather than raw item count:

- Assembly parts affect machines;
- machines create and consume shared mechanical load;
- materials and condition affect performance;
- pipework creates hydraulic demand;
- failures create physical consequences;
- future steam and electrical systems can reuse the same infrastructure;
- media/history systems can record what those systems cause.

## Milestone 1.3 — Physical Industry

Implemented in the development branch:

- shared `MechanicalLoad` model;
- universal `MechanicalLoad.OperatingPoint` contract for rotational consumers;
- pump, crusher, sawmill, mill, fan and press now resolve power/torque/RPM through the same mechanical language;
- shared-source budget regression coverage prevents multiple consumers from duplicating available power;
- power fulfillment;
- torque starvation;
- safe-RPM / overspeed evaluation;
- combined failure-stress evaluation;
- persistent `MechanicalFailure` states shared by transmission hardware;
- shafts accumulate deformation instead of disappearing;
- exposed gears accumulate visible tooth loss;
- gearboxes accumulate bearing damage, friction and wobble;
- critical transmission hardware remains in-world as a seized component until dismantled;
- pulley belts now slip progressively, losing RPM/power before near-ruined belts can snap;
- failure state persists through saves/reloads and feeds transmission efficiency;
- shaft/gearbox stress integration;
- pulley throughput limits and overload wear;
- mechanical pump torque demand from pressure/flow;
- shared `HydraulicLoad` model connecting flow, pressure, torque, cavitation and backpressure;
- closed/restricted discharge now raises backpressure and mechanical demand instead of merely producing zero flow;
- suction starvation creates cavitation, reducing useful flow while increasing vibration and impeller wear;
- pump pressure propagates through the bounded PipeFlow route and can damage pipe sections above their pressure rating;
- pipe current/peak hydraulic pressure persists for world consequences and diagnostics;
- hydraulic regression coverage protects pressure-rating, cavitation and backpressure behavior;
- rotary water-lift heads added for Small Copper, Iron Water, Steel Pressure and Large Water Main families;
- narrow rotary lift heads propagate visual water/terminal spray without draining or creating liquid volume;
- Rotary Large Water Lift conserves real 1000 mB source blocks through large-pipe/duct routes;
- rotary heads consume shared mechanical power while downstream pipes remain passive bounded conduits;
- Windows storage regression no longer fails merely because the host cannot create a test symbolic link; capable hosts still execute the security rejection check;
- crusher demand integration;
- sawmill shared load math;
- GameTest coverage for power vs torque vs speed;
- title-screen Update Log.

## Milestone 1.4 — Materials Have Memory + Electronics Seed

Implemented in the development branch:

- persistent material memory for load cycles, thermal cycles, heat damage, corrosion, deformation and cumulative fatigue;
- common material conductivity, heat tolerance, corrosion resistance, ductility, strength, hardness, fatigue endurance, vibration damping and friction traits;
- material personality now changes shaft yielding, gear-tooth damage, bearing heat/damage, cyclic wear, effective load capacity and safe RPM;
- material history integrated into shafts, exposed gears, gearboxes, pulley wheels/belts, pump impellers, crushers, mills and sawmills;
- transmission models now reflect installed Assembly material and expose qualitative corrosion/heat/fatigue scar cues;
- failing transmission hardware uses material-aware sound/particle feedback instead of generic identical clunks;
- maintenance can improve repairable condition but cannot erase deep fatigue history;
- deterministic regression coverage compares material personalities and verifies memory persistence/service limits;
- user-built 6×4 circuit boards with physical component cells and explicit copper traces;
- input/output terminals, resistors, capacitors, diodes, transistors and relays as installed parts;
- circuit assembly on a dedicated Electronics Workbench, keeping Engineering focused on design/calculation;
- powered Circuit Controller using the existing WayAround Energy Network / energy cables;
- circuit power draw and material aging while energized;
- regression tests for circuit persistence and material-history persistence.

This milestone deliberately stops before a prebuilt computer. It creates the graph, material history and electrical contract that future sensors, automation, memory, logic and computers can reuse.

## Milestone 1.5 — Steam Age

Implemented in the development branch:

- dedicated Electronics Workbench for the electronics profession workflow;
- Engineering Workbench restored to engineering-only use;
- separate Steam Boiler with Pressure Vessel and Safety Valve assembly parts;
- boiler water input by bucket or the existing Pipework fluid network;
- shared steam state with amount, pressure and temperature;
- bounded SteamNetwork routing through existing steam-rated Pipework;
- pipe pressure/temperature ratings affect steam transmission and structural damage;
- Steam Engine no longer burns coal/water internally and no longer creates FE directly;
- Steam Engine now consumes piped steam and exposes RPM, torque and mechanical power;
- normal steam-machine feedback is qualitative, while exact pressure/RPM/temperature readings require a Steam Pressure Gauge;
- steam-generated electricity now requires a downstream mechanical generator;
- dedicated Steam Age GameTests.

The new physical chain is:

`fuel + water → boiler → steam/pressure → steam pipe → piston engine → shaft/gears → generator → electrical grid`

## Milestone 1.6 — The Grid

Implemented in the development branch:

- existing Energy Cable remains the ordinary low-voltage machine network;
- one-way low-voltage distribution lets transformers/protection devices isolate input from output;
- High Voltage Line is a separate transmission topology and does not expose generic FE to ordinary machines;
- transmission distance creates bounded electrical loss;
- Grid Transformer supports low→high and high→low operation with conversion loss;
- Distribution Fuse Box accumulates overload stress, trips and requires a manual reset;
- Electric Motor converts low-voltage electrical energy back into the shared `IRotationalPower` mechanical language;
- Utility Poles provide physical support for visible transmission routes;
- dedicated Grid GameTests cover line loss, transformer conservation, fuse ratings and motor mechanical output.

Connected chain:

`mechanical source → generator → low voltage → transformer → high-voltage line → transformer → fuse box → loads`

And the reverse conversion is now possible:

`electrical grid → electric motor → shaft/gears → mechanical machine`

### Physical electronics editor

- the Electronics Workbench now seats exactly one real Circuit Board at a time;
- normal click seats/removes the PCB and Shift + click opens the editor;
- the PCB editor supports zoom/pan and uses only electronic components actually present in the player's inventory;
- components are dragged onto a physical layout that starts at 6×4 and can be expanded row-by-row to 6×8;
- copper connections are created pad-to-pad and consume Copper Trace according to route distance;
- the Circuit Board is now a green 3D model;
- installed components and copper traces are rendered physically on the workbench after the GUI closes;
- open workbench geometry no longer culls the top face of blocks underneath it;
- PCB rendering uses exterior lighting and reduced model shading to avoid black boards on the table;
- finished boards can be removed from the workbench and installed visibly in the Circuit Controller;
- controller rear redstone and FE now provide the board's runtime inputs;
- LED, reinforced Buzzer and Distance Detector components add visual, audio and proximity behavior to the same copper graph;
- PCB expansion is material-gated and increasingly expensive, ending at 64 Copper Blocks + 64 Redstone Blocks + 64 Diamond Blocks for the final row;
- old 6×4 PCB saves migrate to the expanded format without shifting existing components or traces.

### Emblem discovery tree

- only the root/entry emblem of each WayAround emblem tab is intended to be visible from the start;
- most downstream emblems are hidden until discovered;
- more branches use temporary hint emblems that disappear when the real discovery is earned;
- new hint progressions include accessories, moving images, warfare escalation and multi-line mechanical transmission;
- each emblem tab exposes one real starter emblem from the beginning, with deliberately vague wording;
- temporary clue nodes explicitly begin with `TIP:`, while deeper real emblems remain hidden.

### Abyss ecology / Cleiton

- procedural clouds no longer render through an underwater camera;
- deep-ocean skylight decays much earlier and the abyss fog converges to true black rather than leaving a blue water veil;
- naturally spawned WayAround fish are allowed to despawn again instead of accumulating forever;
- a bounded legacy-population cleanup removes distant accumulated fish first while preserving the nearby visible ecosystem;
- Cleiton keeps the legacy `wayaround:cleinton` registry ID for save compatibility but is displayed as **Cleiton**;
- Cleiton is a persistent four-legged amphibious fish-creature, cannot be damaged or knocked back, and reflects ordinary and WayAround ballistic projectiles.

### Connected-world polish

- WAYAROUND title branding no longer overlaps the vanilla Play stack at small window sizes;
- UPDATE LOG is anchored to a responsive lower-side position;
- the Update Log screen switches to a compact layout when the game window is minimized;
- Antarctica is connected directly after the Southern Ocean in the VISTA tree;
- Antarctica, sunfish basking and puffer-carrot discoveries are secret;
- temporary hidden VISTA hints can appear before those secrets and are revoked when the real discovery is earned;
- Antarctic cold, sky, lightmap, precipitation and wind ambience now begin gradually in the Southern Ocean instead of snapping at the continental biome boundary.

### Mining regions & deferred caverns

- rare deterministic Iron, Gold, Copper and Coal mining regions create real geological destinations instead of only uniform ore scatter;
- regions may be open-pit or underground;
- underground WayAround mine caverns stay latent while a player merely travels over them;
- approaching the planned entrance, reaching the underground volume or digging close to it starts bounded staged generation;
- generation order is structure → ore/Complex pass → mine supports/rails → mobs;
- persistent hidden region anchors keep generation progress across saves;
- Iron/Gold/Copper/Coal Complex blocks contain large but finite persistent reserves;
- breaking a Complex extracts ore while leaving the deposit in place until its reserve is exhausted;
- the Mechanical Miner uses one physical drill head and shared mechanical load/torque to extract adjacent Complex deposits over time;
- /wayaroundmine locate and /wayaroundmine awaken provide focused development testing;
- vanilla cave carvers are intentionally unchanged in this iteration; the deferred mining cavern layer is the safe performance prototype before touching base-game cave generation.

### Performance foundation

- build/assemble now verify a lossless compacted JAR and keep the original whenever recompression would be larger;
- development-only local OGG backups are excluded from shipped resources without reducing canonical audio/image quality;
- compact source-project archives can be produced separately without changing the editable checkout;
- the optional local Vosk model can release native RAM after idle time and hibernate its extracted cache after extended inactivity;
- cold speech-model storage restores locally with checksum, extraction-bound and path-safety validation, without bypassing explicit download consent;
- water surface discovery is spread across ticks with a bounded candidate budget instead of rescanning the full radius synchronously;
- turbulence discovery is also incremental, invalidates safely after movement/world changes and skips unloaded chunks;
- above-water cosmetic surface work is disabled while the camera is underwater;
- hot world-feature checks use a precomputed effective bitmask while saved/network settings remain unchanged;
- storage regression coverage includes lossless freeze/thaw, corruption, traversal, symlink, interrupted-retirement recovery and scan completeness.

This milestone is intentionally an infrastructure pass: it makes existing systems cheaper and safer to keep around before v1.2.1 connects more of them together.

## Minecraft title menu

The title screen keeps showing the active release:

**v1.2.0 — Grande Novo Mundo**

While this update is unfinished it also shows:

**NEXT // v1.2.1 — THE WORLD CONNECTS**

The **UPDATE LOG** button opens the in-game development log.

## Release gate

Do not call this v1.2.1 until the update is actually ready.

Final release commit should:

1. set `mod_version=1.2.1` in `gradle.properties`;
2. set `WayAroundReleaseInfo.RELEASED_NEXT = true`;
3. verify the title screen reads `THE WORLD CONNECTS / v1.2.1`;
4. run compile/GameTests/client smoke;
5. verify dedicated-server startup;
6. verify no download/update path bypasses informed consent;
7. update this file from development preview to released;
8. produce the final player-facing changelog.

## Player-facing changelog draft

### THE WORLD CONNECTS

Machines are starting to stop behaving like isolated blocks.

Mechanical systems now share real concepts of load, power, torque, speed and stress. A machine can spin fast and still be too weak to do the work. Shafts and gearboxes remember what passes through them. Belts have limits. Pumps can fight pressure. Crushers can stall. Existing sawmill failures now use the same mechanical language.

Material history now survives with the parts themselves, and electronics no longer need a future second power system. Players can physically build circuit boards and power them from the same grid that already serves industrial machines.

Steam is now part of that same connected world: boilers create physical steam, pipe ratings matter, piston engines create mechanical work, and electricity remains downstream of a generator.

The Grid continues the chain: electricity has transmission, transformation, protection and motors. Power can travel long distances, step down into local distribution, trip protection under abuse, and return to the mechanical network through electric motors.

This is the foundation for the next stages of WayAround: heavy industry, sensors, automation, logistics, computers, trains, electrification, steam ships and large infrastructure.

The world is beginning to connect.
