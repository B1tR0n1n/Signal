# SIGNAL Minecraft Forge Development Handoff

Version 1.0 • October 9 2026 • Complete implementation brief for Claude Code

## 1 Your assignment

You are the developer helping the owner build SIGNAL as an original Minecraft Java Edition adventure using Forge. Read this entire document, inspect the actual workspace and its instructions, then begin implementation. Do not stop at a plan when a runnable increment can be built. Work in small integrated milestones and report real results.

The owner has explicitly chosen Minecraft Java, Forge, Linux support, and a dark industrial atmosphere inspired by S.T.A.L.K.E.R. The previous browser prototype does not establish the intended platform. If useful code exists, preserve it as a reference or source of reusable algorithms and content. Do not continue a browser implementation as the main deliverable. Do not replace Forge with Fabric, NeoForge, a standalone engine, or a website without the owner's instruction.

Deliver a custom Forge mod, an authored playable world or reproducibly generated scenario, and the assets needed to play. The first result must let the player physically walk through a relay facility, inspect equipment, change power allocation, and observe real service changes. A dashboard alone is insufficient.

The original SIGNAL Development Bible remains an optional thematic reference. This handoff is self-contained and overrides its engine-neutral implementation plan. In particular, Minecraft's native first-person play is now the default; do not build a custom third-person controller. The player may use Minecraft's normal perspective switching. Preserve the deeper design while adapting its presentation to Minecraft.

All fictional names and numerical values below are working canon. The owner can change them. Linux and Forge are explicit requirements. The atmosphere is an artistic direction, not permission to copy another game's assets, maps, factions, dialogue, branding, music, or proprietary models.

## 2 The game we are building

SIGNAL is about a former soldier who now repairs infrastructure in a society that still functions, unevenly, after decades of conflict and institutional fragmentation. People have not lost all technology. They have lost confidence in the systems and agreements connecting them.

The player is Elias Vale, a capable field systems engineer and father of two. His field work restores power and communication between isolated communities. His choices also determine who gains access, who learns to maintain equipment, which reports people can trust, and whether he keeps the commitments he makes at home.

The central theme is transmission: knowledge, wounds, responsibility, faith, fear, and care pass through people as surely as messages pass through relays. The final transmission is assembled from the actual successful playthrough. It reflects particular events without judging the player's soul or claiming to know motives.

The game should feel lonely in the field and human where people live. Rain on a maintenance road, wind through a steel tower, a dim room with a generator fault, a voice struggling through static, then a small warm home with a child excited to show you something. The contrast matters.

The first playable is a compact adventure, not a complete conversion of Minecraft or a survival modpack. It should contain a short physical route, one infrastructure puzzle with several valid outcomes, authored radio communication, a simple home commitment, and persistent consequences.

## 3 Nonnegotiable project constraints

1. Minecraft Java Edition and Forge are required.
2. Linux is a first-class development and play target. Windows compatibility is desirable and should be preserved through normal Java and Gradle practices.
3. The initial project has no mandatory third-party gameplay mods, animation libraries, shaders, cloud services, accounts, or runtime AI.
4. The main deliverable is a Forge mod JAR plus playable scenario content and installation instructions. It is not an HTML preview.
5. The first milestone includes world-space interactions. Walking, inspecting, repairing, and observing must work inside Minecraft.
6. Simulation and persistent narrative state are authoritative on the logical server, including in single-player.
7. Pauses, reading, and time away from the application do not silently damage relationships or expire promises.
8. No visible or hidden universal morality score. Record facts and use bounded authored interpretations.
9. Do not include the owner's real children, partner, medical details, private messages, or literal personal history. Use fictional people.
10. No combat, radiation simulation, gun framework, advanced NPC AI, or large modpack in the first milestone.
11. Every claimed feature needs integrated observable behavior and appropriate verification. Compiling a class is not completion.
12. Do not delete user worlds, reset established saves, change launchers, or install into the owner's active Minecraft instance automatically.

## 4 Toolchain decision and version discipline

Use Minecraft 1.20.1 with Forge 47.x and a 64-bit Java 17 JDK as the starting target. This is a deliberate version-specific baseline, not a claim that 1.20.1 is the newest Minecraft or the only workable version. The official Forge documentation for this branch specifies Java 17 and provides the MDK development workflow.

When inspected on October 9 2026, the official Forge 1.20.1 download page listed recommended 47.4.10 and latest 47.4.26. Prefer the recommended build initially unless actual compatibility or a documented fix justifies another exact 47.x build. Recheck the official page when starting work. Pin the chosen build in project configuration and README; never use a floating latest version.

Use the official Forge MDK and its Gradle wrapper. Preserve the wrapper JAR, scripts, wrapper properties, ForgeGradle setup, and required repositories. Do not invent a wrapper JAR or write a fake Gradle downloader. Do not replace MDK build configuration with speculative snippets from a different Minecraft release.

Record Minecraft version, Forge version, Java target, Gradle wrapper version, mappings, mod version, and any optional dependencies in a compatibility document. Use the selected version's actual mapped source or official documentation when an API signature is uncertain. Avoid mixing newer component APIs, Fabric registration, NeoForge event names, or older networking examples into Forge 1.20.1.

If a project already exists, inspect it before deciding to recreate anything. A functioning compatible Forge project should be extended. If a different version is already built, explain the concrete migration need before changing it. Do not switch versions because a random tutorial uses another release.

