---
type: subsystem
title: Drop tables and instances
description: How NPC and loc loot is defined (TOML tables and @RegisterDropTable Kotlin DSL), loaded by DropTableRegistry, selected per NPC and area, and rolled on kill; and how the instance system allocates private region copies for bosses with settings rows, InstanceScript, and an Active/Grace/Reclaim lifecycle.
tags: [drops, loot, instances, bosses, regions]
verified:
  - by: openwiki/0.7.0
    at: 2026-10-02T20:46:41.242Z
sources:
  - id: openwiki-source-5bfd4b12d4374fea59bb2c9a
    resource: repo://api/drop-table-plugin/src/main/kotlin/org/rsmod/api/droptable/DropTableRegistry.kt
  - id: openwiki-source-b676d51bf176364afd150045
    resource: repo://api/instances/src/main/kotlin/org/rsmod/api/instances/InstanceManager.kt
  - id: openwiki-source-c5430e85c697fb341a04db16
    resource: repo://api/instances/src/main/kotlin/org/rsmod/api/instances/InstanceScript.kt
  - id: openwiki-source-bfe8f068604fab5c243e359e
    resource: repo://content/drops/src/main/kotlin/org/rsmod/content/drops/NpcDropTableKillHook.kt
  - id: openwiki-source-50d75339d8ecb543b0729ba3
    resource: repo://docs/instances.md
  - id: openwiki-source-c58b857579856af9653f863b
    resource: repo://engine/plugin/src/main/kotlin/org/rsmod/plugin/scan/PluginClasspathScan.kt
generated: { by: "claude-code", at: "2026-10-02T20:46:41.242Z" }
---

# Drop tables and instances

Detailed authoring guides live in `docs/drops.md` and `docs/instances.md`; this page explains how
the pieces connect at runtime.

## Drop tables

### Where tables come from

| Form | Location | Use for |
|---|---|---|
| TOML | `content/drops/src/main/resources/drops/tables/**/*.toml` (mostly `monsters/`) | simple tables: weighted main, shared subtables (`gem`, `herb`...), guaranteed / pre-roll / tertiary chance lines, standard hooks |
| Kotlin `@RegisterDropTable` | `content/drops/src/main/kotlin/.../tables/monsters/` and any content module | tables needing conditions, bonus drops, transforms, separate pre-rolls |

A Kotlin table is a `@field:RegisterDropTable @JvmField val` of type
`RSDropTable<Player, DropRollItem>`:

```kotlin
@field:RegisterDropTable
@JvmField
val goblinDropTable = RSDropTable(
    tableIdentifier = "Goblin Drops",
    npcs = npcs("npc.goblin"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        10 weight "obj.bones" count 1
    },
)
```

Each kill rolls independent stages: **guaranteed** → **pre-roll** → **main** (one weighted
pick, plus optional separate rolls) → **tertiary** (clues, pets, keys). The generic rolling
engine is the `dtx` library in `api/drop-table`; RS-specific builders, rate boosts (ring of
wealth etc.), TOML parsing and registration live in `api/drop-table-plugin`.

Tables can be generated from the OSRS Wiki with `tools/wiki-dumping` (simple tables are written as
TOML; `:tools:wiki-dumping:reformatDropTables` canonicalises existing TOML).

### DropTableRegistry

`DropTableRegistry` is built in its constructor from the shared `PluginClasspathScan` (which
also accepts the `drops/tables` resource path):

1. Parse all `.toml` resources in parallel, then register them on the calling thread.
2. Find classes with a `@RegisterDropTable` field (without static-initialising every class) and
   register those fields.

Registering the same NPC from both TOML and Kotlin fails startup with a conflict message.

Lookup is keyed by the NPC type's internal name. When multiple tables target one NPC,
`forNpc(npc, areaChecker)` picks the single table whose `areas` contain the NPC's coords; if
none match it falls back to a table with no areas; if more than one area matches it throws.
`forLoc(loc)` serves chests and other loc-based loot (crystal chest, Larran's chest, Barrows).

### Rolling on kill

`NpcDropTableKillHook` (`content/drops`) is an `NpcDeathKillHook`. On kill it:

1. resolves the table via `forNpc`;
2. rolls it for the killer ("hero") with an `ArgMap` carrying the NPC and `AreaChecker`;
3. for each result: skips "nothing", checks `condition` and `killCondition`, applies
   `transformObj`, rolls the count, grants the collection log entry, lets any
   `NpcDeathDropHook` consume it (e.g. looting bag, auto-loot), otherwise spawns the obj at the
   drop coords visible to the killer for the loot-drop duration and adds it to the loot tracker;
4. recursively spawns `bonusDrops`.

## Instances

An instance is a private copy of one or more map regions allocated at runtime for a group of
players, with its own NPC spawns, damage contributions and cleanup.

### Region allocation

Instanced space sits at `x >= 6400`, split into vertical bands: large build slots (320×320,
`x` 6400–10239), small build slots (128×128, `x` 10240–14079) and world-entity slots (64×64,
`x` 14080–16383, reserved for boats, never reclaimed by the inactivity sweep). Allocation is in
`RegionRegistry` (`api/registry`); constants in `OsrsInstancing` (`api/instances`).

### Defining an instanced boss

1. Add an `InstanceSettingsRow` in `or-cache/.../tables/InstanceSettingsTable.kt` (key, enter/exit
   locs and coords, fee, max players, time limit, grace minutes, boss npc, etc.).
2. Give the row a `dbrow` id in a `gamevals.toml` and rebuild the cache (see
   [Cache build and gamevals](../cache/cache-build-and-gamevals.md)).
3. Subclass `InstanceScript(registry: BossInstanceRegistry)`, returning `settingsRow()` and an
   `area()` built with `InstanceArea.copyRegions(...)` or `InstanceArea.template(...)` plus NPC
   spawns. Op-1 on the enter loc opens the create/join menu and op-1 on the exit loc runs the
   leave flow automatically; `configure()` can override them.

### Lifecycle

`InstanceManager` owns sessions (`InstanceSession`), occupant tracking, NPC-to-instance index
and damage contributions:

```text
create ─► Active ─► boss dies ─► Grace (default 10 min) ─► destroyed
            │
            └─ everyone leaves ─► Reclaim (20 min) ─► destroyed, or Active again on rejoin
```

It also handles join/leave, logout, player death (send to exit coord) and boss kills (kill timer
for groups of ≤ 5, damage contributions used for loot). Timed instances warn at 50 %, 25 %,
12.5 %, 1 min and 30 s remaining.

Related: [NPC AI, hunting and bosses](npc-ai-and-bosses.md) for boss encounters,
[Combat system](combat.md) for damage.
