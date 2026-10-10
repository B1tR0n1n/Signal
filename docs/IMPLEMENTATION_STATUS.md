# Implementation status

Last updated 2026-10-10. Claims are separated by how they were verified.

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
  terrain east of the operator (about 176 x 144 blocks):
  - a winding maintenance road graded to the land (whole-block heights, slab ramps at steps, puddles,
    mud shoulders, blended verges), a power line of wooden poles with sagging chain cable, a burnt-out car;
  - an abandoned rail line on its own graded bed with ascending rails, a level crossing, and a derelict
    boxcar;
  - the fenced compound on a levelled pad blended into the slope: chain-link on concrete posts with gaps,
    lit gate posts, checkpoint booth, the weathered service building (tar roof, rust parapet, vines),
    pump stand and header tank, container, collapsed shed, truck, fire barrel with smoke, lamp posts;
  - relay tower R4 on the high ground: lattice with X-bracing, two platforms, a caged ladder on a solid
    riser through both hatches to a railed deck, the canted antenna and a beacon bound to the relay;
  - Bellwether: three lived-in houses with lit windows and chimney smoke, reached by a lane off the road.
- The milestone-one repair loop is unchanged: fuse from the cabinet, panel preview and apply within the
  6-unit generator, fit F1 while isolated (uses exactly one), close G1's isolator, lamps/gauge/tank respond,
  Nia's acknowledgement once, radio transcript replay, persistence across restarts.
- Operator: `/signal scenario status` (now also reports facility and tower positions),
  `/signal scenario reset bellwether confirm` (rebuilds the facility, new timeline).

## Verification record (2026-10-10, NeoForge build)

| Check | How | Result |
| --- | --- | --- |
| Build | `./gradlew build` (OpenJDK 21, Linux) | Pass; JAR `build/libs/signal-0.2.0.jar` |
| Domain unit tests | JUnit, 21 tests (allocation, repair, isolator, stale revision, radio once, history) | 21/21 pass |
| Integration GameTests | `./gradlew runGameTestServer`: the full site built by the real code on synthetic hilly terrain, then the full repair loop through the real server handlers; every binding inside the bounds; tower above the compound; ladder present on a solid riser at every height to the deck; plus guard, schema refusal and projection safety | 4/4 pass |
| Worldgen | Production NeoForge 21.1.256 server with the JAR, `level-type=signal:meridian`: all six biomes located, no worldgen errors, no mineshafts | Pass |
| Graphical playtest | Real dev client, software OpenGL (Mesa llvmpipe) under Xvfb, driven by xdotool, in a server-generated Meridian world: created the site from chat, viewed road, rail, compound and tower, and the view from the deck in rain and at dusk, climbed the ladder with held keys from the ground through both hatches to stand on the deck (y 106.0) | Pass; screenshots in `docs/screenshots/` |

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
- The site on many different seeds: it was built and viewed on **one** seed (91024), plus synthetic hilly
  terrain in tests. Very steep or watery anchors can still produce long ramps or deep cuts.
- **Bellwether's houses were not seen in the client**: the camera positions tried landed inside a hillside.
  They are built by the same code path the GameTest exercises, but their look is unchecked.
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