Source references:

- Forge setup: https://docs.minecraftforge.net/en/1.20.1/gettingstarted/
- Official builds: https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html
- Logical and physical sides: https://docs.minecraftforge.net/en/1.20.1/concepts/sides/
- Saved data: https://docs.minecraftforge.net/en/1.20.1/datastorage/saveddata/
- Network handling: https://docs.minecraftforge.net/en/1.20.1/networking/simpleimpl/

These references ground the toolchain and Minecraft integration, not the fictional gameplay design. Reverify detailed method signatures during implementation.

## 5 Startup inspection and first response

Inspect applicable AGENTS.md and existing CLAUDE.md instructions, the repository, build files, Java installation, available display environment, and any existing Minecraft project. Run small read-only checks before editing. Look for an existing Gradle wrapper and version pins. Do not inspect unrelated personal files or launcher credentials.

Report briefly what exists, what can be run, and any real blockers. Then implement the first useful increment. If the environment has no graphical display, say that runtime visual verification is unavailable; build the JAR and use meaningful headless tests where possible. Do not describe a headless compilation as a successful graphical playtest.

If Maven downloads or MDK retrieval are blocked, identify the blocked dependency and preserve work already done. Provide the exact next action needed on the owner's machine. Do not substitute an unrelated browser game because network access is inconvenient.

Ask only questions that materially block progress. The owner has already supplied platform, mod loader, Linux requirement, and atmosphere. Do not ask again whether to use Forge or whether they want a dark world.

## 6 Atmosphere and art direction

Use original industrial survival atmosphere: wet forests, concrete utility sites, rusted steel, abandoned railway infrastructure, muddy access roads, reservoir service buildings, sparse settlements, and distant towers. The world is damaged but inhabited. It should show ongoing maintenance, not uniform decay.

Palette: muted olive and forest green, charcoal, weathered concrete, oxidized brown, dirty cream, subdued amber lamps, and limited cool indicator light. Interiors use readable silhouettes and practical illumination. Equipment indicators must remain distinguishable without color alone.

Every meaningful prop should imply use: maintenance labels, fresh cable against an old wall, a clean wrench on a battered bench, a patched window, a sleeping bag, a kettle, dry boots near a heater. Avoid covering every surface with random clutter. Wear is localized to exposure, handling, and neglect.

Keep Minecraft's block structure. We are not promising photorealism. Architecture, custom blocks, restrained textures, lighting, sightlines, weather, and sound can establish identity without disguising the entire renderer.

Initially use existing blocks intelligently: stone and concrete for utility walls, oxidized copper for weathered metal accents, iron bars for tower lattice, glass panes for damaged service glazing, slabs and stairs for massing, and restrained redstone lamps for working indicators. Original custom panels and machines provide precise affordances. Vanilla block names are building materials, not lore claims about the world's technology.

Shaders are optional later. The baseline must remain playable and atmospheric without shader support. Do not make dynamic lighting, shader fog, custom skies, or a particular renderer a first-milestone dependency. Any future shader or optimization stack needs verified Forge and Minecraft compatibility plus an independently tested fallback.

Rain is a mood tool, not a permanent screen obstruction. Weather should give quieter intervals. Do not rely on lightning, strobing, heavy grain, or extremely low brightness for tension.

## 7 Sound direction

Use wind around structures, distant mechanical hum, irregular generator rhythm, relay clicks, rain against roof materials, footsteps, and carefully spaced radio traffic. Sounds communicate state: a pump begins cycling, a relay carrier returns, a failing generator stutters, a service room fan becomes steady.

Do not use copyrighted S.T.A.L.K.E.R. audio, ripped radio chatter, famous songs, or voice imitations. Original or appropriately licensed assets only. Track source, license, author, modification, and attribution for shipped assets. Developer placeholders must be marked and replaced or licensed before distribution.

Critical dialogue must have subtitles and a replayable transcript. Missing audio assets must not prevent completion. Text-first authored radio content is acceptable initially; do not fabricate finished voice acting. Add a brief original static or click cue if it can be generated and tested properly.

Avoid overlapping important messages. Implement a queue with priority and repeat policy. A new urgent line can delay ambience; it should not truncate the only explanation of a power tradeoff. On reload, do not replay every historical message or duplicate the same acknowledgement.

Use existing sound categories initially where appropriate. Do not promise a separate mixer slider before implementing a supported way to provide it. Radio effects should never make speech unintelligible.

## 8 Setting and characters

The setting is the fictional Meridian Corridor. Decades of conflict, selective shutdowns, centralized access, poor maintenance, and incompatible systems created isolated service regions. Schools, administrations, trade, homes, and hospitals still exist. They depend on fragile local arrangements.

The abandoned Continuity Relay Network is colloquially called the Signal. It combined public emergency services with later military and commercial systems. Restoring it creates both coordination and exposure. Origin Station is a regional coordination and archive facility, not a magic worldwide controller.

**Elias Vale:** former soldier, field engineer, capable and observant, dry humor, prone to trying to solve more than he can sustain. His private Christian faith can appear through small original details and optional reflective dialogue; it is not a stat mechanic. Do not require the player to agree with his faith to succeed.

**Nia Serrat:** Bellwether's maintenance technician. She knows pumps and local equipment. She needs help with the legacy relay but is not helpless. Teaching her a bounded task creates durable local capability.

