# Playtest notes

Live play in a real client on the owner's desktop (RTX 5090, Wayland/XWayland), world `demo1`
(Meridian, Bellwether scenario created). Driven by xdotool for keys and chat; the view is turned
server-side by a throwaway datapack in the test world (synthetic mouse motion does not reach a
pointer-locked XWayland window). Severity: **bug** = broken, **rough** = works but feels wrong,
**idea** = would make it better.

## Session 1 — 2026-10-11

### Findings

**Top of the list (night one, first fight):**

A. **bug — no body marker; one death ends the run's combat.** DESIGN_V2 says the pack drops and "your
   body stays as a lootable marker until you reach it". Nothing implements it: death drops 8 loose item
   stacks, which vanish after 5 minutes of loaded time. I died at (260, 65, 240) at night carrying the
   radio, the revolver and all 15 rounds on the island; respawned at the compound (correct), and by the time
   I reached the spot the pack was gone. The site holds no more ammo, so the scenario's combat is over
   after one death. Fix: a corpse block/entity holding the inventory (never despawns, map marker or
   compass bearing), or at minimum `keepInventory` for SIGNAL items until the marker exists.
B. **bug — the Drawn are not rare: 36 loaded around the village on night one.** The limiter refuses a
   natural spawn only when 3 are already within 48 blocks, which is a local cap, so every 48-block patch
   fills to 2-3 over a 7-minute night: 3 within 32 blocks, 6 within 64, 36 in loaded chunks. They also have
   no daylight behaviour (no burning, no retreat, no despawn rule beyond vanilla distance despawn), so
   they persist into the day. Fix: a global budget per player (e.g. at most 2 within 96 and 4 loaded), a
   dawn retreat/despawn, and spawn only away from lit, powered sites.
C. **bug/rough — killed from full health without ever seeing the attacker.** 20 HP to dead in about three
   seconds at night; at 10-12 blocks the Drawn was invisible on screen (dark sky, dark trunks, no
   silhouette). Damage 5 per hit with 26 HP and 0.27 speed is a lot for a "blind, rare" stalker. The design
   wants the player to read it by sound; give it a visible tell too (pale eyes/face, breath particles, a
   rattle before the lunge) and a slower first strike so a careful player can respond.
D. **rough — muzzle flash is a flat torch flame in mid-screen.** Firing shows a large pixel-art flame
   floating at screen centre, detached from the barrel (gun is lower right). Reads as a torch, not a shot.
   Put the flash at the muzzle, make it a 1-2 frame bright burst, and add smoke.
E. **good — the revolver model in hand** now sits right and points forward; first-person reads well.

**Earlier in the session:**

1. **bug — spawn sits in a one-block pit.** The player starts at (120.7, 68.0, 173.3) with the
   surrounding ground at y 69, facing a one-block step. Holding W does nothing until you jump; it
   reads as "the game is frozen". S/A/D work. Fix: put the spawn on the top surface block (heightmap
   `MOTION_BLOCKING_NO_LEAVES` + 1) and face it down the road.
3. **rough — inside a spruce canopy the view is a wall of flat dark squares.** Walking east from
   spawn toward the compound I walked straight into a tree whose leaves reach the ground and got
   wedged between leaf blocks and the trunk at (183.7, 71, 168.7). Up close, at night, leaves render as
   near-black opaque tiles; it read as a broken fence or a render bug. Groves that close to the road
   and the compound approach could keep a clear corridor.
4. **rough — the west gate approach is a staircase of mixed materials.** Outside the gate the
   ground steps up to the road in terraces of dirt, mud brick, stone and slabs, with bare dirt terrace
   faces on the slope behind. Decision #36 says graded slopes finish as grassy banks; here they don't.
5. **rough — the yard is a big empty car park.** About 50 x 48 of flat andesite and gravel with
   saturated orange packed-mud patches. Well dressed at the edges (fire barrel, crates, container,
   shed, lamp posts) but the middle is empty and the orange still pops. Idea: tone the mud toward
   brown, add clutter (pallets, a cable drum, a parked trailer, oil stains) and a worn path, not patches.
