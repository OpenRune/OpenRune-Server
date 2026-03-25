# Corrupted KBD Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a 3-phase custom KBD boss with corruption mechanics, replacing the normal KBD spawn.

**Architecture:** Single `PluginEvent` class following the new plugin pattern (like `SpinningPlugin`, `BankPlugin`). Phase tracking via NPC attribute. Uses existing combat API from `game-plugins/src/main/kotlin/org/alter/plugins/content/combat/` for hit formulas, projectiles, and combat preparation.

**Tech Stack:** Kotlin, PluginEvent system, RSCM for NPC/animation IDs, existing combat framework.

**Spec:** `docs/superpowers/specs/2026-03-25-corrupted-kbd-design.md`

**Testing note:** No unit tests. Verification is compile-check + in-game testing. Use `::npc` command to spawn for quick testing.

**Key reference file:** `game-plugins/src/main/kotlin/org/alter/plugins/content/npcs/kbd/KbdCombatPlugin.kt` — the existing KBD combat logic. Read this first.

---

## File Structure

| File | Responsibility |
|------|----------------|
| `content/src/main/kotlin/org/alter/npcs/bosses/CorruptedKbdPlugin.kt` | Create — combat def, spawn, phase system, all attacks, drops |

---

## Key API Patterns

These are from `KbdCombatPlugin.kt` and the combat framework:

```kotlin
// Combat event registration (legacy KotlinPlugin style uses onNpcCombat)
// New PluginEvent style — check how NPC combat events are registered.
// May need to use on<NpcCombatEvent> or similar.

// NPC combat loop pattern (from KBD):
npc.queue {
    var target = npc.getCombatTarget() ?: return@queue
    while (npc.canEngageCombat(target)) {
        npc.facePawn(target)
        if (npc.moveToAttackRange(it, target, distance, projectile) && npc.isAttackDelayReady()) {
            // do attack
            npc.postAttackLogic(target)
        }
        it.wait(1)
        target = npc.getCombatTarget() ?: break
    }
    npc.resetFacePawn()
    npc.removeCombatTarget()
}

// Attack preparation:
npc.prepareAttack(CombatClass.MELEE, CombatStyle.STAB, AttackStyle.ACCURATE)
npc.animate("sequences.dragon_head_attack")

// Hit delivery:
target.hit(damage, type = HitType.HIT, delay = 1)  // simple
npc.dealHit(target, formula = DragonfireFormula(maxHit = 65), delay = N)  // formula-based

// Projectile:
val proj = npc.createProjectile(target, gfx = 393, startHeight = 43, endHeight = 31, delay = 51, angle = 15, steepness = 127)
world.spawn(proj)

// Poison/freeze:
target.poison(initialDamage = 8) { ... }
target.freeze(cycles = 6) { ... }

// Combat def:
setCombatDef("npcs.king_dragon") { stats { ... } bonuses { ... } anims { ... } aggro { ... } }

// Spawn:
spawnNpc("npcs.king_dragon", x = 2274, z = 4698, walkRadius = 5)

// NPC attributes:
val PHASE_ATTR = AttributeKey<Int>()
npc.attr[PHASE_ATTR] = 1
```

**IMPORTANT:** The KBD uses legacy `KotlinPlugin` style. The new `PluginEvent` system may register NPC combat differently. Before implementing, read how NPC combat events work in the new plugin system. Search for `onNpcCombat` or `NpcCombatEvent` in the codebase. If the new system doesn't support NPC combat registration, use the legacy `KotlinPlugin` pattern instead.

---

## Phase 1: Base Boss (Combat Loop + Stats)

### Task 1: Create CorruptedKbdPlugin with combat def and spawn

**Files:**
- Create: `content/src/main/kotlin/org/alter/npcs/bosses/CorruptedKbdPlugin.kt`

- [ ] **Step 1: Determine which plugin system to use**