**Mara Vale:** the children's mother, responsible for practical scheduling and her own work. She is not a villain or an affection gate. She asks for feasible commitments and accurate updates.

**Rowan Vale:** older child, 9, interested in maps, mechanisms, and stories. Wants Elias to help test a homemade receiver. Speaks like a child, not a moral philosopher.

**Tess Vale:** younger child, 6, playful, stubborn, collects small things and builds a paper town. Interactive content may wait until the second playable. Both children remain safe.

**Amos Reed:** former relay custodian with useful documents and incomplete honesty about the network's past. Later content.

**Iona Mercer:** regional continuity administrator whose central system delivers real services while collecting data and restricting access. Later content.

**The Archivist:** bounded legacy software indexing damaged recordings. It can recombine archived information and create eerie specificity; it cannot read minds or explain every mystery. Later content.

Faction directions for later expansion: centralized continuity administrators, federated community mesh operators, contract transport and security firms, and custodians of legacy infrastructure. Each has real benefits, costs, and internal disagreement. No faction is a uniform alignment label.

## 9 Gameplay identity and Minecraft adaptation

The core loop is receive or discover a request; travel; inspect; diagnose; configure or repair; verify; report honestly; observe a consequence; return or renegotiate commitments.

Use Minecraft's movement and interaction rather than duplicating a game engine. First-person inspection is primary. Normal perspective switching remains usable. Do not add a custom animation framework or cinematics before basic interactions work.

Adventure mode is the default authored experience, with controlled survival settings. Ordinary digging, crafting, farming, enchanting, and hostile mob spawning should not dominate the scenario. Provide supplies appropriate to the mission rather than requiring a diamond pickaxe grind.

Custom block interaction must work in the chosen game mode. Right-click menus and server-validated tool use are preferable to breaking blocks to repair them. Permit only deliberate authored interactions. If the player can damage equipment later, define that as a supported action with consequences rather than accidental map destruction.

Do not blanket-change every world's game rules when the mod loads. Scenario rules apply only to an initialized SIGNAL campaign. Installing the mod in an ordinary world must not automatically remove mobs, freeze time, replace terrain, give campaign items, or overwrite a spawn area.

Redstone can provide visible indicators or authored decorative circuits. The custom infrastructure graph is the source of truth for service capacity and policy. Do not try to model regional power using unlimited vanilla redstone signal alone, and do not create two competing simulations.

## 10 Content delivery and map strategy

The owner needs a map they can actually enter. Choose a reproducible scenario bootstrap before creating a sprawling hand-built save. A generated authored site is appropriate for milestone one because it allows code review, repeatable tests, and easy recovery.

Implement an explicit operator command such as `/signal scenario create bellwether`. The command grammar here is proposed; document the real implemented names. It must not execute automatically. Create in a designated fresh test world or an isolated campaign area with clear bounds and collision checks. If a protected dimension is feasible without excessive scope, it is an acceptable later route, not a prerequisite.

Default bootstrap must refuse to overwrite nonempty occupied terrain, existing campaign content, or an active scenario. A reset command must require a separate explicit confirmation and be restricted to known scenario bounds. Do not delete worlds. Store campaign ID, map schema version, anchor position, and initialization status.

World construction can use procedural authored placement initially and structure templates later. A generator describes specific buildings and routes; it is not a request for limitless procedural worldgen. Use staged placement if the work would block a server tick. Verify template and NBT tools rather than hand-inventing opaque files.

The generated scenario should configure safe spawn and starting equipment only for its participating player. Apply weather and game rules knowingly and document their scope. If using the Overworld, explain that weather changes affect the dimension, and do not apply them in a shared unrelated world.

Delivery can evolve to a packaged clean adventure save with structure templates. Do not distribute Minecraft executables, copyrighted base assets, launcher credentials, or a preauthenticated instance. Ship mod assets and authored world content only.

## 11 First site layout

A practical first-playable area can fit within roughly 160 by 160 blocks, with a 32 by 32 block facility cluster. These are proposed maximums, not a requirement to fill every block.

Start on a short forest maintenance road with a view of the ridge relay. The tower is the landmark. The path passes a drainage channel, a service sign, and an abandoned maintenance vehicle suggested by block construction. The player reaches an open service yard rather than solves a locked-door puzzle before the engineering tutorial.

Facility spaces:

- Entry vestibule: maintenance log, simple instructions, view into the generator room.
- Generator room: generator, isolated fault, spare fuse cabinet, supply indicator.
- Distribution room: panel with pump, local transmitter, relay, and battery circuits.
- Relay equipment room: route terminal, status light, old archive receiver.
- Tower platform or exterior antenna station: accessible alignment control; avoid mandatory dangerous parkour.
- Pump service view: visible motion or lights and a pressure gauge indicating restored operation.

For milestone one, use a single compact room and exterior tower silhouette; add the other spaces as the loop matures. Do not promise the whole layout before its interactions are integrated.

Bellwether can initially be visible at a distance as three modest structures with lamps. A later small settlement visit and home room complete the playable. Walk distances should support atmosphere without making every test take ten minutes.

Hide debug blocks and commands from the normal route. Physical equipment needs clear labels and distinct shapes. A player should not need to remember developer coordinates to find the panel.

## 12 Registered content

