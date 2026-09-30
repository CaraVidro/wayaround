# Assembly Failure Model V1

## Rule

Do not script outcomes when a physical cause can produce them.

Bad:
- "boiler door breaks -> launch door entity"
- "dam reaches 0 HP -> explode dam"

Good:
- pressure differential loads a boundary;
- load travels through real parts/connections;
- the weakest local constraint exceeds capacity;
- the failure model selects a failure family;
- the owning component translates that generic failure into its concrete state.

## Load cases

AssemblyLoadCase currently supports:

- static;
- tension;
- compression;
- shear;
- bending;
- torsion;
- impact;
- vibration;
- pressure;
- hydraulic;
- mechanical.

A load contains magnitude, direction and cyclicity.

Pressure is generated from differential pressure times exposed area. The sign of the differential determines release direction.

## Failure families

AssemblyFailureMode currently includes:

- split;
- fracture;
- shear;
- pull-out;
- buckle;
- bend;
- twist;
- snap;
- slip;
- seize;
- tear;
- rupture;
- detach.

The mode is chosen from material, component kind, connection type, load type, fatigue and localized graph stress.

Examples:

- wood + bending -> split;
- stone + impact -> fracture;
- steel + compression -> buckle;
- shaft + torsion -> twist, then snap at larger overload;
- fastener + pressure/tension -> pull-out;
- belt + overload -> slip or tear;
- bearing + excessive mechanical/torsional/vibration load -> seize.

These are generic physical families, not machine-specific animations.

## Release impulse

A failure event includes a release impulse.

This is the momentum made available when a constraint stops holding a component.

Examples:

- a pressure boundary fastener pulls out;
- the detached door/module receives the pressure-direction impulse;
- a component implementation may convert that into a real entity, falling piece, block removal, hinge motion or other physical representation.

The failure model does not spawn that representation itself.

This keeps cause and presentation separate.

## Localized dispatch

AssemblyEngine can predict or dispatch a failure from one AssemblyLoadCase.

Dispatch calls AssemblyMachine.applyAssemblyFailure(event).

Older machines inherit a compatibility fallback that converts severity into generic wear.

Machines with real internal parts should override the callback and mutate the exact part/connection that failed.

## Water wheel proof

The water wheel is the first real consumer.

- paddle load routes through its frame connection;
- local fastener failure can loosen/pull out the nail and leave the paddle loose;
- destructive board failure can remove the paddle;
- frame failure increases frame damage and can accelerate structural collapse;
- the failure is recorded in Assembly history with target/mode context.

The wheel therefore no longer needs every serious failure to be selected by one global wear threshold.

## Pressure example

A future furnace/boiler door should be represented as:

pressure volume A
     |
     | differential pressure x exposed area
     v
door panel
     |
hinge + latch connections
     |
frame/support

If the latch is weakest:
- latch receives the hotspot;
- failure mode can be PULL_OUT;
- failure event carries outward release impulse;
- door implementation becomes free and receives that impulse.

If the door panel is weaker than the latch:
- panel can BEND/RUPTURE instead.

If the frame is weaker:
- frame can fail first.

Nothing in AssemblyFailureModel contains a "furnace door must fly" rule.

## Dam example

A future dam is the same principle at another scale.

Reservoir pressure becomes hydraulic/pressure load over exposed sections.

That load routes through wall segments, reinforcement, joints, foundations and abutments.

A local rupture changes the graph and hydraulic boundary.

The next evaluation can therefore move stress into neighboring sections.

A progressive breach can emerge from repeated graph changes rather than one scripted whole-dam explosion.

## Future extensions

The failure contract is ready for:

- detached physical component entities;
- crack propagation;
- pressure volumes;
- fluid flow through newly opened breaches;
- thermal expansion loads;
- fatigue accumulation by hotspot rather than whole-machine wear;
- directional support geometry;
- multiple simultaneous load cases;
- load redistribution immediately after a failed connection.

The core rule remains:

cause -> load -> graph -> local failure -> physical consequence
