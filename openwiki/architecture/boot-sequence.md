---
type: architecture
title: Server boot sequence
description: How GameServer starts OpenRune — install checks, plugin module discovery, Guice injector, config/cache/map loading, parallel plugin script startup, the boot gate, and the ServiceManager lifecycle including shutdown.
tags: [boot, startup, guice, services, lifecycle]
verified:
  - by: openwiki/0.7.0
    at: 2026-10-02T20:46:41.242Z
sources:
  - id: openwiki-source-3f027fddc00acb58db24254c
    resource: repo://api/game-process/src/main/kotlin/org/rsmod/api/game/process/PluginScriptBootGate.kt
  - id: openwiki-source-2a9daaac1604f238ef4c63fb
    resource: repo://build.gradle.kts
  - id: openwiki-source-3593e003acca6f44acefa2a9
    resource: repo://server/app/src/main/kotlin/org/rsmod/server/app/GameBootstrap.kt
  - id: openwiki-source-43732b2b08462cf50ea3e845
    resource: repo://server/app/src/main/kotlin/org/rsmod/server/app/GameServer.kt
  - id: openwiki-source-d891ed723963236fa824942b
    resource: repo://server/app/src/main/kotlin/org/rsmod/server/app/GameService.kt
  - id: openwiki-source-a55d97b5e1caf8c5c28d8957
    resource: repo://server/install/src/main/kotlin/org/rsmod/server/install/GameServerInstall.kt
  - id: openwiki-source-467890f4d7f3c46af874d382
    resource: repo://server/services/src/main/kotlin/org/rsmod/server/services/ServiceManager.kt
  - id: openwiki-source-da5b31d5f2df0c7b9a9534e6
    resource: repo://server/shared/src/main/kotlin/org/rsmod/server/shared/loader/PluginModuleLoader.kt
generated: { by: "claude-code", at: "2026-10-02T20:46:41.242Z" }
---

# Server boot sequence

The entrypoint is `org.rsmod.server.app.GameServerKt` (`server/app`), launched by the root
`gradlew run` task, which simply depends on `:server:app:run`. `GameServer` is a Clikt command;
its `run()` checks the installation and then starts the application.

## 1. Installation check

`ensureProperInstallation()` requires three things before anything else loads:

- `.data/cache/LIVE` (vanilla cache) and `.data/cache/SERVER` (server cache) directories,
- a `game.yml` in the working directory.

If any is missing the server aborts with `Please run the install task first: gradlew install`.
If only the RSA key (`.data/game.key`) is missing, it runs the logback copy and RSA generator
in-process and continues.

The root `install` task depends on `:or-cache:freshCache` (download + build the cache), runs
`GameServerInstall` (logback config copy + RSA key generation) and finally copies
`game.example.yml` to `game.yml`. See [Configuration and operations](../operations/configuration.md)
and [Cache build and gamevals](../cache/cache-build-and-gamevals.md).

## 2. Boot phases

`startApplication` runs a sequence of timed phases and logs them as
`Server ready in <total> (<phase>=<duration>, ...)`:

| Phase | What happens |
|---|---|
| `plugin-modules` | `PluginModuleLoader.load(PluginModule)` instantiates every direct `PluginModule` subclass found by the shared classpath scan (no-arg constructors), plus `ExternalPluginLoader.loadModulesAtBoot()` |
| `guice` | `Guice.createInjector(GameServerModule + plugin modules)`; `GameServerModule` installs `GameModule`, `ParserModule`, `ServiceModule` |
| `config` | resolves `ServerConfig` from the injector (parsed from `game.yml`) |
| `cache` | `ServerCacheManager.init(serverConfig.revision)` loads the server cache |
| `map` | `GameMapDecoder.decodeAll` decodes the map; NPC and obj spawns are queued with `addDelayed(..., spawnDelay = 0)` rather than spawned immediately |
| `bootstrap-get` | constructs `GameBootstrap` (gets `Set<Service>`) |
| `scripts` ∥ `services-start` | run concurrently (see below) |

