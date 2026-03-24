# Lumbridge Build-Out Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build complete Fishing, Cooking, and Crafting skill systems, then populate Lumbridge with all missing NPCs, skilling spots, and object interactions.

**Architecture:** Each skill follows the existing 3-layer pipeline: cache table definitions (`dbTable()` in `cache/src/main/kotlin/org/alter/impl/skills/`) → gamevals TOML (`content/src/main/resources/`) → code gen produces Row classes → content plugins (`content/src/main/kotlin/org/alter/skills/`). All plugins extend `PluginEvent`, use RSCM strings for entity IDs, and register via `on<EventType>` DSL.

**Tech Stack:** Kotlin 2.0, Gradle KDS, KSP code generation, OpenRune cache tools, RSProt protocol

**Spec:** `docs/superpowers/specs/2026-03-24-lumbridge-buildout-design.md`

**Testing note:** This codebase has minimal unit tests. Verification is compile-check + in-game functional testing. Each task ends with a compilation check (`./gradlew :content:compileKotlin` or full build).

---

## Phase 1: Fishing Skill System

### Task 1: Fishing Cache Table Definitions

**Files:**
- Create: `cache/src/main/kotlin/org/alter/impl/skills/Fishing.kt`

- [ ] **Step 1: Create the Fishing object with column constants and spot table**

Follow the exact pattern from `Mining.kt`. The `fishing_spots` table defines each fish type with its spot NPC, level, XP, tool, bait, and catch rates.

