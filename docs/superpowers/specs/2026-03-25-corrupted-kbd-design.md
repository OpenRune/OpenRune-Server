# Corrupted King Black Dragon — Design Spec

**Goal:** A custom boss variant of the KBD with three escalating phases, corruption mechanics, and good general loot. Replaces the normal KBD spawn.

---

## 1. Base Stats

Uses the existing `npcs.king_dragon` model and animations.

| Stat | Value |
|------|-------|
| HP | 600 |
| Attack | 320 |
| Strength | 300 |
| Defence | 280 |
| Magic | 300 |
| Attack speed | 3 ticks (2 in Phase 3) |
| Max melee hit | 38 |
| Max dragonfire | 65 |
| Aggro radius | 16 tiles |
| Respawn delay | 50 ticks |

Multi-combat area. Spawns at the existing KBD lair (2274, 4698, region 9033).

Defence bonuses: stab 70, slash 90, crush 90, magic 80, ranged 70 (same as normal KBD).

---

## 2. Phase System

Phase transitions based on current HP. The boss tracks its phase and enables mechanics accordingly.

### Phase 1 (100%–50% HP: 600–300)

Same four KBD attacks:
- **Melee:** Headbutt (stab) or claw (slash), max hit 38
- **Fire breath:** Single-target dragonfire, max hit 65
- **Poison breath:** Dragonfire + **guaranteed** poison (initial damage 8). Normal KBD has 1/6 chance — this always poisons.
- **Freeze breath:** Dragonfire + 1/6 chance to freeze (6 ticks)
- **Shock breath:** Dragonfire + 1/6 chance to drain combat stats by 2

Attack selection: 25% chance melee (if in range), otherwise random from the four breath attacks.

### Phase 2 (50%–20% HP: 300–120) — "Corruption Awakens"

All Phase 1 attacks continue, plus:

**Corruption tiles:** Every 10 ticks, spawn 3-5 purple ground markers (use an existing purple/shadow spotanim) on random tiles within 3 tiles of the boss. Players standing on corruption tiles take 10-15 damage per tick. Tiles last 8 ticks then fade.

**Splash dragonfire:** Fire breath hits a 3x3 area around the target. The primary target takes full damage. Other players within the 3x3 area take 50% damage.

On entering Phase 2, the boss plays `sequences.cerberus_howl` (reusing as a roar) and a message is sent: "The dragon's corruption intensifies!"

### Phase 3 (20%–0% HP: 120–0) — "Enrage Mode"

Attack speed increases from 3 to 2 ticks. All Phase 2 mechanics continue, plus:

**Shadow Burst:** Every 15 ticks, hits ALL players in the arena for 20-30 damage. Blockable by Protect from Magic. Boss plays `sequences.cerberus_special_attack_spray` animation. Visual: `spotanims.cerberus_special_attack_flame` on each player.

**Minion spawn:** On entering Phase 3 (once), spawn 2 small dragons near the boss. Use an existing small dragon NPC (~30 HP, basic melee attacks, max hit 10). On death, each minion explodes: deals 15 damage to all players within 2 tiles. Visual: existing explosion spotanim.

On entering Phase 3, message: "The dragon enters a frenzy!"

---

## 3. Drops

General loot — no custom uniques for now. Generous resource drops to make it worth farming.

**Always:**
- Dragon bones x1
- Black dragonhide x2

**Main table (roll once, total weight 128):**
- Rune longsword x1 (weight 8)
- Rune platelegs x1 (weight 6)
- Dragon med helm x1 (weight 2)
- Dragon dagger x1 (weight 5)
- Fire rune x500 (weight 10)
- Blood rune x50 (weight 8)
- Death rune x75 (weight 8)
- Law rune x50 (weight 8)
- Adamantite bar (noted) x10 (weight 8)
- Runite bar (noted) x3 (weight 5)
- Yew logs (noted) x200 (weight 10)
- Magic logs (noted) x30 (weight 5)
- Shark x5 (weight 10)
- Coins x15000-30000 (weight 15)
- Dragon bones (noted) x10 (weight 8)
- Nothing (weight 12)

---

## 4. File Structure

| File | Purpose |
|------|---------|
| `content/src/main/kotlin/org/alter/npcs/bosses/CorruptedKbdPlugin.kt` | Combat def, spawn, phase system, all attack logic, drops |

Uses the `PluginEvent` system (new plugin style). Overrides the KBD spawn at the same location. The existing `KbdCombatPlugin` and `KbdConfigsPlugin` in `game-plugins/` remain untouched.

---

## 5. Out of Scope

- Custom unique drop items (future addition)
- Instance system (single shared arena for now)
- Custom NPC model/appearance (reuses KBD)
- Corruption/Edgeville lore integration (add later with the island system)