Use mod ID `signal`. Suggested Java package `io.github.b1tr0n1n.signal`, subject to repository naming conventions. Use lowercase resource paths and stable registry IDs. Check for existing mod-ID conflicts if adding external dependencies later.

Minimum milestone content:

| ID | Function | Needed immediately |
| --- | --- | --- |
| `signal:generator` | Authored supply source with operating indicator | Yes |
| `signal:distribution_panel` | Allocation and preview interface | Yes |
| `signal:pump_controller` | Visible service demand and state | Yes |
| `signal:relay_controller` | Relay state and configuration | Yes |
| `signal:field_battery` | Optional bounded reserve supply | After base allocation works |
| `signal:handheld_radio` | Receive authored messages and open transcript | Basic item initially |
| `signal:diagnostic_reader` | Inspect measured state and faults | Can share panel inspection first |
| `signal:replacement_fuse` | One explicit repair component | First repair pass |
| `signal:route_terminal` | Open versus monitored outgoing route | First playable |
| `signal:service_lamp` | Readable visual feedback | Optional; existing lamp can suffice |

Do not add twenty decorative machines before four functional ones work. A replacement fuse is not a universal scrap currency. Durable tools should not disappear after a use unless the content explicitly defines consumption.

Names, models, translations, and textures must resolve inside the built JAR. Pink-and-black missing assets are bugs, not acceptable final placeholders. Simple original textures are preferable to elaborate broken models.

## 13 Infrastructure model

Use a small pure Java domain model for power and communication, independent of world rendering. Each campaign has stable node and edge IDs. Runtime Minecraft positions bind physical blocks to domain nodes. Coordinates alone are not identity.

Node fields: ID, site ID, type, demand or supply, enabled state, health, mode, required conditions, policy, dependencies, and derived outputs. Edge fields: endpoints, kind, enabled state, capacity when relevant, and authorization requirements.

Represent power supply, requested loads, approved allocation, communication reachability, and access policy separately. A physically connected relay can lack power. A powered relay can lack antenna alignment. An aligned relay can be blocked by a route policy.

Prototype power uses capacity units, explicitly fictional abstractions rather than real watts. A generator provides 6 units. Pump consumes 5. Local transmitter consumes 1. Regional relay consumes 3. When enabled demands exceed available supply, the preview explains the shortage and prevents an invalid allocation, or a clearly defined trip behavior occurs. Prefer preventing invalid apply in the first milestone.

The first solver only needs an authored shared bus. Name it accordingly; do not claim a generic graph solver if every load is hard-coded to one generator. Introduce richer topology when a second site needs it.

Battery can supply 3 extra units for one explicit narrative segment. It cannot power the region forever. It remains charged while the user reads menus. Show remaining scheduled duration and the fallback configuration before activating it.

A pump demand of 5 and regional relay demand of 3 total 8; generator plus battery capacity 9 can satisfy them during the window. The default pump-plus-local-transmitter allocation totals 6. A later standby supply installation adds genuine authored capacity; routing menus do not create energy.

## 14 Physical repair sequence

The first fault is a safely isolated damaged fuse, not a request to perform real electrical work. Nia already isolated the supply after noticing heating. The player reads the gauge, confirms the isolated state, takes the replacement fuse from the marked cabinet, and uses the panel interaction to install it.

Attempting installation while powered fails with a clear message and no item consumption. A valid repair consumes exactly one fuse after all checks pass. Duplicate packets or repeated clicks do not consume a second fuse. Restoring power changes actual service state.

Avoid precise timing, rapid button presses, and fake wire-matching games. The pleasure is understanding the dependency and selecting a workable plan. Tool use can be simple when the choice is interesting.

Antenna alignment is initially an authored control with a few positions and a signal indicator. Provide keyboard-accessible selection and an assist option. Do not build physically exact RF propagation or require a custom renderer before this works.

Verification has observable outputs: lamp on, pump gauge rising, machinery sound available, terminal stating reachability, and radio acknowledgment. UI and world must agree. A scripted congratulation with unchanged machinery is not a working repair.

## 15 Panel interface

Opening a distribution panel presents available capacity, each service's demand, current active services, proposed selection, anticipated result, and an Apply action. Separate current state from proposed state.

Example plain-language preview: “Pump 5 + regional relay 3 = 8 required. Generator provides 6. Battery window adds 3. Valid for one work segment. Afterward return to pump and local transmitter.”

Use Minecraft screen and menu conventions with readable text and keyboard navigation. Do not embed a webview. For initial implementation a focused screen can use validated packets; choose the actual supported Forge design and document it.

The client submits an intention, such as requested service IDs and expected revision. The server recalculates validity, checks interaction access, applies the change, and returns a new snapshot. It does not accept the client-provided supply, completed repair status, or consequence history as fact.

When state changed since the screen opened, reject stale apply and refresh the preview. Support cancel without mutation. Closing or disconnecting leaves a valid configuration. Do not advance the narrative schedule while merely inspecting or rearranging an unapplied preview.

## 16 Radio system

The handheld radio is a contextual authored communication interface. Start with discrete channels rather than continuous analog tuning. Channels have labels, availability conditions, messages, and replay access. Later add tuning presentation without changing the underlying message model.

Initial channels: Bellwether maintenance, regional exchange, and a legacy archive frequency. The first two contain live authored content. The archive repeats an old recording. Keep actual source, apparent source, provenance, repeat policy, and conditions distinct.

