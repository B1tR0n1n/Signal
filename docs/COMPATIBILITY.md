# Compatibility pins

| Component | Pinned value | Where it is set |
| --- | --- | --- |
| Minecraft Java Edition | 1.20.1 | `gradle.properties` `minecraft_version` |
| Forge | **47.4.10** (the *recommended* 1.20.1 build on 2026-10-09; latest was 47.4.26) | `gradle.properties` `forge_version` |
| Forge range accepted at runtime | `[47.4.10,48)` | `gradle.properties` `forge_version_range` → `mods.toml` |
| Java | 17 (Gradle toolchain); JDK used for builds: OpenJDK 17.0.20 | `build.gradle` `java.toolchain` |
| Gradle wrapper | 8.8 (from the official MDK) | `gradle/wrapper/gradle-wrapper.properties` |
| ForgeGradle | `[6.0,6.2)` (official MDK default) | `build.gradle` `plugins` |
| Mappings | `official` 1.20.1 (Mojang names) | `gradle.properties` |
| Mod id / version | `signal` / 0.1.0 | `gradle.properties` |
| Test-only | JUnit Jupiter 5.10.2, platform launcher 1.10.2 | `gradle.properties`, `build.gradle` |
| Optional runtime dependencies | **none** | — |

The project was created from the unmodified official MDK
(`forge-1.20.1-47.4.10-mdk.zip`); its wrapper JAR and scripts are the MDK's own. Only the mod
identity, a JUnit test setup and an opt-in `-PquickPlayWorld` client argument were added.

## API notes verified against 47.4.10

Signatures were checked with `javap` against the mapped game JAR and Forge sources, not from tutorials:

- `SavedData` via `DimensionDataStorage.computeIfAbsent(Function<CompoundTag,T>, Supplier<T>, String)`.
- Networking: `NetworkRegistry.newSimpleChannel(...)`, `SimpleChannel.messageBuilder(type, id, NetworkDirection)
  .encoder().decoder().consumerMainThread().add()`.
- `FMLJavaModLoadingContext` is injected into the `@Mod` constructor (47.4.x supports it; the static
  `get()` is deprecated for removal). `ResourceLocation.fromNamespaceAndPath/parse` replace the deprecated constructors.
- GameTests: `@GameTestHolder` + `@PrefixGameTestTemplate(false)`, templates in `data/signal/structures/`.
  `GameTestHelper.makeMockServerPlayerInLevel()` **fails on Forge 1.20.1** (Forge's login hooks need a Netty
  pipeline); tests use `FakePlayerFactory.get(...)` instead.
- `GameTestSequence.thenWaitUntil(long, Runnable)` asserts success at *exactly* that tick; use the untimed overload for "within".

## Configured Gradle tasks (verified to exist in this MDK)

`build`, `test`, `runClient`, `runServer`, `runGameTestServer`, `runData`.
