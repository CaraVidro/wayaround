# WayAround — Mechanical Load & Failure Framework

Development milestone: **1.3 / Physical Industry**

Named release target: **v1.2.1 — THE WORLD CONNECTS**

## Why this exists

WayAround already had real rotational sources, gear ratios, power budgets, part wear and several machine-specific failure rules. This milestone does not replace those systems.

It gives them a shared language:

- requested power;
- granted power;
- available torque;
- required torque;
- RPM;
- safe RPM;
- power starvation;
- torque starvation;
- overspeed;
- combined failure stress.

A machine still does not need to know "I am a sawmill" at the network level. It asks for mechanical work and decides what physical shortage or overload means for its own mechanism.

## Shared rules

`MechanicalLoad` is the common evaluator.

### Power

Power is conserved through the existing `IRotationalPower.consumePower` budget.

A gear ratio may exchange speed for torque, but cannot create free power.

### Torque

Heavy work can require torque independently from RPM.

A fast shaft with insufficient torque may therefore fail to start a crusher or pump against a heavy hydraulic load.

### Overspeed

Machines may define their own safe RPM.

Overspeed does not automatically explode a machine. It feeds wear, vibration, heat or machine-specific failure behavior.

### Stress

`MechanicalLoad.failureStress` combines:

- load;
- overspeed;
- vibration;
- heat;
- condition.

It is intentionally dimensionless. It is a simulation signal, not a real-world engineering unit.

## Current integrations

### Shafts and gearboxes

Existing transmission block entities continue to own their real Assembly part state.

Sustained mechanical load now uses the shared stress model to accelerate wear and failure probability. Poor condition, high load, heat and overspeed compound rather than acting as unrelated timers.

### Pulley belts

A belt no longer represents unlimited invisible throughput.

Its available transmitted power is capped by:

- number of belt lines;
- belt condition;
- wheel geometry;
- transmission efficiency.

Demand beyond the currently transmissible output becomes slip/starvation and increases drive wear.

Existing belt rupture behavior remains in use.

### Mechanical pump

The pump now distinguishes power from torque.

Hydraulic pressure and requested flow create a torque requirement. A rapidly spinning but weak drive can stall against the pipe network.

### Crusher

Crusher work now evaluates the same shared mechanical demand instead of using an isolated torque comparison.

Higher mechanical abuse also contributes to vibration and heat.

### Sawmill

The sawmill keeps its existing rich system for jams, blade/shaft wear, heat, alignment and cutting quality.

Its power fulfillment and overspeed calculations now use the shared mechanical framework rather than parallel formulas.

## Performance rule

The framework evaluates scalar machine/network state. It does not create a physics entity for each tooth, shaft segment or moving part.

The visible result may be noisy and dramatic. The underlying simulation remains bounded.

## Next systems this unlocks

The same language can be reused by:

- steam engines;
- turbines;
- generators;
- machine tools;
- cranes;
- locomotives;
- ship propulsion;
- large pumps;
- industrial fans;
- conveyors;
- heavy manufacturing.

The point of milestone 1.3 is not to add all of those now.

The point is that, when they arrive, they no longer need a second mechanical universe.
