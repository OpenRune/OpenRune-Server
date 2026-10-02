---
type: overview
title: Quickstart
description: Entry point to the OpenRune-Server wiki — what the project is, how to install, build, run and test it, and which page to read for each common task.
tags: [quickstart, overview, onboarding, routing]
verified:
  - by: openwiki/0.7.0
    at: 2026-10-02T20:46:41.242Z
sources:
  - id: openwiki-source-2a9daaac1604f238ef4c63fb
    resource: repo://build.gradle.kts
  - id: openwiki-source-a7605a66ae809a51390b2a0c
    resource: repo://content/other/spawn/pack/src/main/resources/pack/interfaces/spawn_menu.if3
  - id: openwiki-source-ce0d1c40f560f7b1086cf600
    resource: repo://game.example.yml
  - id: openwiki-source-43732b2b08462cf50ea3e845
    resource: repo://server/app/src/main/kotlin/org/rsmod/server/app/GameServer.kt
  - id: openwiki-source-e620d7484b72a53c7fa812cd
    resource: repo://settings.gradle.kts
generated: { by: "claude-code", at: "2026-10-02T20:46:41.242Z" }
---

# OpenRune-Server quickstart

OpenRune-Server is an OSRS-compatible game server (revision 241) written in Kotlin, a modular
fork of RSMod/Alter. The engine and API layers are libraries; gameplay is shipped as
auto-discovered plugins in `content/`.

## Build and run

| Step | Command |
|---|---|
| First-time setup (downloads/builds the cache, RSA keys, logback, `game.yml`) | `gradlew install` |
| Run the server | `gradlew run` (→ `:server:app:run`, main `org.rsmod.server.app.GameServerKt`) |
| After changing `-pack` resources or `gamevals.toml` | `gradlew buildCache` |
| Tests | `gradlew test` or `gradlew :<module>:test` |
| Format | `gradlew spotlessApply` |

Java 21 toolchain. A successful boot logs `Server ready in <time> (<phase breakdown>)`. If boot
says to run the install task, the `.data/cache` directories or `game.yml` are missing. Password
login requires OpenRune Central; the example config runs it embedded (`central.same-instance:
true`).

## Where to read for a task

| I want to... | Read |
|---|---|
| understand the module layout and dependency direction | [Architecture overview](architecture/overview.md) |
| know what happens at startup / why boot fails | [Server boot sequence](architecture/boot-sequence.md) |
| know when my code runs within a tick | [Game cycle and tick processing](architecture/game-cycle.md) |
| understand handlers, suspension, dialogue cancellation | [Events, coroutines and ProtectedAccess](architecture/events-and-coroutines.md) |
| add an NPC, loc, item or command handler | [Writing content plugins](plugins/plugin-scripts.md) |
| ship or hot-reload a plugin jar | [External and hot-loaded plugins](plugins/external-plugins.md) |
| add items/configs/db tables or new gameval symbols | [Cache build and gamevals](cache/cache-build-and-gamevals.md) |
| build or wire a custom interface / clientscript | [Interfaces and clientscripts](interfaces/interfaces-and-clientscripts.md) |
| change inventories, store player state, make a shop | [Inventories, vars and player state](gameplay/inventories-and-player-state.md) |
| touch combat, specials, weapons or spells | [Combat system](gameplay/combat.md) |
| write NPC behaviour or a boss | [NPC AI, hunting and bosses](gameplay/npc-ai-and-bosses.md) |
| add loot tables or an instanced boss | [Drop tables and instances](gameplay/drops-and-instances.md) |
| work on login, packets or the central server | [Networking, login and central server link](networking/protocol-and-central.md) |
| change what is saved or query the DB | [Accounts and database persistence](persistence/accounts-and-database.md) |
| configure `game.yml`, CI or releases | [Configuration and operations](operations/configuration.md) |
| test a change (unit, CI, live client) | [Testing](testing/testing.md) |

## Ground rules worth knowing up front

- Reference everything by gameval symbol (`"npc.bob"`, `"obj.coins"`), never raw ids.
- No registration: a `PluginScript` under `org.rsmod.content` in a `content/` module is found and
  started automatically; one handler per keyed event across the server.
- Store state in varps/varbits; attributes are a last resort.
- All gameplay runs on the single `game` thread every 600 ms; DB and network work happen on other
  threads and hand results back to it.
- `PROGRESS.md` and the README progress block are generated — do not edit them by hand.
- `docs/` holds detailed guides: `drops.md`, `boss-dsl.md`, `boss-hp-bar.md`, `instances.md`,
  `external-plugins.md`, `doors.md`, `gates.md`, `ironman.md`, `quirks.md`, `RELEASE_CI.md`.

## Known discrepancies with AGENTS.md

- AGENTS.md names `game.yml` revision 240.2 and a success line "OpenRune Server Successfully
  initialized"; the code and example config use revision 241 and log `Server ready in ...`.
- AGENTS.md describes a `buildInterface()` Kotlin DSL with `SpawnInterface.kt` /
  `ToolbeltInterface.kt` examples; neither file exists. The spawn menu is defined in
  `content/other/spawn/pack/src/main/resources/pack/interfaces/spawn_menu.if3`.
