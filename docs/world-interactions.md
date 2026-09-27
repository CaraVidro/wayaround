# World Interaction API V1

The interaction layer prevents gameplay systems from hard-coding every possible target.

## WorldForce

`WorldForce` describes a physical influence:

- origin;
- direction;
- radius;
- magnitude;
- kind;
- source ID;
- optional actor.

Kinds currently include push, pull, impact, explosion, vibration, hydraulic and mechanical force.

## StructuralReceiver

A block entity/structure implements `StructuralReceiver` when it can translate generic forces into its own physical consequences.

The emitter does not know the target class.

Example:

`BlueManager` emits `wayaround:blue` as a PULL force.

It does **not** import `WaterWheelHubBlockEntity`.

The water wheel receives the generic force and decides how its own frame, nails, paddles and load react.

## StructuralDamage

`StructuralDamage` is separate from entity/combat damage. It describes damage to constructed systems.

## Performance

V1 scans are intentionally bounded:

- maximum structural-force radius: 8 blocks;
- maximum receivers per emission: 64;
- Blue publishes structural force on a throttled cadence.

This layer is for meaningful constructed systems, not a replacement for every terrain block interaction.

## Migration rule

New physical machines should prefer:

1. `AssemblyMachine` for construction/condition;
2. `StructuralReceiver` for external physical interactions;
3. existing mechanical rotation APIs for power transfer;
4. `EnvironmentalTemperature` for world heat/cold.

That keeps systems composable instead of importing each other directly.
