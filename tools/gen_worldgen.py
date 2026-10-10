#!/usr/bin/env python3
"""Generate SIGNAL's worldgen data pack (biomes, features, world preset) for Minecraft 1.21.1.

The "Meridian Corridor" world type reuses Minecraft's overworld terrain noise and vanilla placed
features by reference, and replaces the biome layer with six original biomes tuned for the setting:
wet conifer forest, logged woodland, open moor, stony ridges, rivers and lakes. Grey skies, greenish
haze, murky water, olive foliage, drifting ash, dead snags, mud and no flowers, farm animals or
villages.

Usage (from the project root):  python3 tools/gen_worldgen.py
"""
import json
import os

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "data")
NS = "signal"


def write(rel, obj):
    path = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def rgb(hexstr):
    return int(hexstr.lstrip("#"), 16)


# ---------------------------------------------------------------- shared vanilla feature steps
ORES = ["minecraft:ore_dirt", "minecraft:ore_gravel", "minecraft:ore_granite_upper", "minecraft:ore_granite_lower",
        "minecraft:ore_diorite_upper", "minecraft:ore_diorite_lower", "minecraft:ore_andesite_upper",
        "minecraft:ore_andesite_lower", "minecraft:ore_tuff", "minecraft:ore_coal_upper", "minecraft:ore_coal_lower",
        "minecraft:ore_iron_upper", "minecraft:ore_iron_middle", "minecraft:ore_iron_small", "minecraft:ore_gold",
        "minecraft:ore_gold_lower", "minecraft:ore_redstone", "minecraft:ore_redstone_lower", "minecraft:ore_diamond",
        "minecraft:ore_diamond_medium", "minecraft:ore_diamond_large", "minecraft:ore_diamond_buried",
        "minecraft:ore_lapis", "minecraft:ore_lapis_buried", "minecraft:ore_copper", "minecraft:underwater_magma",
        "minecraft:disk_sand", "minecraft:disk_clay", "minecraft:disk_gravel"]


def steps(local=(), vegetal=(), lakes=True):
    """The 11 GenerationStep.Decoration slots, vanilla order."""
    return [
        [],                                                                      # raw_generation
        ["minecraft:lake_lava_underground"] if lakes else [],                    # lakes (no surface lava)
        ["minecraft:amethyst_geode", *local],                                    # local_modifications
        ["minecraft:monster_room", "minecraft:monster_room_deep"],               # underground_structures
        [],                                                                      # surface_structures
        [],                                                                      # strongholds
        ORES,                                                                    # underground_ores
        [],                                                                      # underground_decoration
        ["minecraft:spring_water", "minecraft:spring_lava"],                     # fluid_springs
        list(vegetal),                                                           # vegetal_decoration
        ["minecraft:freeze_top_layer"],                                          # top_layer_modification
    ]


CARVERS = {"air": ["minecraft:cave", "minecraft:cave_extra_underground", "minecraft:canyon"]}

MONSTERS = [
    {"type": "minecraft:spider", "weight": 100, "minCount": 4, "maxCount": 4},
    {"type": "minecraft:zombie", "weight": 95, "minCount": 4, "maxCount": 4},
    {"type": "minecraft:skeleton", "weight": 100, "minCount": 4, "maxCount": 4},
    {"type": "minecraft:creeper", "weight": 100, "minCount": 4, "maxCount": 4},
    {"type": "minecraft:slime", "weight": 100, "minCount": 4, "maxCount": 4},
    {"type": "minecraft:enderman", "weight": 10, "minCount": 1, "maxCount": 4},
]
# Wildlife only: no farm animals in the corridor.
WILD = [
    {"type": "minecraft:wolf", "weight": 8, "minCount": 2, "maxCount": 4},
    {"type": "minecraft:fox", "weight": 8, "minCount": 2, "maxCount": 3},
    {"type": "minecraft:rabbit", "weight": 4, "minCount": 2, "maxCount": 3},
]


