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
- `Check-Count` shows `You have opened Bryophyta's chest a total of N times.` in a
  message box.

## Instance

- Copies region 12955 (`InstanceArea.copyRegions`), solo, `destroyWhenEmpty = true`.
- Enter coordinate `3214, 9937`; exit rock pile `loc.gb_moss_caverock_exit` at `3212, 9937`.
- The exit boulder has `Clamber` (`onExitObject`) and `Quick-exit` (`onOpLoc3`, immediate
  leave that also works while the boss is still fighting).
- Leaving uses the DB exit coordinate `3174, 9900`.
- Kill times are not recorded (`recordsKillTime = false`).
- The boss is spawned from the instance settings row at `3220, 9933` and attacks on entry.

## Combat

- Stats match the cache/wiki: 115 hitpoints, 130 Attack, 100 Strength, 100 Defence, 90
  Magic, combat level 128.
- Attack rate 6 ticks.
- Melee: crush, sequence `seq.giant_update_moss_attack`, max hit 16.
- Magic: sequence `seq.giant_basic_spell`, earth blast travel/impact `139`/`140`, max hit 10.
  The projectile uses `172` start height, `124` end height, `16` curve, `64` progress and
  5 client cycles per tile (the post-`map_projanim_v2` timing); the impact graphic is
  attached to the target player, not the cast tile.
- In melee range the boss rolls melee or magic at equal weight; outside melee range it
  only uses magic.
- Members worlds apply poison for 8 damage through `param.npc_poison_severity = 40`.
  Poison is not re-applied while the player is already poisoned.
- The 50% fire elemental weakness comes from the cache NPC params.

## Growthlings

- Stats match the cache/wiki: 10 hitpoints, 40 Attack, 38 Strength, 40 Defence, combat
  level 37, attack speed 4, max hit 5.
- After losing ~25% HP, each boss attack and each player hit has a 1/5 chance to summon
  three growthlings within three tiles of the boss.
- There is a 20-tick cooldown after a summon and after the last growthling dies.
- Only one batch can be alive at once, and summons only roll while the player is targeting
  the boss.
- While any growthling is alive the boss caps all sourced damage to 0; poison and venom
  are unsourced and still apply.
- Growthlings can be damaged normally but cannot die to non-axe weapons: lethal damage is
  capped and the player sees `Cut the growthling down with an axe or secateurs.`
- Growthlings use the tanglefoot animation set: `seq.tanglefoot_ready` /
  `seq.tanglefoot_walk` for idle/walk, `seq.tanglefoot_attack`, `seq.tanglefoot_block`
  and `seq.tanglefoot_death` for combat. Their `maxRange` is 30 so they keep chasing
  across the lair, and `wanderRange` is 0 so a growthling without a target stands still
  instead of wandering.
- Summoned growthlings are non-respawning: pruning deletes them instead of scheduling a
  normal NPC respawn. The boss keeps dead/dying growthlings tracked until they are
  deleted, so a new set cannot spawn while the previous set is still in its death
  animation. The 20-tick cooldown is applied when the last tracked growthling is deleted.
- The growthling is removed on the second tick of `seq.tanglefoot_death`, so the client
  cannot loop the sequence back to its standing frame after the death animation.
- Left-clicking a growthling with an axe or magic secateurs equipped prunes it instantly:
  it plays `seq.human_dhsword_stab`, the same animation Jagex uses when using an axe or
  secateurs on a growthling, dies without a hitsplat and awards combat XP for the
  growthling's maximum hitpoints. The next growthling can be pruned on the following tick.
- Using an axe, secateurs or magic secateurs on a growthling also prunes it instantly
  (`You prune the Growthling.`). Growthlings drop no remains.
- The lair logs provide a bronze axe (`Take-axe`) only when the player has no woodcutting axe
  equipped or in their inventory; otherwise they see `You already have an axe.` Taking it
  leaves empty logs that refill after 50 ticks (30 seconds).

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
  clue, rolls one main entry, then a 1/16 key roll. The chest key is not boosted by a moss
  giant Slayer task.
- Bryophyta's essence is the chest's boosted entry: at the default 1x rate it stays 1/118,
  and the server/player drop-rate multiplier now applies to that roll.
- The boss main table uses its own gem table: Nature talisman on members, and the
  free-to-play subset without talismans, rune javelin, key halves or the mega-rare table.

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
- Chest table selection, base key rate, boosted essence and herb subtable.
- Giant bones Prayer row (150 XP).
- Boss poison severity and suppressed default remains.
- Boss/growthling combat stats, tanglefoot/moss giant animation params and chase range.

Combat achievements and the giantsoul amulet teleport are intentionally out of scope.
