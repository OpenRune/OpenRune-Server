# Drop Tables

How NPC loot works in OpenRune, how to add or edit drops, and what the DSL syntax means.

**Simple tables** live in TOML under `content/drops/src/main/resources/drops/tables/` (mostly `monsters/`). They load at startup alongside Kotlin tables. Complex tables (manual conditions, bonus drops, herb variants, pre-roll separate rolls, etc.) stay as `@RegisterDropTable` Kotlin in `content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/`.

The wiki dumper writes simple tables to TOML by default and only emits Kotlin for tables that are not simple enough. Use `--no-toml` to skip TOML export.

---

## Quick mental model

When an NPC dies, the server rolls several **independent** loot stages:

1. **Guaranteed** — always rolled first; every entry that passes its checks is given.
2. **Pre-roll** — optional extra rolls before the main table (uncommon on monsters).
3. **Main table** — one weighted pick from the pool (plus optional **separate** rolls).
4. **Tertiary** — rare independent rolls (clues, pets, brimstone keys, etc.).

Think of it like OSRS: common loot comes from the main weight table; rare stuff is often a separate `1 outOf N` roll.

---

## Simple tables (TOML)

Most wiki-dumped monster tables are **simple** — flat weighted main entries, shared subtables (`gem`, `herb`, …), guaranteed/pre-roll/tertiary chance lines, inline separate rolls, brimstone key roll, and standard hooks (looting bag, quest gates, clue scroll boxes). Those are stored as TOML instead of Kotlin.

**Location:** `content/drops/src/main/resources/drops/tables/monsters/*.toml`

**Loading:** `DropTableRegistry` reads all `drops/tables/**/*.toml` from the classpath **before** scanning `@RegisterDropTable`. If the same NPC is registered in both TOML and Kotlin, startup fails with a conflict error.

**Example** (main + tertiary clue with scroll-box transform and combat-achievement rate baked into the denominator):

```toml
id = "Zoja Drops"
npcs = ["npc.canafis_woman11"]

[main]
total = 512
name = "Zoja Drops"

[[main.entries]]
weight = 10
obj = "obj.steel_axe"
count = 1

[[main.entries]]
weight = 2
shared = "gem"

[[main.entries]]
weight = 1
nothing = true

[[tertiary]]
numerator = 1
denominator = 121
obj = "obj.trail_clue_easy_simple001"
count = 1
clue_scroll_box = true
```

| TOML | Meaning |
|------|---------|
| `[main]` / `[[main.entries]]` | Weighted main table (`total` = pool size) |
| `[[main.separate_rolls]]` | Inline `N outOf M separate { ... }` rolls |
| `[[guaranteed]]` | Always-dropped items |
| `[[pre_roll]]` / `[[tertiary]]` | `numerator` / `denominator` chance rolls |
| `[[pre_roll_separate_rolls]]` | Pre-roll `N outOf M rolls rsPlayerWeightedTable { ... }` |
| `shared = "gem"` | `SharedDropTables.gem` (and other known shared names) |
| `nothing = true` | Empty main-table roll |
| `brimstone_key_roll = true` | Tertiary `{Brimstone rarity}` roll |
| `should_drop_looting_bag = true` | Wilderness looting bag check |
| `should_drop_brimstone_key = true` | Konar task brimstone key check |
| `quest` / `quest_mode` | Quest gate (`during`, `completed`, `not_completed`) |
| `clue_scroll_box = true` | Clue scroll → scroll box after X Marks the Spot (`clueScrollTransformObj`) |

| TOML omits empty defaults (`areas = []`, false booleans, etc.). String values use double quotes. Unresolved wiki drop rates and manual notes are appended as `#` comment lines at the bottom of the file.

**Reformat** existing TOML files to the canonical layout:

```powershell
./gradlew :tools:wiki-dumping:reformatDropTables
```

---

## A minimal table (Kotlin)

Use Kotlin when the table is not simple enough for TOML, or when you are prototyping by hand.

```kotlin
@field:RegisterDropTable
@JvmField
public val goblinDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Goblin Drops",
    npcs = npcs("npc.goblin"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Goblin Drops")
        10 weight "obj.bones" count 1
        5 weight "obj.coins" count 5..15
        3 weight "obj.goblin_mail" count 1
    },
)
```

- `@RegisterDropTable` — picks up the table at startup (via classpath scan).
- `npcs(...)` — which NPC types use this table.
- `total = 128` — main pool size; all **weights** in that table should add up to this number.
- `10 weight "obj.bones" count 1` — item `"obj.bones"`, quantity 1, weight 10 out of 128.

