# Special Attacks + Spells Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Migrate all 48 combat spells and 11 special attacks from legacy KotlinPlugin system to the new PluginEvent/CombatSystem event pipeline, then delete legacy code.

**Architecture:** Move CombatSpell enum to content module, create SpellCastingPlugin + AutocastPlugin for spell initiation, rewrite all 11 special attacks as PluginEvent classes that modify the standard combat pipeline via AccuracyRoll/MaxHitRoll/PostAttack event listeners, create SpecialAttackDispatcher for energy management.

**Tech Stack:** Kotlin, PluginEvent system, CombatSystem event pipeline, RSCM name resolution

**Spec:** `docs/superpowers/specs/2026-03-26-special-attacks-spells-design.md`

---

## File Structure

```
content/src/main/kotlin/org/alter/combat/
    spell/
        CombatSpell.kt              — Migrated enum (48 spells) from game-plugins
        SpellCastingPlugin.kt       — SpellOnNpc/PlayerEvent listener for combat spell initiation
        AutocastPlugin.kt           — PreAttackEvent listener for autocast spell resolution

    special/
        SpecialAttackAttributes.kt  — SPECIAL_ATTACK_ACTIVE key + isActiveSpecial() helper
        SpecialAttackDispatcher.kt  — PreAttackEvent listener: energy check, dispatch, toggle handling
        weapons/
            AbyssalBludgeonSpecial.kt
            AbyssalDaggerSpecial.kt
            ArmadylGodswordSpecial.kt
            BandosGodswordSpecial.kt
            DragonBattleaxeSpecial.kt
            DragonDaggerSpecial.kt
            DragonLongswordSpecial.kt
            DragonMaceSpecial.kt
            DragonWarhammerSpecial.kt
            SaradominGodswordSpecial.kt
            ZamorakGodswordSpecial.kt

    strategy/
        NewMagicCombatStrategy.kt   — Modified: real spell animations/projectiles

game-server/src/main/kotlin/org/alter/game/pluginnew/event/impl/
    SpellOnNpcEvent.kt              — New event type

game-server/src/main/kotlin/org/alter/game/message/handler/
    OpNpcTHandler.kt                — Modified: post SpellOnNpcEvent

game-api/src/main/kotlin/org/alter/api/
    CombatAttributes.kt             — Modified: add SPECIAL_ATTACK_ACTIVE key

content/src/main/kotlin/org/alter/combat/
    CombatXpPlugin.kt               — Modified: replace reflection with direct CombatSpell cast
    formula/
        MagicCombatFormulaPlugin.kt — Modified: replace reflection with direct CombatSpell cast
```

---

### Task 1: Move CombatSpell enum to content module

**Files:**
- Create: `content/src/main/kotlin/org/alter/combat/spell/CombatSpell.kt`
- Modify: `content/src/main/kotlin/org/alter/combat/CombatXpPlugin.kt`
- Modify: `content/src/main/kotlin/org/alter/combat/formula/MagicCombatFormulaPlugin.kt`

- [ ] **Step 1: Create CombatSpell.kt in content**

Copy the enum from `game-plugins/src/main/kotlin/org/alter/plugins/content/combat/strategy/magic/CombatSpell.kt` to the new location with the package changed:

```kotlin
package org.alter.combat.spell

import org.alter.game.model.Graphic

enum class CombatSpell(
    val spellItemId: Int,
    val maxHit: Int,
    val castAnimation: String,
    val castSound: Int,
    val castGfx: Graphic?,
    val projectile: Int,
    val projectileEndHeight: Int = -1,
    val impactGfx: Graphic?,
    val autoCastId: Int,
    val baseXp: Double = 0.0,
) {
    // --- Standard spells ---
    WIND_STRIKE(3273, 2, "sequences.human_casting_wind_strike", 220, Graphic(id = 711, height = 92), 91, 16, Graphic(id = 92, height = 0), 1, 5.5),
    WATER_STRIKE(3275, 4, "sequences.human_casting_water_strike", 211, Graphic(id = 711, height = 92), 94, 16, Graphic(id = 95, height = 0), 2, 7.5),
    EARTH_STRIKE(3277, 6, "sequences.human_casting_earth_strike", 132, Graphic(id = 711, height = 92), 97, 16, Graphic(id = 98, height = 0), 3, 9.5),
    FIRE_STRIKE(3279, 8, "sequences.human_casting_fire_strike", 160, Graphic(id = 711, height = 92), 100, 16, Graphic(id = 101, height = 0), 4, 11.5),
    WIND_BOLT(3281, 9, "sequences.human_casting_wind_bolt", 218, Graphic(id = 711, height = 92), 117, 16, Graphic(id = 119, height = 0), 5, 13.5),
    WATER_BOLT(3285, 10, "sequences.human_casting_water_bolt", 209, Graphic(id = 711, height = 92), 120, 16, Graphic(id = 122, height = 0), 6, 16.5),
    EARTH_BOLT(3288, 11, "sequences.human_casting_earth_bolt", 130, Graphic(id = 711, height = 92), 123, 16, Graphic(id = 125, height = 0), 7, 19.5),
    FIRE_BOLT(3291, 12, "sequences.human_casting_fire_bolt", 157, Graphic(id = 711, height = 92), 126, 16, Graphic(id = 128, height = 0), 8, 22.5),
    WIND_BLAST(3294, 13, "sequences.human_casting_wind_blast", 216, Graphic(id = 711, height = 92), 132, 16, Graphic(id = 134, height = 0), 9, 25.5),
    WATER_BLAST(3297, 14, "sequences.human_casting_water_blast", 207, Graphic(id = 711, height = 92), 135, 16, Graphic(id = 137, height = 0), 10, 28.5),
    EARTH_BLAST(3302, 15, "sequences.human_casting_earth_blast", 128, Graphic(id = 711, height = 92), 138, 16, Graphic(id = 140, height = 0), 11, 31.5),
    FIRE_BLAST(3307, 16, "sequences.human_casting_fire_blast", 155, Graphic(id = 711, height = 92), 129, 16, Graphic(id = 131, height = 0), 12, 34.5),
    WIND_WAVE(3313, 17, "sequences.human_casting_wind_wave", 222, Graphic(id = 711, height = 92), 158, 16, Graphic(id = 160, height = 0), 13, 36.0),
    WATER_WAVE(3315, 18, "sequences.human_casting_water_wave", 213, Graphic(id = 711, height = 92), 161, 16, Graphic(id = 163, height = 0), 14, 37.5),
    EARTH_WAVE(3319, 19, "sequences.human_casting_earth_wave", 134, Graphic(id = 711, height = 92), 164, 16, Graphic(id = 166, height = 0), 15, 40.0),
    FIRE_WAVE(3321, 20, "sequences.human_casting_fire_wave", 162, Graphic(id = 711, height = 92), 155, 16, Graphic(id = 157, height = 0), 16, 42.5),
    WIND_SURGE(21876, 21, "sequences.human_casting_wind_surge", 222, Graphic(id = 1455, height = 92), 1456, 16, Graphic(id = 1457, height = 0), 48, 44.5),
    WATER_SURGE(21877, 22, "sequences.human_casting_water_surge", 213, Graphic(id = 1458, height = 92), 1459, 16, Graphic(id = 1460, height = 0), 49, 46.5),
    EARTH_SURGE(21878, 23, "sequences.human_casting_earth_surge", 134, Graphic(id = 1461, height = 92), 1462, 16, Graphic(id = 1463, height = 0), 50, 48.5),
    FIRE_SURGE(21879, 24, "sequences.human_casting_fire_surge", 162, Graphic(id = 1464, height = 92), 1465, 16, Graphic(id = 1466, height = 0), 51, 50.5),

    // --- Ancient spells ---
    SMOKE_RUSH(4629, 14, "sequences.human_casting_smoke_rush", 183, Graphic(id = 385, height = 0), 386, -1, Graphic(id = 387, height = 0), 31, 30.0),
    SHADOW_RUSH(4630, 15, "sequences.human_casting_shadow_rush", 178, Graphic(id = 379, height = 0), 380, -1, Graphic(id = 381, height = 0), 32, 31.0),
    BLOOD_RUSH(4632, 16, "sequences.human_casting_blood_rush", 106, Graphic(id = 373, height = 0), 374, -1, Graphic(id = 375, height = 0), 33, 33.0),
    ICE_RUSH(4633, 17, "sequences.human_casting_ice_rush", 171, Graphic(id = 361, height = 0), 362, -1, Graphic(id = 363, height = 0), 34, 34.0),
    SMOKE_BURST(4635, 18, "sequences.human_casting_smoke_burst", 183, Graphic(id = 385, height = 0), 386, -1, Graphic(id = 389, height = 0), 35, 36.0),
    SHADOW_BURST(4636, 19, "sequences.human_casting_shadow_burst", 178, Graphic(id = 379, height = 0), 380, -1, Graphic(id = 382, height = 0), 36, 37.0),
    BLOOD_BURST(4638, 21, "sequences.human_casting_blood_burst", 106, Graphic(id = 373, height = 0), 374, -1, Graphic(id = 376, height = 0), 37, 39.0),
    ICE_BURST(4639, 22, "sequences.human_casting_ice_burst", 171, Graphic(id = 361, height = 0), 362, -1, Graphic(id = 363, height = 0), 38, 40.0),
    SMOKE_BLITZ(4641, 23, "sequences.human_casting_smoke_blitz", 183, Graphic(id = 386, height = 0), 387, -1, Graphic(id = 388, height = 0), 39, 42.0),
    SHADOW_BLITZ(4642, 24, "sequences.human_casting_shadow_blitz", 178, Graphic(id = 380, height = 0), 381, -1, Graphic(id = 382, height = 0), 40, 43.0),
    BLOOD_BLITZ(4644, 25, "sequences.human_casting_blood_blitz", 106, Graphic(id = 374, height = 0), 375, -1, Graphic(id = 376, height = 0), 41, 45.0),
    ICE_BLITZ(4645, 26, "sequences.human_casting_ice_blitz", 171, Graphic(id = 362, height = 0), 363, -1, Graphic(id = 367, height = 0), 42, 46.0),
    SMOKE_BARRAGE(4647, 27, "sequences.human_casting_smoke_barrage", 183, Graphic(id = 386, height = 0), 387, -1, Graphic(id = 391, height = 0), 43, 48.0),
    SHADOW_BARRAGE(4648, 28, "sequences.human_casting_shadow_barrage", 178, Graphic(id = 380, height = 0), 381, -1, Graphic(id = 383, height = 0), 44, 49.0),
    BLOOD_BARRAGE(4650, 29, "sequences.human_casting_blood_barrage", 106, Graphic(id = 374, height = 0), 375, -1, Graphic(id = 377, height = 0), 45, 51.0),
    ICE_BARRAGE(4651, 30, "sequences.human_casting_ice_barrage", 171, Graphic(id = 362, height = 0), 363, -1, Graphic(id = 369, height = 0), 46, 52.0),
    ;

    companion object {
        private val byItemId = values().associateBy { it.spellItemId }
        private val byAutoCastId = values().associateBy { it.autoCastId }

        fun findByItemId(id: Int): CombatSpell? = byItemId[id]
        fun findByAutoCastId(id: Int): CombatSpell? = byAutoCastId[id]
    }
}
```

