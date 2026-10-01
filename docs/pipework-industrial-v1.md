> Follow-up: [mechanics-pipeflow-v1.md](mechanics-pipeflow-v1.md) supersedes this initial topology-only state with valves, liquid movement and staged hollow ducts.

# Pipework / Industrial V1

This branch consolidates WayAround's industrial visual layer and introduces the
first technical pipe family.

## Ported and unified machinery

The current industrial line includes:

- Water Wheel — shared fluid rotation smoothing.
- Mechanical Shaft / Gearbox — shared continuous smoothing.
- Pulley — smoothed wheel and belt travel.
- Industrial Sawmill — existing full machine preserved, with smooth blade/crank
  rendering and manual/rotational operation.
- Mechanical Press — synchronized object-animation timeline restored.
- Mechanical Fan — animated five-blade machine with airflow and mechanical load.
- Water Generator — open-frame animated rotor tied to real generation state.
- Steam Engine — animated flywheel/piston/valve mechanism.
- Reforced Blaster — procedural industrial body and animated blower.

## Pipework creative tab

Pipework is a separate creative category.

The first six pipe families are intentionally different pieces of equipment,
not color variants.

| Pipe | Media | Flow/t | Pressure | Temp |
| --- | --- | ---: | ---: | ---: |
| Small Copper Pipe | liquid | 240 | 3 bar | 180 C |
| Iron Utility Pipe | liquid | 720 | 6 bar | 220 C |
| Large Water Main | liquid | 2400 | 4 bar | 140 C |
| Thin Gas Line | gas | 420 | 12 bar | 160 C |
| Steel Pressure Pipe | liquid / gas | 980 | 18 bar | 450 C |
| Insulated Steam Pipe | gas / steam | 760 | 22 bar | 650 C |

Each family has:

- its own recipe;
- its own geometry / thickness;
- its own block/item model;
- its own loot;
- technical inventory tooltip;
- physical medium compatibility.

Dedicated liquid and dedicated gas pipes do not connect directly. The Steel
Pressure Pipe acts as a legitimate bridge for either medium because it is
rated for both.

## Connected geometry

Pipes use six-direction block states.

A center model is always rendered and directional arm models are added through
multipart blockstates. This naturally produces:

- straight runs;
- elbows;
- T junctions;
- crosses;
- vertical risers;
- ceiling runs;
- dense mixed industrial layouts.

Large mains, pressure pipes and steam lines include heavier collars/flanges,
while instrumentation-like lines remain visually thin.

## Network inspection

PipeNetwork performs bounded searches (maximum 512 pipe blocks).

It reports network bottlenecks:

- flow;
- pressure;
- temperature;
- media shared across the network.

A medium-specific inspection mode already exists for LIQUID, GAS and STEAM.
This is the API future machines should use.

The intended rule is that a large network is only as capable as its weakest
relevant section. A small pipe inserted into a high-flow main becomes an
actual bottleneck.

## Next machines

### 1. Mechanical Pump — implemented

The first active Pipework machine is now implemented.

It consumes rotational power, pulls real liquid through a direct intake or
bounded suction network, buffers up to 2000 mB and pushes through the existing
PipeFlow network. The discharge is limited by pump RPM/power and the actual
pipe bottleneck.

Assembly is intentionally light: pump housing + one impeller cartridge. The
visible rotor only exists after installation.

### 2. Modular Reservoir / Water Tank

After the pump:

- large storage tanks;
- fill level;
- inlet/outlet ports;
- labels/signage;
- pressure/gravity head;
- later dirt/wetness/leak integration.

This directly supports the target visual of a believable water plant.

### 3. Mechanical Mill — implemented

The Mechanical Mill is now part of this branch.

Current scope:

- two visible millstones with the upper stone driven by SmoothObjectAnimation;
- vertical drive shaft and visible grain hopper;
- real rotational-power consumption through MechanicalTransmission;
- wheat -> flour as the first process;
- RPM and accepted load influence throughput;
- dust particles and low grinding sound while working;
- flour tray becomes visible when output exists;
- Assembly frame/shaft/stones/hopper parts with wear and load;
- right-click wheat to feed; empty-hand click collects flour.

This machine is intentionally recipe-light in V1. Future grinding recipes can
extend it without changing the mechanical contract.

### 4. Mechanical Pump — next target

