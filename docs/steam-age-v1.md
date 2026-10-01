# WayAround — 1.5 Steam Age

Named release target: **v1.2.1 — THE WORLD CONNECTS**

## Goal

Steam is no longer shorthand for "put water and coal in one machine and receive electricity".

Milestone 1.5 establishes a connected physical chain:

```
fuel + water
    ↓
Steam Boiler
    ↓
heat / steam / pressure / temperature
    ↓
steam-rated Pipework
    ↓
Steam Engine
    ↓
RPM / torque / mechanical power
    ↓
shaft / gearbox / pulley
    ↓
Water Generator or other mechanical consumer
    ↓
electric grid / electronics / machines
```

## Boiler

The Steam Boiler is a separate machine.

It has a simple but meaningful assembly:

- boiler base;
- Pressure Vessel;
- Safety Valve.

The base alone cannot operate.

This keeps a complex machine physically assembled without turning an early industrial machine into a dozen tiny assembly steps.

### Inputs

The boiler accepts:

- coal;
- water buckets;
- water from the existing Pipework fluid network through NeoForge FluidHandler.

A Mechanical Pump can therefore feed a boiler.

## Steam state

Steam carries:

- amount;
- pressure;
- temperature.

Pressure depends on steam inventory and heat.

The Pressure Vessel and Safety Valve carry normal Assembly quality plus Material Memory. Their condition changes the boiler's safe pressure envelope.

An unsafe state vents steam instead of treating pressure as a cosmetic number.

## Steam transmission

`SteamNetwork` routes steam through the existing Pipework graph.

It:

- never loads chunks to finish a route;
- only traverses pipes that explicitly support `STEAM`;
- respects flow, pressure and temperature ratings;
- damages pipe structure when pressure/temperature exceed the installed pipe's rating;
- distributes a bounded steam budget among connected receivers.

The Insulated Steam Pipe remains the best dedicated route.

Steel Pressure Pipe now also supports steam within its lower rating.

Ordinary liquid copper pipe does not.

## Steam Engine

The old combined Steam Engine no longer stores coal/water and no longer creates FE directly.

It now accepts physical steam from the pipe network and exposes the normal WayAround `IRotationalPower` capability.

Its output is:

- RPM;
- torque;
- mechanical power.

Mechanical load consumes its available power and increases steam use.

The output shaft currently uses the X axis, so normal shafts/gearboxes can carry its work into the existing mechanical network.

## Instruments instead of free telemetry

Normal interaction with the boiler/engine gives qualitative feedback such as:

- water low / adequate / high;
- pressure building / working / near relief;
- cold / warming / boiling;
- engine stopped / slow / working / fast.

Exact values are intentionally hidden behind a physical **Steam Pressure Gauge**.

Using the gauge on a Boiler or Steam Engine reveals pressure, temperature and the machine-specific exact measurements.

This keeps engineering knowledge something the player measures rather than a permanent debug HUD.

## Electricity remains downstream

Steam does not bypass the electrical system.

To make electricity:

```
Boiler
→ steam pipe
→ Steam Engine
→ shaft
→ Water Generator
→ Energy Cable
→ Circuit Controller / electrical machine
```

The existing Water Generator acts as the mechanical-to-electric generator until a later generator family expands this stage.

## Profession tables

Milestone 1.5 also corrects the electronics workflow introduced in 1.4.

The Engineering Workbench is again only for engineering design/calculation.

Circuit construction now belongs to a dedicated **Electronics Workbench**.

This is intentional: profession-like roles should emerge from different physical tools and workflows, not from one universal workstation.

## Future hooks

This foundation can support:

- steam turbines;
- condensers;
- feedwater pumps;
- pressure gauges;
- governors;
- multi-stage engines;
- steam ship propulsion;
- locomotive boilers;
- district steam systems;
- electrically controlled steam valves;
- electronic pressure/temperature sensors.

Those systems should reuse this steam state/network rather than create parallel steam mechanics.
