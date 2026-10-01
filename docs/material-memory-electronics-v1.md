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

Electronics uses a dedicated **Electronics Workbench** with a physical PCB editor.

1. right-click the bench with a Circuit Board to seat exactly one board on the table;
2. right-click the bench again with an empty hand to remove that same board, preserving its exact layout;
3. Shift + right-click while a board is seated to open the PCB editor;
4. the component list is generated from electronic parts that actually exist in the player's inventory;
5. drag a component from that list into one of the board's limited 6×4 physical positions;
6. each placed component exposes a copper connection pad in the editor;
7. click one pad and then another to route a copper trace;
8. longer traces consume more Copper Trace items than short neighboring connections;
9. right-click a populated board position in the editor to recover that component and the copper attached to it.

The editor supports zoom and panning, similar to the Engineering Workbench, but it is not a block catalogue. Its left panel is inventory-driven and its right panel describes the selected electronic component and board state.

The Circuit Board item itself is a simple green 3D PCB. When it is seated on the Electronics Workbench, the board is rendered on the tabletop. Installed resistors, capacitors, diodes, transistors, relays, terminals and copper routes remain visibly present in-world even after the GUI is closed.

This separation is intentional. The Engineering Workbench remains focused on calculation, blueprints and engineering design; the Electronics Workbench becomes the physical workspace for the electronics profession-like role.

## Electricity

The Circuit Controller exposes the normal NeoForge energy capability.

Existing WayAround generators and `EnergyNetwork` can therefore power it through the existing Energy Cable.

There is no separate "electronics energy".

The controller is now the physical runtime socket for a finished board:

- remove the PCB from the Electronics Workbench after editing;
- right-click the Circuit Controller with the board to install it visibly on the controller;
- Shift + empty-hand use removes that same board again with its exact layout intact;
- FE from the existing WayAround electrical network powers the board;
- redstone entering the **rear** becomes the INPUT_TERMINAL source;
- an OUTPUT_TERMINAL routed through copper emits redstone from the controller's **front**;
- Distance Detector components are independent signal sources: they scan living entities up to 16 blocks in front of the controller and create a stronger 1–15 signal for nearer targets;
- LED components are visual sinks and make the controller glow when a routed source is active;
- Buzzer components are audio sinks and emit reinforced note-like pulses whose pitch follows signal strength;
- all of those active parts use the same explicit physical copper graph as ordinary input/output routing;
- the controller consumes FE according to circuit complexity;
- the board still ages while operating and conductivity can fall as corrosion/heat history grows.

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
