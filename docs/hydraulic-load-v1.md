# WayAround — Hydraulic Load V1

Named release target: **v1.2.1 — THE WORLD CONNECTS**

## Goal

Mechanical power and Pipework now form one feedback loop.

A pump is no longer just a rotational consumer that happens to move liquid.
The downstream hydraulic system determines how hard the pump is to turn, while
pump pressure can now stress the actual pipe route.

## Shared hydraulic language

`HydraulicLoad` is a bounded gameplay model that resolves:

- requested liquid flow;
- useful/effective flow;
- pump head / discharge pressure;
- required mechanical power;
- required torque;
- cavitation;
- backpressure;
- vibration;
- hydraulic stress.

It deliberately does not attempt per-cell CFD.

## Backpressure

The pump remembers the mismatch between attempted discharge and what the pipe
route actually accepted.

A restricted or closed discharge therefore causes:

`accepted flow down → backpressure up → pressure/torque demand up → mechanical load up`

That extra rotational demand is submitted through the same
`MechanicalLoad.OperatingPoint` used by the rest of the factory.

A weak transmission can therefore slow or stall because of the hydraulic
system instead of the pump simply reporting zero flow.

## Cavitation

The pump also remembers suction starvation.

High impeller speed with poor intake supply and a low local buffer creates
cavitation. Cavitation reduces useful flow, increases vibration and accelerates
impeller wear/material history.

The effect is intentionally qualitative in normal gameplay: the pump visibly
shakes more and emits irregular liquid/bubble sounds rather than opening a
telemetry HUD.

## Pipe pressure

`PipeFlow.applyPressurePulse` reuses the bounded directional outlet routing.

Actual pump pressure is propagated through admitted loaded pipe nodes without
loading remote chunks. Each pipe records current/peak hydraulic pressure.

Pressure beyond a pipe family's rating causes structural damage. A damaged pipe
is even more vulnerable to the same overpressure. Existing wet/damaged pipe
leak behavior then becomes a natural consequence instead of a disconnected
timer.

Examples:

- Small Copper Pipe remains a low-pressure line;
- Steel Pressure Pipe can survive pressures that damage the copper line;
- closing a downstream valve can raise pressure even when useful flow becomes
  zero.

## Material memory

Pump impellers continue using Assembly + Material Memory.

Hydraulic stress, cavitation, vibration and pressure history now feed that
existing biography instead of creating a separate pump durability system.

## Performance

Hydraulic pressure propagation is bounded by the existing PipeFlow traversal.
Sustained overpressure wear is sampled once per second per affected pipe rather
than applying expensive part updates every tick.

## Validation

Deterministic regression tests cover:

- normal open-discharge flow;
- backpressure increasing required torque/power;
- suction starvation creating cavitation;
- cavitation reducing useful flow and increasing vibration/stress;
- pressure-rating damage;
- stronger pressure-rated pipe surviving the same pressure.

GameTest coverage also verifies that a pressure pulse follows the real
directional discharge route.