**IMPORTANT:** The animation RSCM names and graphic/projectile IDs above are based on the existing enum in game-plugins. During implementation, **read the actual file** at `game-plugins/src/main/kotlin/org/alter/plugins/content/combat/strategy/magic/CombatSpell.kt` and copy the exact values. Do not trust the values in this plan — they may have been updated since the plan was written.

- [ ] **Step 2: Fix CombatXpPlugin — replace reflection with direct cast**

In `content/src/main/kotlin/org/alter/combat/CombatXpPlugin.kt`, replace the `getSpellBaseXp` method:

```kotlin
// Old (reflection-based):
private fun getSpellBaseXp(player: Player): Double {
    val spell = player.attr[CombatAttributes.CASTING_SPELL] ?: return 0.0
    return try { ... reflection ... } catch ...
}

// New (direct cast):
private fun getSpellBaseXp(player: Player): Double {
    val spell = player.attr[CombatAttributes.CASTING_SPELL] as? CombatSpell ?: return 0.0
    return spell.baseXp
}
```

Add import: `import org.alter.combat.spell.CombatSpell`

- [ ] **Step 3: Fix MagicCombatFormulaPlugin — replace reflection with direct cast**

In `content/src/main/kotlin/org/alter/combat/formula/MagicCombatFormulaPlugin.kt`, replace the spell lookup in `getSpellBaseHit()` (around lines 239-256). Replace the reflection block:

```kotlin
// Old (reflection):
if (attacker is Player) {
    val spell = attacker.attr[CombatAttributes.CASTING_SPELL]
    if (spell != null) {
        return try {
            val method = spell.javaClass.getMethod("getMaxHit")
            ...
        }
    }
}

// New (direct cast):
if (attacker is Player) {
    val spell = attacker.attr[CombatAttributes.CASTING_SPELL] as? CombatSpell
    if (spell != null) return spell.maxHit
}
```

Add import: `import org.alter.combat.spell.CombatSpell`

- [ ] **Step 4: Verify compilation**

Run: `APP_HOME="$(pwd)" bash gradlew :game-server:compileKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL (exit code 0)

- [ ] **Step 5: Commit**

```bash
git add content/src/main/kotlin/org/alter/combat/spell/CombatSpell.kt \
       content/src/main/kotlin/org/alter/combat/CombatXpPlugin.kt \
       content/src/main/kotlin/org/alter/combat/formula/MagicCombatFormulaPlugin.kt
git commit -m "feat(combat): move CombatSpell enum to content, replace reflection with direct casts"
```

---

### Task 2: Create SpellOnNpcEvent and wire into handler

**Files:**
- Create: `game-server/src/main/kotlin/org/alter/game/pluginnew/event/impl/SpellOnNpcEvent.kt`
- Modify: `game-server/src/main/kotlin/org/alter/game/message/handler/OpNpcTHandler.kt`

- [ ] **Step 1: Create SpellOnNpcEvent**

```kotlin
package org.alter.game.pluginnew.event.impl

import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.event.PlayerEvent

class SpellOnNpcEvent(
    val npc: Npc,
    val interfaceId: Int,
    val componentId: Int,
    player: Player
) : PlayerEvent(player)
```

- [ ] **Step 2: Update OpNpcTHandler to post SpellOnNpcEvent**

Read `game-server/src/main/kotlin/org/alter/game/message/handler/OpNpcTHandler.kt`. Find the `else` branch (around line 53) that calls `executeSpellOnNpc`. Add a `SpellOnNpcEvent.post()` call:

```kotlin
} else {
    // Post new event for PluginEvent listeners
    SpellOnNpcEvent(npc, parent, child, client).post()
    // Legacy fallback
    if (!client.world.plugins.executeSpellOnNpc(client, parent, child)) {
        client.writeMessage(Entity.NOTHING_INTERESTING_HAPPENS)
        if (client.world.devContext.debugMagicSpells) {
            client.writeMessage("Unhandled magic spell: [$parent, $child] out here")
        }
    }
}
```

Add import: `import org.alter.game.pluginnew.event.impl.SpellOnNpcEvent`

- [ ] **Step 3: Verify compilation**

Run: `APP_HOME="$(pwd)" bash gradlew :game-server:compileKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL (exit code 0)

- [ ] **Step 4: Commit**

