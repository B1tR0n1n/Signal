# SIGNAL — Design v2: Stranded

Owner direction, 2026-10-11. Supersedes the brief's non-combat scope (HANDOFF §3.10, §9 "Adventure mode",
§30 "what not to build") the same way decision #22 superseded its platform. Everything else in the brief
that still fits (server authority, pure-Java domain, original assets only, fictional family, no affection
meter, honest reports) stays.

## The premise

Elias Vale took the Meridian contract four months ago: restore the relay chain on the Meridian islands, a
cluster cut off from the mainland since the shutdown. The ferry that brought him never came back. The
relay chain is the only way off the islands' silence, and the only way to reach Mara and Rowan at home.

**The most important thing to Elias is getting home.** Every system serves that or threatens it.

And something on these islands is not right.

## The wrongness: the Listening, and its Echoes (owner choice)

The shutdown was not decay. Every band on the islands carries a faint carrier, a jammer someone built on
purpose. The islanders call the far woods "the quiet" and do not talk on the radio after dark.

The jamming was never meant to keep the islands from calling out. It was meant to keep something *on* the
islands from hearing. The things in the fog (the **Drawn**: animals, and once people, changed by long
exposure to whatever is under the islands) are drawn to transmission. Every relay Elias restores widens what
he can hear and what can hear him.

**And it answers.** Whatever listens has learned to speak back on the bands it hears. Some of what comes over
the radio is not who it claims to be: a distress call from a settlement that was empty for a year, Nia's
voice saying the pump is fine while the tank runs dry, and, on the clearest nights, a voice that sounds like
Rowan. The echoes are built from transmissions the islands have overheard, so the more Elias talks, the
better they get.

That makes the radio both the way home and the thing that cannot be trusted:

- **Verification is gameplay.** A message can be checked: a bearing that points at a dead site, a reply that
  references something never transmitted, a call-and-response code agreed with someone in person. The
  honest-report thread of the original brief becomes survival: what you believe decides where you go.
- **Echoes are lures.** Answering a false call draws the Drawn to where you are, or sends you to where they
  wait. Some false calls carry true information. The player learns tells: wrong timing, a phrase repeated
  exactly, a bearing that wanders.
- **Home is the hardest test.** Contact with Mara and Rowan needs a way to know it is them, something only
  the family would know, agreed before Elias left and never spoken on the radio. Every time he says it on the
  air, the islands hear it.

The final question is the one the whole game argues about: **bring the network up and call home, knowing
what follows the signal across the water and what may already be speaking in your family's voices, or leave
the islands silent.**

## Pillars

1. **Every transmission is a decision.** Talking costs safety. Silence costs people.
2. **Scarcity is total.** Ammo, fuel, fuses, light. For everybody: Drawn, scavengers and settlers alike.
3. **You cannot be everywhere.** Sites fail while you are away. You choose who keeps power.
4. **The radio is the map.** You find the world by listening to it.
5. **Home is a voice.** The family is reachable only through the network you rebuild, and only sometimes.

## Systems

### Survival (vanilla survival mode, tuned)
- The scenario puts the player in **Survival**, not Adventure. Health, hunger, darkness and death matter.
- No diamond grind: vanilla crafting stays for basics. SIGNAL items (ammo, fuses, batteries, radio parts)
  are **found or traded, never crafted from nothing**. Two exceptions, each needing a workbench at a powered
  site: reloading spent cases, and repairing a fuse.
- Death: respawn at the last *powered* site you restored (a reason to keep sites alive), and drop the pack.
  Your body stays as a lootable marker until you reach it.
- Night is dangerous; Drawn are more active and fog is thicker. Light costs fuel.

### Weapons and ammo
- **Original weapons only**, modelled as Minecraft item models from cubes, with textures from
  `tools/gen_textures.py` and sounds synthesized by a new `tools/gen_sounds.py`. No ripped assets.
- Starting kit: a **service revolver** (6 rounds, slow reload, loud) and the knife. Found later: a
  **bolt-action rifle** (5-round clip, long range, very loud), a **flare pistol** (light and signalling;
  draws Drawn), and a **suppressed .22** (quiet, weak; the stealth option).
- Server-side hitscan with spread, falloff and a short delay for range; headshots on the Drawn. No
  attachments, no ballistic simulation.
- **Ammo is scarce for everybody.** It is a physical item, counted per round. Scavengers carry a few rounds
  and flee when dry. Settlers pay for jobs in ammo. Gunshots are noise: they draw the Drawn within ~64
  blocks, so every shot is also a decision.
- Melee (knife, wrench) always works and costs nothing but risk.

