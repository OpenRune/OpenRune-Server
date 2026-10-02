---
type: guide
title: Writing content plugins
description: How gameplay is added to OpenRune as PluginScript and PluginModule classes that are discovered by a shared classpath scan and constructed by Guice, the handler DSL in api/script and api/script-advanced, module layout with -pack siblings, and where to store state.
tags: [plugins, content, scripting, dsl, guice]
verified:
  - by: openwiki/0.7.0
    at: 2026-10-02T20:46:41.242Z
sources:
  - id: openwiki-source-1df780c4704db14458e6c1cc
    resource: repo://api/script-advanced/src/main/kotlin/org/rsmod/api/script/advanced/NpcScriptEventExtensions.kt
  - id: openwiki-source-8b26790fe9c60f29b088f5e0
    resource: repo://api/script/src/main/kotlin/org/rsmod/api/script/ScriptEventExtensions.kt
  - id: openwiki-source-d4dba4e9f51c4962b563d720
    resource: repo://content/areas/city/lumbridge/src/main/kotlin/org/rsmod/content/areas/city/lumbridge/npcs/Bob.kt
  - id: openwiki-source-9d81f4385801f44213539c01
    resource: repo://engine/plugin/src/main/kotlin/org/rsmod/plugin/module/PluginModule.kt
  - id: openwiki-source-c58b857579856af9653f863b
    resource: repo://engine/plugin/src/main/kotlin/org/rsmod/plugin/scan/PluginClasspathScan.kt
  - id: openwiki-source-1360fc814502cbc1fd7b5049
    resource: repo://engine/plugin/src/main/kotlin/org/rsmod/plugin/scripts/ScriptContext.kt
  - id: openwiki-source-da5b31d5f2df0c7b9a9534e6
    resource: repo://server/shared/src/main/kotlin/org/rsmod/server/shared/loader/PluginModuleLoader.kt
  - id: openwiki-source-3597a7bbbcdea40f139fb396
    resource: repo://server/shared/src/main/kotlin/org/rsmod/server/shared/loader/PluginScriptLoader.kt
generated: { by: "claude-code", at: "2026-10-02T20:46:41.242Z" }
---

# Writing content plugins

New gameplay is a `PluginScript` subclass in a `content/` module. There is no registration step:
the server finds it on the classpath, builds it with Guice and calls its `startup()`.

```kotlin
class Bob @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.bob") { startDialogue(it.npc) }
        onOpNpc3("npc.bob") { player.openShop(it.npc) }
        onUnimplementedOpNpc4("npc.bob") { repairOp(it.npc) }
    }
}
```

(`content/areas/city/lumbridge/.../npcs/Bob.kt`)

## Discovery and construction

`PluginClasspathScan` (`engine/plugin`) performs **one** ClassGraph scan over the
`org.rsmod.api` and `org.rsmod.content` packages (plus the `drops/tables` resource root),
rejecting `*.integration` test packages. Every discovery consumer — modules, scripts, drop tables,
packs — reuses it; it can be released after boot and is rebuilt lazily if needed again.

| Type | Loader | Rules |
|---|---|---|
| `PluginModule` (Guice `AbstractModule`) | `PluginModuleLoader` | **direct** subclasses only, instantiated with a public no-arg constructor, before the injector is created |
| `PluginScript` | `PluginScriptLoader` | every non-abstract, non-interface subclass, created with `injector.getInstance(clazz)` so `@Inject` constructors work; a construction failure aborts boot |

Consequences:

- your class must live under `org.rsmod.content` (or `org.rsmod.api`) to be found — the
  convention is `org.rsmod.content.<category>.<subcategory>`;
- the module must be under `content/` with a `build.gradle.kts`; `server/shared` puts every such
  module on the runtime classpath (see [Architecture overview](../architecture/overview.md));
- scripts start in a background task during boot; the world does not tick and logins are refused
  until all have finished (see [Server boot sequence](../architecture/boot-sequence.md)).

### PluginModule helpers

`PluginModule.bind()` offers `bindInstance<T>()` (singleton), `bindSingleton(instance)`,
`bindProvider`, `bindBaseInstance<Base>(Impl)`, and multibinding via `newSetBinding<T>()` /
`addSetBinding<T>(Impl)`. Set bindings are the main extension seam — e.g. special attacks
(`SpecialAttackMap`), character data pipelines, NPC death/drop hooks, attack validation hooks,
post-tick hooks, and `Service`s.

## The handler DSL

`ScriptContext` gives scripts the `EventBus`, `CheatCommandMap` and `EngineQueueCache`. The
`api/script` and `api/script-advanced` modules add extension functions over it. Identifiers are
gameval symbols (`"npc.bob"`, `"loc.winch"`, `"obj.coins"`, `"interface.x"`,
`"component.x:y"`), never raw ids.