---

## Table sections

| Section | Builder | When it runs |
|---------|---------|--------------|
| Guaranteed | `rsPlayerGuaranteedTable { }` | Every kill, before anything else |
| Pre-roll | `rsPlayerPrerollTable { }` | Extra rolls before main (boss uniques, etc.) |
| Main | `rsPlayerWeightedTable { }` | Primary loot — one weighted outcome |
| Tertiary | `rsPlayerTertiaryTable { }` | Rare extra rolls (clues, pets, keys) |

You only include the sections you need. Most monsters use **main** + **tertiaries**.

---

## DSL syntax cheat sheet

Read each line **left to right** — rate/weight first, then item, then count, then optional modifiers.

### Main table (weighted)

```kotlin
7 weight "obj.chaosrune" count 60..120          // range, no modifier
8 weight "obj.adamant_javelin_head" count 40..50 condition { player -> ... }
1 weight "obj.trail_clue_hard_map001" count 1 transformObj { player -> null }
6 weight SharedDropTables.gem                     // nested shared table
29 weight nothing()                               // empty roll (pool filler)
```

### Guaranteed

```kotlin
guaranteed = rsPlayerGuaranteedTable {
    "obj.dragonhide_green" count 2
    "obj.konar_key" count 1 killCondition { player, npc, areaChecker ->
        player.shouldDropBrimstoneKey(npc, areaChecker)
    }
}
```

### Tertiary / pre-roll (rate-first)

Kotlin needs the `weight` keyword between the rate and the item string:

```kotlin
1 outOf 5000 weight "obj.dragon_slice" count 1
1 outOf 128 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
    // Drops Need Manual (item): Clue scrolls become scroll boxes after X Marks the Spot.
    null
}
```

Pre-roll uses the same item syntax but the builder is `rsPlayerPrerollTable` (internally uses `rolls` instead of `chance`).

### Separate rolls (main table)

A **separate** roll is its own `numerator outOf denominator` check, independent of the main weight pick.

**Single item with a condition** — put the rate *before* `separate`:

```kotlin
15 outOf 472 separate "obj.unidentified_kwuarm" count 1 condition { player ->
    // Drops Need Manual: Only dropped by ancient zygomites in the Stalker Den.
    true
}
```

**Multiple items sharing one separate rate** — nested weighted table:

```kotlin
12 outOf 472 separate rsPlayerWeightedTable {
    12 weight "obj.unidentified_dwarf_weed" count 1
    12 weight "obj.unidentified_cadantine" count 1
}
```

**Simple separate (no condition)** — obj-first still works:

```kotlin
"obj.unidentified_lantadyme" count 1 separate 9 outOf 472
```

Do **not** write `"obj" count 1 condition { ... } separate 15 outOf 472` — that does not compile. Use the rate-first form when you need `condition`, `transformObj`, or `killCondition`.

---

## Count formatting

| Situation | Example |
|-----------|---------|
| Fixed amount | `count 1` |
| Range | `count 5..15` or `count 60..120` |
| Range + modifier after it | `count (40..50) condition { ... }` |

Parentheses around a range are only needed when a modifier (`condition`, `transformObj`, `killCondition`) follows on the same chain.

---

## Conditions and modifiers

These attach **after** `count` on the item chain.

### `condition { player -> ... }`

Player-only check. Return `true` to allow the drop.

```kotlin
"obj.looting_bag" count 1 condition { player -> player.shouldDropLootingBag() }
```

Use for: wilderness-only drops, quest state, ring of wealth behaviour, wiki notes the parser could not turn into real code.

### `killCondition { player, npc, areaChecker -> ... }`

Kill-context check (needs NPC + area). Used for **brimstone keys** and similar.

```kotlin
"obj.konar_key" count 1 killCondition { player, npc, areaChecker ->
    player.shouldDropBrimstoneKey(npc, areaChecker)
}
```

The DSL wraps `dropRollable(...)` for you when `killCondition` is set — you do not write that by hand.

### `transformObj { player -> ... }`

Return the **obj key string** to drop, or `null` to use the default item. Common for clue scrolls that should become scroll boxes after a quest.

In **TOML**, set `clue_scroll_box = true` on the entry instead of writing `transformObj` by hand.

In **Kotlin**, use `player.clueScrollTransformObj("obj.trail_clue_easy_simple001")` (see `ClueScrollDropChecks.kt`).

