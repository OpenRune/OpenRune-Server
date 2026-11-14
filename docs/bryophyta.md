# Bryophyta

Bryophyta is a solo, instanced boss in the Varrock Sewers. This module implements the boss
fight, growthlings, lair access, chest loot and the giant bones drop.

## Module layout

| File | Responsibility |
|---|---|
| `Bryophyta.kt` | Boss spec, melee/magic attacks, poison, growthling summons and pruning |
| `BryophytaInstance.kt` | Lair instance, gate warning/access, check-count, exit |
| `BryophytaChest.kt` | Chest open/key/close, loot spill, boss respawn |
| `BryophytaChestLoot.kt` | Members and free-to-play chest tables |
| `BryophytaDropTable.kt` | Boss drop table (bones, weapons, runes, herbs, seeds, gems, tertiary) |
| `BryophytaBones.kt` | Ground giant bones bury (150 Prayer XP, shared warning toggle) |
| `BryophytaLogs.kt` | Bronze axe from the lair logs |
| `BryophytaGroundItemHook.kt` | Blocks picking giant bones up |

## Access

- The gate is `loc.gb_moss_door_in` (object 32534) at `3174, 9901`.
- The first entry needs a mossy key. The key is not consumed by the gate; it sets the
  permanent `varbit.bryophyta_lair_unlocked` and the gate stays open for future entries.
- Without the varbit and without a key the gate says `It's locked!`.
- With access, the gate shows the instanced-area warning and a yes/no choice, then enters.
- `Check-Count` shows `You have opened Bryophyta's chest a total of N times.` as both a
  game message and a message box.

## Instance

- Copies region 12955 (`InstanceArea.copyRegions`), solo, `destroyWhenEmpty = true`.
- Enter coordinate `3214, 9937`; exit rock pile `loc.gb_moss_caverock_exit` at `3212, 9937`.
- Leaving uses the DB exit coordinate `3174, 9900`.
- Kill times are not recorded (`recordsKillTime = false`).
- The boss is spawned from the instance settings row at `3220, 9933` and attacks on entry.

## Combat

- Attack rate 6 ticks.
- Melee: crush, sequence `seq.giant_update_moss_attack`, max hit 16.
- Magic: sequence `seq.giant_basic_spell`, earth blast travel/impact `139`/`140`, max hit 10.
- The boss is stationary and uses magic outside melee range.
- Members worlds apply poison for 8 damage through `param.npc_poison_severity = 40`.
  Poison is not re-applied while the player is already poisoned.
- The 50% fire elemental weakness comes from the cache NPC params.

## Growthlings

- After losing ~25% HP, each boss attack and each player hit has a 1/5 chance to summon
  three growthlings within three tiles of the boss.
- There is a 20-tick cooldown after a summon and after the last growthling dies.
- Only one batch can be alive at once, and summons only roll while the player is targeting
  the boss.
- While any growthling is alive the boss caps melee, ranged and magic damage to 0; poison
  and venom still apply.
- Growthlings can be damaged normally but cannot die to non-axe weapons: lethal damage is
  capped and the player sees `Cut the growthling down with an axe or secateurs.`
- Left-clicking a growthling with an axe or magic secateurs equipped prunes it instantly:
  it plays the equipped axe's chop animation (or the fairy pruning animation for magic
  secateurs), dies without a hitsplat and awards combat XP for the growthling's maximum
  hitpoints. The next growthling can be pruned on the following tick.
- Using an axe, secateurs or magic secateurs on a growthling also prunes it instantly
  (`You prune the Growthling.`). Growthlings drop no remains.
- The lair logs provide a bronze axe (`Take-axe`).

## Chest

- The chest is `loc.mossykey_chest_closed` (56378) inside the instance.
- `Open` first gives `You hear the leaves stir...`; one tick later the loot spills.
- A mossy key is consumed per open. Without one the player sees
  `You need a Mossy key to open this chest.` and the boss still respawns.
- Opening during the fight shows
  `You can't loot the chest whilst Bryophyta is still attacking you!`
- Loot is placed on the player's tile, not in the inventory.
- `varp.bryophyta_chests_opened` counts opens and is shown in chat and on the gate.
- The chest stays open for 20 ticks, then returns to the closed loc.
- The boss respawns at its spawn tile 15 ticks after the chest is clicked, after the
  instance is re-entered, or when a new instance is created.

## Drops

- 100%: giant bones on members, big bones on free-to-play. Default NPC remains are
  suppressed with `param.dropped_remains = -1`.
- Main table: 128 weight members/free-to-play tables (weapons, runes, coins, steel bars,
  coal, spinach roll, herbs, uncommon seeds, gems).
- Tertiary: beginner clue (1/45, scroll box after X Marks the Spot), mossy key 1/16
  (1/8 on a non-Krystilia moss giant task), moss giant bone 1/4 during Rag and Bone Man II,
  ensouled giant head 1/24, long bone 1/400, giant champion scroll 1/5,000, curved bone
  1/5,012.5.
- The chest uses its own 118-weight members/free-to-play tables, always gives a beginner
  clue, rolls one main entry, then the 1/16 key roll (1/8 on task). Members herbs/seeds and
  the shared gem table are used by the boss main table, not the chest.

## Gamevals and data

Custom IDs live in `content/bosses/bryophyta/src/main/resources/gamevals.toml`,
`api/instances/src/main/resources/gamevals.toml` and the prayer table:

- `varp.bryophyta_lair`, `varbit.bryophyta_lair_unlocked`
- `varp.bryophyta_chests_opened`
- `varn.bryophyta_growthling_cooldown`, `varn.bryophyta_growthling_warned`
- `dbrow.instance_bryophyta`
- `dbrow.giantbones` (skill_prayer row, 150 XP)

Server-side NPC params are in `.data/raw-cache/server/npcs.toml`; the varp/varbit/varn
definitions are in `.data/raw-cache/server/{varp,varbit,varn}.toml`.

## Tests

`gradlew :content:bosses:bryophyta:test` covers:

- Members/free-to-play chest main weights (118 each).
- Boss members/free-to-play main weights (128 each).
- Chest table selection, key rates and herb subtable.
- Giant bones Prayer row (150 XP).
- Boss poison severity and suppressed default remains.

Combat achievements and the giantsoul amulet teleport are intentionally out of scope.
