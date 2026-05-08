# Herblore System

This document explains how the herblore skill is structured and how to add new herbs, unfinished potions, and finished potions to the game without touching any script code.

---

## Overview

The herblore system is **data-driven**. All item definitions and experience values live in two database tables. The scripts (`CleanHerbEvents`, `MakeUnfinishedEvents`, `MakePotionEvents`) iterate those tables at startup and register handlers automatically — you never need to edit a script to add a new herb or potion.

There are three layers:

1. **Database tables** — define which items and XP values belong to each recipe
2. **`gamevals.toml`** — assigns numeric IDs to every new `dbtable`, `dbrow`, and `queue`
3. **Scripts** — read the tables at startup; no changes needed when adding content

---

## Layer 1 — Database tables (`Herblore.kt`)

Located at `or-cache/src/main/kotlin/dev/openrune/tables/skills/Herblore.kt`.

There are two tables:

### `dbtable.herblore_herbs`

Covers everything related to a raw herb: cleaning it and converting it into an unfinished potion.

| Column | Type | Description |
|---|---|---|
| `grimy` | OBJ | The grimy (unidentified) herb |
| `clean` | OBJ | The cleaned herb |
| `unfinished` | OBJ | The unfinished potion made from the clean herb + vial of water |
| `level` | INT | Herblore level required to clean the herb |
| `xp` | INT | XP awarded for cleaning, stored as `xp × 10` (e.g. 2.5 XP → `25`) |

Example row:

```kotlin
row("dbrow.herblore_herb_guam") {
    columnRSCM(COL_GRIMY,      "obj.unidentified_guam")
    columnRSCM(COL_CLEAN,      "obj.guam_leaf")
    columnRSCM(COL_UNFINISHED, "obj.guamvial")
    column(COL_LEVEL, 3)
    column(COL_XP, 25)   // 2.5 XP
}
```

### `dbtable.herblore_potions`

Covers making a finished potion from an unfinished potion and a secondary ingredient.

| Column | Type | Description |
|---|---|---|
| `result` | OBJ | The finished potion (always a 3-dose variant) |
| `unfinished` | OBJ | The unfinished potion used as the first ingredient |
| `secondary` | OBJ | The secondary ingredient |
| `level` | INT | Herblore level required to mix the potion |
| `xp` | INT | XP awarded, stored as `xp × 10` (e.g. 25.0 XP → `250`) |

Example row:

```kotlin
row("dbrow.herblore_potion_attack") {
    columnRSCM(COL_RESULT,         "obj.3dose1attack")
    columnRSCM(COL_POT_UNFINISHED, "obj.guamvial")
    columnRSCM(COL_SECONDARY,      "obj.eye_of_newt")
    column(COL_POT_LEVEL, 3)
    column(COL_POT_XP, 250)   // 25.0 XP
}
```

> **XP is stored multiplied by 10.** The scripts divide by `10.0` when awarding XP, so fractional values (e.g. 2.5, 8.8) are supported without floating-point columns.

---

## Layer 2 — IDs (`gamevals.toml`)

Located at `content/skills/herblore/src/main/resources/gamevals.toml`.

Every `dbtable`, `dbrow`, and `queue` name used in `Herblore.kt` must have a unique numeric ID here. IDs must not overlap with any other `gamevals.toml` in the project.

```toml
[gamevals.dbtable]
herblore_herbs   = 55560
herblore_potions = 55561

[gamevals.dbrow]
herblore_herb_guam        = 55560
herblore_herb_marrentill  = 55561
# ... one entry per row in Herblore.kt ...

herblore_potion_attack    = 55574
herblore_potion_strength  = 55576
# ... one entry per potion row ...

[gamevals.queue]
herblore_clean      = 42
herblore_make       = 43
herblore_unfinished = 44
```

The queue IDs (`herblore_clean`, `herblore_make`, `herblore_unfinished`) are internal to the herblore module and do not need to match anything in `.data/gamevals/queue.rscm`.

---

## How to find RSCM names

All OSRS item names are stored in `.data/gamevals-binary/gamevals.dat` as null-terminated `name=id` entries. You can scan for a name with a small Python script:

