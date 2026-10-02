# WayAround — Mining Regions, Complexes & Deferred Caverns V1

Target: **v1.2.1 — THE WORLD CONNECTS**

## Mining is a place

Ore is no longer only a uniformly scattered block lookup.

The Overworld now has deterministic **Mining Regions** derived from the world
seed. They are intentionally uncommon, large enough to become destinations and
specialized around one dominant material:

- Iron Region;
- Gold Region;
- Copper Region;
- Coal Region.

A region may be an open-pit deposit or an underground mine field.

The geological address exists from the seed even while its expensive geometry
does not.

## Deferred cavern generation

This version does **not** replace Minecraft's vanilla cave carvers.

Doing that safely would require intercepting the chunk-generation carver stage
and later rewriting terrain that may already be saved or observed by other
systems. Instead, WayAround mining caverns are the first controlled deferred
cave layer.

Underground Mining Regions remain latent while a player merely travels over
their surface.

They wake when:

- a player approaches the deterministic entrance area;
- a player is already underground near the reserved cavern volume;
- a player breaks rock close enough to that latent volume.

Generation then advances incrementally:

1. **STRUCTURE** — excavate the large cavern/open pit;
2. **ORES** — expose dense vanilla ore and finite Complex blocks;
3. **STRUCTURES** — add mine supports/rails and physical traces of work;
4. **MOBS** — add cave life/hostiles only after geometry exists;
5. **COMPLETE**.

Only a bounded number of candidates are processed per tick. The region cursor
is persisted in a hidden stone-looking anchor block entity, so a partially
generated mine can resume after a world save instead of restarting from zero.

## Ore Complex

The new deposits are:

- Iron Complex;
- Gold Complex;
- Copper Complex;
- Coal Complex.

A Complex is a locally massive ore body, visually closer to compressed/raw ore
than to one ordinary ore block.

Breaking it does not immediately delete the block. Each successful mining action
removes a finite amount from its persistent reserve and yields real raw material.
When the reserve reaches zero the Complex becomes ordinary stone/deepslate.

World-generated Complex blocks receive large randomized reserves, so one good
deposit can support a mine for a long time without being infinite.

## Mechanical Miner

The Mechanical Miner is intentionally a simple early industrial machine.

It consists of:

- one machine body;
- one installable Mining Drill Head;
- one mechanical power connection.

Its front face must touch a Complex. Rotation comes from the shared mechanical
network through the rear.

The miner uses the same MechanicalLoad language as the rest of the factory:
power, torque, RPM and torque starvation all matter.

It does not remote-mine arbitrary terrain. It repeatedly extracts the adjacent
finite Complex and drops real output beside the machine. The drill head wears
while doing work.

This makes a discovered Mining Region a reason to build infrastructure around a
place instead of moving the ore deposit into a GUI.

## Distribution

Mining cells are 384×384 blocks. Roughly one third of cells receive a region,
with centers kept away from cell edges. Open pits are a minority.

Material weighting currently favors iron/coal, then copper, with gold the
rarest of the four region families.

The exact region address is deterministic from the seed.

## Testing commands

Operators can use:

`/wayaroundmine locate`

to find the nearest deterministic region, and:

`/wayaroundmine awaken`

to force the nearby region into the deferred-generation queue when already close
enough for a runtime test.

## Performance boundary

This system demonstrates deferred expensive world content without changing
vanilla cave generation yet.

A player standing over a latent underground Mining Region pays only deterministic
region-address checks. No cavern blocks, rich ore pass, support pass or mob pass
are created until proximity/excavation activates the region.

This is the safe prototype for future broader cave deferral work.
