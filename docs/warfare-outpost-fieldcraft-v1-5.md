# Warfare Outpost: implemented field equipment

Built on the living-colony branch, preserving its changes. Mundane equipment uses the existing WarProjectileEntity/Infinity pipeline, physical chair-style armor-stand seats, media photo/VHS capture, smoke LOD and bounded dynamic lighting.

## Equipment and operation

- Contact mine: a living entity's physical touch detonates and consumes it. Placement gives 30 ticks to leave. There is no owner immunity after that grace period. Spectators and Entity Spectate ghosts do not trigger sensors.
- Standalone mine: touch latches the actual entity. Looking around is safe; movement beyond .04 block or changing hotbar slot/main/offhand item detonates. Victim, location and held-item identity survive saves. An unloaded/offline victim is not force-loaded; checking resumes when present.
- Spike barrage: damages touching living entities, with a ten-tick contact cadence and slowdown. A solid low shape lets the obstacle hinder movement too.
- Fixed machine gun: crafting makes the stand. Install a separate Gatling barrel assembly, load iron nuggets (one per shot, storage 256), then interact to mount. Hold attack to shoot; sneak to leave. Aim is limited to 65 degrees each side and 35 vertically. Real War Without Reason bullets preserve Infinity/reflection behavior. Heat blocks firing at 80 and cools one unit per tick. Removing the stand returns the installed barrel and remaining ammunition once and releases its operator.
- Camo: apply to a field device. It samples the block immediately below and draws a thin terrain cover over mines/charges or strips around larger objects/spikes. Triggers and collision remain functional. Shears remove the cover, returning its item; normal removal also returns it once.
- Observation drone: deploy the physical item, interact with a controller to link it, then use the controller to pilot. The camera moves; the player remains physical and vulnerable at the controls. Existing media capture records the drone view: P photo, V start/stop VHS (keys remappable), consuming normal photo paper/film. It is available in Media and Warfare Outpost.
- Impact drone: same control system, available in Warfare Outpost. Attack while piloting launches it in its current view direction and disconnects the pilot. Swept block/entity contact detonates and consumes it. A flight timeout or unloaded chunk boundary cancels armed flight rather than creating an explosion in an unobserved region.

Drones use WASD, jump to rise, sprint to descend, sneak to exit. Maximum radio range is 96 blocks; they never load remote chunks to extend the link. Battery is five minutes, retained on recovery/save. Redstone near the physical drone replenishes one minute per item. Empty-hand sneak interaction recovers an idle owned drone. Damage destroys a drone and returns only its chassis; launched drones detonate. Dropping the controller, damage, dimension change, logout, distance or movement of the pilot ends control. The client does not send player movement while piloting; the server independently authorizes owner, session, dimension, controller, range, feature switch and finite orientation, with a two-tick input rate limit.

## Five additional mechanics

1. Passage alarm: an eight-block facing beam ends at real solid collision; a living intruder crossing it rings and emits redstone strength 15 for two seconds. Owner is exempt. Sensors share 64 scans per level/tick.
2. Field barricade: physical low cover blocks the same collision rays as ordinary ballistic impacts; destructible, recoverable and camouflaged with the same net.
3. Remote charge: link the controller to an owned physical charge, then sneak-use the controller within 96 blocks to detonate it. Dimension/owner/loaded-position checks are server-side. No automatic targeting.
4. Smoke canister: thrown object activates after landing/fuse, creates a twenty-second local visibility screen, limited particles and existing coarse smoke volumes. It does not create terrain or pretend to block bullets physically.
5. Flare: thrown, one-minute marker with fire particles and the existing four-source dynamic-light budget. It expires without leaving invisible light blocks.

All equipment has distinct survival recipes. Propellers/chassis and the installed gun barrel are physical inventory components. Enabled mechanics respect the existing Warfare and Media world switches; registrations remain stable. A disabled Warfare switch prevents mines/charges/guns/alarms and expires field canisters; observation drones depend on Media.

## Limits and verification

At most 16 drones are deployed near a launch area, 16 field canisters near a throwing area; shared server ceilings allow 64 alarm scans, 24 effect emissions and 64 shot attempts per level/tick. Each gun also checks the existing 128 nearby projectile limit. Routes/flight do not force-load chunks. Owner, battery, launched motion, camo, ammo, heat, mine state and charge binding save. Active pilot sessions are deliberately transient and cleared on logout/server stop. Network protocol is 30, requiring matching clients and servers.

Focused GameTests cover real explosions, stationary pressure, item changes, cover/save state, contact damage, component/ammo conservation, mounting/firing/ejection, alarm reset, cover collision, remote charge consumption, camera/body separation, spoof rejection, saved battery, launched wall impact, range and temporary screens/lights. Automated client boot verifies renderer/mixin application and missing model/texture warnings. In-world visual balance, sound, packet latency and crowded multiplayer play remain manual QA; no measured FPS improvement is claimed.