Messages have stable IDs, translated text, speaker, optional sound event, prerequisites, priority, interruption policy, transcript eligibility, and acknowledged or delivered state. A critical message should appear once in the live queue and remain replayable. Replay does not create another event or reward.

Reception can initially depend on authored region and active route state. Label it as an abstraction. Do not scan every chunk or ray-trace hundreds of signals each tick. A future directional feature can use a bounded distance and authored obstruction rule.

Important messages:

Opening: Nia says, “Bellwether maintenance. Anyone still up there?”

After local service: “We can hear each other again. I'll get the depot request to the courier.”

After monitored route: “Regional exchange confirms the request. Relay identity logged.”

After verified delegation: “Nia here. Window's closed. Pump and local channel are holding.”

Archive: an old call sign and an inconsistent timestamp. The game marks uncertainty instead of inventing supernatural certainty.

If a new player misses an audio cue, the transcript and equipment state remain sufficient to understand the result.

## 17 Operating policies and consequences

The first playable includes two valid outgoing communication paths. Local open service reaches a cooperative courier and neighbors but lacks immediate regional coordination. Authenticated regional service reaches the exchange immediately but exposes relay metadata to that operator.

Show these known tradeoffs before the choice. No universal correct answer. Do not spawn a surprise ambush to punish openness. A later monitoring consequence requires a defined monitoring station, active reception path, and actual observation event.

Service consequences are separate rules. A depot acknowledgment is one event. A metadata record is another. A visible shipment is a later result if prerequisites remain satisfied. Labels such as “surveillance increased” should be replaced with concrete information: “The exchange has recorded this relay's identifier and active route.”

A consequence stores ID, trigger, eligible schedule, prerequisites, cancellation conditions, output, and delivery state. Each outcome fires once per qualifying trigger. On load, pending outcomes stay pending and delivered ones stay delivered.

## 18 Narrative schedule

Minecraft's day/night cycle is presentation. SIGNAL's commitments use a separate explicit schedule: morning, afternoon, evening, night. Do not derive family reliability from world ticks, real elapsed time, sunrise, sleep, menus, screen focus, or server lag.

Only substantial confirmed narrative actions advance segments. Short walking around the facility, inspection, reading, tuning, ordinary interaction, and menu use do not. A travel transition or extended work action states its cost before confirmation.

If the world lighting is set to reflect a schedule segment, that is a presentation update scoped to the campaign environment. It is not the authoritative clock. Pause during dialogue where supported, but correctness must not depend on the integrated server actually pausing.

Implement one accepted home promise: Elias agreed to help Rowan test his receiver in the evening. Start in the afternoon. A quick repair and return fit. An extended job consumes the evening unless a capable technician completes it after verified teaching.

A promise state contains recipient, action, window, status, notice history, resolution event, and follow-up. States: active, fulfilled, renegotiated, missed, mutually cancelled. Later acknowledgment does not retroactively fulfill a missed promise.

Calls can renegotiate before expiration. A lack of reachable contact is recorded as context. Declining a commitment is allowed. Do not award affection for always accepting impossible work.

## 19 NPC and home implementation

Start with a clearly identified stationary actor or interaction point, not a full autonomous human simulation. Nia can use a basic custom entity with restrained idle behavior or a labeled terminal interaction in milestone one. State honestly which is implemented. Do not claim a believable human NPC if she is only a sign.

Use stable character IDs independent of entity UUIDs. Respawning a visual actor must not reset dialogue history or duplicate rewards. Children are protected authored characters, not vulnerable mobs. Ordinary damage and despawn mechanics must not endanger them.

Home is a small warm room with a worktable, receiver, kitchen suggestion, and children's belongings. Rowan's interaction is playable: connect or test the receiver, listen to what it catches, and respond. It can be text-based initially with physical objects. It should not be only a completion popup.

On-time line: Rowan wants Elias to hear the station he found. Renegotiated line: Rowan remembers the new agreed time. Missed line: “I tried it without you. I wanted you to hear the little.” Then allow a specific follow-up. Avoid forcing the child to reassure Elias or carry an adult confession.

Tess later adds a paper-town activity focused on participation rather than technical optimization. Both children have independent interests and remain loved across imperfect playthroughs. No affection bar that can fall to zero.

## 20 Three first-playable branches

**Quick local service:** Repair supply, power pump and local transmitter, report the limitation accurately, and return for the agreed receiver test. Consequences: local communication restored, depot coordination slower, home promise fulfilled.

**Regional battery window with delegation:** Align relay, activate a bounded battery window, choose authenticated route, teach Nia the fallback procedure, verify her understanding, and return. Consequences: request received immediately, relay metadata recorded, Nia safely restores local allocation after the window, home promise fulfilled.

**Extended durable repair:** Install a defined standby supply component and complete the longer job. Contact home to reschedule or knowingly let the window pass. Consequences: durable expanded capacity, distinct notice history, and a later home scene. No arbitrary declaration that the player is a bad parent.

These branches are first-playable scope, not milestone-one scope. Milestone one can prove allocation and acknowledgment before schedules and children exist. Keep an implementation status distinguishing planned from working content.

## 21 History and ending

