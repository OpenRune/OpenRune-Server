---
type: testing
title: Testing
description: How tests are organised and run in OpenRune-Server — JUnit 5 conventions with parallel execution, which modules have active unit tests, the cache-backed test convention, the dormant src/integration suites left from the RSMod GameTestState harness, CI boot verification, and live-client verification through the OpenRune Developer Tools MCP.
tags: [testing, junit, ci, integration, live-client]
verified:
  - by: openwiki/0.7.0
    at: 2026-10-02T20:46:41.242Z
sources:
  - id: openwiki-source-164e2da859b5277df81c7d94
    resource: repo://.github/workflows/ci.yml
  - id: openwiki-source-efd9a6a296f0be0af8b17b66
    resource: repo://api/bosses/src/test/kotlin/org/rsmod/api/bosses/runtime/EncounterRegistryTest.kt
  - id: openwiki-source-0f22a40e7a6aaad2a31ee889
    resource: repo://api/bosses/src/test/kotlin/org/rsmod/api/bosses/validation/SpecValidatorTest.kt
  - id: openwiki-source-fa625ed6f8b8b5264861511a
    resource: repo://api/game-process/build.gradle.kts
  - id: openwiki-source-6461fe39db8be9b79d7d88f0
    resource: repo://api/game-process/src/test/kotlin/org/rsmod/api/game/process/player/ForcedWalkTest.kt
  - id: openwiki-source-b08d1e81f9847ab592cc2c4a
    resource: repo://build-logic/src/main/kotlin/game-cache-test-conventions.gradle.kts
  - id: openwiki-source-c4b102e23088d386c714f747
    resource: repo://build-logic/src/main/kotlin/integration-test-suite.gradle.kts
  - id: openwiki-source-60afced7e651e11521b54e27
    resource: repo://build-logic/src/main/kotlin/test-conventions.gradle.kts
  - id: openwiki-source-6f974a41a29206678c20ab84
    resource: repo://content/skills/woodcutting/build.gradle.kts
  - id: openwiki-source-90ee786bcc1c9d8dcf3378a3
    resource: repo://content/skills/woodcutting/src/integration/kotlin/org/rsmod/content/skills/woodcutting/scripts/WoodcuttingScriptTest.kt
generated: { by: "claude-code", at: "2026-10-02T20:46:41.242Z" }
---

# Testing

## Conventions

`test-conventions` (applied by every `base-conventions` project) configures JUnit 5 for the
`test` source set:

- JUnit BOM + Jupiter, 2 GB test heap;
- parallel execution **enabled** with classes and methods running concurrently by default;
- extension autodetection disabled.

Tests that touch global singletons must opt out — e.g. `ForcedWalkTest` uses
`@Execution(SAME_THREAD)` and `@ResourceLock("ServerCacheManager")`.

`game-cache-test-conventions` is for test suites that load the real server cache
(`ServerCacheManager.init(240)` in a `@BeforeAll`): it runs tests from the repo root and declares
`.data/cache/SERVER` and `.data/gamevals-binary/gamevals.dat` as task inputs so the up-to-date
check reruns them when the cache changes. Such tests need a built cache (`gradlew install` or
`buildCache`, see [Cache build and gamevals](../cache/cache-build-and-gamevals.md)). It is
applied by e.g. `api/game-process` and `content/quest`.

Other suites: `meta-test-suite` (`api/meta`, `engine/map`) and `benchmark-suite`
(`engine/objtx`, `engine/routefinder`).

Run everything with `gradlew test`, or one module with e.g. `gradlew :api:bosses:test`.

## Where the active tests are

| Module | Covers |
|---|---|
| `api/bosses` | boss DSL runtime (attack delays, HP/hit conditions, transmog, timers, impact ticks, tile resolution, encounter registry) and `SpecValidator` |
| `api/drop-table`, `api/drop-table-plugin` | rolling engine and TOML/registry behaviour |
| `api/game-process` | movement / forced walk against collision with the real cache |
| `api/net` | protocol helpers |
| `api/registry`, `api/combat/combat-commons` | registries, combat helpers |
| `content/quest`, `content/areas/city/draynor`, `content/bosses/barrows`, `content/skills/magic/spell-attacks` | content logic |
| `engine/routefinder`, `or-cache`, `tools/*` | pathfinding, cache tooling, MCP and wiki dumper |

Pure-logic suites (bosses, drop tables) construct domain objects directly and do not need the
cache.

## Dormant integration suites

Many modules still contain `src/integration/kotlin` tests (54 files — e.g.
`api/invtx`, `api/player` ProtectedAccess movement tests, `api/shops`,
`content/skills/woodcutting`, `content/interfaces/bank`) written against RSMod's
`org.rsmod.api.testing.GameTestState` harness (`runGameTest(Script::class) { ... advance(ticks)
... assertMessageSent(...) }`). The `integration-test-suite` convention that compiles them
depends on `:api:testing`, but that module was removed in the "Convert RsMod cache to OpenRune"
refactor and **no build file applies the convention any more**, so these tests are not compiled or
run. Treat them as reference for intended behaviour, not as a safety net; reviving them needs a
replacement harness. `PluginClasspathScan` explicitly rejects `*.integration` packages so such
classes never load as plugins.

## CI

`.github/workflows/ci.yml` generates the cache and gameval sources, runs `gradlew assemble test`,
then boots the server and requires `Server ready in` within the timeout. The boot step matters
because many configuration errors (unmapped gamevals, duplicate handler registrations, invalid
boss specs, drop-table conflicts) only surface at startup. Formatting is checked separately with
Spotless. See [Configuration and operations](../operations/configuration.md).

## Verifying content in a live client

For gameplay changes, the preferred check is driving a real client: the
[OpenRune-Developer-Tools](https://github.com/OpenRune/OpenRune-Developer-Tools) RuneLite/RSProx
plugin exposes an MCP server at `http://127.0.0.1:7780/mcp` (dashboard at
`http://127.0.0.1:7780/`). Agents can interact with NPCs/objects/items, walk dialogue trees,
screenshot and dump interfaces, read var/clientscript/chat history and wait on game conditions.
Setup and example flows are in `AGENTS.md`. Typical loops:

```text
interact_npc {npcName: "bob"} → get_dialogue → select_option / continue_dialogue
interact_object {nameFilter: "tree", option: "Chop down"} → wait_for {condition: "chat_message", textContains: "logs"}
screenshot {interfaceId} + dump_interface {interfaceId}   (layout checks)
get_var_history {sinceMs: 3000}                            (which varbits changed)
type_chat {text: "::item 4151"}                            (server cheat commands)
```

Start the server with `gradlew run`, connect the client with `--developer-mode`, then verify.