The next machine after the mill should be the Mechanical Pump, because it turns
Pipework from rated infrastructure into active fluid infrastructure.

The pump should consume rotational power, select a pressure/flow operating
point, query PipeNetwork bottlenecks, animate a piston or impeller and become
the first machine that actively pushes liquid through the new pipe families.

## Long-term visual target

Pipework is one of the foundations for industrial places that feel built and
used rather than placed from a mod catalogue: dense runs at different heights,
supports, tanks, labels, dust, leaks, puddles, light shafts and gradual surface
wear can all layer on top of this network without replacing it.


## Current limits and acceptance checks

These are implemented ratings and network inspection, not active fluid storage
or transfer. No pump, reservoir, pressure simulation or leak simulation is
included yet. The flow values above are nominal per-tick ratings for the future
transport system; V1 does not yet assign a transported volume unit.

Inspection admits at most 512 pipes and evaluates every admitted pipe, including
the final queued section. Starting in an unloaded chunk returns an empty result;
inspection never loads chunks. Networks beyond this budget are partial snapshots
and must not be treated as a guarantee that an entire larger network is safe.

Manual in-game checks:

1. Open Pipework and place all six families; compare diameter and flange geometry.
2. Build elbows, T junctions, vertical and ceiling runs; remove a neighbor and
   verify both ends update their connections.
3. Place a thin gas line next to a copper liquid line: no direct connection.
4. Add a steel pressure section and inspect the network. Query by medium when
   evaluating a future pump: generic inspection may report no common medium.
5. Assemble the sawmill body, blade and shaft; compare manual crank and powered
   cutting, then inspect wear, vibration and jams. This is the existing sawmill,
   not a newly implemented long industrial batch saw.
6. Feed wheat into a powered Mechanical Mill; collect flour with an empty hand.
   Break a loaded mill and verify both wheat and flour drop.

## Future machinery shortlist — proposals, not implemented

| Machine | Purpose | Visible behavior / dependency |
| --- | --- | --- |
| Mechanical pump | Make liquid pipes operational | Rotating impeller, power draw, reservoir ports, real stored volume |
| Modular water tank | Buffer supply and create the water-plant silhouette | Visible fill level, inlet/outlet, overflow |
| Long industrial saw | Separate batch processor from the compact sawmill | Feed rollers and carriage; proposed 4 logs -> 64 boards, requiring balancing |
| Ore crusher | Begin dedicated iron processing | Jaw motion, torque spikes, ore fragments; preserve vanilla smelting |
| Ore washer | Separate useful ore from waste | Water consumption, sludge output, rotating drum |
| Air compressor | Give gas lines a concrete consumer/producer | Piston, receiver tank, pressure relief; needs gas storage first |
| Mechanical bellows | Supply air to metallurgical machines | Reciprocating leather chamber and mechanical linkage |
| Grain sifter | Extend mill output processing | Shaking mesh, flour sorting and dust |
| Bucket elevator | Lift bulk materials through a plant | Endless belt, visible buckets, power proportional to load |
| Mechanical clock | Useful town machinery | Escapement, moving hands, bell driven by stored mechanical energy |
| Boot polisher | Excessive engineering for tiny convenience | Two rotating brushes, squeak, shoe shine |
| Automatic soup stirrer | Kitchen industry with questionable ambition | Gear-driven paddle, pot and bubbling animation |
| Mechanical applause machine | Celebrate factory milestones | Cam-driven wooden hands; consumes power to clap |
| Executive desk fan | Entire power network dedicated to comfort | Small oscillating fan and dramatically unnecessary gearbox |

Prioritize pump -> reservoir -> ore crusher/washer. The Mechanical Mill already
provides the first grain processor; novelty machines can reuse the same power,
Assembly and animation contracts without delaying the transport foundation.

## Assembly Crushers follow-up

`feature/assembly-crushers-v1` adds three physical crusher assemblies and migrates
the mill from virtual components to installed ItemStacks. See
`docs/assembly-crushers-v1.md` for scale, component tradeoffs, save compatibility
and the explicit boundary between current dropped-item input and future
connected-block transport.


### Assembly complexity rule

Fundamental machines should not be exploded into arbitrary sub-parts merely
because Assembly exists. A simple mechanism should normally use a housing plus
one or two meaningful installed modules. More complicated staged assembly is
reserved for machines whose actual mechanism benefits from those distinctions.
