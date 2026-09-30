# Assembly Engine V1

Assembly Engine turns constructed machines into a common physical model.

## Core rule

Machines own their simulation.

Assembly Engine owns the shared description of:

- parts;
- connections;
- support;
- workmanship;
- structural integrity;
- load capacity;
- current load;
- stress;
- weakest element.

A machine implements `AssemblyMachine`.

Its parts may be real world blocks or virtual components stored inside one block entity.

Examples:

- a mechanical shaft is one physical part;
- a water-wheel hub exposes one virtual frame plus every installed paddle;
- nails are represented as structural connections rather than separate blocks.

## Main types

- `AssemblyPartNode`
- `AssemblyConnection`
- `AssemblyMachine`
- `AssemblySnapshot`
- `AssemblyEngine`
- `AssemblyNetworkScanner`

## Current migrated machines

### Water wheel

The water wheel exposes:

- frame;
- each paddle;
- nailed/contact connections;
- support state;
- torque and downstream mechanical load.

External forces can now add wear, stress the frame, wear/break nails and disturb loose paddles.

### Mechanical shaft / gearbox

Each transmission exposes its manufactured part profile and actual load history.

External force or structural damage is converted into Assembly wear and can eventually fail the component.

### Sawmill

The sawmill exposes its real stored body, drive shaft and saw blade profiles.
Its bearing and blade fastening conditions include alignment, vibration, heat
and component wear. External Assembly damage wears the real installed parts and
can jam a critical machine.

### Pulleys and belts

Pulley wheels expose their wheel and installed belt as Assembly parts. A belt is
an `AssemblyConnection.Type.BELT`, and linked pulley anchors are declared to
the generic network scanner. This lets one Assembly network cross empty blocks
between two physically linked pulleys without teaching the scanner pulley-
specific rules.

### Legacy industrial machines

Machines that predate staged physical assembly now expose virtual compatibility
components through `LegacyMachineAssembly`:

- water generator;
- steam engine;
- solar panel;
- Reforced Blaster.

Their existing simulation stays machine-owned. Assembly Engine can inspect,
load and wear them now, while future staged construction can replace those
virtual components with real manufactured parts without another architecture.

## Connected networks

`AssemblyNetworkScanner` performs a bounded flood-fill over adjacent
`AssemblyMachine` block entities and over generic non-adjacent anchors declared
by `AssemblyMachine.assemblyLinkedAnchors()`.

External links are distance-bounded and still count toward the existing machine
scan limit, preventing an inspection from becoming a world-scale traversal.

The first user is the pulley belt system, but the API is intentionally generic
for future chains, couplings and other physical links.

The Assembly Guide now reports both the selected machine and the connected network summary.

## World history

Actual machine failures emit:

`wayaround:assembly_failure`

The event includes type, reason, part count, workmanship, integrity, load, stress and weakest element.

That means World State, Black Box and future archaeology systems can consume the same event without importing machine internals.