Read these files to understand how NPC combat is registered:
- `game-plugins/src/main/kotlin/org/alter/plugins/content/npcs/kbd/KbdCombatPlugin.kt` (legacy pattern)
- `game-plugins/src/main/kotlin/org/alter/plugins/content/npcs/kbd/KbdConfigsPlugin.kt` (combat def + spawn)
- Search for `onNpcCombat` in `game-server/src/main/kotlin/org/alter/game/plugin/` to find how it's registered
- Search for `NpcCombatEvent` in `game-server/src/main/kotlin/org/alter/game/pluginnew/` to check if the new system supports it

If the new `PluginEvent` system supports NPC combat, use it. If not, use `KotlinPlugin` like KBD does.

- [ ] **Step 2: Create the plugin file with combat def and spawn**

Use the pattern from `KbdConfigsPlugin.kt`. Override with corrupted stats:

```kotlin
// Combat def for npcs.king_dragon (overrides existing)
setCombatDef("npcs.king_dragon") {
    species {
        +NpcSpecies.DRACONIC
        +NpcSpecies.BASIC_DRAGON
    }
    configs {
        attackSpeed = 3
        respawnDelay = 50
    }
    aggro {
        radius = 16
        searchDelay = 1
    }
    stats {
        hitpoints = 600
        attack = 320
        strength = 300
        defence = 280
        magic = 300
    }
    bonuses {
        defenceStab = 70
        defenceSlash = 90
        defenceCrush = 90
        defenceMagic = 80
        defenceRanged = 70
    }
    anims {
        block = "sequences.dragon_block"
        death = "sequences.dragon_death"
    }
}

// Spawn at KBD lair
setMultiCombatRegion(region = 9033)
spawnNpc("npcs.king_dragon", x = 2274, z = 4698, walkRadius = 5)
```

- [ ] **Step 3: Add the basic combat loop (Phase 1 attacks only)**

Same as KBD but with higher max melee hit (38) and guaranteed poison:

```kotlin
// Phase attribute
val PHASE = AttributeKey<Int>()

fun Npc.currentPhase(): Int {
    val hpPercent = (getCurrentHp().toDouble() / getMaxHp()) * 100
    return when {
        hpPercent > 50 -> 1
        hpPercent > 20 -> 2
        else -> 3
    }
}

// In combat loop:
// melee max hit = 38
// poison breath: always poisons (remove the 1/6 chance check)
```

- [ ] **Step 4: Compile check**

Run: `gradle :content:compileKotlin` (or `:game-plugins:compileKotlin` if using legacy plugin)
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: In-game test**

Teleport to KBD lair. The corrupted KBD should:
- Have 600 HP
- Hit harder (up to 38 melee)
- Always poison on poison breath
- Basic Phase 1 combat works

- [ ] **Step 6: Commit**

```bash
git add content/src/main/kotlin/org/alter/npcs/bosses/CorruptedKbdPlugin.kt
git commit -m "feat(boss): add Corrupted KBD with Phase 1 combat"
```

---

## Phase 2: Corruption Mechanics

### Task 2: Add Phase 2 — Corruption Tiles + Splash Dragonfire

**Files:**
- Modify: `content/src/main/kotlin/org/alter/npcs/bosses/CorruptedKbdPlugin.kt`

- [ ] **Step 1: Add corruption tile mechanic**

When `currentPhase() >= 2`, every 10 ticks during combat:

```kotlin
// Track last corruption tick
val LAST_CORRUPTION_TICK = AttributeKey<Int>()

fun Npc.spawnCorruptionTiles(world: World) {
    val centre = tile
    repeat(world.random(3, 5)) {
        val offsetX = world.random(-3, 3)
        val offsetZ = world.random(-3, 3)
        val corruptTile = Tile(centre.x + offsetX, centre.z + offsetZ, centre.height)
        // Spawn a purple ground graphic at corruptTile
        // Use an existing dark/shadow spotanim
        world.spawn(AreaSound(...)) // or TileGraphic
        // Schedule damage + removal after 8 ticks
    }
}
```

