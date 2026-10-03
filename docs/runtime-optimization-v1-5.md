# Runtime optimization after the cloud revamp

Based on `e95b30e7`, retaining the dense downward shadows and Kraken events.

Implemented:

- Cloud face height shading and coherent spatial patches are cached with the
  exposed mesh faces. Whole-cloud breathing is calculated once per cloud per
  frame instead of once per face. Internal lightning remains dynamic; its empty
  event list returns immediately. The normal no-flash path no longer performs
  per-face trigonometry. Vertex colors, cloud geometry and animation remain.
- Shadow rendering merges adjacent tiles only when they have exactly equal
  height and interpolated integer opacity. Boundaries, gradients and terrain
  steps remain separate. A uniform 113-block row needs one quad instead of 113;
  the actual reduction depends on terrain and opacity, with no guaranteed FPS.
  Persistent receivers, downward coverage and tick sampling limits remain.
- Weather advection is computed once per query instead of per candidate cell.
  Conservative radius checks reject distant candidates before biome/water-cache
  adaptation. Point sampling uses the actual maximum cloud/warning influence to
  bound its search grid. The 1.04 maximum regional size factor is included.
- Dynamic light returns immediately for fully lit blocks or no active sources.
  Per-source rejection accounts for existing brightness before square roots.
  Section invalidation follows the actual light bounding box instead of always
  dirtying a 3×3×3 section cube. Existing immutable worker-thread snapshots,
  source limits, update cadence and dirty-section budgets remain.
- Debris distance checks use coordinates directly, avoiding two temporary Vec3
  objects per candidate block per frame. Group fall behavior is unchanged.
- Existing `/wayperf` profiling gains Cloud shadow sampling/rendering sections;
  cloud render timing now begins only at its actual render stage.

Validation:

`python tests/weather/weather_equivalence.py` compares 3,000 randomized sample
and nearby-cell queries with the pre-optimization revision, across null/default
and stubbed regional climates, ranges, negative/positive coordinates and old
world times. Results are identical. Point-sample adaptation calls fell from
363,000 to 17,392 in that workload (about 95% fewer); this is a call-count
comparison using stubs, not a measured overall speedup or actual biome test.

Standalone cloud math tests and git diff --check pass. Full build, client and
dedicated-server startup are validated in CI. In-world visual QA and actual FPS
still need comparison on the same world/camera/settings. Use `/wayperf start 30`
then `/wayperf report` for instrumented sections in singleplayer. Dedicated
servers cannot measure a remote client's GPU/render timing through that report.
Existing broad hydraulic pump/lift test failures are documented separately.

Follow-up polish:

- Shadow opacity is capped at 56/255 (about 22%) instead of 144/255 (56%).
  The same continuous footprint and fades remain, with roughly 61% less opacity.
- Shadow footprint queries return immediately in the fully covered interior or
  beyond the outside boundary, avoiding square roots there. Overlapping coverage
  stops at saturation; invisible receivers skip render-edge math and zero-opacity
  tiles skip redundant easing. Departing coverage still fades smoothly to zero.
- Per-cloud color bias is evaluated once per cloud/frame instead of per face.
  This preserves the tint while removing repeated hash calculations.
