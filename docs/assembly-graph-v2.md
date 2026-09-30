# Assembly Graph V2

## Purpose

Assembly is not a catalogue of multiblocks. It is the physical language used to build machines.

The same graph must be able to describe one hand-built tool, one saw, one water wheel, one generator train, one hydroelectric plant, one coal plant, and future nuclear/solar/industrial systems.

The machine defines what it does. Assembly defines how its parts support, connect, carry load, wear and fail.

## Direct support rule

`AssemblyPartNode.supported` means direct support into the world/foundation. It does NOT mean "connected to another part".

Example:

water-wheel paddle
    |
    | nail / contact
    v
frame
    |
    | support
    v
world

The paddle is not directly supported. Its load has to travel through the nail/contact to the frame and finally to the foundation.

## AssemblyGraph

`AssemblyGraph` consumes `AssemblyPartNode` and `AssemblyConnection` and builds a real topology.

It can answer which parts have a route to a foundation, which connection a load must cross, how much load reaches every part/connection, local stress, unsupported load, hottest part and hottest connection.

A machine can therefore look healthy on average while one bad bearing, bolt, belt or support is overloaded.

## Load routing

Loads are distributed according to part `loadShare`.

For every loaded part the graph routes load toward the strongest/cheapest available path to a directly supported node.

Connection type and condition affect carrying capacity. Current connection families include support, shaft, bearing, fastener, gear, belt and contact.

Weak connections can still work while concentrating stress.

## Large-structure scaling

The graph precomputes support routing with one multi-source search from every foundation. It does not search the entire structure separately for every part.

This is important for future plants with hundreds of parts.

## Modular composition

`AssemblyGraph.Builder.addMachine(prefix, machine)` imports a machine as a graph fragment.

Prefixes keep local IDs separate:

- `turbine/shaft`
- `generator/shaft`
- `boiler/frame`
- `condenser/frame`

Explicit cross-module connections can join fragments.

A future power plant therefore does not need to become one enormous BlockEntity. Different modules can keep their own simulation while a larger structure composes their Assembly fragments.

## Machine families

### Extender machines

These deepen a vanilla material/process instead of replacing Minecraft.

Examples include a saw/sawmill, crusher and mechanical press. A saw can accept ordinary Minecraft logs while blade alignment, RPM, bearings and frame quality affect throughput, jams and wear.

### Native industrial machines

These introduce processes vanilla does not already have: steam boiler, turbine, large generator, compressor, pump, furnace/coal handling machinery, future nuclear components and future solar equipment.

They still use the same Assembly part/connection/load rules.

### Macrostructures

A macrostructure is not a single placeable machine block. It is a collection of modules connected physically and functionally.

A hydroelectric plant can contain reservoir/dam support, intake gate, penstock, turbine, shaft, generator and electrical output.

A coal plant can contain coal handling, furnace, boiler, steam line, turbine, generator, condenser and pumps.

A bad subsystem should create a concrete local problem instead of a generic "multiblock invalid" state.

## Cooperation

Every structure must remain possible in singleplayer.

Multiplayer becomes easier naturally because Assembly permits parallel work: fabrication, foundations, shaft alignment, piping, inspection and maintenance can happen at the same time.

The advantage comes from parallel physical work, not arbitrary multiplayer-only bonuses.

## Failure philosophy

Prefer "bearing 2 is carrying 148% rated load" over "multiblock invalid".

Prefer "west support disconnected, load rerouted through east support" over "structure broken".

Prefer cascading consequences:

weak fastener -> load reroutes -> neighbor stress rises -> vibration grows -> alignment worsens -> efficiency falls -> local failure

## Current foundation

Implemented now:

- composable AssemblyGraph;
- direct support semantics;
- path-to-foundation evaluation;
- load distribution by part share;
- part and connection load/stress;
- unsupported load;
- hottest part/connection diagnostics;
- prefixed graph fragments for future multi-module structures;
- support routing precomputed once for large graphs;
- AssemblyEngine snapshots include localized stress in critical-state evaluation;
- water-wheel paddles route support through frame connections instead of pretending each paddle is directly world-supported.

## Next architecture steps

The next useful layers are explicit inter-module ports, localized wear/failure dispatch from graph hotspots back into real parts, live graph diagnostics in the Assembly Guide, one small extender machine to validate the API, and one multi-module power test rig before attempting a full plant.

The first full macrostructure should reuse these layers rather than create special-case multiblock validation.
