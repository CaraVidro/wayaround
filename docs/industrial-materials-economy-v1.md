# Industrial Materials & Economy V1

## Iron progression

Vanilla iron remains valid Minecraft progression.

`minecraft:iron_ingot` is interpreted as LOW industrial grade:
- useful;
- cheap;
- sufficient for starter machinery;
- worse initial part quality/resistance/fatigue than refined grades.

Industrial iron path:

raw iron
  ->
Ore Drill
  ->
iron drillings
  ->
Ore Drill second pass
  ->
iron dust
  ->
Air Separator + Mechanical Fan
  ->
iron concentrate
  ->
Mechanical Press
  ->
Refined Iron Ingot (MEDIUM)
  ->
Reforced Blaster
  ->
Precision Iron Ingot (HIGH)

The first ore-processing machines are intentionally buildable from low-grade vanilla iron. They are the bootstrap path out of low-grade iron.

Future advanced parts should increasingly require MEDIUM/HIGH iron:
- precision shafts;
- bearings;
- turbine rotors;
- pressure-vessel hardware;
- large generators;
- high-load supports;
- large plant machinery.

Steel remains a future distinct material family, not merely another name for HIGH iron.

## Mechanical airflow

Mechanical Fan is the first reusable rotation -> flow machine.

It exposes:
- airflow;
- pressure;
- direction;
- source position.

Blocking its outlet:
- reduces flow;
- raises pressure;
- reflects more mechanical load;
- increases wear/failure risk.

The Air Separator is the first real consumer of this airflow contract.

Future consumers can include:
- combustion air;
- mine ventilation;
- drying;
- smoke/steam transport;
- machine cooling;
- blowers/compressors.

## Coinage

The Mechanical Press can mint physical mineral coins.

Initial families:
- coal;
- copper;
- iron;
- gold;
- lapis;
- redstone;
- quartz;
- amethyst;
- emerald;
- diamond;
- netherite.

Each coin has a material reference value.

That value is descriptive only.

The mod does NOT enforce a global exchange rate.

A server, city, faction or settlement may:
- accept only certain coin families;
- create its own exchange ratios;
- peg one coin to another;
- reject a material;
- pay different professions in different standards.

This lets player economies emerge instead of being centrally scripted.

## Jobs and commissions

Large Assembly structures should create useful player specialization naturally.

Examples:
- shipbuilder;
- mechanical engineer;
- plant designer;
- maintenance technician;
- miner;
- ore processor;
- machinist;
- mint operator;
- architect;
- contractor.

A player may commission another player to build a Nau, saw line, turbine train, hydro plant or other machine and pay using whatever currency their settlement accepts.

The mod supplies:
- physical materials;
- machine rules;
- coins;
- Assembly diagnostics;
- optional blueprints.

The server supplies:
- wages;
- prices;
- contracts;
- reputation;
- local currency culture.

## Blueprint philosophy

A blueprint is knowledge, not an invisible multiblock lock.

Beginner route:
- obtain/use a known machine model;
- place/assemble parts in the documented arrangement;
- receive predictable behavior.

Expert route:
- understand functional ports/parts;
- design a different geometry;
- satisfy the same structural/mechanical/flow requirements;
- potentially create a better or worse machine.

AssemblyGraph validates physical roles and load paths rather than demanding one exact sculpture.

This makes custom engineering valuable in multiplayer.

A professional builder can sell:
- the finished machine;
- construction labor;
- a custom design;
- maintenance;
- optimization;
- inspection.

## Current implementation

Implemented:
- LOW / MEDIUM / HIGH iron grade foundation;
- vanilla iron ingot reads as LOW;
- refined iron reads as MEDIUM;
- precision iron reads as HIGH;
- Assembly part initialization reacts to iron grade;
- Ore Drill raw iron -> drillings -> dust;
- Mechanical Fan with flow/pressure/blockage/load;
- Air Separator dust -> concentrate using real fan airflow;
- Mechanical Press concentrate -> refined iron;
- Reforced Blaster refined -> precision iron;
- Mechanical Press mineral coin minting;
- material-reference coin tooltip;
- iron-grade tooltip;
- fan blockage participates in localized Assembly failure.

Near-term:
- better final models/animations;
- localized Assembly failures for drill/press/separator;
- quality-aware industrial part recipes;
- optional blueprint/design layer;
- turbine using the same flow contract in reverse.
