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

## 5. Universal flow — IMPLEMENTED IN THIS BRANCH

One flow language now sits above the existing specialized solvers.

Implemented foundation:

- FlowMath provides dependency-free unit/transport math:
  - blocks/tick <-> metres/second;
  - legacy Minecraft mB/t <-> cubic metres/second;
  - volume flow <-> velocity through a cross-section;
  - volume flow -> mass flow;
  - dynamic pressure;
  - pressure-drop velocity;
  - signed orifice flow from delta-P.
- FlowState is the canonical sample:
  - material + phase;
  - velocity in m/s;
  - runtime density;
  - absolute pressure in kPa;
  - volumetric flow in m3/s;
  - turbulence;
  - source/domain marker.
- Runtime density belongs to the flow state rather than always coming from the
  reference material table, so compressed gases/steam can be represented.
- UniversalFlow is the compatibility facade. Different solvers remain bounded:
  - LocalWeatherField/Blizzard remains the atmospheric solver;
  - WaterDynamics remains the open-water/current solver;
  - PipeFlow remains the connected liquid-routing solver;
  - pressure/orifice math handles openings/ventilation.
- atmosphere -> wind:
  - ShipWind now consumes UniversalFlow atmospheric state;
  - existing weather/blizzard direction and gust logic are preserved as the
    current atmospheric solver rather than replaced by fake CFD.
- ocean/water -> current:
  - open vanilla water resolves as FlowState;
  - ocean samples inherit salt-water material and natural pressure;
  - submarine drift and idle ship drift consume UniversalFlow;
  - Water Wheel mechanical current is adapted to FlowState while retaining its
    conservative coherence/stability checks.
- connected conduit -> pipe flow:
  - each PipeBlockEntity remembers recently measured transferred throughput;
  - measured mB/t becomes m3/s and velocity through the real PipeSpec area;
  - pressure, material, direction and turbulence are exposed together;
  - HydraulicLoad exposes physical volume flow, mass flow and conduit velocity
    while preserving existing gameplay fields.
- steam/gas:
  - PressurePhysics can derive runtime gas density from pressure + Celsius;
  - UniversalFlow gas/steam conduit adapters convert mass flow into density,
    volume flow and velocity.
  - SteamNetwork keeps its existing amount-routing budget until conservation
    step 6, but no new steam-flow scale is needed.
- ventilation:
  - OpeningFlow moves gas from higher absolute pressure to lower pressure;
  - external wind projection and pressure-driven transfer share one opening;
  - RegionVentilationModel reports inward/outward m3/s, net transport and ACH;
  - weather is sampled once per region, not once per opening.
- thermal coupling:
  - live ventilation mass flow now becomes advective heat conductance (W/K);
  - open regions therefore cool/heat according to transported air mass, while a
    small passive exchange floor keeps quiet-air openings physical.
- /wayaroundphysics flow reports speed, vector, runtime density, pressure,
  dynamic pressure, turbulence and region ventilation/ACH.
- GameTests lock pressure-direction flow, unit conversion, dynamic pressure,
  pipe cross-section velocity, vanilla-water adaptation, explicit runtime
  density and pressure-aware steam density.

Important boundary:
- this step unifies the *language*, not the numerical solver at every scale.
  Full regional pressure-gradient weather/convection belongs to step 12.
- mass conservation across all representations and steam/liquid phase transfer
  belongs to step 6.

## 5.5 Environmental fields — IMPLEMENTED EARLY IN THIS BRANCH

A sparse 64x64 regional layer now turns the universal matter/temperature/
pressure/flow foundation into low-cost environmental memory before the later
full atmosphere step.

Implemented fields:
- humidity;
- cloud water;
- soil moisture;
- snow budget;
- smoke load;
- persistent air pollution;
- surface-water availability.

Simulation rules:
- only cells around players actively evolve;
- distant cells keep persisted summary state;
- no remote chunk is loaded for environmental simulation;
- open water + warmth + dry air + wind -> regional evaporation;
- humidity above a temperature-dependent saturation threshold -> cloud water;
- rain consumes cloud water rather than being a free timer effect;
- warm precipitation increases soil moisture/water availability;
- cold precipitation increases an abstract snow budget;
- snow is only materialized near players with a strict block-edit budget;
- no ordinary rain puddle blocks are simulated;
- snow melt feeds soil moisture;
- soil dries according to heat, humidity and wind;
- humidity/cloud water/smoke/pollution advect to the downwind neighboring cell;
- rain washes smoke and pollution;
- actual oceans/lakes periodically pull water-availability back toward loaded
  world geometry, so evaporation never deletes ocean blocks.

Cloud/weather integration:
- RegionalCloudClimate uses server environmental humidity/cloud water;
- nearby environmental cells are quantized to 7 bytes per cell and synced every
  two seconds to clients;
- client clouds use synchronized cloud water when available and retain the old
  deterministic loaded-world climate as a fallback;
- existing LocalWeatherField cloud bodies still migrate with their wind field;
- after precipitation removes cloud water, later samples reduce cloud size/
  storm density naturally rather than keeping an infinite rain reservoir.

Cross-system applications already connected:
- dynamic wildfire size/spread is suppressed by wet soil;
- EcologyPlantBlock flammability/fire-spread responds to soil moisture;
- SmokeVolumeEntity drifts through UniversalFlow and deposits regional smoke/
  pollution; rain shortens smoke lifetime;
- coal SteamBoilers emit pollution where fuel is actually burned;
- regional smoke/pollution/humidity tint the existing client fog, providing
  cheap haze/mist without global volumetric particles;
- exposed Pipework material memory receives humidity/rain/salt exposure, so
  coastal/wet machinery can corrode through the existing MaterialMemory path;
- /wayaroundphysics environment exposes all seven regional fields.

This deliberately does not model every raindrop, puddle, snowflake or smoke
particle. High-resolution manifestations are only created where they matter
visually/gameplay-wise.

Deferred to later physical steps:
- exact conserved water mass across ocean -> vapor -> cloud -> rain belongs to
  conservation/phase-change step 6;
- oxygen composition, flashover and true smoke-gas mixtures belong to
  combustion step 7;
- snow structural load belongs to structural step 8;
- fully coupled pressure-gradient atmosphere, terrain uplift/rain-shadow and
  deep-ocean thermohaline circulation remain step 12.

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