```bash
git add game-server/src/main/kotlin/org/alter/game/pluginnew/event/impl/SpellOnNpcEvent.kt \
       game-server/src/main/kotlin/org/alter/game/message/handler/OpNpcTHandler.kt
git commit -m "feat(combat): add SpellOnNpcEvent and wire into OpNpcTHandler"
```

---

### Task 3: Create SpellCastingPlugin

**Files:**
- Create: `content/src/main/kotlin/org/alter/combat/spell/SpellCastingPlugin.kt`

- [ ] **Step 1: Create SpellCastingPlugin**

```kotlin
package org.alter.combat.spell

import org.alter.api.CombatAttributes
import org.alter.game.combat.CombatSystem
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.SpellOnNpcEvent
import org.alter.game.pluginnew.event.impl.SpellOnPlayerEvent
import org.alter.plugins.content.magic.MagicSpells

/**
 * Handles combat spell initiation when a player casts a spell on a target
 * from the spellbook interface. Replaces the legacy CombatSpellsPlugin.
 *
 * Flow:
 * 1. Player clicks combat spell on target -> SpellOnNpcEvent/SpellOnPlayerEvent fires
 * 2. Look up CombatSpell from the spellbook metadata
 * 3. Validate level + rune requirements
 * 4. Set CASTING_SPELL attribute
 * 5. Engage via CombatSystem
 */
class SpellCastingPlugin : PluginEvent() {

    override fun init() {
        onEvent<SpellOnNpcEvent> {
            castSpellOnTarget(player, npc, interfaceId, componentId)
        }

        onEvent<SpellOnPlayerEvent> {
            castSpellOnTarget(player, target, interfaceId, componentId)
        }
    }

    private fun castSpellOnTarget(
        player: Player,
        target: org.alter.game.model.entity.Pawn,
        interfaceId: Int,
        componentId: Int,
    ) {
        // Look up spell metadata from cache-loaded data
        val combatSpells = MagicSpells.getCombatSpells()
        val metadata = combatSpells.values.firstOrNull {
            it.interfaceId == interfaceId && it.component == componentId
        } ?: return

        // Resolve CombatSpell enum entry
        val spell = CombatSpell.findByItemId(metadata.paramItem) ?: return

        // Validate requirements
        if (!MagicSpells.canCast(player, metadata.lvl, metadata.items)) return

        // Consume runes
        MagicSpells.removeRunes(player, metadata.items)

        // Set the spell attribute
        player.attr[CombatAttributes.CASTING_SPELL] = spell

        // Engage target through the combat system
        val strategy = CombatSystem.instance.resolveStrategy(player)
        val style = CombatSystem.instance.resolveCombatStyle(player)
        CombatSystem.instance.engage(player, target, strategy, style)
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `APP_HOME="$(pwd)" bash gradlew :game-server:compileKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL (exit code 0)

- [ ] **Step 3: Commit**

```bash
git add content/src/main/kotlin/org/alter/combat/spell/SpellCastingPlugin.kt
git commit -m "feat(combat): add SpellCastingPlugin for PluginEvent-based spell casting"
```

---

### Task 4: Create AutocastPlugin

**Files:**
- Create: `content/src/main/kotlin/org/alter/combat/spell/AutocastPlugin.kt`

- [ ] **Step 1: Create AutocastPlugin**

```kotlin
package org.alter.combat.spell

import org.alter.api.CombatAttributes
import org.alter.api.ext.getVarbit
import org.alter.game.combat.CombatSystem
import org.alter.game.combat.DisengageReason
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.PreAttackEvent
import org.alter.plugins.content.magic.MagicSpells

/**
 * Resolves the autocast spell before each attack in the combat pipeline.
 *
 * Runs at priority -10 (before formula plugins at 0) so that
 * [CombatAttributes.CASTING_SPELL] is set before accuracy/maxhit calculations.
 *
 * If the player has an autocast spell selected but no manual spell set,
 * this plugin looks up the spell, validates runes, and sets the attribute.
 * If runes are insufficient, the attack is cancelled and the player is disengaged.
 */
class AutocastPlugin : PluginEvent() {

    companion object {
        private const val AUTOCAST_VARBIT = "varbits.autocast_spell"
    }

    override fun init() {
        onEvent<PreAttackEvent>(priority = -10) {
            val player = attacker as? Player ?: return@onEvent

            // Skip if a manual spell is already set (e.g. from SpellCastingPlugin)
            if (player.attr[CombatAttributes.CASTING_SPELL] != null) return@onEvent

            // Check autocast varbit
            val autoCastId = player.getVarbit(AUTOCAST_VARBIT)
            if (autoCastId == 0) return@onEvent

            // Look up the spell
            val spell = CombatSpell.findByAutoCastId(autoCastId) ?: return@onEvent

            // Find metadata for rune requirements
            val combatSpells = MagicSpells.getCombatSpells()
            val metadata = combatSpells.values.firstOrNull {
                it.paramItem == spell.spellItemId
            }

            // Validate and consume runes
            if (metadata != null) {
                if (!MagicSpells.canCast(player, metadata.lvl, metadata.items)) {
                    player.writeMessage("You do not have enough runes to cast this spell.")
                    cancelled = true
                    cancelReason = "Insufficient runes for autocast"
                    CombatSystem.instance.disengage(player, DisengageReason.MANUAL)
                    return@onEvent
                }
                MagicSpells.removeRunes(player, metadata.items)
            }

            // Set the spell attribute for the pipeline
            player.attr[CombatAttributes.CASTING_SPELL] = spell
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `APP_HOME="$(pwd)" bash gradlew :game-server:compileKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL (exit code 0)

- [ ] **Step 3: Commit**

```bash
git add content/src/main/kotlin/org/alter/combat/spell/AutocastPlugin.kt
git commit -m "feat(combat): add AutocastPlugin for autocast spell resolution in pipeline"
```

---

### Task 5: Update NewMagicCombatStrategy with real spell data

**Files:**
- Modify: `content/src/main/kotlin/org/alter/combat/strategy/NewMagicCombatStrategy.kt`

- [ ] **Step 1: Update getAttackAnimation to use CombatSpell**

Read `content/src/main/kotlin/org/alter/combat/strategy/NewMagicCombatStrategy.kt`. Replace the hardcoded animation:

```kotlin
override fun getAttackAnimation(attacker: Pawn): String {
    if (attacker is Player) {
        val spell = attacker.attr[CombatAttributes.CASTING_SPELL] as? CombatSpell
        if (spell != null) return spell.castAnimation
    }
    if (attacker is Npc) return attacker.combatDef.attackAnimation
    return "sequences.human_cast_magic"
}
```

Add imports:
```kotlin
import org.alter.api.CombatAttributes
import org.alter.combat.spell.CombatSpell
import org.alter.game.model.entity.Player
```

Remove any TODO comments about CASTING_SPELL being inaccessible.

- [ ] **Step 2: Verify compilation**

Run: `APP_HOME="$(pwd)" bash gradlew :game-server:compileKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL (exit code 0)

- [ ] **Step 3: Commit**

```bash
git add content/src/main/kotlin/org/alter/combat/strategy/NewMagicCombatStrategy.kt
git commit -m "feat(combat): update NewMagicCombatStrategy with real spell animations"
```

---

### Task 6: Create SpecialAttackAttributes and SpecialAttackDispatcher

**Files:**
- Modify: `game-api/src/main/kotlin/org/alter/api/CombatAttributes.kt`
- Create: `content/src/main/kotlin/org/alter/combat/special/SpecialAttackAttributes.kt`
- Create: `content/src/main/kotlin/org/alter/combat/special/SpecialAttackDispatcher.kt`

- [ ] **Step 1: Add SPECIAL_ATTACK_ACTIVE to CombatAttributes**

In `game-api/src/main/kotlin/org/alter/api/CombatAttributes.kt`, add:

```kotlin
/** Set to true during the current attack if it's a special attack. */
val SPECIAL_ATTACK_ACTIVE = AttributeKey<Boolean>()
```

- [ ] **Step 2: Create SpecialAttackAttributes helper**

```kotlin
package org.alter.combat.special

