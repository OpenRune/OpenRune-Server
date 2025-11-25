# Combat System

## Combat Zones

### Single-Combat
- One attacker per target at a time
- **5-second cooldown** to switch targets (from last damage dealt/received)
- "You're already in combat" if switching too early
- NPCs de-aggress if target switched to another fight

### Multi-Combat
- Multiple attackers allowed
- No target switching cooldown
- Drops → player with **most total damage**

---

## Combat State

| Attribute | Purpose |
|-----------|---------|
| `COMBAT_TARGET_FOCUS_ATTR` | Who you're **trying** to attack (pathfinding) |
| `COMBAT_PARTNER_ATTR` | Who you're **actually** fighting (set on damage) |
| `LAST_COMBAT_ACTION_TIME_ATTR` | Timestamp of last damage dealt/received |
| `COMBAT_STATE_TIMEOUT_MS` | 5000ms cooldown for target switching |

---

## NPC Pathfinding

| Type | Used By |
|------|---------|
| **Smart** (A*/Dijkstra) | Players, NPCs with `canBeStuck=false` |
| **Dumb** (Diagonal-first) | All other NPCs (OSRS-authentic) |

### Dumb Pathfinding
1. Try **diagonal** if both X and Z need adjustment
2. Fall back to **cardinal** if diagonal blocked
3. Builds **full path** (up to 10 tiles) to prevent stuttering
4. Recalculates on **any** target movement

### Stuck NPCs
- Can get stuck behind obstacles (authentic)
- Face target but stop retrying failed paths
- Clear stuck state when target/NPC moves

---

## Line of Sight
- NPCs **cannot attack through walls**
- Melee: `canTraverse()` check for adjacent tiles
- Ranged/Magic: Projectile raycast check

---

## Attack Flow

```
Player clicks NPC
    ↓
Pawn.attack() → sets COMBAT_TARGET_FOCUS_ATTR
    ↓
Combat plugin → canEngage() checks rules
    ↓
┌─────────────────┬─────────────────┐
│ Allowed         │ Blocked         │
├─────────────────┼─────────────────┤
│ handleAttack()  │ "Already in     │
│ deals damage,   │ combat" message │
│ sets PARTNER    │                 │
└─────────────────┴─────────────────┘
```

---

## Combat Formulas

- **Melee/Ranged/Magic** formulas calculate accuracy, max hit, defence
- **Equipment bonuses** applied (10 slots: attack/defence for stab/slash/crush/magic/ranged)
- **Prayer protection**: 100% PvM, 40% PvP reduction
- **Combat XP**: Based on attack style + hitpoints

---

## OSRS Mechanics

### Tick Eating
```
Tick Order: queues.cycle() → cycle() → hitsCycle()
            (eating)                    (damage)
```
- Food consumed **before** damage in same tick
- Eating on death tick = survival

### 1-Tick Prayer Flicking
- Protection checked when damage **calculated** (`hasPrayerIcon()`)
- Drain checked at tick **end**
- Prayer activated **this tick** = no drain yet
- Result: Protection without prayer cost

### Attack Speed
- Weapon-based delay via `ATTACK_DELAY` timer
- `isAttackDelayReady()` check before each attack

### Auto-Retaliate
- `DISABLE_AUTO_RETALIATE_VARP` toggle
- Auto-attacks when hit if enabled

---

## Grace Distance
- **Players**: +3 tile grace when chasing (visual lag compensation)
- **NPCs**: Exact range, no grace
