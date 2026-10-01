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

This is the foundation for the next stages of WayAround: steam, generators, heavy industry, logistics and large infrastructure.

The world is beginning to connect.