6. **good — the service room and DP-1 panel.** Sign-posted spare fuse barrel, generator G1 with a
   lamp and instructions on a sign, and a panel screen that explains itself: applied state, proposed
   breakers with unit costs, a preview that does the sums ("Pump 5 + Local transmitter 1 = 6
   required. Generator provides 6. Fits, but the supply is isolated"), and a Fit fuse button that only
   appears when you carry one. Fuse -> breakers -> apply -> close isolator worked first time and the
   payoff lands: lamp lights, "This place has power now. If you fall, you will wake here.", Nia on
   the radio. Keyboard navigation (Tab/Enter) works on the panel.
7. **idea — the triage choice is invisible.** Pump 5 + transmitter 1 + relay 3 = 9 against 6, so
   something stays off, but nothing tells you who loses. Name the cost in the preview ("the regional
   relay stays dark; R4 cannot reach the mainland") and let Nia react to what you left off.
8. **rough — the barrel holds 2 fuses but only one F1 socket.** The spare is pointless today; either
   make it the reason to come back (F1 burns out again under heat if you run 6/6 for a night) or hold 1.
9. **rough — "pumpPressure=2" has no visible meter.** If the pump runs there should be a gauge,
   water in the header tank or a sound you notice from the yard.
10. **rough — the "container" is a terracotta patchwork.** The shipping container in the yard is a
    random mix of pink, brown and grey terracotta blocks; it does not read as a container. Use one colour
    with ribbing (e.g. rust terracotta + iron trapdoor doors).
11. **good — all five site rounds are findable and fair.** 2 in the drum stack by the pump, 2 + bread in
    the cottage, 1 + bread in the larger house. Scarce, and each find tells a small story.
12. **good — the village at dusk.** Lit windows, lanterns, chimney smoke, the compound's lamps behind:
    the strongest image in the build so far. The larger house's interior (stove under a cobble chimney,
    candle on a barrel) reads as lived in.
13. **rough — respawn faces a wall.** Respawning at the facility puts you in the doorway facing the
    frame. Face into the room or out to the yard.
14. **note — `/save-all` is unavailable in singleplayer** (permission), only the pause menu saves.
    Not a SIGNAL bug.
2. **rough — half-rendered frame.** One capture at night showed the left ~55 % of the window drawn
   and the right side dark. Could be a capture artefact during a resize; watch for it.

## Session 2 — 2026-10-14

Headless client (Xvfb, Mesa) on fresh seed-1 worlds, the pilot driving, the server over RCON. Night counts
by fast-forwarding with `/tick sprint` and counting with `execute if entity`.

### Fixed and re-checked in the client
- **1 (spawn pit):** confirmed: start at y 68.0 inside a bottom tuff slab; W did nothing. Now 121.5 68.9 on
  mud, W walks 11 blocks in 3 s. Respawn there works too.
- **3 (tree at the road):** walked into a trunk on the road's verge 8 blocks from the start. The first two
  apron blocks are now always cleared; the same walk passes.
- **5 (orange yard), and every path:** podzol read as orange as coarse dirt did. A swatch of eight blocks in
  the world settled it: mud and muddy mangrove roots read dark; podzol, dirt, rooted and coarse dirt do not.
- **10 (container):** now one colour of weathered green steel, ribbed, rust along the base.
- **A (pack lost on death):** drops on death never despawn and glow; still there after 8000 ticks; on
  waking: "Your pack is where you fell: 160 66 172, 39 blocks to the east."
- **B (crowds):** before, one night at the village: 43 loaded, 9 within 96, 13 alive at noon. After: 4
  within 128 all night, 0-2 within 96, all gone by day (frozen ones leave the moment they wake). Getting
  there took four measured iterations: caves filled the budget; a spawn-centred count let 6 in; counting
  only ticking ones let the outer ring fill to 14; the final rule counts every living Drawn within 128 of the
  player and spawns only under open sky.
- **C (unseen killer):** damage 5 to 4; a loud rattle and a 1 s wait before the first strike (GameTest, which
  fails without the wait); the head seam and ear veins glow faintly; both Drawn readable at 10 and 20 blocks at
  midnight.
- **D (muzzle flash):** flames now leave the barrel at the gun hand and fly off; seen in slow motion (`/tick
  rate 2`). Crit stars showed as a grey cross at night and electric sparks start dark, so neither was kept.
- **13 (respawn faces a wall):** respawn at the facility now faces out to the yard (not re-checked by dying
  there).

### Still open
- 4 (west gate terraces), 7-9 (triage cost, spare fuse, pump gauge), 2 (half frame, not seen again).
- Sound by ear, shaders, real GPU: not checked here.
