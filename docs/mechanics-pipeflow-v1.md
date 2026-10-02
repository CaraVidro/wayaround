# Mechanics and Pipework: implementation

This revision follows `docs/design/project-master.txt` and `docs/design/machinery-engineering.txt`: installed material, visible operation, bounded simulation and discovery through experimentation. It builds on the three physical crushers and the existing Assembly and MechanicalTransmission implementations.

## Creative organization and presentation

The Industrialization creative tab is removed. Its items now belong to Assembly & Mechanics. Nexustor base, body, fingers and head belong to The Nexus. Registry IDs for existing machines and world feature switches stay compatible. The first accessory advancement is titled `if it fits...`.

Ordinary pipe tooltips no longer list flow, pressure and temperature ratings. Right-clicking a pipe never prints inspection data. Ordinary empty-hand status reports are removed from the steam engine, fan, wheel, generator, solar panel, sawmill, pulley and press. Manufacturing percentages and detailed component tradeoff text are confined to the advanced tooltip mode. Assembly construction feedback and deliberately technical engineering workbench interfaces remain.

Steam engine and Reforced Blaster renderers sample surrounding block/sky lighting rather than the dark center of an opaque logical machine block. The steam engine has a visible water sight glass and only turns its flywheel while generating steam. Actual boiling produces steam particles. The mechanical press emits dust and impact particles alongside its existing sounds.

## Exposed gears and combined sources

Small and large gears have 16 and 32 teeth, separate models and recipes. Placement face chooses the axle axis. Coaxial transmission preserves rotation. Meshing reverses direction and changes speed by the tooth ratio; torque changes inversely and path efficiency reduces power. Small gears can mesh diagonally in their axle plane, including upward/downward placements; a diagonal small gear mounts closer to a large neighbor so the teeth visually meet. Perpendicular adjacent gears redirect rotation between axes.

Multiple independent sources can feed the same shaft/gear train. Compatible source speeds combine power and torque. This lets two water wheels sustain a faster machine under load; it does not double unloaded RPM. A step-up gear produces higher RPM at lower torque. Same-direction wheels contribute at different speeds through a power-weighted shared output. Opposing active wheels jam a rigid axle; contradictory closed gear ratios also lock the transmission rather than generate energy.

Source discovery visits at most 192 loaded nodes, deduplicates source positions and applies load to actual transmission components. Wheel budgets are shared across consumers. The manual crank now also has a per-tick budget, fixing its prior repeated-power grant. Moving shaft and gear renderers follow the computed output ratio, including crank and pulley sources.

## Hollow pipes and staged ducts

The six existing pipe families now have actual inner faces and hollow collision. Their IDs and recipes remain compatible. Ordinary tubes expose their mouths; connected arms remain hollow.

Two new items represent a **section**, not a completed machine:

| Duct | Sections | Footprint across its axis | Length |
| --- | ---: | --- | --- |
| Giant water duct | 15 | 3 × 3 blocks | 3 blocks |
| Colossal duct | 50 | 5 × 5 blocks | 3 blocks |

The first placement consumes one section. Click the fastening point or any installed shell piece with the same item to add another. Every stage consumes exactly one real stack and expands the physical shell. Obstructions or unloaded chunks reject assembly before consuming material. Walls occupy actual blocks; the center is an open passage with no collision. The first click checks the complete shell footprint, and later stages recheck it. Existing interior blocks are not erased or replaced.

Sections preserve their manufacturing profiles and damage in saves and are recovered on dismantling. The controller stores the real stacks; shell blocks store their owner. Breaking a shell dismantles its own duct once and returns paid sections. No entity is created for moving fluid or an installed wall. Large duct sections connect end-to-end at three-block center spacing, and ordinary pipes can meet their mouths through an adapter at two-block spacing. Ducts may be horizontal or vertical.

## Valves and liquid movement

Place a pipe valve on any ordinary pipe or a completed duct. Look along the desired flow direction while installing it. Duct valves follow the duct axis. Empty-hand right-click turns it on/off with a visible wheel and click sound. Sneaking and clicking reverses its direction and closes it.

