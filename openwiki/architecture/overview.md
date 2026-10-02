---
type: architecture
title: Architecture overview
description: The module layering of OpenRune-Server — engine, api, content, server, or-cache and tools — how Gradle auto-includes nested subprojects, how content reaches the runtime classpath, and which way dependencies point.
tags: [architecture, modules, gradle, layering]
verified:
  - by: openwiki/0.7.0
    at: 2026-10-02T20:46:41.242Z
sources:
  - id: openwiki-source-d6534c817648bfe29b3a0826
    resource: repo://api/plugin-commons/build.gradle.kts
  - id: openwiki-source-2de0954197850b0450d7bb39
    resource: repo://build-logic/src/main/kotlin/base-conventions.gradle.kts
  - id: openwiki-source-d7631db4089520712c8e95ae
    resource: repo://content/areas/city/lumbridge/build.gradle.kts
  - id: openwiki-source-52d666625d7077cff4e6fcff
    resource: repo://server/app/build.gradle.kts
  - id: openwiki-source-177b771d5858b2328f0a091d
    resource: repo://server/shared/build.gradle.kts
  - id: openwiki-source-e620d7484b72a53c7fa812cd
    resource: repo://settings.gradle.kts
generated: { by: "claude-code", at: "2026-10-02T20:46:41.242Z" }
---

# Architecture overview

OpenRune-Server is an OSRS-compatible game server written in Kotlin, a modular fork of
RSMod/Alter. The core idea: the engine and API layers are stable libraries; gameplay is shipped
as auto-discovered plugins under `content/` that need no registration.

## Layers

```text
            server/app  (entrypoint, Guice root module, GameService)
                 │
            server/shared  ── api(...) every api/* and content/* subproject
                 │
   content/*  ──►  api/plugin-commons  ──►  api/*  ──►  engine/*
   (plugins)       (facade of common APIs)  (domain)    (core runtime)
                                               │
                                           or-cache  (cache types, gamevals, ServerCacheManager)
```

| Layer | Path | Responsibility |
|---|---|---|
| Engine | `engine/` | Entities and world state (`engine/game`), map and collision (`engine/map`), routefinder, interaction primitives, event bus (`engine/events`), tick coroutines (`engine/coroutine`), Guice module base (`engine/module`), plugin base classes + classpath scan + external loader (`engine/plugin`), obj transactions |
| API | `api/` | ~60 DI-managed domain modules: game processes (tick loop), player/npc APIs, script DSL, combat, stats, inventories, shops, drops, instances, networking, DB/accounts, config |
| Content | `content/` | Gameplay plugins grouped by `areas`, `bosses`, `drops`, `events`, `generic`, `interfaces`, `other`, `quest`, `skills`, `travel` |
| Server | `server/` | `app` (main + Guice root), `install` (setup tasks), `services` (service lifecycle manager), `shared` (plugin loaders, event module), `logging` |
| Cache | `or-cache/` | Cache build tool (`dev.openrune.CacheToolsKt`), db-table definitions, gameval provider, runtime `ServerCacheManager` and map decoder |
| Tools | `tools/` | `osrs-mcp` (MCP server for OSRS data), `wiki-dumping`, `progress` (generates `PROGRESS.md`) |

Dependency direction is strictly downward: content depends on api/engine; api depends on
engine and or-cache; engine does not depend on api. `base-conventions` adds `:or-cache` as an
implementation dependency to every project except `:or-cache` itself, `:engine:map` and
`:engine:routefinder`.

Content modules usually depend on `api:plugin-commons`, which re-exports (`api(...)`) the
commonly needed api and engine modules so a plugin's build file stays one or two lines.

## How content reaches the server

`server/app` does not list content modules. Instead `server/shared` declares
`api(...)` dependencies on **every** `content/*` subproject with a build file and every
`api/*` subproject except `testing`. Because `server/app` depends on `server/shared`, every
plugin lands on the runtime classpath, where the shared classpath scan discovers it (see
[Writing content plugins](../plugins/plugin-scripts.md)). Adding a new content module therefore
needs no edit outside the module itself.

## Gradle project inclusion

`settings.gradle.kts` includes the top-level projects and then walks `api/`, `content/`,
`engine/` and `server/` recursively: every directory containing a `build.gradle.kts` becomes a
project, with its path built from the relative directory (`content/areas/city/lumbridge` →
`:content:areas:city:lumbridge`).

Directories named `pack` are renamed to `<parent>-pack` (e.g. `lumbridge-pack`). Every plugin's
cache-data module lives in a `pack` directory, and without the rename they would all share the
coordinates `org.rsmod:pack` and Gradle conflict resolution would drop all but one. The cache
builder relies on the `-pack` suffix to find them (see
[Cache build and gamevals](../cache/cache-build-and-gamevals.md)).

`settings.gradle.kts` can also `includeBuild` a local central-server checkout when the
`openrune.central.includeBuild` Gradle property is set.

## Build conventions

`build-logic/` provides precompiled convention plugins:

- `base-conventions` = formatter + kotlin + publish + test conventions (+ or-cache dependency);
- `kotlin-conventions`, `formatter-conventions` (Spotless/ktlint);
- `test-conventions`, `integration-test-suite`, `game-cache-test-conventions`,
  `meta-test-suite`, `benchmark-suite` (see [Testing](../testing/testing.md)).

`server/app` applies `application` with main class `org.rsmod.server.app.GameServerKt`, runs
from the repository root, and sets JVM args (`-Xms512m -Xmx4g`, G1 periodic GC to hand memory
back after the boot-time cache decode burst). `shadowJar` produces `server.jar`.

## Where to go next

- Runtime startup: [Server boot sequence](boot-sequence.md)
- The tick loop: [Game cycle and tick processing](game-cycle.md)
- Handler dispatch and blocking scripts: [Events, coroutines and ProtectedAccess](events-and-coroutines.md)
