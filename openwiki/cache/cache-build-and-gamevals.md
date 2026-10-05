---
type: subsystem
title: Cache build and gamevals
description: How the or-cache tool downloads and builds the LIVE (client) and SERVER caches, how content -pack modules contribute configs, models, sprites, CS2, interfaces and db tables, how gameval symbols are stored and merged, and how ServerCacheManager loads types at runtime.
tags: [cache, gamevals, or-cache, packs, cs2, dbtables]
verified:
  - by: openwiki/0.7.0
    at: 2026-10-02T20:46:41.242Z
sources:
  - id: openwiki-source-6e94fdf1eba1dc674d6b78cb
    resource: repo://or-cache/build.gradle.kts
  - id: openwiki-source-e047734423639c9255eb5e5b
    resource: repo://or-cache/src/main/kotlin/dev/openrune/CacheTools.kt
  - id: openwiki-source-68997e8c32aca3cd235ef824
    resource: repo://or-cache/src/main/kotlin/dev/openrune/gamevals/PluginGamevalMerger.kt
  - id: openwiki-source-5205f908ec77220c7bf99edd
    resource: repo://or-cache/src/main/kotlin/dev/openrune/pack/PluginPack.kt
  - id: openwiki-source-4e493fd0520e2277be56aea3
    resource: repo://or-cache/src/main/kotlin/dev/openrune/pack/PluginPacks.kt
  - id: openwiki-source-9da3ed90d229316e6512fa52
    resource: repo://or-cache/src/main/kotlin/dev/openrune/ServerCacheManager.kt
generated: { by: "claude-code", at: "2026-10-02T20:46:41.242Z" }
---

# Cache build and gamevals

The server runs against a game cache produced locally by the `or-cache` module. Content that
adds new cache data (items, NPC configs, interfaces, clientscripts, db tables) ships it in a
plugin `-pack` module, and the cache build merges every pack in.

## Directories

All paths are relative to the repository root, under `.data/` (`DirectoryConstants.DATA_PATH`):

| Path | Contents |
|---|---|
| `.data/cache/LIVE` | client-facing cache (served to the client over JS5) |
| `.data/cache/SERVER` | minified server cache read by the game server |
| `.data/raw-cache/` | base configs, models, sprites and `server` configs packed on every build |
| `.data/gamevals/*.rscm` | text `name=id` symbol tables per type (`obj`, `varbit`, `dbrow`, ...) |
| `.data/gamevals-binary/` | binary symbol dumps (`gamevals.dat`, component names, `max-ids.toml`) |
| user app-data CS2 dir | unpacked/compiled clientscripts (`DirectoryConstants.CS2_PATH`); wiped by `cleanCs2` |

`GameServer` refuses to start unless both cache directories exist (see
[Server boot sequence](../architecture/boot-sequence.md)).

## Gradle tasks

Defined in `or-cache/build.gradle.kts`, all running `dev.openrune.CacheToolsKt` unless noted:

| Task | Arg | Effect |
|---|---|---|
| `freshCache` | `FRESH_INSTALL` | download the revision from OpenRS2 into LIVE/SERVER, reset incremental state, dump gamevals, then run a forced `BUILD`. The root `install` task depends on it |
| `buildCache` | `BUILD` | incremental rebuild with all packs; then builds the SERVER cache and regenerates code |
| `cleanCs2` | `CLEAN_CS2` | delete the generated CS2 directory |
| `mergePluginGamevals` | — | runs `PluginGamevalMergerKt` from the repo root (used by release CI) |

The revision comes from `readRevision()` (the `revision` in config); the README badge and
`game.yml` track it (currently 241).

## Build pipeline (`buildCache`)

`CacheTools.buildCache`:

1. `GameValProvider.load("../")` — load symbol tables so packs can refer to names.
2. `PluginPacks.discover(projectRoot)` — classpath-scan every direct `PluginPack` subclass
   (only `-pack` modules are on the or-cache classpath, so building the cache never compiles
   plugin game scripts) and `validate()` the active ones.