```kotlin
1 outOf 121 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
    player.clueScrollTransformObj("obj.trail_clue_easy_simple001")
}
```

### Manual conditions (`// Drops Need Manual`)

Wiki codegen often leaves a placeholder when it cannot parse the wiki rule:

```kotlin
condition { player ->
    // Drops Need Manual: Only dropped in the Stalker Den.
    true
}
```

Replace `true` with real logic when you implement the check.

---

## Special helpers

| Helper | Purpose |
|--------|---------|
| `nothing()` | Empty main-table roll; respects ring of wealth by default |
| `ringNothing()` | Empty roll for separate-roll tables (always empty) |
| `onBuilder { brimstoneKeyRoll() }` | Tertiary brimstone key roll from combat level (wiki `{{Brimstone rarity}}`) |
| `onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }` | Same, with Konar task bonus |
| `SharedDropTables.gem` / `.herb` / `.seed` / etc. | Standard OSRS subtables in `tables/shared/` |

Brimstone keys on **guaranteed** tables use `killCondition` (see above). Tertiary `{Brimstone rarity}` lines use `brimstoneKeyRoll()` instead.

---

## Shared subtables

Reusable tables in `content/drops/.../tables/shared/` — herb table, gem table, rare drop table, etc.

Reference them by weight in a main table:

```kotlin
6 weight SharedDropTables.gem
22 weight SharedDropTables.herb
```

Herb rolls with multiple sizes (1× / 2× / 3× herb) are inlined by the wiki dumper as nested `rsWeightedTable` blocks.

---

## When to use `DropRollItem(...)` / `dropRollable(...)`

Prefer the readable item-chain syntax above. Fall back to the verbose form only when needed:

- **Bonus drops** — one roll gives multiple items (e.g. both fossil types).
- **Complex nested conditions** the chain cannot express.

Example (bonus drops — from Ancient Zygomite):

```kotlin
18 weight dropRollable(DropRollItem("obj.fossil_pyrophosphite", 1, condition = { player ->
    true
}, bonusDrops = listOf(
    DropRollItem("obj.fossil_calcite", 1),
)))
```

---

## Adding a new table manually

**Simple table:** add `content/drops/src/main/resources/drops/tables/monsters/my_npc.toml` using the format above. No `@RegisterDropTable` needed.

**Complex table:**

1. Create `content/drops/.../tables/monsters/MyNpcDropTable.kt`.
2. Define a public `val` with `@field:RegisterDropTable` and `@JvmField`.
3. Set `tableIdentifier`, `npcs(...)`, and at least `mainTable`.
4. Build `:content:drops` to verify it compiles.

TOML and Kotlin tables are discovered automatically — no manual registry entry. Do not register the same NPC in both.

---

## Generating tables from the OSRS Wiki

The wiki dumper parses drop tables and writes **TOML for simple tables** and **Kotlin for everything else**.

**Single monster:**

```powershell
./gradlew :tools:wiki-dumping:dumpNpcDrops --args="Black Knight --quiet"
```

**All monsters** (slow; overwrites generated files):

```powershell
./gradlew :tools:wiki-dumping:dumpNpcDrops --args="--all-monsters --quiet --root=d:\OpenRune\OpenRune-Server --wiki-dump=D:\OpenRune\OpenRune-FileStore-Server\dumps\wiki"
```

Use `--wiki-dump=...` for offline mode when you have a local wiki XML dump.

| Output | Path |
|--------|------|
| Simple tables (TOML) | `content/drops/src/main/resources/drops/tables/monsters/` |
| Complex tables (Kotlin) | `content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/` |

When a monster’s tables are all simple, the dumper removes the old Kotlin file if one existed. Pass `--no-toml` to generate Kotlin only (legacy behaviour).

After dumping:

1. Build `:content:drops`.
2. For remaining Kotlin files, search for `Drops Need Manual` — those need human logic.
3. Fix any unmapped items listed in file comments.

---

## Common pitfalls

| Problem | Fix |
|---------|-----|
| Main weights do not sum to `total` | Adjust weights or `nothing()` padding; check wiki pool size |
| `condition` after obj-first separate | Use `N outOf M separate "obj" count X condition { }` |
| `1 outOf 5000 "obj" count 1` will not parse | Add `weight`: `1 outOf 5000 weight "obj" count 1` |
| Clue should be a scroll box | TOML: `clue_scroll_box = true`. Kotlin: `clueScrollTransformObj(...)` |
| Brimstone key on guaranteed | Use `killCondition`, not plain `condition` |

---