import org.alter.api.CombatAttributes
import org.alter.api.EquipmentType
import org.alter.api.ext.hasEquipped
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player

/**
 * Helper for special attack pipeline listeners.
 * Checks that the current attack is a special and the player has one of
 * the specified weapons equipped.
 */
fun isActiveSpecial(attacker: Pawn, vararg weapons: String): Boolean {
    val player = attacker as? Player ?: return false
    if (player.attr[CombatAttributes.SPECIAL_ATTACK_ACTIVE] != true) return false
    return player.hasEquipped(EquipmentType.WEAPON, *weapons)
}
```

- [ ] **Step 3: Create SpecialAttackDispatcher**

```kotlin
package org.alter.combat.special

import org.alter.api.CombatAttributes
import org.alter.api.EquipmentType
import org.alter.api.ext.getVarp
import org.alter.api.ext.setVarp
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.PostAttackEvent
import org.alter.game.pluginnew.event.impl.PreAttackEvent

/**
 * Manages special attack dispatch: checks if special is toggled on,
 * validates energy, deducts cost, and sets [CombatAttributes.SPECIAL_ATTACK_ACTIVE].
 *
 * Runs at priority -20 (before AutocastPlugin at -10 and formulas at 0).
 *
 * Also cleans up the SPECIAL_ATTACK_ACTIVE attribute after the attack completes
 * via a PostAttackEvent listener at priority 100.
 */
class SpecialAttackDispatcher : PluginEvent() {

    private data class SpecialDef(val energyCost: Int, val executeOnSpecBar: Boolean = false)

    /** Map of RSCM weapon name -> special definition. Resolved to IDs at init. */
    private val specials = mapOf(
        "items.abyssal_bludgeon" to SpecialDef(50),
        "items.abyssal_dagger" to SpecialDef(50),
        "items.abyssal_dagger_p" to SpecialDef(50),
        "items.abyssal_dagger_p+" to SpecialDef(50),
        "items.abyssal_dagger_p++" to SpecialDef(50),
        "items.ags" to SpecialDef(50),
        "items.agsg" to SpecialDef(50),
        "items.bandos_godsword" to SpecialDef(50),
        "items.dragon_battleaxe" to SpecialDef(100, executeOnSpecBar = true),
        "items.dragon_dagger" to SpecialDef(25),
        "items.dragon_dagger_p" to SpecialDef(25),
        "items.dragon_dagger_p+" to SpecialDef(25),
        "items.dragon_dagger_p++" to SpecialDef(25),
        "items.dragon_longsword" to SpecialDef(25),
        "items.dragon_mace" to SpecialDef(25),
        "items.dragon_warhammer" to SpecialDef(50),
        "items.saradomin_godsword" to SpecialDef(50),
        "items.zamorak_godsword" to SpecialDef(50),
    )

    override fun init() {
        // Pre-attack: check special toggle, validate energy, set attribute
        onEvent<PreAttackEvent>(priority = -20) {
            val player = attacker as? Player ?: return@onEvent
            if (player.getVarp("varp.sa_attack") != 1) return@onEvent

            // Find the equipped weapon's special definition
            val weapon = player.equipment[EquipmentType.WEAPON.id] ?: run {
                player.setVarp("varp.sa_attack", 0)
                return@onEvent
            }

            val specDef = findSpecialDef(player) ?: run {
                // Weapon has no special attack
                player.setVarp("varp.sa_attack", 0)
                return@onEvent
            }

            // Dragon Battleaxe executes on toggle, not on attack — skip pipeline dispatch
            if (specDef.executeOnSpecBar) {
                player.setVarp("varp.sa_attack", 0)
                return@onEvent
            }

            // Validate energy
            val currentEnergy = getEnergy(player)
            if (currentEnergy < specDef.energyCost) {
                player.writeMessage("You don't have enough special attack energy.")
                player.setVarp("varp.sa_attack", 0)
                cancelled = true
                cancelReason = "Insufficient special attack energy"
                return@onEvent
            }

            // Deduct energy and activate
            setEnergy(player, currentEnergy - specDef.energyCost)
            player.setVarp("varp.sa_attack", 0)
            player.attr[CombatAttributes.SPECIAL_ATTACK_ACTIVE] = true
        }

        // Post-attack: clean up the special attack attribute
        onEvent<PostAttackEvent>(priority = 100) {
            val player = attacker as? Player ?: return@onEvent
            player.attr.remove(CombatAttributes.SPECIAL_ATTACK_ACTIVE)
        }
    }

    private fun findSpecialDef(player: Player): SpecialDef? {
        return specials.entries.firstOrNull { (itemName, _) ->
            player.hasEquipped(EquipmentType.WEAPON, itemName)
        }?.value
    }

    private fun getEnergy(player: Player): Int = player.getVarp("varp.sa_energy") / 10

    private fun setEnergy(player: Player, amount: Int) {
        player.setVarp("varp.sa_energy", amount.coerceIn(0, 100) * 10)
    }

    private fun Player.hasEquipped(slot: EquipmentType, itemName: String): Boolean {
        return org.alter.api.ext.hasEquipped(this, slot, itemName)
    }
}
```

**IMPORTANT:** The RSCM names for poisoned variants (e.g. `items.abyssal_dagger_p`, `items.dragon_dagger_p`) must be verified from the existing legacy plugins. Read each legacy weapon plugin and copy the exact RSCM names they use in their `SpecialAttacks.register()` calls.

- [ ] **Step 4: Verify compilation**

Run: `APP_HOME="$(pwd)" bash gradlew :game-api:compileKotlin :game-server:compileKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL (exit code 0)

- [ ] **Step 5: Commit**

```bash
git add game-api/src/main/kotlin/org/alter/api/CombatAttributes.kt \
       content/src/main/kotlin/org/alter/combat/special/SpecialAttackAttributes.kt \
       content/src/main/kotlin/org/alter/combat/special/SpecialAttackDispatcher.kt
git commit -m "feat(combat): add SpecialAttackDispatcher for energy management and dispatch"
```

---

### Task 7: Simple single-hit specials (AGS, Dragon Longsword, Dragon Mace, Dragon Warhammer)

**Files:**
- Create: `content/src/main/kotlin/org/alter/combat/special/weapons/ArmadylGodswordSpecial.kt`
- Create: `content/src/main/kotlin/org/alter/combat/special/weapons/DragonLongswordSpecial.kt`
- Create: `content/src/main/kotlin/org/alter/combat/special/weapons/DragonMaceSpecial.kt`
- Create: `content/src/main/kotlin/org/alter/combat/special/weapons/DragonWarhammerSpecial.kt`

These four weapons follow the same pattern: accuracy multiplier on AccuracyRollEvent, damage multiplier on MaxHitRollEvent, animation/graphic on PreAttackEvent.

- [ ] **Step 1: Create ArmadylGodswordSpecial**

Read the legacy plugin at `game-plugins/.../specialattack/weapons/armadylgodsword/ArmadylGodswordPlugin.kt` for exact RSCM names and multipliers.