```python
with open('.data/gamevals-binary/gamevals.dat', 'rb') as f:
    data = f.read()

pos = 0
while pos < len(data) - 2:
    b = data[pos]
    if 1 <= b <= 80:
        entry_end = data.find(b'\x00', pos + 1)
        if entry_end != -1 and entry_end - pos - 1 == b:
            entry = data[pos + 1:entry_end].decode('ascii', errors='ignore')
            if 'YOUR_SEARCH_TERM' in entry:
                print(entry)
            pos = entry_end + 1
            continue
    pos += 1
```

Replace `YOUR_SEARCH_TERM` with a partial name (e.g. `snapdragon`, `vial`, `eye_of`).

Common naming quirks to watch out for:

| Item | RSCM name |
|---|---|
| Grimy marrentill | `obj.unidentified_marentill` *(single 'r')* |
| Clean marrentill | `obj.marentill` *(single 'r')* |
| Marrentill unfinished | `obj.marrentillvial` *(double 'r')* |
| Finished 3-dose potions | `obj.3dose1attack`, `obj.3dose1strength`, etc. |
| Vial of water | `obj.vial_water` |

---

## Adding a new herb

### Step 1 — Add a row to `dbtable.herblore_herbs`

In `or-cache/src/main/kotlin/dev/openrune/tables/skills/Herblore.kt`, inside `fun herbs()`:

```kotlin
row("dbrow.herblore_herb_myherb") {
    columnRSCM(COL_GRIMY,      "obj.unidentified_myherb")
    columnRSCM(COL_CLEAN,      "obj.myherb")
    columnRSCM(COL_UNFINISHED, "obj.myherbvial")
    column(COL_LEVEL, 55)       // required Herblore level to clean
    column(COL_XP, 1100)        // 110.0 XP × 10
}
```

### Step 2 — Register the dbrow ID in `gamevals.toml`

Pick the next free ID after the last herb row:

```toml
[gamevals.dbrow]
# existing rows...
herblore_herb_myherb = 55591   # next available ID
```

### Step 3 — Rebuild the cache

Run the or-cache build task in Gradle to regenerate `HerbloreHerbsRow.kt`:

```bash
./gradlew :or-cache:build
```

That's all. `CleanHerbEvents` and `MakeUnfinishedEvents` pick up the new row automatically at next server start.

---

## Adding a new potion

### Step 1 — Add a row to `dbtable.herblore_potions`

In `Herblore.kt`, inside `fun potions()`:

```kotlin
row("dbrow.herblore_potion_mypotion") {
    columnRSCM(COL_RESULT,         "obj.3dose1mypotion")   // 3-dose result
    columnRSCM(COL_POT_UNFINISHED, "obj.myherbvial")       // unfinished potion
    columnRSCM(COL_SECONDARY,      "obj.my_secondary")     // secondary ingredient
    column(COL_POT_LEVEL, 55)      // required Herblore level
    column(COL_POT_XP, 1250)       // 125.0 XP × 10
}
```

The `COL_RESULT` item should be the 3-dose variant. The 4-dose, 2-dose, and 1-dose variants are handled separately by the potion-decanting system and do not need entries here.

### Step 2 — Register the dbrow ID in `gamevals.toml`

```toml
[gamevals.dbrow]
# existing rows...
herblore_potion_mypotion = 55592   # next available ID
```

### Step 3 — Rebuild the cache

```bash
./gradlew :or-cache:build
```

`MakePotionEvents` picks up the new row automatically at next server start.

---

## Complete example — adding Grimy Harralander and Restore Potion

This example adds a herb that is already in the game. The steps are the same for any new herb.

**`Herblore.kt` — herbs table:**

```kotlin
row("dbrow.herblore_herb_harralander") {
    columnRSCM(COL_GRIMY,      "obj.unidentified_harralander")
    columnRSCM(COL_CLEAN,      "obj.harralander")
    columnRSCM(COL_UNFINISHED, "obj.harralandervial")
    column(COL_LEVEL, 20)
    column(COL_XP, 630)   // 63.0 XP × 10
}
```

