# Implementation status

Last updated 2026-10-11. Claims are separated by how they were verified.

## Platform

NeoForge 1.21.1 (21.1.256), Java 21, moved from Forge 1.20.1 at the owner's request for Iris shader
support (decision #22). The milestone-one gameplay was ported unchanged and re-verified.

## What happens in-game now

- **World type "SIGNAL: Meridian Corridor"** on the Create World screen: Minecraft terrain with six
  original biomes (wet old-growth forest, logged woodland, open moor, stony ridge, river, reservoir), an
  overcast grey-green sky, haze, peat-dark water, olive foliage, drifting ash, dead snags, stumps, mud and
  scree; no flowers, farm animals, villages or mineshafts. Fog closes in inside these biomes and more in rain.
  The land is gentler than vanilla (relief, peaks and cliffs reduced; decision #32), spruce grows in groves
  with clearings between them, and the woods hold rabbits, foxes, wolf packs, frogs and ridge goats (#33).
- **`/signal scenario create bellwether`** surveys the ground and builds the Bellwether site *into* the
  terrain east of the operator (about 176 x 144 blocks). It refuses, with a message, if the compound
  or the village would stand in a lake (decision #43):
  - a winding maintenance road graded to the land (whole-block heights, slab ramps at steps, puddles,
    mud shoulders, blended verges), a power line of wooden poles with sagging chain cable, a burnt-out car;
  - an abandoned rail line on its own graded bed with ascending rails, a level crossing, and a derelict
    boxcar;
  - graded slopes finish as grassy banks over tuff and andesite faces, not bare dirt terraces (#36);
  - the fenced compound on a levelled pad blended into the slope: weathered tuff hardstanding with packed
    earth, mud hollows holding puddles, weeds along the fence and wheel ruts to the door (#35); chain-link
    on concrete posts with gaps, lit gate posts, checkpoint booth, the weathered service building (tar roof,
    weathered metal parapet, downpipes and rain butt, vines; #40), pump stand and header tank, container,
    collapsed shed, truck, fire barrel with smoke, lamp posts;
  - relay tower R4 on the high ground: lattice with X-bracing, two platforms, a caged ladder on a solid
    riser through both hatches to a railed deck, the canted antenna and a beacon bound to the relay;
  - Bellwether: a lane off the road runs past three houses on levelled plots: a cottage with a vegetable
    garden, woodpile and rain butt, a larger house, both lit with chimney smoke, and an abandoned one with a
    holed roof and no door (#38).
- The milestone-one repair loop is unchanged: fuse from the cabinet, panel preview and apply within the
  6-unit generator, fit F1 while isolated (uses exactly one), close G1's isolator, lamps/gauge/tank respond,
  Nia's acknowledgement once, radio transcript replay, persistence across restarts.
- Operator: `/signal scenario status` (now also reports facility and tower positions),
  `/signal scenario reset bellwether confirm` (rebuilds the facility, new timeline).

## Verification record (NeoForge build; 2026-10-10, re-run and extended 2026-10-11)

| Check | How | Result |
| --- | --- | --- |
| Build | `./gradlew build` (OpenJDK 21, Linux) | Pass; JAR `build/libs/signal-0.2.0.jar` |
| Domain unit tests | JUnit, 21 tests (allocation, repair, isolator, stale revision, radio once, history) | 21/21 pass |
| Integration GameTests | `./gradlew runGameTestServer`: the full site built by the real code on synthetic hilly terrain, then the full repair loop through the real server handlers; every binding inside the bounds; tower above the compound; ladder present on a solid riser at every height to the deck; both lived-in houses' doors on their footings with ground in front and no footing over air; plus guard (now also: a falling gravel block is terrain), schema refusal and projection safety | 4/4 pass |
| Worldgen | Production NeoForge 21.1.256 server with the JAR, `level-type=signal:meridian`: all six biomes located, no worldgen errors, no mineshafts | Pass |
| Graphical playtest | Real dev client, software OpenGL (Mesa llvmpipe) under Xvfb, driven by xdotool, in a server-generated Meridian world: created the site from chat, viewed road, rail, compound and tower, and the view from the deck in rain and at dusk, climbed the ladder with held keys from the ground through both hatches to stand on the deck (y 106.0) | Pass; screenshots in `docs/screenshots/` |
| Site on eight seeds | `tools/site_sweep.py` (dev server over RCON, region files read back), seeds 91024 and 1–7, site built at world spawn | 2026-10-11: 91024, 1, 2, 3, 5 build with 0 hanging water and 0 floating logs; 4, 6, 7 (spawn on a lakeshore) are refused as too wet. The previous `main` on the same seeds: seed 2 left a road puddle hanging over a hole, seed 3 was refused over a falling gravel block |
| Houses and facade | Front and side elevations rendered from the saved world (seed 91024) | Eaves closed; doors, windows and chimneys in place; the abandoned house's roof is holed; parapet and downpipes as built. Not yet seen in a game client |

Problems found by the seed sweep and fixed (2026-10-11): about 40 % of Meridian's dry land was mud (shore
mud applied everywhere, #39); a road gravel block fell through a natural void (#42); a falling gravel entity
refused the whole site (#42); the lane to the houses was carved after them and cut into house 1 (#38; a
GameTest now fails on the old order); the yard read as an orange checkerboard (#35).

Problems found in the playtest and fixed: rail line built as a flat 25-block embankment wall (now follows
the land), vertical cut walls beside routes (now blended aprons), slab "patio" road surfaces (now slabs
only as ramp steps), noisy confetti ground (now noise patches), bright toy-like palette (now tuff/andesite,
tar roof, rust), tower placed below the compound (now takes the highest ground north of it), shoreline
sand (now mud/gravel), mineshafts under the site (biome tags removed), natural lake-floor magma blocking
construction (guard now ignores non-block-entity blocks below ground).

## Not verified

- **Shaders and Distant Horizons**: no GPU in the build container. `docs/GRAPHICS.md` lists the stack;
  you are the judge of how it looks.
- **Sound** (no audio device), **GPU performance**, **Windows**, **multiplayer**.
- The site was built headlessly on eight seeds (above) but viewed in a game client on one only (91024,
  before the 2026-10-11 changes). Very steep anchors can still produce long ramps or deep cuts; watery ones
  are refused.
- **Bellwether's houses and the 2026-10-11 surface changes have not been seen in a game client.** They were
  checked from the saved world (top-down maps, elevations) and by GameTests (both lived-in doors on their
  footings with walkable ground in front, no footing over air), not by eye in-game.
- The repair loop was played by hand on the Forge build; on NeoForge it is verified by the GameTest that
  drives the real handlers, not by hand.

## Known limitations

- The relay can be powered but reaches no one: antenna alignment has no control yet (M2). The deck sign
  marks where it will be.
- One participant per campaign. Reset rebuilds only the facility (sculpted terrain cannot be re-surveyed).
- The create-world screen shows the vanilla "experimental settings" warning for the SIGNAL world type.
- Sign and book text are English literals.

## Planned

| Milestone | Content |
| --- | --- |
| M2 | Antenna alignment at the tower deck mount, route terminal (local open vs authenticated regional), radio channels/tuning/transcript screen, archive frequency, data-driven messages. |
| M3 | Narrative schedule, Rowan's receiver promise, renegotiation, field battery window, Nia training and delegation. |
| M4 | Home and Rowan's receiver scene; three full branches. Bellwether's houses are where this will live. |
| M5 | Evidence-grounded transmission. |
| M6 | Original ambient audio, more sites, original textures for vanilla building blocks (resource pack). |