Maintain canonical successful events with unique IDs and stable sequence. Include action type, actor, target, schedule, payload, and relevant known context. Failed transactions and debug operations are not narrative acts. Events from an abandoned save timeline do not leak into the active one.

Record narrow facts: a service was restored; a temporary limitation was disclosed; a promise was kept; teaching was verified; an independent repair succeeded. Do not infer motives such as needing admiration or loving violence.

The prototype ending selects authored fragments with explicit prerequisites. Every selected statement has provenance. Example: “At Bellwether, the line stayed alive after you left. Nia knew where to look.” This requires both verified training and a successful later action, not merely choosing a “teach” dialogue option.

A single act cannot support a repeated-pattern claim. Relationship statements must be bounded by completed opportunities. Mixed histories remain mixed. No hidden virtue axis determines a good or evil ending.

For Minecraft presentation, begin with an in-world console, ordered text and audio, and a few visible relay lights or planned viewpoints. A full cinematic camera route is later work. Do not implement camera hacks before basic ending selection is verified.

The final two home receivers light as a message arrives. The cinematic can recall private scenes, but the public in-world transmission must not automatically expose private family dialogue. Use a private addressed channel or a simple public arrival tone for the receivers.

## 22 Forge architecture

Use normal Forge registration mechanisms and version-correct event buses. Suggested module layout: registration; blocks and block entities; items; pure domain; campaign persistence; networking; client screens and presentation; radio; narrative definitions; scenario construction; commands; debug tools; data generation.

Pure domain code owns allocation rules, schedule transitions, promises, consequence eligibility, and fragment selection. Minecraft integration owns registry objects, world binding, menus, sound, entity interactions, and save encoding. Avoid a giant main mod class containing every subsystem.

World-level campaign state belongs in a server-side SavedData design appropriate to the selected branch. Mark changed data dirty. For cross-dimension campaigns, choose one documented root storage location rather than accidentally create a separate conflicting campaign per dimension. Verify the exact SavedData creation/load signatures for Forge 1.20.1.

Block entities store only local identity, configuration, and necessary machine state. Do not independently duplicate campaign truth in every machine's NBT. Define which fields are authoritative in local versus campaign storage. After loading, reconcile bindings and recompute derived service state.

Client classes, screens, renderers, and local audio belong behind physical-client boundaries. Common registration and server code must not load client-only classes on a dedicated server. Single-player still contains a logical server. It is not a license to mutate domain state on the client.

Keep the first content server compatible even though the campaign is single-player. A dedicated-server startup check helps catch side mistakes; public multiplayer gameplay and multiple-protagonist commitments are not promised.

## 23 Networking requirements

Use the selected Forge version's supported networking mechanism. For 1.20.1, consult SimpleImpl rather than guessing APIs. Register a stable protocol and packet set. Handle game-state changes on the proper thread through the documented context mechanism.

Initial client-to-server packets: request inspection snapshot, apply panel configuration, attempt fuse repair, select dialogue or work action, request radio transcript or channel action if needed. Server-to-client responses: validated snapshots, action result, message delivery, and limited campaign presentation updates.

Validate sender, campaign participation, loaded position, block type, interaction distance, permissible action, current revision, inventory requirement, and game state. Never allow arbitrary packet coordinates to force-load chunks. Never trust a client request to set a quest complete, manufacture a fuse, add capacity, or alter history.

Snapshot payloads must be bounded. Send changed state and relevant UI data, not the entire event log every tick. Radio history can be paginated or capped. Rate-limit accidental repeated interactions without making ordinary use frustrating.

Even in single-player, this discipline prevents ghost state and ensures UI and saved state agree.

## 24 Persistence and lifecycle

Persist schema version, campaign ID, initialized map bounds, root anchor, schedule, node configuration and health, supply reserves, installed components, active route policies, discovered messages, transcript state, mission facts, commitments, character memories, canonical history, pending consequences, delivered IDs, and scenario generation state.

Use stable string IDs. Never rely solely on runtime entity IDs or translated text. Include migration strategy for changing content IDs. An unsupported old schema should produce a clear message; never silently reinterpret it.

World save and reopen must preserve service output. Unloaded chunks are not automatically failed machines. The campaign model can remember logical nodes without retaining entire chunks. Avoid permanently force-loading the map. Rebind physical indicators on chunk load or through controlled site update work.

Check generator and reserve behavior after unload, reconnect, player death, dimension changes, and restart. Narrative batteries advance only with explicit schedule transitions. Global timers must not drain them during a menu.

Minecraft death does not automatically rewind the world. Do not copy the standalone game's checkpoint semantics without implementing them. Initially prevent ordinary lethal hazards in the scenario. If death occurs, return to a safe campaign spawn with documented state preservation. Proper encounter checkpoints can be designed later.

## 25 Data driven content

Use structured definitions for sites, dialogue, radio messages, consequence rules, and ending fragments where it makes content maintainable. Start with a small validated format; do not design a universal scripting language.

Namespace custom data under `data/signal/` with documented resource locations. Put client assets under `assets/signal/`. Use translation keys for player-facing text. A server-side content loader validates references and logs actionable errors, including filename and ID.

Illustrative radio definition:

```json
{
  "id": "signal:bellwether/local_restored",
  "speaker": "nia",
  "textKey": "radio.signal.bellwether.local_restored",
  "requiredFacts": ["bellwether.local_service_active"],
  "repeatPolicy": "once",
  "transcript": true
}
```

