# Experimental Ship Assembly V0

This is the player-built counterpart to the fixed-shape ships in the **Great Voyages** tab.

Great Voyages remains the easy route: place a finished vessel and accept its shape.

Ship Assembly is the engineering route: construct the hull yourself and accept the hydrodynamic consequences.

## Ship Body

`wayaround:ship_body`

Using the item on terrain/water creates the first modular vessel.

After that, interact with the vessel while holding another Ship Body.

The ship keeps a local build cursor.

- empty-hand right-click: rotate the next construction direction;
- Ship Body right-click: add the next body in front of the cursor;
- the new body inherits that orientation;
- the cursor advances to the new body.

Maximum V0 hull size: **96 bodies**.

Each body stores:

- local X/Z;
- orientation;
- structural integrity;
- burning state.

## Physics model

The entity itself is only a small controller.

The visible/physical hull is a collection of local cells. Collision against terrain is tested cell by cell.

This means a long narrow vessel, square raft, bent hull or asymmetric shape does not need one huge rectangular collision box.

### Mass

Current V0 masses are gameplay-relative values:

- body: 82;
- mast: 34;
- sail: 11;
- anchor: 128;
- chair: 13;
- passenger: approximately 72.

Heavier ships receive less velocity from the same external shove.

### Buoyancy

Every body independently samples water below/through its hull position.

Wet hull bodies contribute displacement.

Parts such as mast, anchor and chairs add mass without adding displacement, so overloading a tiny raft lowers its effective buoyancy.

### Drag

Drag uses:

- hull width;
- hull length;
- each body's orientation.

Moving a long hull broadside is more expensive than moving it along its long axis.

### Terrain collision

Movement is predicted against every hull body's local collision volume.

A small collision pushes/bounces the ship instead of instantly deleting a body.

Meaningful kinetic load accumulates stress and can damage the hull.

## Testing by punching

Ordinary melee hits first create an impulse rather than direct hull deletion.

Impulse is divided by total vessel mass.

Off-center hits also create yaw torque.

Hits while the attacker is in water get a modest leverage bonus, making it possible to manually nudge/test a vessel before it has propulsion.

Repeated/heavy impacts eventually become structural wear.

## Mast and sail

`wayaround:ship_mast` installs a mast on the current build body.

`wayaround:ship_sail` attaches to the nearest mast without a sail.

Sails read `ShipWind`.

The sail angle follows the wind visually and the same wind sample creates physical horizontal impulse.

An off-center mast creates yaw torque, so mast placement affects steering/drift.

There is intentionally no helm/input control in V0.

## Anchor

`wayaround:ship_anchor` installs one heavy anchor on the current hull body.

`wayaround:anchor_chain` operates it.

First chain interaction lowers the anchor.

The anchor descends until:

- terrain is found; or
- the V0 chain limit of 32 blocks is reached.

When it reaches terrain, the vessel is held by a strong horizontal damping/spring rather than teleport-locked.

Interact with the chain again and **hold use** to raise the anchor slowly.

## Chairs

`wayaround:ship_chair` installs a passenger seat on the current hull body.

Sneak + empty-hand interaction boards an available chair.

Seats use the ship as the vehicle, so they move/rotate with the modular hull instead of relying on stationary ArmorStand seats.

## Fire

Wooden hull bodies can ignite from:

- direct fire damage;
- fire/soul fire;
- lava;
- sufficiently high shared `EnvironmentalTemperature`.

Burning bodies:

- emit fire/smoke;
- emit environmental heat;
- lose structural integrity;
- can spread fire to adjacent hull bodies;
- are extinguished rapidly by water.

Destroyed hull bodies disappear and unsupported masts/chairs/anchor modules are pruned.

Changing hull mass/shape changes the remaining vessel physics immediately.

## Assembly Engine integration

The vessel implements:

- `AssemblyMachine`;
- `StructuralReceiver`.

Every body is represented as an Assembly part and adjacent hull cells become Assembly connections.

Masts, sails, chairs and anchor are also exposed as parts/connections.

The normal Assembly Guide can inspect vessel mass/integrity/stress.

The shared `WorldInteractionService` now supports mobile structural receivers, which means systems such as Blue can physically pull the vessel through the same `WorldForce` API used by stationary machines.

## Current experimental limits

V0 intentionally does **not** include:

- helm/rudder player controls;
- engines;
- cargo;
- procedural block placement above the deck;
- roll/pitch simulation;
- cloth deformation;
- multiple anchors;
- full fluid pressure/leak simulation.

Those are future layers. This first version exists to validate modular hull construction, buoyancy, drag, wind, anchoring and mobile Assembly physics.