def spawners(creature=WILD, water=()):
    return {
        "ambient": [{"type": "minecraft:bat", "weight": 10, "minCount": 8, "maxCount": 8}],
        "axolotls": [], "creature": list(creature), "misc": [], "monster": MONSTERS,
        "underground_water_creature": [{"type": "minecraft:glow_squid", "weight": 10, "minCount": 4, "maxCount": 6}],
        "water_ambient": list(water), "water_creature": [],
    }


def effects(sky, fog, water, water_fog, grass, foliage, ash):
    e = {
        "sky_color": rgb(sky), "fog_color": rgb(fog),
        "water_color": rgb(water), "water_fog_color": rgb(water_fog),
        "grass_color": rgb(grass), "foliage_color": rgb(foliage),
        "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0},
        # Quiet: occasional sparse music only, long gaps.
        "music": {"sound": "minecraft:music.overworld.old_growth_taiga", "min_delay": 24000, "max_delay": 48000,
                  "replace_current_music": False},
    }
    if ash:
        e["particle"] = {"options": {"type": "minecraft:white_ash"}, "probability": ash}
    return e


def biome(name, temperature, downfall, eff, local, vegetal, creature=WILD, water=()):
    write(f"{NS}/worldgen/biome/{name}.json", {
        "has_precipitation": True, "temperature": temperature, "downfall": downfall,
        "effects": eff, "spawners": spawners(creature, water), "spawn_costs": {},
        "carvers": CARVERS, "features": steps(local, vegetal),
    })


# ---------------------------------------------------------------- custom features
GROUND = ["minecraft:grass_block", "minecraft:podzol", "minecraft:coarse_dirt", "minecraft:dirt",
          "minecraft:moss_block", "minecraft:mud"]


def simple(name):
    return {"type": "minecraft:simple_state_provider", "state": {"Name": name}}


def disk(name, block, rmin, rmax, targets=GROUND):
    write(f"{NS}/worldgen/configured_feature/{name}.json", {
        "type": "minecraft:disk",
        "config": {"half_height": 1, "radius": {"type": "minecraft:uniform", "min_inclusive": rmin, "max_inclusive": rmax},
                   "state_provider": {"fallback": simple(block), "rules": []},
                   "target": {"type": "minecraft:matching_blocks", "blocks": targets}}})


def surface_placed(name, feature, chance, extra=()):
    write(f"{NS}/worldgen/placed_feature/{name}.json", {"feature": feature, "placement": [
        {"type": "minecraft:rarity_filter", "chance": chance},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
        *extra,
        {"type": "minecraft:biome"}]})


ON_GROUND = {"type": "minecraft:block_predicate_filter", "predicate": {
    "type": "minecraft:matching_blocks", "offset": [0, -1, 0], "blocks": GROUND}}

