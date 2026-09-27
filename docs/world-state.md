# World State Engine — Foundation V1

The World State Engine is the persistent memory layer shared by Way Around systems.

## Scope

It is intentionally **not** a replacement for subsystem-specific runtime managers.

It provides:

- a persistent chronological event stream;
- world-relative game timestamps;
- wall-clock creation timestamps;
- dimension and block position;
- optional actor UUID;
- namespaced NBT payloads;
- small namespaced persistent state components;
- recent-event and nearby-event queries;
- a bounded history to avoid infinite save growth.

## Storage

The engine uses Minecraft `SavedData` in the Overworld data storage:

`wayaround_world_state.dat`

Schema version: **1**

Maximum retained historical events: **8192**

Oldest events are removed first after the cap is reached.

## Event API

Use `WorldStateService.record(...)`.

Events are data-driven. The core engine only needs a namespaced event ID and does not import the internal classes of the system that produced it.

Initial event types:

- `wayaround:jujutsu_awakened`
- `wayaround:justice_incident`

## Components

`WorldStateService.component(...)` and `updateComponent(...)` store small persistent namespaced state snapshots.

Components are for low-frequency world state such as milestones, historical counters or engine metadata.

Do **not** use components as a per-tick database for machines, particles, weather cells or entities.

## Design rule

Subsystems own simulation.

World State owns memory.

A future structure engine can therefore emit a collapse event without the World State Engine needing to know how structural physics work. Media, archaeology, achievements or server history can later consume that event independently.
