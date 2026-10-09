# SIGNAL – project instructions

Original Minecraft adventure mod. Full brief: `docs/HANDOFF.md`. Status: `docs/IMPLEMENTATION_STATUS.md`.

- Platform is fixed: Minecraft Java 1.20.1, Forge 47.4.10 (pinned), Java 17, official MDK Gradle wrapper.
  Do not switch to Fabric/NeoForge/another version or replace the wrapper. Linux is first-class.
- Mod id `signal`, package `io.github.b1tr0n1n.signal`. Verify 1.20.1 API signatures against the mapped JAR
  (`javap`) or Forge sources; see `docs/COMPATIBILITY.md` for gotchas already found.
- Game rules live in `domain/` (pure Java, JUnit-tested). Minecraft code validates on the server and
  projects state onto blocks; block entities hold only a node id. Client classes stay in `client/` and are
  reached only through `DistExecutor`.
- Never change ordinary worlds automatically; scenario changes happen only via `/signal scenario ...` and are
  announced. Do not touch user worlds or launcher instances.
- Before claiming a feature: `./gradlew build` (unit tests) and `./gradlew runGameTestServer` (integration).
  Report graphical testing separately from build/test results.
- Record reversible decisions in `docs/DECISIONS.md` and every shipped asset in `docs/ASSET_PROVENANCE.md`.
  No copied or ripped assets; textures come from `tools/gen_textures.py`.
- Never commit `run/`, worlds, or credentials.
