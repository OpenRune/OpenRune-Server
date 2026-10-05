---
type: subsystem
title: Combat system
description: How player-vs-NPC, NPC-vs-player and PvP combat is structured across the api/combat modules — attack resolution, accuracy and max-hit formulas, the attack manager, specialised weapons, special attacks, combat spells and queued hits — and how content extends it.
tags: [combat, specials, weapons, spells, formulas]
verified:
  - by: openwiki/0.7.0
    at: 2026-10-02T20:46:41.242Z
sources:
  - id: openwiki-source-862032c78139428ddb0e559e
    resource: repo://api/combat/combat-scripts/src/main/kotlin/org/rsmod/api/combat/PvNCombat.kt
  - id: openwiki-source-704d05f62a362c2db4e6eb07
    resource: repo://api/combat/combat-scripts/src/main/kotlin/org/rsmod/api/combat/scripts/PvNCombatScript.kt
  - id: openwiki-source-b61dd0b55d5181d0d6e1a25d
    resource: repo://api/hit-plugin/src/main/kotlin/org/rsmod/api/hit/plugin/NpcHitScript.kt
  - id: openwiki-source-e8cc30d4a8d24a2af286b4ac
    resource: repo://api/specials/src/main/kotlin/org/rsmod/api/specials/SpecialAttackRegistry.kt
  - id: openwiki-source-97f2d1ae42720bf78fcc0280
    resource: repo://api/weapons/src/main/kotlin/org/rsmod/api/weapons/Weapon.kt
  - id: openwiki-source-a2aee423055ebc8d5c24f5d7
    resource: repo://api/weapons/src/main/kotlin/org/rsmod/api/weapons/WeaponAttackManager.kt
  - id: openwiki-source-39160dac93d7cd9a01be8dd3
    resource: repo://content/other/special-attacks/src/main/kotlin/org/rsmod/content/other/special/attacks/melee/DragonLongswordSpecialAttack.kt
  - id: openwiki-source-02654db23515b6f3a2874a59
    resource: repo://content/other/special-attacks/src/main/kotlin/org/rsmod/content/other/special/attacks/SpecialAttackModule.kt
generated: { by: "claude-code", at: "2026-10-02T20:46:41.242Z" }
---

# Combat system

Combat is implemented in the API layer as a set of cooperating modules; content mostly adds
*specialisations* (special attacks, unusual weapons, spells, NPC behaviours) via registries
rather than writing combat loops.

## Module map

| Module | Role |
|---|---|
| `api/combat/combat-commons` | shared model: `CombatAttack` (Melee / Ranged / Spell / Staff), stances, magic spell types and checks, demonbane/dragonfire helpers, ranged ammo |
| `api/combat/combat-formulas` | accuracy and max-hit formulas split by attacker/defender pair (PvN, PvP, NvP, NvN) and style (melee/ranged/magic/multi-style) |
| `api/combat-accuracy`, `api/combat-maxhit` | effective-level/bonus calculations used by the formulas, per player and per NPC |
| `api/combat/combat-manager` | `PlayerAttackManager`, `RangedAmmoManager`, `MagicRuneManager`, `CombatChargeManager` — rolling damage, xp, fx, ammo/rune consumption, combat delay |
| `api/combat/combat-weapon` | attack styles/types from the combat tab varps, weapon speeds |
| `api/combat/combat-scripts` | the `PluginScript`s that bind combat to interactions: `PvNCombatScript`, `NvPCombatScript`, retaliation scripts, freeze timer; orchestrators `PvNCombat`, `PvPCombat`, `NvPCombat` |
| `api/weapons` | `WeaponRegistry` + `WeaponAttackManager` for weapons with bespoke attacks |
| `api/specials` | special attack energy, `SpecialAttackRegistry`, instant and combat specials |
| `api/spells`, `spells-autocast`, `spells-runes` | combat spell registry/attacks, autocast, rune sources (combo, staves, rune pouch-style compact runes, substitutes) |
| `api/hit-plugin` | `NpcHitScript` / `PlayerHitScript` handle the `queue.hit` queues that actually apply damage |

## Player attacks an NPC

`PvNCombatScript` binds:

- `onDefaultApNpc2` / `onDefaultOpNpc2` — the default "Attack" op on any NPC without its own
  handler;
- `onApNpcT(spell.component)` for every combat spell — casting a spell on an NPC.

Flow for the ap/op handlers:

1. Resolve attack type and style from the combat tab, the autocast spell, and build a
   `CombatAttack` from the right-hand weapon.
2. For melee (range 1), switch to op-range (`apRange(-1)`); for ranged/magic walk until in
   range.
3. `canAttack`: every bound `NpcAttackValidateHook` may `Deny` (with a message),
   `Pass`, or `BypassSingleWayPvnRestriction`; then standard single/multi-combat checks.
4. `PvNCombat.attack` dispatches by attack subtype.

For spells, rune checks (`canCastSpell`) happen **before** `canAttack`, matching official
ordering.

`PvNCombat.attackMelee` (ranged and staff follow the same shape):

1. If the player is still on attack delay → `continueCombat` and return.
2. Set the next attack delay from the weapon speed *before* any specialisation runs.
3. If a weapon special is armed → `activateMeleeSpecial`; then shield specials.
4. If `WeaponRegistry` has a specialised melee weapon for the right-hand obj → call it.
5. Otherwise default: roll damage (accuracy + max hit), give xp, play weapon fx, queue the hit,
   and `continueCombat`.

Special attacks and specialised weapons that return `true` own the rest of the attack: they
must explicitly re-engage (call `opnpc2` / `continueCombat`) or `stopCombat`.

Damage is not applied immediately. `queueMeleeHit` etc. enqueue a `queue.hit` on the target;
`NpcHitScript` / `PlayerHitScript` process it on a later tick (see
[Game cycle and tick processing](../architecture/game-cycle.md)).

`NvPCombatScript` and the retaliation scripts handle NPCs attacking players and auto-retaliate.
NPC-side combat behaviour for bosses is covered in
[NPC AI, hunting and bosses](npc-ai-and-bosses.md).

## Extending combat

### Special attacks

Implement `SpecialAttackMap` and register in a `PluginModule` via
`addSetBinding<SpecialAttackMap>(...)`:

```kotlin
class DragonLongswordSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerMelee("obj.dragon_longsword", DragonLongsword(manager))
    }
}
```

`SpecialAttackRegistry.add` rejects duplicates (`AlreadyAdded`) and objs with no special energy
mapping (`SpecialEnergyNotMapped`). Specials typically call manager helpers such as
`rollMeleeDamage(..., accuracyMultiplier = 1.25, maxHitMultiplier = 1.25)`. Examples live in
`content/other/special-attacks`.

### Specialised weapons

Implement `MeleeWeapon`, `RangedWeapon` or `MagicWeapon` (`Weapon<T>` with an `attack` for NPC
and Player targets) and add it to `WeaponRegistry` keyed by obj id. `WeaponAttackManager` exposes
the building blocks (roll accuracy/max hit/damage, queue hits, play fx, give xp, continue/stop
combat). Examples: scythe of vitur, dual macuahuitl, Tumeken's shadow in
`content/other/special-weapons`.

### Attack validation

Bind an `NpcAttackValidateHook` to deny attacks on certain NPCs (e.g. instance ownership, slayer
requirements) without touching combat scripts.

## Testing

Formula modules have integration tests (`api/combat/combat-formulas/src/integration`), and
`combat-commons` has unit tests. See [Testing](../testing/testing.md).
