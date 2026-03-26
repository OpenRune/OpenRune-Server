# Special Attacks + Spells — Design Spec

**Date:** 2026-03-26
**Sub-project:** 5 of 6 (Combat System Rewrite)
**Depends on:** Sub-project 1 (Core Combat Engine), Sub-project 3 (Player Combat)

## 1. Overview

Full migration of combat spells (48) and special attacks (11) from legacy KotlinPlugin system to the new PluginEvent/CombatSystem event pipeline. After this sub-project, no legacy combat code remains in active use.

### Goals

- Move `CombatSpell` enum from game-plugins to content module
- Create `SpellOnNpcEvent` and migrate spell casting to PluginEvent system
- Create `SpellCastingPlugin` in content to handle combat spell initiation
- Create `AutocastPlugin` for autocast spell resolution via `PreAttackEvent`
- Rewrite all 11 special attack weapons as PluginEvent classes that modify the standard combat pipeline
- Update `NewMagicCombatStrategy` to read real spell animations/projectiles/delays
- Delete legacy special attack registry, combat context, and weapon plugins from game-plugins

### Non-Goals

- Lunar/Arceuus spells (non-combat utility spells)
- New special attacks beyond the 11 already implemented
- Ancient spell secondary effects (freeze, shadow, blood heal, smoke poison) — deferred to sub-project 6 or a dedicated ancients sub-project
- Spell requirement loading from cache (keep existing `MagicSpells` utility)

## 2. Combat Spell Migration

### 2.1 CombatSpell Enum

Move `CombatSpell` from `game-plugins/.../strategy/magic/CombatSpell.kt` to `content/src/main/kotlin/org/alter/combat/spell/CombatSpell.kt`.

The enum retains all 48 entries with their existing fields:

```
enum class CombatSpell(
    val spellItemId: Int,      // RSCM item param ID
    val maxHit: Int,
    val castAnimation: String, // RSCM sequence name
    val castSound: Int,
    val castGfx: Graphic?,
    val projectile: Int,
    val projectileEndHeight: Int,
    val impactGfx: Graphic?,
    val autoCastId: Int,
    val baseXp: Double,
)
```

Standard spells: 20 (Wind/Water/Earth/Fire × Strike/Bolt/Blast/Wave/Surge)
Ancient spells: 28 (Smoke/Shadow/Blood/Ice × Rush/Burst/Blitz/Barrage)

A companion `findByItemId(id: Int): CombatSpell?` method allows lookup by the spellbook's param item ID.

### 2.2 Fix Reflection Hacks

Once `CombatSpell` is in content, replace reflection in:
- `CombatXpPlugin.getSpellBaseXp()` — direct cast `spell as CombatSpell` then `.baseXp`
- `MagicCombatFormulaPlugin.getSpellBaseHit()` — direct cast then `.maxHit`

### 2.3 SpellOnNpcEvent

Create `SpellOnNpcEvent` in `game-server/src/main/kotlin/org/alter/game/pluginnew/event/impl/`:

```kotlin
class SpellOnNpcEvent(
    val npc: Npc,
    val interfaceId: Int,
    val componentId: Int,
    player: Player
) : PlayerEvent(player)
```

Mirrors the existing `SpellOnPlayerEvent` pattern. Update `OpNpcTHandler` to post this event in addition to (or instead of) the legacy `executeSpellOnNpc` path.

### 2.4 SpellCastingPlugin

New PluginEvent class at `content/src/main/kotlin/org/alter/combat/spell/SpellCastingPlugin.kt`.

Listens to `SpellOnNpcEvent` and `SpellOnPlayerEvent`:

1. Look up the `CombatSpell` from the interface component's param item ID
2. Validate magic level requirement (from `MagicSpells.getSpellRequirements()`)
3. Validate and consume rune requirements
4. Set `player.attr[CombatAttributes.CASTING_SPELL] = spell`
5. Resolve strategy and style, call `CombatSystem.instance.engage(player, target, strategy, style)`