This is a proposed custom schema, not an existing Forge API. Implement its loader and tests before claiming the format works. Reloading content must not repeat one-shot outcomes or invalidate an active campaign silently.

Maintain a narrow fact vocabulary and explicit event types. Avoid hundreds of opaque boolean flags with no owners. Document major state transitions.

## 26 Linux and build delivery

Use lowercase file and resource names, portable Java paths, and the Gradle wrapper. Preserve executable permission on `gradlew` in version control. Do not assume a Windows launcher directory, drive letter, backslash separators, or a user's home path.

Provide Linux commands from the project root: inspect `java -version`, run `./gradlew build`, and use the MDK's actual development run task such as `./gradlew runClient`. Document whichever tasks are really configured. Do not invent `runGameTestServer` support without verifying the chosen MDK configuration.

Provide Windows equivalents using `gradlew.bat` if tested or clearly labeled untested. The output JAR location follows the actual build, normally under `build/libs`; name the actual artifact rather than guessing.

Installation instructions should describe a separate clean Minecraft instance with the exact Minecraft and Forge versions, the built mod JAR in its mods folder, and scenario initialization in a new disposable test world. Do not automatically touch existing player instances. Shaders and external mods are not required to test milestone one.

A JAR compiling on Linux is not proof of successful graphical rendering. Report build validation, server-side load checks, and graphical playtesting separately. If display access is absent, give a concise manual launch and interaction checklist.

Do not ask the owner to reinstall their operating system or change drivers merely to compensate for a broken mod. Identify actual logs and errors first.

## 27 Milestones and acceptance gates

### M0 Runnable Forge foundation

Official MDK pinned to target versions; mod metadata; stable mod ID; common/client separation; valid assets for one registered functional block; build succeeds; development launch instructions accurate. Record any network or graphical blockers.

### M1 Walkable repair room

Generated safe test room and tower silhouette; generator, distribution panel, pump and relay bindings; fault inspection; fuse isolation and repair; allocation preview; apply action; visible pump and relay output; one contextual radio acknowledgment; state survives world restart.

M1 is complete only when a player can walk to the equipment, understand the 6-unit supply constraint, activate valid services, and observe real world changes. The mod JAR and scenario command must be available. No home scene or combat is required yet.

### M2 Radio and route choice

Handheld radio interaction; channels; transcript; replay; old archive recording; antenna alignment; local versus authenticated routing; server-validated consequences; discovered information persists. All critical information works without audio.

### M3 Commitments and delegation

Explicit schedule; previewed work and travel costs; one accepted promise; renegotiation call; Nia training verification; bounded delegated task; one-shot consequence handling. Inspection does not consume time.

### M4 Home and complete first playable

Small home route or explicit transition; Rowan actor and receiver activity; on-time, renegotiated and missed/follow-up scenes; three full branches; safe characters; no affection meter; debug reset available only in test context.

### M5 Evidence grounded transmission

Fragment selector; visible provenance in debug mode; reachable-route presentation; final receiver lights; coherent outcome from each branch; no unearned pattern statements.

### M6 Atmospheric vertical slice

Forest road, settlement, pump site, relay compound, expanded original assets and ambient audio; ordinary character details; consistent art; performance profiling; optionally one carefully scoped encounter after the noncombat game works.

Do not skip M1 to design a giant map or add a gun mod. Later work is valuable only if the foundational loop survives it.

## 28 Tests that matter

Pure Java unit tests should cover allocation totals and invalid requests, relay prerequisites, reserve expiration under explicit schedule advancement, accurate previews, promise transitions, stale revision rejection, consequence idempotence, and ending provenance. Use test infrastructure compatible with the actual Gradle setup; do not invent passing tests.

Minecraft integration checks should cover registration, resource resolution, block placement and reload, menu opening, validated repair item consumption, server-authoritative configuration, saved-state reattachment, and scenario placement safeguards. Use Forge GameTests where configured and appropriate. Do not replace integration verification with mocks that merely mirror implementation.

Required scenarios:

- Pump plus local transmitter runs on 6 units.
- Pump plus regional relay is rejected on 6 units without reserve.
- Valid reserve permits the defined window and reverts safely once.
- Misaligned relay remains unavailable even with sufficient supply.
- Repeated repair request consumes only one fuse.
- A stale panel snapshot does not overwrite a newer configuration.
- World reload preserves repair, allocation and message state.
- Leaving the chunk does not invent a service failure or erase the machine.
- Replaying a radio clip adds no duplicate event.
- Menus and idle time do not expire promises.
- A missed promise cannot select the fulfilled ending fragment.
- A single training choice without verified independent action cannot select durable-teaching narration.
- Ordinary worlds remain unchanged until explicit scenario initialization.
- Common code does not crash from client-only class loading on a dedicated server.

For graphical testing, record actual launch, inspect HUD and menu scale, confirm textures, walk collision boundaries, activate services, listen or read acknowledgments, save, reopen, and verify indicators. If you cannot perform a step, state it plainly.

## 29 Performance and maintainability

Avoid scanning the whole world for machines each tick. Maintain registered site nodes and event-driven dirty updates. Evaluate the tiny domain model on changes; use bounded intervals for cosmetic updates. Keep world edits and block updates scoped.

