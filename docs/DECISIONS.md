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
31. **Water is never cut into a wall.** Routes and pads stand on the water surface over lakes and rivers
    (a causeway, not a trench), aprons never dig below standing water, and a final pass banks every air cell
    that touches water or lava from the side or above with mud. Known-shape writes do not wake fluids, so
    without this a cut beside a lake left the water hanging as a sheer wall (seen in play).
32. **Meridian terrain is walkable rolling country.** Owner playtest: too hilly, too much climbing, too many
    trees to get around. Meridian now has its own noise settings: vanilla's terrain height (`offset`) is kept
    at 60 % of its relief about the sea-level offset (rivers, lakes and shores stay at the water line), peak
    jaggedness at 35 %, and `factor` is raised 30 % for smoother slopes. Measured on seed 91024 over 1024
    chunks of land (trunks filtered): height spread stdev 14.4 → 8.7, 95th-percentile ground y 111 → 93,
    steps needing a jump 4.1 % → 2.2 %, walls of 4+ blocks 0.7 % → 0.4 %, land fraction unchanged.
    The noise settings file is derived from Minecraft's own overworld settings (see ASSET_PROVENANCE).
    Tunables: `RELIEF`, `JAGGED`, `SMOOTH` in `tools/gen_worldgen.py`.
33. **Groves and clearings, and wildlife.** Spruce trees grow in groves (a broad noise field picks 2–7 per
    chunk in forest, 1–4 in woodland, vanilla is 10): canopy over land 54 % → 25 %. Biomes place more animals
    at generation (`creature_spawn_probability` 0.25, vanilla 0.1) with larger packs: rabbits, foxes and
    wolves everywhere, frogs on the moor and rivers, goats on the ridges. Still no farm animals. Mutant
    creatures are planned as SIGNAL's own entities (original models and textures), not imported.
34. **No server-wide spawning change** (supersedes the `doMobSpawning=false` part of #17). Creating the site
    used to switch mob spawning off for the whole world, which also stopped wildlife from spawning after
    generation. Now natural hostile spawns are refused only inside the site bounds (plus 8 blocks) while a
    campaign is active (`CampaignEvents#onSpawnPosition`); game rules are untouched.
35. **Low-contrast compound yard.** The pad used to pick mud, coarse dirt, gravel, andesite and podzol from
    7-block noise; in play it read as a bright orange checkerboard (coarse dirt is orange, and the five blocks
    span 59–136 in brightness). The yard is now weathered tuff hardstanding with broad (15-block) patches of
    packed earth and mud hollows, grass and podzol creeping in along the fence, a two-rut vehicle track from
    the gate to the door, and puddles only in the mud hollows, each held by the pad on every side.
36. **Cut faces are dressed as rock, verges as grass.** Re-graded aprons beside roads and pads used to be
    40 % coarse dirt from small-scale noise, and every cut showed bare stepped dirt. Verges are now grass with
    podzol drifts (coarse dirt only within two blocks of the edge), and a final pass (`Terrain#dressCuts`)
    turns soil left showing in the side of any sculpted column, or of the natural ground right beside one,
    into tuff/andesite/stone in broad bands. Measured with `tools/site_sweep.py` on seed 91024: bare dirt
    faces in the site 1455 → 655 (the rest is natural terrain outside the sculpted area).
37. **Levelled ground is final.** A pad's levelled columns are recorded; later pad aprons and route aprons
    blend around them instead of re-grading them. Before, two nearby pads chewed into each other, and a lane
    carved after a house could dig its apron into the house's footing.
38. **Bellwether houses: plots, then lanes, then houses.** The lane used to be carved after the houses, with a
    3-block apron that cleared 5 blocks above ground 2 blocks from house 1's front wall. Now each house gets a
    levelled plot, the lane is carved from the road fork past every front door (each leg pinned to the
    levels it joins), and only then are the houses drawn. Three variants instead of three clones: a 7-wide
    cottage with a vegetable garden, woodpile and rain butt; a 9-wide house; an abandoned one (no door, holed
    roof and ridge, broken windows, cobwebs, cold chimney). A stripped-log wall plate closes the eaves (the old
    roof left a 1-block gap at each corner). The road sign gives the measured distance (it said 300 m for
    about 50).
39. **Shore mud only at the shore.** `shore_mud` targeted grass and dirt anywhere, 6 disks per chunk, so about
    40 % of dry land in the Meridian Corridor was mud (measured: mud 39.6 %, grass 27.1 % of the natural
    surface around the site on seed 91024). Its target now also requires water within two blocks beside or
    just above the block, which keeps the intended mud banks on lakes and rivers.
40. **Service building metal is weathered, not exposed, copper.** Exposed copper renders salmon-pink in
    vanilla lighting and was the loudest colour on the site (parapet, awning, booth roof). Weathered copper
    reads as aged green-grey metal. The building also gets downpipes with a rain butt and a damp band along
    the foot of the front wall; equipment, bindings and the tested interior are unchanged.
41. **Headless site sweep** (`tools/site_sweep.py`): builds the site at world spawn on any number of seeds via
    a dev server (RCON bound to localhost, fresh random password per run), then reads the region files directly and reports hanging water, floating logs,
    bare dirt faces and steep steps, with a shaded top-down map per seed. It is the multi-seed check the
    client playtest could not give; it does not replace looking at the site in a real client.
42. **No surface stands on a void; falling terrain never blocks the site.** Found by the seed sweep. Seed 2: a
    road gravel block was placed over a natural air pocket beside a pond (the survey fills from the old ground
    up, not underneath it); gravel schedules its fall even on a known-shape write, so it dropped and left a
    road puddle hanging over the hole. `Terrain#shape` now closes any air, fluid or cover pocket in the 8
    blocks under a new surface, and road puddles also check the actual blocks around them. Seed 3: the guard
    refused the whole site over one `falling_block` entity (worldgen gravel falling as its chunk loaded);
    falling blocks are now treated as terrain. Code review also caught that the tower pad's apron can reach one
    column past the site edge; `shape` now ignores columns outside the bounds (every write there was already
    a no-op), so the new under-fill and cut dressing can never throw "write outside bounds".
43. **Refuse a site whose compound or village would stand in a lake.** Routes cross water as causeways, but on
    seeds 4, 6 and 7 world spawn sits on a lakeshore and the whole compound, lane and houses were built as
    islands in open water. Planning now refuses (new message `command.signal.scenario.too_wet`, telling the
    operator to stand with dry land to the east) when more than 20 % of the compound pad or 30 % of the house
    plots is standing water in the survey.
