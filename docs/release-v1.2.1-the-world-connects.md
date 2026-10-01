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
- power fulfillment;
- torque starvation;
- safe-RPM / overspeed evaluation;
- combined failure-stress evaluation;
- shaft/gearbox stress integration;
- pulley throughput limits and overload wear;
- mechanical pump torque demand from pressure/flow;
- crusher demand integration;
- sawmill shared load math;
- GameTest coverage for power vs torque vs speed;
- title-screen Update Log.

## Milestone 1.4 — Materials Have Memory + Electronics Seed

Implemented in the development branch:

- persistent material memory for load cycles, thermal cycles, heat damage, corrosion and deformation;
- common material conductivity, heat tolerance, corrosion resistance and ductility traits;
- material history integrated into shafts, gearboxes, pump impellers, crushers, mills and sawmills;
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

This is the foundation for the next stages of WayAround: heavy industry, sensors, automation, logistics, computers, trains, steam ships and large infrastructure.

The world is beginning to connect.