Sound loops must stop when the relevant machine, scene, or world unloads. Screens must release listeners and not duplicate packet handlers on reopen. Avoid leaking client state between worlds. Clear campaign presentation caches appropriately on disconnect.

Do not add dozens of permanent chunk tickets. Keep deferred consequences independent of chunk presence and apply visible updates when the site is loaded. Limit persistent event volume to meaningful canonical actions, not every frame of movement.

Profile real bottlenecks before claiming optimization. The performance target is smooth ordinary play on reasonable hardware with settings stated. Do not build only for the owner's high-end machine or assert a universal frame rate without testing.

## 30 What not to build yet

No radiation, anomalies, mutants, faction warfare, custom guns, weapon attachments, ballistics, armor simulation, vehicles, complex thirst or hunger, procedural endless missions, companion squads, custom skeleton rigs, huge texture downloads, online accounts, live AI NPCs, multiplayer narrative, economy grind, romance system, or total overhaul of vanilla content.

S.T.A.L.K.E.R. atmosphere does not require reproducing every S.T.A.L.K.E.R. mechanic. SIGNAL's distinct contribution is repair, trust, and transmission. A muddy utility compound with convincing machinery and one believable voice is a stronger first result than a broken collection of survival mods.

Possible later features: rare hazardous zones, authored security encounters, additional infrastructure media, richer local governance, original weapons or verified compatible integrations, expanded character interactions, and optional visual enhancements. Each must serve existing gameplay and earn its production cost.

## 31 Writing tone and sample scene

Avoid treating every NPC as a philosopher. Concrete work and ordinary relationships carry the meaning. Radio dialogue is concise. Children sound like children. Elias is competent without being superhuman or always right.

Arrival:

Nia: “Pump's on the bypass. We have water. We can't tell the depot what we need.”

Elias: “You isolated the supply?”

Nia: “Fuse was heating. I wasn't going to wait for smoke.”

Elias: “Good. Show me the meter.”

A partial repair report:

Elias: “Local voice and the pump. Regional relay won't fit on this generator.”

Nia: “Then tell the courier what we have. Not what we wish we had.”

A home call:

Elias: “The longer job takes the evening. I said I'd be there.”

Mara: “Can someone finish it?”

Elias: “Nia can, if we verify the fallback.”

Mara: “Then decide what you're actually promising.”

The player must have an implemented option behind that line. Dialogue cannot praise delegation if no delegation command exists.

Home:

Rowan: “It only catches a little.”

Elias: “Let's hear the little.”

The scene gives the player a receiver interaction and room to listen. It does not deliver a speech about becoming a better man.

## 32 Definition of done and reporting

For each increment, report what now happens in-game, the files or modules changed, the exact build and launch commands used, actual test results, untested parts, and the next dependency. Keep implementation claims separate from planned features.

Maintain these project documents as useful small files: README with setup; implementation status; decision log; compatibility pins; asset provenance; and a short design summary linking this handoff. Keep them current rather than generating lengthy plans at every turn.

Use an existing CLAUDE.md if present and applicable; avoid overwriting it with the entire brief. A concise project instruction file can summarize platform constraints and reference the handoff.

Never report “fully playable” when only compilation was verified. Never make up a screenshot or state that Linux works because Java is portable. Validate what you can and provide a precise manual checklist for the rest.

If an implementation choice is reversible and within scope, make it and record it. Ask when it changes the platform, creative direction, existing user data, or important dependency requirements. Do not require approval for every class or texture.

## 33 Immediate execution prompt

When this document is provided, treat the following as the active task:

Build SIGNAL's M0 and M1 in the current workspace. Inspect the project and applicable instructions first. Use Minecraft Java 1.20.1, a verified pinned Forge 47.x MDK, and Java 17 unless an existing compatible project requires a justified adjustment. Linux is required.

Create a real Forge mod with a safe explicit scenario-bootstrap command. The scenario must contain a walkable repair room, visible generator, distribution panel, pump and relay states, and a tower silhouette. Supply is 6 units, pump demand 5, local transmitter demand 1, relay demand 3. Implement inspection, an isolated fuse repair, valid allocation preview and apply, world-space feedback, and one authored radio acknowledgment. Save and reopen must preserve the result.

Keep state authoritative on the logical server, assets bundled, paths portable, and the domain rules independently testable. Do not add external mod dependencies, shaders, guns, runtime AI, or a browser UI. Do not automatically alter ordinary worlds or existing launcher instances. Do not attempt the whole campaign.

Build the JAR and run appropriate tests. Launch and visually test if the environment allows; otherwise state that limitation and give exact local run steps. Finish with a concrete playable increment, accurate artifact paths, and a concise explanation of what is working. Continue to later milestones only after M1 is integrated and verified.

## 34 Longer term destination

Bellwether proves the pattern. Later regions include a monitored urban service ward, a freight exchange, a forest archive line, a flood district connected to Elias's wartime history, and Origin Station. They reuse established tools and rules rather than demand unrelated new engines.

The final campaign should offer tactical danger, mystery, cooperation, difficult truthful reports, independent technicians, and home scenes that retain joy. The network changes because of the player. People can learn enough to maintain parts of it without him.

At Origin, a transmission assembled from real history travels only through surviving authorized paths. Its reach can be incomplete. Its message can contain gratitude and unresolved harm. At home, two receivers light.

The aim is to build a world where reconnecting a machine and keeping a promise are both things the player can actually do.