### Emissions (EMCON) — the core
- Every powered transmitter (handheld radio when keyed, local transmitter, relay R4) has an **emission
  radius** proportional to power and antenna height.
- Keying the handheld radio or powering a transmitter emits **noise events**. Drawn within range get a
  heading and close in. A powered relay is a beacon: it pulls Drawn to the tower over time, so restoring
  R4 creates a defend-the-site moment.
- **Radio discipline is a skill.** Short transmissions; transmit from somewhere you can hold; power the
  relay down when you leave; schedule contact windows.
- The family channel is the most expensive call in the game: the longest-range transmission, from the
  tallest tower, at night when propagation is best and the Drawn are worst.

### Echoes (false transmissions)
- Authored, not random: each echo is a recorded line from the campaign's own transmission log (what the
  player and NPCs actually said), recombined, mistimed or replayed from the wrong place.
- Every echo has at least one **tell** a careful player can catch (bearing, timing, a detail the true speaker
  could not know or would never say) and at least one way to **verify** it (go there, ask a code, cross-check
  a second bearing).
- Echoes grow with the player's transmissions: the domain model counts what has been said on air, and
  echoes may only reuse phrases that have been. Saying the family's private phrase on air permanently
  lets the islands use it.

### Signals intelligence and direction finding
- The handheld radio **scans**. Unknown signals appear: a numbers station, a looping distress call,
  someone answering on a dead channel, the jammer carriers.
- **Take bearings**: the radio shows signal strength by heading. One bearing gives a line; two bearings
  from different places give a fix on the map. The tower deck gives the best bearings, which is what the
  antenna alignment becomes (M2's "align at mount" turns into direction finding).
- Fixes reveal new sites on the map: other relays, settlements, caches, jammer sites.

### Triage and consequences
- A limited number of generators, a fuel supply that runs down, components that wear out.
- Each site's services serve named people: Bellwether's pump (water), the clinic's fridge (medicine), the
  school's lights, the jammer-adjacent watchmen's radio.
- What you power is visible, and what you don't is visible too: lights go dark, a family leaves, a voice
  stops answering, a settlement stops trading ammo with you.
- **Things break while you are away**: storms, wear, scavengers stripping copper. A site's status reaches
  you only if its radio still works, which is the triage problem in one line.

### Home
- Mara and Rowan (fictional, per HANDOFF §3.9) are on the mainland. Contact needs the relay chain up to
  the coast relay, a contact window, and a transmission the Drawn will hear.
- Rowan's receiver: he built one to listen for his father. Early in the game Elias can only *hear* home on
  rare clear nights and cannot answer. Later he can talk. What he chooses to say, and what he reports
  honestly about where he is, are the home scenes.
- No affection meter. What is recorded is facts: calls made, promises given, promises kept.

## Structure: the islands

| Island | Role | Introduces |
| --- | --- | --- |
| **Bellwether** (start) | The current site: relay R4, the compound, the village | Survival, revolver, first Drawn, first transmission and its cost |
| **Ashgrove** | Logged woodland, sawmill settlement, the clinic | Triage (clinic vs pump), trading ammo, scavengers |
| **The Quiet** | Fog-bound forest around jammer J1 | Direction finding to an unknown carrier; first jammer; the truth begins |
| **Coast Relay** | The tall mast that can reach the mainland | The family channel; the final choice |

Islands are linked by causeways, an old rail bridge and a boat that needs fuel.

## Build order (each phase is playable and tested before the next)

| Phase | Name | Delivers |
| --- | --- | --- |
| **A** | Stranded | Survival-mode scenario, death and respawn rules, revolver and knife, ammo item, loot in site containers, gunshot noise, one Drawn creature (original model, texture, sounds, AI), night danger |
| **B** | Emissions | Noise and emission model in `domain/` (pure, unit-tested), the handheld radio's push-to-talk, powered relay as beacon, Drawn homing on emissions, a defend-the-tower moment at R4 |
| **C** | Bearing | Radio scanning UI, bearings and fixes, map markers, the numbers station and distress call, jammer J1's carrier, the first echoes and their tells |
| **D** | Triage | Fuel and wear, second site (Ashgrove), named services and visible consequences, site-status reports over radio |
| **E** | Home | Coast relay, family channel, Rowan's receiver, contact windows, the ending |
| **F** | Above and beyond | Scavenger humans with scarce ammo of their own, rifle and suppressed .22, the boat, original ambient audio, weather fronts |

Every phase follows the repo rules: rules in `domain/` with JUnit tests, server-authoritative, GameTests
for world behaviour, `site_sweep` for site/worldgen changes, decisions recorded, original assets logged in
ASSET_PROVENANCE.
