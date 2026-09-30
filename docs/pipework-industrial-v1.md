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

### 1. Mechanical Pump

The immediate next machine should be a Mechanical Pump rather than another
isolated processing block.

It should:

- consume rotational power;
- pull water/liquid from a source or reservoir;
- choose a target pressure/flow operating point;
- query PipeNetwork for LIQUID capacity;
- be limited by the network bottleneck;
- visibly animate an impeller/piston;
- create pressure noise/vibration;
- eventually leak if assembly quality or pipe pressure is bad.

This makes Pipework useful as infrastructure rather than decorative tubing.

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
