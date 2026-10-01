# WayAround — 1.4 Materials Have Memory + Electronics Seed

Named release target: **v1.2.1 — THE WORLD CONNECTS**

## Goal

Milestone 1.4 connects two foundations:

1. physical materials remember what happened to them;
2. electronics are built from the same electrical infrastructure already used by generators and cables.

This milestone does **not** add a finished computer system.

It makes computers, sensors, controllers and future automation possible without inventing a second magic-tech architecture later.

## Materials have memory

Assembly wear remains the ordinary present condition of a component.

`MaterialMemory` adds persistent history:

- birth/first tracked time;
- load cycles;
- thermal cycles;
- peak heat;
- heat damage;
- corrosion;
- permanent deformation;
- last service time.

Maintenance can clean corrosion and correct small deformation.

It does not erase history or magically remove severe heat damage.

## Shared material properties

`MaterialProperties` gives existing Assembly materials common normalized traits:

- electrical conductivity;
- thermal tolerance;
- corrosion resistance;
- ductility.

These values are gameplay properties, not SI engineering measurements.

Mechanical systems and electronics now use the same material vocabulary.

## Mechanical integration

Material history now affects:

- shafts and gearboxes;
- pump impellers;
- crusher parts;
- mill parts;
- sawmill blade, shaft and frame.

A component can therefore be mechanically repaired while still carrying permanent history from abuse.

## User-built circuit boards

A circuit board is not a recipe for a named machine.

It is a persistent 6×4 construction surface.

A board stores:

- occupied component cells;
- input terminals;
- output terminals;
- resistors;
- capacitors;
- diodes;
- transistors;
- relays;
- explicit copper traces between cells;
- electrical operating cost;
- material history.

The board is carried as one ItemStack, so the exact layout survives removal, transport, storage and later installation.

## Building circuits

Electronics uses a dedicated **Electronics Workbench**.

1. place a Circuit Board on the Electronics Workbench;
2. right-click a top-face cell with an electronic component to install it;
3. use Copper Trace on one cell and then another to route a trace;
4. use the Assembly Hammer on a cell to recover installed parts/traces;
5. Shift + empty-hand use removes the board with its topology intact.

This separation is intentional. The Engineering Workbench remains focused on calculation, blueprints and engineering design; the Electronics Workbench becomes the physical workspace for the electronics profession-like role.

## Electricity

The Circuit Controller exposes the normal NeoForge energy capability.

Existing WayAround generators and `EnergyNetwork` can therefore power it through the existing Energy Cable.

There is no separate "electronics energy".

The controller:

- accepts a player-built circuit board;
- consumes FE according to circuit complexity;
- reads an electrical/redstone input from its rear;
- requires a real conductive path from INPUT_TERMINAL to OUTPUT_TERMINAL;
- emits the resulting signal from its facing side;
- ages the copper board while operating;
- can suffer conductivity loss as corrosion/heat history grows.

## Why this is a seed instead of a computer update

The important new object is the **circuit graph**, not a pre-authored computer.

Future systems can reuse the same board data and electrical controller contract for:

- temperature sensors;
- pressure sensors;
- automatic valves;
- machine governors;
- train signals;
- alarms;
- relay logic;
- industrial automation;
- radios;
- calculators;
- clocks;
- memory;
- logic gates;
- CPUs;
- player-built computers.

The future rule is:

> If a device can be represented as electrical inputs, electrical state and electrical outputs, it should not need a new electronics universe.

## Current limitation

Milestone 1.4 persists components and topology and executes the first powered input/output path.

Individual resistor/capacitor/diode/transistor/relay behavior is intentionally not a complete SPICE-like simulator yet.

Their physical placement is already stored so future electrical simulation can add behavior without invalidating player-built boards.