```kotlin
package org.alter.impl.skills

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object Fishing {

    // fishing_spots columns
    const val COL_SPOT_NPC = 0
    const val COL_LEVEL = 1
    const val COL_XP = 2
    const val COL_FISH_ITEM = 3
    const val COL_TOOL = 4
    const val COL_BAIT = 5
    const val COL_CATCH_RATE_LOW = 6
    const val COL_CATCH_RATE_HIGH = 7
    const val COL_SPOT_TYPE = 8
    const val COL_MEMBERS = 9
    const val COL_ANIMATION = 10

    // fishing_tools columns
    const val TOOL_ITEM = 0
    const val TOOL_TYPE = 1
    const val TOOL_SPEED_MOD = 2
    const val TOOL_ANIMATION = 3

    fun spots() = dbTable("tables.fishing_spots", serverOnly = true) {
        column("spot_npc", COL_SPOT_NPC, VarType.NPC)
        column("level", COL_LEVEL, VarType.INT)
        column("xp", COL_XP, VarType.INT)
        column("fish_item", COL_FISH_ITEM, VarType.OBJ)
        column("tool", COL_TOOL, VarType.OBJ)
        column("bait", COL_BAIT, VarType.OBJ)
        column("catch_rate_low", COL_CATCH_RATE_LOW, VarType.INT)
        column("catch_rate_high", COL_CATCH_RATE_HIGH, VarType.INT)
        column("spot_type", COL_SPOT_TYPE, VarType.STRING)
        column("members", COL_MEMBERS, VarType.BOOLEAN)
        column("animation", COL_ANIMATION, VarType.SEQ)

        // --- F2P Fish ---

        // Shrimps (net, level 1)
        row("dbrows.fishing_shrimps") {
            columnRSCM(COL_FISH_ITEM, "items.raw_shrimps")
            columnRSCM(COL_TOOL, "items.small_fishing_net")
            column(COL_LEVEL, 1)
            column(COL_XP, 10)
            column(COL_CATCH_RATE_LOW, 128)
            column(COL_CATCH_RATE_HIGH, 400)
            column(COL_SPOT_TYPE, "net")
        }

        // Sardine (bait, level 5)
        row("dbrows.fishing_sardine") {
            columnRSCM(COL_FISH_ITEM, "items.raw_sardine")
            columnRSCM(COL_TOOL, "items.fishing_rod")
            columnRSCM(COL_BAIT, "items.fishing_bait")
            column(COL_LEVEL, 5)
            column(COL_XP, 20)
            column(COL_CATCH_RATE_LOW, 120)
            column(COL_CATCH_RATE_HIGH, 380)
            column(COL_SPOT_TYPE, "bait")
        }

        // Herring (bait, level 10)
        row("dbrows.fishing_herring") {
            columnRSCM(COL_FISH_ITEM, "items.raw_herring")
            columnRSCM(COL_TOOL, "items.fishing_rod")
            columnRSCM(COL_BAIT, "items.fishing_bait")
            column(COL_LEVEL, 10)
            column(COL_XP, 30)
            column(COL_CATCH_RATE_LOW, 100)
            column(COL_CATCH_RATE_HIGH, 350)
            column(COL_SPOT_TYPE, "bait")
        }

        // Anchovies (net, level 15)
        row("dbrows.fishing_anchovies") {
            columnRSCM(COL_FISH_ITEM, "items.raw_anchovies")
            columnRSCM(COL_TOOL, "items.small_fishing_net")
            column(COL_LEVEL, 15)
            column(COL_XP, 40)
            column(COL_CATCH_RATE_LOW, 90)
            column(COL_CATCH_RATE_HIGH, 320)
            column(COL_SPOT_TYPE, "net")
        }

        // Trout (lure, level 20)
        row("dbrows.fishing_trout") {
            columnRSCM(COL_FISH_ITEM, "items.raw_trout")
            columnRSCM(COL_TOOL, "items.fly_fishing_rod")
            columnRSCM(COL_BAIT, "items.feather")
            column(COL_LEVEL, 20)
            column(COL_XP, 50)
            column(COL_CATCH_RATE_LOW, 80)
            column(COL_CATCH_RATE_HIGH, 300)
            column(COL_SPOT_TYPE, "lure")
        }

        // Pike (bait, level 25)
        row("dbrows.fishing_pike") {
            columnRSCM(COL_FISH_ITEM, "items.raw_pike")
            columnRSCM(COL_TOOL, "items.fishing_rod")
            columnRSCM(COL_BAIT, "items.fishing_bait")
            column(COL_LEVEL, 25)
            column(COL_XP, 60)
            column(COL_CATCH_RATE_LOW, 60)
            column(COL_CATCH_RATE_HIGH, 250)
            column(COL_SPOT_TYPE, "bait")
        }

        // Salmon (lure, level 30)
        row("dbrows.fishing_salmon") {
            columnRSCM(COL_FISH_ITEM, "items.raw_salmon")
            columnRSCM(COL_TOOL, "items.fly_fishing_rod")
            columnRSCM(COL_BAIT, "items.feather")
            column(COL_LEVEL, 30)
            column(COL_XP, 70)
            column(COL_CATCH_RATE_LOW, 50)
            column(COL_CATCH_RATE_HIGH, 220)
            column(COL_SPOT_TYPE, "lure")
        }

        // Tuna (harpoon, level 35)
        row("dbrows.fishing_tuna") {
            columnRSCM(COL_FISH_ITEM, "items.raw_tuna")
            columnRSCM(COL_TOOL, "items.harpoon")
            column(COL_LEVEL, 35)
            column(COL_XP, 80)
            column(COL_CATCH_RATE_LOW, 30)
            column(COL_CATCH_RATE_HIGH, 200)
            column(COL_SPOT_TYPE, "harpoon")
        }

        // Lobster (cage, level 40)
        row("dbrows.fishing_lobster") {
            columnRSCM(COL_FISH_ITEM, "items.raw_lobster")
            columnRSCM(COL_TOOL, "items.lobster_pot")
            column(COL_LEVEL, 40)
            column(COL_XP, 90)
            column(COL_CATCH_RATE_LOW, 20)
            column(COL_CATCH_RATE_HIGH, 175)
            column(COL_SPOT_TYPE, "cage")
        }

        // Swordfish (harpoon, level 50)
        row("dbrows.fishing_swordfish") {
            columnRSCM(COL_FISH_ITEM, "items.raw_swordfish")
            columnRSCM(COL_TOOL, "items.harpoon")
            column(COL_LEVEL, 50)
            column(COL_XP, 100)
            column(COL_CATCH_RATE_LOW, 10)
            column(COL_CATCH_RATE_HIGH, 130)
            column(COL_SPOT_TYPE, "harpoon")
        }

        // --- Members Fish ---

        // Mackerel (big net, level 16)
        row("dbrows.fishing_mackerel") {
            columnRSCM(COL_FISH_ITEM, "items.raw_mackerel")
            columnRSCM(COL_TOOL, "items.big_fishing_net")
            column(COL_LEVEL, 16)
            column(COL_XP, 20)
            column(COL_CATCH_RATE_LOW, 100)
            column(COL_CATCH_RATE_HIGH, 350)
            column(COL_SPOT_TYPE, "big_net")
        }

        // Cod (big net, level 23)
        row("dbrows.fishing_cod") {
            columnRSCM(COL_FISH_ITEM, "items.raw_cod")
            columnRSCM(COL_TOOL, "items.big_fishing_net")
            column(COL_LEVEL, 23)
            column(COL_XP, 45)
            column(COL_CATCH_RATE_LOW, 70)
            column(COL_CATCH_RATE_HIGH, 280)
            column(COL_SPOT_TYPE, "big_net")
        }

        // Bass (big net, level 46)
        row("dbrows.fishing_bass") {
            columnRSCM(COL_FISH_ITEM, "items.raw_bass")
            columnRSCM(COL_TOOL, "items.big_fishing_net")
            column(COL_LEVEL, 46)
            column(COL_XP, 100)
            column(COL_CATCH_RATE_LOW, 20)
            column(COL_CATCH_RATE_HIGH, 150)
            column(COL_SPOT_TYPE, "big_net")
        }

        // Monkfish (small net, level 62)
        row("dbrows.fishing_monkfish") {
            columnRSCM(COL_FISH_ITEM, "items.raw_monkfish")
            columnRSCM(COL_TOOL, "items.small_fishing_net")
            column(COL_LEVEL, 62)
            column(COL_XP, 120)
            column(COL_CATCH_RATE_LOW, 10)
            column(COL_CATCH_RATE_HIGH, 100)
            column(COL_SPOT_TYPE, "net")
        }

        // Shark (harpoon, level 76)
        row("dbrows.fishing_shark") {
            columnRSCM(COL_FISH_ITEM, "items.raw_shark")
            columnRSCM(COL_TOOL, "items.harpoon")
            column(COL_LEVEL, 76)
            column(COL_XP, 110)
            column(COL_CATCH_RATE_LOW, 5)
            column(COL_CATCH_RATE_HIGH, 80)
            column(COL_SPOT_TYPE, "harpoon")
        }

        // Anglerfish (bait, level 82)
        row("dbrows.fishing_anglerfish") {
            columnRSCM(COL_FISH_ITEM, "items.raw_anglerfish")
            columnRSCM(COL_TOOL, "items.fishing_rod")
            columnRSCM(COL_BAIT, "items.fishing_bait")
            column(COL_LEVEL, 82)
            column(COL_XP, 120)
            column(COL_CATCH_RATE_LOW, 3)
            column(COL_CATCH_RATE_HIGH, 60)
            column(COL_SPOT_TYPE, "bait")
        }

        // Dark crab (cage, level 85)
        row("dbrows.fishing_dark_crab") {
            columnRSCM(COL_FISH_ITEM, "items.raw_dark_crab")
            columnRSCM(COL_TOOL, "items.lobster_pot")
            columnRSCM(COL_BAIT, "items.dark_fishing_bait")
            column(COL_LEVEL, 85)
            column(COL_XP, 130)
            column(COL_CATCH_RATE_LOW, 2)
            column(COL_CATCH_RATE_HIGH, 50)
            column(COL_SPOT_TYPE, "cage")
        }

        // Karambwan (vessel, level 65)
        row("dbrows.fishing_karambwan") {
            columnRSCM(COL_FISH_ITEM, "items.raw_karambwan")
            columnRSCM(COL_TOOL, "items.karambwan_vessel")
            columnRSCM(COL_BAIT, "items.raw_karambwanji")
            column(COL_LEVEL, 65)
            column(COL_XP, 105)
            column(COL_CATCH_RATE_LOW, 15)
            column(COL_CATCH_RATE_HIGH, 120)
            column(COL_SPOT_TYPE, "vessel")
        }

        // Lava eel (oily rod, level 53)
        row("dbrows.fishing_lava_eel") {
            columnRSCM(COL_FISH_ITEM, "items.raw_lava_eel")
            columnRSCM(COL_TOOL, "items.oily_fishing_rod")
            columnRSCM(COL_BAIT, "items.fishing_bait")
            column(COL_LEVEL, 53)
            column(COL_XP, 30)
            column(COL_CATCH_RATE_LOW, 30)
            column(COL_CATCH_RATE_HIGH, 200)
            column(COL_SPOT_TYPE, "bait")
        }

        // Infernal eel (oily rod, level 80)
        row("dbrows.fishing_infernal_eel") {
            columnRSCM(COL_FISH_ITEM, "items.infernal_eel")
            columnRSCM(COL_TOOL, "items.oily_fishing_rod")
            columnRSCM(COL_BAIT, "items.fishing_bait")
            column(COL_LEVEL, 80)
            column(COL_XP, 95)
            column(COL_CATCH_RATE_LOW, 8)
            column(COL_CATCH_RATE_HIGH, 70)
            column(COL_SPOT_TYPE, "bait")
        }

        // Sacred eel (rod, level 87)
        row("dbrows.fishing_sacred_eel") {
            columnRSCM(COL_FISH_ITEM, "items.sacred_eel")
            columnRSCM(COL_TOOL, "items.fishing_rod")
            columnRSCM(COL_BAIT, "items.fishing_bait")
            column(COL_LEVEL, 87)
            column(COL_XP, 105)
            column(COL_CATCH_RATE_LOW, 5)
            column(COL_CATCH_RATE_HIGH, 55)
            column(COL_SPOT_TYPE, "bait")
        }
    }

    fun tools() = dbTable("tables.fishing_tools", serverOnly = true) {
        column("item", TOOL_ITEM, VarType.OBJ)
        column("type", TOOL_TYPE, VarType.STRING)
        column("speed_mod", TOOL_SPEED_MOD, VarType.INT)
        column("animation", TOOL_ANIMATION, VarType.SEQ)

        row("dbrows.fishing_tool_small_net") {
            columnRSCM(TOOL_ITEM, "items.small_fishing_net")
            column(TOOL_TYPE, "net")
            column(TOOL_SPEED_MOD, 100)
            columnRSCM(TOOL_ANIMATION, "sequences.human_net_fishing")
        }

        row("dbrows.fishing_tool_fishing_rod") {
            columnRSCM(TOOL_ITEM, "items.fishing_rod")
            column(TOOL_TYPE, "bait")
            column(TOOL_SPEED_MOD, 100)
            columnRSCM(TOOL_ANIMATION, "sequences.human_rod_fishing")
        }

        row("dbrows.fishing_tool_fly_rod") {
            columnRSCM(TOOL_ITEM, "items.fly_fishing_rod")
            column(TOOL_TYPE, "lure")
            column(TOOL_SPEED_MOD, 100)
            columnRSCM(TOOL_ANIMATION, "sequences.human_lure_fishing")
        }

        row("dbrows.fishing_tool_harpoon") {
            columnRSCM(TOOL_ITEM, "items.harpoon")
            column(TOOL_TYPE, "harpoon")
            column(TOOL_SPEED_MOD, 100)
            columnRSCM(TOOL_ANIMATION, "sequences.human_harpoon_fishing")
        }

        row("dbrows.fishing_tool_lobster_pot") {
            columnRSCM(TOOL_ITEM, "items.lobster_pot")
            column(TOOL_TYPE, "cage")
            column(TOOL_SPEED_MOD, 100)
            columnRSCM(TOOL_ANIMATION, "sequences.human_cage_fishing")
        }

        row("dbrows.fishing_tool_big_net") {
            columnRSCM(TOOL_ITEM, "items.big_fishing_net")
            column(TOOL_TYPE, "big_net")
            column(TOOL_SPEED_MOD, 100)
            columnRSCM(TOOL_ANIMATION, "sequences.human_bignet_fishing")
        }

        row("dbrows.fishing_tool_dragon_harpoon") {
            columnRSCM(TOOL_ITEM, "items.dragon_harpoon")
            column(TOOL_TYPE, "harpoon")
            column(TOOL_SPEED_MOD, 80)
            columnRSCM(TOOL_ANIMATION, "sequences.human_harpoon_fishing")
        }

        row("dbrows.fishing_tool_infernal_harpoon") {
            columnRSCM(TOOL_ITEM, "items.infernal_harpoon")
            column(TOOL_TYPE, "harpoon")
            column(TOOL_SPEED_MOD, 80)
            columnRSCM(TOOL_ANIMATION, "sequences.human_harpoon_fishing")
        }

        row("dbrows.fishing_tool_crystal_harpoon") {
            columnRSCM(TOOL_ITEM, "items.crystal_harpoon")
            column(TOOL_TYPE, "harpoon")
            column(TOOL_SPEED_MOD, 65)
            columnRSCM(TOOL_ANIMATION, "sequences.human_harpoon_fishing")
        }

        row("dbrows.fishing_tool_barb_rod") {
            columnRSCM(TOOL_ITEM, "items.barbarian_rod")
            column(TOOL_TYPE, "barb")
            column(TOOL_SPEED_MOD, 100)
            columnRSCM(TOOL_ANIMATION, "sequences.human_rod_fishing")
        }

        row("dbrows.fishing_tool_karambwan_vessel") {
            columnRSCM(TOOL_ITEM, "items.karambwan_vessel")
            column(TOOL_TYPE, "vessel")
            column(TOOL_SPEED_MOD, 100)
            columnRSCM(TOOL_ANIMATION, "sequences.human_rod_fishing")
        }

        row("dbrows.fishing_tool_oily_rod") {
            columnRSCM(TOOL_ITEM, "items.oily_fishing_rod")
            column(TOOL_TYPE, "bait")
            column(TOOL_SPEED_MOD, 100)
            columnRSCM(TOOL_ANIMATION, "sequences.human_rod_fishing")
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :cache:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add cache/src/main/kotlin/org/alter/impl/skills/Fishing.kt
git commit -m "feat(fishing): add cache table definitions for fishing spots and tools"
```

