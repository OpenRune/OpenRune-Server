# Lumbridge Build-Out Design Specification

**Date:** 2026-03-24
**Scope:** Full Fishing, Cooking, and Crafting skill systems + Lumbridge NPC/world population
**Approach:** Skills-first (global systems), then Lumbridge placement and world population

---

## 1. Overview

Build three complete skill systems (Fishing, Cooking, Crafting) as global modules following existing patterns (Mining, Woodcutting, Smithing), then populate Lumbridge with all missing NPCs, dialogues, skilling spots, and object interactions.

### Existing Foundation
- 16 Lumbridge plugins already exist (2 shops, 1 quest, ~30 NPC spawns, gate toll, stairs)
- Mining, Woodcutting, Smithing, Firemaking, Herblore, Prayer, Runecrafting skills are implemented
- Fishing, Cooking, Crafting have zero implementation — data layer and plugins both missing
- `FoodTable.kt` already defines 83 consumable foods (cooked fish included as eatables)
- `TableGenerater.kt` already knows about "fishing", "cooking", "crafting" prefixes

### Architecture Pattern
All skills follow: Cache table definitions -> Code gen -> Row classes -> Content plugins

```
cache/src/main/kotlin/org/alter/impl/skills/<Skill>.kt   (dbTable definitions)
content/src/main/resources/org/alter/skills/<skill>/gamevals.toml  (RSCM mappings)
  -> TableGenerater produces org.generated.tables.<skill>.<Row>.kt
content/src/main/kotlin/org/alter/skills/<skill>/  (PluginEvent implementations)
```

### Event Registration Convention
The new plugin system has convenience DSL helpers for some events (`onNpcOption`, `onObjectOption`, `onButton`, `onLogin`, `onTimer`, `onItemOnItem`) but **not** for item-on-object or item-on-NPC interactions. For those, use raw event registration:
```kotlin
on<ItemOnObject> {
    where { /* condition */ }
    then { /* action */ }
}.submit()

on<ItemOnNpcEvent> {
    where { /* condition */ }
    then { /* action */ }
}.submit()
```
This matches the pattern used in SmeltingEvents.kt, CampfireEvents.kt, and GildedAlterEvents.kt.

---

## 2. Fishing Skill System

### 2.1 Data Layer

**`cache/src/main/kotlin/org/alter/impl/skills/Fishing.kt`**

DB table `fishing_spots` with columns:
- Spot NPC ID (RSCM) — fishing spots are NPCs in OSRS, not objects
- Fish item output (RSCM)
- Level required (int)
- XP awarded (int, stored as x10 for decimals — e.g., 105 = 10.5 XP)
- Tool required (RSCM — net, rod, harpoon, lobster pot, etc.)
- Bait required (RSCM, nullable — fishing bait, feather, dark bait, etc.)
- Catch rate low (int)
- Catch rate high (int)
- Members-only (boolean)
- Spot type (string — "net", "bait", "lure", "cage", "harpoon")
- Animation ID (int — fishing animation varies by tool type)

DB table `fishing_tools` with columns:
- Tool item ID (RSCM)
- Tool type (string)
- Speed modifier (int, percentage — e.g., 80 = 0.8x, 65 = 0.65x for crystal harpoon)
- Animation ID (int — animation when using this tool)

**`content/src/main/resources/org/alter/skills/fishing/gamevals.toml`**

RSCM entries for tables, dbrows for each fish type and spot configuration.

Code gen produces: `FishingSpotRow`, `FishingToolRow`

### 2.2 Fish Covered

**F2P:**
| Fish | Level | XP | Tool | Bait |
|------|-------|-----|------|------|
| Shrimps | 1 | 10 | Small net | — |
| Sardine | 5 | 20 | Fishing rod | Fishing bait |
| Herring | 10 | 30 | Fishing rod | Fishing bait |
| Anchovies | 15 | 40 | Small net | — |
| Trout | 20 | 50 | Fly fishing rod | Feather |
| Pike | 25 | 60 | Fishing rod | Fishing bait |
| Salmon | 30 | 70 | Fly fishing rod | Feather |
| Tuna | 35 | 80 | Harpoon | — |
| Lobster | 40 | 90 | Lobster pot | — |
| Swordfish | 50 | 100 | Harpoon | — |