# Dead spruce snag: a bare trunk 3-8 blocks tall.
write(f"{NS}/worldgen/configured_feature/dead_snag.json", {
    "type": "minecraft:block_column",
    "config": {"allowed_placement": {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
               "direction": "up", "prioritize_tip": False,
               "layers": [{"height": {"type": "minecraft:uniform", "min_inclusive": 3, "max_inclusive": 8},
                           "provider": {"type": "minecraft:simple_state_provider",
                                        "state": {"Name": "minecraft:spruce_log", "Properties": {"axis": "y"}}}}]}})
# Stump: a 1-2 block trunk (logged ground).
write(f"{NS}/worldgen/configured_feature/stump.json", {
    "type": "minecraft:block_column",
    "config": {"allowed_placement": {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
               "direction": "up", "prioritize_tip": False,
               "layers": [{"height": {"type": "minecraft:uniform", "min_inclusive": 1, "max_inclusive": 2},
                           "provider": {"type": "minecraft:simple_state_provider",
                                        "state": {"Name": "minecraft:stripped_spruce_log", "Properties": {"axis": "y"}}}}]}})
disk("mud_patch", "minecraft:mud", 2, 4)
disk("coarse_patch", "minecraft:coarse_dirt", 2, 5)
disk("podzol_patch", "minecraft:podzol", 2, 4)
disk("gravel_spill", "minecraft:gravel", 2, 4, GROUND + ["minecraft:stone", "minecraft:andesite"])
disk("stone_scree", "minecraft:andesite", 1, 3, GROUND + ["minecraft:stone"])

# Shorelines: Minecraft's surface rules lay bright sand at the waterline; turn it to mud and gravel.
disk("shore_mud", "minecraft:mud", 2, 5, ["minecraft:sand", "minecraft:sandstone", "minecraft:dirt", "minecraft:grass_block"])
disk("shore_gravel", "minecraft:gravel", 2, 4, ["minecraft:sand", "minecraft:sandstone"])
for shore, cnt in (("shore_mud", 6), ("shore_gravel", 4)):
    write(f"{NS}/worldgen/placed_feature/{shore}.json", {"feature": f"{NS}:{shore}", "placement": [
        {"type": "minecraft:count", "count": cnt},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR_WG"},
        {"type": "minecraft:biome"}]})
SHORE = [f"{NS}:shore_mud", f"{NS}:shore_gravel"]

surface_placed("dead_snag_rare", f"{NS}:dead_snag", 6, [ON_GROUND])
surface_placed("dead_snag_common", f"{NS}:dead_snag", 2, [ON_GROUND])
surface_placed("stumps", f"{NS}:stump", 2, [ON_GROUND])
surface_placed("mud_patch", f"{NS}:mud_patch", 2)
surface_placed("mud_patch_wet", f"{NS}:mud_patch", 1)
surface_placed("coarse_patch", f"{NS}:coarse_patch", 3)
surface_placed("podzol_patch", f"{NS}:podzol_patch", 2)
surface_placed("gravel_spill", f"{NS}:gravel_spill", 2)
surface_placed("stone_scree", f"{NS}:stone_scree", 2)

# ---------------------------------------------------------------- biomes
# Shared palette: overcast sky, grey-green haze, olive vegetation, peat-brown water.
SKY, FOG = "#7c858a", "#8f988e"
biome("meridian_forest", 0.55, 0.9,
      effects(SKY, "#7f8a7f", "#3b4a3c", "#16201a", "#5e6b3f", "#465532", 0.0035),
      local=["minecraft:forest_rock", f"{NS}:podzol_patch", f"{NS}:mud_patch", *SHORE],
      vegetal=["minecraft:glow_lichen", "minecraft:patch_large_fern", "minecraft:trees_old_growth_spruce_taiga",
               f"{NS}:dead_snag_rare", "minecraft:patch_grass_taiga", "minecraft:patch_dead_bush",
               "minecraft:brown_mushroom_old_growth", "minecraft:red_mushroom_old_growth", "minecraft:patch_berry_common"])
biome("meridian_woodland", 0.6, 0.85,
      effects(SKY, FOG, "#3d4b3d", "#18221b", "#6a7347", "#525d37", 0.003),
      local=["minecraft:forest_rock", f"{NS}:coarse_patch", f"{NS}:mud_patch", *SHORE],
      vegetal=["minecraft:glow_lichen", "minecraft:patch_large_fern", "minecraft:trees_taiga", f"{NS}:stumps",
               f"{NS}:dead_snag_common", "minecraft:patch_grass_taiga_2", "minecraft:patch_dead_bush",
               "minecraft:brown_mushroom_taiga"])
biome("meridian_moor", 0.65, 0.95,
      effects(SKY, "#949c90", "#43503f", "#1b241d", "#737a4c", "#5d663d", 0.0045),
      local=[f"{NS}:mud_patch_wet", f"{NS}:coarse_patch", *SHORE],
      vegetal=["minecraft:glow_lichen", "minecraft:patch_tall_grass_2", "minecraft:patch_large_fern",
               "minecraft:trees_windswept_hills", f"{NS}:dead_snag_common", "minecraft:patch_grass_taiga",
               "minecraft:patch_dead_bush", "minecraft:patch_waterlily"])
biome("meridian_ridge", 0.45, 0.7,
      effects("#78818a", "#8a9290", "#3e4a42", "#172019", "#6b7148", "#556039", 0.005),
      local=["minecraft:forest_rock", f"{NS}:gravel_spill", f"{NS}:stone_scree", *SHORE],
      vegetal=["minecraft:glow_lichen", "minecraft:trees_windswept_hills", f"{NS}:dead_snag_common",
               "minecraft:patch_grass_badlands", "minecraft:patch_dead_bush"])
biome("meridian_river", 0.6, 0.9,
      effects(SKY, FOG, "#3a463a", "#141c16", "#666e45", "#4f5a36", 0.0025),
      local=[f"{NS}:mud_patch_wet", *SHORE],
      vegetal=["minecraft:glow_lichen", "minecraft:trees_water", "minecraft:patch_grass_taiga",
               "minecraft:patch_sugar_cane", "minecraft:seagrass_river"],
      water=[{"type": "minecraft:salmon", "weight": 5, "minCount": 1, "maxCount": 5}])
biome("meridian_lake", 0.6, 0.9,
      effects(SKY, FOG, "#36423a", "#121a15", "#666e45", "#4f5a36", 0.002),
      local=[*SHORE],
      vegetal=["minecraft:seagrass_river", "minecraft:patch_waterlily"],
      water=[{"type": "minecraft:cod", "weight": 5, "minCount": 1, "maxCount": 4}])

# ---------------------------------------------------------------- world preset
FULL = [-1.0, 1.0]


def point(b, humidity=FULL, continentalness=(-0.19, 1.0), erosion=(-0.375, 1.0), weirdness=FULL, temperature=FULL):
    return {"biome": f"{NS}:{b}", "parameters": {
        "temperature": list(temperature), "humidity": list(humidity), "continentalness": list(continentalness),
        "erosion": list(erosion), "weirdness": list(weirdness), "depth": 0.0, "offset": 0.0}}


VALLEY = (-0.05, 0.05)
SIDES = [(-1.0, -0.05), (0.05, 1.0)]
biomes = [point("meridian_lake", continentalness=(-1.2, -0.19), erosion=FULL),
          point("meridian_river", weirdness=VALLEY, erosion=(-0.2, 1.0))]
for w in SIDES:
    biomes += [
        point("meridian_moor", humidity=(-1.0, -0.15), weirdness=w),
        point("meridian_woodland", humidity=(-0.15, 0.12), weirdness=w),
        point("meridian_forest", humidity=(0.12, 1.0), weirdness=w),
        point("meridian_ridge", continentalness=(0.3, 1.0), erosion=(-1.0, -0.375), weirdness=w),
    ]

write(f"{NS}/worldgen/world_preset/meridian.json", {"dimensions": {
    "minecraft:overworld": {"type": "minecraft:overworld", "generator": {
        "type": "minecraft:noise", "settings": "minecraft:overworld",
        "biome_source": {"type": "minecraft:multi_noise", "biomes": biomes}}},
    "minecraft:the_nether": {"type": "minecraft:the_nether", "generator": {
        "type": "minecraft:noise", "settings": "minecraft:nether",
        "biome_source": {"type": "minecraft:multi_noise", "preset": "minecraft:nether"}}},
    "minecraft:the_end": {"type": "minecraft:the_end", "generator": {
        "type": "minecraft:noise", "settings": "minecraft:end", "biome_source": {"type": "minecraft:the_end"}}},
}})
# List it on the Create World screen's "World Type" button.
write("minecraft/tags/worldgen/world_preset/normal.json", {"replace": False, "values": [f"{NS}:meridian"]})

ALL = [f"{NS}:meridian_{b}" for b in ("forest", "woodland", "moor", "ridge", "river", "lake")]
write(f"{NS}/tags/worldgen/biome/meridian.json", {"replace": False, "values": ALL})
write("minecraft/tags/worldgen/biome/is_overworld.json", {"replace": False, "values": ALL})
write("minecraft/tags/worldgen/biome/is_river.json", {"replace": False, "values": [f"{NS}:meridian_river"]})
write("minecraft/tags/worldgen/biome/is_forest.json", {"replace": False,
                                                       "values": [f"{NS}:meridian_forest", f"{NS}:meridian_woodland"]})
print("worldgen written to", os.path.normpath(ROOT))