---

### Task 2: Fishing Gamevals TOML

**Files:**
- Create: `content/src/main/resources/org/alter/skills/fishing/gamevals.toml`

- [ ] **Step 1: Create gamevals with table and dbrow IDs**

Use IDs in the 97000+ range to avoid conflicts with existing gamevals (mining uses 96600s, firemaking uses 96800s). Tables use 1240+ range.

```toml
[gamevals.tables]
fishing_spots=1240
fishing_tools=1241

[gamevals.dbrows]
fishing_shrimps=97001
fishing_sardine=97002
fishing_herring=97003
fishing_anchovies=97004
fishing_trout=97005
fishing_pike=97006
fishing_salmon=97007
fishing_tuna=97008
fishing_lobster=97009
fishing_swordfish=97010
fishing_mackerel=97011
fishing_cod=97012
fishing_bass=97013
fishing_monkfish=97014
fishing_shark=97015
fishing_anglerfish=97016
fishing_dark_crab=97017
fishing_karambwan=97018
fishing_lava_eel=97019
fishing_infernal_eel=97020
fishing_sacred_eel=97021
fishing_tool_small_net=97050
fishing_tool_fishing_rod=97051
fishing_tool_fly_rod=97052
fishing_tool_harpoon=97053
fishing_tool_lobster_pot=97054
fishing_tool_big_net=97055
fishing_tool_dragon_harpoon=97056
fishing_tool_infernal_harpoon=97057
fishing_tool_crystal_harpoon=97058
fishing_tool_barb_rod=97059
fishing_tool_karambwan_vessel=97060
fishing_tool_oily_rod=97061
```