**Members (core):**
| Fish | Level | XP | Tool | Notes |
|------|-------|-----|------|-------|
| Mackerel | 16 | 20 | Big net | — |
| Cod | 23 | 45 | Big net | — |
| Bass | 46 | 100 | Big net | — |
| Monkfish | 62 | 120 | Small net | Requires Swan Song |
| Shark | 76 | 110 | Harpoon | — |
| Anglerfish | 82 | 120 | Fishing rod | Fishing bait |
| Dark crab | 85 | 130 | Lobster pot | Dark fishing bait |
| Karambwan | 65 | 105 | Karambwan vessel | Requires Tai Bwo Wannai Trio |
| Lava eel | 53 | 30 | Oily fishing rod | — |
| Infernal eel | 80 | 95 | Oily fishing rod | Requires Mor Ul Rek access |
| Sacred eel | 87 | 105 | Fishing rod | Fishing bait, requires Regicide |
| Leaping trout | 48 | 50 | Barbarian rod | Feather, also req Str 15 Agi 15 |
| Leaping salmon | 58 | 70 | Barbarian rod | Feather, also req Str 30 Agi 30 |
| Leaping sturgeon | 70 | 80 | Barbarian rod | Feather, also req Str 45 Agi 45 |

### 2.3 Plugin Architecture

**`content/src/main/kotlin/org/alter/skills/fishing/FishingPlugin.kt`**

Main plugin extending `PluginEvent`. In `init()`:
- Loads all `FishingSpotRow.all()` and groups by spot NPC ID
- Registers `onNpcOption` for each fishing spot NPC with "Net", "Bait", "Lure", "Cage", "Harpoon", "Use-rod" options
- Note: Fishing spots are NPCs in OSRS, not objects. Use `onNpcOption` (not `onObjectOption`)
- Each handler:
  1. Determines which fish are available at this spot for the selected option
  2. Validates tool in inventory or equipped
  3. Validates bait if required
  4. Checks fishing level
  5. Enters main loop via `player.queue { fishLoop(...) }`

Main loop (`suspend fun QueueTask.fishLoop`):
- `repeatWhile(delay = 5, immediate = false, canRepeat = { inventoryNotFull && hasTool && hasBait })`:
  - Play fishing animation
  - Roll `success(catchRateLow, catchRateHigh, fishingLevel)`
  - On success: consume bait (if applicable), add fish to inventory, award XP, post `FishObtainedEvent`
  - On fail: continue animating
- On inventory full: `player.message("You can't carry any more fish.")`
- On missing bait: `player.message("You don't have any bait left.")`

**`content/src/main/kotlin/org/alter/skills/fishing/FishingEnhancers.kt`**

Listens for `FishObtainedEvent` to apply bonuses:
- Angler outfit: 2.5% total XP boost (0.4% hat, 0.8% top, 0.6% waders, 0.2% boots, 0.5% set bonus)
- Rada's blessing: 2/4/6/8% chance for double fish (tiers 1-4)
- Spirit flakes: 50% double fish chance, consumes one flake per catch
- Dragon/Infernal/Crystal harpoon: speed modifier applied in main plugin tool lookup

**`content/src/main/kotlin/org/alter/skills/fishing/FishObtainedEvent.kt`**

```kotlin
class FishObtainedEvent(
    val fish: Int,       // Fish item ID
    val spotNpc: Int,    // Spot NPC ID
    player: Player
) : SkillingActionCompletedGatheringEvent(player, Skills.FISHING, fish)
```

Note: Extends `SkillingActionCompletedGatheringEvent` (not `PlayerEvent`) to integrate with cross-cutting systems like clue bottle drops and collection logging, matching the pattern used by `RockOreObtainedEvent` in Mining.

### 2.4 Spot Movement

Fishing spots are spawned as NPCs (matching OSRS behavior). Movement logic:
- Each spot has a random lifetime of 50-300 ticks
- When timer expires, spot despawns and respawns at a random valid tile within the defined area
- Static spots (Isle of Souls, Karambwan) skip this mechanic

---

## 3. Cooking Skill System

### 3.1 Data Layer

**`cache/src/main/kotlin/org/alter/impl/skills/Cooking.kt`**