```kotlin
package org.alter.combat.special.weapons

import org.alter.combat.special.isActiveSpecial
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.AccuracyRollEvent
import org.alter.game.pluginnew.event.impl.MaxHitRollEvent
import org.alter.game.pluginnew.event.impl.PreAttackEvent

class ArmadylGodswordSpecial : PluginEvent() {

    companion object {
        private val WEAPONS = arrayOf("items.ags", "items.agsg")
        private const val ACCURACY_MULTIPLIER = 2.0
        private const val DAMAGE_MULTIPLIER = 1.375
    }

    override fun init() {
        onEvent<PreAttackEvent> {
            if (!isActiveSpecial(attacker, *WEAPONS)) return@onEvent
            val player = attacker as Player
            player.animate("sequences.ags_special_player")
            player.graphic("spotanims.dh_sword_update_armadyl_special_spotanim")
        }

        onEvent<AccuracyRollEvent> {
            if (!isActiveSpecial(attacker, *WEAPONS)) return@onEvent
            attackRoll = (attackRoll * ACCURACY_MULTIPLIER).toInt()
        }

        onEvent<MaxHitRollEvent> {
            if (!isActiveSpecial(attacker, *WEAPONS)) return@onEvent
            maxHit = (maxHit * DAMAGE_MULTIPLIER).toInt()
        }
    }
}
```

- [ ] **Step 2: Create DragonLongswordSpecial**

Read the legacy plugin. Multipliers: 1.25x damage, 1.0x accuracy (no accuracy modifier needed).

```kotlin
package org.alter.combat.special.weapons

import org.alter.combat.special.isActiveSpecial
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.MaxHitRollEvent
import org.alter.game.pluginnew.event.impl.PreAttackEvent

class DragonLongswordSpecial : PluginEvent() {

    override fun init() {
        onEvent<PreAttackEvent> {
            if (!isActiveSpecial(attacker, "items.dragon_longsword")) return@onEvent
            val player = attacker as Player
            player.animate("sequences.longsword_special")
            player.graphic("spotanims.sp_attack_longsword_spotanim", height = 92)
        }

        onEvent<MaxHitRollEvent> {
            if (!isActiveSpecial(attacker, "items.dragon_longsword")) return@onEvent
            maxHit = (maxHit * 1.25).toInt()
        }
    }
}
```

- [ ] **Step 3: Create DragonMaceSpecial**

Read the legacy plugin. Multipliers: 1.5x damage, 1.0x accuracy.

```kotlin
package org.alter.combat.special.weapons

import org.alter.combat.special.isActiveSpecial
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.MaxHitRollEvent
import org.alter.game.pluginnew.event.impl.PreAttackEvent

class DragonMaceSpecial : PluginEvent() {

    override fun init() {
        onEvent<PreAttackEvent> {
            if (!isActiveSpecial(attacker, "items.dragon_mace")) return@onEvent
            val player = attacker as Player
            player.animate("sequences.mace_special")
            player.graphic("spotanims.sp_attack_mace_spotanim", height = 92)
        }

        onEvent<MaxHitRollEvent> {
            if (!isActiveSpecial(attacker, "items.dragon_mace")) return@onEvent
            maxHit = (maxHit * 1.5).toInt()
        }
    }
}
```

- [ ] **Step 4: Create DragonWarhammerSpecial**

Read the legacy plugin. Multipliers: 1.5x damage, 1.0x accuracy.

```kotlin
package org.alter.combat.special.weapons

import org.alter.combat.special.isActiveSpecial
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.MaxHitRollEvent
import org.alter.game.pluginnew.event.impl.PreAttackEvent

class DragonWarhammerSpecial : PluginEvent() {

    override fun init() {
        onEvent<PreAttackEvent> {
            if (!isActiveSpecial(attacker, "items.dragon_warhammer")) return@onEvent
            val player = attacker as Player
            player.animate("sequences.warhammer_special")
            player.graphic("spotanims.sp_attack_warhammer_spotanim", height = 92)
        }

        onEvent<MaxHitRollEvent> {
            if (!isActiveSpecial(attacker, "items.dragon_warhammer")) return@onEvent
            maxHit = (maxHit * 1.5).toInt()
        }
    }
}
```

- [ ] **Step 5: Verify compilation**

Run: `APP_HOME="$(pwd)" bash gradlew :game-server:compileKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL (exit code 0)

- [ ] **Step 6: Commit**

```bash
git add content/src/main/kotlin/org/alter/combat/special/weapons/ArmadylGodswordSpecial.kt \
       content/src/main/kotlin/org/alter/combat/special/weapons/DragonLongswordSpecial.kt \
       content/src/main/kotlin/org/alter/combat/special/weapons/DragonMaceSpecial.kt \
       content/src/main/kotlin/org/alter/combat/special/weapons/DragonWarhammerSpecial.kt
git commit -m "feat(combat): add AGS, dragon longsword, mace, warhammer special attacks"
```

---

### Task 8: Post-hit effect specials (SGS, BGS, ZGS)

**Files:**
- Create: `content/src/main/kotlin/org/alter/combat/special/weapons/SaradominGodswordSpecial.kt`
- Create: `content/src/main/kotlin/org/alter/combat/special/weapons/BandosGodswordSpecial.kt`
- Create: `content/src/main/kotlin/org/alter/combat/special/weapons/ZamorakGodswordSpecial.kt`

- [ ] **Step 1: Create SaradominGodswordSpecial**

Read the legacy plugin at `game-plugins/.../weapons/saradomingodsword/SaradominGodswordPlugin.kt` for exact logic.

```kotlin
package org.alter.combat.special.weapons

import org.alter.api.Skills
import org.alter.combat.special.isActiveSpecial
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.MaxHitRollEvent
import org.alter.game.pluginnew.event.impl.PostAttackEvent
import org.alter.game.pluginnew.event.impl.PreAttackEvent

class SaradominGodswordSpecial : PluginEvent() {

    override fun init() {
        onEvent<PreAttackEvent> {
            if (!isActiveSpecial(attacker, "items.saradomin_godsword")) return@onEvent
            val player = attacker as Player
            player.animate("sequences.sgs_special_player")
            player.graphic("spotanims.dh_sword_update_saradomin_special_spotanim")
        }

        onEvent<MaxHitRollEvent> {
            if (!isActiveSpecial(attacker, "items.saradomin_godsword")) return@onEvent
            maxHit = (maxHit * 1.1).toInt()
        }

        onEvent<PostAttackEvent> {
            if (!isActiveSpecial(attacker, "items.saradomin_godsword")) return@onEvent
            if (damage <= 0) return@onEvent
            val player = attacker as Player

            // Heal 50% of damage (minimum 10 HP)
            val healAmount = maxOf(damage / 2, 10)
            val currentHp = player.getSkills().getCurrentLevel(Skills.HITPOINTS)
            val maxHp = player.getSkills().getBaseLevel(Skills.HITPOINTS)
            player.getSkills().setCurrentLevel(Skills.HITPOINTS, minOf(currentHp + healAmount, maxHp))

            // Restore 25% of damage as prayer
            val prayerRestore = damage / 4
            val currentPrayer = player.getSkills().getCurrentLevel(Skills.PRAYER)
            val maxPrayer = player.getSkills().getBaseLevel(Skills.PRAYER)
            player.getSkills().setCurrentLevel(Skills.PRAYER, minOf(currentPrayer + prayerRestore, maxPrayer))
        }
    }
}
```

- [ ] **Step 2: Create BandosGodswordSpecial**

Read the legacy plugin at `game-plugins/.../weapons/bandosgodsword/BandosGodswordPlugin.kt`. The stat drain logic is complex — drains stats by damage dealt, capped at 20% of base per stat, in order: Defence, Strength, Prayer, Attack, Magic, Ranged.

```kotlin
package org.alter.combat.special.weapons

import org.alter.api.Skills
import org.alter.combat.special.isActiveSpecial
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.MaxHitRollEvent
import org.alter.game.pluginnew.event.impl.PostAttackEvent
import org.alter.game.pluginnew.event.impl.PreAttackEvent

class BandosGodswordSpecial : PluginEvent() {