3. Compute CS2 overrides from each pack's `cs2/` directory (copied under `custom/`).
4. `buildPackTasks`, in order: base models/configs from `.data/raw-cache`, each pack's
   `configs/`, `models/`, `sprites/`, extra tasks, interface packing (`PackIfType` from DSL
   interfaces + `interfaces/` dirs), unpack default CS2, pack CS2 with overrides, all db tables
   (`tablesToPack()` + each pack's `dbTables()`), world map.
5. Run the cache tool for the LIVE cache — incremental, with an incremental-state database and
   fingerprint verification.
6. For `BUILD`, build the SERVER cache from the server-relevant subset (drops CS2, interface,
   model, sprite and world-map tasks; adds `PackServerConfig` from `.data/raw-cache/server` plus
   pack config dirs, and `MapPackers`), with stripped indices and output-CRC verification.
7. `finalizeServerCache` — dump db columns and component gamevals, decode db tables/rows/enums
   and run code generation (`TableGenerater`, `EnumGenerater`) for typed accessors.

### PluginPack conventions

A pack module contains a no-arg `class XPluginPack : PluginPack()`. Resource directories are
discovered by convention under `src/main/resources/pack/` of the module (or its parent):

- `configs/` → `PackConfig` (also packed into the SERVER cache)
- `models/`, `sprites/` → client only
- `cs2/` → clientscript overrides
- `interfaces/` → interface definitions

Overrides: `dbTables()` for Kotlin-defined db tables, `interfaces()` for `buildInterface` DSL
interfaces, `extraTasks()`, and `isEnabled` / `shouldAlwaysPack` to gate packing.
See [Interfaces and clientscripts](../interfaces/interfaces-and-clientscripts.md).

## Gamevals

Gamevals map symbolic names to cache ids. Content always uses the symbol form
`"<table>.<name>"` (e.g. `"npc.bob"`, `"obj.coins"`), resolved with
`String.asRSCM(RSCMType.X)` from the `dev.openrune.rscm` library; never raw numeric ids.

### Custom symbols

A content module can add new ids in `src/main/resources/gamevals.toml`:

```toml
[gamevals.obj]
poh_tablet_shootingstar = 63477
```

`PluginGamevalMerger.merge(root)`:

- walks `content/` for `gamevals.toml` files (ignoring `build/`, `out/`, `target/`);
- requires each `[gamevals.<table>]` name to be a known `RSCMType` prefix, otherwise fails;
- appends `key=value` to `.data/gamevals/<table>.rscm` only for keys not already present —
  it never rewrites or removes an existing entry, so changing an id requires editing the
  `.rscm` file too.

Custom ids are conventionally placed high (~63000–65535) to avoid clashing with cache ids.

## Runtime: ServerCacheManager

At boot `ServerCacheManager.init(revision)`:

1. `GameValProvider.load()` (symbols from `.data/gamevals` and `.data/gamevals-binary`);
2. loads `.data/cache/SERVER`;
3. registers the LIVE cache as the JS5 group provider;
4. decodes every type table (npcs, objects/locs, items, invs, sequences, varbits, varps,
   structs, db rows/tables, components, stats, projectiles, hitsplats, walk triggers, var*
   types, params, hunt modes...) into read-only maps;
5. computes the transmitted varp list and db master-row indexes.

Lookups are static: `getNpc(id)`, `getItem(id)`, `getVarbit(id)`, ... and `get*OrDefault`
variants that log and return an empty type for unknown ids.

## Typical workflow

- Changed Kotlin only → `gradlew run`.
- Changed a `-pack` resource, a db table, an interface DSL or a `gamevals.toml` →
  `gradlew buildCache`, then run.
- Broken/partial cache → `gradlew cleanInstall` (cleans and re-runs `install`).
