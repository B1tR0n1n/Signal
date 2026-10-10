# Asset provenance

Every shipped asset is listed here with source, licence and author. Nothing is taken from S.T.A.L.K.E.R.
or any other game; no ripped audio, textures, models or music.

## Textures (`src/main/resources/assets/signal/textures/`)

| Files | Source | Author | Licence | Notes |
| --- | --- | --- | --- | --- |
| `block/machine_side.png`, `block/machine_top.png`, `block/generator_front_{isolated,running}.png`, `block/distribution_panel_front_{fault,ok}.png`, `block/pump_controller_front_{0..3}.png`, `block/relay_controller_front_{off,powered,linked}.png`, `block/service_lamp_{off,on}.png`, `item/handheld_radio.png`, `item/replacement_fuse.png` | Generated procedurally by `tools/gen_textures.py` (seeded, reproducible) | SIGNAL project (b1tr0n1n), drawn by the generator script | Project licence (All Rights Reserved, see `gradle.properties`) | Original 16x16 pixel art. Not placeholders. States differ by shape as well as colour (switch blade, needle angle, bar count, cage pattern). |

Regenerate with `python3 tools/gen_textures.py` (requires Pillow).

## Models, blockstates, language

`assets/signal/blockstates/*.json`, `assets/signal/models/**/*.json` and `assets/signal/lang/en_us.json`
are original project files. Block models parent Minecraft's built-in `block/orientable`, `block/cube_all`
and `item/generated` model templates by reference only; no Mojang assets are copied into the JAR.

## Worldgen data

`data/signal/worldgen/**`, `data/signal/tags/**` and `data/minecraft/tags/worldgen/**` are original project
data written by `tools/gen_worldgen.py`. Biomes reference Minecraft's own placed features and sounds by id
(e.g. `minecraft:trees_old_growth_spruce_taiga`). The vanilla data generator was run locally only to read the
reference format.

One file is **derived from Minecraft's own data**: `data/signal/worldgen/noise_settings/meridian.json`. Minecraft
accepts only complete noise settings, so the generator reads the overworld's noise settings from the local
game jar and substitutes three density-function references (`depth`, `factor`, `sloped_cheese`) with
SIGNAL's (`data/signal/worldgen/density_function/meridian/*`, original, which wrap vanilla's functions by
reference). It is terrain configuration data, not art, sound or a model (decision #32).

## Test structures

`data/signal/structures/empty_5.nbt` and `bellwether_site.nbt` are empty GameTest volumes written by
`tools/gen_test_structures.py`. They contain no blocks.

## Audio

**No audio files are shipped.** Machine and radio cues reuse vanilla Minecraft sound *events* by
reference (`BLASTFURNACE_FIRE_CRACKLE`, `PISTON_CONTRACT`, `COMPARATOR_CLICK`, `IRON_TRAPDOOR_*`,
`LEVER_CLICK`, `CHAIN_PLACE`, `UI_BUTTON_CLICK`, `BLAZE_SHOOT`). These play from the player's own
Minecraft installation and are not redistributed. All critical information is also delivered as text.

## In-world authored text

Sign text and the maintenance log book are original English text written for SIGNAL
(`scenario/BellwetherScenario.java`). Radio lines are original and live in `en_us.json`; several are the
owner's working-canon lines from the handoff.