For the corruption tile damage: check how the game handles area-of-effect ground effects. Options:
- Spawn a `GroundItem` or `TileGraphic` and check player positions each tick
- Use a timer on the NPC that checks nearby player positions
- Look for existing AoE ground damage patterns in the codebase

The simplest approach: track corruption tile positions in a list on the NPC, check each combat tick if any player is standing on one, deal damage.

- [ ] **Step 2: Add splash dragonfire**

When `currentPhase() >= 2`, fire breath hits 3x3:

```kotlin
fun Npc.splashFireAttack(target: Pawn) {
    // Normal fire attack on primary target (full damage)
    fireAttack(this, target)

    // Hit nearby players in 3x3 area for 50% damage
    val centre = target.tile
    world.players.filter { player ->
        player != target &&
        player.tile.isWithinRadius(centre, 1) &&  // 3x3 = radius 1
        player.getCurrentHp() > 0
    }.forEach { nearby ->
        val halfDamage = world.random(1, 32)  // ~50% of max 65
        nearby.hit(halfDamage, type = HitType.HIT, delay = 2)
    }
}
```

- [ ] **Step 3: Add phase transition message**

When phase changes from 1 to 2:
```kotlin
// In combat loop, track previous phase
if (previousPhase == 1 && currentPhase() == 2) {
    animate("sequences.cerberus_howl")  // roar animation
    // Message all players in area
    world.players.filter { it.tile.regionId == 9033 }.forEach {
        it.message("The dragon's corruption intensifies!")
    }
}
```

- [ ] **Step 4: Compile check**

Run: `gradle :content:compileKotlin`

- [ ] **Step 5: In-game test**

Fight the KBD down to 50% HP:
- Purple tiles should appear periodically
- Standing on them should deal damage
- Fire breath should splash in 3x3 area
- Phase transition message appears

- [ ] **Step 6: Commit**

```bash
git commit -m "feat(boss): add Phase 2 corruption tiles and splash dragonfire"
```

---

## Phase 3: Enrage Mode

### Task 3: Add Phase 3 — Shadow Burst + Minions

**Files:**
- Modify: `content/src/main/kotlin/org/alter/npcs/bosses/CorruptedKbdPlugin.kt`

- [ ] **Step 1: Increase attack speed in Phase 3**

When `currentPhase() == 3`, use attack speed 2 instead of 3. Modify the combat loop's attack delay check or the NPC's attack speed dynamically:

```kotlin
if (currentPhase() == 3) {
    // Override attack speed to 2
    combatDef.attackSpeed = 2  // or equivalent method
}
```

Check how `attackSpeed` is read during combat — it may be on the combat def or on the NPC directly. Adapt accordingly.

- [ ] **Step 2: Add Shadow Burst**

Every 15 ticks in Phase 3, AoE attack on all players:

```kotlin
val LAST_BURST_TICK = AttributeKey<Int>()

fun Npc.shadowBurst(world: World) {
    animate("sequences.cerberus_special_attack_spray")

    world.players.filter {
        it.tile.regionId == 9033 && it.getCurrentHp() > 0
    }.forEach { player ->
        // Blockable by Protect from Magic
        val damage = if (player.ppisPrayingMagic()) 0
                     else world.random(20, 30)
        player.graphic("spotanims.cerberus_special_attack_flame")
        player.hit(damage, type = HitType.HIT, delay = 1)
    }
}
```

Check how prayer protection is verified — search for `isPrayingMagic`, `Prayers.PROTECT_MAGIC`, or similar in the combat code.

- [ ] **Step 3: Add minion spawn on Phase 3 entry**

Spawn 2 small dragons once when entering Phase 3:

