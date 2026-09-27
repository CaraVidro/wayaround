# Time & Aging Engine V1

Way Around now treats time as a shared world system instead of letting every mechanic invent its own aging counter.

## Goal

Time should leave evidence.

A machine that spent weeks outside in rain should not be equivalent to one assembled yesterday inside a dry workshop. An abandoned stone structure should slowly look abandoned. Long-term condition should emerge from material + environment + inactivity.

## Persistent state

`TemporalAgingData` stores sparse per-dimension state keyed by block position.

Each tracked position keeps:

- age ticks;
- inactive ticks;
- last sample time;
- weathering;
- corrosion;
- organic growth;
- moisture memory.

The store is bounded to 8192 entries per dimension.

## Sampling model

The engine does **not** tick every object every game tick.

Every 200 server ticks it samples loaded areas around players.

For Assembly machines it scans nearby loaded chunk block entities and accepts anything implementing `AssemblyMachine`.

That means future Assembly machines automatically qualify for temporal aging without the Time Engine importing their concrete classes.

Maximum V1 machine samples per level per pass: 160.

Elapsed time is reconstructed from the previous sample timestamp, so aging remains low-frequency and deterministic enough for persistent condition.

## Material response

`TemporalMaterial` maps existing Assembly materials to relative long-term behavior.

Examples:

- wood: meaningful weathering and organic affinity;
- fiber: high weathering;
- iron: strong corrosion response;
- steel: slower corrosion than iron;
- stone: low weathering but high moss affinity;
- diamond: nearly inert.

The Time Engine does not replace Assembly wear/fatigue.

Instead, temporal exposure produces a small wear contribution which is passed back through `AssemblyMachine.applyAssemblyWear`.

So:

environment/time -> temporal condition -> Assembly wear

rather than duplicating a second durability system.

## Environment integration

The aging sample reads:

- shared `EnvironmentalTemperature`;
- rain;
- nearby/occupying water;
- sky exposure;
- machine activity/inactivity.

Corrosion accelerates under wet exposure.

Organic growth prefers damp conditions and abandonment.

Extreme temperature increases weathering stress.

## Abandonment

An Assembly machine is considered active when it currently reports meaningful Assembly load.

Inactive time accumulates persistently.

Activity resets the abandonment clock without resetting physical age, weathering, corrosion or organic growth.

This distinction matters:

- age = how long this thing has existed in the tracked simulation;
- inactivity = how long it has been neglected.

## Visible world weathering

V1 includes deliberately conservative proof-of-concept transformations near players.

Damp, neglected stone can eventually become:

- cobblestone -> mossy cobblestone;
- stone bricks -> mossy stone bricks;
- cobblestone wall -> mossy cobblestone wall;
- stone brick wall -> mossy stone brick wall.

Dry heavily weathered stone bricks can become cracked stone bricks.

This uses sparse candidate state; the engine does not sweep every block in loaded chunks.

## Assembly Guide

Generic Assembly inspection now also reports temporal condition:

- tracked age in Minecraft days;
- corrosion percentage;
- weathering percentage.

This makes the foundation visible while testing without introducing a separate Time GUI.

## World toggle

`TIME_AGING` is a world-scoped feature under Systems.

Disabling it stops Time Engine sampling while keeping registration stable.

## Future consumers

The foundation is intentionally suitable for:

- rust visuals/stages;
- moss/roots/vines;
- abandoned buildings;
- machine seizure;
- wood rot;
- maintenance/oiling;
- preserved indoor artifacts;
- archaeology;
- historical ruins;
- vehicle aging;
- material-specific repair and replacement.

The rule is the same as other Way Around foundations:

> domain systems own their detailed simulation; Time & Aging owns shared long-term condition.