An open valve draws from the side behind it and routes to downstream pipes, following branches to their terminal mouths. A closed valve blocks its branch. The route visits at most 128 loaded controller nodes and does not load remote chunks. Fluid is budgeted in the valve tank; transit nodes keep only temporary visual wetness. Network cycles without an outlet do not drain the origin. Branch outlets are served in rotation.

Intakes accept real source blocks of liquids and NeoForge block fluid handlers. Simulated handler drains precede actual drains, and unlike fluids are not mixed. World sources are removed only when a complete bucket fits. Receivers implementing the same fluid capability accept liquid before it is removed from the valve budget.

Small open outlets emit the transported fluid as a spray, consuming the sprayed amount. The existing large water main and the two staged ducts place full world source blocks, only into free mouths. A blocked outlet retains fluid in the valve tank. The giant and colossal ducts can sample several source cells across their mouths, with bounded four-/sixteen-bucket pumping opportunities; a narrow intervening pipe limits the route's throughput. Fluids without a world block require a compatible receiver at a large outlet and remain buffered otherwise. Vanilla fluid updates, including infinite-water rules, remain vanilla.

The initial implementation accepts liquid fluids through the existing rated pipe families; it does not yet simulate gas compression, steam condensation, pump curves or hydrostatic pressure. Numeric pipe ratings remain internal design data.

Suction and discharge effects move in the flow direction. Water and lava have their own particles; other fluids use their block material. Internal duct particles are generated locally only for a player inside the passage or looking through a nearby mouth. They are not broadcast through an opaque pipe wall. Heat exceeding a pipe's material tolerance damages structural condition. Shared structural forces/damage also degrade it. Recently filled damaged pipes emit dripping particles, with buffered leaking liquid deducted where stored. These effects replace a numerical inspection HUD.

## Validation

Dedicated-server GameTests cover:

- empty frames, medium foundations, payload ownership, crusher conservation, real tool wear and mill save migration;
- source removal, bounded spray quantity, valve and tank persistence;
- fifteen-/fifty-section assemblies, obstruction rejection, hollow collision and exact dismantling recovery;
- closed downstream valves and route admission;
- water and lava source movement;
- inverse gear RPM/torque ratios, bevel axes and diagonal small gears;
- two distinct water wheels sharing an axle, and exhaustion of combined crank budgets.

GitHub Actions also builds the full mod and starts a client to verify registration and changed machinery/pipe assets. That smoke checks resource loading; it is not a pixel-by-pixel visual inspection of every assembled machine. Existing unrelated fish/media asset warnings are outside this revision.


## Mechanical Pump V1

Pipework now has its first dedicated rotational fluid machine.

The Mechanical Pump is deliberately a simple Assembly machine:

- the crafted block is only the iron/copper housing;
- one physical **Pump Impeller Cartridge** completes its internals;
- suction and discharge pipes are external infrastructure, not fake internal
  assembly slots;
- rotation is accepted from side or vertical shaft/gear networks;
- the pump owns a bounded 2000 mB buffer;
- suction can pull from direct tanks/sources or a bounded liquid pipe network;
- discharge reuses the existing PipeFlow outlet routing;
- actual transfer is limited by RPM, received mechanical power and the output
  network bottleneck;
- a closed valve on the discharge root blocks transfer;
- liquid-only routing rejects gas-only pipe families;
- the renderer exposes the impeller only after it is physically installed.

A world source is removed only as a full 1000 mB bucket into available pump
buffer. Low-RPM pumps may therefore gulp a bucket and meter it out over several
ticks; this avoids requiring 1000 u/t instantaneous flow while still conserving
the real source block.

The pump now uses the shared bounded `HydraulicLoad` model. Discharge
restriction creates remembered backpressure, suction starvation creates
cavitation, and both feed mechanical power/torque demand through
`MechanicalLoad`. Pump pressure is propagated through the bounded PipeFlow
route and can structurally damage pipe sections above their pressure rating.

This still deliberately stops short of a complete pump curve, hydrostatic head
solver or per-cell fluid dynamics.
