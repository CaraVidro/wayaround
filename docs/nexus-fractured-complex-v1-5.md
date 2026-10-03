# Nexus: fractured concrete complex

## Implemented world

The Nexus uses `wayaround:nexus_complex`, a seed-dependent chunk generator. It replaces the original cave noise in newly generated chunks. Existing saved terrain and player buildings are retained; use a new world or explore new chunks for the full exterior. Old visit records are accepted as previously activated transit links.

Concrete chambers have variable rectangular footprints (29–67 blocks across), heights of 8–19 blocks, four open doorways, red wall bands, supported floors and connecting hollow corridors. Large rooms are favored. Broken wall/roof sections expose an almost flat suspended rocky plain; about a quarter of cells also hold elevated ruined chambers. Some rooms are empty, some have chairs, some have chairs and a working television, and others have rubble. Furniture reuses the media system's real blocks and block entities. Sparse oak trees, including dead trunks, grow away from corridors. Sheep, cows and pigs can naturally spawn on Nexus floors; Endermen dominate the hostile spawn table.

The custom sky has a red lower hemisphere fading toward a nearly black zenith. An immense stationary white stripe has dark red backing, a genuine gap and branching luminous fractures. It changes with camera orientation, not player translation. Red meteors, falling fragments and hovering blocks are bounded client meshes with depth testing; they are ambient scenery rather than server physics entities. Outdoor red dust spawns only locally, below the vanilla particle system's normal limits. Indoor ceilings obscure sky and debris; dust is emitted only above the local motion-blocking heightmap. Air fog is dark red and begins farther away so nearby floating rooms can be seen. Other dimensions and their clouds keep their existing behavior.

## Permanent thresholds

Completing the existing Nexustor registers a saved transit route. The entrance and return threshold remain independent of the reactor's subsequent existence or chunk loading; crouching at the reactor now reports that the cutoff is inside. Destinations prefer a large procedural room and are unique even for nearby reactors. A small arrival pad is cleared once, including for old cave chunks; revisits do not clear player edits.

One large control room is generated per 5×5-cell region (400×400 blocks). Empty-hand interaction with its cutoff terminal explains the consequence. Crouching while using it cuts **all** Nexus routes for that world. The cutoff survives server restarts and prevents teleportation immediately, including while physical portal removal is queued. Unloaded sides are removed when they later load. Ordinary terminals cannot reopen the network. Losing the threshold still leaves the player stranded; death during a real visit respawns inside the Nexus.

Transit maintenance rotates at most 16 routes every five server ticks and checks only immediately available chunks. Candidate-room searches are bounded and do not generate terrain. Actual travel explicitly loads the destination and prepares its small pad. Geometry writes are restricted to the chunk being generated, with no neighboring world reads or locks.

Operator tools (permission 2): `/nexus visit` visits a generated room without creating an artificial reactor/portal; `/nexus return` exits for inspection; `/nexus reopen` recovers a cut network; existing `/nexus finish` remains available. These are administrative tools, not survival escapes.

## Validation

Standalone geometry contracts cover continuous supported passages, open doors, negative coordinates, seed variation, room/decor diversity and control-room distribution. Five normal dedicated-server integration checks generate actual Nexus chunks, validate physical floors/exterior, reload generated chair/TV block entities, save/reload unique prepared transit destinations and the global cutoff, check the actual natural-spawn tables/predicates, and exercise the physical crouching cutoff. The explicit CI task `runNexusValidationServer` uses a normal seed-42 server because GameTestServer only loads its test dimension; the probe is disabled during ordinary server/client runs. CI also builds and boots the client to validate codec, mixin and resource registration.

Manual in-world checks remain necessary for the sky composition and perceived darkness, meteor/debris appearance, room transitions, sound mix and end-to-end multiplayer travel/respawn. No gameplay screenshot or measured FPS result is claimed by startup tests.