The spell metadata (level requirements, rune costs) is still loaded by `MagicSpells.loadSpellRequirements()` in game-plugins. SpellCastingPlugin accesses this through the existing global spell metadata registry.

### 2.5 AutocastPlugin

New PluginEvent class at `content/src/main/kotlin/org/alter/combat/spell/AutocastPlugin.kt`.

Listens to `PreAttackEvent` at priority -10 (before formula plugins):

1. Check `attacker is Player`
2. Check `attacker.attr[CombatAttributes.CASTING_SPELL]` is null (no manual spell)
3. Read `varbits.autocast_spell` — if 0, return (no autocast)
4. Look up `CombatSpell` by autocast ID
5. Validate rune requirements — if insufficient, cancel attack, message player, disengage
6. Consume runes
7. Set `attacker.attr[CombatAttributes.CASTING_SPELL] = spell`

The strategy resolver already detects `CASTING_SPELL != null` and selects magic strategy.

## 3. NewMagicCombatStrategy Updates

Update `content/src/main/kotlin/org/alter/combat/strategy/NewMagicCombatStrategy.kt`:

### 3.1 Attack Animation

Replace the hardcoded `"sequences.human_cast_magic"` with spell-specific animation:

```kotlin
override fun getAttackAnimation(attacker: Pawn): String {
    if (attacker is Player) {
        val spell = attacker.attr[CombatAttributes.CASTING_SPELL] as? CombatSpell
        if (spell != null) return spell.castAnimation
    }
    // NPC fallback
    return if (attacker is Npc) attacker.combatDef.attackAnimation
           else "sequences.human_cast_magic"
}
```

### 3.2 Attack Speed

Standard spells: 5 ticks. This is already correct in the current implementation.

### 3.3 Projectile and Impact Graphics

Add methods for the combat pipeline to access:

```kotlin
fun getCastGraphic(attacker: Pawn): Graphic?
fun getProjectileId(attacker: Pawn): Int
fun getImpactGraphic(attacker: Pawn): Graphic?
```

These read from the `CombatSpell` on the attacker's `CASTING_SPELL` attribute.

