# Compatibility pins

| Component | Pinned value | Where it is set |
| --- | --- | --- |
| Minecraft Java Edition | **1.21.1** | `gradle.properties` `minecraft_version` |
| NeoForge | **21.1.256** (the version pinned by the official 1.21.1 MDK on 2026-10-10; 21.1.257 was the newest on Maven) | `gradle.properties` `neo_version` |
| NeoForge range accepted at runtime | `[21.1.256,)` | `neoforge.mods.toml` template |
| Java | **21** (Gradle toolchain; OpenJDK 21.0.12 used here) | `build.gradle` `java.toolchain` |
| Gradle wrapper | 9.2.1 (from the official MDK) | `gradle/wrapper/gradle-wrapper.properties` |
| ModDevGradle | 2.0.148 | `build.gradle` `plugins` |
| Mappings | Mojang official + Parchment 2024.11.17 | `gradle.properties` |
| Mod id / version | `signal` / 0.2.0 | `gradle.properties` |
| Test-only | JUnit Jupiter 5.10.2, platform launcher 1.10.2 | `gradle.properties`, `build.gradle` |
| Required runtime mods | **none** | — |
| Supported optional mods | Iris (NeoForge 1.21.1), Distant Horizons (NeoForge 1.21.1) and shader packs; see `GRAPHICS.md` | not bundled |

History: milestone one was built on Forge 1.20.1 / 47.4.10 / Java 17. The owner moved the project to
NeoForge 1.21.1 on 2026-10-10 for Iris shader support (decision log #22).

## API notes verified against NeoForge 21.1.256 / 1.21.1

- Registries: `DeferredRegister.Blocks/Items` with `DeferredBlock`/`DeferredItem`; block entities via
  `DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ...)`.
- Networking: `RegisterPayloadHandlersEvent` → `registrar(version).playToServer/playToClient(type, codec, handler)`.
  Handlers run on the **main thread** by default (`PayloadRegistrar` initial `HandlerThread.MAIN`).
  Send with `PacketDistributor.sendToPlayer/sendToServer`.
- `SavedData.Factory(constructor, (tag, registries) -> load, null)`; `save(CompoundTag, HolderLookup.Provider)`.
- Block entities: `saveAdditional/loadAdditional(CompoundTag, HolderLookup.Provider)`.
- Blocks: `useWithoutItem(...)` (called after a held item declines), `codec()` returning a `MapCodec`.
- Written books: `DataComponents.WRITTEN_BOOK_CONTENT` with `WrittenBookContent`.
- Screens: `Screen.render` draws the background first, then widgets; custom panel content goes in
  `renderBackground`.
- GameTest templates live in `data/<ns>/structure/` (singular since 1.21). Batch test volumes are roofed
  with **barrier** blocks; terrain surveys must look through them.
- `GameTestHelper.makeMockServerPlayerInLevel()` cannot join a Forge-family server; use `FakePlayerFactory`.
- Fog: `ViewportEvent.RenderFog` must be cancelled for modified distances to apply.
- Worldgen: the Create World "World Type" list is the `#minecraft:normal` world-preset tag. Mineshafts are
  enabled by the `#is_forest`/`#is_river` biome tags (not `#is_overworld`, which only feeds strongholds).
  Any data-pack world type shows the one-time "experimental settings" warning.

## Configured Gradle tasks

`build`, `test`, `runClient` (optional `-PquickPlayWorld=<save>`), `runServer`, `runGameTestServer`, `runData`.
