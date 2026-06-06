# Drop Tables

How NPC loot works in OpenRune, how to add or edit drops, and what the DSL syntax means.

Most monster tables live in `content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/` and are auto-generated from the OSRS Wiki. You edit the generated Kotlin, or re-dump from the wiki when tables change.

---

## Quick mental model

When an NPC dies, the server rolls several **independent** loot stages:

1. **Guaranteed** — always rolled first; every entry that passes its checks is given.
2. **Pre-roll** — optional extra rolls before the main table (uncommon on monsters).
3. **Main table** — one weighted pick from the pool (plus optional **separate** rolls).
4. **Tertiary** — rare independent rolls (clues, pets, brimstone keys, etc.).

Think of it like OSRS: common loot comes from the main weight table; rare stuff is often a separate `1 outOf N` roll.

---

## A minimal table

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

```kotlin
1 outOf 128 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
    null  // TODO: return box obj key when quest done
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

1. Create `content/drops/.../tables/monsters/MyNpcDropTable.kt`.
2. Define a public `val` with `@field:RegisterDropTable` and `@JvmField`.
3. Set `tableIdentifier`, `npcs(...)`, and at least `mainTable`.
4. Build `:content:drops` to verify it compiles.

Tables are discovered automatically — no manual registry entry.

---

## Generating tables from the OSRS Wiki

The wiki dumper parses drop tables and writes Kotlin using the DSL above.

**Single monster:**

```powershell
./gradlew :tools:wiki-dumping:dumpNpcDrops --args="Black Knight --quiet"
```

**All monsters** (slow; overwrites generated files):

```powershell
./gradlew :tools:wiki-dumping:dumpNpcDrops --args="--all-monsters --quiet --root=d:\OpenRune\OpenRune-Server --wiki-dump=D:\OpenRune\OpenRune-FileStore-Server\dumps\wiki"
```

Use `--wiki-dump=...` for offline mode when you have a local wiki XML dump. Output goes to `content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/`.

After dumping:

1. Build `:content:drops`.
2. Search for `Drops Need Manual` in new/changed files — those need human logic.
3. Fix any unmapped items listed in file comments.

---

## Common pitfalls

| Problem | Fix |
|---------|-----|
| Main weights do not sum to `total` | Adjust weights or `nothing()` padding; check wiki pool size |
| `condition` after obj-first separate | Use `N outOf M separate "obj" count X condition { }` |
| `1 outOf 5000 "obj" count 1` will not parse | Add `weight`: `1 outOf 5000 weight "obj" count 1` |
| Clue should be a scroll box | Implement `transformObj { player -> "obj...." }` |
| Brimstone key on guaranteed | Use `killCondition`, not plain `condition` |

---