```kotlin
val MINIONS_SPAWNED = AttributeKey<Boolean>()

fun Npc.spawnMinions(world: World) {
    if (attr[MINIONS_SPAWNED] == true) return
    attr[MINIONS_SPAWNED] = true

    // Find a suitable small dragon NPC ID — search gamevals for dragon NPCs
    // e.g. "npcs.baby_black_dragon" or similar
    repeat(2) { i ->
        val offset = if (i == 0) -2 else 2
        val minion = world.spawnNpc("npcs.baby_black_dragon",
            tile.x + offset, tile.z, tile.height)
        // Set minion to attack the KBD's current target
        // Set minion HP to 30
        // On minion death: explode (damage nearby players within 2 tiles for 15)
    }
}
```

For the minion death explosion, check how NPC death hooks work — there may be an `onNpcDeath` event or a death callback.

- [ ] **Step 4: Phase 3 transition message**

```kotlin
if (previousPhase == 2 && currentPhase() == 3) {
    animate("sequences.cerberus_howl")
    world.players.filter { it.tile.regionId == 9033 }.forEach {
        it.message("The dragon enters a frenzy!")
    }
    spawnMinions(world)
}
```

- [ ] **Step 5: Compile check**

Run: `gradle :content:compileKotlin`

- [ ] **Step 6: In-game test**

Fight KBD to 20% HP:
- Attack speed noticeably faster
- Shadow Burst hits all players periodically
- Protect from Magic blocks Shadow Burst
- 2 minions spawn on phase transition
- Minions explode on death

- [ ] **Step 7: Commit**

```bash
git commit -m "feat(boss): add Phase 3 enrage with shadow burst and minions"
```

---

## Phase 4: Drops

### Task 4: Add Drop Table

**Files:**
- Modify: `content/src/main/kotlin/org/alter/npcs/bosses/CorruptedKbdPlugin.kt`

- [ ] **Step 1: Implement drop table**

Check how drop tables are configured in this codebase. The KBD has a commented-out drop table in `KbdConfigsPlugin.kt` using a `drops { }` DSL. If that DSL works, use it:

```kotlin
drops {
    always {
        add("items.dragon_bones", 1)
        add("items.black_dragon_leather", 2)
    }
    main(tableWeight = 128) {
        add("items.rune_longsword", 1, weight = 8)
        add("items.rune_platelegs", 1, weight = 6)
        add("items.dragon_med_helm", 1, weight = 2)
        add("items.dragon_dagger_weapon", 1, weight = 5)
        add("items.fire_rune", 500, weight = 10)
        add("items.blood_rune", 50, weight = 8)
        add("items.death_rune", 75, weight = 8)
        add("items.law_rune", 50, weight = 8)
        // ... etc from spec
    }
}
```

If the drop DSL doesn't work or doesn't exist in the new plugin system, manually roll drops in an `onNpcDeath` handler using `world.random()` and `GroundItem` spawning.

**IMPORTANT:** Look up RSCM names for all items before using them. Use `grep -a -oE '<item_name>=[0-9]+' data/cfg/gamevals-binary/gamevals.dat` to verify.

- [ ] **Step 2: Compile check**

Run: `gradle :content:compileKotlin`

- [ ] **Step 3: In-game test**

Kill the Corrupted KBD. Verify:
- Dragon bones + 2 black dragonhide always drop
- Main table drops appear (runes, bars, logs, etc.)

- [ ] **Step 4: Commit**

```bash
git commit -m "feat(boss): add Corrupted KBD drop table"
```

---

### Task 5: Final Build Verification

- [ ] **Step 1: Full compile**

Run: `gradle build`

- [ ] **Step 2: Full combat test**

1. Engage Corrupted KBD
2. Phase 1: verify poison always applies, max hit ~38
3. Phase 2: verify corruption tiles + splash fire
4. Phase 3: verify enrage speed, shadow burst, minions
5. Kill boss, verify drops
6. Verify respawn after 50 ticks

- [ ] **Step 3: Commit**

```bash
git commit -m "feat(boss): complete Corrupted KBD implementation"
```
