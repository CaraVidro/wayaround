# WayAround — 1.6 The Grid

Named release target: **v1.2.1 — THE WORLD CONNECTS**

## Goal

Milestone 1.6 turns electricity from a generic cable connection into infrastructure.

The intended chain is now:

```
mechanical source
→ generator
→ low-voltage cable
→ step-up transformer
→ high-voltage transmission
→ step-down transformer
→ distribution fuse box
→ low-voltage loads
```

Electricity can also return to the mechanical world:

```
grid
→ Electric Motor
→ RPM / torque / mechanical power
→ shaft / gearbox / machine
```

## Low-voltage network

The existing Energy Cable remains the ordinary machine-level distribution network.

Generators, circuit controllers, motors and ordinary electrical consumers continue to use the normal NeoForge energy capability.

Protected devices can now distribute from one physical side only, which prevents a transformer or fuse box from backfeeding through its own input cable.

## High-voltage transmission

High Voltage Line is a separate topology.

It does **not** expose generic FE.

Only explicit `HighVoltageReceiver` machines can receive from it.

That means an ordinary machine cannot be connected directly to a transmission line and work accidentally.

The transmission network is bounded, never chunk-loads a route and includes distance loss.

## Transformer

A Grid Transformer has two modes:

- low → high voltage;
- high → low voltage.

Sneak + empty-hand interaction reverses the transformer only while it is drained.

The low side uses the normal energy capability.

The high side uses the explicit high-voltage network.

Both directions have conversion loss.

## Distribution Fuse Box

The fuse box physically separates an input network from an output network.

Normal pulses pass through into a local buffer.

Repeated pulses above its rating accumulate overload stress and eventually trip the protection.

A tripped box stops accepting/distributing energy until the player manually resets it.

## Electric Motor

The Electric Motor accepts ordinary low-voltage electrical energy.

It exposes the same `IRotationalPower` contract used by water wheels, steam engines and mechanical transmissions.

Mechanical consumers therefore do not care whether their rotation came from water, steam, a hand crank or electricity.

Load on the motor increases its electrical consumption.

## Utility poles

Utility Pole is a physical support for building visible transmission routes.

It is intentionally not a magical network node. The actual electrical route remains the High Voltage Line placed on/along the structure.

## Polar/UI polish included with 1.6

This branch also fixes connected-world presentation issues:

- title-screen WAYAROUND branding is moved away from the center button stack;
- UPDATE LOG is anchored to a responsive lower-side position;
- the Update Log screen compacts itself on small/minimized windows;
- Antarctica now sits directly after the Southern Ocean in the VISTA tree;
- Antarctica, sunfish basking and puffer-carrot discoveries are secret;
- secret VISTA hints can appear temporarily and are revoked when the real discovery is earned;
- polar cold, lighting, sky color, precipitation and breeze begin gradually in the Southern Ocean and build toward full Antarctica.

## Future hooks

The Grid foundation can later support:

- electrical meters;
- batteries and substations;
- breakers with configurable ratings;
- multi-phase motors;
- electrically controlled pumps/valves;
- train electrification;
- radios and computers;
- transmission towers;
- grid failures and blackouts;
- electronic sensors controlling industrial loads.

Those systems should build on the existing low/high-voltage boundary instead of adding another electrical universe.