The actual projectile spawning and impact graphic application happens in a `PostAttackEvent` listener (or could be part of the strategy's attack execution). The exact rendering hook depends on how the server sends projectile packets — investigate `world.spawn(Projectile(...))` pattern from existing code.

## 4. Special Attack System

### 4.1 Architecture

Special attacks work through the standard combat event pipeline. Each weapon special is a PluginEvent class with listeners on the relevant pipeline stages.

**Detection:** A `SPECIAL_ATTACK_ACTIVE` attribute (`AttributeKey<Boolean>`) on the player indicates the current attack is a special. This is set by `SpecialAttackDispatcher` and cleared after the attack completes.

**Flow:**
1. Player toggles special bar → `varp.sa_attack = 1`
2. Player attacks → `CombatSystem.engage()` → pipeline starts
3. `SpecialAttackDispatcher` (PreAttackEvent, priority -20) detects `varp.sa_attack == 1`:
   - Identifies equipped weapon
   - Checks energy requirement
   - If insufficient: cancel attack, message "You don't have enough special attack energy."
   - If sufficient: deduct energy, set `SPECIAL_ATTACK_ACTIVE = true`, reset `varp.sa_attack = 0`
4. Individual weapon listeners check `SPECIAL_ATTACK_ACTIVE` and equipped weapon to apply their modifiers
5. `SpecialAttackCleanup` (PostAttackEvent, priority 100) clears `SPECIAL_ATTACK_ACTIVE`

### 4.2 SpecialAttackDispatcher

New PluginEvent at `content/src/main/kotlin/org/alter/combat/special/SpecialAttackDispatcher.kt`.

Listens to `PreAttackEvent` at priority -20 (before everything else):

```kotlin
onEvent<PreAttackEvent> {
    val player = attacker as? Player ?: return@onEvent
    if (player.getVarp("varp.sa_attack") != 1) return@onEvent

    val weaponItemId = player.getEquipment(EquipmentType.WEAPON)?.id ?: -1
    val energyCost = getEnergyCost(weaponItemId)
    if (energyCost == null) {
        // Weapon has no special attack
        player.setVarp("varp.sa_attack", 0)
        return@onEvent
    }

    val currentEnergy = AttackTab.getEnergy(player)
    if (currentEnergy < energyCost) {
        cancelled = true
        cancelReason = "Not enough special attack energy."
        player.writeMessage("You don't have enough special attack energy.")
        player.setVarp("varp.sa_attack", 0)
        return@onEvent
    }

    AttackTab.setEnergy(player, currentEnergy - energyCost)
    player.setVarp("varp.sa_attack", 0)
    player.attr[SPECIAL_ATTACK_ACTIVE] = true
}
```

The `getEnergyCost` method maps weapon item IDs to energy costs. This can be a simple `when` block or a registry map populated during init.

### 4.3 Individual Weapon Specials

Each weapon gets its own PluginEvent class in `content/src/main/kotlin/org/alter/combat/special/weapons/`.

**Standard single-hit specials** (AGS, BGS, Dragon Longsword, Dragon Mace, Dragon Warhammer) follow a pattern:

```kotlin
class ArmadylGodswordSpecial : PluginEvent() {
    override fun init() {
        onEvent<PreAttackEvent> {
            if (!isActiveSpecial(attacker, "items.ags", "items.agsg")) return@onEvent
            (attacker as Player).animate("sequences.ags_special_player")
            (attacker as Player).graphic("spotanims.dh_sword_update_armadyl_special_spotanim")
        }

        onEvent<AccuracyRollEvent> {
            if (!isActiveSpecial(attacker, "items.ags", "items.agsg")) return@onEvent
            attackRoll = (attackRoll * 2.0).toInt()
        }

        onEvent<MaxHitRollEvent> {
            if (!isActiveSpecial(attacker, "items.ags", "items.agsg")) return@onEvent
            maxHit = (maxHit * 1.375).toInt()
        }
    }
}
```

Where `isActiveSpecial(attacker, vararg weapons)` checks `attacker.attr[SPECIAL_ATTACK_ACTIVE] == true && attacker.hasEquipped(EquipmentType.WEAPON, *weapons)`.

**Multi-hit specials** (Dragon Dagger, Abyssal Dagger): The `PostAttackEvent` listener fires an additional attack through the pipeline. The second hit also has `SPECIAL_ATTACK_ACTIVE` set but uses a separate `SPECIAL_SECOND_HIT` attribute to distinguish it.

**Post-hit effect specials:**
- SGS: `PostAttackEvent` listener heals player for 50% of damage (min 10 HP), restores 25% as prayer
- BGS: `PostAttackEvent` listener drains target stats by damage dealt
- ZGS: `PostAttackEvent` listener freezes target for 20 seconds if hit landed

**Unique specials:**
- Dragon Battleaxe: Uses `executeOnSpecBar = true` — executes on toggle, not on attack. This special bypasses the combat pipeline entirely. It needs special handling in the attack tab button handler or as a `PreAttackEvent` that fires on spec bar toggle.

### 4.4 Dragon Battleaxe Special Case

Dragon Battleaxe is unique: it executes when the spec bar is toggled on, not when the player attacks. It doesn't involve a target.

Options:
- Handle in `AttackTabPlugin` (already PluginEvent in game-plugins) with a special case
- Create a non-combat `SpecialAbilityEvent` that fires on spec toggle

Since `AttackTabPlugin` already handles the spec toggle button and is already a PluginEvent, the simplest approach is to add the Dragon Battleaxe logic there. Or: move the handling into `SpecialAttackDispatcher` which can also listen to the spec toggle button event and handle execute-on-toggle specials.

Recommendation: `SpecialAttackDispatcher` handles the `onButton` for the spec toggle. If the weapon's special `executeOnSpecBar` is true, execute it immediately (deduct energy, apply effect). Otherwise, just toggle the varp.

### 4.5 Weapon Special Registry

Each weapon special's energy cost and `executeOnSpecBar` flag are registered in a data map within `SpecialAttackDispatcher`:

```kotlin
private data class SpecialDef(val energyCost: Int, val executeOnSpecBar: Boolean = false)

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
```

RSCM names are resolved to IDs at init time. Note: poisoned dagger variants need their actual RSCM names verified from gamevals.

## 5. Files to Delete

After migration is complete:

```
game-plugins/src/main/kotlin/org/alter/plugins/content/combat/specialattack/
    SpecialAttack.kt
    SpecialAttacks.kt
    CombatContext.kt
    weapons/abyssalbludgeon/AbyssalBludgeon.kt
    weapons/abyssaldagger/AbyssalDagger.kt
    weapons/armadylgodsword/ArmadylGodsword.kt
    weapons/bandosgodsword/BandosGodsword.kt
    weapons/dragonbattleaxe/DragonBattleAxe.kt
    weapons/dragondagger/DragonDagger.kt
    weapons/dragonlongsword/DragonLongsword.kt
    weapons/dragonmace/DragonMace.kt
    weapons/dragonwarhammer/DragonWarhammer.kt
    weapons/saradomingodsword/SaradominGodsword.kt
    weapons/zamorakgodsword/ZamorakGodsword.kt

game-plugins/src/main/kotlin/org/alter/plugins/content/combat/strategy/magic/
    CombatSpell.kt
    CombatSpellsPlugin.kt
```

Also remove any references to `SpecialAttacks.register()` or `SpecialAttacks.execute()` in remaining game-plugins code.

## 6. File Structure

```
content/src/main/kotlin/org/alter/combat/
    spell/
        CombatSpell.kt              — Migrated enum (48 spells)
        SpellCastingPlugin.kt       — SpellOnNpc/PlayerEvent listener, rune validation, engage
        AutocastPlugin.kt           — PreAttackEvent listener, autocast resolution

    special/
        SpecialAttackDispatcher.kt  — PreAttackEvent listener, energy check, dispatch
        SpecialAttackAttributes.kt  — SPECIAL_ATTACK_ACTIVE + helpers
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
        NewMagicCombatStrategy.kt   — Updated: real spell animations/projectiles

game-server/src/main/kotlin/org/alter/game/pluginnew/event/impl/
    SpellOnNpcEvent.kt              — New event type

game-api/src/main/kotlin/org/alter/api/
    CombatAttributes.kt             — Add SPECIAL_ATTACK_ACTIVE key
```

## 7. Integration Points

### 7.1 AttackTab Energy System

The existing `AttackTab` in game-plugins provides `getEnergy()`, `setEnergy()`, `restoreEnergy()`. These are accessed from content via the `Player.getVarp()`/`Player.setVarp()` extension functions on `"varp.sa_energy"`.

The `SpecialAttackDispatcher` reads/writes energy directly through varps rather than importing `AttackTab` (which is in game-plugins).

### 7.2 Rune Consumption

`MagicSpells` (game-plugins) loads spell metadata from cache. `SpellCastingPlugin` needs access to this data. Options:
- Access `MagicSpells` statically (it's a global registry)
- Move rune validation to a shared location

Since `MagicSpells` is already loaded at server start, `SpellCastingPlugin` can reference it directly. If module boundaries prevent this, the spell metadata can be exposed through an interface in game-api.

### 7.3 Projectile System

Combat spell projectiles need to be spawned between attacker and target. The existing pattern uses `world.spawn(Projectile(...))`. The `NewMagicCombatStrategy` or a `PostAttackEvent` listener handles this using data from the `CombatSpell` enum.

## 8. Testing Strategy

- Unit tests for `CombatSpell.findByItemId()` — verify all 48 spell lookups
- Unit tests for each special attack's modifiers — verify accuracy/damage multipliers
- Build verification after each task
- Integration: verify spell casting initiates combat through new pipeline
- Integration: verify special attacks consume energy and apply correct effects