- [ ] **Step 2: Run code generation to produce Row classes**

Run: `./gradlew :cache:build` then `./gradlew :content:compileKotlin`

Check that `content/src/main/kotlin/org/generated/tables/fishing/FishingSpotsRow.kt` and `FishingToolsRow.kt` were generated. If the TableGenerater doesn't auto-run, check if there's a specific Gradle task for code gen (look at how mining tables are generated).

**Important:** The exact Row class names are generated from the table name. `fishing_spots` → `FishingSpotsRow`, `fishing_tools` → `FishingToolsRow`. Verify the actual generated names before proceeding.

- [ ] **Step 3: Commit**

```bash
git add content/src/main/resources/org/alter/skills/fishing/gamevals.toml
git add content/src/main/kotlin/org/generated/tables/fishing/
git commit -m "feat(fishing): add gamevals and generated Row classes"
```

---

### Task 3: Fishing Event Definitions

**Files:**
- Create: `content/src/main/kotlin/org/alter/skills/fishing/FishObtainedEvent.kt`

- [ ] **Step 1: Create the FishObtainedEvent**

Follow the `RockOreObtainedEvent` pattern from mining:

```kotlin
package org.alter.skills.fishing

import org.alter.api.Skills
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.event.impl.SkillingActionCompletedGatheringEvent

/**
 * Fired when a player successfully catches a fish.
 * Used by enhancers (angler outfit, rada's blessing, spirit flakes) and
 * cross-cutting systems (clue bottle drops, collection log).
 */
class FishObtainedEvent(
    override val player: Player,
    spotNpc: Npc,
    val fishItem: Int,
    xp: Double,
) : SkillingActionCompletedGatheringEvent(
    player = player,
    skill = Skills.FISHING,
    actionObject = spotNpc,
    experienceGained = xp,
    resourceId = fishItem,
    amountGathered = 1,
)
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :content:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add content/src/main/kotlin/org/alter/skills/fishing/FishObtainedEvent.kt
git commit -m "feat(fishing): add FishObtainedEvent extending SkillingActionCompletedGatheringEvent"
```

---

### Task 4: Fishing Plugin — Core Logic

**Files:**
- Create: `content/src/main/kotlin/org/alter/skills/fishing/FishingPlugin.kt`

- [ ] **Step 1: Create the main FishingPlugin**

This is the core plugin. It registers `onNpcOption` handlers for fishing spot NPCs, validates tools/bait/levels, and runs the main fishing loop. Follow the MiningPlugin pattern with `repeatWhile`.

