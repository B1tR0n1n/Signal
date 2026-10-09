# Implementation status

Last updated 2026-10-09. Claims are separated by how they were verified.

## What happens in-game now (M0 + M1)

- `/signal scenario create bellwether` (operator) builds the Bellwether relay site at the caller's feet
  after an occupancy check, moves the participant to the road start in Adventure mode with a handheld radio,
  sets their spawn there, disables mob spawning and starts rain, and announces each of those changes.
- The site: forest maintenance road with a drainage channel, a service sign, an abandoned maintenance
  truck, gravel yard with lantern posts, one service room, an exterior pump stand with a header tank, and
  a 27-block lattice relay tower as the landmark, inside spruce forest.
- The service room: generator G1, distribution panel DP-1, pump controller P1 (pressure gauge), regional
  relay controller R4, local transmitter desk, service lamps, spare-fuse cabinet with two fuses, work bench,
  Nia's bedroll and heater, maintenance log on a lectern, chain cable tray, hanging lanterns.
- The repair loop: read state → take a fuse → set breakers at the panel (preview explains totals and
  shortfalls) → fit F1 (only while isolated; consumes exactly one) → close G1's isolator (refused onto a
  damaged fuse) → services run. Over-capacity and stale applies are refused.
- Observable results: block textures change (generator switch, panel fuse carrier, pump needle, relay
  carrier), service lamps light (including outside and atop the tower for the relay), the header tank fills
  in three steps, machine sounds are scheduled, chat confirms, and Nia's acknowledgement arrives once.
- Radio: three authored lines (opening, arrival, local service restored) as subtitled chat with a click cue;
  undelivered lines wait for the participant's next login; the radio item replays the transcript read-only.
- Persistence: campaign state in world SavedData (schema 1). Repair, allocation, messages, presentation
  state, history and bindings survive save/quit and full restart. Unknown schemas are refused, never
  rewritten.
- `/signal scenario status`, and `/signal scenario reset bellwether confirm` (two-step) for testing.

## Verification record

| Check | How | Result |
| --- | --- | --- |
| Build | `./gradlew build` (JDK 17.0.20, Linux) | Pass; JAR `build/libs/signal-0.1.0.jar` (147 KB) |
| Domain unit tests | JUnit, 21 tests: 6-unit allocation, pump+relay rejection, misaligned relay unreachable, repair once, live-supply refusal, isolator refusal, stale revision, one-shot acknowledgement, transcript replay adds no event, failed actions not recorded, history restore | 21/21 pass |
| Integration GameTests | `./gradlew runGameTestServer`, 4 tests on the real generated site with Forge `FakePlayer`: full repair loop through the real server handlers (fuse consumed exactly once across duplicate and stale submits, refused isolator, over-capacity and stale applies, lamps/gauge/tank, acknowledgement once, SavedData round trip, transcript replay adds no event, unbound machine does nothing); occupancy guard; unknown schema refused and never written back; projection never loads chunks or overwrites foreign blocks | 4/4 pass |
| Dedicated-server class loading | GameTest server (userdev) **and** an official Forge 47.4.10 production server with the reobfuscated JAR in `mods/`; scenario created from the server console, status printed, second create refused | Pass, no client-class errors |
| Graphical playtest | Real dev client under Xvfb with Mesa llvmpipe (software OpenGL 4.5), driven by `xdotool`. Ran the command from chat, walked the road with held keys, arrival line fired, opened the cabinet in Adventure mode, used the panel with the mouse and with Tab/Enter only, fitted the fuse, closed the isolator, watched lamps/gauge/tank change, saved and quit, restarted the client, confirmed state and that no messages replayed on login, replayed the transcript. Screenshots in `docs/screenshots/`. | Pass |
| Textures/models | All custom blocks and items render with their own textures in the client (no missing-texture checkerboard seen) | Pass |

Two panel layout defects found in the playtest were fixed and re-verified in the client: preview text
overlapping the buttons, and the "1 units" label overflowing its button. Fitting the fuse no longer discards
an unapplied breaker proposal.

## Not verified

- **Sound.** The container has no audio device (OpenAL failed to open), so no sound was heard. Sound events
  are vanilla and the code paths ran, but volume, rhythm and audibility are unchecked.
- **GPU rendering, performance and frame rate.** Only software rendering was used. No profiling was done.
- **Windows.** Nothing was run on Windows.
- **Real player in multiplayer** and **player death / dimension change** during the scenario.
- **Unloading the site chunks in a live client** (covered only by the projection GameTest and by the
  design: the campaign holds the state and re-projects when chunks are loaded).
- **Non-superflat terrain**: the builder flattens natural terrain inside its bounds; the edge appearance on
  hills was not looked at.

## Known limitations

- The relay can be powered but never reaches anyone: antenna alignment has no control yet (M2).
- One participant per campaign; another player gets "isn't connected to anything here". A scenario created
  from the server console has no participant and accepts any player.
- `doMobSpawning=false` is a server-wide game rule; the scenario is meant for a dedicated test world.
- Sign and book text are English literals; UI, radio and messages are translatable.
- Nia is a voice on the radio only; there is no actor in the world yet.
- Reset rebuilds the site and starts a new timeline but does not touch player inventories.

## Planned, not implemented

| Milestone | Content |
| --- | --- |
| M2 | Radio channels (Bellwether maintenance, regional exchange, legacy archive), tuning UI, transcript screen, priority queue; antenna alignment control with assist; route terminal (local open vs authenticated regional) with shown trade-offs; consequences with persistence; diagnostic reader; data-driven message definitions with a validated loader. |
| M3 | Narrative schedule (morning/afternoon/evening/night) advanced only by confirmed actions; Rowan's receiver promise; renegotiation call; field battery window (3 units, one segment, previewed fallback); Nia training and verification; delegated task. |
| M4 | Home room, Rowan's receiver activity with on-time / renegotiated / missed scenes; three full branches. |
| M5 | Evidence-grounded transmission with provenance. |
| M6 | Expanded atmosphere: settlement, more sites, original ambient audio. |

## Next dependency

M2 starts with antenna alignment (relay `relayAligned` already exists in the domain and the relay
controller already has a `linked` visual state) and the route terminal, then the radio channel model.