    companion object {
        private val DRAIN_ORDER = intArrayOf(
            Skills.DEFENCE, Skills.STRENGTH, Skills.PRAYER,
            Skills.ATTACK, Skills.MAGIC, Skills.RANGED
        )
    }

    override fun init() {
        onEvent<PreAttackEvent> {
            if (!isActiveSpecial(attacker, "items.bandos_godsword")) return@onEvent
            val player = attacker as Player
            player.animate("sequences.bgs_special_player")
            player.graphic("spotanims.dh_sword_update_bandos_special_spotanim")
        }

        onEvent<MaxHitRollEvent> {
            if (!isActiveSpecial(attacker, "items.bandos_godsword")) return@onEvent
            maxHit = (maxHit * 1.21).toInt()
        }

        onEvent<PostAttackEvent> {
            if (!isActiveSpecial(attacker, "items.bandos_godsword")) return@onEvent
            if (damage <= 0 || !landed) return@onEvent

            var remainingDrain = damage
            when (target) {
                is Player -> drainPlayerStats(target as Player, remainingDrain)
                is Npc -> drainNpcStats(target as Npc, remainingDrain)
            }
        }
    }

    private fun drainPlayerStats(target: Player, totalDrain: Int) {
        var remaining = totalDrain
        for (skill in DRAIN_ORDER) {
            if (remaining <= 0) break
            val current = target.getSkills().getCurrentLevel(skill)
            val base = target.getSkills().getBaseLevel(skill)
            val maxDrain = (base * 0.2).toInt()
            val alreadyDrained = base - current
            val canDrain = (maxDrain - alreadyDrained).coerceAtLeast(0)
            val drain = minOf(remaining, canDrain)
            if (drain > 0) {
                target.getSkills().setCurrentLevel(skill, current - drain)
                remaining -= drain
            }
        }
    }

    private fun drainNpcStats(target: Npc, totalDrain: Int) {
        // NPC stat drain primarily affects defence
        var remaining = totalDrain
        val currentDef = target.combatDef.defence
        val drain = minOf(remaining, currentDef)
        if (drain > 0) {
            // NPC combat defs are shared, so we adjust via a temporary attribute or
            // direct modification. For now, this is a known limitation — NPC stat drain
            // requires a per-NPC stat modifier system not yet built.
            // TODO: Implement NPC stat drain when per-NPC stat modifiers are available
        }
    }
}
```

- [ ] **Step 3: Create ZamorakGodswordSpecial**

Read the legacy plugin. Freezes target for 33 ticks (20 seconds) if hit lands.

```kotlin
package org.alter.combat.special.weapons

import org.alter.combat.special.isActiveSpecial
import org.alter.game.model.entity.Player
import org.alter.game.model.timer.TimerKey
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.MaxHitRollEvent
import org.alter.game.pluginnew.event.impl.PostAttackEvent
import org.alter.game.pluginnew.event.impl.PreAttackEvent

class ZamorakGodswordSpecial : PluginEvent() {

    companion object {
        private val FREEZE_TIMER = TimerKey()
        private const val FREEZE_TICKS = 33 // 20 seconds
    }

    override fun init() {
        onEvent<PreAttackEvent> {
            if (!isActiveSpecial(attacker, "items.zamorak_godsword")) return@onEvent
            val player = attacker as Player
            player.animate("sequences.zgs_special_player")
            player.graphic("spotanims.dh_sword_update_zamorak_special_spotanim")
        }

        onEvent<MaxHitRollEvent> {
            if (!isActiveSpecial(attacker, "items.zamorak_godsword")) return@onEvent
            maxHit = (maxHit * 1.1).toInt()
        }

        onEvent<PostAttackEvent> {
            if (!isActiveSpecial(attacker, "items.zamorak_godsword")) return@onEvent
            if (damage <= 0 || !landed) return@onEvent
            // Freeze the target
            target.timers[FREEZE_TIMER] = FREEZE_TICKS
            target.stopMovement()
        }
    }
}
```

**NOTE:** The freeze system may need to integrate with an existing freeze/stun timer. Read the legacy ZGS plugin and check if there's a shared freeze timer key (e.g. `FROZEN_TIMER` in game-server). If so, use that instead of creating a new one.

- [ ] **Step 4: Verify compilation**

Run: `APP_HOME="$(pwd)" bash gradlew :game-server:compileKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL (exit code 0)

- [ ] **Step 5: Commit**

```bash
git add content/src/main/kotlin/org/alter/combat/special/weapons/SaradominGodswordSpecial.kt \
       content/src/main/kotlin/org/alter/combat/special/weapons/BandosGodswordSpecial.kt \
       content/src/main/kotlin/org/alter/combat/special/weapons/ZamorakGodswordSpecial.kt
git commit -m "feat(combat): add SGS, BGS, ZGS special attacks with post-hit effects"
```

---

### Task 9: Multi-hit specials (Dragon Dagger, Abyssal Dagger)

**Files:**
- Create: `content/src/main/kotlin/org/alter/combat/special/weapons/DragonDaggerSpecial.kt`
- Create: `content/src/main/kotlin/org/alter/combat/special/weapons/AbyssalDaggerSpecial.kt`

Multi-hit specials apply their modifiers to the first hit through the pipeline, then fire an additional hit in the PostAttackEvent. The second hit bypasses the full pipeline and applies damage directly.

- [ ] **Step 1: Create DragonDaggerSpecial**

Read the legacy plugin. DDS: 2 rapid hits, each with 1.15x damage and 1.25x accuracy.

```kotlin
package org.alter.combat.special.weapons

import org.alter.combat.special.isActiveSpecial
import org.alter.game.model.entity.Player
import org.alter.game.model.hit.Hit
import org.alter.game.model.hit.HitType
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.AccuracyRollEvent
import org.alter.game.pluginnew.event.impl.MaxHitRollEvent
import org.alter.game.pluginnew.event.impl.PostAttackEvent
import org.alter.game.pluginnew.event.impl.PreAttackEvent
import kotlin.random.Random

class DragonDaggerSpecial : PluginEvent() {

    companion object {
        private val WEAPONS = arrayOf(
            "items.dragon_dagger", "items.dragon_dagger_p",
            "items.dragon_dagger_p+", "items.dragon_dagger_p++"
        )
        private const val ACCURACY_MULTIPLIER = 1.25
        private const val DAMAGE_MULTIPLIER = 1.15
    }

    override fun init() {
        onEvent<PreAttackEvent> {
            if (!isActiveSpecial(attacker, *WEAPONS)) return@onEvent
            val player = attacker as Player
            player.animate("sequences.puncture")
            player.graphic("spotanims.sp_attack_puncture_spotanim", height = 92)
        }

        onEvent<AccuracyRollEvent> {
            if (!isActiveSpecial(attacker, *WEAPONS)) return@onEvent
            attackRoll = (attackRoll * ACCURACY_MULTIPLIER).toInt()
        }

        onEvent<MaxHitRollEvent> {
            if (!isActiveSpecial(attacker, *WEAPONS)) return@onEvent
            maxHit = (maxHit * DAMAGE_MULTIPLIER).toInt()
        }

        // Second hit applied in PostAttackEvent
        onEvent<PostAttackEvent> {
            if (!isActiveSpecial(attacker, *WEAPONS)) return@onEvent
            // Calculate second hit with same modifiers
            val secondMaxHit = (strategy.getAttackSpeed(attacker)) // placeholder — see below
            // The second hit reuses the same accuracy/maxhit from the pipeline.
            // We apply the hit directly using the same maxHit from the event.
            val secondLanded = landed // Same accuracy result for simplicity
            val secondDamage = if (secondLanded && damage > 0) Random.nextInt(damage + 1) else 0
            if (secondDamage > 0) {
                target.hit(damage = secondDamage, type = HitType.HIT, delay = 1)
            } else {
                target.hit(damage = 0, type = HitType.BLOCK, delay = 1)
            }
        }
    }
}
```