DB table `cooking_recipes` with columns:
- Raw item (RSCM)
- Cooked item (RSCM)
- Burnt item (RSCM)
- Level required (int)
- XP awarded (double)
- Burn stop level on fire (int)
- Burn stop level on range (int)
- Cooking method — 0=both fire and range, 1=range only, 2=spit roast only
- Members-only (boolean)

**`content/src/main/resources/org/alter/skills/cooking/gamevals.toml`**

RSCM entries for all raw, cooked, and burnt item variants plus table/dbrow IDs.

Code gen produces: `CookingRecipeRow`

### 3.2 Items Covered

**Fish (F2P):** Shrimps(1, 30xp), Sardine(1, 40xp), Herring(5, 50xp), Anchovies(1, 30xp), Trout(15, 70xp), Pike(20, 80xp), Salmon(25, 90xp), Tuna(30, 100xp), Lobster(40, 120xp), Swordfish(45, 140xp)

**Fish (Members):** Mackerel(10, 60xp), Cod(18, 75xp), Bass(43, 130xp), Monkfish(62, 150xp), Shark(80, 210xp), Sea turtle(82, 211.3xp), Anglerfish(84, 230xp), Manta ray(91, 216.2xp), Karambwan(30, 190xp)

**Meat:** Cooked meat(1, 30xp), Cooked chicken(1, 30xp)

**Baked goods (range only):** Bread(1, 40xp), Redberry pie(10, 78xp), Meat pie(20, 110xp), Apple pie(30, 130xp), Plain pizza(35, 143xp), Meat pizza(45, 169xp), Anchovy pizza(55, 182xp), Cake(40, 180xp), Chocolate cake(50, 210xp)

**Other:** Stew(25, 117xp), Wine(35, 200xp — no burn, fermentation delay), Baked potato(7, 15xp)

### 3.3 Plugin Architecture

**`content/src/main/kotlin/org/alter/skills/cooking/CookingPlugin.kt`**

