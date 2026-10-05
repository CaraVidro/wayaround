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

## 3. Universal temperature — IMPLEMENTED IN THIS BRANCH

EnvironmentalTemperature remains the canonical shared service while the old
RegionalTemperature field is retained as the sparse/LOD storage layer.

Implemented foundation:

- ThermalPhysics:
  - joules for energy;
  - watts for heat-source power;
  - material/phase heat capacity;
  - energy <-> temperature conversion;
  - conductive wall power from material conductivity, area and thickness.
- ThermalRegionModel:
  - room/compartment air heat capacity comes from PhysicalRegion volume;
  - wall heat loss comes from the materials found by step 2;
  - openings add explicit air-exchange conductance;
  - SEALED and VENTED regions naturally get different cooling constants;
  - INDETERMINATE topology is never rewarded with perfect insulation.
- RegionalTemperature thermal cells can now retain a region-specific relaxation
  constant instead of every disturbance cooling with the old fixed 240-tick
  curve.
- real energy can be injected uniformly into a bounded PhysicalRegion while the
  existing sparse 8-block field remains the runtime representation.
- vanilla heat-source adapters now provide physical power and source temperature
  for lava, fire, campfires and magma.
- a lit vanilla campfire is a reliable source through its existing block entity:
  campfire -> watts -> PhysicalRegion -> heat capacity -> sparse temperature.
- opportunistic player-local source discovery remains bounded and now routes
  through the same energy API.
- /wayaroundphysics region now also reports local/ambient Celsius, enclosure
  heat-loss conductance and thermal time constant.
- MaterialMemory accepts real Celsius through a compatibility bridge; hot fluid
  in existing Pipework feeds that path once per second rather than inventing a
  separate pipe heat scale.
- GameTests verify heat capacity, stone-vs-wood conduction, sealed-vs-vented
  cooling and Celsius-driven MaterialMemory damage.

Still intentionally deferred:
- latent heat and phase changes (step 6);
- pressure feedback from heated gases (step 4);
- true convection/flow transport (step 5);
- wall thermal mass and multi-layer walls can refine ThermalRegionModel later
  without changing its public contract.

## 4. Universal pressure — IMPLEMENTED IN THIS BRANCH

One pressure contract now bridges natural fluids, gases and legacy machinery.

Implemented foundation:

- PressureMath is dependency-free and makes kPa the canonical internal unit.
  Bar remains a compatibility/UI unit.
- PressureState explicitly separates absolute pressure from its reference
  pressure and exposes gauge pressure and pressure differential.
- PressurePhysics provides:
  - hydrostatic pressure from density, gravity and depth;
  - gas pressure from mass, volume, temperature and a material reference state;
  - pressure force over area;
  - thin-wall hoop-stress input for later structural failure;
  - the shared bounded overpressure-damage curve used by legacy machinery.
- PhysicalMaterials now has a simplified salt-water material (1025 kg/m3) for
  natural ocean hydrostatics while fresh vanilla water remains its own material.
- NaturalPressure samples the world without loading chunks:
  - atmosphere varies gently with elevation;
  - vanilla water columns derive pressure from their local free surface;
  - ocean biomes below sea level use the known sea surface directly, which is
    both cheaper and correct under overhangs.
- UniversalPressure is the canonical facade for natural, region and gas pressure.
- sealed PhysicalRegions couple temperature into air pressure; vented and
  indeterminate regions remain tied to natural atmosphere until universal flow
  tracks real gas exchange.
- abyss capsule/submarine runtime now consumes external natural ocean pressure
  and compares it against internal atmospheric pressure. The old 70/85-block
  limits survive only as pressure-equivalent fallback hull ratings because those
  vehicles do not yet expose real wall material/thickness.
- OceanPressure keeps its old depth API only as a compatibility adapter for old
  tests/callers; it immediately converts depth to hydrostatic delta-P.
- SteamThermodynamics now derives boiler pressure from steam mass, effective
  vessel volume and Celsius using the shared gas-pressure model instead of its
  private fill/heat pressure curve.
- HydraulicLoad pressure damage delegates to PressureMath.
- mechanical-pump pressure pulses now travel through PipeFlow in canonical kPa;
  bar getters/saves remain for compatibility.
- /wayaroundphysics pressure reports natural absolute/gauge pressure, depth and
  medium, and reports sealed-region gas pressure when applicable.
- GameTests verify seawater pressure, a real vanilla water column, heated-gas
  pressure and pressure force. The standalone OceanPressure regression also
  checks the new delta-P runtime path.

Still intentionally deferred:
- exact vessel yield/failure from material strength belongs to structural step 8;
- connected-fluid static head/backpressure topology becomes richer in flow step 5;
- tracked gas mass transfer between rooms belongs to flow/conservation steps 5/6.

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
