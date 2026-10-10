# Decision log

Reversible choices made during implementation. Newest last.

1. **Forge 47.4.10 (recommended), not 47.4.26 (latest).** Rechecked the official promotions feed on
   2026-10-09: recommended 47.4.10, latest 47.4.26. No documented fix in a later build was needed.
2. **Project created from the unmodified official MDK.** The repository was empty; nothing existed to extend.
   The MDK's README/LICENSE/CREDITS are kept under `docs/mdk/`.
3. **Package `io.github.b1tr0n1n.signal`, mod id `signal`.** As suggested by the handoff.
4. **Authored shared bus, not a graph solver.** `BellwetherBus` hangs every load off one generator, fuse
   and isolator. It is named for what it is. A real topology arrives with a second site.
5. **Invalid allocations are prevented, never tripped.** Apply is refused when demand exceeds the
   6-unit rating; the preview explains the shortfall.
6. **Breakers may be set while isolated.** Configuration is separate from power; services start when the
   isolator closes. The isolator refuses to close onto a damaged fuse (so "repair while live" is
   unreachable in play, but the rule is still enforced and unit-tested).
7. **Generator controls:** use = close isolator (or inspect when live); sneak-use = isolate. Labelled on
   the in-world sign next to it.
8. **Antenna alignment exists in the domain but has no player control yet (M2).** A powered relay shows
   "powered, no route". The relay therefore cannot reach anyone in M1, which is honest to the canon
   ("knocked off its bearing").
9. **Campaign storage:** one `SavedData` (`signal_campaign`) in the Overworld's data storage, regardless of
   which dimension the site is built in. Schema version 1. Unknown schemas or unreadable data are
   refused with a logged message and *never written back*.
10. **Block entities hold only a node id.** Service state lives in the campaign and is projected onto block
    states (one direction). Projection skips unloaded chunks (no force loading) and refuses to touch a block
    whose node id does not match its binding.
11. **Service lamps ignore redstone.** A vanilla redstone lamp would create a second source of truth.
12. **Header tank = vanilla cauldron** whose water level mirrors pump pressure (0-3). Safe, visible, and
    needs no fluid simulation.
13. **Radio is text-first** (subtitled chat lines + a click cue). Messages are delivered once, shown live
    once (or on next login if delivered while offline), and the transcript replay is read-only.
14. **Radio content is hard-coded in `RadioCatalog` for M1.** The data-driven format (handoff §25) is
    planned; it is not claimed until its loader and validation exist.
15. **Scenario bootstrap is procedural** (`BellwetherScenario`), seeded, inside fixed bounds
    (48 x 28 footprint, 3 below to 30 above the anchor). Known-shape block flags avoid cascading updates;
    a final pass connects bars, panes, fences and walls.
16. **Occupancy guard:** natural terrain (soil, stone, sand, gravel, ores, vegetation, trees, water, snow,
    ice) may be cleared; any other block, any block entity, or any non-mob entity in the bounds refuses
    creation. Recommended use: a fresh superflat test world.
17. **Scenario rules are applied only by `/signal scenario create`** and announced in chat: participant set
    to Adventure mode, spawn point set to the road start, a handheld radio given, hostile mobs inside the
    bounds removed, `doMobSpawning=false` (server-wide game rule), rain for 6000 ticks in the Overworld.
    Installing the mod alone changes nothing.
18. **In-world sign and book text is English literal for M1.** UI, radio and messages use translation keys.
19. **Panel screen does not pause single-player.** Correctness never depends on pausing, and inspecting does
    not advance any narrative clock (there is none yet).
20. **GameTests ship in the main source set** (Forge only loads them when the `signal` gametest namespace is
    enabled, i.e. in `runGameTestServer`/dev runs). They use Forge `FakePlayer` because the vanilla mock
    player cannot join a Forge 1.20.1 server.
21. **Scenario equipment is unbreakable outside Creative** (`strength(-1)`), so the authored site cannot be
    destroyed accidentally.
22. **Platform moved to NeoForge 1.21.1 (owner decision, 2026-10-10).** The owner asked for the best
    possible graphics on an RTX 5090. On Forge 1.20.1 the shader loader is an unofficial port (Oculus) and
    the strongest ray-traced packs target Iris. Options presented: stay (A), NeoForge 1.21.1 + Iris (B),
    Fabric + Minecraft 26.x with alpha hardware ray tracing (C). Owner chose **B**. Ported from the official
    NeoForge 1.21.1 MDK; all tests re-run.
23. **Meridian Corridor world type** (`signal:meridian`): vanilla overworld terrain noise, six original
    biomes. Reuses vanilla placed features by reference; adds snags, stumps, mud/gravel/scree patches and
    turns shore sand to mud. No flowers, farm animals or villages. Not tagged `#is_forest`/`#is_river`, so
    no mineshafts. Generated by `tools/gen_worldgen.py`.
24. **Client fog** eases in inside `#signal:meridian` biomes (far plane 128, 72 in rain). Shader packs
    override it.
25. **The site is built into the terrain**, not on a cleared box: a survey records natural ground first;
    the compound sits on a median-height pad with an 8-block blended apron; roads and paths are splines
    graded with half-slabs; the rail line gets 1:1 embankments/cuttings; the tower takes the highest ground
    in a window north-east of the compound. Site footprint 176 x 144; vertical bounds come from the survey.
26. **Facility frame.** The tested milestone-one interior is unchanged and drawn in a movable frame; the
    campaign stores the frame (schema 2). `reset` rebuilds only the facility, because sculpted terrain can no
    longer be told apart from natural ground.
27. **Guard below ground:** more than 3 blocks under the surveyed surface only block entities count as
    occupied, so caves, lava lakes, geodes and lake-floor magma never block construction; a player's buried
    base is still detected by its chests and furnaces. At and above the surface the natural allow-list applies.
    Refusals name the first offending block.
28. **Tower R4** is 34 m to the deck with a caged ladder on a solid concrete riser, hatches through both
    platforms (cage opens where you step off), a railed deck with one section missing, and the canted antenna.
29. **Shaders are supported, not bundled.** See `docs/GRAPHICS.md`; nothing third-party ships in the JAR.
30. **World-generated block entities never block the guard.** Mob spawners, trial spawners, vaults and
    containers still holding their unrolled loot table (an unopened dungeon or buried chest) are exempt at any
    depth, because survival play cannot place them. An opened loot chest looks like a placed one and still
    counts. Found in play: an unopened dungeon chest 40 blocks under a hill refused the whole site.