Main plugin. In `init()`:
- Loads all `CookingRecipeRow.all()` keyed by raw item ID
- Registers `on<ItemOnObject> { where { ... } then { ... } }` for raw food items on range objects (category-based) and fire objects
- Note: No `onItemOnObject` DSL helper exists in the new event system. Use raw `on<ItemOnObject>` with `where` clauses, matching the pattern in SmeltingEvents.kt and CampfireEvents.kt
- Handler flow:
  1. Look up recipe from raw item ID
  2. Check cooking level
  3. Check cooking method compatibility (range-only items can't be cooked on fires)
  4. Open cooking interface — item icon, "How many?" selection
  5. Enter cook loop via `player.queue { cookLoop(...) }`

Cook loop (`suspend fun QueueTask.cookLoop`):
- `repeatWhile(delay = 4, immediate = false, canRepeat = { hasRawItem && count > 0 })`:
  - Play cooking animation + sound
  - Roll burn check via `CookingBurnRates.shouldBurn(player, recipe, isFire)`
  - If burned: remove raw, add burnt item, message "You accidentally burn the food"
  - If success: remove raw, add cooked item, award XP, post `FoodCookedEvent`
  - Decrement count

Wine special case:
- Combine grapes + jug of water → no interface, starts 12-tick ferment timer
- After timer: jug of wine or jug of bad wine (based on level, stops failing at 68)

**`content/src/main/kotlin/org/alter/skills/cooking/CookingBurnRates.kt`**

```kotlin
fun shouldBurn(player: Player, recipe: CookingRecipeRow, isFire: Boolean): Boolean {
    // Cooking cape = never burn
    if (hasEquipped(player, "items.cooking_cape", "items.cooking_cape_t", "items.max_cape"))
        return false

    val burnStop = if (isFire) recipe.burnStopFire else recipe.burnStopRange

    // Cooking gauntlets override for specific fish
    val effectiveBurnStop = if (hasEquipped(player, "items.cooking_gauntlets"))
        getGauntletBurnStop(recipe, burnStop) else burnStop

    // Lumbridge range bonus
    val lumbridgeBonus = isLumbridgeRange(player) && hasCompletedCooksAssistant(player)

    val level = player.getSkills().getCurrentLevel(Skills.COOKING)
    if (level >= effectiveBurnStop) return false

    val burnChance = (effectiveBurnStop - level).toDouble() / effectiveBurnStop
    val modifier = if (lumbridgeBonus) 0.95 else 1.0
    return Math.random() < (burnChance * modifier)
}
```

Gauntlet overrides (specific burn stop level reductions):
- Lobster: 74→64
- Swordfish: 86→81
- Monkfish: 92→87
- Shark: 99→94

**`content/src/main/kotlin/org/alter/skills/cooking/CookingEvents.kt`**

```kotlin
class FoodCookedEvent(
    val rawItem: Int,
    val cookedItem: Int,
    val isFire: Boolean,  // true=campfire, false=range
    player: Player
) : SkillingActionCompletedEvent(player, Skills.COOKING)
```

Note: Extends `SkillingActionCompletedEvent` (not `PlayerEvent`) for cross-cutting integration. Includes `isFire` field for enhancers that depend on cooking method.

---

## 4. Crafting Skill System

### 4.1 Data Layer

**`cache/src/main/kotlin/org/alter/impl/skills/Crafting.kt`**

Multiple DB tables:

**`crafting_spinning`**: input item, output item, level, XP
**`crafting_pottery`**: unfired item, fired item, level, shape XP, fire XP
**`crafting_leather`**: output item, leather type, amount needed, level, XP, thread cost
**`crafting_gems`**: uncut gem, cut gem, crush item (nullable), level, XP, crush XP
**`crafting_jewelry_gold`**: gem (nullable for plain), output, level, XP, mould
**`crafting_jewelry_silver`**: output, level, XP, mould
**`crafting_glass`**: output, level, XP

**`content/src/main/resources/org/alter/skills/crafting/gamevals.toml`**

RSCM entries for all crafting items, tools, tables, and dbrows.

Code gen produces: `CraftingSpinningRow`, `CraftingPotteryRow`, `CraftingLeatherRow`, `CraftingGemRow`, `CraftingJewelryGoldRow`, `CraftingJewelrySilverRow`, `CraftingGlassRow`

### 4.2 Subsystems

#### 4.2.1 Spinning (`SpinningPlugin.kt`)

`on<ItemOnObject> { where { ... } then { ... } }` for items on spinning wheel objects.

| Input | Output | Level | XP |
|-------|--------|-------|----|
| Wool | Ball of wool | 1 | 2.5 |
| Flax | Bow string | 10 | 15 |
| Sinew/Roots | Crossbow string | 10 | 15 |
| Linen | Linen yarn | 12 | 16 |
| Magic roots | Magic string | 19 | 30 |
| Hair | Rope | 30 | 25 |
| Hemp | Hemp yarn | 39 | 60 |
| Cotton | Cotton yarn | 73 | 105 |

Loop: 3-tick cycle, animate, consume input, produce output, award XP.

#### 4.2.2 Pottery (`PotteryPlugin.kt`)

Two-step process:

**Step 1 — Shaping:** `on<ItemOnObject>` for soft clay on potter's wheel.
Opens interface to select item. Each shape: animate, consume clay, produce unfired item, award shape XP.

**Step 2 — Firing:** `on<ItemOnObject>` for unfired item on pottery oven.
Animate, consume unfired, produce finished item, award fire XP.

| Item | Level | Shape XP | Fire XP | Total |
|------|-------|----------|---------|-------|
| Pot | 1 | 6.3 | 6.3 | 12.6 |
| Empty cup | 3 | 8.5 | 8.5 | 17 |
| Pie dish | 7 | 15 | 10 | 25 |
| Bowl | 8 | 18 | 15 | 33 |
| Plant pot | 19 | 20 | 17.5 | 37.5 |
| Pot lid | 25 | 20 | 20 | 40 |

#### 4.2.3 Leather (`LeatherPlugin.kt`)

`onItemOnItem` for needle on leather (or leather on needle — use `SatisfyType.ANY` so order doesn't matter).
Opens interface showing available items for the leather type held.
Each craft: 4-tick cycle, animate, consume leather + thread, produce item, award XP.

**Regular leather (F2P):**
| Item | Level | XP |
|------|-------|----|
| Gloves | 1 | 13.8 |
| Boots | 7 | 16.3 |
| Cowl | 9 | 18.5 |
| Vambraces | 11 | 22 |
| Body | 14 | 25 |
| Chaps | 18 | 27 |
| Hardleather body | 28 | 35 |

**Dragonhide (Members):**
| Item | Level | XP | Hides |
|------|-------|----|-------|
| Green d'hide vambraces | 57 | 62 | 1 |
| Green d'hide chaps | 60 | 124 | 2 |
| Green d'hide body | 63 | 186 | 3 |
| Blue d'hide vambraces | 66 | 70 | 1 |
| Blue d'hide chaps | 68 | 140 | 2 |
| Blue d'hide body | 71 | 210 | 3 |
| Red d'hide vambraces | 73 | 78 | 1 |
| Red d'hide chaps | 75 | 156 | 2 |
| Red d'hide body | 77 | 234 | 3 |
| Black d'hide vambraces | 79 | 86 | 1 |
| Black d'hide chaps | 82 | 172 | 2 |
| Black d'hide body | 84 | 258 | 3 |

Studded armor: leather body/chaps + steel studs at 41/44.

Thread consumption: 1 per item, 4 for bodies.

#### 4.2.4 Gem Cutting (`GemCuttingPlugin.kt`)

`onItemOnItem` for chisel on uncut gem (use `SatisfyType.ANY` so order doesn't matter). No interface — instant craft.
3-tick cycle, animate, consume uncut, produce cut gem, award XP.

**F2P:**
| Gem | Level | XP |
|-----|-------|----|
| Sapphire | 20 | 50 |
| Emerald | 27 | 67.5 |
| Ruby | 34 | 85 |
| Diamond | 43 | 107.5 |

**Members:**
| Gem | Level | XP | Can Crush |
|-----|-------|----|-----------|
| Opal | 1 | 15 | Yes (3.8xp) |
| Jade | 13 | 20 | Yes (5xp) |
| Red topaz | 16 | 25 | Yes (6.3xp) |
| Dragonstone | 55 | 137.5 | No |
| Onyx | 67 | 167.5 | No |
| Zenyte | 89 | 200 | No |

Semi-precious gems (opal, jade, red topaz) have a chance to crush based on crafting level.

#### 4.2.5 Jewelry (`JewelryPlugin.kt`)

`on<ItemOnObject>` for gold/silver bar on furnace. Opens interface showing available jewelry.
Each craft: 3-tick cycle, animate, consume bar + gem (if applicable), produce jewelry, award XP.
Requires appropriate mould in inventory (ring mould, necklace mould, amulet mould, bracelet mould).

**Gold jewelry (F2P):**
| Item | Level | XP | Gem |
|------|-------|----|-----|
| Gold ring | 5 | 15 | — |
| Gold necklace | 6 | 20 | — |
| Gold bracelet | 7 | 25 | — |
| Gold amulet (u) | 8 | 30 | — |
| Sapphire ring | 20 | 40 | Sapphire |
| Sapphire necklace | 22 | 55 | Sapphire |
| Sapphire bracelet | 23 | 60 | Sapphire |
| Sapphire amulet (u) | 24 | 65 | Sapphire |
| Emerald ring | 27 | 55 | Emerald |
| Emerald necklace | 29 | 60 | Emerald |
| Emerald bracelet | 30 | 65 | Emerald |
| Emerald amulet (u) | 31 | 70 | Emerald |
| Ruby ring | 34 | 70 | Ruby |
| Ruby necklace | 40 | 75 | Ruby |
| Ruby bracelet | 42 | 80 | Ruby |
| Ruby amulet (u) | 50 | 85 | Ruby |
| Diamond ring | 43 | 85 | Diamond |
| Diamond necklace | 56 | 90 | Diamond |
| Diamond bracelet | 58 | 95 | Diamond |
| Diamond amulet (u) | 70 | 100 | Diamond |

**Members gold jewelry** extends with Dragonstone, Onyx, Zenyte tiers.

**Silver jewelry (Members):** Opal/Jade/Topaz rings, necklaces, bracelets, amulets. Holy symbol, Unholy symbol, Tiara.

#### 4.2.6 Glass Blowing (`GlassBlowingPlugin.kt`)

`onItemOnItem` for glassblowing pipe on molten glass. Opens interface.
Each craft: 3-tick cycle, animate, consume molten glass, produce item, award XP.

| Item | Level | XP |
|------|-------|----|
| Beer glass | 1 | 17.5 |
| Empty candle lantern | 4 | 19 |
| Empty oil lamp | 12 | 25 |
| Vial | 33 | 35 |
| Fishbowl | 42 | 42.5 |
| Unpowered orb | 46 | 52.5 |
| Lantern lens | 49 | 55 |
| Light orb | 87 | 70 |

Molten glass creation: Bucket of sand + soda ash on furnace (separate interaction, part of furnace handlers).

---

## 5. Lumbridge NPC Population

### 5.1 New NPC Plugins

All NPCs get exact OSRS dialogue. Quest-related dialogue branches on quest state (not started / in progress / completed). Quest progression logic itself is out of scope — just the dialogue trees.

#### Duke Horacio (`DukeHoracioPlugin.kt`)
- **Spawn:** (3210, 3220, height=1) — Castle 1st floor
- **Options:** Talk-to
- **Dialogue:** Discusses ruling Lumbridge, concerned about threats. Quest hooks for Rune Mysteries (gives air talisman), Shield of Arrav. Gives anti-dragon shield on request.
- **Walk radius:** 0 (stays in throne room)

#### Sigmund (`SigmundPlugin.kt`)
- **Spawn:** (3210, 3222, height=1) — Castle 1st floor
- **Options:** Talk-to
- **Dialogue:** Duke's advisor. Suspicious, secretive. Lost Tribe quest hook.
- **Walk radius:** 2

#### Father Urhney (`FatherUrhneyPlugin.kt`)
- **Spawn:** (3147, 3175) — Lumbridge Swamp house
- **Options:** Talk-to
- **Dialogue:** Grumpy hermit. Gives Ghostspeak amulet for Restless Ghost quest. Replacement amulet if lost.
- **Walk radius:** 0

#### Fred the Farmer (`FredTheFarmerPlugin.kt`)
- **Spawn:** (3190, 3273) — Farm north of Lumbridge
- **Options:** Talk-to
- **Dialogue:** Sheep Shearer quest start. Needs 20 balls of wool. Discusses farming life.
- **Walk radius:** 5

#### Gillie Groats (`GillieGroatsPlugin.kt`)
- **Spawn:** (3253, 3270) — Cow field east of river
- **Options:** Talk-to
- **Dialogue:** Teaches cow milking. Explains how to use bucket on dairy cow. Discusses Groats family farm.
- **Walk radius:** 3

#### Millie Miller (`MillieMillerPlugin.kt`)
- **Spawn:** (3230, 3318) — Windmill ground floor
- **Options:** Talk-to
- **Dialogue:** Teaches flour making process. Explains hopper, controls, and flour bin. Step-by-step instructions.
- **Walk radius:** 2

#### Veos (`VeosPlugin.kt`)
- **Spawn:** (3228, 3241) — Inside The Sheared Ram pub
- **Options:** Talk-to
- **Dialogue:** X Marks the Spot quest start hook. Offers passage to Great Kourend. Discusses treasure hunting.
- **Walk radius:** 0

#### Perdu (`PerduPlugin.kt`)
- **Spawn:** (3229, 3218) — Near castle
- **Options:** Talk-to, Trade
- **Dialogue:** Lost item reclamation. Opens shop interface for repurchasing lost quest/achievement items.
- **Walk radius:** 0

#### Adventurer Jon (`AdventurerJonPlugin.kt`)
- **Spawn:** (3234, 3224) — Near castle
- **Options:** Talk-to
- **Dialogue:** Adventure paths guide. Suggests activities for new players. Points to quests, skills, combat.
- **Walk radius:** 3

#### Arthur the Clue Hunter (`ArthurClueHunterPlugin.kt`)
- **Spawn:** (3209, 3214) — Castle ground floor
- **Options:** Talk-to
- **Dialogue:** Explains clue scrolls, treasure trails tiers, how to obtain them.
- **Walk radius:** 0

### 5.2 Existing NPCs — Dialogue Updates

**Father Aereck** (already spawned at 3243, 3206):
- Add Restless Ghost quest dialogue branch. Currently just spawned, needs full quest-start conversation.

**Hans** (already at 3221, 3219):
- Already has dialogue. Verify playtime checking works correctly.

### 5.3 Additional NPC Spawns

Add to existing spawn plugins or create new spawn file:

**Cow field (east of river):**
- 8-10 Cows at (3253-3265, 3255-3300)
- 2-3 Calves
- 1 Dairy cow object

**Chicken coop (Fred's farm area):**
- 5-6 Chickens at (3185-3195, 3275-3280)
- Egg ground item spawns

**General wanderers:**
- Additional Men/Women NPCs in Lumbridge town center
- Guard NPCs near castle entrance

---

## 6. Lumbridge Object Interactions

### 6.1 Skilling Objects

#### Cooking Range
- **Location:** Castle kitchen (3211, 3216)
- **Interaction:** Handled globally by CookingPlugin — all ranges are registered by object category, no Lumbridge-specific file needed
- **Special property:** Lumbridge range has reduced burn rate after Cook's Assistant completion. CookingBurnRates checks player tile + quest completion for bonus.

#### Spinning Wheel
- **Location:** Castle 1st floor (~3209, 3220, height=1)
- **Interaction:** Handled globally by SpinningPlugin — all spinning wheels registered by object ID/category, no Lumbridge-specific file needed

#### Furnace
- **Location:** Smithing building (~3226, 3254)
- Already exists for Smithing. Crafting jewelry/glass interactions register on the same furnace object category.
- No new plugin needed — JewelryPlugin and GlassBlowingPlugin register globally on furnace objects.

#### Church Altar (`AltarPlugin.kt` — global, under `content/src/main/kotlin/org/alter/objects/`)
- **Interaction:** `onObjectOption` "Pray-at" on all altar objects
- **Effect:** Restore prayer points to max, play prayer animation, message "You recharge your Prayer points."
- Global plugin, not Lumbridge-specific. Lumbridge church altar at (3243, 3206) is one of many.

### 6.2 Resource Interactions

#### Dairy Cow (`DairyCowPlugin.kt` — global, under `content/src/main/kotlin/org/alter/objects/`)
- **Interaction:** `onObjectOption` "Milk" or `on<ItemOnObject>` bucket on dairy cow
- **Effect:** If player has empty bucket → animate → give bucket of milk
- **No bucket:** "You need an empty bucket to milk this cow."
- Global plugin — works on all dairy cow objects. Lumbridge cow field is one location.

#### Sheep Shearing (`SheepShearingPlugin.kt` — global, under `content/src/main/kotlin/org/alter/interactions/`)
- **NPC interaction:** `on<ItemOnNpcEvent>` shears on sheep, or `onNpcOption` "Shear"
- **Effect:** Animate shearing → give wool → sheep transforms to shorn variant NPC
- **Regrowth:** Shorn sheep respawns as woolly after ~100 ticks
- Global plugin — works on all sheep NPCs.

#### Windmill (`WindmillPlugin.kt` — global, under `content/src/main/kotlin/org/alter/objects/`)
- **Three objects (works on all windmills globally):**
  1. **Hopper** (top floor): `on<ItemOnObject>` grain on hopper → "You put the grain in the hopper." Sets player varbit.
  2. **Hopper controls** (top floor): `onObjectOption` "Operate" → "You operate the hopper. The grain slides down." Updates varbit.
  3. **Flour bin** (ground floor): `onObjectOption` "Empty" with empty pot in inventory → gives pot of flour. Clears varbit.
- Varbit tracks: 0=empty, 1=grain in hopper, 2=flour ready in bin
- Lumbridge windmill at (~3230, 3318) is one of several.

### 6.3 Ground Item Spawns

Add to Lumbridge spawn plugins:
- **Eggs:** Chicken coop areas (~3185, 3275) and (~3235, 3298)
- **Cabbage:** Near general store (~3213, 3250)
- **Onion:** Near general store (~3210, 3248)
- **Bronze pickaxe:** Gate towers
- Verify existing spawns (logs, mind runes, bronze arrows, daggers) are correct.

---

## 7. File Structure

### New Files

```
# Cache module (data layer)
cache/src/main/kotlin/org/alter/impl/skills/
  Fishing.kt
  Cooking.kt
  Crafting.kt

# Gamevals (RSCM mappings)
content/src/main/resources/org/alter/skills/fishing/gamevals.toml
content/src/main/resources/org/alter/skills/cooking/gamevals.toml
content/src/main/resources/org/alter/skills/crafting/gamevals.toml

# Generated (by TableGenerater — not hand-written)
content/src/main/kotlin/org/generated/tables/fishing/
  FishingSpotRow.kt
  FishingToolRow.kt
content/src/main/kotlin/org/generated/tables/cooking/
  CookingRecipeRow.kt
content/src/main/kotlin/org/generated/tables/crafting/
  CraftingSpinningRow.kt
  CraftingPotteryRow.kt
  CraftingLeatherRow.kt
  CraftingGemRow.kt
  CraftingJewelryGoldRow.kt
  CraftingJewelrySilverRow.kt
  CraftingGlassRow.kt

# Skill plugins
content/src/main/kotlin/org/alter/skills/fishing/
  FishingPlugin.kt
  FishingEnhancers.kt
  FishObtainedEvent.kt
content/src/main/kotlin/org/alter/skills/cooking/
  CookingPlugin.kt
  CookingBurnRates.kt
  CookingEvents.kt
content/src/main/kotlin/org/alter/skills/crafting/
  SpinningPlugin.kt
  PotteryPlugin.kt
  LeatherPlugin.kt
  GemCuttingPlugin.kt
  JewelryPlugin.kt
  GlassBlowingPlugin.kt

# Lumbridge NPCs
content/src/main/kotlin/org/alter/areas/lumbridge/npcs/
  DukeHoracioPlugin.kt
  SigmundPlugin.kt
  FatherUrhneyPlugin.kt
  FredTheFarmerPlugin.kt
  GillieGroatsPlugin.kt
  MillieMillerPlugin.kt
  VeosPlugin.kt
  PerduPlugin.kt
  AdventurerJonPlugin.kt
  ArthurClueHunterPlugin.kt

# Global object/interaction plugins (not Lumbridge-specific)
content/src/main/kotlin/org/alter/objects/
  AltarPlugin.kt
  DairyCowPlugin.kt
  WindmillPlugin.kt
content/src/main/kotlin/org/alter/interactions/
  SheepShearingPlugin.kt

# Lumbridge spawns
content/src/main/kotlin/org/alter/areas/lumbridge/spawns/
  FishingSpotSpawns.kt
  CowFieldSpawns.kt
  ItemSpawnPlugin.kt
```

### Modified Files
- Existing Lumbridge `SpawnPlugin.kt` / `ChatSpawnsPlugin.kt` — add missing NPC spawns
- Existing Father Aereck plugin — add Restless Ghost dialogue branches

---

## 8. Implementation Order

1. **Fishing data layer** — cache table defs, gamevals, code gen
2. **Fishing plugins** — FishingPlugin, FishingEnhancers, FishObtainedEvent
3. **Cooking data layer** — cache table defs, gamevals, code gen
4. **Cooking plugins** — CookingPlugin, CookingBurnRates, CookingEvents
5. **Crafting data layer** — cache table defs, gamevals, code gen
6. **Crafting plugins** — all 6 subsystem plugins
7. **Global interaction plugins** — AltarPlugin, DairyCowPlugin, WindmillPlugin, SheepShearingPlugin (these are global, not Lumbridge-specific)
8. **Lumbridge spawns** — fishing spots, cows, chickens, eggs, cabbages, missing NPCs (can be parallelized with step 7)
9. **Lumbridge NPC plugins** — all 10 new NPCs with exact dialogue
10. **Existing NPC updates** — Father Aereck Restless Ghost dialogue

---

## 9. Out of Scope

- Quest progression logic (dialogue stubs only, no quest state machines)
- Barbarian fishing (requires separate training unlock)
- Minigames (Tempoross, Fishing Trawler)
- Brewing subsystem
- Advanced members crafting (crystal singing, birdhouses, xerician, splitbark, mixed hide, hueycoatl)
- Aerial fishing, drift net fishing, bare-handed fishing
- 2-tick cooking exploit mechanics
- Lumbridge Swamp caves / H.A.M. Hideout
- Music tracks
- Agility shortcuts