| Area | Examples |
|---|---|
| NPC ops | `onOpNpc1..5`, `onApNpc1..5`, `onOpNpcT` (spell on npc), `onOpNpcU` (item on npc), `onOpContentNpc*` (by content group), `onDefaultOpNpc*`, `onUnimplementedOpNpc*` |
| Locs | `onOpLoc1..5`, `onApLoc*`, `onOpLocU`, `onOpLocT`, `onOpLocCategory*`, `onOpContentLoc*` |
| Items | `onOpHeld1..5`, `onOpHeldU` (item on item), `onOpHeldSubOp`, `onOpWorn1..9`, `onOpObj1..5` (ground items), `onDropHeld`, `onDestroyHeld` |
| Players | `onOpPlayer1..5`, `onOpPlayerT`/`U`, `onPlayerLogin`/`Logout`/`Init`, `onPlayerTimer`, `onPlayerQueue`/`SoftQueue`, `onPlayerHit` |
| NPC AI | `onAiOpPlayer*`, `onAiApPlayer*`, `onAiTimer`, `onNpcQueue`, `onNpcTimer`, `onNpcHit`, `onModifyNpcHit` |
| Controllers | `onConTimer`, `onConQueue`, `onAiConTimer` |
| World/areas | `onArea`/`onAreaExit`, `onZone`/`onZoneExit`, `onMapzone`, `onPlayerWalkTrigger`, `onWorldMapClick`, `onGameStartup` |
| Interfaces | `onIfOpen`, `onIfClose`, `onIfModalButton`, `onIfOverlayButton`, `onIfModalDrag`, `onIfScriptTrigger` (see [Interfaces and clientscripts](../interfaces/interfaces-and-clientscripts.md)) |
| Stats | `onAdvanceStat`, `onChangeStat` |
| Commands | `onCommand("name") { requiredRights = ...; desc = ...; cheat { ... } }` |
| Raw | `onEvent<T>`, `onProtectedEvent<T>(id)` |

Only one handler may exist per keyed event (e.g. one `onOpNpc1("npc.bob")` across the whole
server); a duplicate fails at startup.

`onDefault*` handlers run when no specific handler exists (used by combat for "Attack");
`onUnimplemented*` marks ops whose content isn't written yet.

### Blocking flows

Player handlers receive a `ProtectedAccess` receiver and are `suspend` lambdas, so dialogue,
animation waits and multi-tick actions are written sequentially:

```kotlin
private suspend fun ProtectedAccess.startDialogue(npc: Npc) =
    startDialogue(npc) {
        when (choice3("Give me a quest!", 1, "Have you anything to sell?", 2, "Repair?", 3)) {
            1 -> { chatPlayer(happy, "Give me a quest!"); chatNpc(angry, "Get yer own!") }
            2 -> player.openShop(npc)
            3 -> repairItems()
        }
    }
```

Movement or another action cancels the script automatically. See
[Events, coroutines and ProtectedAccess](../architecture/events-and-coroutines.md).

## Module layout

```text
content/<category>/<feature>/
├── build.gradle.kts          plugins { id("base-conventions") } + deps (usually api.pluginCommons)
├── src/main/kotlin/org/rsmod/content/<category>/<feature>/...   scripts and modules
├── src/main/resources/gamevals.toml                             optional custom symbols
└── pack/                      optional cache data (becomes :...:<feature>-pack)
    ├── src/main/kotlin/.../<Feature>PluginPack.kt
    └── src/main/resources/pack/{configs,models,sprites,cs2,interfaces}/
```

Pack modules are compiled only into the cache builder, never into the server. After touching
pack resources or `gamevals.toml`, run `gradlew buildCache` (see
[Cache build and gamevals](../cache/cache-build-and-gamevals.md)).

## Where to put state

1. varps/varbits (permanent for persistent, temporary for session state) via delegates such as
   `boolVarBit("varbit.x")`;
2. inventories for item containers;
3. `AttributeKey`/attrs only for data a var cannot represent.

See [Inventories, vars and player state](../gameplay/inventories-and-player-state.md).

## Other extension points

- Drop tables: TOML or `@RegisterDropTable` ([Drop tables and instances](../gameplay/drops-and-instances.md))
- Bosses: boss DSL ([NPC AI, hunting and bosses](../gameplay/npc-ai-and-bosses.md))
- Weapons/specials/spells: registries ([Combat system](../gameplay/combat.md))
- Plugins shipped outside the build: [External and hot-loaded plugins](external-plugins.md)