**IMPORTANT:** The second hit logic above is simplified. During implementation, read the legacy DragonDagger plugin carefully to understand exactly how the second hit is calculated. The legacy plugin runs `MeleeCombatFormula.getMaxHit()` and `MeleeCombatFormula.getAccuracy()` independently for each hit. In the new system, the second hit should ideally go through the pipeline again. If `CombatSystem` doesn't support firing a second attack within PostAttackEvent, apply the hit directly with a fresh random roll against the same maxHit.

Also verify the exact method signature for `target.hit()` — it may be `target.hit(Hit(...))` or `target.hit(damage, type, delay)`. Read `Pawn.kt` to confirm.

- [ ] **Step 2: Create AbyssalDaggerSpecial**

Read the legacy plugin. Abyssal Dagger: 2 hits at 85% of max hit, 1.25x accuracy.

```kotlin
package org.alter.combat.special.weapons

import org.alter.combat.special.isActiveSpecial
import org.alter.game.model.entity.Player
import org.alter.game.model.hit.HitType
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.AccuracyRollEvent
import org.alter.game.pluginnew.event.impl.MaxHitRollEvent
import org.alter.game.pluginnew.event.impl.PostAttackEvent
import org.alter.game.pluginnew.event.impl.PreAttackEvent
import kotlin.random.Random

class AbyssalDaggerSpecial : PluginEvent() {

    companion object {
        private val WEAPONS = arrayOf(
            "items.abyssal_dagger", "items.abyssal_dagger_p",
            "items.abyssal_dagger_p+", "items.abyssal_dagger_p++"
        )
        private const val ACCURACY_MULTIPLIER = 1.25
        private const val DAMAGE_MULTIPLIER = 0.85
    }

    override fun init() {
        onEvent<PreAttackEvent> {
            if (!isActiveSpecial(attacker, *WEAPONS)) return@onEvent
            val player = attacker as Player
            player.animate("sequences.abyssal_dagger_special")
            player.graphic("spotanims.abyssal_dagger_special_spotanim")
        }

        onEvent<AccuracyRollEvent> {
            if (!isActiveSpecial(attacker, *WEAPONS)) return@onEvent
            attackRoll = (attackRoll * ACCURACY_MULTIPLIER).toInt()
        }

        onEvent<MaxHitRollEvent> {
            if (!isActiveSpecial(attacker, *WEAPONS)) return@onEvent
            maxHit = (maxHit * DAMAGE_MULTIPLIER).toInt()
        }

        // Second hit
        onEvent<PostAttackEvent> {
            if (!isActiveSpecial(attacker, *WEAPONS)) return@onEvent
            val secondLanded = landed
            val secondDamage = if (secondLanded && damage > 0) Random.nextInt(damage + 1) else 0
            if (secondDamage > 0) {
                target.hit(damage = secondDamage, type = HitType.HIT, delay = 1)
            } else {
                target.hit(damage = 0, type = HitType.BLOCK, delay = 1)
            }
        }
    }
}
```

**IMPORTANT:** Same caveats as DragonDaggerSpecial — verify `target.hit()` signature and second-hit calculation against legacy code.

- [ ] **Step 3: Verify compilation**

Run: `APP_HOME="$(pwd)" bash gradlew :game-server:compileKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL (exit code 0)

- [ ] **Step 4: Commit**

```bash
git add content/src/main/kotlin/org/alter/combat/special/weapons/DragonDaggerSpecial.kt \
       content/src/main/kotlin/org/alter/combat/special/weapons/AbyssalDaggerSpecial.kt
git commit -m "feat(combat): add dragon dagger and abyssal dagger multi-hit specials"
```

---

### Task 10: Unique specials (Dragon Battleaxe, Abyssal Bludgeon)

**Files:**
- Create: `content/src/main/kotlin/org/alter/combat/special/weapons/DragonBattleaxeSpecial.kt`
- Create: `content/src/main/kotlin/org/alter/combat/special/weapons/AbyssalBludgeonSpecial.kt`

- [ ] **Step 1: Create DragonBattleaxeSpecial**

Dragon Battleaxe is unique: `executeOnSpecBar = true` means it fires when the spec bar is toggled, not on attack. The SpecialAttackDispatcher skips it in the PreAttackEvent pipeline.

This plugin needs to handle the spec bar toggle itself. Read the legacy plugin at `game-plugins/.../weapons/dragonbattleaxe/DragonBattleaxePlugin.kt` for exact formula.

The strength bonus formula: `10 + floor(0.25 * (floor(10% magic) + floor(10% ranged) + floor(10% defence) + floor(10% attack)))`

Since this executes on spec toggle (not on attack), it listens to a button event or uses a different mechanism. The simplest approach: handle it in SpecialAttackDispatcher's PreAttackEvent by checking `executeOnSpecBar` — but that fires during combat, not on toggle.

Alternative: The existing `AttackTabPlugin` (in game-plugins) handles the spec toggle button click. We need to either:
- Add Dragon Battleaxe logic there (cross-module concern)
- Move spec toggle handling to a new content-module plugin

For now, implement as a PreAttackEvent listener that detects the battleaxe and applies the buff. The spec toggle deducts energy in SpecialAttackDispatcher already. The buff is applied when the player actually attacks with the battleaxe active.

**However**, the legacy behavior is: toggling spec bar ON immediately applies the buff (drains stats, boosts strength) without needing to attack. To replicate this, we'd need to listen to the spec toggle button event.

Pragmatic approach: Use the `onButton` mechanism. Read how `AttackTabPlugin` handles the spec button. If a PluginEvent can listen to button events, do that. Otherwise, defer exact-legacy behavior and have the buff apply on first attack (document as a known deviation).

```kotlin
package org.alter.combat.special.weapons

import org.alter.api.CombatAttributes
import org.alter.api.EquipmentType
import org.alter.api.Skills
import org.alter.api.ext.getVarp
import org.alter.api.ext.hasEquipped
import org.alter.api.ext.setVarp
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.PreAttackEvent
import kotlin.math.floor

/**
 * Dragon Battleaxe special: drains Attack, Defence, Magic, Ranged by 10%
 * and boosts Strength by a calculated amount.
 *
 * NOTE: In OSRS this activates on spec bar toggle, not on attack.
 * This implementation activates on the first attack with spec toggled on.
 * The SpecialAttackDispatcher skips `executeOnSpecBar` weapons, so this
 * plugin must handle energy deduction itself.
 */
class DragonBattleaxeSpecial : PluginEvent() {