```kotlin
package org.alter.skills.fishing

import dev.openrune.ServerCacheManager.getItem
import org.alter.api.Skills
import org.alter.api.ext.filterableMessage
import org.alter.api.ext.message
import org.alter.api.success
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.PluginEvent
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
// NOTE: Import the actual generated Row class name — verify after code gen
// import org.generated.tables.fishing.FishingSpotsRow
// import org.generated.tables.fishing.FishingToolsRow

class FishingPlugin : PluginEvent() {

    companion object {
        // Map of spot type -> list of fish rows, sorted by level descending
        // (higher level fish are attempted first at multi-fish spots)
        lateinit var fishBySpotType: Map<String, List<Any>> // Replace Any with actual Row type
        lateinit var toolsByType: Map<String, List<Any>>    // Replace Any with actual Row type
    }

    override fun init() {
        // TODO: Replace with actual generated Row class names after code gen
        // val allSpots = FishingSpotsRow.all()
        // val allTools = FishingToolsRow.all()
        // fishBySpotType = allSpots.groupBy { it.spotType }.mapValues { it.value.sortedByDescending { f -> f.level } }
        // toolsByType = allTools.groupBy { it.type }

        // Register NPC options for each fishing spot type
        // Net spots: "Net" option
        // Bait spots: "Bait" option
        // Lure spots: "Lure" option
        // Cage spots: "Cage" option
        // Harpoon spots: "Harpoon" option

        // Example registration pattern (fill in actual NPC RSCM IDs):
        // onNpcOption("npcs.fishing_spot_net_bait", "Net") {
        //     player.queue { fishLoop(player, npc, "net") }
        // }
        // onNpcOption("npcs.fishing_spot_net_bait", "Bait") {
        //     player.queue { fishLoop(player, npc, "bait") }
        // }
    }

    // Main fishing loop — follows mining's repeatWhile pattern
    // suspend fun QueueTask.fishLoop(player: Player, spot: Npc, spotType: String) {
    //     val fishOptions = fishBySpotType[spotType] ?: return
    //     val tool = findBestTool(player, spotType) ?: run {
    //         player.message("You need a ${getToolNameForType(spotType)} to fish here.")
    //         return
    //     }
    //
    //     player.filterableMessage("You cast out your ${getItem(tool.item)}...")
    //     player.animate(tool.animation)
    //
    //     repeatWhile(delay = 5, immediate = false, canRepeat = {
    //         player.inventory.freeSlotCount > 0 && hasTool(player, spotType) && hasBait(player, fishOptions)
    //     }) {
    //         player.animate(tool.animation)
    //
    //         // Try to catch fish, highest level first
    //         for (fish in fishOptions) {
    //             if (player.getSkills().getCurrentLevel(Skills.FISHING) < fish.level) continue
    //             if (fish.bait != null && !player.inventory.contains(fish.bait)) continue
    //
    //             if (success(fish.catchRateLow, fish.catchRateHigh, player.getSkills().getCurrentLevel(Skills.FISHING))) {
    //                 // Consume bait
    //                 if (fish.bait != null) player.inventory.remove(fish.bait)
    //
    //                 // Give fish
    //                 player.inventory.add(fish.fishItem)
    //                 player.addXp(Skills.FISHING, fish.xp.toDouble())
    //                 player.filterableMessage("You catch some ${getItem(fish.fishItem)}.")
    //
    //                 // Post event for enhancers
    //                 FishObtainedEvent(player, spot, fish.fishItem, fish.xp.toDouble()).post()
    //                 break
    //             }
    //         }
    //     }
    //
    //     player.animate(RSCM.NONE)
    //     if (player.inventory.freeSlotCount == 0) {
    //         player.message("You can't carry any more fish.")
    //     }
    // }
}
```

**IMPORTANT NOTE:** This task contains TODO placeholders because the exact generated Row class names and property names must be verified after Task 2 code gen completes. The implementer must:
1. Check the generated Row class names in `content/src/main/kotlin/org/generated/tables/fishing/`
2. Check the generated property names (e.g., `spotNpc`, `fishItem`, `level`, etc.)
3. Replace all `Any` types and commented-out code with actual types
4. Look up actual fishing spot NPC RSCM IDs from the cache data (e.g., `npcs.fishing_spot_1`, `npcs.fishing_spot_2`)

- [ ] **Step 2: Uncomment and wire up with actual generated types**

After verifying Row class names from Task 2, replace all TODO/commented sections with real code.

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :content:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add content/src/main/kotlin/org/alter/skills/fishing/FishingPlugin.kt
git commit -m "feat(fishing): add core FishingPlugin with fishing loop"
```

---

### Task 5: Fishing Enhancers

**Files:**
- Create: `content/src/main/kotlin/org/alter/skills/fishing/FishingEnhancers.kt`

- [ ] **Step 1: Create FishingEnhancers plugin**

Listens for `FishObtainedEvent` to apply outfit XP bonuses and double-fish mechanics.

```kotlin
package org.alter.skills.fishing

import org.alter.api.Skills
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent

class FishingEnhancers : PluginEvent() {

    companion object {
        // Angler outfit pieces and their individual XP bonuses
        private val ANGLER_PIECES = mapOf(
            "items.angler_hat" to 0.004,
            "items.angler_top" to 0.008,
            "items.angler_waders" to 0.006,
            "items.angler_boots" to 0.002,
        )
        private const val ANGLER_SET_BONUS = 0.005
        private const val ANGLER_FULL_BONUS = 0.025 // total with set bonus
    }

    override fun init() {
        on<FishObtainedEvent> {
            where { true }
            then {
                // Angler outfit XP bonus
                val xpBonus = getAnglerBonus(player)
                if (xpBonus > 0.0) {
                    val bonusXp = (experienceGained ?: 0.0) * xpBonus
                    player.addXp(Skills.FISHING, bonusXp)
                }

                // Rada's blessing double fish
                // TODO: Check player equipment for rada's blessing tiers
                // val radaChance = getRadaChance(player)
                // if (radaChance > 0.0 && Math.random() < radaChance) {
                //     player.inventory.add(fishItem)
                // }

                // Spirit flakes: 50% double fish
                // if (player.inventory.contains("items.spirit_flakes".asRSCM())) {
                //     if (Math.random() < 0.5) {
                //         player.inventory.add(fishItem)
                //     }
                //     player.inventory.remove("items.spirit_flakes".asRSCM())
                // }
            }
        }
    }

