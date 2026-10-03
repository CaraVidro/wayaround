# Branch junctions and nearby speech

Implemented branch behavior:
- Existing tree-segment blocks gain short wooden arms where off-axis face neighbors are branches or logs. This joins rising limbs, right-angle bends and forks without bridging air or leaves. Roots retain their terrain-following geometry.
- Selection and collision use the same arm bounds as the mesh. Shapes update from the adjacent wood; breaking a neighbor removes the joint. Existing trees gain the geometry when their chunk mesh rebuilds, without terrain replacement or new block-state properties.
- Six neighboring cells are inspected during shape evaluation/chunk tessellation. No remote chunks are requested. Cached shape combinations and a 512-entry mesh cache share repeated junctions; existing vegetation LOD/culling remains active. No moving entities or per-frame tree scan is added.

Implemented voice behavior:
- Normal speech stays within the existing 48-block, same-dimension recipient limit and excludes the microphone sender. Volume is full within three blocks, falls smoothly with distance, and reaches silence at 48 blocks.
- Attenuation is applied separately to each recipient's signed 16-bit PCM frame on the server. Raw capture stays unchanged for black-box recordings, parrots and broadcasts. Projected Tukuna speech attenuates from its host, preserving host muting and sender feedback exclusion.
- The packet format remains unchanged. This is distance-dependent mono playback; directional stereo panning is not implemented. Recorded/radio/TV playback keeps its existing routes.

Validation: standalone PCM tests cover smooth falloff, range limits, signed sample polarity and capture immutability. Dedicated-server branch tests inspect uninterrupted collision paths across every axis/thickness and verify removal, wood-only links and root compatibility. Existing client startup/resource and Kraken animation checks run alongside these tests. In-world junction appearance, camera motion and live microphone loudness still require gameplay inspection.