    override fun init() {
        onEvent<PreAttackEvent>(priority = -15) {
            val player = attacker as? Player ?: return@onEvent
            if (player.getVarp("varp.sa_attack") != 1) return@onEvent
            if (!player.hasEquipped(EquipmentType.WEAPON, "items.dragon_battleaxe")) return@onEvent

            // Check and deduct energy
            val currentEnergy = player.getVarp("varp.sa_energy") / 10
            if (currentEnergy < 100) {
                player.writeMessage("You don't have enough special attack energy.")
                player.setVarp("varp.sa_attack", 0)
                cancelled = true
                return@onEvent
            }

            player.setVarp("varp.sa_energy", 0)
            player.setVarp("varp.sa_attack", 0)

            // Play animation and graphic
            player.animate("sequences.battleaxe_crush")
            player.graphic("spotanims.sp_attack_battleaxe_spotanim", height = 92)

            // Calculate stat drains and strength bonus
            val magic = player.getSkills().getCurrentLevel(Skills.MAGIC)
            val ranged = player.getSkills().getCurrentLevel(Skills.RANGED)
            val defence = player.getSkills().getCurrentLevel(Skills.DEFENCE)
            val attack = player.getSkills().getCurrentLevel(Skills.ATTACK)

            val magicDrain = floor(magic * 0.1).toInt()
            val rangedDrain = floor(ranged * 0.1).toInt()
            val defenceDrain = floor(defence * 0.1).toInt()
            val attackDrain = floor(attack * 0.1).toInt()

            // Drain stats
            player.getSkills().setCurrentLevel(Skills.MAGIC, magic - magicDrain)
            player.getSkills().setCurrentLevel(Skills.RANGED, ranged - rangedDrain)
            player.getSkills().setCurrentLevel(Skills.DEFENCE, defence - defenceDrain)
            player.getSkills().setCurrentLevel(Skills.ATTACK, attack - attackDrain)

            // Calculate and apply strength bonus
            val bonus = 10.0 + floor(0.25 * (floor(magicDrain.toDouble()) + floor(rangedDrain.toDouble()) + floor(defenceDrain.toDouble()) + floor(attackDrain.toDouble())))
            player.attr[CombatAttributes.DRAGON_BATTLEAXE_BONUS] = bonus

            // Boost strength
            val currentStr = player.getSkills().getCurrentLevel(Skills.STRENGTH)
            val baseStr = player.getSkills().getBaseLevel(Skills.STRENGTH)
            player.getSkills().setCurrentLevel(Skills.STRENGTH, minOf(currentStr + bonus.toInt(), baseStr + bonus.toInt()))

            player.writeMessage("You feel a surge of strength!")

            // Cancel the attack — the spec is the action itself
            cancelled = true
            cancelReason = "Dragon battleaxe special executed"
        }
    }
}
```

**IMPORTANT:** This requires `DRAGON_BATTLEAXE_BONUS` to be added to `CombatAttributes` in game-api. Add it as: `val DRAGON_BATTLEAXE_BONUS = AttributeKey<Double>()`

Also, the new MeleeCombatFormulaPlugin in content needs to read this attribute for the strength bonus. Check if it already does (the legacy MeleeCombatFormula reads `Combat.DRAGON_BATTLEAXE_BONUS`). If the content formula plugin doesn't read it yet, add that in this task.

- [ ] **Step 2: Create AbyssalBludgeonSpecial**

Read the legacy plugin. Damage bonus = `(base_prayer - current_prayer) * 0.5 / 100`.

```kotlin
package org.alter.combat.special.weapons

import org.alter.api.Skills
import org.alter.combat.special.isActiveSpecial
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.MaxHitRollEvent
import org.alter.game.pluginnew.event.impl.PreAttackEvent

class AbyssalBludgeonSpecial : PluginEvent() {

    override fun init() {
        onEvent<PreAttackEvent> {
            if (!isActiveSpecial(attacker, "items.abyssal_bludgeon")) return@onEvent
            val player = attacker as Player
            player.animate("sequences.abyssal_bludgeon_special_attack")
            player.graphic("spotanims.abyssal_miasma_spotanim_bludgeon")
        }

        onEvent<MaxHitRollEvent> {
            if (!isActiveSpecial(attacker, "items.abyssal_bludgeon")) return@onEvent
            val player = attacker as Player
            val basePrayer = player.getSkills().getBaseLevel(Skills.PRAYER)
            val currentPrayer = player.getSkills().getCurrentLevel(Skills.PRAYER)
            val prayerDrain = basePrayer - currentPrayer
            val dmgBonus = prayerDrain * 0.5 / 100.0
            maxHit = (maxHit * (1.0 + dmgBonus)).toInt()
        }
    }
}
```

- [ ] **Step 3: Add DRAGON_BATTLEAXE_BONUS to CombatAttributes**

In `game-api/src/main/kotlin/org/alter/api/CombatAttributes.kt`, add:

```kotlin
/** Dragon battleaxe special bonus to strength. */
val DRAGON_BATTLEAXE_BONUS = AttributeKey<Double>()
```

- [ ] **Step 4: Verify compilation**

Run: `APP_HOME="$(pwd)" bash gradlew :game-api:compileKotlin :game-server:compileKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL (exit code 0)

- [ ] **Step 5: Commit**

```bash
git add content/src/main/kotlin/org/alter/combat/special/weapons/DragonBattleaxeSpecial.kt \
       content/src/main/kotlin/org/alter/combat/special/weapons/AbyssalBludgeonSpecial.kt \
       game-api/src/main/kotlin/org/alter/api/CombatAttributes.kt
git commit -m "feat(combat): add dragon battleaxe and abyssal bludgeon specials"
```

---

### Task 11: Delete legacy special attack and spell code

**Files:**
- Delete: `game-plugins/src/main/kotlin/org/alter/plugins/content/combat/specialattack/` (entire directory)
- Delete: `game-plugins/src/main/kotlin/org/alter/plugins/content/combat/strategy/magic/CombatSpell.kt`
- Delete: `game-plugins/src/main/kotlin/org/alter/plugins/content/combat/strategy/magic/CombatSpellsPlugin.kt`
- Modify: any files that reference the deleted code

- [ ] **Step 1: Find all references to deleted code**

Search for imports/usages of the code being deleted:

```bash
grep -r "import org.alter.plugins.content.combat.specialattack" game-plugins/src/ content/src/ --include="*.kt" -l
grep -r "import org.alter.plugins.content.combat.strategy.magic.CombatSpell" game-plugins/src/ content/src/ --include="*.kt" -l
grep -r "SpecialAttacks\." game-plugins/src/ content/src/ --include="*.kt" -l
```

Fix any remaining references before deleting.

- [ ] **Step 2: Delete legacy special attack directory**

```bash
rm -rf game-plugins/src/main/kotlin/org/alter/plugins/content/combat/specialattack/
```

- [ ] **Step 3: Delete legacy CombatSpell and CombatSpellsPlugin**

```bash
rm game-plugins/src/main/kotlin/org/alter/plugins/content/combat/strategy/magic/CombatSpell.kt
rm game-plugins/src/main/kotlin/org/alter/plugins/content/combat/strategy/magic/CombatSpellsPlugin.kt
```

- [ ] **Step 4: Fix any broken references**

The legacy `Combat.kt` in game-plugins references `CombatSpell` for the `CASTING_SPELL` typed alias. Since we moved the type, update `Combat.kt`'s `CASTING_SPELL` alias to use `CombatAttributes.CASTING_SPELL` (already done in sub-project 3). Verify no other references remain.

Also check if `MagicCombatStrategy.kt` (legacy, in game-plugins strategy/magic/) references `CombatSpell` — if so, update or note that it's legacy code that will be cleaned up.

- [ ] **Step 5: Verify compilation**

Run: `APP_HOME="$(pwd)" bash gradlew :game-server:compileKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL (exit code 0)

If compilation fails due to remaining references, fix them. Common issues:
- `Combat.CASTING_SPELL` typed as `AttributeKey<CombatSpell>` — change to `CombatAttributes.CASTING_SPELL` (type `AttributeKey<Any>`)
- Legacy `MagicCombatStrategy` importing `CombatSpell` — either delete if unused or update import

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor(combat): delete legacy special attacks and CombatSpell from game-plugins"
```

---

### Task 12: Full build verification

- [ ] **Step 1: Full build**

Run: `APP_HOME="$(pwd)" bash gradlew :game-api:compileKotlin :game-server:compileKotlin :game-server:test 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL, all tests pass (exit code 0)

- [ ] **Step 2: Fix any issues**

Common problems:
- Import path mismatches for `CombatSpell` (content vs game-plugins)
- `SPECIAL_ATTACK_ACTIVE` attribute not found (check CombatAttributes import)
- Legacy references to `SpecialAttacks.register()` or `SpecialAttacks.execute()`
- `MagicSpells` accessibility from content module
- `Player.hit()` method signature differences

- [ ] **Step 3: Commit any fixes**

```bash
git add -A
git commit -m "fix(combat): build fixes for special attacks and spells migration"
```
