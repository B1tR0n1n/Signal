# Owner art (Aseprite sources)

Drawn by the owner in Aseprite. Each `art/<path>.aseprite` exports to
`src/main/resources/assets/signal/textures/<path>.png` with `sh tools/export_art.sh`, and
`tools/gen_textures.py` never overwrites a texture that has a source here.

| Folder | Size | Notes |
| --- | --- | --- |
| `block/` | 16x16 | Same file name as the generated texture it replaces (e.g. `block/machine_side`). |
| `item/` | 16x16 | Transparent background. |
| `entity/` | per model | Mob skins. The model fixes the size and where each body part's faces sit (UV layout); a layout template is provided with each new mob. |

Animated textures: draw the frames in Aseprite's timeline; the export stacks them vertically and the
mob or block gets a `.png.mcmeta` (added when it is wired in).

Commit both the `.aseprite` source and the exported `.png`. Record every file in `docs/ASSET_PROVENANCE.md`.
