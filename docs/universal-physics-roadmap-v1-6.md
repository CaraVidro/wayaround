# Universal physics roadmap v1.6

This roadmap turns WayAround's isolated physical simulations into shared physical
contracts. It is intentionally incremental: existing systems keep working while
new universal services become the canonical source of truth.

## Rule zero: space and time are the stage

Minecraft already gives WayAround world coordinates, dimensions, game time,
loaded chunks and collision geometry. Keep those as the initial substrate.

Do not encode astronomy as `timeOfDay == celestialPosition`. Future celestial
work must be able to give the sun, moon and other bodies independent orbits:
the moon may be visible during daytime and eclipses must emerge from alignment,
not from a hard-coded "12:00" event. This is recorded now but is not implemented
in this phase.

## Existing systems that must be bridged, not recreated

The current v1.6 line already contains useful pieces:

- Assembly part material and workmanship profiles.
- MaterialMemory for corrosion, fatigue, deformation and thermal history.
- EnvironmentalTemperature / RegionalTemperature.
- pipework flow and hydraulic load.
- boiler-local steam pressure / thermodynamics.
- ocean-depth pressure exposure.
- weather/ship wind helpers.
- dynamic vanilla-fire extension, smoke and bounded fire LOD.
- WorldState and time-aging foundations.

The universal physics work must progressively make these systems consume common
matter/temperature/pressure/flow contracts instead of deleting them.

## 1. Universal matter vocabulary — IMPLEMENTED IN THIS BRANCH

Goal: one canonical answer to "what is this made of and what phase is it in?"

- MatterPhase: SOLID, LIQUID, GAS.
- MaterialDefinition:
  - phase-specific density, heat capacity, thermal conductivity,
    compressibility and viscosity;
  - phase-transition temperatures;
  - shared gameplay engineering traits.
- PhysicalMaterials: canonical registry for core WayAround/vanilla materials.
- MatterState: runtime sample carrying material, phase, amount/volume,
  temperature and pressure.
- BlockMatterResolver: compatibility bridge from vanilla BlockState/FluidState
  into the physical vocabulary.
- Assembly material constants now resolve through PhysicalMaterials instead of
  owning a second independent physical table.
- Vanilla campfires are a real integration test: lit campfires can loft embers;
  if an ember lands beside a combustible vanilla surface such as grass/wood,
  it places vanilla fire, which is automatically adopted by WayAround's dynamic
  fire layer.

This step deliberately does NOT simulate global temperature, pressure or flow.

## 2. Physical regions, volumes and boundaries — IMPLEMENTED IN THIS BRANCH

Goal: describe where matter can exist.

Implemented foundation:

- PhysicalVolume shared geometry contract.
- PhysicalBoundaryFace with wall material, area and approximate thickness.
- PhysicalOpening for known atmosphere/world-edge connections.
- PhysicalRegionSnapshot with SEALED / VENTED / INDETERMINATE closure.
- PhysicalRegionScanner:
  - bounded flood fill;
  - never loads remote chunks;
  - records free volume instead of assuming every traversable cell is empty;
  - returns INDETERMINATE when cell/radius/chunk limits prevent a safe answer.
- PhysicalBlockGeometry adapts ordinary Minecraft collision geometry into the
  coarse regional model.
- openable vanilla blocks participate through their OPEN state.
- a debug command, /wayaroundphysics region [radius] [maxCells], inspects the
  player's current cavity and marks known atmosphere openings.
- GameTests build a real vanilla stone room with an oak fence gate: closed is
  sealed, open is vented.
- PipeSpec now derives internal cross-section and volume from its existing
  radius so later pressure/flow work does not invent a second pipe geometry.

This is intentionally cell-scale topology, not a CFD solver. Open connected
spaces become one region; thin/sub-block topology can be refined later without
changing the PhysicalVolume contract.

This becomes the common substrate for tanks, rooms, pipes, boilers, caves,
submarines, ship compartments and atmosphere cells.

## 3. Universal temperature

Promote EnvironmentalTemperature from "environmental heat API" into the shared
thermal service.

- matter/region temperature;
- thermal energy rather than arbitrary heat bars where practical;
- conduction through boundaries;
- bounded convection hooks;
- source/sink adapters for vanilla fire, campfires, lava, sunlight and machines;
- MaterialMemory observes real thermal cycles.

Existing RegionalTemperature remains the sparse/LOD implementation layer.

## 4. Universal pressure

One pressure contract for gases and liquids.

- pressure belongs to matter in a volume;
- external vs internal pressure;
- wall stress derived from delta-P, geometry and material;
- bridge boiler pressure, hydraulic pressure and ocean pressure;
- no hard-coded "submarine fails at depth X" when a physical vessel description
  is available.

## 5. Universal flow

One flow language, multiple solvers.

A flow sample contains medium, direction, velocity, density and pressure state.

Adapters:
- atmosphere -> wind;
- ocean -> current;
- connected conduit -> pipe flow;
- steam -> gas flow;
- ventilation -> room gas exchange.

The algorithms may differ by scale, but they share contracts.

## 6. Conservation and phase change

- mass is conserved across representation changes;
- solid <-> liquid <-> gas where the material supports it;
- freezing, melting, boiling and condensation;
- latent heat can be introduced once the thermal solver needs it;
- pumps move matter instead of creating arbitrary throughput.

## 7. Combustion, smoke and gas mixtures

Merge dynamic fire with matter + temperature + gas availability.

- fuel + oxidizer + ignition temperature;
- moisture changes ignition;
- smoke is produced matter / regional gas, not only particles;
- ventilation and wind affect smoke and flame;
- campfires, furnaces and machine fireboxes become ordinary physical sources.

## 8. Structural stress and failure

Shared tension/compression/shear/torsion/fatigue vocabulary.

Bridge:
- structural collapse/debris;
- MaterialMemory;
- machine shafts/gears;
- pressure-vessel failure;
- ship hull damage.

## 9. Buoyancy and hydrodynamics

Mass + displaced fluid volume + fluid density.

Use it for ships, debris, fish/carcasses where useful, submarines and flooding.

## 10. Rotation, torque and mechanical networks

Unify current rotational/mechanical load systems around torque, angular
velocity, inertia, friction and transmitted load. Existing machinery remains the
implementation target rather than being recreated.

## 11. Electricity and thermal loss

Voltage/current/resistance/power/heat using the same material conductivity and
temperature state.

## 12. Regional atmosphere, weather and ocean coupling

Once temperature + pressure + flow exist:

- convection;
- pressure-gradient wind;
- humidity transport;
- cloud/rain hooks;
- large-scale ocean currents;
- weather remains region/LOD based, never molecule simulation.

## 13. Celestial space-time refinement

Only after the terrestrial foundation is stable:

- independent solar/lunar/celestial trajectories;
- moon visible during daytime when geometry permits;
- seasons and illumination derived from orbital state;
- eclipses from alignment and occlusion;
- no fixed "12:00 means celestial noon" assumption.

## Performance law

Full simulation is local and bounded. Medium distance uses simplified regional
state. Far distance stores summarized history. No solver may load remote chunks
merely to continue physics.

## Migration law

Each step must:
1. identify old isolated implementations;
2. add a compatibility adapter;
3. move one real vanilla/WayAround feature onto the common contract;
4. add a bounded test or runtime validation;
5. only then deprecate duplicate math.
