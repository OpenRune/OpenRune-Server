# Giant Mole — Implementation Plan

Branch: `feat/giant-mole-boss`, based on `main` (`c0a6571ef`).
This repository has no `master` branch; `main` is the integration branch.

## Goal

Add the OSRS Giant Mole boss under Falador Park as a first-class overworld boss:

- the Giant Mole fight (200 HP, melee only, burrow mechanic);
- the Mole Hole entrance, darkness, light sources and insect hazard;
- kill count, Slayer XP, Brimstone keys, clue scrolls, pet and collection log;
- OSRS-accurate drops, including the Hard Falador Diary noted mole parts.

The core target is the 2024 capture in
`D:\Runescape\sniffs\Event inspector\giant-mole-2024-04-19 21-58-047.txt`.
Current-OSRS extras (private instance, immaculate mole skin) are called out separately.

## References

- OSRS wiki: [Giant Mole](https://oldschool.runescape.wiki/w/Giant_Mole),
  [Giant Mole/Strategies](https://oldschool.runescape.wiki/w/Giant_Mole/Strategies),
  [Mole Hole](https://oldschool.runescape.wiki/w/Mole_Hole),
  [Mole hill](https://oldschool.runescape.wiki/w/Mole_hill),
  [Light sources](https://oldschool.runescape.wiki/w/Light_sources),
  [Wyson the gardener](https://oldschool.runescape.wiki/w/Wyson_the_gardener),
  [Fire of Eternal Light](https://oldschool.runescape.wiki/w/Fire_of_Eternal_Light),
  [Baby Mole (NPC)](https://oldschool.runescape.wiki/w/Baby_Mole_(NPC)).
- Event inspector capture: `giant-mole-2024-04-19 21-58-047.txt`, lines
  `4364`–`4886` cover the dig, warning, entry, fight, burrow, death and drops.
- Repo docs: `docs/boss-dsl.md`, `docs/boss-hp-bar.md`, `docs/drops.md`,
  `docs/instances.md`.
- Existing patterns: `content/bosses/scurrius/` (simple boss + instance),
  `content/bosses/spindel/` (lair bounds, flee/teleport, target reset),
  `content/areas/city/lumbridge/LumbridgeSwampLocs.kt` (darkness, insects,
  light-source check), `content/generic/generic-locs/spade/SpadeScript.kt`
  (spade-dig hook), `content/generic/generic-items/.../nests/`
  (bird nest search), `content/other/pets/` (pet drops and pets).

## What already exists

| Piece | State |
|---|---|
| NPC `npc.mole_giant` (5779) | In cache; earth weakness 50% already set in `.data/raw-cache/server/npcs.toml` |
| Baby moles `npc.mole_baby_01..03` (5780–5782) | 30 spawns already in `.data/raw-cache/map/npcs/giant_mole_lair.toml`; one in Motherlode Mine |
| Giant Mole map spawn | `0_27_81_31_5` (1759, 5189, size 3) already in the same file |
| Drop table | `content/drops/src/main/resources/drops/tables/monsters/giant_mole.toml`; mostly matches the wiki |
| Pet | `obj.molepet` / `obj.molepet_naked`, `npc.mole_pet` / `npc.mole_pet_naked`, dialogue and morphs already exist |
| Collection log | `collection_log_category_giant_mole` and its cache struct already exist; drop hook grants entries automatically |
| Wyson | Existing `Talk-to` dialogue tree in `content/areas/city/falador/.../FaladorTownsfolkScript.kt`; mole-part exchange is not implemented |
| Music | Cache midi `the_mad_mole` (573) exists |
| Fire pit | `loc.my2arm_fire_pit_mole` (33336) and `loc.my2arm_fire_eternallight` (33320) exist in cache; no script |
| Private instance | `loc.mole_hill_spade` (29460) exists in cache; no instance row/script |

## What is missing

1. No boss AI for `npc.mole_giant`.
2. No mole-hill entry, warning interface, exit, darkness or insect handling.
3. No kill-count NPC param (`varp.total_mole_kills`, id 1515).
4. Drop table is missing `obj.big_bones` and the Hard Falador Diary noted variants.
5. `obj.molepet` is rolled twice: once by the Giant Mole drop table and once by
   `content/other/pets/pack/.../PetDropsTable.kt`. One source must be removed.
6. The elite clue rate is 1/475 in the table but 1/500 on the wiki.
7. Wyson does not exchange mole parts yet.
8. Fire of Eternal Light is not buildable.
9. No private Giant Mole instance.

## Mechanics matrix

| Mechanic | Value / behaviour | Source |
|---|---|---|
| Combat level | 230 | Wiki infobox |
| Hitpoints | 200 | Wiki infobox |
| Size | 3 | Wiki infobox |
| Attack style | Crush, melee only | Wiki infobox |
| Attack speed | 4 ticks (2.4 s) | Wiki infobox; capture attacks every ~2.4 s |
| Max hit | 21 | Wiki infobox |
| Respawn | 16 s (map spawn at 1759, 5189) | Wiki infobox |
| Aggressive | No; retaliates after being attacked | Wiki; capture |
| Slayer XP | 215 | Wiki; capture `4875` |
| Elemental weakness | 50% earth | Current wiki; already set in `npcs.toml` |
| Burrow HP window | 11–100 HP (above 5%, at or below 50%) | Wiki + 2016 change; capture `4509`–`4510` |
| Burrow chance | 25% per player/thrall **attack** (not per hitsplat) | Mod Ash via wiki |
| No burrow at ≤10 HP | Yes | 2016 change; capture `4874`–`4875` (1/30 ≈ 7 HP, no dig, dies) |
| Magic splash | Cannot trigger a burrow | Wiki |
| Poison/venom | Cannot trigger a burrow | Wiki |
| Burrow effect | Move to another lair point, lose aggression | Wiki; capture spawns |
| Dirt throw | May extinguish open light sources within melee range | Wiki; capture `4643`, `4815` |
| Entry | Spade on a mole hill + lit light source | Wiki; capture `4364`–`4393` |
| No light source | "It's going to be dark down there, I should bring a light source." | Capture `4364`, `4372` |
| No tinderbox | Warning interface 568, "Proceed regardless." / "Don't go down yet." | Capture `4383`, `4388` |
| Entry teleport | (1752, 5237) | Capture `4393` |
| Entry message | "You seem to have dropped down into a network of mole tunnels." | Capture `4393` |
| Entry sound/music | `roof_collapse`; "The Mad Mole" (573) | Capture `4393` |
| Darkness | `varbit.darkness_level` = 2; `interface.medium_dark` (98) | Capture `4393` |
| No-light warning | After 9 s: "You hear tiny insects skittering over the ground..." | Wiki; capture `4659` |
| No-light damage | After 18 s: "Tiny biting insects swarm all over you!" then 1 damage/tick | Wiki; capture `4674` |
| Light sources | Lit candle/black candle/torch/oil lamp; closed lanterns unaffected by dirt | Wiki |
| Kill count | `varp.total_mole_kills` 1515; message "Your Giant Mole kill count is: 1." | Capture `4877` |
| Drops | Big bones always; mole claw always; mole skin 1–3 always; noted under Hard Falador Diary | Wiki; capture `4877` |
| Diary noted items | `obj.cert_mole_claw` 7417, `obj.cert_mole_skin` 7419 | Capture `4877` |
| Pet | `obj.molepet` 1/3000, exactly once | Wiki; current table |
| Collection log | Mole claw, mole skin, immaculate mole skin, baby mole | Cache category |

## Exact IDs useful for implementation

- NPCs: `npc.mole_giant` 5779, `npc.mole_baby_01` 5780, `_02` 5781, `_03` 5782.
- Varp: `varp.total_mole_kills` 1515 (the 2024 client calls it `giant_mole_killcount`).
- Varbits: `varbit.darkness_level` 278, `varbit.cws_warning_3` 3853
  (the 2024 client calls it `warning_falador_mole_lair`),
  `varbit.my2arm_fire_mole` 6532, `varbit.falador_diary_hard_complete` 4464.
- Locs: `loc.mole_hill` 12202, `loc.mole_well` 12201,
  `loc.mole_rope_01` 12229, `loc.mole_rope_02` 12230,
  `loc.mole_hill_spade` 29460, `loc.my2arm_fire_pit_mole` 33336,
  `loc.my2arm_fire_eternallight` 33320.
- Sequences: `seq.mole_attack` 3312, `seq.mole_defend` 3311,
  `seq.mole_death` 3310, `seq.mole_burrow_down` 3314,
  `seq.mole_burrow_up` 3315, `seq.mole_ready` 3309, `seq.mole_walk` 3313.
- Sounds: `synth.mole_burrow_down` 1641, `synth.mole_attack` 1642,
  `synth.mole_death` 1645, `synth.mole_hit` 1646,
  `synth.roof_collapse` 1384, `synth.insect_swarm` 1582,
  `synth.insect_bites` 1581, `synth.digspade` 1470.
- Music: `midi.the_mad_mole` 573.
- Warning interface: 568 (text 14, proceed 17, cancel 18); darkness overlay 98.
- Objects: `obj.spade` 952, `obj.big_bones` 532,
  `obj.mole_claw` 7416, `obj.cert_mole_claw` 7417,
  `obj.mole_skin` 7418, `obj.cert_mole_skin` 7419,
  `obj.immaculate_mole_skin` 33382, `obj.molepet` 12646.
- Open light sources: `obj.lit_candle` 33 -> `obj.unlit_candle` 36,
  `obj.lit_black_candle` 32 -> `obj.unlit_black_candle` 38,
  `obj.torch_lit` 594 -> `obj.torch_unlit` 596,
  `obj.oil_lamp_lit` 4524 -> `obj.oil_lamp_unlit` 4522.
  Closed sources: `obj.bullseye_lantern_lit` 4550, `obj.candle_lantern_lit` 4531,
  `obj.oil_lantern_lit` 4539.

## Burrow points

The wiki strategy map gives 13 points. The capture confirms several of them with
small offsets caused by the mole walking after spawning:

```
1739,5220   1775,5232   1737,5208   1778,5207   1753,5197
1778,5187   1741,5177   1758,5179   1746,5170   1775,5173
1778,5164   1760,5163   1753,5150
```

Capture spawn points seen after a burrow:

```
1778,5189   1763,5179   1757,5194   1739,5163   1745,5219
```

Plan: keep a named Kotlin list of candidate points (like
`GROWTHLING_SPAWN_TILES` in the giant-bosses branch), validate each candidate's
3x3 footprint against collision, and pick uniformly while avoiding the player's
current tile and the tile the mole just left. Store the final list in the
module and cover it with a test that every point is walkable and inside the
lair. Verify the exact spawn coordinate convention (SW corner vs centre) in a
live client before committing the numbers.

## Proposed files

```
content/bosses/giant-mole/build.gradle.kts
content/bosses/giant-mole/src/main/kotlin/org/rsmod/content/bosses/giantmole/
  GiantMole.kt                 // boss DSL + burrow reaction + light-source dirt
  GiantMoleLair.kt             // entry/exit, warning, darkness, insect hazard, look-inside
  GiantMoleRules.kt            // pure helpers: burrow window, chance, footprint, open-source map
  GiantMoleModule.kt           // SpadeDigHook, NpcAttackValidateHook, bindings
  GiantMoleDropTable.kt        // Kotlin table if we need the diary transform
content/bosses/giant-mole/src/test/kotlin/org/rsmod/content/bosses/giantmole/
  GiantMoleRulesTest.kt
  GiantMoleDropTableTest.kt
  GiantMoleLairTest.kt
```

Edits:

- `.data/raw-cache/server/npcs.toml` — killcount varp, HP bar mode, and any
  missing attack/defend/death params for `npc.mole_giant`.
- `content/drops/.../giant_mole.toml` — big bones, rate audit, or replace with
  `GiantMoleDropTable.kt` if the diary transform is done in the table.
- `content/other/pets/pack/.../PetDropsTable.kt` — remove the duplicate
  `npc.mole_giant` row.
- `content/interfaces/collection-log/.../CollectionLog.kt` — normalize cert
  items to their `certtemplate` base before granting (needed for noted mole
  parts; also fixes other noted-drop collection log gaps).
- `content/areas/city/falador/.../FaladorTownsfolkScript.kt` — optional Wyson
  exchange follow-up.
- `or-cache/.../InstanceSettingsTable.kt` +
  `api/instances/src/main/resources/gamevals.toml` — optional private instance.
- `content/bosses/giant-mole/pack` — only needed if we add custom interface,
  component, varn or dbtable symbols. The existing cache already provides the
  locs, NPCs and items, so the smallest implementation needs no pack.

## Phase 1 — Core boss and lair (MVP)

### 1. Module and NPC params

- Create `content/bosses/giant-mole/build.gradle.kts` with `base-conventions`
  and the dependencies used by `Scurrius`/`Brutus` (`api.bosses`,
  `api.pluginCommons`, `api.route`, `api.npc`, `api.player`, `api.repo`,
  `api.combat.combatFormulas`). The module is picked up automatically by
  `settings.gradle.kts`.
- Add to the `npc.mole_giant` params:

```toml
"param.killcount_varp"="varp.total_mole_kills"
"param.boss_hp_bar_mode"=1          # ON_ATTACK overworld boss
"param.attack_anim"="seq.mole_attack"
"param.defend_anim"="seq.mole_defend"
"param.death_anim"="seq.mole_death"
"param.defend_sound"="synth.mole_hit"
```

- Keep the existing earth weakness params.
- Verify the cache NPC already has 200 HP, 230 combat, size 3, attack speed 4,
  max hit 21, crush style, respawn 16 and Slayer XP 215. Add overrides only for
  values the cache gets wrong.

### 2. Boss AI (`GiantMole.kt`)

Use a single-phase `BossPluginScript`:

```kotlin
override val spec = boss("npc.mole_giant") {
    stats(attackRate = 4)
    val melee = ability("melee") {
        anim("seq.mole_attack")
        sound("synth.mole_attack", radius = 10)
        hit { damage(0..21).roll(); type(Melee) }
    }
    phase("combat") { rotationSelector { +then(melee) } }
}
```

Register with `BossCombat.register(this, spec, deps, onHit = { maybeBurrow() })`.
The `onHit` callback is the only place that needs custom code.

`maybeBurrow()` rules, in order:

1. Ignore if a burrow is already in progress (`varn.mole_burrow_lock`).
2. Ignore if the hit is a poison/venom hitmark (copy
   `matchesHitmarkGroup` from `PlayerDeathCause.kt`).
3. Ignore magic 0-damage hits (`hit.type == Magic && hit.damage == 0`).
4. Ignore if `npc.hitpoints` is outside `11..100` (derive from
   `baseHitpointsLvl`; at or below 10 HP the mole never digs).
5. Roll once per game tick, not once per hitsplat. A Scythe hits 3 times in
   one attack; rolling per hit would make the effective chance ~58% instead of
   25%. Store `varn.mole_last_burrow_roll_tick` and compare to `deps.mapClock.cycle`.
6. Roll `deps.random.of(4) == 0` (25%).
7. Perform the burrow:
   - set `varn.mole_burrow_lock = 1`;
   - `npc.anim("seq.mole_burrow_down")`;
   - `deps.worldRepo.soundArea(npc.coords, "synth.mole_burrow_down", radius = 10)`;
   - optionally spawn 3–5 `mole_burrow_projectile` visual projectiles around
     the tile (id 570; use the cache projectile or raw id if the symbol cannot
     be resolved);
   - `npc.clearInteractionRoute()`, `npc.resetFaceEntity()`, `npc.defaultMode()`;
   - pick a random walkable burrow point (3x3 footprint, not the player's tile,
     not the current tile) and `npc.telejump(deps.collision, dest)` after 1–2
     ticks via `deps.worldQueues.add(2) { ... }`;
   - on arrival play `seq.mole_burrow_up` and reset the lock;
   - clear the lock on death/respawn as well.
8. Dirt throw: for each player within melee distance of the mole's 3x3
   footprint, find an open light source in inventory and replace it with the
   unlit variant; send
   `"Your <item> seems to have been extinguished by the mud."`.
   Closed lanterns and the Fire of Eternal Light are unaffected. The exact
   chance is not documented; start with a configurable constant and tune against
   the live client. The capture has two successful extinguish messages from two
   burrows in melee range.

Do not reset HP, poison or venom on burrow. Do not re-target players after the
burrow; the mole loses aggression and only re-engages when attacked again.

### 3. Lair access, warning and darkness (`GiantMoleLair.kt`)

Entry interaction:
- `onOpLocU("loc.mole_hill", "obj.spade")` and a `SpadeDigHook` for clicking
  the spade while standing on a hill. The six hill tiles are
  `(2984,3387), (2987,3387), (2999,3375), (2996,3377), (3005,3376), (2989,3378)`.
- `claims(player)` should be true when the player is on/adjacent to one of
  those tiles. The capture digs from `2987,3387`.
- No lit light source and no `varbit.my2arm_fire_mole`:
  `mes("It's going to be dark down there, I should bring a light source.")`
  and abort.
- Otherwise play `seq.human_dig` / `synth.digspade` (the generic spade script
  already does this if the `SpadeDigHook` path is used). If the player has no
  tinderbox and has not yet confirmed, open the warning interface; otherwise
  teleport.
- Warning: use interface 568 with text on component 14 and buttons on 17/18.
  First run `dump_interface {interfaceId:568}` and check whether the cache
  already exposes friendly component names. If it does, use them directly.
  If it does not, do **not** try to encode packed `interface:component` values
  in `gamevals.toml`: `PluginGamevalMerger` only accepts integer values and
  will silently skip strings such as `568:14`. Instead, add a tiny local
  helper that sends `IfSetText` / `IfSetEvents` with the packed id
  `(568 shl 16) or componentId`, using
  `ServerCacheManager.fromComponent(packed)` and `player.client.write(...)`.
  This keeps the raw ids in one named constants block rather than spreading
  them through the script.
  Increment `varbit.cws_warning_3` each time the warning is shown (the capture
  goes 1, 2, 3, 4). On "Proceed", close the interface and run the same dig +
  teleport path; on "Don't", close only.
- Look-inside: `onOpLoc1("loc.mole_hill")` counts players within the lair
  bounds (`x 1728..1791`, `z 5120..5247`) and prints "The lair is currently
  empty." / "There are N players in the lair." The exact OSRS wording can be
  checked live; copy the Venenatis lair peek style if unsure.

On entry:
- `telejump(CoordGrid(1752, 5237))`;
- `mes("You seem to have dropped down into a network of mole tunnels.")`
  as a spam message;
- `soundSynth("synth.roof_collapse")`;
- start "The Mad Mole" (area music should handle it; add the area mapping if
  the music player does not);
- set `varbit.darkness_level = 2` and open `interface.medium_dark` (98).

On exit (rope/well at the southern end):
- clear `varbit.darkness_level` to 0 and close `interface.medium_dark`;
- play the climb animation/sound;
- teleport to the surface well south of White Knights' Castle. Resolve the
  exact tile from the `loc.mole_well` / `loc.mole_rope_*` map placement in the
  cache or in a live client before committing.

Darkness and insects:
- A `PlayerPostTickHook` (or a soft timer like Lumbridge) tracks time without
  a light source while inside the lair.
- `hasLightSource(player)` should use `LightSourcesRow.all()` so the full cache
  list is covered, including Kandarin headgear, Firemaking cape, Bruma torch,
  Jar of Light and Abyssal lantern. Treat `varbit.my2arm_fire_mole != 0` as a
  light source only inside the Mole Hole.
- At 16 ticks (9.6 s) without light: warning + `synth.insect_swarm`.
- At 30 ticks (18 s) without light: "Tiny biting insects swarm all over you!",
  `synth.insect_bites`, then 1 typeless damage per tick until the player
  leaves or lights a source.
- Reset the counter when a light source is present or the player leaves.
- Add a `NpcAttackValidateHook` that denies attacks when
  `varbit.darkness_level > 0` and the player has no light source. The wiki
  says players cannot attack in dark areas but can still be attacked. Scope
  the check carefully so it only affects genuine darkness areas.

### 4. Drops, kill count and collection log

Kill count:
- The generic `KillcountNpcKillHook` reads `param.killcount_varp`; once the
  param is set, the message and varp update are automatic.
- Verify the NPC name renders as "Giant Mole" (the capture message has a space,
  the event symbol is `giant_mole`).

Drops:
- Add `obj.big_bones` to the guaranteed table. The capture confirms big bones;
  the current TOML does not include them.
- Fix the elite clue rate to 1/500 if we are aligning with the current wiki
  (current table: 1/475).
- Keep mole claw always and mole skin 1–3 always.
- Hard Falador Diary noted parts: the cleanest path is to replace the TOML
  table with a Kotlin `@RegisterDropTable` and use `transformObj` on the
  guaranteed mole claw/skin entries:

```kotlin
"obj.mole_claw" count 1 transformObj { player ->
    if (player.vars["varbit.falador_diary_hard_complete"] != 0) {
        "obj.cert_mole_claw"
    } else {
        null
    }
}
```

  The Kotlin table is justified because this is no longer a "simple" table and
  `docs/drops.md` says complex tables stay Kotlin. If the team prefers TOML,
  add a small `falador_hard_diary` hook to `TomlDropHooks` and the resolver
  instead, but do not hardcode Giant Mole logic into the generic drop hook.
- Collection log: when the drop is transformed to a cert item, the current
  `CollectionLog.grant` would try to grant `obj.cert_mole_claw`, which is not
  in the collection log. Normalize cert items to their base item at the top of
  `CollectionLog.grant`:

```kotlin
val baseId = ServerCacheManager.getItem(objId)?.certtemplate?.takeIf { it > 0 } ?: objId
```

  This also fixes noted drops for other content.
- Pet: remove `drop("npc.mole_giant", "obj.molepet", 3000)` from
  `PetDropsTable.kt`. Its own comment says NPCs whose drop table already lists
  the pet must not appear in the table. The Giant Mole drop table already has
  the 1/3000 tertiary.
- Brimstone key: keep `brimstone_key_roll = true`; it is gated by the Konar
  task check. The capture was on a Slayer task (`22:07:24`), so this is worth
  testing.
- Clue: `obj.trail_elite_emote_exp1` with `clue_scroll_box = true`; keep the
  1/500 rate.
- Collection log category: the cache category already references the four
  items; the drop hook grants them automatically. Add a test for the category
  completion varbit if practical.

### 5. Tests

Unit tests (preferred, cheap):
- `GiantMoleRulesTest`
  - burrow window: 10 HP false, 11 HP true, 100 HP true, 101 HP false;
  - 25% chance per attack, once per tick, not per hitsplat;
  - magic splash excluded, poison/venom excluded, 0-damage melee/ranged
    included;
  - every burrow point is walkable for a size-3 NPC and inside the lair;
  - dirt extinguish: open candle/torch/oil lamp -> unlit; bullseye lantern,
    candle lantern, oil lantern and Fire of Eternal Light unchanged.
- `GiantMoleDropTableTest`
  - big bones, mole claw, mole skin 1–3 present;
  - main weights sum to 131; RDT 4/131, gem 6/131;
  - hard diary transforms mole claw/skin to cert variants; no diary leaves
    them unnoted;
  - pet appears exactly once across the drop table and `PetDropsTable`;
  - `CollectionLog.grant` normalizes cert -> base.
- `GiantMoleLairTest`
  - no light source -> message and no teleport;
  - light source + no tinderbox -> warning interface opened;
  - warning proceed -> teleport to 1752,5237 and darkness set;
  - 16-tick warning and 30-tick swarm/damage timers;
  - attack denied without light, allowed with light.

Live-client verification (OpenRune Developer Tools MCP):
1. `walk_to {x:2987,y:3387}` then `item_action {itemName:"spade", option:"Dig"}`
   -> expect "network of mole tunnels" and the darkness overlay.
2. Remove the light source / extinguish the candle -> expect the warning at
   ~9 s and the swarm + damage at ~18 s (`get_skills`, `get_effect_history`).
3. `screenshot {interfaceId:568}` for the warning; `dump_interface` if the
   component names need resolving.
4. `interact_npc {npcName:"giant mole", option:"Attack"}` -> verify
   `seq.mole_defend`, `seq.mole_attack`, `synth.mole_hit` and the HP bar.
5. Force the burrow HP window with `::` commands or repeated attacks; verify
   `seq.mole_burrow_down`, `synth.mole_burrow_down`, the position change,
   loss of aggression and the dirt message.
6. `wait_for {condition:"npc_dead", npcName:"giant mole", timeoutMs:60000}`;
   `get_var_history` for `varp.total_mole_kills`; `get_inventory` /
   `list_ground_items` for big bones, mole claw, mole skin.
7. `::varbit falador_diary_hard_complete 1` then kill -> expect
   `obj.cert_mole_claw` / `obj.cert_mole_skin` and the collection log base
   items unlocked.
8. `::varbit my2arm_fire_mole 1` -> verify no insect damage and no attack
   block.
9. Exit via the rope/well -> darkness cleared, surface well tile reached.

## Phase 2 — Drop-loop and quest polish (recommended follow-up)

### Wyson mole-part exchange

- Add a dialogue branch to `FaladorTownsfolkScript.wyson()` when the player has
  mole parts. Accept noted and unnoted mole claws/skins and immaculate skins.
- Each claw/skin gives one roll: 72% Wyson seed nest, 20% ring nest, 8% empty
  nest. Each immaculate skin gives five rolls. Confirm the exact Wyson nest
  item and matching nest-box symbols against the existing bird nest code
  (`BirdNestSearchScript` / `bird_nest_seeds_jan2019`) before using them.
- If the inventory cannot hold all nests, give the matching nest boxes
  exactly as the wiki describes; do not merge into existing boxes.
- Make the exchange atomic: count parts, verify space/boxes, delete parts,
  add rewards. Add a test for full inventory, noted parts and the immaculate
  five-nest case.

### Fire of Eternal Light

- `onOpLoc1("loc.my2arm_fire_pit_mole")` (or the cache's exact op) checks
  `quest_makingfriendswithmyarm`, 35 Construction, 66 Firemaking,
  hammer/saw/tinderbox and the recipe items.
- On success set `varbit.my2arm_fire_mole = 1`, replace the loc with
  `loc.my2arm_fire_eternallight`, grant 350 Construction + 300 Firemaking XP.
- Treat the varbit as a light source only while inside the Mole Hole.
- If the quest is not implemented, gate on the quest varbit and leave a clear
  TODO; do not fake quest completion.

### Falador shield tracking

- Verify whether the client already draws the flashing arrow with Falador
  shield 3/4 in the inventory when the mole burrows. It is likely client-side.
- If it does not, add a server-side fallback (a chat hint with the direction,
  or a `runClientScript` if the client supports it). Do not invent a custom
  arrow sprite without checking the client's interface support.

### Private instance (current OSRS, not in the 2024 capture)

- Add `dbrow.instance_giant_mole` to `InstanceSettingsTable.kt` and the
  matching `gamevals.dbrow` id in `api/instances/src/main/resources/gamevals.toml`.
- Add `GiantMoleInstance.kt` as an `InstanceScript`:
  - `settingsRow()` = `dbrow.instance_giant_mole`;
  - `area()` copies regions 6992 and 6993 (map squares 27_80 and 27_81) and
    spawns `npc.mole_giant` at 1759,5189 plus the baby moles;
  - `ENTER_OBJECT = loc.mole_hill_spade`, `EXIT_OBJECT = loc.mole_rope_01`
    (inside the instance);
  - fee 25,000; 12,500 after Hard Combat Achievements; halve for ironman.
    The framework's `FEE` is static, so compute the discount in the entry
    flow or add a small `feeFor(player)` hook.
  - boss HP bar in the instance should use ON_ENTER (mode 2) if the client
    supports it; otherwise keep mode 1.
- This is a stretch goal. The core static lair should ship first; the private
  instance must not change the shared lair's behaviour.

## Out of scope

- Combat Achievement tasks (`ca_task_mole_*`). The cache has them, but the
  repo has no CA task system. Do not fake completion.
- The Falador Diary task system. The noted drops can read
  `varbit.falador_diary_hard_complete` even if the diary tasks are not
  implemented; use `::varbit` for testing.
- A full Making Friends With My Arm quest. The Fire of Eternal Light can be
  gated on the existing quest varbit if desired, but the quest itself is a
  separate project.
- Rebuilding the collection log interface. The cache category and drop hook
  are already enough.

## Risks and open questions

1. **Warning interface symbols.** Interface 568 and components 14/17/18 are
   known from the capture, but the cache may not expose friendly component
   names. Resolve them with `dump_interface`; if they are unnamed, use the
   local packed-component helper described in the entry section. Packed
   component ids cannot be added through `gamevals.toml`.
2. **Exact exit tile.** The well south of White Knights' Castle needs to be
   read from the cache map or a live client. Do not guess in code.
3. **Burrow point coordinate convention.** The wiki dots and the capture
   differ by a few tiles because the mole walks after spawning. Validate the
   3x3 footprint with collision before committing the list.
4. **Dirt extinguish rate.** The wiki says "a chance"; the capture shows two
   successes. Start with a named constant, verify with the client and adjust.
5. **Diary and quest systems.** The noted drops and Fire of Eternal Light
   depend on varbits that may not be reachable through normal play yet. That
   is a content gap, not a reason to hardcode the perk.
6. **Client-side shield arrow.** The server cannot draw the HUD arrow if the
   client does not support it. Verify before adding a fallback.
7. **Multi-hit weapons.** Rolling burrow per hitsplat would make the Scythe
   and other multi-hit weapons far stronger at forcing digs. The once-per-tick
   guard is required.
8. **Pet duplicate.** The current pet drop is rolled twice. Fix this before
   shipping or the pet rate will be 1/1500 instead of 1/3000.
9. **Noted drops and collection log.** The collection log must receive the
   base item, not the cert item. Normalize `certtemplate` before granting.
10. **Attack block in darkness.** A generic darkness hook can affect other
    dark areas. Scope it carefully and test Lumbridge Swamp Caves too.

## Suggested commit sequence

1. `feat: add giant mole boss module and NPC params`
2. `feat: implement giant mole entry, lair darkness and light sources`
3. `feat: implement giant mole burrow and combat mechanics`
4. `fix: align giant mole drops with OSRS (big bones, diary noted parts, pet duplicate)`
5. `test: cover giant mole burrow, drops and lair mechanics`
6. Optional: `feat: add Wyson mole part exchange`
7. Optional: `feat: add Fire of Eternal Light`
8. Optional: `feat: add private giant mole instance`

## Definition of done for Phase 1

- A player can dig into the Mole Hole from any mole hill, with the OSRS warning
  flow, music, darkness and insect hazard.
- The Giant Mole fights with OSRS stats and burrows at the correct HP window,
  25% per attack, with the correct splash/poison exclusions, dirt extinguish
  and aggression loss.
- Killing the mole increments `varp.total_mole_kills`, grants 215 Slayer XP
  on task, and rolls the OSRS drop table including big bones, noted diary
  parts, Brimstone key, clue, pet and collection log entries.
- The duplicate pet roll is gone.
- Unit tests cover the burrow rules, drop table and light/darkness helpers.
- The live-client checklist above passes.