    private fun getAnglerBonus(player: Player): Double {
        var bonus = 0.0
        var piecesWorn = 0
        for ((piece, xp) in ANGLER_PIECES) {
            if (player.equipment.contains(piece)) {
                bonus += xp
                piecesWorn++
            }
        }
        if (piecesWorn == ANGLER_PIECES.size) {
            bonus += ANGLER_SET_BONUS
        }
        return bonus
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :content:compileKotlin`

- [ ] **Step 3: Commit**

```bash
git add content/src/main/kotlin/org/alter/skills/fishing/FishingEnhancers.kt
git commit -m "feat(fishing): add FishingEnhancers with angler outfit XP bonus"
```

---

### Task 6: Full Build Verification

- [ ] **Step 1: Run full project build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Start the server and verify fishing spots load**

Run: Start the game server via `Launcher.main()`. Check console output for any errors related to fishing table loading. Verify the fishing Row classes are loadable.

- [ ] **Step 3: Commit any fixes**

If any compilation or runtime issues were found, fix and commit.

---

## Phase 2: Cooking Skill System

> Follows the exact same 3-layer pattern as Phase 1. Key differences:
> - Uses `on<ItemOnObject>` (not `onNpcOption`) since cooking uses range/fire objects
> - Has burn rate calculation logic
> - Integrates with existing `FoodTable.kt` for consumable food definitions

### Task 7: Cooking Cache Table Definitions

**Files:**
- Create: `cache/src/main/kotlin/org/alter/impl/skills/Cooking.kt`

- [ ] **Step 1: Create the Cooking object with cooking_recipes table**

Follow the Firemaking.kt pattern (simpler than Mining). Define columns for raw item, cooked item, burnt item, level, XP, burn stop levels, and cooking method.

Include all fish from Phase 1 plus meat, bread, pies, pizza, cake, stew, wine. See spec Section 3.2 for the complete item list with exact levels and XP values.

Column constants:
```kotlin
const val COL_RAW_ITEM = 0
const val COL_COOKED_ITEM = 1
const val COL_BURNT_ITEM = 2
const val COL_LEVEL = 3
const val COL_XP = 4
const val COL_BURN_STOP_FIRE = 5
const val COL_BURN_STOP_RANGE = 6
const val COL_METHOD = 7  // 0=both, 1=range only, 2=spit only
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :cache:compileKotlin`

- [ ] **Step 3: Commit**

### Task 8: Cooking Gamevals TOML

**Files:**
- Create: `content/src/main/resources/org/alter/skills/cooking/gamevals.toml`

- [ ] **Step 1: Create gamevals** — Use table ID 1242, dbrow IDs in 97100+ range
- [ ] **Step 2: Run code gen** — `./gradlew :cache:build && ./gradlew :content:compileKotlin`
- [ ] **Step 3: Commit**

### Task 9: Cooking Events

**Files:**
- Create: `content/src/main/kotlin/org/alter/skills/cooking/CookingEvents.kt`

- [ ] **Step 1: Create FoodCookedEvent**

```kotlin
package org.alter.skills.cooking

import org.alter.api.Skills
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.event.impl.SkillingActionCompletedEvent

class FoodCookedEvent(
    override val player: Player,
    val rawItem: Int,
    val cookedItem: Int,
    val isFire: Boolean,
) : SkillingActionCompletedEvent(
    player = player,
    skill = Skills.COOKING,
    actionObject = null,
)
```

- [ ] **Step 2: Verify & commit**

### Task 10: Cooking Burn Rates

**Files:**
- Create: `content/src/main/kotlin/org/alter/skills/cooking/CookingBurnRates.kt`

- [ ] **Step 1: Implement burn rate calculation**

Contains `shouldBurn(player, recipe, isFire)` function with:
- Cooking cape check (never burn)
- Gauntlet overrides for lobster (74→64), swordfish (86→81), monkfish (92→87), shark (99→94)
- Lumbridge range bonus check (tile-based + Cook's Assistant quest completion)
- Range vs fire burn stop level selection
- Linear burn chance formula

See spec Section 3.3 for the exact `shouldBurn` implementation.

- [ ] **Step 2: Verify & commit**

### Task 11: Cooking Plugin

**Files:**
- Create: `content/src/main/kotlin/org/alter/skills/cooking/CookingPlugin.kt`

- [ ] **Step 1: Implement CookingPlugin**

Uses `on<ItemOnObject>` to register raw food on range/fire objects. Handler flow:
1. Look up recipe from raw item ID
2. Check cooking level
3. Check cooking method compatibility
4. Open cooking interface (item icon, quantity)
5. Enter cook loop: `repeatWhile(delay = 4, immediate = false, ...)` — animate, burn check, produce cooked/burnt, award XP, post `FoodCookedEvent`

Wine special case: Grapes + jug of water → 12-tick ferment timer.

See spec Section 3.3 for full architecture.

- [ ] **Step 2: Full build verification** — `./gradlew build`
- [ ] **Step 3: Commit**

---

## Phase 3: Crafting Skill System

> Six subsystem plugins, each following the same pattern. All use `on<ItemOnObject>` or `onItemOnItem` with `SatisfyType.ANY`.

### Task 12: Crafting Cache Table Definitions

**Files:**
- Create: `cache/src/main/kotlin/org/alter/impl/skills/Crafting.kt`

- [ ] **Step 1: Create all 7 crafting tables**

Define `crafting_spinning`, `crafting_pottery`, `crafting_leather`, `crafting_gems`, `crafting_jewelry_gold`, `crafting_jewelry_silver`, `crafting_glass` tables. See spec Section 4.1 for column definitions and Section 4.2 for all item data.

- [ ] **Step 2: Verify compilation** — `./gradlew :cache:compileKotlin`
- [ ] **Step 3: Commit**

### Task 13: Crafting Gamevals TOML

**Files:**
- Create: `content/src/main/resources/org/alter/skills/crafting/gamevals.toml`

- [ ] **Step 1: Create gamevals** — Tables 1243-1249, dbrows in 97200+ range
- [ ] **Step 2: Run code gen**
- [ ] **Step 3: Commit**

### Task 14: Spinning Plugin

**Files:**
- Create: `content/src/main/kotlin/org/alter/skills/crafting/SpinningPlugin.kt`

- [ ] **Step 1: Implement SpinningPlugin**

`on<ItemOnObject>` for spinning wheel objects. Open interface to select item if multiple options. 3-tick loop: animate, consume input, produce output, award XP. See spec Section 4.2.1 for items.

- [ ] **Step 2: Verify & commit**

### Task 15: Pottery Plugin

**Files:**
- Create: `content/src/main/kotlin/org/alter/skills/crafting/PotteryPlugin.kt`

- [ ] **Step 1: Implement PotteryPlugin** — Two-step: shape on wheel, fire in oven. See spec Section 4.2.2.
- [ ] **Step 2: Verify & commit**

### Task 16: Leather Plugin

**Files:**
- Create: `content/src/main/kotlin/org/alter/skills/crafting/LeatherPlugin.kt`

- [ ] **Step 1: Implement LeatherPlugin** — `onItemOnItem` needle + leather with `SatisfyType.ANY`. Interface for item selection. See spec Section 4.2.3.
- [ ] **Step 2: Verify & commit**

### Task 17: Gem Cutting Plugin

**Files:**
- Create: `content/src/main/kotlin/org/alter/skills/crafting/GemCuttingPlugin.kt`

- [ ] **Step 1: Implement GemCuttingPlugin** — `onItemOnItem` chisel + gem. No interface. Semi-precious crush chance. See spec Section 4.2.4.
- [ ] **Step 2: Verify & commit**

### Task 18: Jewelry Plugin

**Files:**
- Create: `content/src/main/kotlin/org/alter/skills/crafting/JewelryPlugin.kt`

- [ ] **Step 1: Implement JewelryPlugin** — `on<ItemOnObject>` bar on furnace. Interface for jewelry selection. Mould check. See spec Section 4.2.5.
- [ ] **Step 2: Verify & commit**

### Task 19: Glass Blowing Plugin

**Files:**
- Create: `content/src/main/kotlin/org/alter/skills/crafting/GlassBlowingPlugin.kt`

- [ ] **Step 1: Implement GlassBlowingPlugin** — `onItemOnItem` pipe + molten glass. Interface. See spec Section 4.2.6.
- [ ] **Step 2: Full build verification** — `./gradlew build`
- [ ] **Step 3: Commit**

---

## Phase 4: Global Interaction Plugins

### Task 20: Altar Plugin

**Files:**
- Create: `content/src/main/kotlin/org/alter/objects/AltarPlugin.kt`

- [ ] **Step 1: Implement AltarPlugin**

```kotlin
package org.alter.objects

import org.alter.api.Skills
import org.alter.api.ext.message
import org.alter.game.pluginnew.PluginEvent

class AltarPlugin : PluginEvent() {
    override fun init() {
        // Register for all altar objects by option "Pray-at" or "Pray"
        // Use onObjectOption for known altar object IDs
        // Handler: restore prayer to max, animate, message
        onObjectOption("objects.altar", "Pray-at", "Pray") {
            val maxPrayer = player.getSkills().getBaseLevel(Skills.PRAYER)
            player.getSkills().setCurrentLevel(Skills.PRAYER, maxPrayer)
            player.animate("sequences.human_burybone")
            player.message("You recharge your Prayer points.")
        }
    }
}
```

Note: Look up actual altar object RSCM IDs from cache. There may be multiple altar variants.

- [ ] **Step 2: Verify & commit**

### Task 21: Dairy Cow Plugin

**Files:**
- Create: `content/src/main/kotlin/org/alter/objects/DairyCowPlugin.kt`

- [ ] **Step 1: Implement DairyCowPlugin** — `onObjectOption` "Milk" on dairy cow objects. Check for empty bucket → give bucket of milk.
- [ ] **Step 2: Verify & commit**

### Task 22: Sheep Shearing Plugin

**Files:**
- Create: `content/src/main/kotlin/org/alter/interactions/SheepShearingPlugin.kt`

- [ ] **Step 1: Implement SheepShearingPlugin** — `on<ItemOnNpcEvent>` shears on sheep + `onNpcOption` "Shear". Give wool, transform sheep to shorn variant, schedule regrowth timer (~100 ticks).
- [ ] **Step 2: Verify & commit**

### Task 23: Windmill Plugin

**Files:**
- Create: `content/src/main/kotlin/org/alter/objects/WindmillPlugin.kt`

- [ ] **Step 1: Implement WindmillPlugin** — Three objects: hopper (`on<ItemOnObject>` grain), controls (`onObjectOption` "Operate"), flour bin (`onObjectOption` "Empty" + pot check). Track state via player varbit.
- [ ] **Step 2: Full build verification** — `./gradlew build`
- [ ] **Step 3: Commit**

---

## Phase 5: Lumbridge World Population

### Task 24: Lumbridge Spawns — Cows, Chickens, Fishing Spots, Items

**Files:**
- Create: `content/src/main/kotlin/org/alter/areas/lumbridge/spawns/CowFieldSpawns.kt`
- Create: `content/src/main/kotlin/org/alter/areas/lumbridge/spawns/FishingSpotSpawns.kt`
- Modify: `content/src/main/kotlin/org/alter/areas/lumbridge/spawns/SpawnPlugin.kt` — add missing item spawns

- [ ] **Step 1: Create CowFieldSpawns** — Spawn 8-10 cows, 2-3 calves at (3253-3265, 3255-3300). Spawn 5-6 chickens at (3185-3195, 3275-3280).
- [ ] **Step 2: Create FishingSpotSpawns** — Spawn net/bait fishing spot NPC at swamp area. Spawn lure/bait fishing spot NPC at River Lum.
- [ ] **Step 3: Add item spawns** — Eggs at chicken coops, cabbage/onion near general store.
- [ ] **Step 4: Verify & commit**

### Task 25: Lumbridge NPC — Duke Horacio

**Files:**
- Create: `content/src/main/kotlin/org/alter/areas/lumbridge/npcs/DukeHoracioPlugin.kt`

- [ ] **Step 1: Remove Duke spawn from ChatSpawnsPlugin.kt** — The Duke is already spawned in `content/src/main/kotlin/org/alter/areas/lumbridge/spawns/ChatSpawnsPlugin.kt` as `"npcs.duke_of_lumbridge"` at (3212, 3220, height=1). Remove that spawn line since DukeHoracioPlugin will handle it.

- [ ] **Step 2: Implement DukeHoracioPlugin** — Spawn using `"npcs.duke_of_lumbridge"` (the correct RSCM name) at (3212, 3220, height=1). Talk-to dialogue with exact OSRS text. Fetch exact dialogue from the OSRS wiki Duke Horacio dialogue page. Quest state branches for Rune Mysteries. Anti-dragon shield on request.
- [ ] **Step 2: Verify & commit**

### Task 26: Lumbridge NPC — Father Urhney

**Files:**
- Create: `content/src/main/kotlin/org/alter/areas/lumbridge/npcs/FatherUrhneyPlugin.kt`

- [ ] **Step 1: Implement FatherUrhneyPlugin** — Spawn at (3147, 3175). Grumpy hermit dialogue. Gives Ghostspeak amulet for Restless Ghost quest. Replacement amulet if lost.
- [ ] **Step 2: Verify & commit**

### Task 27: Lumbridge NPC — Fred the Farmer

**Files:**
- Create: `content/src/main/kotlin/org/alter/areas/lumbridge/npcs/FredTheFarmerPlugin.kt`

- [ ] **Step 1: Implement FredTheFarmerPlugin** — Spawn at (3190, 3273), walkRadius=5. Sheep Shearer quest start dialogue. Needs 20 balls of wool.
- [ ] **Step 2: Verify & commit**

### Task 28: Lumbridge NPC — Gillie Groats

**Files:**
- Create: `content/src/main/kotlin/org/alter/areas/lumbridge/npcs/GillieGroatsPlugin.kt`

- [ ] **Step 1: Implement GillieGroatsPlugin** — Spawn at (3253, 3270), walkRadius=3. Teaches cow milking. Exact OSRS dialogue.
- [ ] **Step 2: Verify & commit**

### Task 29: Lumbridge NPC — Millie Miller

**Files:**
- Create: `content/src/main/kotlin/org/alter/areas/lumbridge/npcs/MillieMillerPlugin.kt`

- [ ] **Step 1: Implement MillieMillerPlugin** — Spawn at (3230, 3318), walkRadius=2. Teaches flour making.
- [ ] **Step 2: Verify & commit**

### Task 30: Lumbridge NPC — Sigmund & Veos

**Files:**
- Create: `content/src/main/kotlin/org/alter/areas/lumbridge/npcs/SigmundPlugin.kt`
- Create: `content/src/main/kotlin/org/alter/areas/lumbridge/npcs/VeosPlugin.kt`

- [ ] **Step 1: Implement SigmundPlugin** — Spawn at (3210, 3222, height=1), walkRadius=2. Duke's advisor. Lost Tribe quest hook. Exact OSRS dialogue.
- [ ] **Step 2: Implement VeosPlugin** — Spawn at (3228, 3241), walkRadius=0. X Marks the Spot quest hook. Passage to Kourend.
- [ ] **Step 3: Verify & commit**

### Task 31: Lumbridge NPC — Perdu, Adventurer Jon, Arthur

**Files:**
- Create: `content/src/main/kotlin/org/alter/areas/lumbridge/npcs/PerduPlugin.kt`
- Create: `content/src/main/kotlin/org/alter/areas/lumbridge/npcs/AdventurerJonPlugin.kt`
- Create: `content/src/main/kotlin/org/alter/areas/lumbridge/npcs/ArthurClueHunterPlugin.kt`

- [ ] **Step 1: Implement PerduPlugin** — Spawn at (3229, 3218). Lost item reclamation shop.
- [ ] **Step 2: Implement AdventurerJonPlugin** — Spawn at (3234, 3224), walkRadius=3. Adventure paths guide.
- [ ] **Step 3: Implement ArthurClueHunterPlugin** — Spawn at (3209, 3214). Clue scroll tutorial.
- [ ] **Step 4: Verify & commit**

### Task 32: Father Aereck — Restless Ghost Dialogue

**Files:**
- Modify: `content/src/main/kotlin/org/alter/areas/lumbridge/npcs/` (find existing Aereck spawn/plugin in `ChatSpawnsPlugin.kt`)

- [ ] **Step 1: Add Restless Ghost quest dialogue branch** — Father Aereck already spawned. Add quest-start conversation with state-aware branching.
- [ ] **Step 2: Verify & commit**

### Task 33: Final Build & Verification

- [ ] **Step 1: Full build** — `./gradlew build`
- [ ] **Step 2: Start server, log in, walk around Lumbridge** — Verify all NPCs are spawned, dialogues work, fishing spots appear, cooking range works, spinning wheel works, cow milking works, sheep shearing works.
- [ ] **Step 3: Final commit with any fixes**

---

## Summary

| Phase | Tasks | Description |
|-------|-------|-------------|
| 1 | 1-6 | Fishing skill — data layer, events, plugin, enhancers |
| 2 | 7-11 | Cooking skill — data layer, burn rates, plugin |
| 3 | 12-19 | Crafting skill — data layer, 6 subsystem plugins |
| 4 | 20-23 | Global interactions — altar, dairy cow, sheep, windmill |
| 5 | 24-33 | Lumbridge population — spawns, 10+ NPCs, dialogue |