**`Herblore.kt` — potions table:**

```kotlin
row("dbrow.herblore_potion_restore") {
    columnRSCM(COL_RESULT,         "obj.3dosestatrestore")
    columnRSCM(COL_POT_UNFINISHED, "obj.harralandervial")
    columnRSCM(COL_SECONDARY,      "obj.red_spiders_eggs")
    column(COL_POT_LEVEL, 22)
    column(COL_POT_XP, 6250)   // 625.0 XP × 10 — wait, check OSRS wiki
}
```

**`gamevals.toml`:**

```toml
herblore_herb_harralander    = 55563
herblore_potion_restore      = 55577
```

After rebuilding the cache, the server will:
- Allow cleaning Grimy Harralander at level 20 for 63.0 XP
- Allow mixing Harralander (unf) + Red spiders' eggs into Restore potion at level 22

---

## Reference — current herbs and potions

### Herbs

| dbrow name | Grimy | Clean | Level | XP |
|---|---|---|---|---|
| `herblore_herb_guam` | Unidentified guam | Guam leaf | 3 | 2.5 |
| `herblore_herb_marrentill` | Unidentified marentill | Marentill | 5 | 3.8 |
| `herblore_herb_tarromin` | Unidentified tarromin | Tarromin | 11 | 5.0 |
| `herblore_herb_harralander` | Unidentified harralander | Harralander | 20 | 6.3 |
| `herblore_herb_ranarr` | Unidentified ranarr | Ranarr weed | 25 | 7.5 |
| `herblore_herb_toadflax` | Unidentified toadflax | Toadflax | 30 | 8.0 |
| `herblore_herb_irit` | Unidentified irit | Irit leaf | 40 | 8.8 |
| `herblore_herb_avantoe` | Unidentified avantoe | Avantoe | 48 | 10.0 |
| `herblore_herb_kwuarm` | Unidentified kwuarm | Kwuarm | 54 | 11.3 |
| `herblore_herb_snapdragon` | Unidentified snapdragon | Snapdragon | 59 | 11.8 |
| `herblore_herb_cadantine` | Unidentified cadantine | Cadantine | 65 | 12.5 |
| `herblore_herb_lantadyme` | Unidentified lantadyme | Lantadyme | 67 | 13.1 |
| `herblore_herb_dwarf_weed` | Unidentified dwarf weed | Dwarf weed | 70 | 13.8 |
| `herblore_herb_torstol` | Unidentified torstol | Torstol | 75 | 15.0 |

### Potions

| dbrow name | Result | Secondary | Level | XP |
|---|---|---|---|---|
| `herblore_potion_attack` | Attack potion | Eye of newt | 3 | 25.0 |
| `herblore_potion_antipoison` | Antipoison | Unicorn horn dust | 5 | 37.5 |
| `herblore_potion_strength` | Strength potion | Limpwurt root | 12 | 50.0 |
| `herblore_potion_restore` | Restore potion | Red spiders' eggs | 22 | 62.5 |
| `herblore_potion_energy` | Energy potion | Chocolate dust | 26 | 67.5 |
| `herblore_potion_defence` | Defence potion | White berries | 30 | 75.0 |
| `herblore_potion_prayer` | Prayer potion | Snape grass | 38 | 87.5 |
| `herblore_potion_super_attack` | Super attack | Eye of newt | 45 | 100.0 |
| `herblore_potion_superantipoison` | Super antipoison | Unicorn horn dust | 48 | 106.3 |
| `herblore_potion_super_energy` | Super energy | Mort myre fungus | 52 | 117.5 |
| `herblore_potion_super_strength` | Super strength | Limpwurt root | 55 | 112.5 |
| `herblore_potion_super_restore` | Super restore | Red spiders' eggs | 63 | 142.5 |
| `herblore_potion_super_defence` | Super defence | White berries | 66 | 150.0 |
| `herblore_potion_ranging` | Ranging potion | Wine of zamorak | 72 | 162.5 |
| `herblore_potion_magic` | Magic potion | Cactus spine | 76 | 172.5 |
