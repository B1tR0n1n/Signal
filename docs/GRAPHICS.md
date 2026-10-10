# Graphics: getting the most out of SIGNAL

SIGNAL runs on **NeoForge 1.21.1**, chosen so the official **Iris** shader loader is available
(Iris supports NeoForge from 1.21.1). Nothing below is required to play; SIGNAL has its own fog and
atmosphere without shaders. None of these mods or packs are bundled with SIGNAL. Install them yourself
in the same CurseForge profile.

> Status: SIGNAL's *own* visuals (biomes, fog, site) were verified in a software-rendered client. The
> shader stack below was **not** run here (no GPU in the build container), so treat the versions as a
> starting point and check each project's page for the newest NeoForge 1.21.1 file.

## Recommended stack (RTX-class GPU)

| Mod / pack | Why | Where |
| --- | --- | --- |
| **Iris Shaders** (NeoForge 1.21.1 build) | Official shader loader. Includes Sodium-style rendering on NeoForge. | CurseForge / Modrinth "Iris Shaders", file for *NeoForge 1.21.1* |
| **Distant Horizons** (NeoForge 1.21.1 build) | Renders terrain far past normal view distance, so the tower and ridges stand out against miles of forest. | CurseForge / Modrinth "Distant Horizons" |
| Shader pack, pick one: | | put the `.zip` in the profile's `shaderpacks/` folder |
| - **Complementary Reimagined** | Best all-round: volumetric fog, wet reflections, god rays, Distant Horizons support. Start here. | CurseForge "Complementary Shaders - Reimagined" (r5.8.x lists 1.21.1 + Iris) |
| - **Rethinking Voxels** | Voxel ray-traced colored light and shadows, so the amber lamps and fire barrel really light the rooms. Built on Complementary. Iris only. | github.com/gri573/rethinking-voxels |
| - **PathMax** / **Serie** | Experimental full voxel path tracing on Iris. Heaviest option; screenshots and slow walks. | Modrinth / CurseForge |

Install order in CurseForge: add Iris and Distant Horizons to the `Signal` profile (Add Content → Mods),
launch once, close, copy the shader `.zip` into `shaderpacks/`, launch, then Options → Video Settings →
Shader Packs.

## Settings that suit the Meridian Corridor

- **Weather:** keep rain on. The scenario starts with rain, and wet surfaces are where shaders shine.
- **Complementary:** raise *Fog*/*Atmospheric Fog* a little, keep *Rain Puddles* on, and turn *Waving Plants* on.
- **Distant Horizons:** LOD distance 64-128 chunks on a 5090. Fog from shaders blends the LOD edge.
- **Vanilla (no shaders):** render distance 12-16; SIGNAL's fog pulls in inside Meridian biomes and tighter in rain.

## Known interactions

- Shader packs replace vanilla fog. SIGNAL's fog is then largely overridden; the pack's fog takes over.
- A reported Complementary + Distant Horizons issue shows gaps at screen edges at very high FOV on
  1.21.1 NeoForge; keep FOV near default if you see it.
- The world-creation screen warns that the SIGNAL world type uses *experimental settings*. This appears
  for any custom world type from a data pack; choose "I know what I'm doing" once per world.
