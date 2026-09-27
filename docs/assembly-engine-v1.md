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

## Connected networks

`AssemblyNetworkScanner` performs a bounded flood-fill over adjacent `AssemblyMachine` block entities.

The Assembly Guide now reports both the selected machine and the connected network summary.

## World history

Actual machine failures emit:

`wayaround:assembly_failure`

The event includes type, reason, part count, workmanship, integrity, load, stress and weakest element.

That means World State, Black Box and future archaeology systems can consume the same event without importing machine internals.
