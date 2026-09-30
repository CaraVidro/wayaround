# Industrial Progression V2

## Principle

Progression should unlock new physical capabilities, not only blocks with larger numbers.

A new machine should usually teach or reuse one engine that later machines need.

The current Assembly direction is:

primitive assembly
    ->
bench workshop
    ->
mechanical production
    ->
flow machinery
    ->
large generation
    ->
macrostructures

## Stage 1 - Bench Saw

Existing `wayaround:sawmill`.

Display identity: Bench Saw / Serra de Bancada.

Purpose:

- compact workshop machine;
- can run from a manual crank or mechanical source;
- cuts one log at a time;
- manufactures early Assembly components;
- lets the player learn blade, shaft, alignment, vibration, heat, jam and maintenance.

Current wood throughput remains:

1 log -> 10 planks

It is flexible and compact, not the throughput king.

## Stage 2 - Long Saw Line

The Bench Saw becomes a Long Saw when four physical table-extension blocks exist in one line:

extension - extension - saw - extension - extension

The larger footprint is real world geometry.

In long-line mode:

- central blade renders larger;
- feed table becomes about five blocks long;
- logs visibly travel through the line;
- manual crank is no longer enough for productive industrial cutting;
- mechanical demand rises sharply;
- vibration/jam risk matters more;
- blade, shaft and bearing participate in AssemblyGraph localized load/failure.

Bulk wood recipe:

4 matching logs -> 64 matching planks

The species is preserved.

This is intentionally much more efficient than vanilla/bench processing. The reward comes from building, powering and maintaining a larger physical machine.

### Future Long Saw products

The long saw is the natural unlock point for standardized industrial wood products, not all implemented yet:

- structural beams;
- long boards;
- slats;
- machine frames;
- sleepers/ties;
- formwork;
- sawdust/offcuts.

These can later become important inputs for bridges, plant buildings, large shafts/supports, dams, ships and chemical/fuel chains.

## Stage 3 - Mechanical Fan

Recommended next machine.

Why it belongs here:

The player already understands rotation. The fan introduces the first reusable conversion:

rotation -> directed flow

A fan should not merely apply a potion-like effect.

Assembly parts:

- frame;
- shaft;
- two bearings;
- hub;
- individual blades;
- optional belt/gear input;
- guard/housing.

Simulation output:

- airflow vector;
- pressure rise;
- flow volume;
- turbulence/vibration;
- mechanical load reflected into the shaft.

Possible uses:

- cool machines;
- feed combustion air;
- move smoke/steam/particles;
- ventilate rooms/mines;
- dry materials;
- later interact with furnaces/boilers;
- provide the conceptual base for compressors/blowers.

Failure examples:

- bent blade -> imbalance;
- bad bearing -> seize;
- loose blade -> detach;
- blocked outlet -> pressure/load rise;
- overspeed -> blade/shaft failure.

Important: natural wind is not required for this machine. It creates forced airflow from mechanical power.

## Stage 4 - Turbine

The turbine reuses the flow model in reverse:

directed fluid/steam flow -> rotation

This is the key bridge to large power plants.

The turbine should be assembled from modules/parts rather than placed as a magical generator.

Core parts:

- casing/frame;
- rotor shaft;
- bearings;
- staged blades;
- inlet;
- outlet;
- governor/valve later.

Inputs can eventually include:

- water head/flow;
- steam pressure/flow;
- future gas flow.

Output is `IRotationalPower`, not FE.

The existing Water Generator remains useful downstream:

flow -> turbine rotation -> shaft -> generator -> FE

This separation lets one generator work with water, steam or future prime movers.

## Stage 5 - Hydroelectric Plant

A hydro plant is a macrostructure, not one block.

Possible chain:

reservoir
  ->
intake gate
  ->
penstock
  ->
hydraulic turbine
  ->
shaft train
  ->
generator
  ->
electrical network

AssemblyGraph handles supports, load paths and local failures.

Hydraulic pressure/flow handles the energy source.

The plant should be possible alone but naturally easier with multiple players building/aligning/maintaining separate modules.

## Stage 6 - Coal / Steam Plant

The current Steam Engine is a compact legacy machine.

A full coal plant should decompose its process:

coal handling
  ->
furnace
  ->
boiler
  ->
steam boundary
  ->
steam turbine
  ->
shaft
  ->
generator
  ->
condenser
  ->
feedwater pump

The new AssemblyFailureModel is especially important here.

A pressure vessel does not have a scripted "explode door" event.

Differential pressure loads the real boundary, connections fail locally, and a detached component receives the release impulse produced by that load.

## Stage 7 - Solar Remake

The current Solar Panel is intentionally considered a legacy/simple implementation:

place panel -> sunlight model -> FE

The remake should become a separate electrical-materials branch rather than a mechanical machine.

Suggested modular structure:

support frame
  +
panel/cell modules
  +
bus bars/wiring
  +
junction/output module
  +
optional tilt/tracking structure

Assembly still matters:

- frame support;
- glass/cell damage;
- wiring/contact quality;
- thermal expansion;
- wind loading later;
- dirty/broken modules reduce array output locally.

Electrical behavior should be compositional too:

- modules can be wired in series/parallel;
- shading affects sections;
- one damaged module need not turn the whole array off;
- larger arrays require real footprint and cable design.

The old block can remain as a compatibility/legacy device until the modular replacement is mature.

## Why fan before turbine

Fan and turbine are two sides of the same reusable engine.

Fan:
mechanical rotation -> flow

Turbine:
flow -> mechanical rotation

Building the fan first gives a small, visible, controllable machine for validating:

- directional flow fields;
- pressure;
- blockage;
- blade geometry;
- rotational load;
- bearing/blade failures.

Once that engine behaves well, the turbine is no longer a one-off simulation.

## Near-term implementation order

1. Stabilize Long Saw gameplay and Assembly failures.
2. Add a generic `FlowMachine` / directed-flow contract.
3. Build Mechanical Fan as the first flow producer.
4. Build Turbine as the first flow consumer / rotational producer.
5. Couple turbine + existing generator into a small test power train.
6. Expand that train into hydro and steam macrostructures.
7. Replace the legacy Solar Panel with a modular electrical array.

This order grows engines first and content second.