`GameModule` binds the core singletons the engine needs: `PlayerList`, `NpcList`,
`ControllerList`, region lists, `MapClock`, the three event maps, `CheatCommandMap`,
`EngineQueueCache`, and `GameProcess → MainGameProcess`.

## 3. Script startup in parallel with services

Inside `runBlocking`, plugin script loading is launched on `Dispatchers.Default` while
`GameBootstrap.startupUntilReady()` starts services on the calling thread.

`loadScripts`:

1. `PluginScriptLoader.load(PluginScript, injector)` + `ExternalPluginLoader.loadScriptsAtBoot`
   construct every concrete `PluginScript` through Guice (see
   [Writing content plugins](../plugins/plugin-scripts.md)).
2. Calls each script's `ScriptContext.startup()` and logs the ten slowest.
3. Flushes `EntityDelayedProcess` so the map spawns queued in the `map` phase appear *after* all
   `onNpcSpawn`-style handlers are registered, and before login opens.
4. Calls `PluginScriptBootGate.markReady()`.

The boot gate is a one-shot `CountDownLatch`. `GameService.setup()` calls
`scriptBootGate.awaitReady()` before `process.startup()`, so the game loop (and the
`GameLifecycle.Startup` event) cannot begin until every script has registered its handlers.
`awaitReady` fails with "Timed out waiting for plugin scripts..." after 5 minutes.

After both branches finish, `ExternalPluginLoader.releaseAllClassLoaders()` releases external
plugin jar locks, and the main thread blocks in `bootstrap.awaitShutdown`.

## 4. GameBootstrap and the ServiceManager

`GameBootstrap.startupUntilReady()`:

1. `EmbeddedSameInstancePostgres.ensureStarted(serverConfig)` — optional embedded PostgreSQL
   (see [Accounts and database persistence](../persistence/accounts-and-database.md)).
2. Starts the embedded central server if configured and the OpenRune central inbound watch
   (see [Networking, login and central server link](../networking/protocol-and-central.md)).
3. `serviceManager.awaitStartup()`; an `Error` result is rethrown.
4. Registers a JVM shutdown hook.

On any failure it stops the embedded central and Postgres before rethrowing.

`ServiceManager` (`server/services`) orchestrates `Service`, `ListenerService`,
`ScheduledService` and `ScheduledListenerService` implementations, all contributed as a Guice
`Set<Service>` multibinding. Startup order:

1. start all core services concurrently (`supervisorScope`);
2. signal startup to listener services;
3. create a dedicated executor + coroutine scope per scheduled service;
4. signal startup to scheduled listener services;
5. schedule each scheduled service's `run` loop.

A failure at any step shuts down the already-started services and returns
`StartResult.Error.Clean` or `Error.Lingering` (if cleanup itself failed). Errors thrown later
by a scheduled service also trigger a full shutdown.

`GameService` is the scheduled listener service that drives the game loop: a single-thread
executor on a daemon thread named `game`, ticking every 600 ms. See
[Game cycle and tick processing](game-cycle.md).

## 5. Shutdown

The shutdown hook in `GameBootstrap`:

- starts a `shutdown-watchdog` daemon that force-stops embedded Postgres and calls
  `Runtime.halt(1)` if shutdown has not completed after 20 s;
- calls `serviceManager.shutdown()` and waits with 5 s signal/cleanup/shutdown timeouts;
- stops the embedded central and Postgres (force-stopping Postgres on error).

`GameService.signalShutdown` marks the service as shutting down and calls
`MainGameProcess.preShutdown()`, which flags all players `pendingShutdown`, fast-forwards the
DB synchronizer and runs up to 1024 back-to-back cycles so players are logged out and saved.

## Integration test entry

`GameServer` has a secondary constructor argument `skipTypeVerificationOverride` and public
`createInjector()` / `prepareGame(injector)` methods. Integration tests construct the server
directly (Clikt args are not parsed) and `prepareGame(injector)` loads scripts synchronously
when no phase map is supplied. See [Testing](../testing/testing.md).
